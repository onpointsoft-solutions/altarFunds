/**
 * AltarFunds Custom Admin JavaScript
 * Interactive features for the modern admin interface
 */

// Global variables
let refreshInterval;
let chartInstances = {};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    initializeAdmin();
});

function initializeAdmin() {
    // Initialize tooltips
    initializeTooltips();
    
    // Initialize data tables
    initializeDataTables();
    
    // Set up auto-refresh
    setupAutoRefresh();
    
    // Initialize search functionality
    initializeSearch();
    
    // Initialize notifications
    initializeNotifications();
    
    console.log('AltarFunds Admin initialized');
}

/**
 * Initialize tooltips
 */
function initializeTooltips() {
    const tooltipElements = document.querySelectorAll('[data-tooltip]');
    
    tooltipElements.forEach(element => {
        element.addEventListener('mouseenter', function(e) {
            showTooltip(e.target, e.target.getAttribute('data-tooltip'));
        });
        
        element.addEventListener('mouseleave', function() {
            hideTooltip();
        });
    });
}

function showTooltip(element, text) {
    // Remove existing tooltip
    hideTooltip();
    
    const tooltip = document.createElement('div');
    tooltip.className = 'custom-tooltip';
    tooltip.textContent = text;
    tooltip.style.cssText = `
        position: absolute;
        background: #1e293b;
        color: white;
        padding: 0.5rem 0.75rem;
        border-radius: 6px;
        font-size: 0.875rem;
        z-index: 1000;
        pointer-events: none;
        opacity: 0;
        transition: opacity 0.2s ease;
    `;
    
    document.body.appendChild(tooltip);
    
    // Position tooltip
    const rect = element.getBoundingClientRect();
    tooltip.style.left = rect.left + (rect.width / 2) - (tooltip.offsetWidth / 2) + 'px';
    tooltip.style.top = rect.top - tooltip.offsetHeight - 8 + 'px';
    
    // Show tooltip
    setTimeout(() => {
        tooltip.style.opacity = '1';
    }, 10);
}

function hideTooltip() {
    const tooltip = document.querySelector('.custom-tooltip');
    if (tooltip) {
        tooltip.remove();
    }
}

/**
 * Initialize data tables with sorting and filtering
 */
function initializeDataTables() {
    const tables = document.querySelectorAll('.table');
    
    tables.forEach(table => {
        // Add sorting functionality
        const headers = table.querySelectorAll('th[data-sortable]');
        
        headers.forEach(header => {
            header.style.cursor = 'pointer';
            header.addEventListener('click', function() {
                sortTable(table, header);
            });
            
            // Add sort indicator
            const indicator = document.createElement('i');
            indicator.className = 'fas fa-sort sort-indicator';
            indicator.style.cssText = `
                margin-left: 0.5rem;
                opacity: 0.3;
                font-size: 0.75rem;
            `;
            header.appendChild(indicator);
        });
    });
}

function sortTable(table, header) {
    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    const columnIndex = Array.from(header.parentNode.children).indexOf(header);
    const isAscending = !header.classList.contains('sort-asc');
    
    // Remove existing sort classes
    table.querySelectorAll('th').forEach(th => {
        th.classList.remove('sort-asc', 'sort-desc');
        const indicator = th.querySelector('.sort-indicator');
        if (indicator) {
            indicator.className = 'fas fa-sort sort-indicator';
        }
    });
    
    // Add current sort class
    header.classList.add(isAscending ? 'sort-asc' : 'sort-desc');
    const indicator = header.querySelector('.sort-indicator');
    if (indicator) {
        indicator.className = `fas fa-sort-${isAscending ? 'up' : 'down'} sort-indicator`;
        indicator.style.opacity = '1';
    }
    
    // Sort rows
    rows.sort((a, b) => {
        const aValue = a.children[columnIndex].textContent.trim();
        const bValue = b.children[columnIndex].textContent.trim();
        
        // Try to parse as numbers
        const aNum = parseFloat(aValue.replace(/[^0-9.-]/g, ''));
        const bNum = parseFloat(bValue.replace(/[^0-9.-]/g, ''));
        
        if (!isNaN(aNum) && !isNaN(bNum)) {
            return isAscending ? aNum - bNum : bNum - aNum;
        }
        
        // Sort as strings
        return isAscending ? 
            aValue.localeCompare(bValue) : 
            bValue.localeCompare(aValue);
    });
    
    // Reorder rows
    rows.forEach(row => tbody.appendChild(row));
}

