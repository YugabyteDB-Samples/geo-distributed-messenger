package com.yugabyte.app.messenger.data.repository;

import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, GeoId> {
    public Profile findByEmail(String email);
}