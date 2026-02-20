from rest_framework import viewsets, status, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from django.utils import timezone
from django.db import transaction
from .models import AttendanceRecord, MemberAttendance, ServiceType, AttendanceSettings
from .serializers import (
    AttendanceRecordSerializer, AttendanceRecordDetailSerializer,
    MemberAttendanceSerializer, ServiceTypeSerializer, 
    AttendanceSettingsSerializer, QuickAttendanceSerializer
)
from accounts.permissions import IsChurchAdmin, IsChurchStaff, IsUsher
from churches.models import ChurchService
from accounts.models import User


class AttendanceRecordViewSet(viewsets.ModelViewSet):
    """ViewSet for AttendanceRecord model"""
    
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = AttendanceRecordSerializer
    
    def get_queryset(self):
        user = self.request.user
        if user.is_superuser:
            return AttendanceRecord.objects.all()
        
        # Filter by user's church
        if user.church:
            return AttendanceRecord.objects.filter(church=user.church)
        return AttendanceRecord.objects.none()
    
    def get_serializer_class(self):
        if self.action == 'retrieve':
            return AttendanceRecordDetailSerializer
        return AttendanceRecordSerializer
    
    def perform_create(self, serializer):
        # Set the church from the current user
        user = self.request.user
        if not user.church:
            raise serializers.ValidationError("User must be assigned to a church")
        
        serializer.save(
            church=user.church,
            recorded_by=user
        )
    
    @action(detail=False, methods=['post'], permission_classes=[IsUsher])
    def quick_mark_attendance(self, request):
        """Quick attendance marking for ushers"""
        serializer = QuickAttendanceSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        
        user = request.user
        if not user.church:
            return Response(
                {"error": "Usher must be assigned to a church"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        try:
            with transaction.atomic():
                # Get or create attendance record
                service = ChurchService.objects.get(id=serializer.validated_data['service_id'])
                service_date = serializer.validated_data['service_date']
                
                attendance_record, created = AttendanceRecord.objects.get_or_create(
                    church=user.church,
                    service=service,
                    service_date=service_date,
                    recorded_by=user,
                    defaults={
                        'total_attendance': 0,
                        'male_attendance': 0,
                        'female_attendance': 0,
                        'children_attendance': 0,
                        'youth_attendance': 0,
                        'visitors_count': 0,
                        'new_converts': 0,
                        'notes': serializer.validated_data.get('notes', '')
                    }
                )
                
                # Mark members as present
                member_ids = serializer.validated_data['member_ids']
                members = User.objects.filter(id__in=member_ids, church=user.church)
                
                marked_count = 0
                for member in members:
                    member_attendance, created = MemberAttendance.objects.get_or_create(
                        attendance_record=attendance_record,
                        member=member,
                        defaults={
                            'is_present': True,
                            'notes': 'Marked by usher'
                        }
                    )
                    if created or not member_attendance.is_present:
                        member_attendance.is_present = True
                        member_attendance.save()
                        marked_count += 1
                
                # Update total counts
                attendance_record.total_attendance = MemberAttendance.objects.filter(
                    attendance_record=attendance_record, 
                    is_present=True
                ).count()
                attendance_record.save()
                
                return Response({
                    "message": f"Successfully marked {marked_count} members as present",
                    "attendance_record_id": attendance_record.id,
                    "total_present": attendance_record.total_attendance
                })
        
        except ChurchService.DoesNotExist:
            return Response(
                {"error": "Service not found"},
                status=status.HTTP_404_NOT_FOUND
            )
        except Exception as e:
            return Response(
                {"error": str(e)},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
    
    @action(detail=True, methods=['post'], permission_classes=[IsUsher])
    def mark_member_present(self, request, pk=None):
        """Mark a specific member as present for this attendance record"""
        attendance_record = self.get_object()
        member_id = request.data.get('member_id')
        notes = request.data.get('notes', '')
        
        if not member_id:
            return Response(
                {"error": "member_id is required"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        try:
            member = User.objects.get(id=member_id, church=attendance_record.church)
            
            member_attendance, created = MemberAttendance.objects.get_or_create(
                attendance_record=attendance_record,
                member=member,
                defaults={
                    'is_present': True,
                    'notes': notes or 'Marked by usher'
                }
            )
            
            if not created:
                member_attendance.is_present = True
                member_attendance.notes = notes or member_attendance.notes
                member_attendance.save()
            
            # Update total count
            attendance_record.total_attendance = MemberAttendance.objects.filter(
                attendance_record=attendance_record, 
                is_present=True
            ).count()
            attendance_record.save()
            
            return Response({
                "message": f"Successfully marked {member.get_full_name()} as present",
                "member_attendance_id": member_attendance.id
            })
        
        except User.DoesNotExist:
            return Response(
                {"error": "Member not found"},
                status=status.HTTP_404_NOT_FOUND
            )
    
    @action(detail=True, methods=['get'])
    def attendance_summary(self, request, pk=None):
        """Get attendance summary for a specific record"""
        attendance_record = self.get_object()
        
        member_attendances = MemberAttendance.objects.filter(
            attendance_record=attendance_record
        )
        
        present_count = member_attendances.filter(is_present=True).count()
        visitor_count = member_attendances.filter(is_visitor=True).count()
        
        summary = {
            'total_registered': member_attendances.count(),
            'present_count': present_count,
            'visitor_count': visitor_count,
            'recorded_totals': {
                'total_attendance': attendance_record.total_attendance,
                'male_attendance': attendance_record.male_attendance,
                'female_attendance': attendance_record.female_attendance,
                'children_attendance': attendance_record.children_attendance,
                'youth_attendance': attendance_record.youth_attendance,
                'visitors_count': attendance_record.visitors_count,
                'new_converts': attendance_record.new_converts
            }
        }
        
        return Response(summary)


class MemberAttendanceViewSet(viewsets.ModelViewSet):
    """ViewSet for MemberAttendance model"""
    
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = MemberAttendanceSerializer
    
    def get_queryset(self):
        user = self.request.user
        if user.is_superuser:
            return MemberAttendance.objects.all()
        
        # Filter by user's church
        if user.church:
            return MemberAttendance.objects.filter(attendance_record__church=user.church)
        return MemberAttendance.objects.none()
    
    def perform_create(self, serializer):
        # Validate that user can create attendance for this record
        attendance_record = serializer.validated_data['attendance_record']
        user = self.request.user
        
        if not user.is_superuser and attendance_record.church != user.church:
            raise serializers.ValidationError("You can only create attendance for your church")
        
        serializer.save()


class ServiceTypeViewSet(viewsets.ModelViewSet):
    """ViewSet for ServiceType model"""
    
    permission_classes = [IsChurchAdmin]
    serializer_class = ServiceTypeSerializer
    
    def get_queryset(self):
        user = self.request.user
        if user.is_superuser:
            return ServiceType.objects.all()
        
        if user.church:
            return ServiceType.objects.filter(church=user.church)
        return ServiceType.objects.none()
    
    def perform_create(self, serializer):
        user = self.request.user
        if not user.church:
            raise serializers.ValidationError("User must be assigned to a church")
        
        serializer.save(church=user.church)


class AttendanceSettingsViewSet(viewsets.ModelViewSet):
    """ViewSet for AttendanceSettings model"""
    
    permission_classes = [IsChurchAdmin]
    serializer_class = AttendanceSettingsSerializer
    
    def get_queryset(self):
        user = self.request.user
        if user.is_superuser:
            return AttendanceSettings.objects.all()
        
        if user.church:
            return AttendanceSettings.objects.filter(church=user.church)
        return AttendanceSettings.objects.none()
    
    def perform_create(self, serializer):
        user = self.request.user
        if not user.church:
            raise serializers.ValidationError("User must be assigned to a church")
        
        serializer.save(church=user.church)
