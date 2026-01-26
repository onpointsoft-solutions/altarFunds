import json
from django.http import JsonResponse, HttpResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from django.utils import timezone
from django.db import transaction
from ..models import Transaction, MpesaRequest, WebhookLog, AuditLog
from ..utils import get_client_ip, deliver_webhook


@csrf_exempt
@require_http_methods(["POST"])
def mpesa_callback(request):
    """Handle M-Pesa STK Push callback"""
    try:
        # Parse callback data
        callback_data = json.loads(request.body)
        
        # Get callback metadata
        body = callback_data.get('Body', {})
        stk_callback = body.get('stkCallback', {})
        
        # Extract key information
        merchant_request_id = stk_callback.get('MerchantRequestID', '')
        checkout_request_id = stk_callback.get('CheckoutRequestID', '')
        result_code = stk_callback.get('ResultCode', '')
        result_desc = stk_callback.get('ResultDesc', '')
        
        # Find the corresponding M-Pesa request
        try:
            mpesa_request = MpesaRequest.objects.get(
                checkout_request_id=checkout_request_id
            )
            transaction = mpesa_request.transaction
        except MpesaRequest.DoesNotExist:
            return JsonResponse({
                'ResultCode': 1,
                'ResultDesc': 'Transaction not found'
            })
        
        # Update callback received time
        mpesa_request.callback_received_at = timezone.now()
        
        # Process result
        if result_code == '0':  # Success
            # Extract payment details
            callback_metadata = stk_callback.get('CallbackMetadata', {})
            metadata_items = callback_metadata.get('Item', [])
            
            # Extract specific values
            mpesa_receipt = ''
            amount = 0
            phone_number = ''
            transaction_date = None
            
            for item in metadata_items:
                name = item.get('Name', '')
                value = item.get('Value', '')
                
                if name == 'MpesaReceiptNumber':
                    mpesa_receipt = value
                elif name == 'Amount':
                    amount = float(value)
                elif name == 'PhoneNumber':
                    phone_number = value
                elif name == 'TransactionDate':
                    transaction_date = timezone.datetime.strptime(value, '%Y%m%d%H%M%S')
            
            # Update M-Pesa request
            mpesa_request.mpesa_receipt = mpesa_receipt
            mpesa_request.transaction_date = transaction_date
            mpesa_request.result_code = result_code
            mpesa_request.result_description = result_desc
            mpesa_request.save(update_fields=[
                'callback_received_at', 'mpesa_receipt', 
                'transaction_date', 'result_code', 'result_description'
            ])
            
            # Update transaction
            with transaction.atomic():
                transaction.status = 'completed'
                transaction.completed_at = timezone.now()
                transaction.save(update_fields=['status', 'completed_at'])
                
                # Log success
                AuditLog.objects.create(
                    merchant=transaction.merchant,
                    action='mpesa_payment_completed',
                    resource_type='transaction',
                    resource_id=str(transaction.id),
                    ip_address=get_client_ip(request),
                    user_agent=request.META.get('HTTP_USER_AGENT', ''),
                    success=True,
                    new_values={
                        'mpesa_receipt': mpesa_receipt,
                        'amount': amount,
                        'phone_number': phone_number
                    }
                )
                
                # Trigger webhook to merchant
                if transaction.callback_url:
                    webhook_payload = {
                        'event': 'payment.completed',
                        'transaction': {
                            'reference': transaction.reference,
                            'amount': transaction.amount,
                            'currency': transaction.currency,
                            'status': 'completed',
                            'payment_method': 'mpesa',
                            'mpesa_receipt': mpesa_receipt,
                            'customer_phone': phone_number,
                            'completed_at': transaction.completed_at.isoformat()
                        }
                    }
                    
                    webhook_log = WebhookLog.objects.create(
                        merchant=transaction.merchant,
                        transaction=transaction,
                        webhook_url=transaction.callback_url,
                        event_type='payment.completed',
                        payload=webhook_payload
                    )
                    
                    # Deliver webhook asynchronously
                    deliver_webhook(webhook_log)
        
        else:  # Failed
            # Update M-Pesa request
            mpesa_request.result_code = result_code
            mpesa_request.result_description = result_desc
            mpesa_request.save(update_fields=[
                'callback_received_at', 'result_code', 'result_description'
            ])
            
            # Update transaction
            transaction.status = 'failed'
            transaction.save(update_fields=['status'])
            
            # Log failure
            AuditLog.objects.create(
                merchant=transaction.merchant,
                action='mpesa_payment_failed',
                resource_type='transaction',
                resource_id=str(transaction.id),
                ip_address=get_client_ip(request),
                user_agent=request.META.get('HTTP_USER_AGENT', ''),
                success=False,
                error_message=result_desc
            )
            
            # Trigger webhook to merchant
            if transaction.callback_url:
                webhook_payload = {
                    'event': 'payment.failed',
                    'transaction': {
                        'reference': transaction.reference,
                        'amount': transaction.amount,
                        'currency': transaction.currency,
                        'status': 'failed',
                        'payment_method': 'mpesa',
                        'error': result_desc,
                        'result_code': result_code
                    }
                }
                
                webhook_log = WebhookLog.objects.create(
                    merchant=transaction.merchant,
                    transaction=transaction,
                    webhook_url=transaction.callback_url,
                    event_type='payment.failed',
                    payload=webhook_payload
                )
                
                # Deliver webhook asynchronously
                deliver_webhook(webhook_log)
        
        return JsonResponse({
            'ResultCode': 0,
            'ResultDesc': 'Success'
        })
        
    except json.JSONDecodeError:
        return JsonResponse({
            'ResultCode': 1,
            'ResultDesc': 'Invalid JSON'
        }, status=400)
    except Exception as e:
        return JsonResponse({
            'ResultCode': 1,
            'ResultDesc': 'Internal server error'
        }, status=500)


