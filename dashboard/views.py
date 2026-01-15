from django.shortcuts import render
from django.contrib.auth.decorators import login_required
from rest_framework import viewsets, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from django.db.models import Sum, Count, Q
from django.utils import timezone
from datetime import timedelta

from giving.models import GivingTransaction, GivingCategory
from expenses.models import Expense
from accounts.models import User

@login_required
def dashboard_view(request):
    """Renders the main dashboard page."""
    return render(request, 'dashboard/index.html')

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def financial_summary(request):
    """Get financial summary for dashboard."""
    user = request.user
    
    # Calculate income (donations)
    total_income = GivingTransaction.objects.filter(
        church=user.church
    ).aggregate(total=Sum('amount'))['total'] or 0
    
    monthly_income = GivingTransaction.objects.filter(
        church=user.church,
        created_at__gte=timezone.now() - timedelta(days=30)
    ).aggregate(total=Sum('amount'))['total'] or 0
    
    # Calculate expenses
    total_expenses = Expense.objects.filter(
        church=user.church
    ).aggregate(total=Sum('amount'))['total'] or 0
    
    monthly_expenses = Expense.objects.filter(
        church=user.church,
        created_at__gte=timezone.now() - timedelta(days=30)
    ).aggregate(total=Sum('amount'))['total'] or 0
    
    return Response({
        'totalIncome': total_income,
        'monthlyIncome': monthly_income,
        'totalExpenses': total_expenses,
        'monthlyExpenses': monthly_expenses,
        'netIncome': total_income - total_expenses,
        'monthlyNetIncome': monthly_income - monthly_expenses,
    })

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def monthly_trend(request):
    """Get monthly income/expense trend."""
    user = request.user
    months = 12
    
    trend_data = []
    for i in range(months):
        month_start = timezone.now() - timedelta(days=30 * (months - i - 1))
        month_end = timezone.now() - timedelta(days=30 * (months - i - 2))
        
        income = GivingTransaction.objects.filter(
            church=user.church,
            created_at__range=[month_start, month_end]
        ).aggregate(total=Sum('amount'))['total'] or 0
        
        expenses = Expense.objects.filter(
            church=user.church,
            created_at__range=[month_start, month_end]
        ).aggregate(total=Sum('amount'))['total'] or 0
        
        trend_data.append({
            'month': month_start.strftime('%Y-%m'),
            'income': income,
            'expenses': expenses,
            'net': income - expenses
        })
    
    return Response(trend_data)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def income_breakdown(request):
    """Get income breakdown by category."""
    user = request.user
    
    categories = GivingCategory.objects.filter(church=user.church)
    breakdown = []
    
    for category in categories:
        total = GivingTransaction.objects.filter(
            church=user.church,
            category=category
        ).aggregate(total=Sum('amount'))['total'] or 0
        
        breakdown.append({
            'category': category.name,
            'amount': total,
            'percentage': (total / max(1, GivingTransaction.objects.filter(
                church=user.church
            ).aggregate(total=Sum('amount'))['total'] or 1)) * 100
        })
    
    return Response(breakdown)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def expense_breakdown(request):
    """Get expense breakdown by category."""
    user = request.user
    
    # Group expenses by category
    expenses = Expense.objects.filter(church=user.church).values('category').annotate(
        total=Sum('amount'),
        count=Count('id')
    ).order_by('-total')
    
    total_expenses = Expense.objects.filter(church=user.church).aggregate(
        total=Sum('amount')
    )['total'] or 1
    
    breakdown = []
    for expense in expenses:
        breakdown.append({
            'category': expense['category'],
            'amount': expense['total'],
            'count': expense['count'],
            'percentage': (expense['total'] / total_expenses) * 100
        })
    
    return Response(breakdown)
