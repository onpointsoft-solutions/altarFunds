from django.urls import path
from .views import (
    financial_summary,
    giving_trends,
    member_statistics,
    church_performance,
    system_overview
)

app_name = 'altarfunds_reports'

urlpatterns = [
    path('financial-summary/', financial_summary, name='financial_summary'),
    path('giving-trends/', giving_trends, name='giving_trends'),
    path('member-statistics/', member_statistics, name='member_statistics'),
    path('church-performance/', church_performance, name='church_performance'),
    path('system-overview/', system_overview, name='system_overview'),
]
