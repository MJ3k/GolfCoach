package com.example.golfcoach.network

// 1. 引入 Retrofit 注解和 Call 类型
import retrofit2.Call
import retrofit2.http.*

// 2. 引入上传文件需要的类型
import okhttp3.MultipartBody
import okhttp3.RequestBody

// ---------- 后端返回的数据模型 ----------

// 登录返回：{"user_id": 1, "email": "xxx"}
data class LoginResponse(
    val user_id: Int,
    val email: String
)

// 单个视频在列表里的信息：{"video_id": 1, "title": "My Swing"}
data class VideoItem(
    val video_id: Int,
    val title: String
)

// 上传视频的返回：{"video_id": 1, "title": "My Swing"}
data class UploadResponse(
    val video_id: Int,
    val title: String
)

// AI 分析结果里的细节部分
data class AnalysisDetail(
    val head_stability: Double,
    val x_factor: Double,
    val hand_speed: Double,
    val suggestion: String
)

// AI 分析接口总返回：
// {
//   "video_id": 1,
//   "title": "My Swing",
//   "analysis": { ... 上面那几个字段 ... }
// }
data class AiAnalysis(
    val video_id: Int,
    val title: String,
    val analysis: AnalysisDetail
)

// ---------- 定义访问后端的所有 HTTP 接口 ----------

// 注意：这里的路径要和 FastAPI 里的 @app.post("/xxx") 对得上
interface ApiService {

    // 注册接口：POST /auth/register
    @FormUrlEncoded
    @POST("/auth/register")
    fun register(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // 1. 登录接口：POST /auth/login
    // FastAPI 版本：
    // @app.post("/auth/login")
    // def login(email: str = Form(...), password: str = Form(...))
    @FormUrlEncoded
    @POST("/auth/login")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // 2. 上传视频接口：POST /videos/upload
    // FastAPI:
    // @app.post("/videos/upload")
    // async def upload_video(user_id: int = Form(...), title: str = Form(...), file: UploadFile = File(...))
    @Multipart
    @POST("/videos/upload")
    fun uploadVideo(
        // 文本字段 user_id，对应 Form(...)，这里用 RequestBody
        @Part("user_id") userId: RequestBody,
        // 文本字段 title
        @Part("title") title: RequestBody,
        // 文件字段 file
        @Part file: MultipartBody.Part
    ): Call<UploadResponse>

    // 3. 获取视频列表：GET /videos?user_id=xxx
    // FastAPI:
    // @app.get("/videos")
    // def list_videos(user_id: int, ...)
    @GET("/videos")
    fun getVideos(
        @Query("user_id") userId: Int
    ): Call<List<VideoItem>>

    // 4. AI 分析：POST /ai/analyze/{video_id}
    // FastAPI:
    // @app.post("/ai/analyze/{video_id}")
    @POST("/ai/analyze/{video_id}")
    fun analyzeVideo(
        @Path("video_id") videoId: Int
    ): Call<AiAnalysis>
}
