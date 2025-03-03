document.addEventListener('DOMContentLoaded', function() {
    const orderForm = document.getElementById('orderForm');
    const addProductBtn = document.getElementById('addProductBtn');
    const productContainer = document.getElementById('productContainer');
    const orderAlert = document.getElementById('orderAlert');
    const alertMessage = document.getElementById('alertMessage');
    const notificationArea = document.querySelector('.notification-area');

    // Product catalog data (id, name, price)
    const products = [
        { id: 1, name: "Smartphone", price: 899.99 },
        { id: 2, name: "Laptop", price: 1299.99 },
        { id: 3, name: "Headphones", price: 249.99 },
        { id: 4, name: "Tablet", price: 499.99 },
        { id: 5, name: "Smartwatch", price: 299.99 }
    ];

    // API URL for orders
    const apiUrl = 'http://localhost:8085/api/orders';

    // Add product row with dropdown
    addProductBtn.addEventListener('click', function() {
        // Create product dropdown
        let productOptions = '<option value="">Select Product</option>';
        products.forEach(product => {
            productOptions += `<option value="${product.id}">${product.name} - $${product.price}</option>`;
        });

        // Create product div
        const productDiv = document.createElement('div');
        productDiv.className = 'product-row';

        productDiv.innerHTML = `
            <select class="product-select" required>
                ${productOptions}
            </select>
            <input type="number" class="product-quantity" min="1" required placeholder="Quantity">
            <button type="button" class="btn-remove">Remove</button>
        `;

        productContainer.appendChild(productDiv);

        // Add event listener to remove button
        productDiv.querySelector('.btn-remove').addEventListener('click', function() {
            productContainer.removeChild(productDiv);
        });
    });

    // Add initial product row
    addProductBtn.click();

    // Form submission
    orderForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const customerId = document.getElementById('customerId').value.trim();
        const destinationCountry = document.getElementById('destinationCountry').value;

        // Validate customer ID
        if (!customerId) {
            showAlert('Please enter a Customer ID', 'danger');
            scrollToNotification();
            return;
        }

        // Validate destination country
        if (!destinationCountry) {
            showAlert('Please select a destination country', 'danger');
            scrollToNotification();
            return;
        }

        // Get all product rows
        const productRows = document.querySelectorAll('.product-row');

        // Validate at least one product
        if (productRows.length === 0) {
            showAlert('Please add at least one product', 'danger');
            scrollToNotification();
            return;
        }

        // Create order items array
        const orderItems = [];
        let isValid = true;

        productRows.forEach(row => {
            const productId = row.querySelector('.product-select').value;
            const quantity = row.querySelector('.product-quantity').value;

            if (!productId || !quantity) {
                isValid = false;
                return;
            }

            orderItems.push({
                productId: parseInt(productId),
                quantity: parseInt(quantity)
            });
        });

        // Validate all products have ID and quantity
        if (!isValid) {
            showAlert('Please fill in all product details', 'danger');
            scrollToNotification();
            return;
        }

        // Create order object
        const order = {
            customerId: customerId,
            destinationCountry: destinationCountry,
            orderItems: orderItems
        };

        submitOrder(order);
    });

    function submitOrder(order) {
        console.log('Submitting order:', order);
        showAlert('Processing your order...', 'success', false);
        scrollToNotification();

        fetch(apiUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(order)
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok: ' + response.statusText);
            }
            return response.json();
        })
        .then(data => {
            console.log('Order created successfully:', data);

            // Show persistent success notification with order ID
            showAlert(`Order #${data.orderId} submitted successfully! View details in Order Status`, 'success', true);
            scrollToNotification();

            // Reset the form
            resetForm();
        })
        .catch(error => {
            console.error('Error submitting order:', error);
            showAlert('Error: ' + error.message, 'danger', true);
            scrollToNotification();
        });
    }

    // Function to scroll to the notification area
    function scrollToNotification() {
        // Scroll to the top of the page smoothly
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    }

    // Reset form after successful submission
    function resetForm() {
        document.getElementById('customerId').value = '';
        document.getElementById('destinationCountry').value = '';

        // Remove all product rows except the first one
        const productRows = document.querySelectorAll('.product-row');
        for (let i = 1; i < productRows.length; i++) {
            productContainer.removeChild(productRows[i]);
        }

        // Reset the first product row
        const firstRow = document.querySelector('.product-row');
        if (firstRow) {
            firstRow.querySelector('.product-select').value = '';
            firstRow.querySelector('.product-quantity').value = '';
        }
    }

    // Show alert message - if persistent is true, doesn't auto-dismiss
    function showAlert(message, type, persistent = false) {
        // Clear any existing timeout
        if (window.alertTimeout) {
            clearTimeout(window.alertTimeout);
        }

        alertMessage.textContent = message;
        orderAlert.className = `alert alert-${type}`;

        // Only auto-hide for non-persistent alerts
        if (!persistent) {
            window.alertTimeout = setTimeout(dismissAlert, 3000);
        }
    }

    // Global function to dismiss the alert
    window.dismissAlert = function() {
        orderAlert.className = 'alert hidden';
    };
});