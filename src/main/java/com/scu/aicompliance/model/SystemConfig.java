package com.scu.aicompliance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统配置数据模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    /** API密钥，支持所有OpenAI兼容厂商 */
    private String openaiApiKey;

    /** API基础地址，可自定义任何兼容端点 */
    private String openaiBaseUrl;

    /** 模型名称 */
    private String modelName;

    /** 温度系数 */
    private Double temperature;

    /** 最大生成长度 */
    private Integer maxTokens;

    /** 超时时间（秒） */
    private Integer timeoutSeconds;

    /** 是否启用输出合规检查，默认false */
    private Boolean enableOutputCheck = false;
}