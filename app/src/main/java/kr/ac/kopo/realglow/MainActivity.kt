package kr.ac.kopo.realglow

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Camera
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import kotlin.random.Random


class MainActivity : AppCompatActivity(), View.OnClickListener {
    val CAMERA = arrayOf(android.Manifest.permission.CAMERA)
    val STORAGE = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    val CAMERA_CODE = 98
    val STORAGE_CODE = 99

    lateinit var btnCapture: ImageButton
    lateinit var btnGallery: ImageButton
    lateinit var btnSave: ImageButton
    lateinit var imgV: ImageView

    lateinit var category_skin: TextView
    lateinit var category_lip: TextView
    lateinit var category_hair: TextView
    lateinit var fragmentLayout: FrameLayout

    private var isExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCapture = findViewById<ImageButton>(R.id.btnCapture)
        btnGallery = findViewById<ImageButton>(R.id.btnGallery)
        btnSave = findViewById<ImageButton>(R.id.btnSave)
        imgV = findViewById<ImageView>(R.id.imgV)

        category_skin = findViewById<TextView>(R.id.category_skin)
        category_lip = findViewById<TextView>(R.id.category_lip)
        category_hair = findViewById<TextView>(R.id.category_hair)
        fragmentLayout = findViewById<FrameLayout>(R.id.fragmentLayout)

        category_skin.setOnClickListener(this)
        category_lip.setOnClickListener(this)
        category_hair.setOnClickListener(this)


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

        // 카메라
        btnCapture.setOnClickListener {
            CallCamera()
        }

        // 갤러리 호출
        btnGallery.setOnClickListener {
            GetAlbum()
        }

        // 이미지 저장
        btnSave.setOnClickListener {
            val drawable = imgV.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val uri = saveFile(RandomFileName(), "image/jpeg", bitmap)
                if (uri != null) {
                    Toast.makeText(this, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "이미지가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.category_skin -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentLayout, SkinFragment())
                    .commit()
            }

            R.id.category_lip -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentLayout, LipFragment())
                    .commit()
            }

            R.id.category_hair -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentLayout, HairFragment())
                    .commit()
            }
        }
    }

    // 카메라 권한, 저장소 권한
    // 요청 권한
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CAMERA_CODE -> {
                for (grant in grantResults){
                    if(grant != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "카메라 권한을 승인해 주세요", Toast.LENGTH_LONG).show()
                    } else {
                        // 카메라 권한이 허용된 경우 저장소 권한 허용 요청
                        checkPermission(STORAGE, STORAGE_CODE)
                    }
                }
            }
            STORAGE_CODE -> {
                for(grant in grantResults){
                    if(grant != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "저장소 권한을 승인해 주세요", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // 다른 권한등도 확인이 가능하도록
    fun checkPermission(permissions: Array<out String>, type:Int):Boolean{
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            for (permission in permissions){
                if(ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this, permissions, type)
                    return false
                }
            }
        }
        return true
    }

    // 카메라 촬영 - 권한 처리
    fun CallCamera(){
        if(checkPermission(CAMERA, CAMERA_CODE) && checkPermission(STORAGE, STORAGE_CODE)){
            val itt = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(itt, CAMERA_CODE)
        }
    }

    // 사진 저장
    fun saveFile(fileName:String, mimeType:String, bitmap: Bitmap):Uri?{
        var CV = ContentValues()

        // MediaStore 에 파일명, mimeType 을 지정
        CV.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        CV.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        // 안정성 검사
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            CV.put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        // MediaStore 에 파일을 저장
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, CV)
        if(uri != null){
            var scriptor = contentResolver.openFileDescriptor(uri, "w")

            val fos = FileOutputStream(scriptor?.fileDescriptor)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                CV.clear()
                // IS_PENDING 을 초기화
                CV.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, CV, null, null)
            }
        }
        return uri
    }

    // 결과
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                CAMERA_CODE -> {
                    if(data?.extras?.get("data") != null){
                        val img = data?.extras?.get("data") as Bitmap
                        imgV.setImageBitmap(img)
                    }
                }
                STORAGE_CODE -> {
                    val uri = data?.data
                    imgV.setImageURI(uri)
                }
            }
        }
    }

    // 파일명을 날짜로 해서 저장
    fun RandomFileName() : String{
        val fileName = SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis())
        return fileName
    }

    // 갤러리 취득
    fun GetAlbum(){
        if(checkPermission(STORAGE, STORAGE_CODE)){
            val itt = Intent(Intent.ACTION_PICK)
            itt.type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(itt, STORAGE_CODE)
        }
    }
}