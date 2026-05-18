package com.scu.aicompliance.ui;

import com.scu.aicompliance.model.ChatMessage;
import com.scu.aicompliance.model.ComplianceResult;
import com.scu.aicompliance.service.AiService;
import com.scu.aicompliance.service.ChatService;
import com.scu.aicompliance.service.ComplianceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天对话主界面 — 用户与 AI 模型自由对话，系统在对话过程中自动进行合规审查。
 * <p>
 * 合规审查作为中间层工作：
 * <ul>
 *   <li><b>输入审查</b>：用户消息发送前，先经 ComplianceService 审核，违规内容被拦截</li>
 *   <li><b>输出审查</b>：AI 回复返回后（如启用），经 ComplianceService 审核，违规内容被替换</li>
 * </ul>
 * 所有审查结果全部写入 ChatService 历史，供日志/统计页面使用。
 * </p>
 */
@Route(value = "chat", layout = MainLayout.class)
@PermitAll
public class ChatView extends VerticalLayout {

    private final ChatService chatService;
    private final AiService aiService;
    private final ComplianceService complianceService;

    /** 聊天历史显示区域（滚动容器） */
    private final VerticalLayout chatHistory = new VerticalLayout();

    /** 消息输入框 */
    private final TextArea inputField = new TextArea();

    /** 发送按钮 */
    private final Button sendButton = new Button("发送");

    /** 清空历史按钮 */
    private final Button clearButton = new Button("清空");

    /** 加载提示是否已显示 */
    private boolean loadingShown = false;

    public ChatView(ChatService chatService, AiService aiService,
                    ComplianceService complianceService) {
        this.chatService = chatService;
        this.aiService = aiService;
        this.complianceService = complianceService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        configureChatHistory();
        configureInputArea();

        // 加载已有的对话历史
        reloadHistory();

        add(chatHistory, createInputBar());
    }

    // ======================== 布局配置 ========================

    /**
     * 配置聊天历史区域。
     */
    private void configureChatHistory() {
        chatHistory.setSizeFull();
        chatHistory.setPadding(true);
        chatHistory.setSpacing(false);
        chatHistory.getStyle()
                .set("overflow-y", "auto")
                .set("background-color", "#EDEDED")
                .set("background-image", "linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%)");
        chatHistory.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
    }

