package com.yugabyte.app.messenger.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.GeoId;

public interface ChannelRepository extends JpaRepository<Channel, GeoId>{
    
}
