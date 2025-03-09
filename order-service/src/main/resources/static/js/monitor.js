document.addEventListener('DOMContentLoaded', function() {
    const ordersTableBody = document.getElementById('ordersTableBody');
    const statusFilter = document.getElementById('statusFilter');
    const searchInput = document.getElementById('searchInput');
    const refreshBtn = document.getElementById('refreshBtn');
    const pagination = document.getElementById('pagination');
    const monitorAlert = document.getElementById('monitorAlert');
    const alertMessage = document.getElementById('alertMessage');

    // Constants
    const ITEMS_PER_PAGE = 10;
    const API_URL = 'http://localhost:8091/api/order-status/all';

    const STATUS_TO_CODE = {
        'ORDER_RECEIVED': 1,
        'ORDER_PROCESS': 2,
        'SHIPPED_SUCCESSFUL': 3,
        'NO_SHIP_AVAILABLE': 4,
        'ORDER_FAILED': 5,
        'NO_SHIP_AVAILABLE_DLT': 6
    };

    let orders = [];
    let filteredOrders = [];
    let currentPage = 1;

    function fetchOrders() {
        showAlert('Loading orders...', 'alert-success');

        fetch(API_URL, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                orders = data.map(item => ({
                    id: item.id,
                    order_id: item.orderId,
                    customer_id: item.customerId || 'N/A',
                    status_code: STATUS_TO_CODE[item.status] || 1,
                    status: item.status,
                    created_at: item.createdAt
                }));

                applyFilters();
                showAlert('Orders loaded successfully!', 'alert-success');
                setTimeout(() => dismissAlert(), 3000);
            })
            .catch(error => {
                console.error('Error fetching orders:', error);
                showAlert('Error loading orders. Please try again.', 'alert-danger');
            });
    }

    // Apply filters and search
    function applyFilters() {
        const statusValue = statusFilter.value;
        const searchValue = searchInput.value.toLowerCase();

        filteredOrders = orders.filter(order => {
            // Status filter
            if (statusValue !== 'all' && order.status_code !== parseInt(statusValue)) {
                return false;
            }

            // Search filter - only for customer_id
            if (searchValue) {
                const customerId = String(order.customer_id).toLowerCase();
                return customerId.includes(searchValue);
            }

            return true;
        });

        currentPage = 1;
        renderTable();
        renderPagination();
    }

    // Render table with current page data
    function renderTable() {
        ordersTableBody.innerHTML = '';

        const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        const endIndex = startIndex + ITEMS_PER_PAGE;
        const pageData = filteredOrders.slice(startIndex, endIndex);

        if (pageData.length === 0) {
            ordersTableBody.innerHTML = `
                <tr>
                    <td colspan="5" style="text-align: center; padding: 20px;">
                        No orders found matching your criteria.
                    </td>
                </tr>
            `;
            return;
        }

        pageData.forEach((order, index) => {
            const row = document.createElement('tr');
            const displayIndex = startIndex + index + 1; // Calculate the continuous index

            row.innerHTML = `
                <td>${displayIndex}</td>
                <td>${order.order_id}</td>
                <td>${order.customer_id}</td>
                <td>
                    <span class="status-badge status-${order.status_code}">
                        ${order.status}
                    </span>
                </td>
                <td>${formatDate(order.created_at)}</td>
            `;

            ordersTableBody.appendChild(row);
        });
    }

    // Render pagination
    function renderPagination() {
        pagination.innerHTML = '';

        const totalPages = Math.ceil(filteredOrders.length / ITEMS_PER_PAGE);

        if (totalPages <= 1) {
            return;
        }

        // Previous button
        const prevBtn = document.createElement('button');
        prevBtn.textContent = '←';
        prevBtn.disabled = currentPage === 1;
        prevBtn.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage--;
                renderTable();
                renderPagination();
            }
        });
        pagination.appendChild(prevBtn);

        // Page buttons
        let startPage = Math.max(1, currentPage - 2);
        let endPage = Math.min(totalPages, startPage + 4);

        if (endPage - startPage < 4) {
            startPage = Math.max(1, endPage - 4);
        }

        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.textContent = i;
            pageBtn.classList.toggle('active', i === currentPage);
            pageBtn.addEventListener('click', () => {
                currentPage = i;
                renderTable();
                renderPagination();
            });
            pagination.appendChild(pageBtn);
        }

        // Next button
        const nextBtn = document.createElement('button');
        nextBtn.textContent = '→';
        nextBtn.disabled = currentPage === totalPages;
        nextBtn.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                renderTable();
                renderPagination();
            }
        });
        pagination.appendChild(nextBtn);
    }

    function formatDate(dateString) {
        const date = new Date(dateString);
        const options = {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        };

        return date.toLocaleDateString('en-US', options);
    }

    function showAlert(message, className) {
        monitorAlert.className = 'alert ' + className;
        alertMessage.textContent = message;
        monitorAlert.classList.remove('hidden');
    }

    function dismissAlert() {
        monitorAlert.classList.add('hidden');
    }

    statusFilter.addEventListener('change', applyFilters);
    searchInput.addEventListener('input', applyFilters);
    refreshBtn.addEventListener('click', fetchOrders);

    fetchOrders();
});