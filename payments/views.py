from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.shortcuts import get_object_or_404
from .models import PaymentRequest, Payment, Transaction
from .serializers import PaymentRequestSerializer, PaymentSerializer, TransactionSerializer

class PaymentRequestViewSet(viewsets.ModelViewSet):
    queryset = PaymentRequest.objects.all()
    serializer_class = PaymentRequestSerializer

    @action(detail=True, methods=['post'])
    def approve(self, request, pk=None):
        payment_request = self.get_object()
        payment_request.status = 'approved'
        payment_request.save()
        return Response({'status': 'approved'})

class PaymentViewSet(viewsets.ModelViewSet):
    queryset = Payment.objects.all()
    serializer_class = PaymentSerializer

class TransactionViewSet(viewsets.ModelViewSet):
    queryset = Transaction.objects.all()
    serializer_class = TransactionSerializer