/**
 * Setup auto-refresh for dashboard data
 */
function setupAutoRefresh() {
    const refreshToggle = document.getElementById('auto-refresh-toggle');
    const refreshInterval = document.getElementById('refresh-interval');
    
    if (refreshToggle) {
        refreshToggle.addEventListener('change', function() {
            if (this.checked) {
                const interval = parseInt(refreshInterval.value) * 1000;
                startAutoRefresh(interval);
            } else {
                stopAutoRefresh();
            }
        });
    }
}

function startAutoRefresh(interval) {
    stopAutoRefresh(); // Clear existing interval
    
    refreshInterval = setInterval(() => {
        refreshDashboardData();
    }, interval);
    
    showNotification('Auto-refresh enabled', 'success');
}

function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

function refreshDashboardData() {
    // Show loading state
    const loadingElements = document.querySelectorAll('.stat-card h3');
    loadingElements.forEach(el => {
        el.style.opacity = '0.5';
    });
    
    // Fetch fresh data
    fetch('/admin/api-stats/')
        .then(response => response.json())
        .then(data => {
            updateDashboardStats(data);
            showNotification('Dashboard refreshed', 'success');
        })
        .catch(error => {
            console.error('Error refreshing dashboard:', error);
            showNotification('Failed to refresh dashboard', 'error');
        })
        .finally(() => {
            // Restore opacity
            loadingElements.forEach(el => {
                el.style.opacity = '1';
            });
        });
}

function updateDashboardStats(data) {
    // Update stat cards
    const statCards = {
        'total_churches': data.overview.total_churches,
        'total_users': data.overview.total_users,
        'total_giving': data.monthly_stats.total_giving,
        'total_transactions': data.overview.total_transactions
    };
    
    Object.entries(statCards).forEach(([key, value]) => {
        const element = document.querySelector(`[data-stat="${key}"]`);
        if (element) {
            animateValue(element, parseInt(element.textContent) || 0, value, 1000);
        }
    });
}

/**
 * Animate number changes
 */
function animateValue(element, start, end, duration) {
    const range = end - start;
    const increment = range / (duration / 16);
    let current = start;
    
    const timer = setInterval(() => {
        current += increment;
        
        if ((increment > 0 && current >= end) || (increment < 0 && current <= end)) {
            element.textContent = formatNumber(end);
            clearInterval(timer);
        } else {
            element.textContent = formatNumber(Math.round(current));
        }
    }, 16);
}

function formatNumber(num) {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
}

/**
 * Initialize search functionality
 */
function initializeSearch() {
    const searchInput = document.getElementById('admin-search');
    const searchResults = document.getElementById('search-results');
    
    if (searchInput) {
        let searchTimeout;
        
        searchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            const query = this.value.trim();
            
            if (query.length < 2) {
                hideSearchResults();
                return;
            }
            
            searchTimeout = setTimeout(() => {
                performSearch(query);
            }, 300);
        });
        
        // Hide results when clicking outside
        document.addEventListener('click', function(e) {
            if (!searchInput.contains(e.target) && !searchResults.contains(e.target)) {
                hideSearchResults();
            }
        });
    }
}

function performSearch(query) {
    fetch(`/admin/search/?q=${encodeURIComponent(query)}`)
        .then(response => response.json())
        .then(data => {
            showSearchResults(data.results);
        })
        .catch(error => {
            console.error('Search error:', error);
        });
}

function showSearchResults(results) {
    const searchResults = document.getElementById('search-results');
    if (!searchResults) return;
    
    if (results.length === 0) {
        searchResults.innerHTML = '<div class="search-no-results">No results found</div>';
    } else {
        searchResults.innerHTML = results.map(result => `
            <div class="search-result-item">
                <div class="search-result-title">${result.title}</div>
                <div class="search-result-type">${result.type}</div>
                <a href="${result.url}" class="search-result-link">View →</a>
            </div>
        `).join('');
    }
    
    searchResults.style.display = 'block';
}

