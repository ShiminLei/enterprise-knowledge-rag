#!/usr/bin/env bash

set -euo pipefail

user_id="${1:-zhangsan}"
tenant_id="${2:-00000000-0000-0000-0000-000000000001}"
scope="${3:-}"
issuer="${RAG_JWT_ISSUER:-enterprise-knowledge-rag}"
ttl_seconds="${JWT_TTL_SECONDS:-3600}"
jwt_secret="${RAG_JWT_SECRET:-}"

if [[ -z "$jwt_secret" ]]; then
  echo "错误：请先设置 RAG_JWT_SECRET" >&2
  exit 1
fi

secret_bytes="$(printf '%s' "$jwt_secret" | wc -c | tr -d ' ')"
if (( secret_bytes < 32 )); then
  echo "错误：RAG_JWT_SECRET 至少需要 32 字节" >&2
  exit 1
fi

if [[ ! "$user_id" =~ ^[A-Za-z0-9._@-]+$ ]]; then
  echo "错误：本地脚本的 userId 只允许字母、数字和 ._@-" >&2
  exit 1
fi

if [[ ! "$tenant_id" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]]; then
  echo "错误：tenantId 必须是 UUID" >&2
  exit 1
fi

scope_pattern='^[A-Za-z0-9._: -]*$'
if [[ ! "$scope" =~ $scope_pattern ]]; then
  echo "错误：scope 包含不支持的字符" >&2
  exit 1
fi

if [[ ! "$issuer" =~ ^[A-Za-z0-9._:-]+$ ]]; then
  echo "错误：RAG_JWT_ISSUER 包含不支持的字符" >&2
  exit 1
fi

if [[ ! "$ttl_seconds" =~ ^[0-9]+$ ]] || (( ttl_seconds < 1 )); then
  echo "错误：JWT_TTL_SECONDS 必须是正整数" >&2
  exit 1
fi

base64_url_encode() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

issued_at="$(date +%s)"
expires_at="$((issued_at + ttl_seconds))"
header='{"alg":"HS256","typ":"JWT"}'

if [[ -n "$scope" ]]; then
  payload="$(printf \
    '{"iss":"%s","sub":"%s","tenant_id":"%s","scope":"%s","iat":%s,"exp":%s}' \
    "$issuer" "$user_id" "$tenant_id" "$scope" "$issued_at" "$expires_at")"
else
  payload="$(printf \
    '{"iss":"%s","sub":"%s","tenant_id":"%s","iat":%s,"exp":%s}' \
    "$issuer" "$user_id" "$tenant_id" "$issued_at" "$expires_at")"
fi

encoded_header="$(printf '%s' "$header" | base64_url_encode)"
encoded_payload="$(printf '%s' "$payload" | base64_url_encode)"
signing_input="${encoded_header}.${encoded_payload}"
signature="$(printf '%s' "$signing_input" \
  | openssl dgst -sha256 -hmac "$jwt_secret" -binary \
  | base64_url_encode)"

echo "Header : $header" >&2
echo "Payload: $payload" >&2
printf '%s.%s\n' "$signing_input" "$signature"
