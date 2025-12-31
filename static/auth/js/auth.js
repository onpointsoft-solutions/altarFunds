// JavaScript for authentication page functionality
document.addEventListener('DOMContentLoaded', function() {
    // Password visibility toggle for login form
    const togglePassword = document.getElementById('togglePassword');
    const passwordInput = document.querySelector('input[name="password"]');
    
    if (togglePassword && passwordInput) {
        togglePassword.addEventListener('click', function() {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            
            // Toggle icon
            const icon = this.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-eye');
                icon.classList.toggle('fa-eye-slash');
            }
        });
    }
    
    // Password visibility toggle for registration form
    const togglePasswordConfirm = document.getElementById('togglePasswordConfirm');
    const passwordConfirmInput = document.querySelector('input[name="password_confirm"]');
    
    if (togglePasswordConfirm && passwordConfirmInput) {
        togglePasswordConfirm.addEventListener('click', function() {
            const type = passwordConfirmInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordConfirmInput.setAttribute('type', type);
            
            // Toggle icon
            const icon = this.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-eye');
                icon.classList.toggle('fa-eye-slash');
            }
        });
    }
    
    // Form validation feedback
    const forms = document.querySelectorAll('.auth-form');
    forms.forEach(form => {
        form.addEventListener('submit', function(event) {
            const requiredFields = form.querySelectorAll('[required]');
            let isValid = true;
            
            requiredFields.forEach(field => {
                if (!field.value.trim()) {
                    isValid = false;
                    field.classList.add('is-invalid');
                } else {
                    field.classList.remove('is-invalid');
                }
            });
            
            if (!isValid) {
                event.preventDefault();
                // Show general error message
                const alertDiv = document.createElement('div');
                alertDiv.className = 'alert alert-danger alert-dismissible fade show';
                alertDiv.innerHTML = `
                    Please fill in all required fields.
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                `;
                form.insertBefore(alertDiv, form.firstChild);
            }
        });
    });
    
    // Remove invalid class on input
    const inputs = document.querySelectorAll('.form-control');
    inputs.forEach(input => {
        input.addEventListener('input', function() {
            if (this.value.trim()) {
                this.classList.remove('is-invalid');
            }
        });
    });
});
