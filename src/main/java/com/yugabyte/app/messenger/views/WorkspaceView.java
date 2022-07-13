package com.yugabyte.app.messenger.views;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.Workspace;
import com.yugabyte.app.messenger.data.service.MessagingService;
import com.yugabyte.app.messenger.event.ChannelChangeEvent;

public class WorkspaceView extends VerticalLayout {

    private ComboBox<Workspace> wComboBox;
    private ListBox<Channel> channelsList;

    private Workspace selectedWorkspace;
    private Channel selectedChannel;

    private MessagingService messagingService;

    public WorkspaceView(List<Workspace> workspaces, MessagingService messagingService) {
        this.messagingService = messagingService;

        createWorkspacesComboBox(workspaces);

        createChannelsListBox();

        add(wComboBox, channelsList);
        setSizeFull();
    }

    private void createWorkspacesComboBox(List<Workspace> workspaces) {
        wComboBox = new ComboBox<>("Workspace");
        wComboBox.setItems(workspaces);

        selectedWorkspace = workspaces.get(0);
        wComboBox.setValue(selectedWorkspace);

        wComboBox.setItemLabelGenerator(workspace -> {
            return workspace.getName();
        });

        wComboBox.addValueChangeListener(event -> {
            selectedWorkspace = event.getHasValue().getValue();

            loadChannelsForSelectedWorkspace();
            notifyChannelSelectionChanged(true);
        });
    }

    private void createChannelsListBox() {
        channelsList = new ListBox<>();

        loadChannelsForSelectedWorkspace();

        channelsList.setItemLabelGenerator(channel -> {
            return channel.getName();
        });

        channelsList.addValueChangeListener(event -> {
            selectedChannel = event.getHasValue().getValue();

            if (selectedChannel != null) {
                notifyChannelSelectionChanged(true);
            }
        });
    }

    private void loadChannelsForSelectedWorkspace() {
        List<Channel> channels = messagingService.getWorkspaceChannels(selectedWorkspace);

        channelsList.setItems(channels);

        selectedChannel = channels.get(0);
        channelsList.setValue(selectedChannel);
    }

    private void notifyChannelSelectionChanged(boolean fromClient) {
        ChannelChangeEvent channelChangeEvent = new ChannelChangeEvent(this,
                fromClient, selectedChannel);

        ComponentUtil.fireEvent(UI.getCurrent(), channelChangeEvent);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        notifyChannelSelectionChanged(true);
    }
}
