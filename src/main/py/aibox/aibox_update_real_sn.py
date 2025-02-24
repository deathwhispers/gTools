import requests
import base64
import mysql.connector
from pathlib import Path
import redis
import sys
import yaml  # 用于解析 YAML 配置文件


# 读取 YAML 配置文件
def read_config():
    config = {}
    config_file = Path("aibox_update_real_sn_config.yml")
    if config_file.exists():
        with open(config_file, "r", encoding="utf-8") as f:
            try:
                config = yaml.safe_load(f)
            except yaml.YAMLError as e:
                print(f"YAML 解析失败: {e}")
    return config

# 获取配置
config = read_config()

# EMQX 配置
EMQX_CONFIG = config.get("EMQX", {})
EMQX_IP = EMQX_CONFIG.get("IP", "100.65.105.15")
EMQX_USERNAME = EMQX_CONFIG.get("USERNAME", "admin")
EMQX_PASSWORD = EMQX_CONFIG.get("PASSWORD", "public")
HTTP_TIMEOUT = EMQX_CONFIG.get("TIMEOUT", 5)
EMQX_URL = f"http://{EMQX_IP}:8081/api/v4/clients"
print(f"EMQX_USERNAME: {EMQX_USERNAME}")
print(f"EMQX_PASSWORD: {EMQX_PASSWORD}")
print(f"EMQX_URL: {EMQX_URL}")
# 数据库配置
MYSQL_CONFIG = config.get("MYSQL", {})
MYSQL_HOST = MYSQL_CONFIG.get("HOST", "localhost")
MYSQL_USER = MYSQL_CONFIG.get("USERNAME", "root")
MYSQL_PASSWORD = MYSQL_CONFIG.get("PASSWORD", "password")
MYSQL_DB = MYSQL_CONFIG.get("DB", "your_database")
ENABLE_SQL_EXECUTION = MYSQL_CONFIG.get("ENABLE_SQL_EXECUTION", False)
MYSQL_CONNECT_TIMEOUT = MYSQL_CONFIG.get("CONNECT_TIMEOUT", 5)

# Redis 配置
REDIS_CONFIG = config.get("REDIS", {})
REDIS_HOST = REDIS_CONFIG.get("HOST", "127.0.0.1")
REDIS_PORT = REDIS_CONFIG.get("PORT", 6379)
REDIS_PASSWORD = REDIS_CONFIG.get("PASSWORD", "Allen@rhy&0825#21")
REDIS_DB_LIST = REDIS_CONFIG.get("DB_LIST", [0])
REDIS_KEY_PREFIX_LIST = REDIS_CONFIG.get("KEY_PREFIX_LIST", [])
ENABLE_REDIS_CLEANUP = REDIS_CONFIG.get("ENABLE_CLEANUP", True)
REDIS_CONNECT_TIMEOUT = REDIS_CONFIG.get("CONNECT_TIMEOUT", 5)
print(f"REDIS_HOST: {REDIS_HOST}")
print(f"REDIS_PORT: {REDIS_PORT}")
# SQL 配置
SQL_TEMPLATE = config.get("SQL_TEMPLATE", "update aibox.t_aibox_devinfo set real_sn = '{sn}' where bord_ip = '{ip}';")
OUTPUT_FILE = config.get("OUTPUT_FILE", "output.sql")

# 获取EMQX客户端信息
def get_emqx_clients():
    try:
        # 构造Basic认证头
        auth_str = f"{EMQX_USERNAME}:{EMQX_PASSWORD}"
        auth_bytes = base64.b64encode(auth_str.encode('utf-8'))
        auth_header = auth_bytes.decode('utf-8')

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Basic {auth_header}"
        }
        print(f"HTTP_TIMEOUT: {HTTP_TIMEOUT}")
        try:
            response = requests.get(EMQX_URL, headers=headers, timeout=HTTP_TIMEOUT)
            response.raise_for_status()
            print("请求成功！")
        except requests.exceptions.Timeout as e:
            print(f"请求超时: {e}")
        except requests.exceptions.HTTPError as e:
            print(f"HTTP 错误: {e}")
            print(response.text)
        except requests.exceptions.RequestException as e:
            print(f"请求失败: {e}")

        clients = response.json()
        return clients.get("data", [])  # 假设API返回的结构是 {"data": [...]}

    except requests.exceptions.RequestException as e:
        print(f"请求失败: {e}")
        return None

