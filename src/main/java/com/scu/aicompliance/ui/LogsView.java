package com.scu.aicompliance.ui;

import com.scu.aicompliance.model.ChatMessage;
import com.scu.aicompliance.service.ChatService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;

/**
 * 日志查询页面 — 展示当前会话的完整对话记录。
 * <p>
 * 被合规审查拦截的消息以醒目红色边框标记，并显示风险等级和原因。
 * </p>
 */
@Route(value = "logs", layout = MainLayout.class)
@PermitAll
public class LogsView extends VerticalLayout {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    private final ChatService chatService;

    public LogsView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("会话日志");
        title.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        Span hint = new Span("带 🔒 标记的条目表示被合规审查拦截的内容");
        hint.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("display", "block");

        VirtualList<ChatMessage> logList = new VirtualList<>();
        logList.setRenderer(new ComponentRenderer<>(this::renderLogItem));
        logList.setSizeFull();

        // 实时刷新历史
        logList.setItems(chatService.getHistory());

        add(title, hint, logList);
    }

    private Div renderLogItem(ChatMessage msg) {
        Div item = new Div();

        // 时间
        Span time = new Span(msg.getTimestamp() != null
                ? msg.getTimestamp().format(DF) : "");
        time.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-right", "var(--lumo-space-m)");

        // 角色
        String roleIcon = "user".equals(msg.getRole()) ? "👤 用户" : "🤖 AI";
        Span role = new Span(roleIcon);
        role.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-right", "var(--lumo-space-m)");

        // 内容摘要（限 120 字）
        String contentText = msg.getContent();
        if (contentText != null && contentText.length() > 120) {
            contentText = contentText.substring(0, 120) + "...";
        }
        Span content = new Span(contentText);
        content.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("white-space", "pre-wrap")
                .set("color", "var(--lumo-body-text-color)");

        // 组合基本信息
        Div infoRow = new Div(time, role, content);
        infoRow.getStyle()
                .set("display", "flex")
                .set("align-items", "baseline")
                .set("flex-wrap", "wrap");

        // 合规审查结果标记
        if (msg.getReviewResult() != null) {
            // 拦截标记徽章
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
            Span badge = new Span("🔒 " + msg.getReviewResult().getRiskLevel());
            badge.getStyle()
                    .set("display", "inline-block")
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "600")
                    .set("background-color", riskColor)
                    .set("color", "#FFFFFF")
                    .set("margin-left", "var(--lumo-space-s)");

            Span reason = new Span(msg.getReviewResult().getReason());
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

            item.add(infoRow, reviewRow);

            // 整行红色边框
            item.getStyle()
                    .set("border-left", "3px solid " + riskColor)
                    .set("background-color",
                            "高风险".equals(riskLevel) ? "rgba(255,77,79,0.04)"
                                    : "rgba(250,173,20,0.04)");
        } else {
            item.add(infoRow);
        }

        item.getStyle()
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        return item;
    }
}