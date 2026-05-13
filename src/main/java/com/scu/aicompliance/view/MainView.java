package com.scu.aicompliance.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

/**
 * 合规审查系统主页面 — 映射到根路径 "/"
 */
@Route("")
@PermitAll
public class MainView extends VerticalLayout {

    public MainView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("AI 合规审查系统");
        Paragraph subtitle = new Paragraph("基于 GPT 的智能内容合规审查平台");

        Button startButton = new Button("开始审查", e ->
                getUI().ifPresent(ui -> ui.navigate("review")));

        add(title, subtitle, startButton);
    }
}