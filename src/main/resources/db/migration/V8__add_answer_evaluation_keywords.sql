UPDATE evaluation_case
SET expected_keywords = '["公司邮箱", "动态口令"]'::jsonb
WHERE case_key = 'vpn-first-login';

UPDATE evaluation_case
SET expected_keywords = '["身份认证失败", "重置"]'::jsonb
WHERE case_key = 'vpn-e401';

UPDATE evaluation_case
SET expected_keywords = '["三个工作日"]'::jsonb
WHERE case_key = 'leave-missed-clock';
