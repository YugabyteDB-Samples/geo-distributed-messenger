package com.yugabyte.app.messenger.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.shared.Registration;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Message;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.service.MessagingService;
import com.yugabyte.app.messenger.data.service.ProfileService;
import com.yugabyte.app.messenger.event.ChannelChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.security.PermitAll;

import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Geo-Distributed Messenger")
@Route(value = "messages", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PermitAll
public class MessagesView extends HorizontalLayout {

    private MessageList messageList;
    private TextField newMessageField;
    private Button sayHello;

    private Registration registration;

    @Autowired
    MessagingService messagingService;

    @Autowired
    ProfileService profileService;

    public MessagesView() {
        messageList = new MessageList();

        newMessageField = new TextField("Your name");
        sayHello = new Button("Say hello");
        sayHello.addClickListener(e -> {
            Notification.show("Hello " + newMessageField.getValue());
        });
        sayHello.addClickShortcut(Key.ENTER);

        setMargin(true);
        setVerticalComponentAlignment(Alignment.END, messageList, newMessageField, sayHello);

        add(messageList, newMessageField, sayHello);
    }

    public void changeChannel(Channel newChannel) {
        List<Message> messages = messagingService.getMessages(newChannel);
        List<MessageListItem> messageListItems = new ArrayList<>(messages.size());

        for (Message message : messages) {

            GeoId geoId = new GeoId();
            geoId.setCountryCode(message.getSenderCountryCode());
            geoId.setId(message.getSenderId());

            Optional<Profile> userProfile = profileService.get(geoId);

            MessageListItem mItem = new MessageListItem(message.getMessage(),
                    message.getSentAt().toInstant(),
                    userProfile.isPresent() ? userProfile.get().getFullName() : "Uknown");

            messageListItems.add(mItem);
        }

        messageList.setItems(messageListItems);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registration = ComponentUtil.addListener(
                attachEvent.getUI(),
                ChannelChangeEvent.class,
                event -> {
                    changeChannel(event.getSelectedChannel());
                });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Unregister from the event bus
        registration.remove();
    }
}
