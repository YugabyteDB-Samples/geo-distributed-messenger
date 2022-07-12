package com.yugabyte.app.messenger.views;

import java.util.List;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.event.ChannelChangeEvent;

public class ChannelsView extends VerticalLayout {

    private ListBox<Channel> channelsList;
    private Channel selectedChannel;

    public ChannelsView(List<Channel> channels) {
        channelsList = new ListBox<>();
        channelsList.setItems(channels);

        selectedChannel = channels.get(0);
        channelsList.setValue(selectedChannel);

        channelsList.setItemLabelGenerator(new ItemLabelGenerator<Channel>() {
            @Override
            public String apply(Channel item) {
                return item.getName();
            }
        });

        channelsList.addValueChangeListener(event -> {
            Channel selectedChannel = event.getHasValue().getValue();
            this.selectedChannel = selectedChannel;

            ChannelChangeEvent channelChangeEvent = new ChannelChangeEvent(this,
                    true, selectedChannel);

            ComponentUtil.fireEvent(UI.getCurrent(), channelChangeEvent);
        });

        add(channelsList);
        setSizeFull();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        ChannelChangeEvent channelChangeEvent = new ChannelChangeEvent(this,
                true, selectedChannel);

        ComponentUtil.fireEvent(UI.getCurrent(), channelChangeEvent);
    }
}
