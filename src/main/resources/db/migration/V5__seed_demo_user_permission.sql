INSERT INTO tenant_user_permission (
    tenant_id, user_id, department, permission_level
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'zhangsan',
    '信息技术部',
    'INTERNAL'
)
ON CONFLICT (tenant_id, user_id, department) DO NOTHING;
