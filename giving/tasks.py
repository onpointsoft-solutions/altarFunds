from celery import shared_task
from django.utils import timezone
from .models import ChurchDisbursement, GivingTransaction
from .paystack_disbursement import PaystackDisbursementService
import logging

logger = logging.getLogger(__name__)


@shared_task
def schedule_church_disbursement(transaction_id):
    """Schedule disbursement for a giving transaction"""
    try:
        logger.info(f"Scheduling disbursement for transaction {transaction_id}")
        
        # Get the giving transaction
        try:
            transaction = GivingTransaction.objects.get(id=transaction_id)
        except GivingTransaction.DoesNotExist:
            logger.error(f"Transaction {transaction_id} not found")
            return
        
        # Check if disbursement already exists
        if hasattr(transaction, 'disbursement'):
            logger.info(f"Disbursement already exists for transaction {transaction_id}")
            return
        
        # Wait a short period to ensure payment is fully processed
        # This allows for any potential chargebacks or issues
        from datetime import timedelta
        disbursement_time = timezone.now() + timedelta(minutes=5)
        
        # Schedule the actual disbursement
        process_church_disbursement.apply_async(
            args=[transaction_id],
            eta=disbursement_time
        )
        
        logger.info(f"Scheduled disbursement for transaction {transaction_id} at {disbursement_time}")
        
    except Exception as e:
        logger.error(f"Error scheduling disbursement for transaction {transaction_id}: {str(e)}")


@shared_task
def process_church_disbursement(transaction_id):
    """Process disbursement for a giving transaction"""
    try:
        logger.info(f"Processing disbursement for transaction {transaction_id}")
        
        # Get the giving transaction
        try:
            transaction = GivingTransaction.objects.get(id=transaction_id)
        except GivingTransaction.DoesNotExist:
            logger.error(f"Transaction {transaction_id} not found")
            return
        
        # Check if disbursement already exists
        if hasattr(transaction, 'disbursement'):
            logger.info(f"Disbursement already exists for transaction {transaction_id}")
            return
        
        # Initiate disbursement
        result = PaystackDisbursementService.disburse_to_church(transaction)
        
        if result['success']:
            logger.info(f"Successfully initiated disbursement for transaction {transaction_id}")
        else:
            logger.error(f"Failed to initiate disbursement for transaction {transaction_id}: {result['error']}")
            
    except Exception as e:
        logger.error(f"Error processing disbursement for transaction {transaction_id}: {str(e)}")


@shared_task
def retry_disbursement(disbursement_id):
    """Retry a failed disbursement"""
    try:
        logger.info(f"Retrying disbursement {disbursement_id}")
        
        # Get the disbursement
        try:
            disbursement = ChurchDisbursement.objects.get(id=disbursement_id)
        except ChurchDisbursement.DoesNotExist:
            logger.error(f"Disbursement {disbursement_id} not found")
            return
        
        # Check if it can be retried
        if not disbursement.can_retry:
            logger.warning(f"Disbursement {disbursement_id} cannot be retried")
            return
        
        # Reset status to processing
        disbursement.status = 'processing'
        disbursement.save()
        
        # Retry the disbursement
        result = PaystackDisbursementService.disburse_to_church(disbursement.giving_transaction)
        
        if result['success']:
            logger.info(f"Successfully retried disbursement {disbursement_id}")
        else:
            logger.error(f"Failed to retry disbursement {disbursement_id}: {result['error']}")
            
    except Exception as e:
        logger.error(f"Error retrying disbursement {disbursement_id}: {str(e)}")


@shared_task
def cleanup_old_disbursements():
    """Clean up old failed disbursements that have exceeded retry limit"""
    try:
        logger.info("Starting cleanup of old disbursements")
        
        # Find disbursements that failed and exceeded retry limit
        cutoff_date = timezone.now() - timezone.timedelta(days=30)
        
        old_failed_disbursements = ChurchDisbursement.objects.filter(
            status='failed',
            retry_count__gte=models.F('max_retries'),
            created_at__lt=cutoff_date
        )
        
        count = old_failed_disbursements.count()
        old_failed_disbursements.update(status='archived')
        
        logger.info(f"Archived {count} old failed disbursements")
        
    except Exception as e:
        logger.error(f"Error cleaning up old disbursements: {str(e)}")


@shared_task
def send_disbursement_reminders():
    """Send reminders for pending disbursements"""
    try:
        logger.info("Sending disbursement reminders")
        
        # Find disbursements pending for more than 24 hours
        cutoff_time = timezone.now() - timezone.timedelta(hours=24)
        
        pending_disbursements = ChurchDisbursement.objects.filter(
            status__in=['pending', 'processing'],
            created_at__lt=cutoff_time
        )
        
        for disbursement in pending_disbursements:
            # Send notification to church admin
            from common.services import NotificationService
            
            message = f"Payment disbursement of KES {disbursement.amount} is still pending. Status: {disbursement.get_status_display()}"
            
            NotificationService.send_notification(
                user=disbursement.church.senior_pastor or disbursement.giving_transaction.member.user,
                title="Pending Disbursement Reminder",
                message=message,
                notification_type='disbursement_reminder'
            )
        
        logger.info(f"Sent reminders for {pending_disbursements.count()} pending disbursements")
        
    except Exception as e:
        logger.error(f"Error sending disbursement reminders: {str(e)}")


# Schedule periodic tasks
from celery.schedules import crontab
from celery import current_app

@current_app.on_after_finalize.connect
def setup_periodic_tasks(sender, **kwargs):
    # Run cleanup daily at 2 AM
    sender.add_periodic_task(
        crontab(hour=2, minute=0),
        cleanup_old_disbursements.s(),
        name='cleanup-old-disbursements'
    )
    
    # Run reminders every 6 hours
    sender.add_periodic_task(
        crontab(minute=0, hour='*/6'),
        send_disbursement_reminders.s(),
        name='send-disbursement-reminders'
    )
