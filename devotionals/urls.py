from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import DevotionalViewSet, DevotionalCommentViewSet

app_name = 'devotionals'

router = DefaultRouter()
router.register(r'devotionals', DevotionalViewSet, basename='devotional')
router.register(r'comments', DevotionalCommentViewSet, basename='devotional-comment')

urlpatterns = [
    path('', include(router.urls)),
    # Additional endpoints for devotional-specific comments and reactions
    path('devotionals/<int:pk>/comments/', DevotionalViewSet.as_view({'get': 'comments'}), name='devotional-comments'),
    path('devotionals/<int:pk>/reactions/', DevotionalViewSet.as_view({'get': 'reactions'}), name='devotional-reactions'),
]
