package com.jiat.globaltrade.entity.enums;

/**
 * Lifecycle status of a persistent supply chain alert.
 */
public enum SupplyChainAlertStatus {

    /** Problem condition actively detected and awaiting operational attention. */
    OPEN,

    /** Alert acknowledged by authorized personnel while condition remains active. */
    ACKNOWLEDGED,

    /** Problem condition has cleared or was successfully resolved. */
    RESOLVED
}
