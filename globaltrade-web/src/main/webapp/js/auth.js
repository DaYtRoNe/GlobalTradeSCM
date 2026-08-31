/**
 * GlobalTrade Supply Chain Management System
 * Authentication & Session Management Layer
 * Handles role-based identity checks, UI login flow, and sessionStorage persistence.
 *
 * NOTE:
 * This sessionStorage approach is for the local assignment demo only.
 * Production systems should use HTTPS and a server-side/token-based secure session architecture.
 */

const AuthManager = (() => {
    // Current authenticated user state (In-memory)
    let _currentUser = null;

    // SessionStorage keys
    const SESSION_KEY_USERNAME = 'globaltrade.auth.username';
    const SESSION_KEY_BASIC = 'globaltrade.auth.basic';

    /**
     * Authenticates user via dedicated UI login endpoint (POST /api/ui-auth/login).
     * Prevents browser-native HTTP Basic popups upon failure.
     * @param {string} username 
     * @param {string} password 
     * @returns {Promise<object>} User session details
     */
    async function login(username, password) {
        if (!username || !password) {
            throw { message: 'Please enter both username and password.' };
        }

        const trimmedUser = username.trim();

        try {
            // Authenticate through container realm via UI login endpoint
            const authResult = await ApiClient.uiLogin(trimmedUser, password);
            if (!authResult || !authResult.authenticated) {
                logout();
                throw { message: 'Invalid username or password.' };
            }

            // Construct Basic Auth token for subsequent API calls
            const basicToken = 'Basic ' + btoa(unescape(encodeURIComponent(trimmedUser + ':' + password)));

            // Store in sessionStorage for local assignment demo session persistence
            sessionStorage.setItem(SESSION_KEY_USERNAME, trimmedUser);
            sessionStorage.setItem(SESSION_KEY_BASIC, basicToken);

            // Update ApiClient state
            ApiClient.setBasicAuth(trimmedUser, basicToken);

            _currentUser = {
                principal: authResult.principal,
                roles: authResult.roles || {},
                authenticated: true
            };

            return _currentUser;
        } catch (err) {
            logout();
            throw err;
        }
    }

    /**
     * Attempts to restore user session from sessionStorage upon page load/refresh.
     * Validates stored credentials via GET /api/security/whoami.
     * @returns {Promise<object|null>}
     */
    async function restoreSession() {
        const storedUser = sessionStorage.getItem(SESSION_KEY_USERNAME);
        const storedBasic = sessionStorage.getItem(SESSION_KEY_BASIC);

        if (!storedUser || !storedBasic) {
            logout();
            return null;
        }

        ApiClient.setBasicAuth(storedUser, storedBasic);

        try {
            const whoami = await ApiClient.whoami();
            if (whoami && whoami.authenticated) {
                _currentUser = {
                    principal: whoami.principal,
                    roles: whoami.roles || {},
                    authenticated: true
                };
                return _currentUser;
            } else {
                logout();
                return null;
            }
        } catch (e) {
            logout();
            return null;
        }
    }

    /**
     * Terminates the current session and purges all credentials from memory and sessionStorage.
     */
    function logout() {
        sessionStorage.removeItem(SESSION_KEY_USERNAME);
        sessionStorage.removeItem(SESSION_KEY_BASIC);
        ApiClient.clearCredentials();
        _currentUser = null;
    }

    function getUser() {
        return _currentUser;
    }

    function isAuthenticated() {
        return _currentUser !== null && _currentUser.authenticated === true;
    }

    function hasRole(roleName) {
        if (!_currentUser || !_currentUser.roles) return false;
        return !!_currentUser.roles[roleName];
    }

    function isCustomer() {
        return hasRole('CUSTOMER');
    }

    function isVendorRep() {
        return hasRole('VENDOR_REPRESENTATIVE');
    }

    function isStaff() {
        return hasRole('ADMIN') ||
               hasRole('LOGISTICS_COORDINATOR') ||
               hasRole('WAREHOUSE_MANAGER') ||
               hasRole('CUSTOMS_AGENT');
    }

    function getPrimaryRoleName() {
        if (!_currentUser || !_currentUser.roles) return 'GUEST';
        if (hasRole('ADMIN')) return 'ADMIN';
        if (hasRole('LOGISTICS_COORDINATOR')) return 'LOGISTICS_COORDINATOR';
        if (hasRole('WAREHOUSE_MANAGER')) return 'WAREHOUSE_MANAGER';
        if (hasRole('CUSTOMS_AGENT')) return 'CUSTOMS_AGENT';
        if (hasRole('VENDOR_REPRESENTATIVE')) return 'VENDOR_REPRESENTATIVE';
        if (hasRole('CUSTOMER')) return 'CUSTOMER';
        return 'USER';
    }

    return {
        login,
        restoreSession,
        logout,
        getUser,
        isAuthenticated,
        hasRole,
        isCustomer,
        isVendorRep,
        isStaff,
        getPrimaryRoleName
    };
})();
