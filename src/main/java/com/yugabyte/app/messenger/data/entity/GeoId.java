package com.yugabyte.app.messenger.data.entity;

import java.io.Serializable;

public class GeoId implements Serializable {
    private Integer id;

    private String countryCode;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    
}
