from django.urls import path
from .views import (
    BudgetListCreateView, BudgetDetailView,
    BudgetPinListCreateView, BudgetPinRevokeView, verify_budget_pin,
)

app_name = 'budgets'

urlpatterns = [
    # Budget CRUD
    path('', BudgetListCreateView.as_view(), name='budget-list-create'),
    path('<int:pk>/', BudgetDetailView.as_view(), name='budget-detail'),

    # Budget Access PINs
    path('pins/',                   BudgetPinListCreateView.as_view(), name='budget-pin-list-create'),
    path('pins/<int:pk>/revoke/',   BudgetPinRevokeView.as_view(),     name='budget-pin-revoke'),
    path('pins/verify/',            verify_budget_pin,                 name='budget-pin-verify'),
]
