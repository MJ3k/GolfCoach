from fastapi import FastAPI, Depends, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
import os

from database import Base, engine, get_db
from models import User, Video
from ai_dummy import analyze_video

Base.metadata.create_all(bind=engine)

app = FastAPI()

origins = [
    "http://localhost",
    "http://127.0.0.1",
    "http://10.0.2.2",
    "*",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

VIDEO_DIR = "videos"
os.makedirs(VIDEO_DIR, exist_ok=True)

# 用户注册 & 登录
@app.post("/auth/register")
def register(
    email: str = Form(...),
    password: str = Form(...),
    db: Session = Depends(get_db)
):
    if db.query(User).filter(User.email == email).first():
        raise HTTPException(status_code=400, detail="Email already registered")
    user = User(email=email, password=password)
    db.add(user)
    db.commit()
    db.refresh(user)
    return {"user_id": user.id, "email": user.email}


@app.post("/auth/login")
def login(
    email: str = Form(...),
    password: str = Form(...),
    db: Session = Depends(get_db)
):
    user = db.query(User).filter(
        User.email == email,
        User.password == password
    ).first()
    if not user:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return {"user_id": user.id, "email": user.email}


# 视频上传 / 列表 / 播放
@app.post("/videos/upload")
async def upload_video(
    user_id: int = Form(...),
    title: str = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    save_name = f"user{user_id}_{file.filename}"
    save_path = os.path.join(VIDEO_DIR, save_name)

    with open(save_path, "wb") as f:
        f.write(await file.read())

    video = Video(title=title, file_path=save_path, owner_id=user_id)
    db.add(video)
    db.commit()
    db.refresh(video)

    return {"video_id": video.id, "title": video.title}


@app.get("/videos")
def list_videos(user_id: int, db: Session = Depends(get_db)):
    videos = db.query(Video).filter(Video.owner_id == user_id).all()
    return [
        {"video_id": v.id, "title": v.title}
        for v in videos
    ]


@app.get("/videos/{video_id}")
def stream_video(video_id: int, db: Session = Depends(get_db)):
    video = db.query(Video).filter(Video.id == video_id).first()
    if not video:
        raise HTTPException(status_code=404, detail="Video not found")
    return FileResponse(video.file_path, media_type="video/mp4")


# upcoming

@app.post("/ai/analyze/{video_id}")
def ai_analyze(video_id: int, db: Session = Depends(get_db)):
    video = db.query(Video).filter(Video.id == video_id).first()
    if not video:
        raise HTTPException(status_code=404, detail="Video not found")

    result = analyze_video(video.file_path)
    return {
        "video_id": video.id,
        "title": video.title,
        "analysis": result
    }
