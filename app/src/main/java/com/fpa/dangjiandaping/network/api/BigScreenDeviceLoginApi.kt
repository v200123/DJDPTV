package com.fpa.dangjiandaping.network.api

import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginRequest
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginResponse
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/** 大屏设备认证接口定义。 */
interface BigScreenDeviceLoginApi {
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("swapi/TokenAuth/siginByDeviceCode")
    suspend fun login(
        @Body request: BigScreenDeviceLoginRequest,
    ): Response<BigScreenDeviceLoginResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("swapi/TokenAuth/siginByDeviceCode")
    suspend fun loginRaw(
        @Body request: BigScreenDeviceLoginRequest,
    ): Response<ResponseBody>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("swapi/services/app/learnTimes/add")
    suspend fun addLearnTime(
        @Header("Authorization") authorization: String,
        @Body request: LearnTimesRequest,
    ): Response<ResponseBody>
}

@Serializable
data class LearnTimesRequest(
    val courseId: String,
    val coursewareId: String,
    val userId: String,
    val organId: String,
    val learnHours: Int = 900,
    val totalTimes: Int = 2,
    val type: Int = 1,
    val watchTimes: Int = 1,
    val classId: String,
)