    /**
     * 配置输入区域。
     */
    private void configureInputArea() {
        inputField.setPlaceholder("输入您的问题，按 Enter 发送，Shift+Enter 换行...");
        inputField.setWidthFull();
        inputField.setMinHeight("48px");
        inputField.setMaxHeight("100px");
        inputField.getStyle()
                .set("resize", "none")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-size", "var(--lumo-font-size-m)");

        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.setIcon(VaadinIcon.PAPERPLANE.create());
        sendButton.getStyle().set("margin-left", "var(--lumo-space-s)");
        sendButton.addClickListener(e -> handleSend());

        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        clearButton.setIcon(VaadinIcon.TRASH.create());
        clearButton.getStyle().set("margin-left", "4px");
        clearButton.addClickListener(e -> handleClear());

        // 按下 Enter 发送（Shift+Enter 换行，纯 Enter 发送）
        inputField.getElement().executeJs(
            "this.addEventListener('keydown', function(e) {" +
            "  if (e.key === 'Enter' && !e.shiftKey) {" +
            "    e.preventDefault();" +
            "    this._sendOnServer = true;" +
            "    this.dispatchEvent(new Event('change', {bubbles: true}));" +
            "  }" +
            "});"
        );
        inputField.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                inputField.getElement().executeJs(
                    "var send = this._sendOnServer; this._sendOnServer = false; return send;"
                ).then(Boolean.class, shouldSend -> {
                    if (Boolean.TRUE.equals(shouldSend)) {
                        handleSend();
                    }
                });
            }
        });
    }

    /**
     * 构建底部输入栏。
     */
    private HorizontalLayout createInputBar() {
        HorizontalLayout inputBar = new HorizontalLayout(inputField, sendButton, clearButton);
        inputBar.setWidthFull();
        inputBar.setAlignItems(Alignment.END);
        inputBar.setPadding(true);
        inputBar.setSpacing(false);
        inputBar.expand(inputField);
        inputBar.getStyle()
                .set("border-top", "1px solid #D4D4D4")
                .set("background-color", "#F7F7F7")
                .set("box-shadow", "0 -2px 8px rgba(0,0,0,0.06)")
                .set("padding", "12px 16px");
        return inputBar;
    }

    // ======================== 消息渲染（聊天气泡风格） ========================

    /**
     * 在聊天历史中追加一条消息气泡。
     */
    private void appendMessage(String role, String content) {
        boolean isUser = "user".equals(role);

        // 外侧容器：控制左右对齐 + 间距
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", isUser ? "flex-end" : "flex-start")
                .set("margin", "6px 0")
                .set("padding", isUser ? "0 4px 0 60px" : "0 60px 0 4px");
        wrapper.addClassName("chat-bubble");

        // 头像 + 名称行
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setSpacing(true);
        headerRow.getStyle().set("align-items", "center");

        Span avatar = new Span(isUser ? "👤" : "🤖");
        avatar.getStyle()
                .set("font-size", "18px")
                .set("width", "28px")
                .set("height", "28px")
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("border-radius", "50%")
                .set("background-color", isUser ? "#1677FF" : "#52C41A")
                .set("flex-shrink", "0");

        Span nameLabel = new Span(isUser ? "我" : "AI");
        nameLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "#666")
                .set("font-weight", "500");

        if (isUser) {
            headerRow.add(nameLabel, avatar);
        } else {
            headerRow.add(avatar, nameLabel);
        }

        // 消息气泡主体
        Div bubble = new Div();
        Span contentSpan = new Span(content);
        contentSpan.getStyle().set("white-space", "pre-wrap");
        bubble.add(contentSpan);
        bubble.getStyle()
                .set("max-width", "100%")
                .set("padding", "10px 14px")
                .set("border-radius", isUser
                        ? "12px 4px 12px 12px"
                        : "4px 12px 12px 12px")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("line-height", "1.6")
                .set("word-break", "break-word");

        if (isUser) {
            bubble.getStyle()
                    .set("background", "linear-gradient(135deg, #1677FF 0%, #4096FF 100%)")
                    .set("color", "#FFFFFF")
                    .set("box-shadow", "0 1px 3px rgba(22,119,255,0.3)");
        } else {
            bubble.getStyle()
                    .set("background-color", "#FFFFFF")
                    .set("color", "var(--lumo-body-text-color)")
                    .set("box-shadow", "0 1px 3px rgba(0,0,0,0.08)");
        }

        wrapper.add(headerRow, bubble);
        chatHistory.add(wrapper);
    }

    /**
     * 追加一条系统提示消息（合规审查拦截通知等），使用醒目样式。
     */
    private void appendSystemMessage(String content, String color) {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("margin", "10px 0")
                .set("padding", "0 40px");

        Span badge = new Span("🔒 " + content);
        badge.getStyle()
                .set("display", "inline-block")
                .set("padding", "6px 16px")
                .set("border-radius", "16px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500")
                .set("background-color", color)
                .set("color", "#FFFFFF")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.12)");

        wrapper.add(badge);
        chatHistory.add(wrapper);
    }

    /**
     * 显示/移除 "AI正在思考..." 加载提示。
     */
    private void setLoading(boolean loading) {
        if (loading && !loadingShown) {
            Div thinking = new Div();
            thinking.setId("thinking-indicator");
            thinking.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("padding", "8px 0 8px 70px")
                    .set("margin", "6px 0");

            Span avatar = new Span("🤖");
            avatar.getStyle()
                    .set("font-size", "18px")
                    .set("width", "28px")
                    .set("height", "28px")
                    .set("display", "inline-flex")
                    .set("align-items", "center")
                    .set("justify-content", "center")
                    .set("border-radius", "50%")
                    .set("background-color", "#52C41A")
                    .set("margin-right", "8px");

            Span dots = new Span("正在思考");
            dots.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "#999")
                    .set("font-style", "italic");

            thinking.add(avatar, dots);
            chatHistory.add(thinking);
            loadingShown = true;

            dots.getElement().executeJs(
                    "let i=0; this._anim = setInterval(() => {" +
                    "  this.textContent = '正在思考' + '.'.repeat((++i % 4));" +
                    "}, 400);"
            );
        } else if (!loading && loadingShown) {
            chatHistory.getChildren()
                    .filter(c -> "thinking-indicator".equals(c.getId().orElse("")))
                    .forEach(c -> {
                        c.getElement().executeJs(
                                "if(this._anim) clearInterval(this._anim);"
                        );
                        chatHistory.remove(c);
                    });
            loadingShown = false;
        }
    }

    // ======================== 历史管理 ========================

    /**
     * 从 ChatService 重新加载完整对话历史到界面。
     * <p>
     * 被审查拦截的消息以系统消息形式显示。
     * </p>
     */
    private void reloadHistory() {
        chatHistory.removeAll();
        loadingShown = false;
        for (ChatMessage msg : chatService.getHistory()) {
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
                String label = "user".equals(msg.getRole()) ? "消息" : "回复";
                appendSystemMessage(
                        "AI " + label + "已被拦截 — "
                                + msg.getReviewResult().getReason()
                                + "（" + riskLevel + "）",
                        riskColor);
            } else {
                appendMessage(msg.getRole(), msg.getContent());
            }
        }
    }

    // ======================== 交互处理 ========================

    /**
     * 处理发送消息 — 集成合规审查中间层，所有审查结果写入历史。
     * <p>
     * 流程：
     * <ol>
     *   <li>用户输入 → 合规审查（输入审查）</li>
     *   <li>违规 → 拦截 + 写入历史 + 显示警告，不发送给 AI</li>
     *   <li>合规 → 发送给 AI 模型</li>
     *   <li>AI 回复 → 合规审查（输出审查）</li>
     *   <li>违规 → 替换为警告消息 + 写入历史标记</li>
     *   <li>合规 → 正常显示</li>
     * </ol>
     * </p>
     */
    private void handleSend() {
        String userInput = inputField.getValue().replaceFirst("\\n$", "").trim();
        if (userInput.isEmpty()) {
            inputField.clear();
            return;
        }

        // 保存用户输入文本（在清空输入框之前）
        final String userText = userInput;

        // 1. 禁用发送按钮防重复提交
        setInputEnabled(false);

        // 2. 清空输入框
        inputField.clear();

        // 3. === 输入合规审查 ===
        ComplianceResult inputReview = complianceService.review(userText);
        if (inputReview.isViolation()) {
            // 违规 — 拦截，不显示用户消息，不发送给 AI
            String riskColor;
            switch (inputReview.getRiskLevel()) {
                case "高风险":
                    riskColor = "#FF4D4F";
                    break;
                case "中风险":
                    riskColor = "#FAAD14";
                    break;
                default:
                    riskColor = "#FF7875";
            }
            appendSystemMessage(
                    "消息已被拦截 — " + inputReview.getReason()
                            + "（" + inputReview.getRiskLevel() + "）",
                    riskColor);
            // ★ 写入历史，日志/统计页面可查
            chatService.addBlockedUserMessage(userText, inputReview);
            setInputEnabled(true);
            inputField.focus();
            uiScrollToBottom();
            return;
        }

        // 合规 — 正常显示用户消息
        appendMessage("user", userText);
        chatService.addUserMessage(userText);

        // 4. 显示加载提示
        setLoading(true);
        uiScrollToBottom();

        // 5. 深拷贝对话历史快照，过滤掉被合规审查拦截的消息
        final List<ChatMessage> historyCopy = chatService.getHistory().stream()
                .filter(m -> m.getReviewResult() == null)
                .collect(java.util.stream.Collectors.toList());

        // 6. 捕获当前 Spring 请求上下文
        final ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 7. 异步获取 AI 回复
        getUI().ifPresent(ui -> {
            new Thread(() -> {
                try {
                    String reply = aiService.generateResponse(historyCopy);

                    // === 输出合规审查 ===
                    ComplianceResult outputReview = complianceService.review(reply);
                    String finalReply;
                    if (outputReview.isViolation()) {
                        finalReply = "⚠️ [合规审查拦截] AI 回复内容违规已被屏蔽："
                                + outputReview.getReason()
                                + "（" + outputReview.getRiskLevel() + "）";
                    } else {
                        finalReply = reply;
                    }

                    ui.access(() -> {
                        restoreRequestAttributes(requestAttributes);
                        try {
                            setLoading(false);
                            appendMessage("assistant", finalReply);
                            if (outputReview.isViolation()) {
                                // ★ 写入带审查结果的历史
                                chatService.addBlockedAssistantMessage(finalReply, outputReview);
                            } else {
                                chatService.addAssistantMessage(finalReply);
                            }
                            setInputEnabled(true);
                            inputField.focus();
                            uiScrollToBottom();
                        } finally {
                            RequestContextHolder.resetRequestAttributes();
                        }
                    });
                } catch (Exception ex) {
                    String errorMsg = "❌ 调用失败：" + ex.getMessage();
                    ui.access(() -> {
                        restoreRequestAttributes(requestAttributes);
                        try {
                            setLoading(false);
                            appendMessage("assistant", errorMsg);
                            chatService.addAssistantMessage(errorMsg);
                            setInputEnabled(true);
                            inputField.focus();
                            uiScrollToBottom();
                        } finally {
                            RequestContextHolder.resetRequestAttributes();
                        }
                    });
                }
            }).start();
        });
    }

    /**
     * 恢复 Spring 的 RequestContextHolder。
     */
    private static void restoreRequestAttributes(ServletRequestAttributes attrs) {
        if (attrs != null) {
            RequestContextHolder.setRequestAttributes(attrs);
        }
    }

    /**
     * 启用/禁用输入区域。
     */
    private void setInputEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
        inputField.setEnabled(enabled);
    }

    /**
     * 滚动聊天区域到底部。
     */
    private void uiScrollToBottom() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "setTimeout(function() {" +
                "  var el = document.querySelector('vaadin-vertical-layout[class*=\"chatHistory\"]');" +
                "  if(el) el.scrollTop = el.scrollHeight;" +
                "}, 80);"
        ));
    }

    /**
     * 处理清空历史。
     */
    private void handleClear() {
        chatService.clearHistory();
        chatHistory.removeAll();
        loadingShown = false;
    }

}