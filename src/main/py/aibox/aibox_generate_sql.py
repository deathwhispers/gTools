import requests
import base64
from pathlib import Path


# 读取配置文件
def read_config():
    config = {}
    config_file = Path("aibox_generate_sql_config.txt")
    if config_file.exists():
        with open(config_file, "r") as f:
            for line in f:
                line = line.strip()
                if line and "=" in line:
                    key, value = line.split("=", 1)
                    config[key.strip()] = value.strip()
    return config

# 获取配置
config = read_config()
EMQX_IP = config.get("EMQX_IP", "100.65.105.15")
EMQX_URL = f"http://{EMQX_IP}:8081/api/v4/clients"
SQL_TEMPLATE = config.get("SQL_TEMPLATE", "update aibox.t_aibox_devinfo set real_sn = '{sn}' where bord_ip = '{ip}';")
OUTPUT_FILE = config.get("OUTPUT_FILE", "output.sql")
USERNAME = config.get("USERNAME", "admin")
PASSWORD = config.get("PASSWORD", "public")

def get_emqx_clients():
    try:
        # 构造Basic认证头
        auth_str = f"{USERNAME}:{PASSWORD}"
        auth_bytes = base64.b64encode(auth_str.encode('utf-8'))
        auth_header = auth_bytes.decode('utf-8')

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Basic {auth_header}"
        }

        response = requests.get(EMQX_URL, headers=headers)
        response.raise_for_status()  # 检查请求是否成功

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
    with open(OUTPUT_FILE, "w") as file:
        for sql in sql_statements:
            file.write(sql + "\n")
    print(f"SQL文件已生成: {OUTPUT_FILE}")

def main():
    print("获取EMQX客户端信息...")
    clients = get_emqx_clients()
    if clients:
        sql_statements = generate_sql(clients)
        save_to_file(sql_statements)
    else:
        print("无法获取客户端信息。请检查EMQX服务器配置。")

if __name__ == "__main__":
    main()