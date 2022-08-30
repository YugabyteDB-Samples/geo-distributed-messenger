package com.yugabyte.app.messenger.data.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(WorkspaceProfileKey.class)
public class WorkspaceProfile {
    @Id
    private Integer profileId;

    @Id
    private Integer workspaceId;

    @Id
    private String workspaceCountry;

    private String profileCountry;

    public String getProfileCountry() {
        return profileCountry;
    }

    public void setProfileCountry(String profileCountry) {
        this.profileCountry = profileCountry;
    }

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
