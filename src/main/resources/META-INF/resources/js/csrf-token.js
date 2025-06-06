// CSRF Token Handler
document.addEventListener('DOMContentLoaded', function() {
    const csrfTokenField = document.getElementById('csrfToken');
    if (csrfTokenField) {
        // Recupera il token CSRF e lo inserisce nel campo nascosto
        fetch('/auth/csrf-token')
            .then(response => response.json())
            .then(data => {
                csrfTokenField.value = data.csrfToken;
            })
            .catch(error => {
                console.error('Error fetching CSRF token:', error);
            });
    }
});
