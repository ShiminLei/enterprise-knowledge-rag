-- The default evaluation persona is zhangsan from the IT department with
-- INTERNAL clearance (seeded by V5). Cases from other departments or above
-- that clearance deliberately expect a refusal.
INSERT INTO evaluation_case (
    case_key, question, expected_external_document_ids, expected_keywords,
    should_answer, required_permission_level, tags
) VALUES
    (
        'vpn-clock-drift',
        '系统时间偏差多少会导致 VPN 动态口令校验失败？',
        '["IT-VPN-004"]'::jsonb,
        '["两分钟", "动态口令"]'::jsonb,
        TRUE,
        'INTERNAL',
        '["retrieval", "it", "answerable", "boundary"]'::jsonb
    ),
    (
        'vpn-e503',
        'VPN 出现 E503 错误应该怎么处理？',
        '["IT-VPN-004"]'::jsonb,
        '["网关不可用", "IT 服务台"]'::jsonb,
        TRUE,
        'INTERNAL',
        '["retrieval", "it", "answerable", "error-code"]'::jsonb
    ),
    (
        'vpn-lockout',
        'VPN 连续登录失败多少次会锁定，锁定多久？',
        '["IT-VPN-004"]'::jsonb,
        '["五次", "30 分钟"]'::jsonb,
        TRUE,
        'INTERNAL',
        '["retrieval", "it", "answerable", "security"]'::jsonb
    ),
    (
        'leave-annual-notice',
        '年假原则上需要提前多少个工作日申请？',
        '["HR-LEAVE-008"]'::jsonb,
        '["三个工作日"]'::jsonb,
        TRUE,
        'PUBLIC',
        '["retrieval", "hr", "public", "answerable"]'::jsonb
    ),
    (
        'leave-sick-certificate',
        '连续病假超过多少天需要医院证明，对医院有什么要求？',
        '["HR-LEAVE-008"]'::jsonb,
        '["两个工作日", "二级及以上医院"]'::jsonb,
        TRUE,
        'PUBLIC',
        '["retrieval", "hr", "public", "answerable"]'::jsonb
    ),
    (
        'leave-personal-approval',
        '连续事假超过三天需要谁审批？',
        '["HR-LEAVE-008"]'::jsonb,
        '["部门负责人", "人力资源负责人"]'::jsonb,
        TRUE,
        'PUBLIC',
        '["retrieval", "hr", "public", "answerable"]'::jsonb
    ),
    (
        'travel-current-hotel-limit',
        '现行制度中一线城市的住宿标准是多少？',
        '["FIN-TRAVEL-002"]'::jsonb,
        '[]'::jsonb,
        FALSE,
        'INTERNAL',
        '["retrieval", "finance", "permission", "should-refuse"]'::jsonb
    ),
    (
        'travel-obsolete-limit',
        '差旅制度旧版的一线城市住宿标准是多少？',
        '["FIN-TRAVEL-001"]'::jsonb,
        '[]'::jsonb,
        FALSE,
        'INTERNAL',
        '["retrieval", "finance", "permission", "legacy-version", "should-refuse"]'::jsonb
    ),
    (
        'purchase-three-quotes',
        '单笔采购超过多少金额原则上需要三家有效报价？',
        '["FIN-PROC-003"]'::jsonb,
        '[]'::jsonb,
        FALSE,
        'INTERNAL',
        '["retrieval", "procurement", "permission", "should-refuse"]'::jsonb
    ),
    (
        'crm-duplicate-customer',
        'CRM 中客户被重复创建后应该怎么处理？',
        '["IT-CRM-005"]'::jsonb,
        '[]'::jsonb,
        FALSE,
        'INTERNAL',
        '["retrieval", "sales", "permission", "should-refuse"]'::jsonb
    ),
    (
        'equipment-e32',
        'A100 装配线出现 E32 故障时应检查哪些项目？',
        '["OPS-EQP-006"]'::jsonb,
        '[]'::jsonb,
        FALSE,
        'CONFIDENTIAL',
        '["retrieval", "operations", "permission", "should-refuse"]'::jsonb
    ),
    (
        'after-sales-seven-day-return',
        '标准产品申请七日退货需要满足什么条件？',
        '["CS-AFTERSALE-007"]'::jsonb,
        '[]'::jsonb,
        FALSE,
        'INTERNAL',
        '["retrieval", "customer-service", "permission", "should-refuse"]'::jsonb
    ),
    (
        'security-phishing-report',
        '发现疑似钓鱼邮件后应在多久内报告？',
        '["SEC-BASELINE-010"]'::jsonb,
        '[]'::jsonb,
        FALSE,
        'INTERNAL',
        '["retrieval", "security", "permission", "should-refuse"]'::jsonb
    ),
    (
        'restricted-db-p1-threshold',
        '超过多少用户无法完成交易时，核心数据库故障判定为 P1？',
        '["IT-DB-009"]'::jsonb,
        '[]'::jsonb,
        FALSE,
        'RESTRICTED',
        '["retrieval", "it", "permission", "should-refuse"]'::jsonb
    ),
    (
        'unknown-cafeteria-subsidy',
        '公司食堂每月的餐补标准是多少？',
        '[]'::jsonb,
        '[]'::jsonb,
        FALSE,
        NULL,
        '["retrieval", "no-evidence", "should-refuse"]'::jsonb
    )
ON CONFLICT (case_key) DO NOTHING;
