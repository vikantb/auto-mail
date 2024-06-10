async function fetchEmailAddresses() {
    try {
        const url = 'http://localhost:8080/auto-mail/fetch-email-addresses';
        const requestBody = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json' // Set the content type to JSON
            },
            body: JSON.stringify({ query: '' }) // Add a request body if needed
        };
        const response = await fetch(url, requestBody);
        console.log('Fetched email addresses:', response);
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        const emailAddresses = await response.json();

        const emailListContainer = document.querySelector('.email-list');
        emailAddresses.forEach(email => {
            const emailItem = document.createElement('div');
            emailItem.classList.add('email-item');
            emailItem.innerHTML = `
                    <input type="checkbox" id="${email}" value="${email}">
                    <label for="${email}">${email}</label>
                `;
            emailListContainer.appendChild(emailItem);
        });
    } catch (error) {
        console.error('Error fetching email addresses:', error);
    }
}

function deleteSelectedEmails() {
    // Find all checkboxes
    const checkboxes = document.querySelectorAll('.email-item input[type="checkbox"]:checked');
    // Get the email addresses of selected checkboxes
    const selectedEmails = Array.from(checkboxes).map(checkbox => checkbox.value);
    // You can now send the selected email addresses to the server for deletion
    console.log('Selected Email Addresses:', selectedEmails);
}

window.onload = function () {
    fetchUserDetails();
};

async function fetchUserDetails() {
    // Fetch user details from the API
    fetch('http://localhost:8080/auto-mail/user/details')
        .then(response => response.json())
        .then(user => {
            const userImage = document.querySelector('.user-image');
            userImage.setAttribute("src", user.attributes.picture);
            const userName = document.querySelector('.user-name');
            userName.innerHTML = user.attributes.name;
        })
        .catch(error => {
            console.error('Error fetching user details:', error);
        });
}