package com.yugabyte.app.messenger.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.yugabyte.app.messenger.data.entity.Channel;
import com.yugabyte.app.messenger.data.entity.Profile;
import com.yugabyte.app.messenger.data.entity.Workspace;
import com.yugabyte.app.messenger.data.service.MessagingService;
import com.yugabyte.app.messenger.security.AuthenticatedUser;

import java.util.List;
import java.util.Optional;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    private H1 viewTitle;

    private AuthenticatedUser authenticatedUser;

    private MessagingService messengingService;

    public MainLayout(AuthenticatedUser authenticatedUser, MessagingService messengingService) {
        this.authenticatedUser = authenticatedUser;
        this.messengingService = messengingService;

        setPrimarySection(Section.DRAWER);

        addToNavbar(true, createHeaderContent());
        addToDrawer(createDrawerContent());
    }

    private Component createHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.addClassNames("view-toggle");
        toggle.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames("view-title");

        Header header = new Header(toggle, viewTitle);
        header.addClassNames("view-header");
        return header;
    }

    private Component createDrawerContent() {
        Optional<Profile> maybeUser = authenticatedUser.get();
        com.vaadin.flow.component.html.Section section;

        if (maybeUser.isPresent()) {
            Profile user = maybeUser.get();

            List<Workspace> workspaces = messengingService.getUserWorkspaces(user);

            section = new com.vaadin.flow.component.html.Section(
                    createChannelsList(user, workspaces), createFooter());
        } else {
            H2 appName = new H2("Login Required");
            appName.addClassNames("app-name");

            section = new com.vaadin.flow.component.html.Section(appName, createFooter());
        }

        section.addClassNames("drawer-section");

        return section;
    }

    private Component createChannelsList(Profile user, List<Workspace> workspaces) {
        WorkspaceView channelsView = new WorkspaceView(workspaces, messengingService);
        channelsView.addClassName("channels-list");

        return channelsView;
    }

    private Footer createFooter() {
        Footer layout = new Footer();
        layout.addClassNames("app-nav-footer");

        Optional<Profile> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            Profile user = maybeUser.get();

            Avatar avatar = new Avatar(user.getFullName(), user.getUserPictureUrl());
            avatar.addClassNames("me-xs");

            ContextMenu userMenu = new ContextMenu(avatar);
            userMenu.setOpenOnClick(true);
            userMenu.addItem("Logout", e -> {
                authenticatedUser.logout();
            });

            Span name = new Span(user.getFullName());
            name.addClassNames("font-medium", "text-s", "text-secondary");

            layout.add(avatar, name);
        } else {
            Anchor loginLink = new Anchor("login", "Sign in");
            layout.add(loginLink);
        }

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
