package com.yugabyte.app.messenger.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(WorkspaceProfileKey.class)
public class WorkspaceProfile {
    @Id
    @Column(name = "profile_id")
    private Integer profileId;

    @Id
    @Column(name = "workspace_id")
    private Integer workspaceId;

    @Id
    @Column(name = "workspace_country")
    private String workspaceCountry;

    public Integer getProfileId() {
        return profileId;
    }

    public void setProfileId(Integer profileId) {
        this.profileId = profileId;
    }

    public Integer getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Integer workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceCountry() {
        return workspaceCountry;
    }

    public void setWorkspaceCountry(String workspaceCountry) {
        this.workspaceCountry = workspaceCountry;
    }
}
