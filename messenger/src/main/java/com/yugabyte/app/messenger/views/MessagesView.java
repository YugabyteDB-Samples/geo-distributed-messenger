package com.yugabyte.app.messenger.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.GeoId;
import com.yugabyte.app.messenger.data.entity.Message;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.service.FileUploadService;
import com.yugabyte.app.messenger.data.service.MessagingService;
import com.yugabyte.app.messenger.data.service.ProfileService;
import com.yugabyte.app.messenger.event.ChannelChangeEvent;
import com.yugabyte.app.messenger.security.AuthenticatedUser;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;

import javax.annotation.security.PermitAll;

import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Geo-Distributed Messenger")
@Route(value = "messages", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PermitAll
public class MessagesView extends Section {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM);

    private Channel currentChannel;
    private List<Message> currentMessages;

    private ListBox<Message> messageList;
    private TextArea newMessageArea;
    private Button sendMessageButton;

    private Registration registration;

    @Autowired
    MessagingService messagingService;

    @Autowired
    ProfileService profileService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    @Autowired
    private FileUploadService uploadService;

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
            sendMessage(newMessageArea.getValue().trim(), false, true);
        });
        sendMessageButton.addClickShortcut(Key.ENTER);

        MemoryBuffer fileBuffer = new MemoryBuffer();
        Upload fileUpload = new Upload(fileBuffer);
        fileUpload.setAcceptedFileTypes("image/png", "image/jpg", ".jpg", ".jpeg");

        fileUpload.addFinishedListener(event -> {
            String fileName = event.getFileName();
            String mimeType = event.getMIMEType();

            Optional<String> fileUrl = uploadService.uploadAttachment(fileName, mimeType,
                    fileBuffer.getInputStream());

            if (fileUrl.isPresent()) {
                sendMessage(fileUrl.get(), true, false);
                fileUpload.clearFileList();
            } else {
                Notification.show("File uploading failed");
            }
        });

        newMessageLayout.addClassName("app-message-view-new-message-area");
        newMessageLayout.setPadding(true);
        newMessageLayout.add(newMessageArea, sendMessageButton, fileUpload);

        return newMessageLayout;
    }

    private void sendMessage(String message, boolean attachment, boolean clearMessageArea) {
        Optional<Profile> userOptional = authenticatedUser.get();

        if (userOptional.isPresent()) {
            Profile user = userOptional.get();

            Message newMessage = new Message();

            newMessage.setChannelId(currentChannel.getId());
            newMessage.setCountryCode(currentChannel.getCountryCode());
            newMessage.setSenderId(user.getId());
            newMessage.setSenderCountryCode(user.getCountryCode());

            newMessage.setMessage(message);
            newMessage.setAttachment(attachment);

            newMessage = messagingService.addMessage(newMessage);

            if (newMessage != null) {
                currentMessages.add(newMessage);
                messageList.setItems(currentMessages);

                messageList.getChildren().toList().get(
                        currentMessages.size() - 1).scrollIntoView();

                if (clearMessageArea)
                    newMessageArea.setValue("");
            } else {
                Notification.show("Failed to send the message");
            }
        } else {
            Notification.show("Log in before sending messages!");
        }
    }

    private void createMessagesListSection() {
        messageList = new ListBox<>();
        messageList.addClassName("app-message-view-list");

        messageList.setRenderer(new ComponentRenderer<>(message -> {
            HorizontalLayout row = new HorizontalLayout();

            GeoId geoId = new GeoId();
            geoId.setCountryCode(message.getSenderCountryCode());
            geoId.setId(message.getSenderId());

            Optional<Profile> userProfile = profileService.get(geoId);

            String userName = userProfile.isPresent() ? userProfile.get().getFullName() : "Unknown";

            Avatar avatar = new Avatar();
            avatar.setName(userName);

            Span nameSpan = new Span(userName);

            Span timeSpan = new Span(" " + dateTimeFormatter.format(message.getSentAt().toLocalDateTime()));
            timeSpan.getStyle().set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");

            HorizontalLayout messageTitle = new HorizontalLayout();
            messageTitle.add(nameSpan, timeSpan);
            messageTitle.setPadding(false);
            messageTitle.setSpacing(false);
            messageTitle.setAlignItems(FlexComponent.Alignment.CENTER);

            VerticalLayout content;

            if (!message.isAttachment()) {
                Span messageSpan = new Span(message.getMessage());
                messageSpan.getStyle()
                        .set("color", "var(--lumo-body-text-color)")
                        .set("font-size", "var(--lumo-font-size-m)");

                content = new VerticalLayout(messageTitle, messageSpan);
                content.setPadding(false);
                content.setSpacing(false);

                row.setAlignItems(FlexComponent.Alignment.CENTER);
            } else {
                Image image = new Image(message.getMessage(), "Some picture");
                image.setMaxHeight(200, Unit.PIXELS);
                content = new VerticalLayout(messageTitle, image);
                content.setPadding(false);
                content.setSpacing(false);
            }

            row.add(avatar, content);
            row.getStyle().set("line-height", "var(--lumo-line-height-m)");
            return row;
        }));
    }

    public void changeChannel(Channel newChannel) {
        currentChannel = newChannel;

        currentMessages = messagingService.getMessages(newChannel);
        messageList.setItems(currentMessages);

        if (currentMessages.size() > 0) {
            messageList.getChildren().toList().get(
                    currentMessages.size() - 1).scrollIntoView();
        }
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
