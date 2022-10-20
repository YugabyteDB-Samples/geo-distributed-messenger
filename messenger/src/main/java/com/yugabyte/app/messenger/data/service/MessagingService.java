package com.yugabyte.app.messenger.data.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yugabyte.app.messenger.data.DynamicDataSource;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Message;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.entity.Workspace;
import com.yugabyte.app.messenger.data.entity.WorkspaceProfile;
import com.yugabyte.app.messenger.data.repository.ChannelRepository;
import com.yugabyte.app.messenger.data.repository.MessageRepository;
import com.yugabyte.app.messenger.data.repository.SessionManagementRepository;
import com.yugabyte.app.messenger.data.repository.WorkspaceProfileRepository;
import com.yugabyte.app.messenger.data.repository.WorkspaceRepository;

@Service
public class MessagingService {
    @Autowired
    private ChannelRepository channelsRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceProfileRepository workspaceProfileRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SessionManagementRepository sManagementRepository;

    @Autowired
    private DynamicDataSource dataSource;

    public List<Channel> getWorkspaceChannels(Workspace workspace) {
        return channelsRepository.findByWorkspaceId(workspace.getId());
    }

    public List<Workspace> getUserWorkspaces(Profile user) {
        List<WorkspaceProfile> wProfiles = workspaceProfileRepository.findByProfileId(user.getId());

        List<GeoId> workspaceIds = new ArrayList<>(wProfiles.size());

        for (WorkspaceProfile wProfile : wProfiles) {
            GeoId id = new GeoId();
            id.setId(wProfile.getWorkspaceId());
            id.setCountryCode(wProfile.getWorkspaceCountry());

            workspaceIds.add(id);
        }

        return workspaceRepository.findAllById(workspaceIds);
    }

    public List<Message> getMessages(Channel channel) {
        return messageRepository.findByChannelIdOrderByIdAsc(channel.getId());
    }

    @Transactional
    public Message addMessage(Message newMessage) {
        if (dataSource.isReplicaConnection())
            sManagementRepository.switchToReadWriteTxMode();

        Message message = messageRepository.save(newMessage);

        return message;
    }

}
