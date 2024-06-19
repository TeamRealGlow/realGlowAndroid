package kr.ac.kopo.realglow;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;

public interface RetrofitAPI {
    @GET("/loadItem/hair")
    fun getLoadItem(): Call<JsonObject>
}
