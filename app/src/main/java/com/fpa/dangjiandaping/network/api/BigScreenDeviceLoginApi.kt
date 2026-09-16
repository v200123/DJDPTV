package com.fpa.dangjiandaping.network.api

import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginRequest
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/** 大屏设备认证接口定义。 */
interface BigScreenDeviceLoginApi {
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("swapi/TokenAuth/siginByDeviceCode")
    suspend fun login(
        @Body request: BigScreenDeviceLoginRequest,
    ): Response<BigScreenDeviceLoginResponse>
}
