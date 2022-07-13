package com.yugabyte.app.messenger.event;

import com.vaadin.flow.component.ComponentEvent;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.views.WorkspaceView;

public class ChannelChangeEvent extends ComponentEvent<WorkspaceView> {

    private Channel selectedChannel;

    public ChannelChangeEvent(WorkspaceView source, boolean fromClient, Channel selectedChanel) {
        super(source, fromClient);
        this.selectedChannel = selectedChanel;
    }

    public Channel getSelectedChannel() {
        return selectedChannel;
    }

    public void setSelectedChannel(Channel selectedChannel) {
        this.selectedChannel = selectedChannel;
    }
}
