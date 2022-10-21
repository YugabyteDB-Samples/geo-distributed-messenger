package com.yugabyte.app.messenger.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Message;

public interface MessageRepository extends JpaRepository<Message, GeoId> {

    public List<Message> findByChannelIdAndCountryCodeOrderByIdAsc(Integer channelId, String countryCode);
}
