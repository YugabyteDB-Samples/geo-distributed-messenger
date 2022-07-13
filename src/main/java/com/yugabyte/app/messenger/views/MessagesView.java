package com.yugabyte.app.messenger.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
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
import com.yugabyte.app.messenger.security.AuthenticatedUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.security.PermitAll;

import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Geo-Distributed Messenger")
@Route(value = "messages", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PermitAll
public class MessagesView extends Section {

    private Channel currentChannel;
    private List<MessageListItem> currentMessages;

    private MessageList messageList;
    private TextArea newMessageArea;
    private Button sendMessageButton;

    private Registration registration;

    @Autowired
    MessagingService messagingService;

    @Autowired
    ProfileService profileService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    public MessagesView() {
        HorizontalLayout newMessageLayout = createSendNewMessageSection();

        createMessagesListSection();

        addClassName("app-message-view");
        add(messageList, newMessageLayout);
    }

    private HorizontalLayout createSendNewMessageSection() {
        HorizontalLayout newMessageLayout = new HorizontalLayout();

        newMessageArea = new TextArea();
        newMessageArea.setWidthFull();
        newMessageArea.setValueChangeMode(ValueChangeMode.EAGER);

        sendMessageButton = new Button("Send");
        sendMessageButton.addClickListener(e -> {
            Optional<Profile> userOptional = authenticatedUser.get();

            if (userOptional.isPresent()) {
                Profile user = userOptional.get();

                Message newMessage = new Message();

                newMessage.setChannelId(currentChannel.getId());
                newMessage.setCountryCode(currentChannel.getCountryCode());
                newMessage.setSenderId(user.getId());
                newMessage.setSenderCountryCode(user.getCountryCode());

                newMessage.setMessage(newMessageArea.getValue().trim());

                newMessage = messagingService.addMessage(newMessage);

                if (newMessage != null) {
                    MessageListItem newMessageListItem = createMessageListItem(newMessage);
                    currentMessages.add(newMessageListItem);

                    messageList.setItems(currentMessages);
                    newMessageArea.setValue("");
                } else {
                    Notification.show("Failed to send the message");
                }
            } else {
                Notification.show("Log in before sending messages!");
            }
        });
        sendMessageButton.addClickShortcut(Key.ENTER);

        newMessageLayout.addClassName("app-message-view-new-message-area");
        newMessageLayout.setPadding(true);
        newMessageLayout.add(newMessageArea, sendMessageButton);
        return newMessageLayout;
    }

    private void createMessagesListSection() {
        messageList = new MessageList();
        messageList.addClassName("app-message-view-list");
    }

    public void changeChannel(Channel newChannel) {
        currentChannel = newChannel;

        List<Message> messages = messagingService.getMessages(newChannel);
        List<MessageListItem> messageListItems = new ArrayList<>(messages.size());

        for (Message message : messages) {

            MessageListItem mItem = createMessageListItem(message);

            messageListItems.add(mItem);
        }

        currentMessages = messageListItems;
        messageList.setItems(messageListItems);
    }

    private MessageListItem createMessageListItem(Message message) {
        GeoId geoId = new GeoId();
        geoId.setCountryCode(message.getSenderCountryCode());
        geoId.setId(message.getSenderId());

        Optional<Profile> userProfile = profileService.get(geoId);

        MessageListItem mItem = new MessageListItem(message.getMessage(),
                message.getSentAt().toInstant(),
                userProfile.isPresent() ? userProfile.get().getFullName() : "Unknown");

        return mItem;
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