function hideSearchResults() {
    const searchResults = document.getElementById('search-results');
    if (searchResults) {
        searchResults.style.display = 'none';
    }
}

/**
 * Initialize notifications
 */
function initializeNotifications() {
    // Check for notifications periodically
    setInterval(checkNotifications, 60000); // Check every minute
}

function checkNotifications() {
    fetch('/admin/notifications/')
        .then(response => response.json())
        .then(data => {
            if (data.notifications && data.notifications.length > 0) {
                showNotificationBadge(data.notifications.length);
                data.notifications.forEach(notification => {
                    showNotification(notification.message, notification.type);
                });
            }
        })
        .catch(error => {
            console.error('Error checking notifications:', error);
        });
}

function showNotificationBadge(count) {
    const badge = document.getElementById('notification-badge');
    if (badge) {
        badge.textContent = count > 99 ? '99+' : count;
        badge.style.display = count > 0 ? 'block' : 'none';
    }
}

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `admin-notification admin-notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <span class="notification-message">${message}</span>
            <button class="notification-close" onclick="this.parentElement.parentElement.remove()">×</button>
        </div>
    `;
    
    // Add styles
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        padding: 1rem;
        z-index: 9999;
        min-width: 300px;
        border-left: 4px solid ${getNotificationColor(type)};
        animation: slideInRight 0.3s ease-out;
    `;
    
    document.body.appendChild(notification);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (notification.parentElement) {
            notification.style.animation = 'slideOutRight 0.3s ease-out';
            setTimeout(() => notification.remove(), 300);
        }
    }, 5000);
}

function getNotificationColor(type) {
    const colors = {
        'success': '#10b981',
        'error': '#ef4444',
        'warning': '#f59e0b',
        'info': '#06b6d4'
    };
    return colors[type] || colors.info;
}

/**
 * Export functionality
 */
function exportData(format = 'csv') {
    const currentUrl = window.location.href;
    const exportUrl = `${currentUrl}?export=${format}`;
    
    window.open(exportUrl, '_blank');
    showNotification(`Exporting data as ${format.toUpperCase()}...`, 'info');
}

/**
 * Keyboard shortcuts
 */
document.addEventListener('keydown', function(e) {
    // Ctrl/Cmd + K for search
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        const searchInput = document.getElementById('admin-search');
        if (searchInput) {
            searchInput.focus();
        }
    }
    
    // Escape to close modals/search
    if (e.key === 'Escape') {
        hideSearchResults();
        closeAllModals();
    }
});

function closeAllModals() {
    document.querySelectorAll('.modal').forEach(modal => {
        modal.style.display = 'none';
    });
}

/**
 * Utility functions
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Add CSS animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
    
    .admin-notification {
        animation: slideInRight 0.3s ease-out;
    }
    
    .notification-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    
    .notification-close {
        background: none;
        border: none;
        font-size: 1.2rem;
        cursor: pointer;
        opacity: 0.5;
        padding: 0;
        margin-left: 1rem;
    }
    
    .notification-close:hover {
        opacity: 1;
    }
    
    .search-result-item {
        padding: 0.75rem;
        border-bottom: 1px solid #e2e8f0;
        cursor: pointer;
    }
    
    .search-result-item:hover {
        background: #f8fafc;
    }
    
    .search-result-title {
        font-weight: 500;
        color: #1e293b;
    }
    
    .search-result-type {
        font-size: 0.875rem;
        color: #64748b;
        margin: 0.25rem 0;
    }
    
    .search-result-link {
        color: #2563eb;
        text-decoration: none;
        font-size: 0.875rem;
    }
    
    .search-result-link:hover {
        text-decoration: underline;
    }
    
    .search-no-results {
        padding: 1rem;
        text-align: center;
        color: #64748b;
    }
    
    .sort-indicator {
        transition: opacity 0.2s ease;
    }
    
    th[data-sortable]:hover {
        background: #f1f5f9;
    }
`;

document.head.appendChild(style);
