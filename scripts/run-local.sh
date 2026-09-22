#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
env_file="${project_dir}/.env"

if [[ ! -f "${env_file}" ]]; then
  echo "缺少 ${env_file}，请先执行: cp .env.example .env" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${env_file}"
set +a

if [[ -z "${AI_API_KEY:-}" || "${AI_API_KEY}" == "replace-with-your-bailian-api-key" ]]; then
  echo "请先在 .env 中填写阿里云百炼 AI_API_KEY（不要提交该文件）。" >&2
  exit 1
fi

cd "${project_dir}"
exec ./mvnw spring-boot:run
