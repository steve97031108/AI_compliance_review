package com.scu.aicompliance.ui;

import com.scu.aicompliance.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView(UserService userService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(true);
        setPadding(true);

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

        // 用户注册按钮
        Button registerButton = new Button("创建新账户");
        registerButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        registerButton.getStyle()
                .set("margin-top", "var(--lumo-space-m)")
                .set("width", "100%");
        registerButton.addClickListener(e -> openRegisterDialog(userService));

        // 登录表单卡片容器
        VerticalLayout card = new VerticalLayout(title, subtitle, loginForm, registerButton);
        card.setAlignItems(Alignment.CENTER);
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidth("420px");
        card.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-l)")
                .set("padding", "var(--lumo-space-xl)");

        add(card);
    }

    /**
     * 打开用户注册弹窗。
     */
    private void openRegisterDialog(UserService userService) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("创建新账户");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        TextField regUsername = new TextField("用户名");
        regUsername.setPlaceholder("3-20个字符，字母/数字/下划线");
        regUsername.setWidthFull();
        regUsername.setRequired(true);

        PasswordField regPassword = new PasswordField("密码");
        regPassword.setPlaceholder("至少6位密码");
        regPassword.setWidthFull();
        regPassword.setRequired(true);

        PasswordField regPasswordConfirm = new PasswordField("确认密码");
        regPasswordConfirm.setPlaceholder("再次输入密码");
        regPasswordConfirm.setWidthFull();
        regPasswordConfirm.setRequired(true);

        Paragraph hint = new Paragraph("注册后需要管理员审批通过才能登录使用。");
        hint.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-top", "var(--lumo-space-s)");

        Button submitButton = new Button("提交注册");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();

        Button cancelButton = new Button("取消");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.setWidthFull();

        submitButton.addClickListener(e -> {
            String username = regUsername.getValue().trim();
            String password = regPassword.getValue();
            String passwordConfirm = regPasswordConfirm.getValue();

            // 客户端校验
            if (username.isEmpty()) {
                regUsername.setErrorMessage("请输入用户名");
                regUsername.setInvalid(true);
                return;
            }
            if (password.isEmpty()) {
                regPassword.setErrorMessage("请输入密码");
                regPassword.setInvalid(true);
                return;
            }
            if (!password.equals(passwordConfirm)) {
                regPasswordConfirm.setErrorMessage("两次输入的密码不一致");
                regPasswordConfirm.setInvalid(true);
                return;
            }

            // 注册
            String error = userService.registerUser(username, password);
            if (error != null) {
                Notification.show(error, 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } else {
                dialog.close();
                Notification.show(
                        "账户 '" + username + "' 注册成功！请等待管理员审批。",
                        5000,
                        Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        });

        VerticalLayout dialogLayout = new VerticalLayout(
                regUsername, regPassword, regPasswordConfirm, hint,
                new HorizontalLayout(submitButton, cancelButton)
        );
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);
        dialogLayout.setWidth("360px");

        cancelButton.addClickListener(e -> dialog.close());
        dialog.add(dialogLayout);
        dialog.open();
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