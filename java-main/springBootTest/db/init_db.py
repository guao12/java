# -*- coding: utf-8 -*-
"""
kr_build 数据库初始化脚本（等效于 mysql -uroot -p123456 < db/init.sql）
本机没有 mysql 命令行客户端时使用。
用法：python init_db.py
"""
import os
import re
import sys

import pymysql

SQL_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "init.sql")

def db_exists(cur, name):
    cur.execute("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME=%s", (name,))
    return cur.fetchone() is not None

def main():
    force = "--force" in sys.argv

    conn = pymysql.connect(host="127.0.0.1", port=3306, user="root",
                           password="123456", charset="utf8mb4", autocommit=True)
    try:
        with conn.cursor() as cur:
            if db_exists(cur, "kr_build") and not force:
                cur.execute("SELECT COUNT(*) FROM kr_build.kr_user")
                print(f"SKIP: kr_build 已存在，用户数 = {cur.fetchone()[0]}（如需重建加 --force）")
                return

        with open(SQL_FILE, "r", encoding="utf-8") as f:
            sql = f.read()

        # 去掉 -- 开头的单行注释，避免驱动误解析
        sql = re.sub(r"^\s*--.*$", "", sql, flags=re.MULTILINE)

        with conn.cursor() as cur:
            for stmt in sql.split(";"):
                stmt = stmt.strip()
                if not stmt:
                    continue
                cur.execute(stmt)
                # 打印 SELECT 的结果
                if stmt.upper().startswith("SELECT"):
                    for row in cur.fetchall():
                        print(row[0])
        # 验证
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM kr_build.kr_user")
            count = cur.fetchone()[0]
            print(f"OK: kr_build.kr_user 就绪，当前用户数 = {count}")
    finally:
        conn.close()

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAILED: {e}", file=sys.stderr)
        sys.exit(1)
