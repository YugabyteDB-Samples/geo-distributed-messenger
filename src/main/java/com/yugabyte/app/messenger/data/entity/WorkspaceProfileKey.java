package com.yugabyte.app.messenger.data.entity;

import java.io.Serializable;

public class WorkspaceProfileKey implements Serializable {
    private Integer profileId;

    private Integer workspaceId;

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