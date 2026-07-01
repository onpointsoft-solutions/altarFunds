import json
import logging
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from .models import AuditLog

logger = logging.getLogger(__name__)


def _is_elevated(user):
    """system_admin or legacy admin superuser."""
    return user.role == 'system_admin' or (user.role == 'admin' and user.is_superuser)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def audit_logs(request):
    """
    GET /api/audit/logs/?page_size=N
    Accessible to: system_admin, admin (superuser), denomination_admin.
    """
    user = request.user
    if not (_is_elevated(user) or user.role == 'denomination_admin'):
        return Response({'detail': 'Forbidden'}, status=status.HTTP_403_FORBIDDEN)

    try:
        page_size = min(int(request.query_params.get('page_size', 50)), 500)
    except (ValueError, TypeError):
        page_size = 50

    try:
        qs = AuditLog.objects.select_related('user').order_by('-created_at')[:page_size]
        data = []
        for log in qs:
            details_val = ''
            if log.details:
                try:
                    details_val = json.dumps(log.details, ensure_ascii=False)
                except Exception:
                    details_val = str(log.details)

            data.append({
                'id':         log.id,
                'user_email': log.user.email if log.user else 'system',
                'action':     log.action,
                'details':    details_val,
                'ip_address': str(log.ip_address) if log.ip_address else '',
                'created_at': log.created_at.isoformat() if log.created_at else '',
            })
        return Response({'count': len(data), 'results': data})
    except Exception as e:
        logger.error(f"audit_logs error: {e}", exc_info=True)
        return Response({'count': 0, 'results': [], 'error': str(e)})
