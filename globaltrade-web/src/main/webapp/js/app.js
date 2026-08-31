/**
 * GlobalTrade Supply Chain Management System
 * Main Application View Controller and UI Router
 * Pure Vanilla JavaScript implementation. Zero external dependencies.
 */

(function () {
    'use strict';

    // Application UI State
    let activeView = null;

    // DOM Elements Cache
    const el = {
        toastContainer: document.getElementById('toast-container'),
        viewLogin: document.getElementById('view-login'),
        viewApp: document.getElementById('view-app'),
        loginForm: document.getElementById('form-login'),
        inputUsername: document.getElementById('input-username'),
        inputPassword: document.getElementById('input-password'),
        loginError: document.getElementById('login-error'),
        btnLoginSubmit: document.getElementById('btn-login-submit'),
        btnLogout: document.getElementById('btn-logout'),
        sidebarNav: document.getElementById('sidebar-nav'),
        sidebarPortalTag: document.getElementById('sidebar-portal-tag'),
        headerPortalTitle: document.getElementById('header-portal-title'),
        userDisplayName: document.getElementById('user-display-name'),
        userDisplayRole: document.getElementById('user-display-role'),
        userAvatarInitials: document.getElementById('user-avatar-initials'),
        mainContent: document.getElementById('main-content'),

        // Modals
        modalShipment: document.getElementById('modal-shipment'),
        modalDispatch: document.getElementById('modal-dispatch'),
        modalReplenish: document.getElementById('modal-replenish'),

        // Dispatch Form
        formDispatch: document.getElementById('form-dispatch'),
        dispatchShipmentId: document.getElementById('dispatch-shipment-id'),
        dispatchTrackingDisplay: document.getElementById('dispatch-tracking-display'),
        dispatchInventorySelect: document.getElementById('dispatch-inventory-select'),
        dispatchQuantityInput: document.getElementById('dispatch-quantity-input'),
        btnDispatchConfirm: document.getElementById('btn-dispatch-confirm'),

        // Replenish Form
        formReplenish: document.getElementById('form-replenish'),
        replenishItemId: document.getElementById('replenish-item-id'),
        replenishItemDisplay: document.getElementById('replenish-item-display'),
        replenishQuantityInput: document.getElementById('replenish-quantity-input'),
        btnReplenishConfirm: document.getElementById('btn-replenish-confirm')
    };

    // =========================================================================
    // Utilities & Toast Notifications
    // =========================================================================

    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const msgSpan = document.createElement('span');
        msgSpan.className = 'toast-message';
        msgSpan.textContent = message;

        const closeBtn = document.createElement('button');
        closeBtn.className = 'toast-close';
        closeBtn.innerHTML = '&times;';
        closeBtn.onclick = () => toast.remove();

        toast.appendChild(msgSpan);
        toast.appendChild(closeBtn);
        el.toastContainer.appendChild(toast);

        setTimeout(() => {
            if (toast.parentElement) {
                toast.style.opacity = '0';
                toast.style.transition = 'opacity 0.25s ease';
                setTimeout(() => toast.remove(), 250);
            }
        }, 4000);
    }

    function escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function formatDate(dateStr) {
        if (!dateStr) return '-';
        if (Array.isArray(dateStr)) {
            return `${dateStr[0]}-${String(dateStr[1]).padStart(2, '0')}-${String(dateStr[2]).padStart(2, '0')}`;
        }
        return String(dateStr).split('T')[0];
    }

    function formatDateTime(dtStr) {
        if (!dtStr) return '-';
        if (Array.isArray(dtStr)) {
            return `${dtStr[0]}-${String(dtStr[1]).padStart(2, '0')}-${String(dtStr[2]).padStart(2, '0')} ${String(dtStr[3] || 0).padStart(2, '0')}:${String(dtStr[4] || 0).padStart(2, '0')}`;
        }
        return String(dtStr).replace('T', ' ').substring(0, 19);
    }

    function getShipmentStatusBadge(status) {
        const s = (status || '').toUpperCase();
        switch (s) {
            case 'PENDING':
                return '<span class="badge badge-pending">PENDING</span>';
            case 'IN_TRANSIT':
                return '<span class="badge badge-intransit">IN TRANSIT</span>';
            case 'CUSTOMS_HOLD':
                return '<span class="badge badge-customshold">CUSTOMS HOLD</span>';
            case 'DELIVERED':
                return '<span class="badge badge-delivered">DELIVERED</span>';
            case 'CANCELLED':
                return '<span class="badge badge-cancelled">CANCELLED</span>';
            default:
                return `<span class="badge badge-neutral">${escapeHtml(s)}</span>`;
        }
    }

    function getCustomsStatusBadge(status) {
        const s = (status || '').toUpperCase();
        switch (s) {
            case 'PENDING':
                return '<span class="badge badge-pending">PENDING</span>';
            case 'SUBMITTED':
                return '<span class="badge badge-intransit">SUBMITTED</span>';
            case 'APPROVED':
                return '<span class="badge badge-delivered">APPROVED</span>';
            case 'REJECTED':
                return '<span class="badge badge-cancelled">REJECTED</span>';
            default:
                return `<span class="badge badge-neutral">${escapeHtml(s)}</span>`;
        }
    }

    // =========================================================================
    // Modal Helpers
    // =========================================================================

    function openModal(modalEl) {
        if (modalEl) modalEl.classList.remove('hidden');
    }

    function closeModal(modalEl) {
        if (modalEl) modalEl.classList.add('hidden');
    }

    document.querySelectorAll('[data-close-modal]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const targetId = e.currentTarget.getAttribute('data-close-modal');
            const targetModal = document.getElementById(targetId);
            if (targetModal) closeModal(targetModal);
        });
    });

    // =========================================================================
    // Authentication & Navigation Lifecycle
    // =========================================================================

    el.loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        el.loginError.style.display = 'none';
        el.btnLoginSubmit.disabled = true;
        el.btnLoginSubmit.textContent = 'Authenticating...';

        const username = el.inputUsername.value;
        const password = el.inputPassword.value;

        try {
            const user = await AuthManager.login(username, password);
            showToast(`Welcome back, ${user.principal}!`, 'success');
            renderAppShell(user);
        } catch (err) {
            el.loginError.textContent = err.message || 'Invalid username or password.';
            el.loginError.style.display = 'block';
        } finally {
            el.btnLoginSubmit.disabled = false;
            el.btnLoginSubmit.textContent = 'Sign In';
            el.inputPassword.value = '';
        }
    });

    el.btnLogout.addEventListener('click', () => {
        AuthManager.logout();
        activeView = null;
        el.viewApp.classList.add('hidden');
        el.viewLogin.classList.remove('hidden');
        el.inputUsername.value = '';
        el.inputPassword.value = '';
        el.loginError.style.display = 'none';
        showToast('You have signed out successfully.', 'info');
    });

    function renderAppShell(user) {
        el.viewLogin.classList.add('hidden');
        el.viewApp.classList.remove('hidden');

        // Populate User Info Header
        el.userDisplayName.textContent = user.principal;
        const roleName = AuthManager.getPrimaryRoleName();
        el.userDisplayRole.textContent = roleName.replace(/_/g, ' ');
        el.userAvatarInitials.textContent = user.principal.substring(0, 2).toUpperCase();

        if (AuthManager.isCustomer()) {
            el.sidebarPortalTag.textContent = 'Customer Portal';
            el.headerPortalTitle.textContent = 'GlobalTrade Customer Consignment Portal';
            setupCustomerNavigation();
        } else if (AuthManager.isVendorRep()) {
            el.sidebarPortalTag.textContent = 'Vendor Portal';
            el.headerPortalTitle.textContent = 'GlobalTrade Vendor Representative Portal';
            setupVendorNavigation();
        } else {
            el.sidebarPortalTag.textContent = 'Staff Workspace';
            el.headerPortalTitle.textContent = 'GlobalTrade Supply Chain Operations';
            setupStaffNavigation();
        }
    }

    // Check existing session in sessionStorage on startup
    window.addEventListener('DOMContentLoaded', async () => {
        try {
            const user = await AuthManager.restoreSession();
            if (user) {
                renderAppShell(user);
            }
        } catch (e) {
            AuthManager.logout();
        }
    });

    // =========================================================================
    // Navigation Menus Setup
    // =========================================================================

    function setupCustomerNavigation() {
        el.sidebarNav.innerHTML = '';
        const items = [
            { id: 'cust-dashboard', label: 'Dashboard', icon: '📊', handler: renderCustomerDashboard },
            { id: 'cust-shipments', label: 'My Shipments', icon: '📦', handler: renderCustomerShipments }
        ];
        buildNavMenu(items, 'cust-dashboard');
    }

    function setupVendorNavigation() {
        el.sidebarNav.innerHTML = '';
        const items = [
            { id: 'vendor-profile', label: 'My Vendor Profile', icon: '🏢', handler: renderVendorProfile }
        ];
        buildNavMenu(items, 'vendor-profile');
    }

    function setupStaffNavigation() {
        el.sidebarNav.innerHTML = '';
        const items = [];

        items.push({ id: 'staff-dashboard', label: 'Dashboard', icon: '📊', handler: renderStaffDashboard });

        if (AuthManager.hasRole('ADMIN') || AuthManager.hasRole('LOGISTICS_COORDINATOR') || AuthManager.hasRole('CUSTOMS_AGENT') || AuthManager.hasRole('WAREHOUSE_MANAGER')) {
            items.push({ id: 'staff-shipments', label: 'Shipments', icon: '📦', handler: renderStaffShipments });
        }

        if (AuthManager.hasRole('ADMIN') || AuthManager.hasRole('WAREHOUSE_MANAGER') || AuthManager.hasRole('LOGISTICS_COORDINATOR')) {
            items.push({ id: 'staff-inventory', label: 'Inventory', icon: '🏭', handler: renderStaffInventory });
        }

        if (AuthManager.hasRole('ADMIN') || AuthManager.hasRole('CUSTOMS_AGENT') || AuthManager.hasRole('LOGISTICS_COORDINATOR')) {
            items.push({ id: 'staff-customs', label: 'Customs', icon: '📋', handler: renderStaffCustoms });
        }

        if (AuthManager.hasRole('ADMIN') || AuthManager.hasRole('LOGISTICS_COORDINATOR')) {
            items.push({ id: 'staff-vendors', label: 'Vendors', icon: '🏢', handler: renderStaffVendors });
        }

        if (AuthManager.hasRole('ADMIN')) {
            items.push({ id: 'staff-audit', label: 'Audit Logs', icon: '🛡️', handler: renderStaffAuditLogs });
        }

        buildNavMenu(items, 'staff-dashboard');
    }

    function buildNavMenu(items, defaultId) {
        items.forEach(item => {
            const a = document.createElement('a');
            a.className = 'nav-item';
            a.id = `nav-${item.id}`;
            a.innerHTML = `<span class="nav-icon">${item.icon}</span> <span>${item.label}</span>`;
            a.onclick = () => {
                if (activeView === item.id) return; // Prevent duplicate execution on same tab click
                document.querySelectorAll('.nav-item').forEach(navEl => navEl.classList.remove('active'));
                a.classList.add('active');
                activeView = item.id;
                item.handler();
            };
            el.sidebarNav.appendChild(a);
        });

        // Activate default
        const defaultItem = document.getElementById(`nav-${defaultId}`);
        if (defaultItem) defaultItem.click();
    }

    // =========================================================================
    // CUSTOMER PORTAL VIEWS
    // =========================================================================

    async function renderCustomerDashboard() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">Customer Overview</h2>
                <p class="page-subtitle">Track your incoming consignments, international shipments, and customs status in real-time.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Loading your shipment statistics...</span>
            </div>
        `;

        try {
            const shipments = await ApiClient.getMyShipments();
            const total = shipments.length;
            const inTransit = shipments.filter(s => s.shipmentStatus === 'IN_TRANSIT').length;
            const delivered = shipments.filter(s => s.shipmentStatus === 'DELIVERED').length;
            const customsHold = shipments.filter(s => s.shipmentStatus === 'CUSTOMS_HOLD').length;

            let recentRows = '';
            if (shipments.length === 0) {
                recentRows = '<tr><td colspan="6" class="text-center text-muted">No consignments found assigned to your account.</td></tr>';
            } else {
                shipments.slice(0, 5).forEach(s => {
                    recentRows += `
                        <tr>
                            <td class="font-mono font-bold">${escapeHtml(s.trackingNumber)}</td>
                            <td>${escapeHtml(s.origin)}</td>
                            <td>${escapeHtml(s.destination)}</td>
                            <td>${getShipmentStatusBadge(s.shipmentStatus)}</td>
                            <td>${formatDate(s.expectedDeliveryDate)}</td>
                            <td class="text-right">
                                <button class="btn btn-secondary btn-sm" onclick="App.viewShipmentDetails(${s.id})">
                                    View Details
                                </button>
                            </td>
                        </tr>
                    `;
                });
            }

            el.mainContent.innerHTML = `
                <div class="page-header">
                    <h2 class="page-title">Customer Consignment Overview</h2>
                    <p class="page-subtitle">Real-time status of freight consignments registered to <strong>${escapeHtml(AuthManager.getUser().principal)}</strong>.</p>
                </div>

                <div class="kpi-grid">
                    <div class="kpi-card">
                        <div class="kpi-label">Total Shipments</div>
                        <div class="kpi-value">${total}</div>
                        <div class="kpi-subtext">Active and historical consignments</div>
                    </div>
                    <div class="kpi-card">
                        <div class="kpi-label">In Transit</div>
                        <div class="kpi-value" style="color: var(--primary);">${inTransit}</div>
                        <div class="kpi-subtext">Dispatched & moving through network</div>
                    </div>
                    <div class="kpi-card">
                        <div class="kpi-label">Delivered</div>
                        <div class="kpi-value" style="color: var(--success);">${delivered}</div>
                        <div class="kpi-subtext">Successfully received packages</div>
                    </div>
                    <div class="kpi-card">
                        <div class="kpi-label">Customs Hold</div>
                        <div class="kpi-value" style="color: var(--warning);">${customsHold}</div>
                        <div class="kpi-subtext">Pending statutory clearance</div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">Recent Consignments</h3>
                        <button class="btn btn-secondary btn-sm" onclick="App.navigateTo('cust-shipments')">View All</button>
                    </div>
                    <div class="card-body" style="padding: 0;">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Tracking #</th>
                                        <th>Origin</th>
                                        <th>Destination</th>
                                        <th>Current Status</th>
                                        <th>Expected Delivery</th>
                                        <th class="text-right">Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${recentRows}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message)}</span>
                </div>
            `;
        }
    }

    async function renderCustomerShipments() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">My Shipments</h2>
                <p class="page-subtitle">Complete list of consignments assigned to your client account.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Loading shipment records...</span>
            </div>
        `;

        try {
            const shipments = await ApiClient.getMyShipments();
            let rows = '';

            if (shipments.length === 0) {
                rows = '<tr><td colspan="7" class="text-center text-muted">No consignments found.</td></tr>';
            } else {
                shipments.forEach(s => {
                    rows += `
                        <tr>
                            <td class="font-mono font-bold">${escapeHtml(s.trackingNumber)}</td>
                            <td>${escapeHtml(s.origin)}</td>
                            <td>${escapeHtml(s.destination)}</td>
                            <td>${escapeHtml(s.vendorName || '-')}</td>
                            <td>${getShipmentStatusBadge(s.shipmentStatus)}</td>
                            <td>${formatDate(s.expectedDeliveryDate)}</td>
                            <td class="text-right">
                                <button class="btn btn-primary btn-sm" onclick="App.viewShipmentDetails(${s.id})">
                                    Track & Details
                                </button>
                            </td>
                        </tr>
                    `;
                });
            }

            el.mainContent.innerHTML = `
                <div class="page-header">
                    <h2 class="page-title">My Shipments</h2>
                    <p class="page-subtitle">Consignments registered to <strong>${escapeHtml(AuthManager.getUser().principal)}</strong>.</p>
                </div>

                <div class="card">
                    <div class="card-body" style="padding: 0;">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Tracking #</th>
                                        <th>Origin</th>
                                        <th>Destination</th>
                                        <th>Supplier / Vendor</th>
                                        <th>Status</th>
                                        <th>Expected Delivery</th>
                                        <th class="text-right">Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${rows}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message)}</span>
                </div>
            `;
        }
    }

    async function viewShipmentDetails(shipmentId) {
        try {
            const shipment = await ApiClient.getShipmentById(shipmentId);
            let customsDocs = [];
            try {
                customsDocs = await ApiClient.getCustomsByShipment(shipmentId);
            } catch (e) {
                customsDocs = [];
            }

            document.getElementById('det-tracking-number').textContent = shipment.trackingNumber;
            document.getElementById('det-status').innerHTML = getShipmentStatusBadge(shipment.shipmentStatus);
            document.getElementById('det-origin').textContent = shipment.origin;
            document.getElementById('det-destination').textContent = shipment.destination;
            document.getElementById('det-vendor').textContent = shipment.vendorName || '-';
            document.getElementById('det-expected-date').textContent = formatDate(shipment.expectedDeliveryDate);
            document.getElementById('det-actual-date').textContent = formatDate(shipment.actualDeliveryDate);

            // Render Workflow Visualizer
            renderWorkflowProgress(shipment.shipmentStatus);

            // Render Customs Docs Table
            const customsTbody = document.getElementById('det-customs-tbody');
            if (!customsDocs || customsDocs.length === 0) {
                customsTbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No customs declarations attached to this consignment.</td></tr>';
            } else {
                customsTbody.innerHTML = customsDocs.map(d => `
                    <tr>
                        <td class="font-mono font-bold">${escapeHtml(d.documentNumber)}</td>
                        <td>${escapeHtml(d.documentType)}</td>
                        <td>${getCustomsStatusBadge(d.status)}</td>
                        <td>${formatDate(d.submissionDeadline)}</td>
                    </tr>
                `).join('');
            }

            openModal(el.modalShipment);
        } catch (err) {
            showToast(err.message || 'Unable to load consignment details.', 'error');
        }
    }

    function renderWorkflowProgress(status) {
        const wfContainer = document.getElementById('shipment-progress-workflow');
        const st = (status || 'PENDING').toUpperCase();

        if (st === 'CANCELLED') {
            wfContainer.innerHTML = `
                <div class="workflow-step cancelled">
                    <div class="step-node"></div>
                    <div class="step-label">Order Created</div>
                </div>
                <div class="workflow-step cancelled">
                    <div class="step-node"></div>
                    <div class="step-label">Consignment Cancelled</div>
                </div>
            `;
            return;
        }

        const steps = [
            { id: 'PENDING', label: 'Order Registered' },
            { id: 'IN_TRANSIT', label: 'Dispatched & In Transit' },
            { id: 'CUSTOMS_HOLD', label: 'Customs Clearance' },
            { id: 'DELIVERED', label: 'Delivered to Destination' }
        ];

        const stepOrder = ['PENDING', 'IN_TRANSIT', 'CUSTOMS_HOLD', 'DELIVERED'];
        const currentIndex = stepOrder.indexOf(st);

        let html = '';
        steps.forEach((step, idx) => {
            let cls = '';
            if (idx < currentIndex) {
                cls = 'completed';
            } else if (idx === currentIndex) {
                cls = 'current';
            }
            html += `
                <div class="workflow-step ${cls}">
                    <div class="step-node"></div>
                    <div class="step-label">${step.label}</div>
                </div>
            `;
        });

        wfContainer.innerHTML = html;
    }

    // =========================================================================
    // STAFF PORTAL VIEWS (ROLE-AWARE)
    // =========================================================================

    async function renderStaffDashboard() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">Operations Dashboard</h2>
                <p class="page-subtitle">Real-time supply chain operational telemetry.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Aggregating workspace telemetry...</span>
            </div>
        `;

        const role = AuthManager.getPrimaryRoleName();

        try {
            // ADMIN or LOGISTICS_COORDINATOR: Authorized for Vendors, Inventory, Shipments, Customs
            if (role === 'ADMIN' || role === 'LOGISTICS_COORDINATOR') {
                const [vendors, inventory, shipments, customs] = await Promise.all([
                    ApiClient.getVendors(),
                    ApiClient.getInventory(),
                    ApiClient.getShipments(),
                    ApiClient.getCustoms()
                ]);

                const lowStockItems = inventory.filter(i => i.quantity <= i.reorderLevel);
                const activeShipments = shipments.filter(s => s.shipmentStatus === 'IN_TRANSIT' || s.shipmentStatus === 'PENDING');
                const pendingCustoms = customs.filter(c => c.status === 'PENDING' || c.status === 'SUBMITTED');

                let lowStockRows = '';
                if (lowStockItems.length === 0) {
                    lowStockRows = '<tr><td colspan="5" class="text-center text-muted">All inventory items are currently above reorder thresholds.</td></tr>';
                } else {
                    lowStockItems.forEach(i => {
                        lowStockRows += `
                            <tr>
                                <td class="font-mono font-bold">${escapeHtml(i.sku)}</td>
                                <td>${escapeHtml(i.itemName)}</td>
                                <td>${escapeHtml(i.warehouseName || '-')}</td>
                                <td style="color: var(--danger); font-weight: 700;">${i.quantity} / ${i.reorderLevel} units</td>
                                <td><span class="badge badge-lowstock">LOW STOCK</span></td>
                            </tr>
                        `;
                    });
                }

                let recentShipmentRows = '';
                if (shipments.length === 0) {
                    recentShipmentRows = '<tr><td colspan="5" class="text-center text-muted">No shipments found.</td></tr>';
                } else {
                    shipments.slice(0, 5).forEach(s => {
                        recentShipmentRows += `
                            <tr>
                                <td class="font-mono font-bold">${escapeHtml(s.trackingNumber)}</td>
                                <td>${escapeHtml(s.origin)} &rarr; ${escapeHtml(s.destination)}</td>
                                <td>${escapeHtml(s.vendorName || '-')}</td>
                                <td>${getShipmentStatusBadge(s.shipmentStatus)}</td>
                                <td class="text-right">
                                    <button class="btn btn-secondary btn-sm" onclick="App.viewShipmentDetails(${s.id})">Details</button>
                                </td>
                            </tr>
                        `;
                    });
                }

                el.mainContent.innerHTML = `
                    <div class="page-header">
                        <h2 class="page-title">Operations Command Center</h2>
                        <p class="page-subtitle">Welcome, <strong>${escapeHtml(AuthManager.getUser().principal)}</strong> (${escapeHtml(role)}).</p>
                    </div>

                    <div class="kpi-grid">
                        <div class="kpi-card">
                            <div class="kpi-label">Active Suppliers</div>
                            <div class="kpi-value">${vendors.length}</div>
                            <div class="kpi-subtext">Verified vendor partners</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Catalog SKUs</div>
                            <div class="kpi-value">${inventory.length}</div>
                            <div class="kpi-subtext">Across regional warehouses</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Active Shipments</div>
                            <div class="kpi-value" style="color: var(--primary);">${activeShipments.length}</div>
                            <div class="kpi-subtext">Pending & in-transit consignments</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Pending Customs</div>
                            <div class="kpi-value" style="color: var(--warning);">${pendingCustoms.length}</div>
                            <div class="kpi-subtext">Declarations awaiting review</div>
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px;">
                        <div class="card">
                            <div class="card-header">
                                <h3 class="card-title">Recent Consignments</h3>
                                <button class="btn btn-secondary btn-sm" onclick="App.navigateTo('staff-shipments')">View Hub</button>
                            </div>
                            <div class="card-body" style="padding: 0;">
                                <div class="table-responsive">
                                    <table class="data-table">
                                        <thead>
                                            <tr>
                                                <th>Tracking #</th>
                                                <th>Route</th>
                                                <th>Supplier</th>
                                                <th>Status</th>
                                                <th class="text-right">Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${recentShipmentRows}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>

                        <div class="card">
                            <div class="card-header">
                                <h3 class="card-title">Low Stock Reorder Alerts</h3>
                                <button class="btn btn-secondary btn-sm" onclick="App.navigateTo('staff-inventory')">Manage Stock</button>
                            </div>
                            <div class="card-body" style="padding: 0;">
                                <div class="table-responsive">
                                    <table class="data-table">
                                        <thead>
                                            <tr>
                                                <th>SKU</th>
                                                <th>Item Name</th>
                                                <th>Warehouse</th>
                                                <th>Stock / Level</th>
                                                <th>State</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${lowStockRows}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                return;
            }

            // WAREHOUSE_MANAGER: Authorized for Inventory and Shipments ONLY
            if (role === 'WAREHOUSE_MANAGER') {
                const [inventory, shipments] = await Promise.all([
                    ApiClient.getInventory(),
                    ApiClient.getShipments()
                ]);

                const lowStockItems = inventory.filter(i => i.quantity <= i.reorderLevel);
                const activeShipments = shipments.filter(s => s.shipmentStatus === 'PENDING' || s.shipmentStatus === 'IN_TRANSIT');
                const deliveredShipments = shipments.filter(s => s.shipmentStatus === 'DELIVERED');

                let lowStockRows = '';
                if (lowStockItems.length === 0) {
                    lowStockRows = '<tr><td colspan="5" class="text-center text-muted">All warehouse stock levels are healthy.</td></tr>';
                } else {
                    lowStockItems.forEach(i => {
                        lowStockRows += `
                            <tr>
                                <td class="font-mono font-bold">${escapeHtml(i.sku)}</td>
                                <td>${escapeHtml(i.itemName)}</td>
                                <td>${escapeHtml(i.warehouseName || '-')}</td>
                                <td style="color: var(--danger); font-weight: 700;">${i.quantity} / ${i.reorderLevel} units</td>
                                <td class="text-right">
                                    <button class="btn btn-success btn-sm" onclick="App.openReplenishModal(${i.id}, '${escapeHtml(i.sku)}', '${escapeHtml(i.itemName)}')">Replenish</button>
                                </td>
                            </tr>
                        `;
                    });
                }

                let recentShipments = '';
                if (shipments.length === 0) {
                    recentShipments = '<tr><td colspan="4" class="text-center text-muted">No shipments found.</td></tr>';
                } else {
                    shipments.slice(0, 5).forEach(s => {
                        recentShipments += `
                            <tr>
                                <td class="font-mono font-bold">${escapeHtml(s.trackingNumber)}</td>
                                <td>${escapeHtml(s.origin)} &rarr; ${escapeHtml(s.destination)}</td>
                                <td>${getShipmentStatusBadge(s.shipmentStatus)}</td>
                                <td class="text-right">
                                    <button class="btn btn-secondary btn-sm" onclick="App.viewShipmentDetails(${s.id})">Details</button>
                                </td>
                            </tr>
                        `;
                    });
                }

                el.mainContent.innerHTML = `
                    <div class="page-header">
                        <h2 class="page-title">Warehouse Management Dashboard</h2>
                        <p class="page-subtitle">Real-time inventory levels, storage facility metrics, and outbound dispatch monitoring.</p>
                    </div>

                    <div class="kpi-grid">
                        <div class="kpi-card">
                            <div class="kpi-label">Warehouse SKUs</div>
                            <div class="kpi-value">${inventory.length}</div>
                            <div class="kpi-subtext">Managed inventory catalog items</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Low Stock Alerts</div>
                            <div class="kpi-value" style="color: var(--danger);">${lowStockItems.length}</div>
                            <div class="kpi-subtext">Items at or below reorder threshold</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Active Consignments</div>
                            <div class="kpi-value" style="color: var(--primary);">${activeShipments.length}</div>
                            <div class="kpi-subtext">Pending and dispatched shipments</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Delivered Shipments</div>
                            <div class="kpi-value" style="color: var(--success);">${deliveredShipments.length}</div>
                            <div class="kpi-subtext">Completed carrier deliveries</div>
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px;">
                        <div class="card">
                            <div class="card-header">
                                <h3 class="card-title">Low Stock Reorder Alerts</h3>
                                <button class="btn btn-secondary btn-sm" onclick="App.navigateTo('staff-inventory')">View Inventory</button>
                            </div>
                            <div class="card-body" style="padding: 0;">
                                <div class="table-responsive">
                                    <table class="data-table">
                                        <thead>
                                            <tr>
                                                <th>SKU</th>
                                                <th>Item Name</th>
                                                <th>Facility</th>
                                                <th>Stock / Threshold</th>
                                                <th class="text-right">Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${lowStockRows}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>

                        <div class="card">
                            <div class="card-header">
                                <h3 class="card-title">Recent Warehouse Shipments</h3>
                                <button class="btn btn-secondary btn-sm" onclick="App.navigateTo('staff-shipments')">View Shipments</button>
                            </div>
                            <div class="card-body" style="padding: 0;">
                                <div class="table-responsive">
                                    <table class="data-table">
                                        <thead>
                                            <tr>
                                                <th>Tracking #</th>
                                                <th>Route</th>
                                                <th>Status</th>
                                                <th class="text-right">Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${recentShipments}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                return;
            }

            // CUSTOMS_AGENT: Authorized for Customs and Shipments ONLY
            if (role === 'CUSTOMS_AGENT') {
                const [customs, shipments] = await Promise.all([
                    ApiClient.getCustoms(),
                    ApiClient.getShipments()
                ]);

                const pendingDocs = customs.filter(c => c.status === 'PENDING' || c.status === 'SUBMITTED');
                const approvedDocs = customs.filter(c => c.status === 'APPROVED');
                const inTransitShipments = shipments.filter(s => s.shipmentStatus === 'IN_TRANSIT');
                const customsHoldShipments = shipments.filter(s => s.shipmentStatus === 'CUSTOMS_HOLD');

                let pendingRows = '';
                if (pendingDocs.length === 0) {
                    pendingRows = '<tr><td colspan="5" class="text-center text-muted">No declarations currently pending customs review.</td></tr>';
                } else {
                    pendingDocs.slice(0, 5).forEach(d => {
                        pendingRows += `
                            <tr>
                                <td class="font-mono font-bold">${escapeHtml(d.documentNumber)}</td>
                                <td>${escapeHtml(d.documentType)}</td>
                                <td class="font-mono">${escapeHtml(d.shipmentTrackingNumber || '-')}</td>
                                <td>${getCustomsStatusBadge(d.status)}</td>
                                <td class="text-right">
                                    <button class="btn btn-success btn-sm" onclick="App.reviewCustomsDoc(${d.id}, '${escapeHtml(d.documentNumber)}')">Approve</button>
                                </td>
                            </tr>
                        `;
                    });
                }

                let recentShipments = '';
                if (shipments.length === 0) {
                    recentShipments = '<tr><td colspan="4" class="text-center text-muted">No shipments found.</td></tr>';
                } else {
                    shipments.slice(0, 5).forEach(s => {
                        recentShipments += `
                            <tr>
                                <td class="font-mono font-bold">${escapeHtml(s.trackingNumber)}</td>
                                <td>${escapeHtml(s.origin)} &rarr; ${escapeHtml(s.destination)}</td>
                                <td>${getShipmentStatusBadge(s.shipmentStatus)}</td>
                                <td class="text-right">
                                    <button class="btn btn-secondary btn-sm" onclick="App.viewShipmentDetails(${s.id})">Details</button>
                                </td>
                            </tr>
                        `;
                    });
                }

                el.mainContent.innerHTML = `
                    <div class="page-header">
                        <h2 class="page-title">Customs Regulatory Command Center</h2>
                        <p class="page-subtitle">Statutory cross-border clearance, tariff declarations, and consignment review.</p>
                    </div>

                    <div class="kpi-grid">
                        <div class="kpi-card">
                            <div class="kpi-label">Total Declarations</div>
                            <div class="kpi-value">${customs.length}</div>
                            <div class="kpi-subtext">On record across all consignments</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Pending Review</div>
                            <div class="kpi-value" style="color: var(--warning);">${pendingDocs.length}</div>
                            <div class="kpi-subtext">Awaiting clearance decision</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Approved Clearance</div>
                            <div class="kpi-value" style="color: var(--success);">${approvedDocs.length}</div>
                            <div class="kpi-subtext">Granted regulatory clearance</div>
                        </div>
                        <div class="kpi-card">
                            <div class="kpi-label">Customs Holds</div>
                            <div class="kpi-value" style="color: #854d0e;">${customsHoldShipments.length}</div>
                            <div class="kpi-subtext">Consignments held for inspection</div>
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px;">
                        <div class="card">
                            <div class="card-header">
                                <h3 class="card-title">Declarations Awaiting Clearance</h3>
                                <button class="btn btn-secondary btn-sm" onclick="App.navigateTo('staff-customs')">View Customs Hub</button>
                            </div>
                            <div class="card-body" style="padding: 0;">
                                <div class="table-responsive">
                                    <table class="data-table">
                                        <thead>
                                            <tr>
                                                <th>Document #</th>
                                                <th>Type</th>
                                                <th>Linked Shipment</th>
                                                <th>Status</th>
                                                <th class="text-right">Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${pendingRows}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>

                        <div class="card">
                            <div class="card-header">
                                <h3 class="card-title">Consignments in Network</h3>
                                <button class="btn btn-secondary btn-sm" onclick="App.navigateTo('staff-shipments')">View Shipments</button>
                            </div>
                            <div class="card-body" style="padding: 0;">
                                <div class="table-responsive">
                                    <table class="data-table">
                                        <thead>
                                            <tr>
                                                <th>Tracking #</th>
                                                <th>Route</th>
                                                <th>Status</th>
                                                <th class="text-right">Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${recentShipments}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                return;
            }

            // Fallback for generic staff
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon">👤</span>
                    <span class="state-text">Welcome, ${escapeHtml(AuthManager.getUser().principal)}. Use the sidebar menu to navigate.</span>
                </div>
            `;

        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message || 'Unable to load dashboard telemetry.')}</span>
                </div>
            `;
        }
    }

    async function renderStaffShipments() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">Consignment Management Hub</h2>
                <p class="page-subtitle">Manage routes, carrier assignments, and transactional dispatch execution.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Loading shipment registry...</span>
            </div>
        `;

        try {
            const shipments = await ApiClient.getShipments();
            const canDispatch = AuthManager.hasRole('ADMIN') ||
                                AuthManager.hasRole('LOGISTICS_COORDINATOR') ||
                                AuthManager.hasRole('WAREHOUSE_MANAGER');

            let rows = '';
            if (shipments.length === 0) {
                rows = '<tr><td colspan="8" class="text-center text-muted">No shipments found in system.</td></tr>';
            } else {
                shipments.forEach(s => {
                    const isPending = s.shipmentStatus === 'PENDING';
                    const dispatchBtn = (canDispatch && isPending)
                        ? `<button class="btn btn-success btn-sm" onclick="App.openDispatchModal(${s.id}, '${escapeHtml(s.trackingNumber)}')">Dispatch</button>`
                        : '';

                    rows += `
                        <tr>
                            <td class="font-mono font-bold">${escapeHtml(s.trackingNumber)}</td>
                            <td>${escapeHtml(s.origin)}</td>
                            <td>${escapeHtml(s.destination)}</td>
                            <td>${escapeHtml(s.vendorName || '-')}</td>
                            <td><span class="badge badge-neutral">${escapeHtml(s.customerUsername || 'Unassigned')}</span></td>
                            <td>${getShipmentStatusBadge(s.shipmentStatus)}</td>
                            <td>${formatDate(s.expectedDeliveryDate)}</td>
                            <td class="text-right" style="white-space: nowrap;">
                                <button class="btn btn-secondary btn-sm" onclick="App.viewShipmentDetails(${s.id})">Details</button>
                                ${dispatchBtn}
                            </td>
                        </tr>
                    `;
                });
            }

            el.mainContent.innerHTML = `
                <div class="page-header">
                    <h2 class="page-title">Consignment Management Hub</h2>
                    <p class="page-subtitle">Track, monitor, and dispatch international cargo shipments.</p>
                </div>

                <div class="card">
                    <div class="card-body" style="padding: 0;">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Tracking #</th>
                                        <th>Origin</th>
                                        <th>Destination</th>
                                        <th>Supplier / Vendor</th>
                                        <th>Assigned Client</th>
                                        <th>Status</th>
                                        <th>Expected Delivery</th>
                                        <th class="text-right">Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${rows}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message)}</span>
                </div>
            `;
        }
    }

    async function openDispatchModal(shipmentId, trackingNumber) {
        el.dispatchShipmentId.value = shipmentId;
        el.dispatchTrackingDisplay.value = trackingNumber;
        el.dispatchQuantityInput.value = '10';

        el.dispatchInventorySelect.innerHTML = '<option value="">Loading available inventory items...</option>';
        openModal(el.modalDispatch);

        try {
            const inventory = await ApiClient.getInventory();
            if (inventory.length === 0) {
                el.dispatchInventorySelect.innerHTML = '<option value="">No inventory items found</option>';
            } else {
                el.dispatchInventorySelect.innerHTML = inventory.map(item => `
                    <option value="${item.id}">
                        ${escapeHtml(item.sku)} - ${escapeHtml(item.itemName)} (Available: ${item.quantity} units, ${escapeHtml(item.warehouseName || 'Warehouse')})
                    </option>
                `).join('');
            }
        } catch (err) {
            el.dispatchInventorySelect.innerHTML = '<option value="1">SKU-ELEC-001 (Default Inventory Item)</option>';
        }
    }

    el.formDispatch.addEventListener('submit', async (e) => {
        e.preventDefault();
        const shipmentId = el.dispatchShipmentId.value;
        const inventoryId = el.dispatchInventorySelect.value;
        const quantity = parseInt(el.dispatchQuantityInput.value, 10);

        if (!shipmentId || !inventoryId || !quantity || quantity <= 0) {
            showToast('Please specify valid dispatch parameters.', 'warning');
            return;
        }

        el.btnDispatchConfirm.disabled = true;
        el.btnDispatchConfirm.textContent = 'Processing Dispatch...';

        try {
            await ApiClient.dispatchShipment(shipmentId, inventoryId, quantity);
            showToast(`Shipment ${el.dispatchTrackingDisplay.value} dispatched successfully. Inventory stock deducted.`, 'success');
            closeModal(el.modalDispatch);
            renderStaffShipments();
        } catch (err) {
            showToast(err.message || 'Dispatch operation failed.', 'error');
        } finally {
            el.btnDispatchConfirm.disabled = false;
            el.btnDispatchConfirm.textContent = 'Confirm Dispatch';
        }
    });

    async function renderStaffInventory() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">Warehouse Inventory Management</h2>
                <p class="page-subtitle">Real-time inventory levels, reorder thresholds, and warehouse stock reconciliation.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Loading warehouse catalog...</span>
            </div>
        `;

        try {
            const items = await ApiClient.getInventory();
            const canReplenish = AuthManager.hasRole('ADMIN') || AuthManager.hasRole('WAREHOUSE_MANAGER');

            let rows = '';
            if (items.length === 0) {
                rows = '<tr><td colspan="8" class="text-center text-muted">No inventory items found.</td></tr>';
            } else {
                items.forEach(item => {
                    const isLowStock = item.quantity <= item.reorderLevel;
                    const stockBadge = isLowStock
                        ? '<span class="badge badge-lowstock">LOW STOCK</span>'
                        : '<span class="badge badge-instock">IN STOCK</span>';

                    const replenishBtn = canReplenish
                        ? `<button class="btn btn-success btn-sm" onclick="App.openReplenishModal(${item.id}, '${escapeHtml(item.sku)}', '${escapeHtml(item.itemName)}')">Replenish Stock</button>`
                        : '';

                    rows += `
                        <tr>
                            <td class="font-mono font-bold">${escapeHtml(item.sku)}</td>
                            <td>${escapeHtml(item.itemName)}</td>
                            <td>${escapeHtml(item.warehouseName || '-')} <small class="text-muted">(${escapeHtml(item.warehouseCode || '')})</small></td>
                            <td class="font-bold">${item.quantity} units</td>
                            <td>${item.reorderLevel} units</td>
                            <td>$${Number(item.unitPrice || 0).toFixed(2)}</td>
                            <td>${stockBadge}</td>
                            <td class="text-right">${replenishBtn}</td>
                        </tr>
                    `;
                });
            }

            el.mainContent.innerHTML = `
                <div class="page-header">
                    <h2 class="page-title">Warehouse Inventory Management</h2>
                    <p class="page-subtitle">Monitor stock levels and replenish regional warehouse supplies.</p>
                </div>

                <div class="card">
                    <div class="card-body" style="padding: 0;">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>SKU</th>
                                        <th>Item Description</th>
                                        <th>Facility Location</th>
                                        <th>Current Stock</th>
                                        <th>Reorder Level</th>
                                        <th>Unit Price</th>
                                        <th>Status</th>
                                        <th class="text-right">Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${rows}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message)}</span>
                </div>
            `;
        }
    }

    function openReplenishModal(itemId, sku, itemName) {
        el.replenishItemId.value = itemId;
        el.replenishItemDisplay.value = `${sku} - ${itemName}`;
        el.replenishQuantityInput.value = '50';
        openModal(el.modalReplenish);
    }

    el.formReplenish.addEventListener('submit', async (e) => {
        e.preventDefault();
        const itemId = el.replenishItemId.value;
        const quantity = parseInt(el.replenishQuantityInput.value, 10);

        if (!itemId || !quantity || quantity <= 0) {
            showToast('Please enter a valid positive quantity.', 'warning');
            return;
        }

        el.btnReplenishConfirm.disabled = true;
        el.btnReplenishConfirm.textContent = 'Replenishing...';

        try {
            await ApiClient.replenishInventory(itemId, quantity);
            showToast(`Added ${quantity} units to ${el.replenishItemDisplay.value}.`, 'success');
            closeModal(el.modalReplenish);
            renderStaffInventory();
        } catch (err) {
            showToast(err.message || 'Failed to replenish stock.', 'error');
        } finally {
            el.btnReplenishConfirm.disabled = false;
            el.btnReplenishConfirm.textContent = 'Confirm Replenishment';
        }
    });

    async function renderStaffCustoms() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">Customs & Statutory Declarations</h2>
                <p class="page-subtitle">International trade documentation, import declarations, and clearance reviews.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Loading customs declarations...</span>
            </div>
        `;

        try {
            const docs = await ApiClient.getCustoms();
            const canReview = AuthManager.hasRole('ADMIN') || AuthManager.hasRole('CUSTOMS_AGENT');

            let rows = '';
            if (docs.length === 0) {
                rows = '<tr><td colspan="6" class="text-center text-muted">No customs declarations found.</td></tr>';
            } else {
                docs.forEach(doc => {
                    const isPending = doc.status === 'PENDING' || doc.status === 'SUBMITTED';
                    const reviewBtn = (canReview && isPending)
                        ? `<button class="btn btn-success btn-sm" onclick="App.reviewCustomsDoc(${doc.id}, '${escapeHtml(doc.documentNumber)}')">Approve Clearance</button>`
                        : '';

                    rows += `
                        <tr>
                            <td class="font-mono font-bold">${escapeHtml(doc.documentNumber)}</td>
                            <td>${escapeHtml(doc.documentType)}</td>
                            <td class="font-mono">${escapeHtml(doc.shipmentTrackingNumber || '-')}</td>
                            <td>${getCustomsStatusBadge(doc.status)}</td>
                            <td>${formatDate(doc.submissionDeadline)}</td>
                            <td class="text-right">${reviewBtn}</td>
                        </tr>
                    `;
                });
            }

            el.mainContent.innerHTML = `
                <div class="page-header">
                    <h2 class="page-title">Customs & Statutory Declarations</h2>
                    <p class="page-subtitle">Review and grant regulatory approval for international consignments.</p>
                </div>

                <div class="card">
                    <div class="card-body" style="padding: 0;">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Document #</th>
                                        <th>Filing Type</th>
                                        <th>Linked Consignment</th>
                                        <th>Clearance Status</th>
                                        <th>Submission Deadline</th>
                                        <th class="text-right">Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${rows}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message)}</span>
                </div>
            `;
        }
    }

    async function reviewCustomsDoc(docId, docNumber) {
        if (!confirm(`Are you sure you want to approve customs declaration ${docNumber}?`)) {
            return;
        }

        try {
            await ApiClient.reviewCustoms(docId);
            showToast(`Customs Document ${docNumber} approved successfully.`, 'success');
            renderStaffCustoms();
        } catch (err) {
            showToast(err.message || 'Customs review failed.', 'error');
        }
    }

    async function renderStaffVendors() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">Supplier & Vendor Directory</h2>
                <p class="page-subtitle">Enterprise suppliers, contractual compliance, and performance ratings.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Loading registered suppliers...</span>
            </div>
        `;

        try {
            const vendors = await ApiClient.getVendors();
            let rows = '';

            if (vendors.length === 0) {
                rows = '<tr><td colspan="7" class="text-center text-muted">No suppliers found.</td></tr>';
            } else {
                vendors.forEach(v => {
                    const rating = v.performanceRating ? Number(v.performanceRating).toFixed(2) + ' / 5.00' : 'N/A';
                    rows += `
                        <tr>
                            <td class="font-mono font-bold">${escapeHtml(v.vendorCode)}</td>
                            <td class="font-bold">${escapeHtml(v.companyName)}</td>
                            <td>${escapeHtml(v.contactName || '-')}</td>
                            <td>${escapeHtml(v.country || '-')}</td>
                            <td>${escapeHtml(v.email || '-')}</td>
                            <td><span class="badge badge-active">${escapeHtml(v.status || 'ACTIVE')}</span></td>
                            <td class="font-bold">${rating}</td>
                        </tr>
                    `;
                });
            }

            el.mainContent.innerHTML = `
                <div class="page-header">
                    <h2 class="page-title">Supplier & Vendor Directory</h2>
                    <p class="page-subtitle">Registered external suppliers and performance scores.</p>
                </div>

                <div class="card">
                    <div class="card-body" style="padding: 0;">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Vendor Code</th>
                                        <th>Company Name</th>
                                        <th>Primary Contact</th>
                                        <th>Country</th>
                                        <th>Email</th>
                                        <th>Status</th>
                                        <th>Rating</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${rows}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message)}</span>
                </div>
            `;
        }
    }

    async function renderVendorProfile() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">My Supplier Profile</h2>
                <p class="page-subtitle">Supplier organization information, compliance rating, and registered contact details.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Loading supplier profile...</span>
            </div>
        `;

        try {
            const v = await ApiClient.getMyVendorProfile();
            const rating = v.performanceRating ? Number(v.performanceRating).toFixed(2) + ' / 5.00' : 'N/A';

            el.mainContent.innerHTML = `
                <div class="page-header">
                    <h2 class="page-title">My Supplier Profile</h2>
                    <p class="page-subtitle">Organization profile mapped to <strong>${escapeHtml(AuthManager.getUser().principal)}</strong>.</p>
                </div>

                <div class="card" style="max-width: 800px;">
                    <div class="card-header">
                        <h3 class="card-title">${escapeHtml(v.companyName)}</h3>
                        <span class="badge badge-active">${escapeHtml(v.status || 'ACTIVE')}</span>
                    </div>
                    <div class="card-body">
                        <div class="details-grid">
                            <div class="detail-item">
                                <span class="detail-label">Vendor Code</span>
                                <span class="detail-value font-mono font-bold">${escapeHtml(v.vendorCode)}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Performance Rating</span>
                                <span class="detail-value font-bold" style="color: var(--primary); font-size: 1.1rem;">${rating}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Primary Contact Person</span>
                                <span class="detail-value">${escapeHtml(v.contactName || '-')}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Country</span>
                                <span class="detail-value">${escapeHtml(v.country || '-')}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Contact Email</span>
                                <span class="detail-value">${escapeHtml(v.email || '-')}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Contact Telephone</span>
                                <span class="detail-value">${escapeHtml(v.phone || '-')}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Member Since</span>
                                <span class="detail-value">${formatDate(v.createdAt)}</span>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message)}</span>
                </div>
            `;
        }
    }

    async function renderStaffAuditLogs() {
        el.mainContent.innerHTML = `
            <div class="page-header">
                <h2 class="page-title">Enterprise Audit & Compliance Trail</h2>
                <p class="page-subtitle">Autonomous, immutable audit records persisted across all business transactions.</p>
            </div>
            <div class="state-container">
                <span class="state-icon">⏳</span>
                <span class="state-text">Loading audit trail...</span>
            </div>
        `;

        try {
            const logs = await ApiClient.getAuditLogs(50);
            let rows = '';

            if (logs.length === 0) {
                rows = '<tr><td colspan="6" class="text-center text-muted">No audit logs found.</td></tr>';
            } else {
                logs.forEach(log => {
                    rows += `
                        <tr>
                            <td class="font-mono text-muted" style="font-size: 0.8rem;">${formatDateTime(log.timestamp)}</td>
                            <td class="font-bold">${escapeHtml(log.action)}</td>
                            <td>${escapeHtml(log.entityType)} ${log.entityId ? '#' + log.entityId : ''}</td>
                            <td><span class="badge badge-neutral">${escapeHtml(log.performedBy || 'SYSTEM')}</span></td>
                            <td style="font-size: 0.85rem;">${escapeHtml(log.details || '-')}</td>
                        </tr>
                    `;
                });
            }

            el.mainContent.innerHTML = `
                <div class="page-header">
                    <h2 class="page-title">Enterprise Audit & Compliance Trail</h2>
                    <p class="page-subtitle">Showing the 50 most recent immutable audit log entries.</p>
                </div>

                <div class="card">
                    <div class="card-body" style="padding: 0;">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Timestamp</th>
                                        <th>Action</th>
                                        <th>Target Entity</th>
                                        <th>Operator</th>
                                        <th>Event Details</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${rows}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
        } catch (err) {
            el.mainContent.innerHTML = `
                <div class="state-container">
                    <span class="state-icon" style="color: var(--danger);">⚠️</span>
                    <span class="state-text">${escapeHtml(err.message)}</span>
                </div>
            `;
        }
    }

    // Export Global App Object for inline HTML event bindings
    window.App = {
        navigateTo: (viewId) => {
            const navBtn = document.getElementById(`nav-${viewId}`);
            if (navBtn) navBtn.click();
        },
        viewShipmentDetails,
        openDispatchModal,
        openReplenishModal,
        reviewCustomsDoc
    };

})();
