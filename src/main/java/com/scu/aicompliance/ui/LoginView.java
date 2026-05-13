package com.scu.aicompliance.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(true);
        setPadding(true);

        // 设置页面背景色
        getStyle().set("background-color", "var(--lumo-shade-5pct)");

        // 标题
        H1 title = new H1("AI安全合规对话系统");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin-bottom", "var(--lumo-space-s)");

        // 副标题
        Paragraph subtitle = new Paragraph("AI 驱动的内容合规智能审查平台");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("margin-bottom", "var(--lumo-space-l)");

        // 登录表单
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.setAction("login");

        // 登录表单卡片容器
        VerticalLayout card = new VerticalLayout(title, subtitle, loginForm);
        card.setAlignItems(Alignment.CENTER);
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidth("400px");
        card.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-l)")
                .set("padding", "var(--lumo-space-xl)");

        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }
}