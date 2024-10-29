package kr.ac.kopo.realglow

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TabHost
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class SkinFragment : Fragment() {
    private lateinit var imgV: ImageView
    private lateinit var api: RetrofitAPI
    private lateinit var textViewContent: TextView
    private var itemLink: String? = null
    private var dataList: List<RetrofitDTO.skinText.Row> = listOf()
    private var selectedView: View? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_skin, container, false)
        imgV = activity?.findViewById(R.id.imgV) ?: throw IllegalStateException("ImageView not found")
        textViewContent = (activity as MainActivity).findViewById(R.id.textViewContent)

        val retrofit = (activity as MainActivity).retrofit
        api = retrofit.create(RetrofitAPI::class.java)

        setupTabs(view)
        callLoadItem("skin")  // 서버로부터 데이터를 로드합니다.

        return view
    }

    private fun setupTabs(view: View) {
        val tabHost = view.findViewById<TabHost>(R.id.tabHost)
        tabHost.setup()

        var spec = tabHost.newTabSpec("Tab 1")
        spec.setContent(R.id.tabproduct1)
        spec.setIndicator("밀키스테이")
        tabHost.addTab(spec)

        spec = tabHost.newTabSpec("Tab 2")
        spec.setContent(R.id.tabproduct2)
        spec.setIndicator("디자이너 리프트")
        tabHost.addTab(spec)

        // TabWidget에서 각 탭의 TextView를 가져와서 텍스트 크기를 변경
        for (i in 0 until tabHost.tabWidget.childCount) {
            val tv = tabHost.tabWidget.getChildAt(i).findViewById<TextView>(android.R.id.title)
            tv.textSize = 25f  // 텍스트 크기를 20sp로 설정
        }

        view.findViewById<View>(R.id.skin_none1).setOnClickListener {
            handleSkinColorClick(it, -1, -1)
        }
        view.findViewById<View>(R.id.skin_none2).setOnClickListener {
            handleSkinColorClick(it, -1, -1)
        }
        view.findViewById<View>(R.id.skinColor1_1).setOnClickListener {
            handleSkinColorClick(it, 0, 0)
        }
        view.findViewById<View>(R.id.skinColor1_2).setOnClickListener {
            handleSkinColorClick(it, 0, 1)
        }
        view.findViewById<View>(R.id.skinColor1_3).setOnClickListener {
            handleSkinColorClick(it, 0, 2)
        }
        view.findViewById<View>(R.id.skinColor2_1).setOnClickListener {
            handleSkinColorClick(it, 1, 0)
        }
        view.findViewById<View>(R.id.skinColor2_2).setOnClickListener {
            handleSkinColorClick(it, 1, 1)
        }
        view.findViewById<View>(R.id.skinColor2_3).setOnClickListener {
            handleSkinColorClick(it, 1, 2)
        }
    }

    private fun handleSkinColorClick(view: View, rowIndex: Int, colorIndex: Int) {
        val activity = activity as MainActivity
        val base64Image = activity.getOriginalBase64Image()

        if (base64Image != null) {
            if (view != null) {
                selectView(view)
            }
            if (rowIndex == -1 && colorIndex == -1) {
                activity.setSkinColor(listOf(255, 255, 255, -11))
                // 더보기란에서 헤어 관련 정보 삭제
                activity.selectedInfoList.removeIf { it.startsWith("Skin") }
                updateTextViewContent()  // 더보기란 갱신
            } else {
                // 새로운 색상을 선택한 경우
                val currentColor = dataList[rowIndex].Color[colorIndex]
                showItemInfo(rowIndex, colorIndex, "Skin")
                activity.setSkinColor(parseColor(currentColor))
            }

            // Parsing 작업 진행
            val parsingRequest = RetrofitDTO.ParsingRequest(
                img = base64Image,
                category = "skin",
                color = activity.makeupColorInfo.skinColor ?: listOf(255, 255, 255, -11)
            )

            api.postImage(parsingRequest).enqueue(object : Callback<RetrofitDTO.ParsingResponse> {
                override fun onResponse(call: Call<RetrofitDTO.ParsingResponse>, response: Response<RetrofitDTO.ParsingResponse>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            // 기존 applyMakeup 함수 호출
                            applyMakeup(base64Image, responseBody.PNGImage, activity.makeupColorInfo.skinColor ?: listOf(255, 255, 255, -11))
                        } else {
                            Log.e("Retrofit", "Failed to receive parsing response")
                        }
                    }
                }

                override fun onFailure(call: Call<RetrofitDTO.ParsingResponse>, t: Throwable) {
                    Log.e("Retrofit", "Failed to parse image: ${t.message}")
                }
            })
        } else {
            Toast.makeText(activity, "원본 이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectView(view: View) {
        // 이전에 선택된 뷰가 있다면 선택 상태 해제
        selectedView?.foreground = null

        // 현재 선택된 뷰에 선택 상태 표시 (ImageView와 일반 View 모두 처리)
        if (view is ImageView) {
            // ImageView의 경우 선택된 상태를 배경색이나 테두리로 표시
            view.foreground = resources.getDrawable(R.drawable.selected_border, null)
        } else {
            // 다른 View의 경우에도 동일하게 foreground로 표시
            view.foreground = resources.getDrawable(R.drawable.selected_border, null)
        }

        // 선택된 뷰를 업데이트
        selectedView = view
    }

    private fun callLoadItem(itemType: String) {
        api.getLoadItem(itemType).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    val gson = Gson()
                    val dataParsed = gson.fromJson(result, RetrofitDTO.skinText::class.java)
                    dataList = dataParsed.row
                } else {
                    Log.e("Retrofit", "Error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.e("Retrofit", "Failed to load items: ${t.message}")
            }
        })
    }

    private fun showItemInfo(rowIndex: Int, colorIndex: Int, category: String) {
        if (rowIndex < dataList.size && colorIndex < dataList[rowIndex].Color.size) {
            val item = dataList[rowIndex]
            val colorName = item.ColorName[colorIndex]
            val itemName = item.itemName
            val link = item.Link  // 서버에서 가져온 링크

            val activity = activity as MainActivity
            // 새로운 정보로 업데이트
            val itemInfoWithLink = "$category - [$itemName] $colorName - $link"
            activity.updateSelectedInfo(itemInfoWithLink)  // 업데이트된 정보를 리스트의 최상단에 추가

            updateTextViewContent()
        }
    }

    private fun updateTextViewContent() {
        val activity = activity as MainActivity
        val content = SpannableStringBuilder()

        // selectedInfoList의 각 항목을 SpannableString으로 처리
        for (infoWithLink in activity.selectedInfoList) {
            val linkStart = infoWithLink.lastIndexOf(" - ") + 3
            val info = infoWithLink.substring(0, linkStart - 3)  // 링크를 제외한 정보
            val link = infoWithLink.substring(linkStart)  // 링크만 추출

            val spannableString = SpannableString(info)

            // 제품명에 대한 하이퍼링크 적용
            val itemNameStart = info.indexOf('[')
            val itemNameEnd = info.indexOf(']') + 1
            if (itemNameStart >= 0 && itemNameEnd > itemNameStart && link.isNotEmpty()) {
                spannableString.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                            context?.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("LipFragment", "Error opening link: ${e.message}")
                            Toast.makeText(context, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, itemNameStart, itemNameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // 항목을 content에 추가
            content.append(spannableString).append("\n")
        }

        textViewContent.text = content
        textViewContent.movementMethod = LinkMovementMethod.getInstance()
    }

    fun applyMakeup(base64Image: String, parsedImage: String, skinColor: List<Int>) {
        val transparentColor = listOf(255, 255, 255, -11)
        val mainActivity = activity as? MainActivity ?: return // MainActivity가 null이면 함수 종료
        val makeupRequest = RetrofitDTO.MakeupRequest(
            img = base64Image,
            parsing = parsedImage,
            skin_color = mainActivity.makeupColorInfo.skinColor ?: skinColor,
            hair_color = mainActivity.makeupColorInfo.hairColor ?: transparentColor,
            lip_color = mainActivity.makeupColorInfo.lipColor ?: transparentColor
        )

        api.postMakeup(makeupRequest).enqueue(object : Callback<RetrofitDTO.MakeupResponse> {
            override fun onResponse(call: Call<RetrofitDTO.MakeupResponse>, response: Response<RetrofitDTO.MakeupResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        decodeBase64ToImageView(responseBody.changeImg)
                    }
                }
            }

            override fun onFailure(call: Call<RetrofitDTO.MakeupResponse>, t: Throwable) {
                Log.e("Retrofit", "Failed to apply makeup: ${t.message}")
            }
        })
    }

    private fun convertImageViewToBase64(): String? {
        val drawable = imgV.drawable as? BitmapDrawable
        val bitmap = drawable?.bitmap
        return if (bitmap != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } else {
            null
        }
    }

    private fun decodeBase64ToImageView(base64String: String) {
        if (base64String.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                imgV.setImageBitmap(decodedBitmap)
            } catch (e: IllegalArgumentException) {
                Log.e("Retrofit", "Base64 decoding failed: ${e.message}")
            }
        }
    }

    private fun parseColor(colorHex: String): List<Int> {
        val color = android.graphics.Color.parseColor(colorHex)
        return listOf(
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color),
            1
        )
    }
}
