from django.shortcuts import render
from django.http import HttpResponse, JsonResponse
from django.contrib.auth.decorators import login_required
from django.db.models import Sum, Count, Q
from django.utils import timezone
from datetime import timedelta
from payments.models import Transaction
from merchants.models import ApiKey


@login_required
def dashboard_view(request):
    """Main merchant dashboard with transaction management"""
    # Get statistics
    merchant = request.user
    total_revenue = Transaction.objects.filter(
        merchant=merchant,
        status='completed'
    ).aggregate(total=Sum('amount'))['total'] or 0
    
    transaction_count = Transaction.objects.filter(merchant=merchant).count()
    
    completed_count = Transaction.objects.filter(
        merchant=merchant,
        status='completed'
    ).count()
    
    success_rate = (completed_count / transaction_count * 100) if transaction_count > 0 else 0
    
    # Get unique customers
    active_customers = Transaction.objects.filter(
        merchant=merchant,
        status='completed'
    ).values('customer_email').distinct().count()
    
    # Get recent transactions
    recent_transactions = Transaction.objects.filter(
        merchant=merchant
    ).order_by('-created_at')[:10]
    
    # Get API keys
    api_keys = ApiKey.objects.filter(merchant=merchant).order_by('-created_at')
    
    context = {
        'total_revenue': f"{total_revenue:,.0f}",
        'transaction_count': transaction_count,
        'success_rate': f"{success_rate:.1f}",
        'active_customers': active_customers,
        'recent_transactions': recent_transactions,
        'api_keys': api_keys,
    }
    
    return render(request, 'merchant_dashboard.html', context)


def docs_view(request):
    """API documentation view"""
    return render(request, 'docs.html')


def support_view(request):
    """Support page view"""
    return render(request, 'support.html')


@login_required
def get_dashboard_stats(request):
    """AJAX endpoint for dashboard statistics"""
    merchant = request.user
    
    # Get date range from request
    days = int(request.GET.get('days', 30))
    start_date = timezone.now() - timedelta(days=days)
    
    stats = {
        'total_revenue': Transaction.objects.filter(
            merchant=merchant,
            status='completed',
            created_at__gte=start_date
        ).aggregate(total=Sum('amount'))['total'] or 0,
        
        'transaction_count': Transaction.objects.filter(
            merchant=merchant,
            created_at__gte=start_date
        ).count(),
        
        'completed_count': Transaction.objects.filter(
            merchant=merchant,
            status='completed',
            created_at__gte=start_date
        ).count(),
        
        'failed_count': Transaction.objects.filter(
            merchant=merchant,
            status='failed',
            created_at__gte=start_date
        ).count(),
        
        'pending_count': Transaction.objects.filter(
            merchant=merchant,
            status='pending',
            created_at__gte=start_date
        ).count(),
    }
    
    return JsonResponse(stats)