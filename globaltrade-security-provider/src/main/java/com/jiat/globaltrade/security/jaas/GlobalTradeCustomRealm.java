package com.jiat.globaltrade.security.jaas;

import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Custom Payara 6 / GlassFish Security Realm for GlobalTrade Supply Chain Management System.
 * Extends com.sun.appserv.security.AppservRealm.
 *
 * Configured with JAAS Context: GlobalTradeCustomJaas
 * Uses DataSource: jdbc/GlobalTradeDS
 * Authentication Table: app_users
 * Role / Group Table: user_roles
 */
public class GlobalTradeCustomRealm extends AppservRealm {

    private static final Logger LOGGER = Logger.getLogger(GlobalTradeCustomRealm.class.getName());

    public static final String AUTH_TYPE = "GlobalTradeCustomAuth";
    public static final String DEFAULT_JAAS_CONTEXT = "GlobalTradeCustomJaas";

    public static final String PARAM_DATASOURCE_JNDI = "datasource-jndi";
    public static final String DEFAULT_DATASOURCE_JNDI = "jdbc/GlobalTradeDS";

    public static final String PARAM_USER_TABLE = "user-table";
    public static final String DEFAULT_USER_TABLE = "app_users";

    public static final String PARAM_USER_NAME_COLUMN = "user-name-column";
    public static final String DEFAULT_USER_NAME_COLUMN = "username";

    public static final String PARAM_PASSWORD_COLUMN = "password-column";
    public static final String DEFAULT_PASSWORD_COLUMN = "password_hash";

    public static final String PARAM_GROUP_TABLE = "group-table";
    public static final String DEFAULT_GROUP_TABLE = "user_roles";

    public static final String PARAM_GROUP_NAME_COLUMN = "group-name-column";
    public static final String DEFAULT_GROUP_NAME_COLUMN = "role_name";

    public static final String PARAM_GROUP_TABLE_USER_NAME_COLUMN = "group-table-user-name-column";
    public static final String DEFAULT_GROUP_TABLE_USER_NAME_COLUMN = "username";

    public static final String PARAM_DIGEST_ALGORITHM = "digest-algorithm";
    public static final String DEFAULT_DIGEST_ALGORITHM = "SHA-256";

    public static final String PARAM_ENCODING = "encoding";
    public static final String DEFAULT_ENCODING = "hex";

    public static final String PARAM_CHARSET = "charset";
    public static final String DEFAULT_CHARSET = "UTF-8";

    private String datasourceJndi;
    private String userTable;
    private String userNameColumn;
    private String passwordColumn;
    private String groupTable;
    private String groupNameColumn;
    private String groupTableUserNameColumn;
    private String digestAlgorithm;
    private String encoding;
    private String charset;

    @Override
    public synchronized void init(Properties props) throws BadRealmException, NoSuchRealmException {
        super.init(props);

        // Bind default JAAS Context if not explicitly set
        String jaasCtx = props.getProperty(JAAS_CONTEXT_PARAM);
        if (jaasCtx == null || jaasCtx.trim().isEmpty()) {
            setProperty(JAAS_CONTEXT_PARAM, DEFAULT_JAAS_CONTEXT);
        }

        this.datasourceJndi = props.getProperty(PARAM_DATASOURCE_JNDI, DEFAULT_DATASOURCE_JNDI);
        this.userTable = props.getProperty(PARAM_USER_TABLE, DEFAULT_USER_TABLE);
        this.userNameColumn = props.getProperty(PARAM_USER_NAME_COLUMN, DEFAULT_USER_NAME_COLUMN);
        this.passwordColumn = props.getProperty(PARAM_PASSWORD_COLUMN, DEFAULT_PASSWORD_COLUMN);
        this.groupTable = props.getProperty(PARAM_GROUP_TABLE, DEFAULT_GROUP_TABLE);
        this.groupNameColumn = props.getProperty(PARAM_GROUP_NAME_COLUMN, DEFAULT_GROUP_NAME_COLUMN);
        this.groupTableUserNameColumn = props.getProperty(PARAM_GROUP_TABLE_USER_NAME_COLUMN, DEFAULT_GROUP_TABLE_USER_NAME_COLUMN);
        this.digestAlgorithm = props.getProperty(PARAM_DIGEST_ALGORITHM, DEFAULT_DIGEST_ALGORITHM);
        this.encoding = props.getProperty(PARAM_ENCODING, DEFAULT_ENCODING);
        this.charset = props.getProperty(PARAM_CHARSET, DEFAULT_CHARSET);

        LOGGER.log(Level.INFO, "[GlobalTradeCustomRealm] Initialized with JAAS Context: {0}, JNDI DataSource: {1}, User Table: {2}, Group Table: {3}",
                new Object[]{getJAASContext(), this.datasourceJndi, this.userTable, this.groupTable});
    }

    @Override
    public String getAuthType() {
        return AUTH_TYPE;
    }

    @Override
    public Enumeration<String> getGroupNames(String username) throws InvalidOperationException, NoSuchUserException {
        if (username == null || username.trim().isEmpty()) {
            return Collections.emptyEnumeration();
        }

        Vector<String> groups = new Vector<>();
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?",
                this.groupNameColumn, this.groupTable, this.groupTableUserNameColumn);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String role = rs.getString(1);
                    if (role != null && !role.trim().isEmpty()) {
                        groups.add(role.trim());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[GlobalTradeCustomRealm] Failed to query groups for user: " + username, e);
            throw new InvalidOperationException("Failed to query user groups from realm database.");
        }

        return groups.elements();
    }

    @Override
    public Enumeration<String> getUserNames() throws BadRealmException {
        Vector<String> users = new Vector<>();
        String sql = String.format("SELECT %s FROM %s", this.userNameColumn, this.userTable);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String u = rs.getString(1);
                if (u != null) {
                    users.add(u);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[GlobalTradeCustomRealm] Could not enumerate all user names: " + e.getMessage());
        }

        return users.elements();
    }

    @Override
    public Enumeration<String> getGroupNames() throws BadRealmException {
        Vector<String> groups = new Vector<>();
        String sql = String.format("SELECT DISTINCT %s FROM %s", this.groupNameColumn, this.groupTable);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String g = rs.getString(1);
                if (g != null) {
                    groups.add(g);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[GlobalTradeCustomRealm] Could not enumerate group names: " + e.getMessage());
        }

        return groups.elements();
    }

    @Override
    public boolean supportsUserManagement() {
        return false;
    }

    private Connection getConnection() throws Exception {
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup(this.datasourceJndi != null ? this.datasourceJndi : DEFAULT_DATASOURCE_JNDI);
        return ds.getConnection();
    }

    public String getDatasourceJndi() {
        return datasourceJndi;
    }

    public String getUserTable() {
        return userTable;
    }

    public String getUserNameColumn() {
        return userNameColumn;
    }

    public String getPasswordColumn() {
        return passwordColumn;
    }

    public String getGroupTable() {
        return groupTable;
    }

    public String getGroupNameColumn() {
        return groupNameColumn;
    }

    public String getGroupTableUserNameColumn() {
        return groupTableUserNameColumn;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getCharset() {
        return charset;
    }
}
