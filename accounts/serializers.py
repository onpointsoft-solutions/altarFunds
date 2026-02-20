from rest_framework import serializers
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from .models import User, Member, UserSession, PasswordResetToken, ChurchJoinRequest
from common.serializers import BaseSerializer, UserSerializer
from common.validators import validate_phone_number, validate_id_number
from common.exceptions import AltarFundsException
import logging

logger = logging.getLogger(__name__)

class UserRegistrationSerializer(serializers.ModelSerializer):
    """User registration serializer"""
    
    password = serializers.CharField(
        write_only=True,
        min_length=12,
        validators=[validate_password]
    )
    password_confirm = serializers.CharField(write_only=True)
    
    class Meta:
        model = User
        fields = [
            'email', 'first_name', 'last_name', 'phone_number',
            'password', 'password_confirm', 'role', 'church'
        ]
        extra_kwargs = {
            'password': {'write_only': True},
            'church': {'required': False}
        }
    
    def to_internal_value(self, data):
        """Override to handle church_data without validation"""
        # Extract church_data before validation
        church_data = data.pop('church_data', None)
        
        # If church_data is a list with one item, extract the dict
        if isinstance(church_data, list) and len(church_data) > 0:
            church_data = church_data[0]
        
        # Call parent to validate other fields
        validated = super().to_internal_value(data)
        
        # Add church_data back without validation
        if church_data and isinstance(church_data, dict):
            validated['church_data'] = church_data
        
        return validated
    
    def validate_email(self, value):
        """Validate email is not already registered"""
        if User.objects.filter(email=value.lower()).exists():
            raise serializers.ValidationError("Email already registered")
        return value.lower()
    
    def validate_phone_number(self, value):
        """Validate and format phone number"""
        return validate_phone_number(value)
    
    def validate(self, attrs):
        """Validate password confirmation"""
        if attrs['password'] != attrs['password_confirm']:
            raise serializers.ValidationError("Passwords don't match")
        
        return attrs
    
    def create(self, validated_data):
        """Create user with encrypted password and optional church"""
        logger.info("\n--- SERIALIZER: Starting create method ---")
        logger.info("validated_data keys: %s", list(validated_data.keys()))
        
        validated_data.pop('password_confirm')
        church = validated_data.pop('church', None)
        church_data = validated_data.pop('church_data', None)
        
        logger.info("church_data present: %s", church_data is not None)
        logger.info("church_data type: %s", type(church_data))
        logger.info("church_data value: %s", church_data)
        logger.info("church_data bool evaluation: %s", bool(church_data))
        
        if church_data:
            logger.info("church_data keys: %s", list(church_data.keys()))
            logger.info("church_data values: %s", {k: v for k, v in church_data.items() if k not in ['logo']})
        else:
            logger.warning("church_data is falsy - not creating church")

        password = validated_data.pop('password')

        # Create church if church_data is provided
        if church_data:
            from churches.models import Church
            
            try:
                logger.info("SERIALIZER: Attempting to create church...")
                # Set default status for new churches
                church_data['status'] = 'pending'
                church_data['is_verified'] = False
                
                # Create the church
                logger.info("SERIALIZER: Creating church with data: %s", {k: v for k, v in church_data.items() if k not in ['logo']})
                church = Church.objects.create(**church_data)
                logger.info("SERIALIZER: Church created with ID: %s", church.pk)
                
                # Generate church code
                church.generate_church_code()
                logger.info("SERIALIZER: Church code generated: %s", church.church_code)
                
                # Verify church was saved
                if not church.pk:
                    logger.error("SERIALIZER: Church pk is None after creation!")
                    raise serializers.ValidationError("Failed to create church. Please try again.")
                
                logger.info("SERIALIZER: Church created successfully: %s (ID: %s)", church.name, church.pk)
                    
            except Exception as e:
                logger.error("SERIALIZER: Church creation exception: %s", str(e))
                raise serializers.ValidationError(f"Church creation failed: {str(e)}")
        
        # Only create user if church was created successfully (or no church required)
        if church_data and not church:
            raise serializers.ValidationError("Cannot register admin without a valid church.")
        
        user = User.objects.create_user(password=password, church=church, **validated_data)
        user.save()

        # Create member profile
        if church:
            Member.objects.create(user=user, church=church)

        return user


