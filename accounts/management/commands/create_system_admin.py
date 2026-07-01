"""
Management command: create or promote a system admin user.

Usage:
    # Create new system admin
    python manage.py create_system_admin --email admin@sanctum.co.ke --password StrongPass123!

    # Promote existing user to system_admin
    python manage.py create_system_admin --email existing@example.com --promote-only
"""
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction


class Command(BaseCommand):
    help = 'Create a new system admin user or promote an existing user to system_admin role'

    def add_arguments(self, parser):
        parser.add_argument('--email',    required=True, help='Email address')
        parser.add_argument('--password', default=None,  help='Password (required for new users)')
        parser.add_argument('--first-name', default='System', dest='first_name')
        parser.add_argument('--last-name',  default='Admin',  dest='last_name')
        parser.add_argument(
            '--promote-only', action='store_true',
            help='Only update role on an existing user — do not create a new one'
        )

    def handle(self, *args, **options):
        from accounts.models import User

        email      = options['email'].strip().lower()
        promote    = options['promote_only']
        password   = options.get('password')

        with transaction.atomic():
            try:
                user = User.objects.get(email=email)
                self.stdout.write(f"Found existing user: {user.email}  current role={user.role}")
            except User.DoesNotExist:
                if promote:
                    raise CommandError(f"User '{email}' not found. Remove --promote-only to create.")
                if not password:
                    raise CommandError("--password is required when creating a new user.")

                user = User.objects.create_user(
                    email      = email,
                    password   = password,
                    first_name = options['first_name'],
                    last_name  = options['last_name'],
                )
                self.stdout.write(f"Created new user: {user.email}")

            # Promote to system_admin
            user.role         = 'system_admin'
            user.is_staff     = True
            user.is_superuser = True
            user.is_active    = True
            user.is_email_verified = True
            if password and not promote:
                user.set_password(password)
            user.save()

        self.stdout.write(self.style.SUCCESS(
            f"\n✅  {user.email} is now system_admin  "
            f"(is_staff={user.is_staff}, is_superuser={user.is_superuser})\n"
            f"   Login with this account in the Sanctum desktop app.\n"
        ))
