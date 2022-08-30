package com.yugabyte.app.messenger.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, GeoId> {

}
