package com.yugabyte.app.messenger.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotEmpty;

@Entity
@IdClass(GeoId.class)
public class Channel {
    @Id
    private Integer id;

    @Id
    @Column(name = "country_code")
    private String countryCode;

    @NotEmpty
    private String name;

    @NotEmpty
    @Column(name = "workspace_id")
    private Integer workspaceId;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Integer workspaceId) {
        this.workspaceId = workspaceId;
    }
}