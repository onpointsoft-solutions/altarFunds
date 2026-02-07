from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import SuggestionViewSet

app_name = 'suggestions'

router = DefaultRouter()
router.register(r'', SuggestionViewSet, basename='suggestion')

urlpatterns = [
    path('', include(router.urls)),
]
