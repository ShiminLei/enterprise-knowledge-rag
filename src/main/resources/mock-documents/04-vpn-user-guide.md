---
documentId: IT-VPN-004
title: 企业 VPN 使用手册
category: MANUAL
version: "2.3"
updatedAt: 2026-05-20
department: 信息技术部
permissionLevel: INTERNAL
source: IT 服务台/远程办公
---

# 企业 VPN 使用手册

## 首次登录

访问 `vpn.example.internal`，使用公司邮箱和统一身份认证密码登录。首次登录必须绑定动态口令，不允许使用私人邮箱注册。

## 无法连接

1. 确认网络可以访问公网。
2. 检查系统时间是否准确，时间偏差超过两分钟会导致动态口令校验失败。
3. 删除过期配置后重新下载 VPN 配置。
4. 错误码 E401 表示身份认证失败，可先重置统一身份认证密码。
5. 错误码 E503 表示网关不可用，请联系 IT 服务台。

## 安全要求

禁止在公共电脑保存 VPN 密码。连续五次登录失败后账号会锁定 30 分钟。
