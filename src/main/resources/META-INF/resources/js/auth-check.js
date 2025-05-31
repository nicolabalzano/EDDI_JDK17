/**
 * Authentication check utility for E.D.D.I
 * Checks if user is authenticated and redirects to login if not
 */

$(document).ready(function() {
    // Check if user is authenticated when page loads
    checkAuthentication();
    
    // Add logout functionality if logout button exists
    bindLogoutHandler();
});

function checkAuthentication() {
    $.ajax({
        url: '/logout/userAuthenticated',
        method: 'GET',
        success: function(response) {
            // If response is "false", user is not authenticated
            if (response === 'false' || response === false) {
                redirectToLogin();
            }
            // If authenticated, continue normally
        },
        error: function(xhr) {
            // If endpoint returns 401 or 403, redirect to login
            if (xhr.status === 401 || xhr.status === 403) {
                redirectToLogin();
            }
            // For other errors, log but don't redirect
            console.warn('Authentication check failed:', xhr.status);
        }
    });
}

function redirectToLogin() {
    // Store current page URL for redirect after login
    const currentUrl = window.location.pathname + window.location.search;
    if (currentUrl !== '/auth/login') {
        sessionStorage.setItem('redirectAfterLogin', currentUrl);
    }
    
    // Redirect to login page
    window.location.href = '/auth/login';
}

function bindLogoutHandler() {
    // Look for logout button or link
    const $logoutBtn = $('#logoutBtn, .logout-btn, [data-action="logout"]');
    
    if ($logoutBtn.length > 0) {
        $logoutBtn.on('click', function(e) {
            e.preventDefault();
            performLogout();
        });
    }
}

function performLogout() {
    $.ajax({
        url: '/logout',
        method: 'POST',
        success: function() {
            // Clear any stored redirect URL
            sessionStorage.removeItem('redirectAfterLogin');
            
            // Redirect to login page
            window.location.href = '/auth/login';
        },
        error: function(xhr) {
            console.error('Logout failed:', xhr.status);
            // Still redirect to login even if logout fails
            window.location.href = '/auth/login';
        }
    });
}

// Export functions for use in other scripts
window.EDDIAuth = {
    checkAuthentication: checkAuthentication,
    redirectToLogin: redirectToLogin,
    performLogout: performLogout
};
