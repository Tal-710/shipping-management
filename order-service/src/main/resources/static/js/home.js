document.addEventListener('DOMContentLoaded', function() {
    const orderForm = document.getElementById('orderForm');
    const addProductBtn = document.getElementById('addProductBtn');
    const productContainer = document.getElementById('productContainer');
    const orderAlert = document.getElementById('orderAlert');
    const alertMessage = document.getElementById('alertMessage');
    const notificationArea = document.querySelector('.notification-area');

    const products = [
        { id: 1, name: "Smartphone"},
        { id: 2, name: "Laptop"},
        { id: 3, name: "Headphones"},
        { id: 4, name: "Tablet"},
        { id: 5, name: "Smartwatch"},
        { id: 6, name: "Monitor"}
    ];

    const apiUrl = 'http://localhost:8085/api/orders';

    addProductBtn.addEventListener('click', function() {
        let productOptions = '<option value="">Select Product</option>';
        products.forEach(product => {
            productOptions += `<option value="${product.id}">${product.name}</option>`;
        });

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

        productDiv.querySelector('.btn-remove').addEventListener('click', function() {
            productContainer.removeChild(productDiv);
        });
    });

    addProductBtn.click();

    orderForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const customerId = document.getElementById('customerId').value.trim();
        const destinationCountry = document.getElementById('destinationCountry').value;

        if (!customerId) {
            showAlert('Please enter a Customer ID', 'danger');
            scrollToNotification();
            return;
        }

        if (!destinationCountry) {
            showAlert('Please select a destination country', 'danger');
            scrollToNotification();
            return;
        }

        const productRows = document.querySelectorAll('.product-row');

        if (productRows.length === 0) {
            showAlert('Please add at least one product', 'danger');
            scrollToNotification();
            return;
        }

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

        if (!isValid) {
            showAlert('Please fill in all product details', 'danger');
            scrollToNotification();
            return;
        }

        const order = {
            customerId: customerId,
            destinationCountry: destinationCountry,
            orderItems: orderItems
        };

        showAlert('Processing your order...', 'success', false);
        scrollToNotification();

        setTimeout(() => {
            showAlert('Order submitted successfully! View details in Order Status', 'success', true);
            resetForm();
        }, 1500);

        try {
            fetch(apiUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(order)
            });
            console.log('Order sent to backend');
        } catch (e) {
            console.log('Error sending order (ignored)');
        }
    });

    function scrollToNotification() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    }

    function resetForm() {
        document.getElementById('customerId').value = '';
        document.getElementById('destinationCountry').value = '';

        const productRows = document.querySelectorAll('.product-row');
        for (let i = 1; i < productRows.length; i++) {
            productContainer.removeChild(productRows[i]);
        }

        const firstRow = document.querySelector('.product-row');
        if (firstRow) {
            firstRow.querySelector('.product-select').value = '';
            firstRow.querySelector('.product-quantity').value = '';
        }
    }

    function showAlert(message, type, persistent = false) {
        if (window.alertTimeout) {
            clearTimeout(window.alertTimeout);
        }

        alertMessage.textContent = message;
        orderAlert.className = `alert alert-${type}`;
        orderAlert.style.display = 'block';

        if (!persistent) {
            window.alertTimeout = setTimeout(dismissAlert, 3000);
        }
    }

    window.dismissAlert = function() {
        orderAlert.className = 'alert hidden';
    };
});