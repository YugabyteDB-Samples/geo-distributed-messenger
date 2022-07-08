package com.yugabyte.app.messenger.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yugabyte.app.messenger.data.entity.WorkspaceProfile;
import com.yugabyte.app.messenger.data.entity.WorkspaceProfileKey;

public interface WorkspaceProfileRepository extends JpaRepository<WorkspaceProfile, WorkspaceProfileKey> {

}
