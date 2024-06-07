package ru.mai.views.login;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.extern.slf4j.Slf4j;
import ru.mai.views.chatrooms.ChatroomsView;

@Slf4j
@Route(value = "")
public class LoginView extends VerticalLayout {
    public LoginView() {
        TextField login = new TextField("Login");

        Button loginButton = new Button("Log in");

        loginButton.addClickListener(event -> {
            String username = login.getValue();

            if (username.isEmpty()) {
                Notification.show("Login cannot be empty").addThemeVariants(NotificationVariant.LUMO_ERROR);
            } else {
                Notification.show("Log in successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate(ChatroomsView.class, username));
            }
        });

        setSizeFull();
        add(login, loginButton);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    }

}
