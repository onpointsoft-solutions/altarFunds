import re
from django.core.exceptions import ValidationError

def validate_phone_number(value):
    """Simple phone number validator"""
    if not value:
        return value
    
    # Remove any non-digit characters
    digits = re.sub(r'\D', '', value)
    
    if len(digits) < 10:
        raise ValidationError('Phone number must have at least 10 digits')
    
    return value

def validate_paybill_number(value):
    """Simple paybill number validator"""
    if not value:
        return value
    
    if not value.isdigit() or len(value) < 5:
        raise ValidationError('Paybill number must be at least 5 digits')
    
    return value

def validate_till_number(value):
    """Simple till number validator"""
    if not value:
        return value
    
    if not value.isdigit() or len(value) < 5:
        raise ValidationError('Till number must be at least 5 digits')
    
    return value

def validate_bank_account_number(value):
    """Simple bank account number validator"""
    if not value:
        return value
    
    if not value.isdigit() or len(value) < 6:
        raise ValidationError('Bank account number must be at least 6 digits')
    
    return value

def validate_amount(value):
    """Simple amount validator"""
    if not value:
        raise ValidationError('Amount is required')
    
    try:
        amount = float(value)
        if amount <= 0:
            raise ValidationError('Amount must be greater than 0')
        return amount
    except (ValueError, TypeError):
        raise ValidationError('Enter a valid amount')

def validate_id_number(value):
    """Simple ID number validator"""
    if not value:
        return value
    
    if not value.isdigit() or len(value) < 5:
        raise ValidationError('ID number must be at least 5 digits')
    
    return value

def validate_church_name(value):
    """Simple church name validator"""
    if not value or len(value.strip()) < 2:
        raise ValidationError('Church name must be at least 2 characters long')
    
    return value.strip()

def validate_church_code(value):
    """Simple church code validator"""
    if not value or len(value.strip()) < 2:
        raise ValidationError('Church code must be at least 2 characters long')
    
    return value.strip().upper()

def validate_category_name(value):
    """Simple category name validator"""
    if not value or len(value.strip()) < 2:
        raise ValidationError('Category name must be at least 2 characters long')
    
    return value.strip()
