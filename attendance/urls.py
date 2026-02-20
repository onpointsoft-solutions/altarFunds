from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    AttendanceRecordViewSet, MemberAttendanceViewSet,
    ServiceTypeViewSet, AttendanceSettingsViewSet
)

router = DefaultRouter()
router.register(r'records', AttendanceRecordViewSet, basename='attendance-records')
router.register(r'members', MemberAttendanceViewSet, basename='member-attendances')
router.register(r'service-types', ServiceTypeViewSet, basename='service-types')
router.register(r'settings', AttendanceSettingsViewSet, basename='attendance-settings')

app_name = 'attendance'

urlpatterns = [
    path('', include(router.urls)),
]
