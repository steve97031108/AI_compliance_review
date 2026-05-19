package com.scu.aicompliance.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.ServletException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 主布局 — 顶部导航栏 + 侧边栏菜单，作为所有页面的父布局。
 */
@AnonymousAllowed
public class MainLayout extends AppLayout {

    public MainLayout() {
        configureHeader();
        configureDrawer();
    }

    /**
     * 配置顶部导航栏。
     */
    private void configureHeader() {
        // 系统标题
        H2 title = new H2("AI安全合规对话系统");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("margin", "0 var(--lumo-space-m)");

        // 收起/展开侧边栏按钮
        DrawerToggle drawerToggle = new DrawerToggle();

        // 用户信息区域
        HorizontalLayout userArea = buildUserArea();

        HorizontalLayout header = new HorizontalLayout(drawerToggle, title, userArea);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.getStyle()
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        addToNavbar(header);
    }

    /**
     * 配置侧边抽屉导航。
     */
    private void configureDrawer() {
        SideNav sideNav = new SideNav();
        sideNav.setWidthFull();

        // 公共菜单项 — 每个 SideNavItem 都设置 width:100% 以撑满整行
        SideNavItem chatItem = new SideNavItem("对话", "/chat",
                VaadinIcon.CHAT.create());
        styleNavItem(chatItem);
        SideNavItem logsItem = new SideNavItem("日志查询", "/logs",
                VaadinIcon.SEARCH.create());
        styleNavItem(logsItem);
        SideNavItem statsItem = new SideNavItem("合规统计", "/statistics",
                VaadinIcon.CHART.create());
        styleNavItem(statsItem);

        sideNav.addItem(chatItem, logsItem, statsItem);

        // 所有已登录用户都能看到"个人信息"
        SideNavItem profileItem = new SideNavItem("个人信息", "/profile",
                VaadinIcon.USER.create());
        styleNavItem(profileItem);
        sideNav.addItem(profileItem);

        // ADMIN 角色才显示
        if (isCurrentUserAdmin()) {
            SideNavItem convItem = new SideNavItem("对话审查", "/admin/conversations",
                    VaadinIcon.RECORDS.create());
            styleNavItem(convItem);
            sideNav.addItem(convItem);

            SideNavItem userMgmtItem = new SideNavItem("用户管理", "/admin/users",
                    VaadinIcon.USERS.create());
            styleNavItem(userMgmtItem);
            sideNav.addItem(userMgmtItem);

            SideNavItem settingsItem = new SideNavItem("系统设置", "/settings",
                    VaadinIcon.COG.create());
            styleNavItem(settingsItem);
            sideNav.addItem(settingsItem);
        }

        VerticalLayout drawer = new VerticalLayout(sideNav);
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.setWidthFull();
        drawer.getStyle().set("padding-top", "var(--lumo-space-m)");

        addToDrawer(drawer);
    }

    /**
     * 设置 SideNavItem 撑满整行样式。
     */
    private void styleNavItem(SideNavItem item) {
        item.getStyle()
                .set("width", "100%")
                .set("justify-content", "flex-start")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
        // 通过 JS 设置内部链接元素样式以确保撑满
        item.getElement().executeJs(
                "this.shadowRoot && this.shadowRoot.querySelector('a') && " +
                "(this.shadowRoot.querySelector('a').style.display='block');"
        );
    }

    /**
     * 构建顶部右侧用户信息 + 退出按钮区域。
     */
    private HorizontalLayout buildUserArea() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated())
                ? auth.getName()
                : "未登录";
        String role = getCurrentUserRoleDisplay();

        Span userLabel = new Span(username + " (" + role + ")");
        userLabel.getStyle()
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-right", "var(--lumo-space-m)");

        Icon logoutIcon = VaadinIcon.SIGN_OUT.create();
        logoutIcon.getStyle().set("margin-right", "var(--lumo-space-xs)");

        Button logoutBtn = new Button("退出登录", logoutIcon, e -> handleLogout());
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        logoutBtn.getStyle()
                .set("color", "var(--lumo-error-text-color)")
                .set("font-weight", "500")
                .set("cursor", "pointer");

        HorizontalLayout userArea = new HorizontalLayout(userLabel, logoutBtn);
        userArea.setAlignItems(FlexComponent.Alignment.CENTER);
        userArea.setSpacing(false);
        return userArea;
    }

    /**
     * 判断当前用户是否为 ADMIN 角色。
     */
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));
    }

    /**
     * 获取当前用户角色显示名称。
     */
    private String getCurrentUserRoleDisplay() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "";
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::shortRoleName)
                .findFirst()
                .orElse("USER");
    }

    /**
     * 处理退出登录 — 使用 Servlet 级别登出，绕过 Vaadin 路由。
     */
    private void handleLogout() {
        try {
            VaadinServletRequest.getCurrent().getHttpServletRequest().logout();
        } catch (ServletException ignored) {
            // 忽略登出异常
        }
        getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
    }

    private String shortRoleName(String authority) {
        if (authority == null) return "USER";
        if (authority.startsWith("ROLE_")) {
            return authority.substring(5);
        }
        return authority;
    }
}