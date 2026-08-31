package com.jiat.globaltrade.web.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for UI authentication requests.
 */
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
