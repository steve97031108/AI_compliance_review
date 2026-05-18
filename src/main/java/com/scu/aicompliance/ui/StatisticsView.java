package com.scu.aicompliance.ui;

import com.scu.aicompliance.model.ChatMessage;
import com.scu.aicompliance.service.ChatService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 合规统计页面 — 基于当前会话的对话历史计算统计指标。
 * <p>
 * 风险等级统计直接从 ChatMessage.reviewResult 字段读取，
 * 不再依赖消息内容文本匹配，确保数据准确。
 * </p>
 */
@Route(value = "statistics", layout = MainLayout.class)
@PermitAll
public class StatisticsView extends VerticalLayout {

    private final ChatService chatService;

    public StatisticsView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("会话统计");
        title.getStyle().set("margin-bottom", "var(--lumo-space-l)");

        List<ChatMessage> history = chatService.getHistory();

        if (history.isEmpty()) {
            Paragraph emptyHint = new Paragraph("当前会话暂无对话记录，开始对话后将自动生成统计数据。");
            emptyHint.getStyle()
                    .set("color", "var(--lumo-tertiary-text-color)")
                    .set("font-size", "var(--lumo-font-size-m)");
            add(title, emptyHint);
            return;
        }

        HorizontalLayout cards = buildStatCards(history);
        add(title, cards);
    }

    private HorizontalLayout buildStatCards(List<ChatMessage> history) {
        // 基础指标
        long userCount = history.stream().filter(m -> "user".equals(m.getRole())).count();
        long aiCount = history.stream().filter(m -> "assistant".equals(m.getRole())).count();
        long totalChars = history.stream().mapToLong(m -> m.getContent().length()).sum();
        int avgCharsPerMsg = (int) (totalChars / Math.max(history.size(), 1));

        // 首次 / 最近消息时间
        LocalDateTime first = history.get(0).getTimestamp();
        LocalDateTime last = history.get(history.size() - 1).getTimestamp();
        String duration = first != null && last != null
                ? ChronoUnit.MINUTES.between(first, last) + " 分钟"
                : "—";

        // ★ 风险等级统计 — 直接从 reviewResult 字段读取，准确可靠
        long riskHigh = history.stream()
                .filter(m -> m.getReviewResult() != null
                        && "高风险".equals(m.getReviewResult().getRiskLevel()))
                .count();
        long riskMid = history.stream()
                .filter(m -> m.getReviewResult() != null
                        && "中风险".equals(m.getReviewResult().getRiskLevel()))
                .count();
        long riskLow = history.stream()
                .filter(m -> m.getReviewResult() != null
                        && "低风险".equals(m.getReviewResult().getRiskLevel()))
                .count();

        HorizontalLayout row = new HorizontalLayout(
                buildCard("📊", "总消息数", String.valueOf(history.size())),
                buildCard("👤", "用户消息", String.valueOf(userCount)),
                buildCard("🤖", "AI 回复", String.valueOf(aiCount)),
                buildCard("📝", "平均字数", avgCharsPerMsg + " 字"),
                buildCard("⏱️", "对话跨度", duration),
                buildCard("⚠️", "高风险", riskHigh + " 条"),
                buildCard("🔶", "中风险", riskMid + " 条"),
                buildCard("✅", "低风险", riskLow + " 条")
        );
        row.getStyle().set("flex-wrap", "wrap");
        row.getStyle().set("gap", "var(--lumo-space-m)");
        return row;
    }

    private Div buildCard(String icon, String label, String value) {
        Div card = new Div();

        Span iconSpan = new Span(icon);
        iconSpan.getStyle().set("font-size", "var(--lumo-font-size-xxl)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set("display", "block");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)");

        card.add(iconSpan, valueSpan, labelSpan);
        card.getStyle()
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("width", "160px")
                .set("min-width", "150px")
                .set("text-align", "center");
        return card;
    }
}