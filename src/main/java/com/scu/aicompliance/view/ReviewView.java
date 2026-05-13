package com.scu.aicompliance.view;

import com.scu.aicompliance.model.ComplianceResult;
import com.scu.aicompliance.service.ComplianceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route("review")
@PermitAll
public class ReviewView extends VerticalLayout {

    private final ComplianceService complianceService;

    private final TextArea inputArea = new TextArea("待审查内容");
    private final VerticalLayout resultLayout = new VerticalLayout();
    private final Button reviewBtn = new Button("提交审查");
    private final Button backBtn = new Button("返回首页");

    public ReviewView(ComplianceService complianceService) {
        this.complianceService = complianceService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("AI 内容合规审查");

        inputArea.setPlaceholder("请输入需要审查的文本内容...");
        inputArea.setWidthFull();
        inputArea.setMinHeight("200px");

        reviewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        reviewBtn.addClickListener(e -> performReview());

        backBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));

        HorizontalLayout buttonBar = new HorizontalLayout(reviewBtn, backBtn);
        buttonBar.setSpacing(true);

        resultLayout.setVisible(false);
        resultLayout.setWidthFull();
        resultLayout.setPadding(true);
        resultLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        resultLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

        add(title, inputArea, buttonBar, resultLayout);
    }

    private void performReview() {
        String content = inputArea.getValue().trim();
        if (content.isEmpty()) {
            Notification.show("请输入待审查的内容", 3000, Notification.Position.MIDDLE);
            return;
        }

        reviewBtn.setEnabled(false);
        reviewBtn.setText("审查中...");

        try {
            ComplianceResult result = complianceService.review(content);
            showResult(result);
        } catch (Exception e) {
            Notification notification = Notification.show(
                    "审查失败: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            reviewBtn.setEnabled(true);
            reviewBtn.setText("提交审查");
        }
    }

    private void showResult(ComplianceResult result) {
        resultLayout.removeAll();
        resultLayout.setVisible(true);

        H3 heading = new H3("审查结果");

        String riskColor;
        if ("高风险".equals(result.getRiskLevel())) {
            riskColor = "var(--lumo-error-color)";
        } else if ("中风险".equals(result.getRiskLevel())) {
            riskColor = "var(--lumo-warning-color)";
        } else {
            riskColor = "var(--lumo-success-color)";
        }

        Paragraph violationPara = new Paragraph();
        violationPara.getElement().setProperty("innerHTML",
                "违规判定: <b style='color:" + riskColor + "'>" +
                (result.isViolation() ? "违规 ⚠" : "合规 ✓") + "</b>");

        Paragraph riskPara = new Paragraph("风险等级: " + result.getRiskLevel());
        Paragraph reasonPara = new Paragraph("原因: " + result.getReason());

        reasonPara.getStyle().set("white-space", "pre-wrap");

        resultLayout.add(heading, violationPara, riskPara, reasonPara);
    }
}