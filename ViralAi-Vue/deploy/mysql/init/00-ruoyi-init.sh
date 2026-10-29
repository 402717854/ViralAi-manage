#!/bin/bash
# =============================================================================
# RuoYi 数据库初始化
# 挂载到 MySQL 官方镜像的 /docker-entrypoint-initdb.d/，仅在数据目录为空时执行一次。
# 必须显式指定顺序：字母顺序会把 crm_model_billing.sql 排到最前，
# 而它里面有往 sys_dict_type / sys_dict_data 的 insert，依赖 ry_20260417.sql 先建表。
# =============================================================================
set -euo pipefail

SQL_DIR="${RUOYI_SQL_DIR:-/opt/ruoyi-sql}"

if [ -z "${MYSQL_DATABASE:-}" ]; then
    echo "[ruoyi-init] MYSQL_DATABASE 未设置，跳过初始化"
    exit 0
fi

MYSQL_ARGS=(--protocol=socket -uroot)
if [ -n "${MYSQL_ROOT_PASSWORD:-}" ]; then
    MYSQL_ARGS+=("-p${MYSQL_ROOT_PASSWORD}")
fi

# 顺序不能调整
FILES=(
    ry_20260417.sql
    quartz.sql
    crm_model_billing.sql
    crm_supplier_project.sql
)

for file in "${FILES[@]}"; do
    if [ ! -f "${SQL_DIR}/${file}" ]; then
        echo "[ruoyi-init] 跳过 ${file}（文件不存在）"
        continue
    fi
    echo "[ruoyi-init] 导入 ${file} -> ${MYSQL_DATABASE}"
    mysql "${MYSQL_ARGS[@]}" "${MYSQL_DATABASE}" < "${SQL_DIR}/${file}"
done

echo "[ruoyi-init] 初始化完成"
