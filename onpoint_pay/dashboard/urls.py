from django.urls import path
from . import views

urlpatterns = [
    path('', views.dashboard_view, name='dashboard'),
    path('docs/', views.docs_view, name='docs'),
    path('support/', views.support_view, name='support'),
]