class UserLoginSerializer(serializers.Serializer):
    """User login serializer"""
    
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)
    
    def validate(self, attrs):
        """Validate login credentials"""
        email = attrs.get('email').lower()
        password = attrs.get('password')
        
        user = authenticate(username=email, password=password)
        
        if not user:
            raise serializers.ValidationError("Invalid credentials")
        
        if not user.is_active:
            raise serializers.ValidationError("Account is suspended")
        
        attrs['user'] = user
        return attrs


class UserProfileSerializer(UserSerializer):
    """Extended user profile serializer"""
    
    member_profile = serializers.SerializerMethodField()
    church_name = serializers.CharField(source='church.name', read_only=True)
    church_info = serializers.SerializerMethodField()
    role_display = serializers.CharField(source='get_role_display', read_only=True)
    permissions = serializers.SerializerMethodField()
    
    class Meta(UserSerializer.Meta):
        fields = UserSerializer.Meta.fields + [
            'date_of_birth', 'gender', 'address_line1', 'address_line2',
            'city', 'county', 'postal_code', 'profile_picture',
            'church_name', 'church_info', 'member_profile', 'permissions',
            'email_notifications', 'sms_notifications', 'push_notifications',
            'is_phone_verified', 'is_email_verified'
        ]
    
    def get_member_profile(self, obj):
        """Get member profile information"""
        try:
            member = obj.member_profile
            return MemberProfileSerializer(member).data
        except Member.DoesNotExist:
            return None
    
    def get_church_info(self, obj):
        """Get church information"""
        if obj.church:
            from churches.serializers import ChurchListSerializer
            return ChurchListSerializer(obj.church).data
        return None
    
    def get_permissions(self, obj):
        """Get user's permissions"""
        return obj.get_church_permissions()


class MemberProfileSerializer(serializers.ModelSerializer):
    """Member profile serializer"""
    
    user = UserSerializer(read_only=True)
    departments = serializers.StringRelatedField(many=True, read_only=True)
    small_group = serializers.StringRelatedField(read_only=True)
    
    class Meta:
        model = Member
        fields = [
            'user', 'membership_number', 'membership_status', 'membership_date',
            'id_number', 'kra_pin', 'occupation', 'employer', 'marital_status',
            'spouse_name', 'emergency_contact_name', 'emergency_contact_phone',
            'departments', 'small_group', 'is_tithe_payer',
            'preferred_giving_method', 'monthly_giving_goal'
        ]
        read_only_fields = ['membership_number', 'user']


class MemberUpdateSerializer(serializers.ModelSerializer):
    """Member profile update serializer"""
    
    class Meta:
        model = Member
        fields = [
            'occupation', 'employer', 'marital_status', 'spouse_name',
            'emergency_contact_name', 'emergency_contact_phone',
            'is_tithe_payer', 'preferred_giving_method', 'monthly_giving_goal'
        ]
    
    def validate_emergency_contact_phone(self, value):
        """Validate emergency contact phone"""
        if value:
            return validate_phone_number(value)
        return value


class UserUpdateSerializer(serializers.ModelSerializer):
    """User profile update serializer"""
    
    member_profile = MemberUpdateSerializer(required=False)
    
    class Meta:
        model = User
        fields = [
            'first_name', 'last_name', 'phone_number', 'date_of_birth',
            'gender', 'address_line1', 'address_line2', 'city', 'county',
            'postal_code', 'profile_picture', 'email_notifications',
            'sms_notifications', 'push_notifications', 'member_profile'
        ]
    
    def validate_phone_number(self, value):
        """Validate phone number"""
        return validate_phone_number(value)
    
    def update(self, instance, validated_data):
        """Update user and member profile"""
        member_data = validated_data.pop('member_profile', None)
        
        # Update user
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        
        # Update member profile
        if member_data and hasattr(instance, 'member_profile'):
            member = instance.member_profile
            for attr, value in member_data.items():
                setattr(member, attr, value)
            member.save()
        
        return instance


