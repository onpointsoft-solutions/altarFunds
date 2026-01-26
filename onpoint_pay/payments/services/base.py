from abc import ABC, abstractmethod


class PaymentService(ABC):
    """Base class for payment services"""
    
    @abstractmethod
    def initiate_payment(self, *args, **kwargs):
        """Initiate a payment"""
        pass
    
    @abstractmethod
    def check_status(self, *args, **kwargs):
        """Check payment status"""
        pass
    
    @abstractmethod
    def refund(self, *args, **kwargs):
        """Refund a payment"""
        pass
