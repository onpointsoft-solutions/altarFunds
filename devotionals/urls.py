from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import DevotionalViewSet, DevotionalCommentViewSet

app_name = 'devotionals'

router = DefaultRouter()
router.register(r'devotionals', DevotionalViewSet,        basename='devotional')
router.register(r'comments',    DevotionalCommentViewSet, basename='devotional-comment')

# The router auto-generates:
#   GET    /devotionals/                 — list
#   POST   /devotionals/                 — create
#   GET    /devotionals/{id}/            — retrieve  (uses DevotionalDetailSerializer)
#   PUT    /devotionals/{id}/            — update
#   DELETE /devotionals/{id}/            — destroy
#   GET    /devotionals/{id}/comments/   — @action
#   POST   /devotionals/{id}/comment/    — @action
#   GET    /devotionals/{id}/reactions/  — @action
#   POST   /devotionals/{id}/react/      — @action
#   DELETE /devotionals/{id}/react/      — @action (same method, different HTTP verb)

urlpatterns = [
    path('', include(router.urls)),

    # Explicit paths for endpoints that need both GET and POST/DELETE
    # (the router handles GET-only actions automatically; explicit paths
    #  are needed for the comment POST and react DELETE).
    path(
        'devotionals/<int:pk>/comments/',
        DevotionalViewSet.as_view({'get': 'comments', 'post': 'comment'}),
        name='devotional-comments',
    ),
    path(
        'devotionals/<int:pk>/comment/',
        DevotionalViewSet.as_view({'post': 'comment'}),
        name='devotional-comment-post',
    ),
    path(
        'devotionals/<int:pk>/reactions/',
        DevotionalViewSet.as_view({'get': 'reactions'}),
        name='devotional-reactions',
    ),
    path(
        'devotionals/<int:pk>/react/',
        DevotionalViewSet.as_view({'post': 'react', 'delete': 'react'}),
        name='devotional-react',
    ),
]
