package com.jiat.globaltrade.web.dto;

import com.jiat.globaltrade.entity.Vendor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Safe, detached REST response DTO for Vendor entities.
 */
public class VendorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String vendorCode;
    private String companyName;
    private String contactName;
    private String email;
    private String phone;
    private String country;
    private String status;
    private BigDecimal performanceRating;
    private LocalDateTime createdAt;

    public VendorResponse() {
    }

    public static VendorResponse fromEntity(Vendor vendor) {
        if (vendor == null) {
            return null;
        }
        VendorResponse dto = new VendorResponse();
        dto.setId(vendor.getId());
        dto.setVendorCode(vendor.getVendorCode());
        dto.setCompanyName(vendor.getCompanyName());
        dto.setContactName(vendor.getContactName());
        dto.setEmail(vendor.getEmail());
        dto.setPhone(vendor.getPhone());
        dto.setCountry(vendor.getCountry());
        dto.setStatus(vendor.getStatus() != null ? vendor.getStatus().name() : null);
        dto.setPerformanceRating(vendor.getPerformanceRating());
        dto.setCreatedAt(vendor.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(BigDecimal performanceRating) {
        this.performanceRating = performanceRating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
