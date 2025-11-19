package com.example.golfcoach.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.golfcoach.network.LoginResponse
import com.example.golfcoach.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginScreen(
    onLoginSuccess: (Int) -> Unit
) {
    // true = 登录模式；false = 注册模式
    var isLoginMode by remember { mutableStateOf(true) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // 标题 + 模式切换
                Text(
                    text = "GolfCoach AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isLoginMode) "Login to your account" else "Create a new account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(24.dp))

                // ===== 表单区域 =====

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isLoginMode) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 提示信息
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (info != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = info!!,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ===== 主按钮：登录 / 注册 =====
                Button(
                    onClick = {
                        error = null
                        info = null

                        // 基本输入校验
                        if (email.isBlank() || password.isBlank()) {
                            error = "Email and password cannot be empty."
                            return@Button
                        }
                        if (!email.contains("@")) {
                            error = "Please enter a valid email address."
                            return@Button
                        }
                        if (password.length < 6) {
                            error = "Password should be at least 6 characters."
                            return@Button
                        }

                        if (!isLoginMode) {
                            // 注册模式多检查一次确认密码
                            if (password != confirmPassword) {
                                error = "Passwords do not match."
                                return@Button
                            }
                        }

                        loading = true

                        if (isLoginMode) {
                            // ===== 调用登录接口 =====
                            RetrofitClient.api.login(email, password)
                                .enqueue(object : Callback<LoginResponse> {
                                    override fun onResponse(
                                        call: Call<LoginResponse>,
                                        response: Response<LoginResponse>
                                    ) {
                                        loading = false
                                        if (response.isSuccessful && response.body() != null) {
                                            val body = response.body()!!
                                            onLoginSuccess(body.user_id)
                                        } else {
                                            error = "Login failed: ${response.code()}"
                                        }
                                    }

                                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                        loading = false
                                        error = "Network error: ${t.message}"
                                    }
                                })
                        } else {
                            // ===== 调用注册接口 =====
                            RetrofitClient.api.register(email, password)
                                .enqueue(object : Callback<LoginResponse> {
                                    override fun onResponse(
                                        call: Call<LoginResponse>,
                                        response: Response<LoginResponse>
                                    ) {
                                        loading = false
                                        if (response.isSuccessful && response.body() != null) {
                                            val body = response.body()!!
                                            info = "Register success. Logging in..."
                                            // 注册成功后，直接视为登录成功
                                            onLoginSuccess(body.user_id)
                                        } else if (response.code() == 400) {
                                            error = "This email is already registered."
                                        } else {
                                            error = "Register failed: ${response.code()}"
                                        }
                                    }

                                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                        loading = false
                                        error = "Network error: ${t.message}"
                                    }
                                })
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            loading && isLoginMode -> "Logging in..."
                            loading && !isLoginMode -> "Registering..."
                            isLoginMode -> "Login"
                            else -> "Register"
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ===== 模式切换文案 =====
                Row {
                    Text(
                        text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (isLoginMode) "Register" else "Login",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            // 切换模式时，清空一些状态
                            isLoginMode = !isLoginMode
                            error = null
                            info = null
                            password = ""
                            confirmPassword = ""
                        }
                    )
                }
            }
        }
    }
}
