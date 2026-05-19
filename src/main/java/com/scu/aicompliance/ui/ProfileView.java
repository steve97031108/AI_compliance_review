package com.scu.aicompliance.ui;

import com.scu.aicompliance.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 个人信息页面 — 所有已登录用户可修改用户名和密码。
 */
@Route(value = "profile", layout = MainLayout.class)
@PermitAll
public class ProfileView extends VerticalLayout {

    private final UserService userService;

    private final TextField usernameField = new TextField("当前用户名");
    private final TextField newUsernameField = new TextField("新用户名");
    private final PasswordField oldPassword = new PasswordField("当前密码");
    private final PasswordField newPassword = new PasswordField("新密码");
    private final PasswordField confirmPassword = new PasswordField("确认密码");

    private final Button updateUsernameBtn = new Button("修改用户名");
    private final Button updatePasswordBtn = new Button("修改密码");

    private String currentUsername;

    public ProfileView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        currentUsername = auth.getName();

        H2 title = new H2("个人信息");
        title.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        // 用户名区域
        usernameField.setValue(currentUsername);
        usernameField.setReadOnly(true);
        usernameField.setWidthFull();

        newUsernameField.setPlaceholder("3-20个字符，字母/数字/下划线");
        newUsernameField.setWidthFull();

        updateUsernameBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        updateUsernameBtn.addClickListener(e -> handleUpdateUsername());

        H2 usernameSection = new H2("修改用户名");
        usernameSection.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin-top", "var(--lumo-space-l)");

        // 密码区域
        oldPassword.setPlaceholder("请输入当前密码");
        oldPassword.setWidthFull();

        newPassword.setPlaceholder("至少6位新密码");
        newPassword.setWidthFull();

        confirmPassword.setPlaceholder("再次输入新密码");
        confirmPassword.setWidthFull();

        updatePasswordBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        updatePasswordBtn.addClickListener(e -> handleUpdatePassword());

        H2 passwordSection = new H2("修改密码");
        passwordSection.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin-top", "var(--lumo-space-xl)");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        form.add(newUsernameField);
        form.setWidth("100%");
        form.getStyle().set("max-width", "600px");

        add(title,
            usernameField,
            usernameSection,
            form, updateUsernameBtn,
            passwordSection,
            oldPassword, newPassword, confirmPassword,
            updatePasswordBtn);
    }

    /**
     * 处理用户名修改，成功后弹窗提示重新登录。
     */
    private void handleUpdateUsername() {
        String newUsername = newUsernameField.getValue().trim();

        if (newUsername.isEmpty()) {
            newUsernameField.setErrorMessage("请输入新用户名");
            newUsernameField.setInvalid(true);
            return;
        }
        if (newUsername.equals(currentUsername)) {
            Notification.show("新用户名不能与当前用户名相同", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        String error = userService.updateUsername(currentUsername, newUsername);
        if (error != null) {
            Notification.show(error, 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            Notification.show("用户名已修改为 '" + newUsername + "'，请重新登录",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // 延迟后强制登出，让用户用新用户名重新登录
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(function(){ window.location.href = 'logout'; }, 2500);"
            );
        }
    }

    /**
     * 处理密码修改。
     */
    private void handleUpdatePassword() {
        String oldPwd = oldPassword.getValue();
        String newPwd = newPassword.getValue();
        String confirmPwd = confirmPassword.getValue();

        // 客户端校验
        if (oldPwd.isEmpty()) {
            oldPassword.setErrorMessage("请输入当前密码");
            oldPassword.setInvalid(true);
            return;
        }
        if (newPwd.isEmpty()) {
            newPassword.setErrorMessage("请输入新密码");
            newPassword.setInvalid(true);
            return;
        }
        if (newPwd.length() < 6) {
            newPassword.setErrorMessage("新密码长度不能少于6位");
            newPassword.setInvalid(true);
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            confirmPassword.setErrorMessage("两次输入的密码不一致");
            confirmPassword.setInvalid(true);
            return;
        }

        String error = userService.updatePassword(currentUsername, oldPwd, newPwd);
        if (error != null) {
            Notification.show(error, 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            Notification.show("密码修改成功！请重新登录",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // 清空密码字段
            oldPassword.clear();
            newPassword.clear();
            confirmPassword.clear();

            // 延迟后强制登出
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(function(){ window.location.href = 'logout'; }, 2500);"
            );
        }
    }
}