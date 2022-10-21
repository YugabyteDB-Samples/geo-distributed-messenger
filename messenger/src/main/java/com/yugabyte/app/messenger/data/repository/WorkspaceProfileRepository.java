package com.yugabyte.app.messenger.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yugabyte.app.messenger.data.entity.WorkspaceProfile;
import com.yugabyte.app.messenger.data.entity.WorkspaceProfileKey;

public interface WorkspaceProfileRepository extends JpaRepository<WorkspaceProfile, WorkspaceProfileKey> {
    List<WorkspaceProfile> findByWorkspaceIdAndWorkspaceCountry(Integer workspaceId, String workspaceCountry);

    List<WorkspaceProfile> findByProfileId(Integer profileId);
}
