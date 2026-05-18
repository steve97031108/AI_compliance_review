package com.scu.aicompliance.ui;

import com.scu.aicompliance.model.SystemConfig;
import com.scu.aicompliance.service.ConfigService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * 管理员系统设置页面 — 配置 API 密钥、模型参数、合规检查开关等。
 */
@Route(value = "settings", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SettingsView extends VerticalLayout {

    private final ConfigService configService;

    private final PasswordField apiKeyField = new PasswordField("API 密钥");
    private final TextField baseUrlField = new TextField("API 基础地址");
    private final TextField modelNameField = new TextField("模型名称");
    private final NumberField temperatureField = new NumberField("温度系数");
    private final IntegerField maxTokensField = new IntegerField("最大生成长度");
    private final IntegerField timeoutField = new IntegerField("超时时间（秒）");
    private final Checkbox enableOutputCheck = new Checkbox("启用输出合规检查");

    private final Button saveButton = new Button("保存配置");

    public SettingsView(ConfigService configService) {
        this.configService = configService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("系统设置");
        title.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        configureFields();
        loadCurrentConfig();

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveConfig());

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        form.add(apiKeyField, baseUrlField, modelNameField,
                temperatureField, maxTokensField, timeoutField,
                enableOutputCheck);
        form.setWidth("100%");
        form.getStyle().set("max-width", "800px");

        add(title, form, saveButton);
    }

    /**
     * 配置各字段的属性、提示和验证。
     */
    private void configureFields() {
        // API 密钥
        apiKeyField.setPlaceholder("sk-...");
        apiKeyField.setHelperText("支持所有 OpenAI 兼容厂商的 API 密钥");
        apiKeyField.setWidthFull();
        apiKeyField.setRequired(true);

        // API 基础地址
        baseUrlField.setPlaceholder("https://api.openai.com/v1");
        baseUrlField.setHelperText("例如 https://api.openai.com/v1、https://dashscope.aliyuncs.com/compatible-mode/v1");
        baseUrlField.setWidthFull();

        // 模型名称
        modelNameField.setPlaceholder("gpt-3.5-turbo");
        modelNameField.setHelperText("例如 gpt-3.5-turbo、claude-3-sonnet、qwen-turbo");
        modelNameField.setWidthFull();
        modelNameField.setRequired(true);

        // 温度系数
        temperatureField.setPlaceholder("0.7");
        temperatureField.setHelperText("范围 0.0 ~ 1.0，越高越随机");
        temperatureField.setMin(0.0);
        temperatureField.setMax(1.0);
        temperatureField.setStep(0.1);
        temperatureField.setValue(0.7);
        temperatureField.setWidthFull();

        // 最大生成长度
        maxTokensField.setPlaceholder("2048");
        maxTokensField.setHelperText("单次回复最大 token 数");
        maxTokensField.setMin(1);
        maxTokensField.setStepButtonsVisible(true);
        maxTokensField.setValue(2048);
        maxTokensField.setWidthFull();

        // 超时时间
        timeoutField.setPlaceholder("60");
        timeoutField.setHelperText("API 调用超时时间，单位秒");
        timeoutField.setMin(5);
        timeoutField.setStepButtonsVisible(true);
        timeoutField.setValue(60);
        timeoutField.setWidthFull();

        // 合规检查开关
        enableOutputCheck.getStyle().set("padding-top", "var(--lumo-space-m)");
    }

    /**
     * 从 ConfigService 加载当前配置到表单字段。
     */
    private void loadCurrentConfig() {
        SystemConfig config = configService.getConfig();

        if (config.getOpenaiApiKey() != null) {
            apiKeyField.setValue(config.getOpenaiApiKey());
        }
        if (config.getOpenaiBaseUrl() != null) {
            baseUrlField.setValue(config.getOpenaiBaseUrl());
        }
        if (config.getModelName() != null) {
            modelNameField.setValue(config.getModelName());
        }
        if (config.getTemperature() != null) {
            temperatureField.setValue(config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            maxTokensField.setValue(config.getMaxTokens());
        }
        if (config.getTimeoutSeconds() != null) {
            timeoutField.setValue(config.getTimeoutSeconds());
        }
        enableOutputCheck.setValue(
                config.getEnableOutputCheck() != null && config.getEnableOutputCheck());
    }

    /**
     * 保存配置到 ConfigService 并显示通知。
     */
    private void saveConfig() {
        // 基本校验
        if (apiKeyField.isEmpty()) {
            Notification.show("请输入 API 密钥", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            apiKeyField.focus();
            return;
        }
        if (modelNameField.isEmpty()) {
            Notification.show("请输入模型名称", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            modelNameField.focus();
            return;
        }

        try {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setOpenaiApiKey(apiKeyField.getValue());
            newConfig.setOpenaiBaseUrl(
                    baseUrlField.isEmpty() ? null : baseUrlField.getValue());
            newConfig.setModelName(modelNameField.getValue());
            newConfig.setTemperature(temperatureField.getValue());
            newConfig.setMaxTokens(maxTokensField.getValue());
            newConfig.setTimeoutSeconds(timeoutField.getValue());
            newConfig.setEnableOutputCheck(enableOutputCheck.getValue());

            configService.saveConfig(newConfig);

            Notification.show("配置已保存成功", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("保存失败: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}