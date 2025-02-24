import redis
import sys
from pathlib import Path

# 读取配置文件
def read_config():
    config = {}
    config_file = Path("clean_redis_config.txt")
    if config_file.exists():
        with open(config_file, "r") as f:
            for line in f:
                line = line.strip()
                if line and "=" in line:
                    key, value = line.split("=", 1)
                    config[key.strip()] = value.strip()
    return config

def main():
    # 加载配置
    config = read_config()
    host = config.get("host", "127.0.0.1")
    port = int(config.get("port", "6379"))
    password = config.get("password", "Allen@rhy&0825#21")
    db_list = [int(db) for db in config.get("db_list", "0").split(",")]
    key_prefix_list = config.get("key_prefix_list", "").split(",")

    # 连接到Redis
    try:
        r = redis.StrictRedis(host=host, port=port, password=password, decode_responses=True)
    except redis.exceptions.ConnectionError as e:
        print(f"无法连接到Redis服务器: {e}")
        sys.exit(1)

    # 遍历需要清理的数据库
    for db in db_list:
        print(f"切换到数据库 {db}...")
        r.execute_command("SELECT", db)

        # 遍历需要清理的Key前缀
        for prefix in key_prefix_list:
            if not prefix:
                continue
            print(f"清理Key前缀: {prefix}")
            cursor, keys = r.scan(match=f"{prefix}*", count=1000)
            while cursor != 0 or keys:
                if keys:
                    r.delete(*keys)
                    print(f"已删除 {len(keys)} 个Key: {keys}")
                cursor, keys = r.scan(cursor=cursor, match=f"{prefix}*", count=1000)

    print("清理完成！")

if __name__ == "__main__":
    main()