class PasswordChangeSerializer(serializers.Serializer):
    """Password change serializer"""
    
    current_password = serializers.CharField(write_only=True)
    new_password = serializers.CharField(
        write_only=True,
        min_length=12,
        validators=[validate_password]
    )
    new_password_confirm = serializers.CharField(write_only=True)
    
    def validate_current_password(self, value):
        """Validate current password"""
        if not self.context['request'].user.check_password(value):
            raise serializers.ValidationError("Current password is incorrect")
        return value
    
    def validate(self, attrs):
        """Validate password confirmation"""
        if attrs['new_password'] != attrs['new_password_confirm']:
            raise serializers.ValidationError("New passwords don't match")
        return attrs
    
    def save(self):
        """Change user password"""
        user = self.context['request'].user
        user.set_password(self.validated_data['new_password'])
        user.save()
        return user


class PasswordResetRequestSerializer(serializers.Serializer):
    """Password reset request serializer"""
    
    email = serializers.EmailField()
    
    def validate_email(self, value):
        """Validate email exists"""
        if not User.objects.filter(email=value.lower()).exists():
            raise serializers.ValidationError("Email not found")
        return value.lower()


class PasswordResetConfirmSerializer(serializers.Serializer):
    """Password reset confirmation serializer"""
    
    token = serializers.UUIDField()
    new_password = serializers.CharField(
        write_only=True,
        min_length=12,
        validators=[validate_password]
    )
    new_password_confirm = serializers.CharField(write_only=True)
    
    def validate(self, attrs):
        """Validate token and password confirmation"""
        if attrs['new_password'] != attrs['new_password_confirm']:
            raise serializers.ValidationError("Passwords don't match")
        
        # Validate token
        try:
            token_obj = PasswordResetToken.objects.get(
                token=attrs['token'],
                is_used=False
            )
            if not token_obj.is_valid():
                raise serializers.ValidationError("Token has expired")
        except PasswordResetToken.DoesNotExist:
            raise serializers.ValidationError("Invalid token")
        
        attrs['token_obj'] = token_obj
        return attrs
    
    def save(self):
        """Reset password"""
        token_obj = self.validated_data['token_obj']
        user = token_obj.user
        
        user.set_password(self.validated_data['new_password'])
        user.save()
        
        # Mark token as used
        token_obj.is_used = True
        token_obj.used_at = timezone.now()
        token_obj.save()
        
        return user


class UserSessionSerializer(serializers.ModelSerializer):
    """User session serializer"""
    
    user_email = serializers.CharField(source='user.email', read_only=True)
    duration = serializers.SerializerMethodField()
    
    class Meta:
        model = UserSession
        fields = [
            'id', 'user_email', 'ip_address', 'user_agent', 'device_info',
            'is_active', 'created_at', 'last_activity', 'duration'
        ]
    
    def get_duration(self, obj):
        """Get session duration"""
        from django.utils import timezone
        duration = timezone.now() - obj.created_at
        return str(duration).split('.')[0]


class UserListSerializer(serializers.ModelSerializer):
    """User list serializer for admin views"""
    
    full_name = serializers.CharField(read_only=True)
    church_name = serializers.CharField(source='church.name', read_only=True)
    role_display = serializers.CharField(source='get_role_display', read_only=True)
    is_active_colored = serializers.SerializerMethodField()
    
    class Meta:
        model = User
        fields = [
            'id', 'email', 'first_name', 'last_name', 'full_name', 'phone_number', 
            'role', 'role_display', 'church_name', 'is_active', 'is_phone_verified', 
            'is_email_verified', 'is_active_colored', 'date_joined', 'last_login'
        ]
    
    def get_is_active_colored(self, obj):
        """Get colored status"""
        if obj.is_active and not obj.is_suspended:
            return {'status': 'active', 'color': 'green'}
        elif obj.is_suspended:
            return {'status': 'suspended', 'color': 'orange'}
        else:
            return {'status': 'inactive', 'color': 'red'}


