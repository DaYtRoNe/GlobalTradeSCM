/**
 * GlobalTrade Supply Chain Management System
 * API Client & Network Communications Layer
 * Handles in-memory credential injection, error normalization, and non-JSON container responses.
 */

const ApiClient = (() => {
    // In-memory credentials
    let _authCredentials = null;

    /**
     * Set in-memory credentials from raw username and password.
     * @param {string} username 
     * @param {string} password 
     */
    function setCredentials(username, password) {
        if (!username || !password) {
            _authCredentials = null;
            return;
        }
        const token = btoa(unescape(encodeURIComponent(username + ':' + password)));
        _authCredentials = {
            username: username,
            basicHeader: 'Basic ' + token
        };
    }

    /**
     * Restores credentials from a pre-encoded Basic header (e.g. from sessionStorage).
     * @param {string} username 
     * @param {string} basicHeader 
     */
    function setBasicAuth(username, basicHeader) {
        if (!username || !basicHeader) {
            _authCredentials = null;
            return;
        }
        _authCredentials = {
            username: username,
            basicHeader: basicHeader
        };
    }

    /**
     * Clears credentials from memory.
     */
    function clearCredentials() {
        _authCredentials = null;
    }

    /**
     * Returns current credentials object if present.
     */
    function getCredentials() {
        return _authCredentials;
    }

    /**
     * Resolves the base REST API context path.
     */
    function getApiBasePath() {
        const path = window.location.pathname;
        const prefix = path.substring(0, path.indexOf('/', 1));
        return (prefix && prefix !== '/') ? prefix + '/api' : '/globaltrade/api';
    }

    /**
     * Core API Request dispatcher.
     * @param {string} endpoint - e.g. '/shipments', '/inventory/1/replenish'
     * @param {object} options - Fetch options (method, headers, body, params)
     * @returns {Promise<any>}
     */
    async function request(endpoint, options = {}) {
        const basePath = getApiBasePath();
        let url = basePath + (endpoint.startsWith('/') ? endpoint : '/' + endpoint);

        // Append query parameters if provided
        if (options.params) {
            const query = new URLSearchParams(options.params).toString();
            if (query) {
                url += (url.includes('?') ? '&' : '?') + query;
            }
        }

        const headers = {
            'Accept': 'application/json',
            ...(options.headers || {})
        };

        // Attach Basic Auth if available and not explicitly skipped
        if (!options.skipAuth && _authCredentials && _authCredentials.basicHeader) {
            headers['Authorization'] = _authCredentials.basicHeader;
        }

        const fetchOptions = {
            method: options.method || 'GET',
            headers: headers
        };

        if (options.body) {
            if (typeof options.body === 'object') {
                fetchOptions.body = JSON.stringify(options.body);
                headers['Content-Type'] = 'application/json';
            } else {
                fetchOptions.body = options.body;
            }
        }

        try {
            const response = await fetch(url, fetchOptions);
            const contentType = response.headers.get('content-type') || '';

            // Parse response body safely
            let data = null;
            if (contentType.includes('application/json')) {
                try {
                    data = await response.json();
                } catch (e) {
                    data = null;
                }
            }

            if (response.ok) {
                return data;
            }

            // Normalize HTTP error states without injecting raw HTML into DOM
            const errorObj = {
                status: response.status,
                statusText: response.statusText,
                message: 'An unexpected error occurred.'
            };

            if (response.status === 401) {
                errorObj.message = (data && data.message) ? data.message : 'Authentication required or invalid credentials.';
            } else if (response.status === 403) {
                if (data && data.message) {
                    errorObj.message = data.message;
                } else {
                    errorObj.message = 'Access denied. Your account does not have permission for this action.';
                }
            } else if (response.status === 404) {
                errorObj.message = (data && data.message) ? data.message : 'The requested resource was not found.';
            } else if (response.status === 409) {
                errorObj.message = (data && data.message) ? data.message : 'Conflict during operation. Available inventory may be insufficient.';
            } else if (response.status === 400) {
                errorObj.message = (data && data.message) ? data.message : 'Invalid request parameters.';
            } else if (response.status >= 500) {
                errorObj.message = 'An internal server error occurred while processing the request.';
            }

            throw errorObj;
        } catch (err) {
            // Re-throw normalized error objects or connection failures
            if (err.status !== undefined) {
                throw err;
            }
            throw {
                status: 0,
                statusText: 'Network Error',
                message: 'Unable to connect to the GlobalTrade application server.'
            };
        }
    }

    // =========================================================================
    // Real Business Endpoints
    // =========================================================================

    return {
        setCredentials,
        setBasicAuth,
        clearCredentials,
        getCredentials,
        request,

        // Dedicated UI Login Endpoint (No WWW-Authenticate Basic popup)
        uiLogin: (username, password) => request('/ui-auth/login', {
            method: 'POST',
            body: { username, password },
            skipAuth: true
        }),

        // Authenticated WhoAmI Probe
        whoami: () => request('/security/whoami'),

        // Vendors
        getVendors: () => request('/vendors'),
        getMyVendorProfile: () => request('/vendors/me'),

        // Inventory
        getInventory: () => request('/inventory'),
        replenishInventory: (id, quantity) => request(`/business-security/inventory/${id}/replenish`, {
            method: 'POST',
            params: { quantity }
        }),

        // Shipments
        getShipments: () => request('/shipments'),
        getMyShipments: () => request('/shipments/my-shipments'),
        getShipmentById: (id) => request(`/shipments/${id}`),
        dispatchShipment: (shipmentId, inventoryId, quantity) => request(`/business-security/shipment/${shipmentId}/dispatch`, {
            method: 'POST',
            params: { inventoryId, quantity }
        }),

        // Customs
        getCustoms: () => request('/customs'),
        getCustomsByShipment: (shipmentId) => request(`/customs/shipment/${shipmentId}`),
        reviewCustoms: (documentId) => request(`/business-security/customs/${documentId}/review`, {
            method: 'POST'
        }),

        // Audit Logs
        getAuditLogs: (limit = 50) => request('/audit-logs', {
            params: { limit }
        })
    };
})();
