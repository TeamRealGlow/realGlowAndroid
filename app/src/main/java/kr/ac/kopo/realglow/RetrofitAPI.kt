package kr.ac.kopo.realglow

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RetrofitAPI {
    @GET("/loadItem/{itemType}")
    fun getLoadItem(@Path("itemType") itemType: String): Call<JsonObject>

    @POST("/parsing")
    fun postImage(@Body requestBody: RetrofitDTO.ParsingRequest): Call<RetrofitDTO.ParsingResponse>

    @POST("/makeup")
    fun postMakeup(@Body requestBody: RetrofitDTO.MakeupRequest): Call<RetrofitDTO.MakeupResponse>
}


