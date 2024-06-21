package kr.ac.kopo.realglow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TabHost
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SkinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SkinFragment : Fragment() {
    // TODO: Rename and change types of parameters
    val TAG = "TAG_MainActivity" // 로그 분류 태그
    private var param1: String? = null
    private var param2: String? = null
    lateinit var textViewContent : TextView

    lateinit var mRetrofit: Retrofit
    lateinit var mRetrofitAPI: RetrofitAPI
    lateinit var mCallSkinItemInfo: retrofit2.Call<JsonObject>

    private var itemLink: String? = null

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
        val view = inflater.inflate(R.layout.fragment_skin, container, false)

        textViewContent = activity?.findViewById(R.id.textViewContent) ?: throw IllegalStateException("TextView not found")

        // TabHost 초기화
        val tabHost = view.findViewById<TabHost>(R.id.tabHost)
        tabHost.setup()

        // Tab1 설정
        var spec = tabHost.newTabSpec("Tab 1")
        spec.setContent(R.id.tabproduct1)
        spec.setIndicator("제품명1")
        tabHost.addTab(spec)

        // Tab2 설정
        spec = tabHost.newTabSpec("Tab 2")
        spec.setContent(R.id.tabproduct2)
        spec.setIndicator("제품명2")
        tabHost.addTab(spec)

        // Tab3 설정
        spec = tabHost.newTabSpec("Tab 3")
        spec.setContent(R.id.tabproduct3)
        spec.setIndicator("제품명3")
        tabHost.addTab(spec)



        setRetrofit()//레트로핏 세팅
        val item1 = view.findViewById<View>(R.id.skinColor1_1)
        //버튼 클릭하면 가져오기
        item1.setOnClickListener {
            callLoadItem("skin")
        }

        textViewContent.setOnClickListener {
            itemLink?.let {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                startActivity(intent)
            }
        }




        return view

    }

    private fun setRetrofit(){
        //레트로핏으로 가져올 url설정하고 세팅
        mRetrofit = Retrofit
            .Builder()
            .baseUrl(getString(R.string.baseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //인터페이스로 만든 레트로핏 api요청 받는 것 변수로 등록
        mRetrofitAPI = mRetrofit.create(RetrofitAPI::class.java)
    }



    private val mRetrofitCallback = (object : retrofit2.Callback<JsonObject>{
        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            // 서버에서 데이터 요청 성공시
            val result = response.body()
            Log.d("testt", "결과는 ${result}")
            var gson = Gson();
            val dataParsed1 = gson.fromJson(result, RetrofitDTO.skinText::class.java)

            if (dataParsed1.row.isNotEmpty()) {
                val firstItem = dataParsed1.row[0]
                val stringBuilder = StringBuilder()
                stringBuilder.append("${firstItem.itemName}\t${firstItem.ColorName[0]}\n${firstItem.Link}")

                textViewContent.text = stringBuilder.toString()
                itemLink = firstItem.Link
            } else {
                textViewContent.text = "No data available"
            }
            //val chatItem = RetrofitDTO.hairText(dataParsed1.Item, TYPE_BOT)
//            Log.d("testt",dataParsed1.row[0].Link );
        }

        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            // 서버 요청 실패
            t.printStackTrace()
            Log.d("testt", "에러입니다. ${t.message}")
        }
    })

    private fun callLoadItem(skinItem: String){
        mCallSkinItemInfo = mRetrofitAPI.getLoadSkinItem(skinItem) // RetrofitAPI 에서 JSON 객체를 요청해서 반환하는 메소드 호출
        mCallSkinItemInfo.enqueue(mRetrofitCallback) // 응답을 큐에 넣어 대기 시켜놓음. 즉, 응답이 생기면 뱉어낸다.
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SkinFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SkinFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}