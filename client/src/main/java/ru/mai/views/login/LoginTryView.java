package ru.mai.views.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import ru.mai.views.MainLayout;
import ru.mai.views.chatrooms.ChatroomsView;

@PageTitle("Login")
@Route(value = "login")
public class LoginTryView extends VerticalLayout {
    public LoginTryView() {
        TextField login = new TextField("Login");

        Button loginButton = new Button("Log in", event -> {
            String username = login.getValue();

            if (username.isEmpty()) {
                Notification.show("Login cannot be empty").addThemeVariants(NotificationVariant.LUMO_ERROR);
            } else {
                Notification.show("Log in successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().navigate(ChatroomsView.class, ChatroomsView.createParameter(username));
//                UI.getCurrent().navigate("chat/" + username);
//                UI.getCurrent().navigate(username);
            }
        });

        setSizeFull();
        add(login, loginButton);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    }

}
