from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .join_requests_views import ChurchJoinRequestViewSet

app_name = 'join_requests'

router = DefaultRouter()
router.register(r'', ChurchJoinRequestViewSet, basename='church-join-request')

urlpatterns = [
    path('', include(router.urls)),
]