def generate_sql(clients):
    sql_statements = []
    for client in clients:
        clientid = client.get("clientid")
        ip_address = client.get("ip_address")
        # 处理 clientid，去掉 "_lan" 后缀
        if clientid and "_lan" not in clientid:
            continue
        # 处理 clientid，去掉 "_lan" 后缀
        sn = clientid.replace("_lan", "")  # 生成SN编号
        if ip_address and sn:
            sql = SQL_TEMPLATE.format(sn=sn, ip=ip_address)
            sql_statements.append(sql)
    return sql_statements

def save_to_file(sql_statements):
    try:
        with open(OUTPUT_FILE, "w") as file:
            for sql in sql_statements:
                file.write(sql + "\n")
        print(f"SQL文件已生成: {OUTPUT_FILE}")
    except Exception as e:
        print(f"保存 SQL 文件失败: {e}")

def execute_sql(sql_statements):
    try:
        # 连接数据库
        conn = mysql.connector.connect(
            host=MYSQL_HOST,
            user=MYSQL_USER,
            password=MYSQL_PASSWORD,
            database=MYSQL_DB,
            connect_timeout=MYSQL_CONNECT_TIMEOUT
        )
        cursor = conn.cursor()

        # 执行每条SQL语句
        for sql in sql_statements:
            cursor.execute(sql)
        # 提交事务
        conn.commit()

        rowcount = cursor.rowcount
        print(f"成功执行了 {rowcount} 条SQL语句。")

    except mysql.connector.Error as e:
        if e.errno == errorcode.ER_ACCESS_DENIED_ERROR:
            print("MySQL  用户名或密码错误。")
        elif e.errno == errorcode.ER_BAD_DB_ERROR:
            print("数据库不存在。")
        else:
            print(f"数据库错误: {e}")
    except Exception as e:
        print(f"执行SQL时发生错误: {e}")
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()

def clean_redis_cache():
    try:
        # 连接到Redis
        r = redis.StrictRedis(
            host=REDIS_HOST,
            port=REDIS_PORT,
            password=REDIS_PASSWORD,
            decode_responses=True,
            socket_timeout=REDIS_CONNECT_TIMEOUT
        )
    except redis.exceptions.ConnectionError as e:
        print(f"无法连接到Redis服务器: {e}")
        return

    # 遍历需要清理的数据库
    for db in REDIS_DB_LIST:
        print(f"切换到数据库 {db}...")
        try:
            r.execute_command("SELECT", db)
        except redis.exceptions.ResponseError as e:
            print(f"Redis 错误: {e}")
            continue

        # 遍历需要清理的Key前缀
        for prefix in REDIS_KEY_PREFIX_LIST:
            if not prefix:
                continue
            print(f"清理Key前缀: {prefix}")
            cursor, keys = r.scan(match=f"{prefix}*", count=1000)
            while cursor != 0 or keys:
                if keys:
                    r.delete(*keys)
                    print(f"已删除 {len(keys)} 个Key: {keys}")
                try:
                    cursor, keys = r.scan(cursor=cursor, match=f"{prefix}*", count=1000)
                except redis.exceptions.ResponseError as e:
                    print(f"Redis 错误: {e}")
                    break

    print("Redis 缓存清理完成！")

def main():
    print("获取EMQX客户端信息...")
    clients = get_emqx_clients()
    print(f"clients: {clients}")
    if not clients:
        print("未获取到任何客户端信息。脚本退出。")
        sys.exit(1)  # 退出脚本

    sql_statements = generate_sql(clients)
    save_to_file(sql_statements)

    # 是否执行生成的SQL语句
    if ENABLE_SQL_EXECUTION:
        execute_sql(sql_statements)  # 执行SQL语句
    else:
        print("SQL语句未执行，不启用SQL执行功能！")

    # 如果启用了Redis清理功能，执行清理
    if ENABLE_REDIS_CLEANUP:
        clean_redis_cache()
    else:
        print("Redis 缓存未清理，不启用Redis 缓存清理功能！")

if __name__ == "__main__":
    main()
