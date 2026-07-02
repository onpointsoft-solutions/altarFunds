"""
System-Admin API views
Accessible only to users with role='system_admin' or (role='admin' AND is_superuser).
Endpoints:
  GET /api/system/financials/    – platform-wide giving, expenses, net income, top churches
  GET /api/system/users/         – all users paginated with role, church, status
  GET /api/system/giving/        – all giving transactions (paginated, filterable)
"""
import logging
from decimal import Decimal
from django.db.models import Sum, Count, Q
from django.utils import timezone
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from common.permissions import _is_system_admin

logger = logging.getLogger(__name__)


def _check_system_admin(user):
    if not _is_system_admin(user):
        return Response({'success': False, 'message': 'System admin access required'},
                        status=status.HTTP_403_FORBIDDEN)
    return None


# ── GET /api/system/financials/ ───────────────────────────────────────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def system_financials(request):
    """
    Returns comprehensive platform-wide financial data:
      - Monthly and all-time giving totals
      - Monthly expenses
      - Net income
      - Top 10 churches by giving this month
      - Last 50 giving transactions
    """
    denied = _check_system_admin(request.user)
    if denied: return denied

    try:
        from giving.models import GivingTransaction
        from giving.serializers import GivingTransactionSerializer
        from expenses.models import Expense

        now       = timezone.now()
        month     = now.month
        year      = now.year

        # ── All-time totals ───────────────────────────────────────────
        all_time_giving = GivingTransaction.objects.filter(
            status='completed'
        ).aggregate(total=Sum('amount'), count=Count('id'))

        # ── This month ────────────────────────────────────────────────
        this_month_qs = GivingTransaction.objects.filter(
            status='completed',
            transaction_date__year=year,
            transaction_date__month=month,
        )
        monthly_giving = this_month_qs.aggregate(total=Sum('amount'), count=Count('id'))

        this_month_exp = Expense.objects.filter(
            status='approved',
            date__year=year,
            date__month=month,
        ).aggregate(total=Sum('amount'))
        monthly_expenses = this_month_exp['total'] or Decimal('0')

        monthly_giving_total = monthly_giving['total'] or Decimal('0')
        net_income = monthly_giving_total - monthly_expenses

        # ── By-church giving breakdown this month ─────────────────────
        top_churches = (
            this_month_qs
            .values('church__name')
            .annotate(total=Sum('amount'), count=Count('id'))
            .order_by('-total')[:10]
        )

        # ── By-category this month ────────────────────────────────────
        by_category = (
            this_month_qs
            .values('category__name')
            .annotate(total=Sum('amount'), count=Count('id'))
            .order_by('-total')
        )

        # ── Last 50 transactions ──────────────────────────────────────
        recent_qs = GivingTransaction.objects.select_related(
            'member__user', 'church', 'category'
        ).order_by('-transaction_date')[:50]

        recent = []
        for tx in recent_qs:
            try:
                member_name = tx.member.user.get_full_name() if tx.member and tx.member.user else 'Anonymous'
            except Exception:
                member_name = 'Unknown'
            recent.append({
                'id':               tx.id,
                'transaction_id':   str(tx.transaction_id),
                'date':             tx.transaction_date.date().isoformat() if tx.transaction_date else '',
                'church':           tx.church.name if tx.church else '',
                'member':           member_name,
                'category':         tx.category.name if tx.category else '',
                'amount':           float(tx.amount),
                'payment_method':   tx.payment_method,
                'status':           tx.status,
            })

        return Response({
            'success': True,
            'data': {
                'all_time': {
                    'total_giving': float(all_time_giving['total'] or 0),
                    'transaction_count': all_time_giving['count'] or 0,
                },
                'this_month': {
                    'total_giving':    float(monthly_giving_total),
                    'transaction_count': monthly_giving['count'] or 0,
                    'total_expenses':  float(monthly_expenses),
                    'net_income':      float(net_income),
                },
                'top_churches': list(top_churches),
                'by_category':  list(by_category),
                'recent_transactions': recent,
            }
        })

    except Exception as e:
        logger.error(f"system_financials error: {e}", exc_info=True)
        return Response({'success': False, 'message': str(e)},
                        status=status.HTTP_500_INTERNAL_SERVER_ERROR)


