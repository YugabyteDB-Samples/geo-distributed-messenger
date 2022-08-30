package com.yugabyte.app.messenger.data.entity;

import java.io.Serializable;

public class GeoId implements Serializable {
    private Integer id;

    private String countryCode;

    public GeoId() {
    }

    public GeoId(Integer id, String countryCode) {
        this.id = id;
        this.countryCode = countryCode;
    }

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((countryCode == null) ? 0 : countryCode.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GeoId other = (GeoId) obj;
        if (countryCode == null) {
            if (other.countryCode != null)
                return false;
        } else if (!countryCode.equals(other.countryCode))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
