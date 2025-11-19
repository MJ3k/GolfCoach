from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# 使用 MySQL + pymysql
# 格式：mysql+pymysql://用户名:密码@主机:端口/数据库名?charset=utf8mb4
SQLALCHEMY_DATABASE_URL = "mysql+pymysql://golfuser:password@localhost:3306/golfcoach?charset=utf8mb4"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    pool_pre_ping=True,   # 避免连接断开带来的问题
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