# ── GET /api/system/users/ ────────────────────────────────────────────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def system_users(request):
    """
    Returns all users with role, church, active status.
    Supports ?search=<email|name>&?role=<role>&page_size=N
    """
    denied = _check_system_admin(request.user)
    if denied: return denied

    try:
        from accounts.models import User

        qs = User.objects.select_related('church').order_by('role', 'email')

        # Optional filters
        search = request.query_params.get('search', '').strip()
        role   = request.query_params.get('role', '').strip()
        if search:
            qs = qs.filter(
                Q(email__icontains=search) |
                Q(first_name__icontains=search) |
                Q(last_name__icontains=search)
            )
        if role:
            qs = qs.filter(role=role)

        page_size = min(int(request.query_params.get('page_size', 200)), 1000)
        total     = qs.count()
        qs        = qs[:page_size]

        # Role counts (always over whole queryset pre-filter for summary)
        all_qs  = User.objects.all()
        roles   = dict(all_qs.values_list('role').annotate(cnt=Count('id')))

        users = []
        for u in qs:
            users.append({
                'id':           u.id,
                'email':        u.email,
                'first_name':   u.first_name,
                'last_name':    u.last_name,
                'role':         u.role,
                'church_name':  u.church.name if u.church else '—',
                'is_active':    u.is_active,
                'is_superuser': u.is_superuser,
                'is_email_verified': u.is_email_verified,
                'date_joined':  u.date_joined.date().isoformat() if u.date_joined else '',
                'last_login':   u.last_login.date().isoformat() if u.last_login else 'Never',
            })

        return Response({
            'success': True,
            'total':   total,
            'count':   len(users),
            'role_summary': roles,
            'results': users,
        })

    except Exception as e:
        logger.error(f"system_users error: {e}", exc_info=True)
        return Response({'success': False, 'message': str(e)},
                        status=status.HTTP_500_INTERNAL_SERVER_ERROR)


# ── GET /api/system/giving/ ───────────────────────────────────────────────────
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def system_giving(request):
    """
    Paginated giving transaction list for system admin.
    Params: ?page_size=N&church=<id>&status=<pending|completed>
    """
    denied = _check_system_admin(request.user)
    if denied: return denied

    try:
        from giving.models import GivingTransaction

        qs = GivingTransaction.objects.select_related(
            'member__user', 'church', 'category'
        ).order_by('-transaction_date')

        church_id = request.query_params.get('church')
        tx_status = request.query_params.get('status')
        if church_id: qs = qs.filter(church_id=church_id)
        if tx_status: qs = qs.filter(status=tx_status)

        page_size = min(int(request.query_params.get('page_size', 100)), 500)
        total = qs.count()
        qs    = qs[:page_size]

        rows = []
        for tx in qs:
            try:
                member_name = tx.member.user.get_full_name() if tx.member and tx.member.user else 'Anonymous'
            except Exception:
                member_name = 'Unknown'
            rows.append({
                'id':              tx.id,
                'transaction_id':  str(tx.transaction_id),
                'date':            tx.transaction_date.date().isoformat() if tx.transaction_date else '',
                'church':          tx.church.name if tx.church else '',
                'member':          member_name,
                'category':        tx.category.name if tx.category else '',
                'amount':          float(tx.amount),
                'payment_method':  tx.payment_method,
                'status':          tx.status,
            })

        return Response({
            'success':   True,
            'total':     total,
            'count':     len(rows),
            'results':   rows,
        })

    except Exception as e:
        logger.error(f"system_giving error: {e}", exc_info=True)
        return Response({'success': False, 'message': str(e)},
                        status=status.HTTP_500_INTERNAL_SERVER_ERROR)
