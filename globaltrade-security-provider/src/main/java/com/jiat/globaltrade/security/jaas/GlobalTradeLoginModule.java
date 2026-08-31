package com.jiat.globaltrade.security.jaas;

import com.sun.appserv.security.AppservPasswordLoginModule;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import javax.sql.DataSource;

/**
 * Custom JAAS Password Login Module for GlobalTrade Supply Chain Management System.
 * Extends com.sun.appserv.security.AppservPasswordLoginModule.
 *
 * Enforces:
 * 1. Username & password non-empty validation
 * 2. Database user existence in 'app_users'
 * 3. Active account enforcement ('app_users.active == true')
 * 4. SHA-256 lowercase hexadecimal password digest verification
 * 5. Dynamic group/role mapping from 'user_roles'
 * 6. Commit to Payara container security context via commitUserAuthentication(groups)
 */
public class GlobalTradeLoginModule extends AppservPasswordLoginModule {

    private static final Logger LOGGER = Logger.getLogger(GlobalTradeLoginModule.class.getName());

    private static final String DEFAULT_DATASOURCE_JNDI = "jdbc/GlobalTradeDS";

    @Override
    protected void authenticateUser() throws LoginException {
        // 1. Obtain and validate credentials from inherited fields
        if (_username == null || _username.trim().isEmpty() || _password == null || _password.isEmpty()) {
            LOGGER.log(Level.WARNING, "[GlobalTradeLoginModule] Authentication rejected: Empty username or password.");
            throw new LoginException("Invalid credentials.");
        }

        String username = _username.trim();
        String jndiName = DEFAULT_DATASOURCE_JNDI;
        String userTable = "app_users";
        String userCol = "username";
        String passCol = "password_hash";
        String activeCol = "active";
        String groupTable = "user_roles";
        String groupUserCol = "username";
        String groupNameCol = "role_name";

        if (_currentRealm instanceof GlobalTradeCustomRealm realm) {
            if (realm.getDatasourceJndi() != null) jndiName = realm.getDatasourceJndi();
            if (realm.getUserTable() != null) userTable = realm.getUserTable();
            if (realm.getUserNameColumn() != null) userCol = realm.getUserNameColumn();
            if (realm.getPasswordColumn() != null) passCol = realm.getPasswordColumn();
            if (realm.getGroupTable() != null) groupTable = realm.getGroupTable();
            if (realm.getGroupTableUserNameColumn() != null) groupUserCol = realm.getGroupTableUserNameColumn();
            if (realm.getGroupNameColumn() != null) groupNameCol = realm.getGroupNameColumn();
        }

        DataSource ds = lookupDataSource(jndiName);

        String userSql = String.format("SELECT %s, %s FROM %s WHERE %s = ?", passCol, activeCol, userTable, userCol);
        String groupSql = String.format("SELECT %s FROM %s WHERE %s = ?", groupNameCol, groupTable, groupUserCol);

        String storedPasswordHash = null;
        boolean isActive = false;
        List<String> userGroups = new ArrayList<>();

        try (Connection conn = ds.getConnection()) {

            // 2. Query user credentials and active status
            try (PreparedStatement psUser = conn.prepareStatement(userSql)) {
                psUser.setString(1, username);
                try (ResultSet rsUser = psUser.executeQuery()) {
                    if (rsUser.next()) {
                        storedPasswordHash = rsUser.getString(1);
                        isActive = rsUser.getBoolean(2);
                    } else {
                        LOGGER.log(Level.WARNING, "[GlobalTradeLoginModule] Authentication failed: User ''{0}'' not found in {1}.",
                                new Object[]{username, userTable});
                        throw new LoginException("Invalid credentials.");
                    }
                }
            }

            // 3. Enforce active user status
            if (!isActive) {
                LOGGER.log(Level.WARNING, "[GlobalTradeLoginModule] Authentication failed: User ''{0}'' account is deactivated (active=false).",
                        username);
                throw new LoginException("Invalid credentials.");
            }

            // 4. Verify password hash using SHA-256 lowercase hex
            String computedHash = computeSha256Hex(_password);
            if (storedPasswordHash == null || !storedPasswordHash.equalsIgnoreCase(computedHash)) {
                LOGGER.log(Level.WARNING, "[GlobalTradeLoginModule] Authentication failed: Invalid password for user ''{0}''.", username);
                throw new LoginException("Invalid credentials.");
            }

            // 5. Query user groups / roles
            try (PreparedStatement psGroup = conn.prepareStatement(groupSql)) {
                psGroup.setString(1, username);
                try (ResultSet rsGroup = psGroup.executeQuery()) {
                    while (rsGroup.next()) {
                        String role = rsGroup.getString(1);
                        if (role != null && !role.trim().isEmpty()) {
                            userGroups.add(role.trim());
                        }
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[GlobalTradeLoginModule] Database error during authentication for user: " + username, e);
            throw new LoginException("Authentication service unavailable.");
        }

        // 6. Commit successful authentication to container
        String[] groupArray = userGroups.toArray(new String[0]);
        commitUserAuthentication(groupArray);

        LOGGER.log(Level.INFO, "[GlobalTradeLoginModule] Authentication SUCCESS for user ''{0}''. Assigned roles: {1}",
                new Object[]{username, userGroups});
    }

    private DataSource lookupDataSource(String jndiName) throws LoginException {
        try {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup(jndiName);
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, "[GlobalTradeLoginModule] JNDI DataSource lookup failed for: " + jndiName, e);
            throw new LoginException("Authentication DataSource configuration error.");
        }
    }

    private String computeSha256Hex(String input) throws LoginException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "[GlobalTradeLoginModule] SHA-256 algorithm not available in JVM", e);
            throw new LoginException("Security digest algorithm unavailable.");
        }
    }
}
