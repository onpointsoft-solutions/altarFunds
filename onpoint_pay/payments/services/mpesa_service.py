import base64
import requests
from datetime import datetime
from django.conf import settings
from django.utils import timezone
from .base import PaymentService


class MpesaService(PaymentService):
    """M-Pesa Daraja API integration service"""
    
    def __init__(self):
        self.consumer_key = settings.MPESA_CONSUMER_KEY
        self.consumer_secret = settings.MPESA_CONSUMER_SECRET
        self.passkey = settings.MPESA_PASSKEY
        self.shortcode = settings.MPESA_SHORTCODE
        
        # Use sandbox URLs in development
        if getattr(settings, 'DEBUG', False):
            self.base_url = "https://sandbox.safaricom.co.ke"
        else:
            self.base_url = "https://api.safaricom.co.ke"
            
        self.callback_url = settings.MPESA_CALLBACK_URL
        self.confirmation_url = settings.MPESA_CONFIRMATION_URL
        self.validation_url = settings.MPESA_VALIDATION_URL
    
    def get_oauth_token(self):
        """Get OAuth access token from M-Pesa"""
        url = f"{self.base_url}/oauth/v1/generate?grant_type=client_credentials"
        
        # Create basic auth header
        auth_string = f"{self.consumer_key}:{self.consumer_secret}"
        auth_bytes = auth_string.encode('ascii')
        auth_b64 = base64.b64encode(auth_bytes).decode('ascii')
        
        headers = {
            'Authorization': f'Basic {auth_b64}',
            'Content-Type': 'application/json'
        }
        
        try:
            response = requests.get(url, headers=headers)
            response.raise_for_status()
            return response.json().get('access_token')
        except requests.exceptions.RequestException as e:
            raise Exception(f"Failed to get OAuth token: {str(e)}")
    
    def initiate_stk_push(self, phone_number, amount, reference, callback_url=None, description=""):
        """Initiate M-Pesa STK Push payment"""
        try:
            # Get OAuth token
            access_token = self.get_oauth_token()
            
            # Format phone number (remove +254 if present and ensure 254 prefix)
            if phone_number.startswith('+254'):
                phone_number = phone_number[1:]
            elif not phone_number.startswith('254'):
                phone_number = f"254{phone_number[-9:]}"  # Take last 9 digits
            
            # Generate timestamp
            timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
            
            # Generate password
            password_str = f"{self.shortcode}{self.passkey}{timestamp}"
            password_bytes = password_str.encode('ascii')
            password_b64 = base64.b64encode(password_bytes).decode('ascii')
            
            # Prepare request
            url = f"{self.base_url}/mpesa/stkpush/v1/processrequest"
            headers = {
                'Authorization': f'Bearer {access_token}',
                'Content-Type': 'application/json'
            }
            
            payload = {
                'BusinessShortCode': self.shortcode,
                'Password': password_b64,
                'Timestamp': timestamp,
                'TransactionType': 'CustomerPayBillOnline',
                'Amount': int(amount),
                'PartyA': phone_number,
                'PartyB': self.shortcode,
                'PhoneNumber': phone_number,
                'CallBackURL': callback_url or self.callback_url,
                'AccountReference': reference[:12],  # Max 12 characters
                'TransactionDesc': description[:13],  # Max 13 characters
                'CallBackMetadata': {
                    'Reference': reference,
                    'Initiator': 'OnPoint Pay'
                }
            }
            
            response = requests.post(url, json=payload, headers=headers)
            response.raise_for_status()
            
            data = response.json()
            
            # Check if request was successful
            if data.get('ResponseCode') == '0':
                return {
                    'success': True,
                    'checkout_request_id': data.get('CheckoutRequestID'),
                    'merchant_request_id': data.get('MerchantRequestID'),
                    'customer_message': data.get('CustomerMessage', 'Please enter your PIN to complete payment')
                }
            else:
                return {
                    'success': False,
                    'error': data.get('errorMessage', 'Failed to initiate payment'),
                    'response_code': data.get('ResponseCode')
                }
                
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': f"Network error: {str(e)}"
            }
        except Exception as e:
            return {
                'success': False,
                'error': f"Payment initiation failed: {str(e)}"
            }
    
    def check_transaction_status(self, checkout_request_id):
        """Check the status of an STK Push transaction"""
        try:
            # Get OAuth token
            access_token = self.get_oauth_token()
            
            # Generate timestamp and password
            timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
            password_str = f"{self.shortcode}{self.passkey}{timestamp}"
            password_bytes = password_str.encode('ascii')
            password_b64 = base64.b64encode(password_bytes).decode('ascii')
            
            # Prepare request
            url = f"{self.base_url}/mpesa/stkpushquery/v1/query"
            headers = {
                'Authorization': f'Bearer {access_token}',
                'Content-Type': 'application/json'
            }
            
            payload = {
                'BusinessShortCode': self.shortcode,
                'Password': password_b64,
                'Timestamp': timestamp,
                'CheckoutRequestID': checkout_request_id
            }
            
            response = requests.post(url, json=payload, headers=headers)
            response.raise_for_status()
            
            data = response.json()
            
            # Parse response
            result_code = data.get('ResultCode')
            if result_code == '0':
                return {
                    'success': True,
                    'result_code': result_code,
                    'mpesa_receipt': data.get('Result', {}).get('MpesaReceiptNumber'),
                    'transaction_date': data.get('Result', {}).get('TransactionDate'),
                    'amount': data.get('Result', {}).get('Amount'),
                    'phone_number': data.get('Result', {}).get('PhoneNumber')
                }
            else:
                return {
                    'success': False,
                    'result_code': result_code,
                    'error': data.get('ResultDesc', 'Transaction failed or is pending')
                }
                
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': f"Network error: {str(e)}"
            }
        except Exception as e:
            return {
                'success': False,
                'error': f"Status check failed: {str(e)}"
            }
    
    def register_c2b_urls(self):
        """Register C2B confirmation and validation URLs"""
        try:
            # Get OAuth token
            access_token = self.get_oauth_token()
            
            # Prepare request
            url = f"{self.base_url}/mpesa/c2b/v1/registerurl"
            headers = {
                'Authorization': f'Bearer {access_token}',
                'Content-Type': 'application/json'
            }
            
            payload = {
                'ShortCode': self.shortcode,
                'ResponseType': 'Completed',
                'ConfirmationURL': self.confirmation_url,
                'ValidationURL': self.validation_url
            }
            
            response = requests.post(url, json=payload, headers=headers)
            response.raise_for_status()
            
            data = response.json()
            
            if data.get('ResponseCode') == '0':
                return {
                    'success': True,
                    'message': 'C2B URLs registered successfully'
                }
            else:
                return {
                    'success': False,
                    'error': data.get('errorMessage', 'Failed to register C2B URLs')
                }
                
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': f"Network error: {str(e)}"
            }
        except Exception as e:
            return {
                'success': False,
                'error': f"C2B URL registration failed: {str(e)}"
            }
    
    def simulate_c2b_payment(self, phone_number, amount, reference):
        """Simulate C2B payment for testing"""
        try:
            # Get OAuth token
            access_token = self.get_oauth_token()
            
            # Format phone number
            if phone_number.startswith('+254'):
                phone_number = phone_number[1:]
            elif not phone_number.startswith('254'):
                phone_number = f"254{phone_number[-9:]}"
            
            # Prepare request
            url = f"{self.base_url}/mpesa/c2b/v1/simulate"
            headers = {
                'Authorization': f'Bearer {access_token}',
                'Content-Type': 'application/json'
            }
            
            payload = {
                'ShortCode': self.shortcode,
                'CommandID': 'CustomerPayBillOnline',
                'Amount': int(amount),
                'Msisdn': phone_number,
                'BillRefNumber': reference[:12]
            }
            
            response = requests.post(url, json=payload, headers=headers)
            response.raise_for_status()
            
            data = response.json()
            
            if data.get('ResponseCode') == '0':
                return {
                    'success': True,
                    'transaction_id': data.get('TransactionID'),
                    'conversation_id': data.get('ConversationID'),
                    'amount': data.get('Amount')
                }
            else:
                return {
                    'success': False,
                    'error': data.get('errorMessage', 'Simulation failed')
                }
                
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': f"Network error: {str(e)}"
            }
        except Exception as e:
            return {
                'success': False,
                'error': f"C2B simulation failed: {str(e)}"
            }
    
    def reverse_transaction(self, transaction_id, amount, remark=""):
        """Reverse an M-Pesa transaction"""
        try:
            # Get OAuth token
            access_token = self.get_oauth_token()
            
            # Generate timestamp and password
            timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
            password_str = f"{self.shortcode}{self.passkey}{timestamp}"
            password_bytes = password_str.encode('ascii')
            password_b64 = base64.b64encode(password_bytes).decode('ascii')
            
            # Prepare request
            url = f"{self.base_url}/mpesa/reversal/v1/request"
            headers = {
                'Authorization': f'Bearer {access_token}',
                'Content-Type': 'application/json'
            }
            
            payload = {
                'Initiator': 'OnPoint Pay',
                'SecurityCredential': password_b64,
                'CommandID': 'TransactionReversal',
                'TransactionID': transaction_id,
                'Amount': int(amount),
                'ReceiverParty': self.shortcode,
                'RecieverIdentifierType': '4',
                'ResultURL': self.callback_url,
                'QueueTimeOutURL': self.callback_url,
                'Remark': remark[:50],  # Max 50 characters
                'Occasion': 'Reversal'
            }
            
            response = requests.post(url, json=payload, headers=headers)
            response.raise_for_status()
            
            data = response.json()
            
            if data.get('ResponseCode') == '0':
                return {
                    'success': True,
                    'conversation_id': data.get('ConversationID'),
                    'originator_conversation_id': data.get('OriginatorConversationID')
                }
            else:
                return {
                    'success': False,
                    'error': data.get('errorMessage', 'Reversal failed')
                }
                
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': f"Network error: {str(e)}"
            }
        except Exception as e:
            return {
                'success': False,
                'error': f"Transaction reversal failed: {str(e)}"
            }
