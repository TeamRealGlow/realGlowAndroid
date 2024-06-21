package kr.ac.kopo.realglow;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path

interface RetrofitAPI {
    @GET("/loadItem/{hairItem}")
    fun getLoadHairItem(@Path("hairItem") hairItem: String): Call<JsonObject>

    @GET("/loadItem/{skinItem}")
    fun getLoadSkinItem(@Path("skinItem") skinItem: String): Call<JsonObject>

    @GET("/loadItem/{lipItem}")
    fun getLoadLipItem(@Path("lipItem") lipItem: String): Call<JsonObject>
}
