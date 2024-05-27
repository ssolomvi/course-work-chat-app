//package ru.mai.views;
//
//import com.vaadin.flow.component.Component;
//import com.vaadin.flow.component.applayout.AppLayout;
//import com.vaadin.flow.component.html.Div;
//import com.vaadin.flow.component.html.H1;
//import com.vaadin.flow.component.html.Header;
//import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.theme.lumo.LumoUtility.*;
//
///**
// * The main view is a top-level placeholder for other views.
// */
//public class MainLayout extends VerticalLayout {
//
//    public MainLayout() {
//        addToNavbar(createHeaderContent());
//        setDrawerOpened(false);
//    }
//
//    private Component createHeaderContent() {
//        Header header = new Header();
//        header.addClassNames(BoxSizing.BORDER, Display.FLEX, FlexDirection.COLUMN, Width.FULL);
//
//        Div layout = new Div();
//        layout.addClassNames(Display.FLEX, AlignItems.CENTER, Padding.Horizontal.LARGE);
//
//        H1 appName = new H1("Chat");
//        appName.addClassNames(Margin.Vertical.MEDIUM, Margin.End.AUTO, FontSize.LARGE);
//        layout.add(appName);
//
//
//        header.add(layout);
//        return header;
//    }
//}
