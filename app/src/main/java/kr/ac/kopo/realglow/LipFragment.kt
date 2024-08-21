package kr.ac.kopo.realglow

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TabHost
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class LipFragment : Fragment() {
    val TAG = "TAG_MainActivity" // 로그 분류 태그
    private var param1: String? = null
    private var param2: String? = null
    lateinit var textViewContent: TextView
    lateinit var imgV: ImageView

    lateinit var mRetrofit: Retrofit
    lateinit var mRetrofitAPI: RetrofitAPI
    lateinit var mCallLipItemInfo: retrofit2.Call<JsonObject>

    private var itemLink: String? = null
    private var dataList: List<RetrofitDTO.lipText.Row> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_lip, container, false)

        textViewContent = activity?.findViewById(R.id.textViewContent)
            ?: throw IllegalStateException("TextView not found")
        imgV = activity?.findViewById(R.id.imgV)
            ?: throw IllegalStateException("ImageView not found")

        // TabHost 초기화
        val tabHost = view.findViewById<TabHost>(R.id.tabHost)
        tabHost.setup()

        // Tab 설정
        var spec = tabHost.newTabSpec("Tab 1")
        spec.setContent(R.id.tabproduct1)
        spec.setIndicator("제품명1")
        tabHost.addTab(spec)

        spec = tabHost.newTabSpec("Tab 2")
        spec.setContent(R.id.tabproduct2)
        spec.setIndicator("제품명2")
        tabHost.addTab(spec)

        spec = tabHost.newTabSpec("Tab 3")
        spec.setContent(R.id.tabproduct3)
        spec.setIndicator("제품명3")
        tabHost.addTab(spec)

        setRetrofit() // 레트로핏 세팅

        val lipColor1_1 = view.findViewById<View>(R.id.lipColor1_1)
        val lipColor1_2 = view.findViewById<View>(R.id.lipColor1_2)
        val lipColor2_1 = view.findViewById<View>(R.id.lipColor2_1)
        val lipColor2_2 = view.findViewById<View>(R.id.lipColor2_2)

        // 버튼 클릭 리스너 설정
        val clickListener = View.OnClickListener { v ->
            if (imgV.drawable != null && imgV.drawable is BitmapDrawable) {
                when (v.id) {
                    R.id.hairColor1_1 -> showItemInfo(0, 0)
                    R.id.hairColor1_2 -> showItemInfo(0, 1)
                    R.id.hairColor2_1 -> showItemInfo(1, 0)
                    R.id.hairColor2_2 -> showItemInfo(1, 1)
                }
            } else {
                Toast.makeText(activity, "이미지를 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        lipColor1_1.setOnClickListener(clickListener)
        lipColor1_2.setOnClickListener(clickListener)
        lipColor2_1.setOnClickListener(clickListener)
        lipColor2_2.setOnClickListener(clickListener)

        textViewContent.setOnClickListener {
            itemLink?.let {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                startActivity(intent)
            }
        }

        callLoadItem("lip") // 데이터를 로드하는 함수 호출

        return view
    }

    private fun setRetrofit() {
        // 레트로핏으로 가져올 url 설정하고 세팅
        mRetrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.baseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 인터페이스로 만든 레트로핏 api 요청 받는 것 변수로 등록
        mRetrofitAPI = mRetrofit.create(RetrofitAPI::class.java)
    }

    private fun callLoadItem(lipItem: String) {
        mCallLipItemInfo = mRetrofitAPI.getLoadItem(lipItem) // RetrofitAPI에서 JSON 객체를 요청해서 반환하는 메소드 호출
        mCallLipItemInfo.enqueue(mRetrofitCallback) // 응답을 큐에 넣어 대기시킴
    }

    private fun showItemInfo(rowIndex: Int, colorIndex: Int) {
        if (rowIndex < dataList.size && colorIndex < dataList[rowIndex].Color.size) {
            val item = dataList[rowIndex]
            val color = item.Color[colorIndex]
            val colorName = item.ColorName[colorIndex]
            val stringBuilder = StringBuilder()
            stringBuilder.append("Item Name: ${item.itemName}\t\t\t $colorName \n ${item.Link}")

            textViewContent.text = stringBuilder.toString()
            itemLink = item.Link
        } else {
            textViewContent.text = "No data available"
        }
    }


    private val mRetrofitCallback = object : retrofit2.Callback<JsonObject> {
        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            // 서버에서 데이터 요청 성공 시
            val result = response.body()
            Log.d("testt", "결과는 $result")
            val gson = Gson()
            val dataParsed1 = gson.fromJson(result, RetrofitDTO.lipText::class.java)

            dataList = dataParsed1.row
        }

        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            // 서버 요청 실패
            t.printStackTrace()
            Log.d("testt", "에러입니다. ${t.message}")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LipFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
