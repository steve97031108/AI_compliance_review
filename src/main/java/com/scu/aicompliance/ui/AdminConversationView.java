package com.scu.aicompliance.ui;

import com.scu.aicompliance.model.ChatMessage;
import com.scu.aicompliance.service.ConversationStore;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 管理员对话审查页面 — 查看所有用户的对话历史和拦截情况。
 * <p>
 * 管理员可以从下拉列表中选择用户，查看该用户的完整对话记录（含合规审查拦截标记）。
 * </p>
 */
@Route(value = "admin/conversations", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminConversationView extends VerticalLayout {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    private final ConversationStore conversationStore;

    /** 用户选择下拉框 */
    private final ComboBox<String> userSelector = new ComboBox<>("选择用户");

    /** 统计信息区域 */
    private final Div statsArea = new Div();

    /** 虚拟列表展示对话历史 */
    private final VirtualList<ChatMessage> messageList = new VirtualList<>();

    /** 当前选定用户的对话历史 */
    private List<ChatMessage> currentHistory = Collections.emptyList();

    public AdminConversationView(ConversationStore conversationStore) {
        this.conversationStore = conversationStore;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // 标题
        H2 title = new H2("用户对话审查");
        title.getStyle().set("margin-bottom", "0");

        Span hint = new Span("选择一个用户查看其完整对话记录和合规审查拦截情况");
        hint.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("display", "block");

        // 用户选择 + 刷新
        configureUserSelector();

        Button refreshBtn = new Button("刷新用户列表", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        refreshBtn.addClickListener(e -> refreshUserList());

        HorizontalLayout toolbar = new HorizontalLayout(userSelector, refreshBtn);
        toolbar.setAlignItems(Alignment.END);
        toolbar.setSpacing(true);

        // 统计区域
        statsArea.getStyle()
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-s)");
        statsArea.setVisible(false);

        // 虚拟列表
        messageList.setRenderer(new ComponentRenderer<>(this::renderMessageItem));
        messageList.setSizeFull();
        messageList.setItems(currentHistory);

        add(title, hint, toolbar, statsArea, messageList);

        // 初始加载用户列表
        refreshUserList();
    }

    // ======================== 用户选择器 ========================

    private void configureUserSelector() {
        userSelector.setWidth("240px");
        userSelector.setPlaceholder("请选择用户...");
        userSelector.setHelperText("选择后自动加载该用户对话记录");
        userSelector.addValueChangeListener(e -> {
            String username = e.getValue();
            if (username != null && !username.isEmpty()) {
                loadUserHistory(username);
            } else {
                currentHistory = Collections.emptyList();
                messageList.setItems(currentHistory);
                statsArea.setVisible(false);
            }
        });
    }

    private void refreshUserList() {
        List<String> users = conversationStore.listAllUsers();
        userSelector.setItems(users);
        if (!users.isEmpty() && userSelector.getValue() == null) {
            // 默认不自动选中，等待管理员手动选择
        }
    }

    // ======================== 加载与统计 ========================

    private void loadUserHistory(String username) {
        currentHistory = conversationStore.loadHistory(username);
        messageList.setItems(currentHistory);

        if (currentHistory.isEmpty()) {
            statsArea.removeAll();
            Span emptySpan = new Span("用户 " + username + " 暂无对话记录");
            emptySpan.getStyle()
                    .set("color", "var(--lumo-tertiary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
            statsArea.add(emptySpan);
            statsArea.setVisible(true);
            return;
        }

        // 统计指标
        long userMsgCount = currentHistory.stream().filter(m -> "user".equals(m.getRole())).count();
        long aiMsgCount = currentHistory.stream().filter(m -> "assistant".equals(m.getRole())).count();

        long riskHigh = currentHistory.stream()
                .filter(m -> m.getReviewResult() != null && "高风险".equals(m.getReviewResult().getRiskLevel()))
                .count();
        long riskMid = currentHistory.stream()
                .filter(m -> m.getReviewResult() != null && "中风险".equals(m.getReviewResult().getRiskLevel()))
                .count();
        long riskLow = currentHistory.stream()
                .filter(m -> m.getReviewResult() != null && "低风险".equals(m.getReviewResult().getRiskLevel()))
                .count();

        statsArea.removeAll();
        HorizontalLayout statCards = new HorizontalLayout(
                buildStatBadge("📊 总消息", String.valueOf(currentHistory.size()), "#666"),
                buildStatBadge("👤 用户消息", String.valueOf(userMsgCount), "#1677FF"),
                buildStatBadge("🤖 AI回复", String.valueOf(aiMsgCount), "#52C41A"),
                buildStatBadge("⚠️ 高风险", String.valueOf(riskHigh), "#FF4D4F"),
                buildStatBadge("🔶 中风险", String.valueOf(riskMid), "#FAAD14"),
                buildStatBadge("✅ 低风险", String.valueOf(riskLow), "#52C41A")
        );
        statCards.getStyle().set("flex-wrap", "wrap");
        statCards.getStyle().set("gap", "var(--lumo-space-s)");
        statsArea.add(statCards);
        statsArea.setVisible(true);
    }

    private Div buildStatBadge(String label, String value, String color) {
        Div badge = new Div();
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-right", "var(--lumo-space-xs)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-weight", "700")
                .set("color", color);

        badge.add(labelSpan, valueSpan);
        badge.getStyle()
                .set("padding", "4px 12px")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("white-space", "nowrap");
        return badge;
    }

    // ======================== 消息渲染 ========================

    private Div renderMessageItem(ChatMessage msg) {
        Div item = new Div();

        // 时间
        Span time = new Span(msg.getTimestamp() != null
                ? msg.getTimestamp().format(DF) : "");
        time.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-right", "var(--lumo-space-m)");

        // 角色标识
        String roleIcon = "user".equals(msg.getRole()) ? "👤 用户" : "🤖 AI";
        Span role = new Span(roleIcon);
        role.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-right", "var(--lumo-space-m)");

        // 内容
        String contentText = msg.getContent();
        if (contentText != null && contentText.length() > 200) {
            contentText = contentText.substring(0, 200) + "...";
        }
        Span content = new Span(contentText);
        content.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-word");

        Div infoRow = new Div(time, role, content);
        infoRow.getStyle()
                .set("display", "flex")
                .set("align-items", "baseline")
                .set("flex-wrap", "wrap");

        item.add(infoRow);

        // 合规审查结果标记
        if (msg.getReviewResult() != null) {
            String riskColor;
            String riskLevel = msg.getReviewResult().getRiskLevel();
            switch (riskLevel) {
                case "高风险":
                    riskColor = "#FF4D4F";
                    break;
                case "中风险":
                    riskColor = "#FAAD14";
                    break;
                default:
                    riskColor = "#FF7875";
            }

            Span badge = new Span("🔒 " + riskLevel);
            badge.getStyle()
                    .set("display", "inline-block")
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "600")
                    .set("background-color", riskColor)
                    .set("color", "#FFFFFF")
                    .set("margin-left", "var(--lumo-space-s)");

            Span reason = new Span("原因: " + msg.getReviewResult().getReason());
            reason.getStyle()
                    .set("display", "block")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", riskColor)
                    .set("margin-top", "4px")
                    .set("font-style", "italic");

            Div reviewRow = new Div(badge, reason);
            reviewRow.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("flex-wrap", "wrap")
                    .set("margin-top", "4px");

            item.add(reviewRow);

            item.getStyle()
                    .set("border-left", "3px solid " + riskColor)
                    .set("background-color",
                            "高风险".equals(riskLevel) ? "rgba(255,77,79,0.04)"
                                    : "rgba(250,173,20,0.04)");
        }

        item.getStyle()
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        return item;
    }
}