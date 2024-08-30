package kr.ac.kopo.realglow

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity(), View.OnClickListener {
    val CAMERA_CODE = 98
    val STORAGE_CODE = 99

    lateinit var textViewContent: TextView
    lateinit var btnCapture: ImageButton
    lateinit var btnGallery: ImageButton
    lateinit var btnSave: ImageButton
    lateinit var imgV: ImageView

    lateinit var category_skin: TextView
    lateinit var category_lip: TextView
    lateinit var category_hair: TextView
    lateinit var fragmentLayout: FrameLayout

    private var isExpanded = false

    private lateinit var retrofit: Retrofit
    private lateinit var api: RetrofitAPI

    companion object {
        const val GALLERY = 1
        const val REQUEST_IMAGE_CAPTURE = 2
    }

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

        // Retrofit 초기화
        retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.baseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(RetrofitAPI::class.java)

        val anotherLayout = LayoutInflater.from(this).inflate(R.layout.fragment_skin, null)

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

        // 사진 촬영
        btnCapture.setOnClickListener {
            if (checkPermission1()) {
                dispatchTakePictureIntentEx()
            } else {
                requestPermission1()
            }
        }

        // 갤러리 호출
        btnGallery.setOnClickListener {
            if (checkPermission2()) {
                val intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.setType("image/*")
                startActivityForResult(intent, GALLERY)
            } else {
                requestPermission2()
            }
        }

        // 이미지 저장 및 서버로 전송
        btnSave.setOnClickListener {
            val drawable = imgV.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val uri = saveFile(RandomFileName(), "image/png", bitmap)
                if (uri != null) {
                    Toast.makeText(this, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()

                    // 이미지 바이트코드로 변환하고 Base64로 인코딩
                    val base64String = convertImageViewToBase64()
                    if (base64String != null) {
                        // Base64 문자열을 사용하여 서버에 전송
                        sendImageToServer(base64String)
                    } else {
                        Toast.makeText(this, "이미지를 변환할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
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

    // 카메라 촬영 권한 요청
    private fun requestPermission1() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA), 1)
    }

    // 갤러리 열기 권한 요청
    private fun requestPermission2() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
    }

    // 카메라 촬영 권한 여부 확인
    private fun checkPermission1(): Boolean {
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    // 갤러리 열기 권한 여부 확인
    private fun checkPermission2(): Boolean {
        return (ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    // 카메라 권한, 저장소 권한 요청 처리
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_CODE -> {
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "카메라 권한을 승인해 주세요", Toast.LENGTH_LONG).show()
                    }
                }
            }
            GALLERY -> {
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "저장소 권한을 승인해 주세요", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // 촬영한 사진, 갤러리에서 선택한 이미지를 이미지뷰에 연결
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                var ImageData: Uri? = data?.data
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, ImageData)
                    imgV.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                val imageBitmap: Bitmap? = data?.extras?.get("data") as Bitmap
                imgV.setImageBitmap(imageBitmap)
            } else if (requestCode == REQUEST_CREATE_EX) {
                if (photoURI != null) {
                    val bitmap = loadBitmapFromMediaStoreBy(photoURI!!)
                    imgV.setImageBitmap(bitmap)
                    photoURI = null
                }
            }
        }
    }

    // 카메라 원본 이미지를 저장하기 위한 이미지 경로 Uri 생성
    fun createImageUri(filename: String, mimeType: String): Uri? {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    // 카메라를 동작하기 위한 함수
    private var photoURI: Uri? = null
    private val REQUEST_CREATE_EX = 3
    private fun dispatchTakePictureIntentEx() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val takePictureIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val uri: Uri? = createImageUri("PNG_${timeStamp}_", "image/png")
        photoURI = uri
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(takePictureIntent, REQUEST_CREATE_EX)
    }

    // 생성된 Uri 경로에 이미지를 MediaStore를 사용해서 읽어옴.
    fun loadBitmapFromMediaStoreBy(photoUri: Uri): Bitmap? {
        var image: Bitmap? = null
        try {
            image = if (Build.VERSION.SDK_INT > 27) {
                val source: ImageDecoder.Source =
                    ImageDecoder.createSource(this.contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }

    // 사진 저장
    fun saveFile(fileName: String, mimeType: String, bitmap: Bitmap): Uri? {
        var CV = ContentValues()

        // MediaStore 에 파일명, mimeType 을 지정
        CV.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        CV.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        // 안정성 검사
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CV.put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        // MediaStore 에 파일을 저장
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, CV)
        if (uri != null) {
            var scriptor = contentResolver.openFileDescriptor(uri, "w")

            val fos = FileOutputStream(scriptor?.fileDescriptor)

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                CV.clear()
                // IS_PENDING 을 초기화
                CV.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, CV, null, null)
            }
        }
        return uri
    }

    // 파일명을 날짜로 해서 저장
    fun RandomFileName(): String {
        val fileName = SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis())
        return fileName
    }

    // 이미지를 Base64 인코딩
    fun convertImageViewToBase64(): String? {
        val drawable = imgV.drawable as? BitmapDrawable
        val bitmap = drawable?.bitmap

        if (bitmap != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
        return null
    }

    // Base64 문자열을 디코딩하여 ImageView에 표시하는 함수
    fun decodeBase64ToImageView(base64String: String) {
        if (base64String.isNotEmpty()) {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            imgV.setImageBitmap(decodedBitmap)
            Log.d("Retrofit", "Image successfully decoded and set to ImageView")
        } else {
            Log.e("Retrofit", "Received an empty base64 string")
        }
    }

    // 이미지를 서버로 전송하는 함수
    fun sendImageToServer(base64Image: String) {
        val jsonObject = JsonObject().apply {
            addProperty("img", base64Image)
        }

        api.postImage(jsonObject).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val parsedImage = responseBody.get("PNGImage").asString
                        Log.d("Retrofit", "Parsed Image: $parsedImage")
                        applyMakeup(parsedImage)
                    }
                } else {
                    Log.e("Retrofit", "Error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.e("Retrofit", "Failed to send image: ${t.message}")
            }
        })
    }

    // 메이크업 색상을 적용하고 이미지를 서버로 전송하는 함수
    fun applyMakeup(parsedImage: String) {
        val base64Image = convertImageViewToBase64()
        if (base64Image != null) {
            val color = parseColor("#FF5733") // 예시 색상, 실제 색상 값으로 변경 가능
            val jsonObject = JsonObject().apply {
                addProperty("img", base64Image)
                addProperty("parsing", parsedImage)
                add("skin_color", JsonArray().apply {
                    add(JsonArray().apply {
                        add(color[0])
                        add(color[1])
                        add(color[2])
                    })
                    add(1) // alpha 값을 추가
                })
                add("hair_color", JsonArray().apply {
                    add(JsonArray().apply {
                        add(color[0])
                        add(color[1])
                        add(color[2])
                    })
                    add(1) // alpha 값을 추가
                })
                add("lip_color", JsonArray().apply {
                    add(JsonArray().apply {
                        add(color[0])
                        add(color[1])
                        add(color[2])
                    })
                    add(1) // alpha 값을 추가
                })
            }


            Log.d("Retrofit", "Makeup Request: $jsonObject")

            api.postMakeup(jsonObject).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            val changeFaceImg = responseBody.get("changeImg").asString
                            Log.d("Retrofit", "Makeup Applied Image: $changeFaceImg")
                            decodeBase64ToImageView(changeFaceImg)
                        }
                    } else {
                        Log.e("Retrofit", "Error: ${response.code()} - ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.e("Retrofit", "Failed to apply makeup: ${t.message}")
                }
            })
        } else {
            Toast.makeText(this, "이미지를 변환할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Hex 색상을 RGB로 변환하는 함수
    fun parseColor(colorHex: String): List<Int> {
        val color = android.graphics.Color.parseColor(colorHex)
        Log.d("Retrofit", "Parsed Color: R=${android.graphics.Color.red(color)}, G=${android.graphics.Color.green(color)}, B=${android.graphics.Color.blue(color)}")
        return listOf(
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color),
            1
        )
    }
}
