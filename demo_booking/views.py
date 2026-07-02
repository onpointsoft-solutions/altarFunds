import logging
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from django.core.mail import send_mail
from django.conf import settings
from .models import DemoBooking

logger = logging.getLogger(__name__)


@api_view(['POST'])
@permission_classes([AllowAny])
def book_demo(request):
    """
    POST /api/demo-booking/
    Public endpoint — no authentication required.
    Stores a demo request and sends a confirmation email.
    """
    data = request.data

    required = ['firstName', 'lastName', 'email', 'phoneNumber', 'churchName']
    missing = [f for f in required if not data.get(f, '').strip()]
    if missing:
        return Response(
            {'success': False, 'message': f'Missing fields: {", ".join(missing)}'},
            status=status.HTTP_400_BAD_REQUEST
        )

    try:
        booking = DemoBooking.objects.create(
            first_name   = data['firstName'].strip(),
            last_name    = data['lastName'].strip(),
            email        = data['email'].strip().lower(),
            phone_number = data['phoneNumber'].strip(),
            church_name  = data['churchName'].strip(),
            church_size  = data.get('churchSize', '').strip(),
            demo_type    = data.get('demoType', 'online').strip(),
            specific_needs = data.get('specificNeeds', '').strip(),
        )

        # Parse optional date / time
        demo_date = data.get('demoDate', '').strip()
        demo_time = data.get('demoTime', '').strip()
        if demo_date:
            from datetime import date
            try:
                booking.demo_date = date.fromisoformat(demo_date)
            except ValueError:
                pass
        if demo_time:
            from datetime import time
            try:
                h, m = demo_time.split(':')
                booking.demo_time = time(int(h), int(m))
            except Exception:
                pass
        booking.save(update_fields=['demo_date', 'demo_time'])

        # Send confirmation email (non-fatal)
        try:
            send_mail(
                subject='Demo Request Received — Sanctum',
                message=(
                    f"Hi {booking.full_name},\n\n"
                    f"Thank you for requesting a demo of Sanctum!\n\n"
                    f"Our team will contact you within 24 hours to confirm your appointment.\n\n"
                    f"Details:\n"
                    f"  Church: {booking.church_name}\n"
                    f"  Type:   {booking.get_demo_type_display()}\n"
                    f"  Date:   {demo_date or 'To be confirmed'}\n"
                    f"  Time:   {demo_time or 'To be confirmed'}\n\n"
                    f"If you have questions, reach us at demo@sanctum.co.ke\n\n"
                    f"Regards,\nThe Sanctum Team"
                ),
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[booking.email],
                fail_silently=True,
            )

            # Notify internal sales team
            send_mail(
                subject=f'New Demo Request — {booking.church_name}',
                message=(
                    f"New demo booking #{ booking.id }\n\n"
                    f"Name:    {booking.full_name}\n"
                    f"Email:   {booking.email}\n"
                    f"Phone:   {booking.phone_number}\n"
                    f"Church:  {booking.church_name} ({booking.church_size})\n"
                    f"Type:    {booking.get_demo_type_display()}\n"
                    f"Date:    {demo_date or 'Not specified'}\n"
                    f"Time:    {demo_time or 'Not specified'}\n"
                    f"Needs:   {booking.specific_needs or 'None'}\n"
                ),
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=['demo@sanctum.co.ke'],
                fail_silently=True,
            )
        except Exception as mail_err:
            logger.warning(f"Demo booking email failed: {mail_err}")

        return Response({
            'success': True,
            'message': 'Demo request submitted successfully. We will contact you within 24 hours.',
            'booking_id': booking.id,
        }, status=status.HTTP_201_CREATED)

    except Exception as e:
        logger.error(f"Demo booking error: {e}", exc_info=True)
        return Response(
            {'success': False, 'message': f'Failed to submit demo request: {str(e)}'},
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def list_demo_bookings(request):
    """GET /api/demo-booking/list/ — system admin only."""
    from common.permissions import _is_system_admin
    if not _is_system_admin(request.user):
        return Response({'detail': 'Forbidden'}, status=status.HTTP_403_FORBIDDEN)
    bookings = DemoBooking.objects.all().values(
        'id', 'first_name', 'last_name', 'email', 'phone_number',
        'church_name', 'church_size', 'demo_type', 'demo_date',
        'demo_time', 'status', 'specific_needs', 'created_at'
    )
    return Response({'count': len(list(bookings)), 'results': list(bookings)})
