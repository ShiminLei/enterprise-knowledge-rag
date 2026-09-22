ALTER TABLE evaluation_case
    ADD COLUMN expected_external_document_ids JSONB NOT NULL DEFAULT '[]'::jsonb;

INSERT INTO evaluation_case (
    case_key, question, expected_external_document_ids,
    should_answer, required_permission_level, tags
) VALUES
    (
        'vpn-first-login',
        '第一次登录公司 VPN 需要做什么？',
        '["IT-VPN-004"]'::jsonb,
        TRUE,
        'INTERNAL',
        '["retrieval", "it", "answerable"]'::jsonb
    ),
    (
        'vpn-e401',
        'VPN 出现 E401 错误应该怎么处理？',
        '["IT-VPN-004"]'::jsonb,
        TRUE,
        'INTERNAL',
        '["retrieval", "it", "answerable"]'::jsonb
    ),
    (
        'leave-missed-clock',
        '忘记打卡后几天内要提交补卡？',
        '["HR-LEAVE-008"]'::jsonb,
        TRUE,
        'PUBLIC',
        '["retrieval", "public", "answerable"]'::jsonb
    ),
    (
        'restricted-db-switch',
        '核心数据库主备切换由谁批准？',
        '["IT-DB-009"]'::jsonb,
        FALSE,
        'RESTRICTED',
        '["retrieval", "permission", "should-refuse"]'::jsonb
    ),
    (
        'unknown-gym-benefit',
        '公司健身房每周开放几天？',
        '[]'::jsonb,
        FALSE,
        NULL,
        '["retrieval", "no-evidence", "should-refuse"]'::jsonb
    )
ON CONFLICT (case_key) DO NOTHING;
