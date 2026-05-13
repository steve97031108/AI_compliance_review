package com.scu.aicompliance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 合规审查结果数据模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceResult {

    /** 是否违规 */
    private boolean violation;

    /** 违规原因 */
    private String reason;

    /** 风险等级（低风险/中风险/高风险） */
    private String riskLevel;
}