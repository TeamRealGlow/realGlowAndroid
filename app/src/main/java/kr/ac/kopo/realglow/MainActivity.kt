package kr.ac.kopo.realglow

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class MainActivity : AppCompatActivity() {
    lateinit var category_skin : TextView
    lateinit var category_lip : TextView
    lateinit var category_hair : TextView
    lateinit var changable: FrameLayout
//    lateinit var frag_skin: TextView
//    lateinit var frag_lip: TextView
//    lateinit var frag_hair: TextView

    private var isExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // binding 방식으로 바꾸는 법 알아보기
        category_skin = findViewById<TextView>(R.id.category_skin)
        category_lip = findViewById<TextView>(R.id.category_lip)
        category_hair = findViewById<TextView>(R.id.category_hair)
        changable = findViewById<FrameLayout>(R.id.changable)

        category_skin.setOnClickListener {

        }

        //    더보기
        val textViewContent = findViewById<TextView>(R.id.textViewContent)
        val textExpand = findViewById<TextView>(R.id.textExpand)
        textExpand.setOnClickListener {
            if (isExpanded) {
                textViewContent.maxLines = 1
                textExpand.text = "더보기 +"
            } else {
                textViewContent.maxLines = Int.MAX_VALUE
                textExpand.text = "접기 -"
            }
            isExpanded = !isExpanded
        }
    }


}