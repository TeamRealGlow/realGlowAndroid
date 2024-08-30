package kr.ac.kopo.realglow
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class faceparsingDTO {
    @Expose
    @SerializedName("img")
    private var img: String? = null

    @SerializedName("PNGImage")
    private val PNGImage: String? = null

    fun getText(): String? {
        return img
    }

    fun setText(text: String?) {
        this.img = text
    }

    fun getResult(): String? {
        return PNGImage
    }
}