@csrf_exempt
@require_http_methods(["POST"])
def mpesa_confirmation(request):
    """Handle M-Pesa C2B confirmation"""
    try:
        # Parse confirmation data
        confirmation_data = json.loads(request.body)
        
        # Extract transaction details
        transaction_type = confirmation_data.get('TransactionType', '')
        trans_id = confirmation_data.get('TransID', '')
        trans_time = confirmation_data.get('TransTime', '')
        amount = confirmation_data.get('TransAmount', '0')
        business_short_code = confirmation_data.get('BusinessShortCode', '')
        bill_ref_number = confirmation_data.get('BillRefNumber', '')
        invoice_number = confirmation_data.get('InvoiceNumber', '')
        org_account_balance = confirmation_data.get('OrgAccountBalance', '0')
        third_party_trans_id = confirmation_data.get('ThirdPartyTransID', '')
        msisdn = confirmation_data.get('MSISDN', '')
        first_party_name = confirmation_data.get('FirstName', '')
        middle_name = confirmation_data.get('MiddleName', '')
        last_name = confirmation_data.get('LastName', '')
        
        # Log the confirmation
        AuditLog.objects.create(
            action='mpesa_c2b_confirmation',
            resource_type='transaction',
            resource_id=trans_id,
            ip_address=get_client_ip(request),
            user_agent=request.META.get('HTTP_USER_AGENT', ''),
            success=True,
            new_values={
                'trans_id': trans_id,
                'amount': amount,
                'msisdn': msisdn,
                'bill_ref_number': bill_ref_number
            }
        )
        
        # Here you would typically:
        # 1. Find the merchant by business_short_code
        # 2. Create or update a transaction record
        # 3. Trigger webhook to the merchant
        
        return JsonResponse({
            'ResultCode': 0,
            'ResultDesc': 'Success'
        })
        
    except json.JSONDecodeError:
        return JsonResponse({
            'ResultCode': 1,
            'ResultDesc': 'Invalid JSON'
        }, status=400)
    except Exception as e:
        return JsonResponse({
            'ResultCode': 1,
            'ResultDesc': 'Internal server error'
        }, status=500)


@csrf_exempt
@require_http_methods(["POST"])
def mpesa_validation(request):
    """Handle M-Pesa C2B validation"""
    try:
        # Parse validation data
        validation_data = json.loads(request.body)
        
        # Extract transaction details
        transaction_type = validation_data.get('TransactionType', '')
        trans_id = validation_data.get('TransID', '')
        trans_time = validation_data.get('TransTime', '')
        amount = validation_data.get('TransAmount', '0')
        business_short_code = validation_data.get('BusinessShortCode', '')
        bill_ref_number = validation_data.get('BillRefNumber', '')
        invoice_number = validation_data.get('InvoiceNumber', '')
        org_account_balance = validation_data.get('OrgAccountBalance', '0')
        third_party_trans_id = validation_data.get('ThirdPartyTransID', '')
        msisdn = validation_data.get('MSISDN', '')
        first_party_name = validation_data.get('FirstName', '')
        middle_name = validation_data.get('MiddleName', '')
        last_name = validation_data.get('LastName', '')
        
        # Log the validation
        AuditLog.objects.create(
            action='mpesa_c2b_validation',
            resource_type='transaction',
            resource_id=trans_id,
            ip_address=get_client_ip(request),
            user_agent=request.META.get('HTTP_USER_AGENT', ''),
            success=True,
            new_values={
                'trans_id': trans_id,
                'amount': amount,
                'msisdn': msisdn,
                'bill_ref_number': bill_ref_number
            }
        )
        
        # Here you would typically:
        # 1. Validate the transaction against your business rules
        # 2. Return appropriate response code
        # 3. Log the validation result
        
        return JsonResponse({
            'ResultCode': 0,
            'ResultDesc': 'Success'
        })
        
    except json.JSONDecodeError:
        return JsonResponse({
            'ResultCode': 1,
            'ResultDesc': 'Invalid JSON'
        }, status=400)
    except Exception as e:
        return JsonResponse({
            'ResultCode': 1,
            'ResultDesc': 'Internal server error'
        }, status=500)
