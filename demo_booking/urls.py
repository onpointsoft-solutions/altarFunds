from django.urls import path
from .views import book_demo, list_demo_bookings

app_name = 'demo_booking'

urlpatterns = [
    path('',      book_demo,           name='book_demo'),
    path('list/', list_demo_bookings,  name='list_demo_bookings'),
]