class StaffRegistrationSerializer(serializers.ModelSerializer):
    """Serializer for denomination admin to register pastor/treasurer/usher"""
    
    password = serializers.CharField(
        write_only=True,
        min_length=12,
        validators=[validate_password]
    )
    password_confirm = serializers.CharField(write_only=True)
    
    class Meta:
        model = User
        fields = [
            'email', 'first_name', 'last_name', 'phone_number',
            'password', 'password_confirm', 'role', 'church'
        ]
        extra_kwargs = {
            'password': {'write_only': True},
        }
    
    def get_fields(self):
        """Override to set church queryset dynamically"""
        fields = super().get_fields()
        request = self.context.get('request')
        
        from churches.models import Church
        
        if request and hasattr(request, 'user'):
            user = request.user
            if user.role == 'denomination_admin' and user.church:
                # Only allow churches in the same denomination
                fields['church'].queryset = Church.objects.filter(
                    denomination=user.church.denomination
                )
            elif user.role == 'system_admin':
                fields['church'].queryset = Church.objects.all()
            else:
                fields['church'].queryset = Church.objects.none()
        else:
            fields['church'].queryset = Church.objects.none()
        
        return fields
    
    def validate_email(self, value):
        """Validate email is not already registered"""
        if User.objects.filter(email=value.lower()).exists():
            raise serializers.ValidationError("Email already registered")
        return value.lower()
    
    def validate_phone_number(self, value):
        """Validate and format phone number"""
        if value:
            return validate_phone_number(value)
        return value
    
    def validate_role(self, value):
        """Validate role is pastor, treasurer, or usher"""
        request = self.context.get('request')
        if request and request.user.role == 'denomination_admin':
            if value not in ['pastor', 'treasurer', 'usher']:
                raise serializers.ValidationError(
                    "Denomination admins can only register pastors, treasurers, and ushers"
                )
        return value
    
    def validate(self, attrs):
        """Validate password confirmation and permissions"""
        if attrs['password'] != attrs['password_confirm']:
            raise serializers.ValidationError("Passwords don't match")
        
        # Validate church access
        request = self.context.get('request')
        if request and request.user.role == 'denomination_admin':
            church = attrs.get('church')
            if church and church.denomination != request.user.church.denomination:
                raise serializers.ValidationError(
                    "You can only register staff for churches in your denomination"
                )
        
        return attrs
    
    def create(self, validated_data):
        """Create staff user"""
        validated_data.pop('password_confirm')
        password = validated_data.pop('password')
        church = validated_data.get('church')
        
        user = User.objects.create_user(password=password, **validated_data)
        user.save()
        
        # Create member profile
        Member.objects.create(user=user, church=church)
        
        return user


class ChurchJoinRequestSerializer(serializers.ModelSerializer):
    """Serializer for church join requests"""
    
    user = serializers.SerializerMethodField()
    reviewed_by = serializers.SerializerMethodField()
    
    class Meta:
        model = ChurchJoinRequest
        fields = [
            'id', 'user', 'church', 'church_code', 'status', 'message',
            'reviewed_by', 'reviewed_at', 'rejection_reason', 'created_at'
        ]
        read_only_fields = ['id', 'user', 'reviewed_by', 'reviewed_at', 'created_at']
    
    def get_user(self, obj):
        """Get user details"""
        if obj.user:
            return {
                'id': obj.user.id,
                'email': obj.user.email,
                'first_name': obj.user.first_name,
                'last_name': obj.user.last_name,
                'phone_number': obj.user.phone_number if hasattr(obj.user, 'phone_number') else None
            }
        return None
    
    def get_reviewed_by(self, obj):
        """Get reviewer name"""
        if obj.reviewed_by:
            return {
                'first_name': obj.reviewed_by.first_name,
                'last_name': obj.reviewed_by.last_name
            }
        return None
