package com.scu.aicompliance.ui;

import com.scu.aicompliance.model.User;
import com.scu.aicompliance.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 用户管理页面 — 管理员审批待审核用户、查看所有用户、删除用户。
 */
@Route(value = "admin/users", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private final UserService userService;

    private final Grid<User> pendingGrid = new Grid<>(User.class, false);
    private final Grid<User> allUsersGrid = new Grid<>(User.class, false);

    private final Span pendingBadge = new Span();
    private final VerticalLayout pendingTab = new VerticalLayout();
    private final VerticalLayout allUsersTab = new VerticalLayout();

    public UserManagementView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("用户管理");
        title.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        configurePendingGrid();
        configureAllUsersGrid();

        // Tab 切换
        Tabs tabs = new Tabs(new Tab("待审批用户"), new Tab("全部用户"));
        pendingBadge.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background-color", "#FF4D4F")
                .set("color", "#FFFFFF")
                .set("border-radius", "10px")
                .set("padding", "2px 8px")
                .set("margin-left", "6px");

        pendingTab.add(pendingGrid);
        pendingTab.setSizeFull();
        allUsersTab.add(allUsersGrid);
        allUsersTab.setSizeFull();
        allUsersTab.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            int selectedIndex = tabs.getSelectedIndex();
            pendingTab.setVisible(selectedIndex == 0);
            allUsersTab.setVisible(selectedIndex == 1);
        });

        add(title, tabs, pendingBadge, pendingTab, allUsersTab);
        refreshTables();
    }

    private void configurePendingGrid() {
        pendingGrid.addColumn(User::getUsername).setHeader("用户名").setAutoWidth(true);
        pendingGrid.addColumn(u -> "待审批").setHeader("状态")
                .setAutoWidth(true)
                .setClassNameGenerator(u -> "pending-status");

        pendingGrid.addComponentColumn(user -> {
            Button approveBtn = new Button("通过");
            approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            approveBtn.addClickListener(e -> {
                if (userService.approveUser(user.getUsername())) {
                    Notification.show("用户 '" + user.getUsername() + "' 已审批通过",
                            3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshTables();
                }
            });

            Button rejectBtn = new Button("拒绝");
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            rejectBtn.addClickListener(e -> {
                Dialog confirmDialog = new Dialog();
                confirmDialog.setHeaderTitle("确认拒绝");
                Span msg = new Span("确定要拒绝用户 '" + user.getUsername() + "' 的注册申请吗？该操作不可撤销。");
                msg.getStyle().set("font-size", "var(--lumo-font-size-s)");

                Button confirmBtn = new Button("确认拒绝");
                confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                confirmBtn.addClickListener(ev -> {
                    userService.rejectUser(user.getUsername());
                    confirmDialog.close();
                    Notification.show("已拒绝用户 '" + user.getUsername() + "' 的注册申请",
                            3000, Notification.Position.MIDDLE);
                    refreshTables();
                });

                Button cancelBtn = new Button("取消");
                cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                cancelBtn.addClickListener(ev -> confirmDialog.close());

                confirmDialog.add(new VerticalLayout(msg,
                        new HorizontalLayout(confirmBtn, cancelBtn)));
                confirmDialog.open();
            });

            return new HorizontalLayout(approveBtn, rejectBtn);
        }).setHeader("操作").setAutoWidth(true);

        pendingGrid.setSizeFull();
        pendingGrid.getElement().setProperty("placeholder", "暂无待审批用户");
    }

    private void configureAllUsersGrid() {
        allUsersGrid.addColumn(User::getUsername).setHeader("用户名").setAutoWidth(true);
        allUsersGrid.addColumn(u -> u.getRole().equals("ADMIN") ? "管理员" : "普通用户")
                .setHeader("角色").setAutoWidth(true);
        allUsersGrid.addColumn(u -> "PENDING".equals(u.getStatus()) ? "待审批" : "正常")
                .setHeader("状态").setAutoWidth(true);

        allUsersGrid.addComponentColumn(user -> {
            String currentUser = SecurityContextHolder.getContext()
                    .getAuthentication().getName();

            Button deleteBtn = new Button("删除");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            // 不允许删除自己或管理员
            if (currentUser.equals(user.getUsername())) {
                deleteBtn.setEnabled(false);
                deleteBtn.setTooltipText("不能删除自己的账户");
            } else if ("ADMIN".equals(user.getRole())) {
                deleteBtn.setEnabled(false);
                deleteBtn.setTooltipText("不能删除管理员账户");
            } else {
                deleteBtn.addClickListener(e -> {
                    Dialog confirmDialog = new Dialog();
                    confirmDialog.setHeaderTitle("确认删除");
                    Span msg = new Span("确定要删除用户 '" + user.getUsername() + "' 吗？此操作不可撤销。");

                    Button confirmBtn = new Button("确认删除");
                    confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    confirmBtn.addClickListener(ev -> {
                        String error = userService.deleteUser(currentUser, user.getUsername());
                        confirmDialog.close();
                        if (error != null) {
                            Notification.show(error, 3000, Notification.Position.MIDDLE)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        } else {
                            Notification.show("用户 '" + user.getUsername() + "' 已删除",
                                    3000, Notification.Position.MIDDLE);
                            refreshTables();
                        }
                    });

                    Button cancelBtn = new Button("取消");
                    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    cancelBtn.addClickListener(ev -> confirmDialog.close());

                    confirmDialog.add(new VerticalLayout(msg,
                            new HorizontalLayout(confirmBtn, cancelBtn)));
                    confirmDialog.open();
                });
            }
            return new HorizontalLayout(deleteBtn);
        }).setHeader("操作").setAutoWidth(true);

        allUsersGrid.setSizeFull();
    }

    private void refreshTables() {
        List<User> pending = userService.findPendingUsers();
        pendingGrid.setItems(pending);

        int count = pending.size();
        if (count > 0) {
            pendingBadge.setText(String.valueOf(count));
            pendingBadge.setVisible(true);
        } else {
            pendingBadge.setVisible(false);
        }

        allUsersGrid.setItems(userService.getAllUsers());
    }
}