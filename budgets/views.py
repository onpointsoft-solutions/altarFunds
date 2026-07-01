from rest_framework import generics, permissions, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from django.db.models import Sum, Count, Q, F
from django.utils import timezone
from .models import Budget, BudgetAccessPin, _generate_pin
from accounts.models import User
from rest_framework import serializers
from datetime import timedelta


class BudgetSerializer(serializers.ModelSerializer):
    remaining_amount = serializers.ReadOnlyField()
    utilization_percentage = serializers.ReadOnlyField()
    # user_email for display; user PK is set automatically from the request
    user_email = serializers.EmailField(source='user.email', read_only=True)

    class Meta:
        model = Budget
        fields = '__all__'
        read_only_fields = ['user', 'created_at', 'updated_at']


class BudgetListCreateView(generics.ListCreateAPIView):
    """List and create budgets"""
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = BudgetSerializer

    def get_queryset(self):
        user = self.request.user
        # Church admins (pastor, treasurer) see all budgets for their church users
        if user.role in ('treasurer', 'pastor', 'denomination_admin', 'system_admin'):
            from churches.models import Church
            queryset = Budget.objects.filter(user__church=user.church)
        else:
            queryset = Budget.objects.filter(user=user)

        year = self.request.query_params.get('year')
        department = self.request.query_params.get('department')
        if year:
            queryset = queryset.filter(year=year)
        if department:
            queryset = queryset.filter(department__icontains=department)

        return queryset.order_by('-year', '-month')

    def perform_create(self, serializer):
        """Automatically assign the budget to the requesting user."""
        serializer.save(user=self.request.user)


class BudgetDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Retrieve, update, delete budget"""
    permission_classes = [permissions.IsAuthenticated]
    queryset = Budget.objects.all()
    serializer_class = BudgetSerializer
    
    def get_queryset(self):
        return Budget.objects.filter(user=self.request.user)


# ── BudgetAccessPin serializers + views ──────────────────────────────────────
# The BudgetAccessPin model lives in budgets/models.py — imported above.



class BudgetPinSerializer(serializers.ModelSerializer):
    is_valid    = serializers.SerializerMethodField()
    remaining   = serializers.SerializerMethodField()
    created_by_email = serializers.EmailField(source='created_by.email', read_only=True)

    class Meta:
        model = BudgetAccessPin
        fields = [
            'id', 'pin', 'label', 'expires_at', 'is_active',
            'view_count', 'max_uses', 'created_at',
            'created_by_email', 'is_valid', 'remaining',
        ]
        read_only_fields = ['id', 'pin', 'view_count', 'created_at', 'created_by_email']

    def get_is_valid(self, obj):
        return obj.is_valid()

    def get_remaining(self, obj):
        """Seconds until expiry (negative if expired)."""
        delta = obj.expires_at - timezone.now()
        return int(delta.total_seconds())


class BudgetPinListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/budgets/pins/         — list all PINs the treasurer created
    POST /api/budgets/pins/         — generate a new PIN
        Body: { "label": "...", "hours": 24, "max_uses": null }
    """
    serializer_class   = BudgetPinSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        if not user.church:
            return BudgetAccessPin.objects.none()
        return BudgetAccessPin.objects.filter(church=user.church)

    def create(self, request, *args, **kwargs):
        if request.user.role not in ('treasurer', 'pastor',
                                     'denomination_admin', 'system_admin'):
            return Response(
                {'error': 'Only the treasurer or pastor can create budget access PINs.'},
                status=status.HTTP_403_FORBIDDEN,
            )
        if not request.user.church:
            return Response({'error': 'No church assigned.'}, status=status.HTTP_400_BAD_REQUEST)

        hours    = int(request.data.get('hours', 24))
        label    = request.data.get('label', '').strip()
        max_uses = request.data.get('max_uses', None)
        if max_uses is not None:
            try:
                max_uses = int(max_uses)
            except (TypeError, ValueError):
                max_uses = None

        hours = max(1, min(hours, 720))   # clamp: 1 h – 30 days

        pin_obj = BudgetAccessPin.objects.create(
            pin        = _generate_pin(6),
            label      = label,
            expires_at = timezone.now() + timedelta(hours=hours),
            max_uses   = max_uses,
            created_by = request.user,
            church     = request.user.church,
        )
        return Response(BudgetPinSerializer(pin_obj).data, status=status.HTTP_201_CREATED)


class BudgetPinRevokeView(generics.UpdateAPIView):
    """
    PATCH /api/budgets/pins/<id>/revoke/   — deactivate a PIN immediately
    """
    serializer_class   = BudgetPinSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return BudgetAccessPin.objects.filter(church=self.request.user.church)

    def patch(self, request, *args, **kwargs):
        pin_obj = self.get_object()
        pin_obj.is_active = False
        pin_obj.save(update_fields=['is_active'])
        return Response({'message': 'PIN revoked.', 'id': pin_obj.pk})


@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def verify_budget_pin(request):
    """
    POST /api/budgets/pins/verify/
    Body: { "pin": "123456" }

    Called by the Android member app.  On success returns a lightweight
    budget summary — total allocated, total spent, remaining — for the
    member's church.  The member never sees individual line-item details
    unless the treasurer explicitly shares them.

    Returns 403 if the PIN is invalid / expired.
    """
    pin_value = request.data.get('pin', '').strip()
    if not pin_value:
        return Response({'error': 'pin is required.'}, status=status.HTTP_400_BAD_REQUEST)

    user = request.user
    if not user.church:
        return Response({'error': 'No church assigned.'}, status=status.HTTP_400_BAD_REQUEST)

    try:
        pin_obj = BudgetAccessPin.objects.get(
            pin    = pin_value,
            church = user.church,
        )
    except BudgetAccessPin.DoesNotExist:
        return Response({'error': 'Invalid PIN.'}, status=status.HTTP_403_FORBIDDEN)

    if not pin_obj.is_valid():
        return Response(
            {'error': 'PIN has expired or reached its usage limit.'},
            status=status.HTTP_403_FORBIDDEN,
        )

    # Increment usage counter
    BudgetAccessPin.objects.filter(pk=pin_obj.pk).update(
        view_count=F('view_count') + 1
    )

    # Build a budget summary for the church
    budgets = Budget.objects.filter(user__church=user.church)
    total_allocated = budgets.aggregate(s=Sum('allocated_amount'))['s'] or 0
    total_spent     = budgets.aggregate(s=Sum('spent_amount'))['s'] or 0

    by_department = list(
        budgets.values('department')
               .annotate(
                   allocated=Sum('allocated_amount'),
                   spent=Sum('spent_amount'),
               )
               .order_by('department')
    )

    return Response({
        'success'        : True,
        'church_name'    : user.church.name,
        'pin_label'      : pin_obj.label or 'Budget Summary',
        'expires_at'     : pin_obj.expires_at,
        'total_allocated': float(total_allocated),
        'total_spent'    : float(total_spent),
        'total_remaining': float(total_allocated - total_spent),
        'by_department'  : [
            {
                'department': d['department'],
                'allocated' : float(d['allocated'] or 0),
                'spent'     : float(d['spent']     or 0),
                'remaining' : float((d['allocated'] or 0) - (d['spent'] or 0)),
            }
            for d in by_department
        ],
    })
