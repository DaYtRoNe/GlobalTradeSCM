package com.jiat.globaltrade.service;

import jakarta.ejb.Stateless;

@Stateless
public class SystemHealthBean {

    public String getStatus() {
        return "GlobalTrade EJB Module is running";
    }
}