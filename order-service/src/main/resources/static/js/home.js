document.addEventListener('DOMContentLoaded', function() {
    const orderForm = document.getElementById('orderForm');
    const addProductBtn = document.getElementById('addProductBtn');
    const productContainer = document.getElementById('productContainer');
    const orderAlert = document.getElementById('orderAlert');
    const orderStatus = document.getElementById('orderStatus');
    const orderDetails = document.getElementById('orderDetails');

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

    // Check if we're on mobile
    const isMobile = window.innerWidth < 600;

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

        const customerId = document.getElementById('customerId').value;
        const destinationCountry = document.getElementById('destinationCountry').value;

        // Validate destination country
        if (!destinationCountry) {
            showAlert('Please select a destination country', 'danger');
            return;
        }

        // Get all product rows
        const productRows = document.querySelectorAll('.product-row');

        // Validate at least one product
        if (productRows.length === 0) {
            showAlert('Please add at least one product', 'danger');
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
            return;
        }

        // Create order object
        const order = {
            customerId: customerId || null,
            destinationCountry: destinationCountry,
            orderItems: orderItems
        };

        // Send order to API
        submitOrder(order);
    });

    // Submit order to API
    function submitOrder(order) {
        console.log('Submitting order:', order);
        showAlert('Processing your order...', 'success');

        // Use fetch API to send order to backend
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
            showOrderStatus(data);
            simulateOrderProgress(data);
            showAlert(`Order #${data.orderId} created successfully!`, 'success');
        })
        .catch(error => {
            console.error('Error submitting order:', error);
            showAlert('Error: ' + error.message, 'danger');
        });
    }

    // Show alert message
    function showAlert(message, type) {
        orderAlert.textContent = message;
        orderAlert.className = `alert alert-${type}`;

        // Hide alert after 5 seconds
        setTimeout(() => {
            orderAlert.className = 'alert hidden';
        }, 5000);
    }

    // Show order status
    function showOrderStatus(order) {
        // Show order status card
        orderStatus.classList.remove('hidden');

        // Create order details HTML
        let detailsHtml = `
            <h3>Order #${order.orderId}</h3>
            <p><strong>Customer ID:</strong> ${order.customerId || 'N/A'}</p>
            <p><strong>Destination:</strong> ${order.destinationCountry}</p>
            <p><strong>Created At:</strong> ${new Date(order.createdAt).toLocaleString()}</p>
            <h4>Order Items:</h4>
            <div class="order-table-container">
                <table class="responsive-table">
                    <thead>
                        <tr>
                            <th>Product</th>
                            <th>Quantity</th>
                            <th>Price</th>
                            <th>Total</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        let orderTotal = 0;

        order.orderItems.forEach(item => {
            // Find product details from our local catalog
            const product = products.find(p => p.id === item.productId) ||
                            { name: `Product #${item.productId}`, price: 0 };

            const itemTotal = product.price * item.quantity;
            orderTotal += itemTotal;

            detailsHtml += `
                <tr>
                    <td>${product.name}</td>
                    <td>${item.quantity}</td>
                    <td>$${product.price.toFixed(2)}</td>
                    <td>$${itemTotal.toFixed(2)}</td>
                </tr>
            `;
        });

        detailsHtml += `
                    </tbody>
                    <tfoot>
                        <tr>
                            <td colspan="3" style="text-align: right;"><strong>Order Total:</strong></td>
                            <td><strong>$${orderTotal.toFixed(2)}</strong></td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        `;

        orderDetails.innerHTML = detailsHtml;

        // Scroll to order status
        orderStatus.scrollIntoView({ behavior: 'smooth' });
    }

    // Simulate order progress
    function simulateOrderProgress(order) {
        const steps = [
            document.getElementById('stepCreated'),
            document.getElementById('stepInventory'),
            document.getElementById('stepShip'),
            document.getElementById('stepShipped')
        ];

        // Step 1 is already active
        steps[0].classList.add('active');

        // Simulate inventory check
        setTimeout(() => {
            steps[1].classList.add('active');
            steps[0].classList.add('completed');
            showAlert('Inventory check completed', 'success');
        }, 2000);

        // Simulate ship assignment
        setTimeout(() => {
            steps[2].classList.add('active');
            steps[1].classList.add('completed');
            showAlert('Ship assigned to order', 'success');
        }, 4000);

        // Simulate shipped
        setTimeout(() => {
            steps[3].classList.add('active');
            steps[2].classList.add('completed');
            showAlert('Order has been shipped', 'success');
        }, 6000);
    }
});