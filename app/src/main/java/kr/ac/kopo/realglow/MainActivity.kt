package kr.ac.kopo.realglow

import android.app.Activity
import android.app.AlertDialog
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
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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
    // 각 선택된 정보를 저장할 리스트
    val selectedInfoList: MutableList<String> = mutableListOf()

    data class MakeupColorInfo(
        var hairColor: List<Int>? = null,
        var lipColor: List<Int>? = null,
        var skinColor: List<Int>? = null
    )

    // 마지막에 선택된 색상 정보를 저장할 DTO
    var makeupColorInfo: MakeupColorInfo = MakeupColorInfo()

    // Getter와 Setter를 통해 색상 정보 업데이트
    fun setHairColor(color: List<Int>) {
        makeupColorInfo.hairColor = color
    }

    fun setLipColor(color: List<Int>) {
        makeupColorInfo.lipColor = color
    }

    fun setSkinColor(color: List<Int>) {
        makeupColorInfo.skinColor = color
    }

    // 선택된 정보를 업데이트하는 메서드 (기존 항목을 삭제하고 리스트 최상단에 추가)
    // 선택된 정보를 업데이트하는 메서드 (같은 카테고리의 기존 항목을 지우고 새 항목을 추가)
    fun updateSelectedInfo(newInfo: String) {
        // 카테고리 이름 추출 (카테고리명은 아이템 정보 문자열의 처음에 있다고 가정)
        val category = newInfo.split(" - ")[0]

        // 같은 카테고리의 기존 항목이 있으면 제거
        selectedInfoList.removeIf { it.startsWith(category) }

        // 리스트 최상단에 새 항목 추가
        selectedInfoList.add(0, newInfo)
    }

    val CAMERA_CODE = 98

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

    lateinit var retrofit: Retrofit

    private var hairFragment: HairFragment? = null
    private var lipFragment: LipFragment? = null
    private var skinFragment: SkinFragment? = null


    companion object {
        const val GALLERY = 1
        const val REQUEST_IMAGE_CAPTURE = 2
    }

    private var originalBase64Image: String? = null  // 원본 이미지를 저장할 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Fragment 초기화 및 저장
        hairFragment = HairFragment()
        lipFragment = LipFragment()
        skinFragment = SkinFragment()

        // LogoFragment를 초기 상태로 추가
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentLayout, LogoFragment())
                .commit()
        }

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

        // '전체삭제' 버튼 클릭 리스너
        val btnClear = findViewById<TextView>(R.id.btnClear)
        btnClear.setOnClickListener {
            resetToOriginalImage()  // 전체 삭제 동작
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
                intent.type = "image/*"
                startActivityForResult(intent, GALLERY)
            } else {
                requestPermission2()
            }
        }

        // 이미지 저장
        btnSave.setOnClickListener {
            val drawable = imgV.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val uri = saveFile(RandomFileName(), "image/png", bitmap)
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

    // 원본 이미지를 프래그먼트에서 가져갈 수 있게 하는 getter
    fun getOriginalBase64Image(): String? {
        return originalBase64Image
    }

    // 원본 이미지를 저장하는 함수 (갤러리/카메라에서 이미지 선택 시 호출)
    private fun saveOriginalImage(bitmap: Bitmap) {
        originalBase64Image = convertBitmapToBase64(bitmap)
    }

    // '전체삭제' 클릭 시 호출
    private fun resetToOriginalImage() {
        // 원본 이미지가 있을 경우 ImageView에 적용
        if (originalBase64Image != null) {
            decodeBase64ToImageView(originalBase64Image!!, findViewById(R.id.imgV))
            Toast.makeText(this, "이미지가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "초기 이미지가 없습니다.", Toast.LENGTH_SHORT).show()
        }

        // 선택된 정보 리스트 초기화
        selectedInfoList.clear()

        // 더보기란의 상품 정보 초기화
        clearTextViewContent()

        // 색상 정보 초기화
        makeupColorInfo = MakeupColorInfo()

        Toast.makeText(this, "전체 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // Bitmap을 Base64로 변환
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Base64 문자열을 ImageView에 디코딩하여 설정
    private fun decodeBase64ToImageView(base64String: String, imageView: ImageView) {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        imageView.setImageBitmap(decodedBitmap)
    }

    override fun onClick(v: View) {
        // 이미지뷰에 이미지가 없는 경우 토스트 메시지 출력
        if (imgV.drawable == null) {
            Toast.makeText(this, "원본 이미지가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 모든 카테고리의 글자 색상을 기본 색상으로 초기화
        resetCategoryColor()

        // 클릭된 카테고리의 글자 색상을 빨간색으로 변경
        when (v.id) {
            R.id.category_skin -> {
                category_skin.setTextColor(ContextCompat.getColor(this, R.color.red))
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentLayout, SkinFragment())
                    .commit()
            }

            R.id.category_lip -> {
                category_lip.setTextColor(ContextCompat.getColor(this, R.color.red))
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentLayout, LipFragment())
                    .commit()
            }

            R.id.category_hair -> {
                category_hair.setTextColor(ContextCompat.getColor(this, R.color.red))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            // 기존 이미지, 색상 정보, 더보기란, 그리고 뷰 초기화
            imgV.setImageDrawable(null)
            resetMakeupColors()
            clearTextViewContent()
            resetToLogoState()

            if (requestCode == GALLERY) {
                val imageData: Uri? = data?.data
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageData)
                    imgV.setImageBitmap(bitmap)
                    saveOriginalImage(bitmap) // 갤러리 이미지 저장
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (photoURI != null) {
                    val bitmap = loadBitmapFromMediaStoreBy(photoURI!!)
                    if (bitmap != null) {
                        imgV.setImageBitmap(bitmap)
                        saveOriginalImage(bitmap) // 고해상도 이미지 저장
                    } else {
                        Log.e("MainActivity", "Failed to load bitmap from MediaStore.")
                        Toast.makeText(this, "이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                    photoURI = null
                } else {
                    val imageBitmap: Bitmap? = data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        imgV.setImageBitmap(imageBitmap)
                        saveOriginalImage(imageBitmap) // 썸네일 이미지 저장
                    } else {
                        Log.e("MainActivity", "Failed to retrieve image data.")
                        Toast.makeText(this, "이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (requestCode == REQUEST_CREATE_EX) {
                if (photoURI != null) {
                    val bitmap = loadBitmapFromMediaStoreBy(photoURI!!)
                    if (bitmap != null) {
                        imgV.setImageBitmap(bitmap)
                        saveOriginalImage(bitmap) // 고해상도 이미지 저장
                    } else {
                        Log.e("MainActivity", "Failed to load bitmap from MediaStore.")
                        Toast.makeText(this, "이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                    photoURI = null
                } else {
                    Log.e("MainActivity", "photoURI is null, cannot load image.")
                    Toast.makeText(this, "이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 색상 정보 초기화 메서드
    private fun resetMakeupColors() {
        makeupColorInfo.hairColor = null
        makeupColorInfo.lipColor = null
        makeupColorInfo.skinColor = null
    }

    // 더보기란의 textViewContent 초기화 메서드
    private fun clearTextViewContent() {
        val textViewContent = findViewById<TextView>(R.id.textViewContent)
        textViewContent.text = ""
    }

    // 뷰를 로고 상태로 초기화하는 메서드
    private fun resetToLogoState() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentLayout, LogoFragment())
            .commit()
        resetCategoryColor()
    }

    private fun resetCategoryColor() {
        // 모든 카테고리의 글자 색상을 기본 색상으로 초기화
        category_skin.setTextColor(ContextCompat.getColor(this, R.color.black))
        category_lip.setTextColor(ContextCompat.getColor(this, R.color.black))
        category_hair.setTextColor(ContextCompat.getColor(this, R.color.black))
    }


    // 카메라 원본 이미지를 저장하기 위한 이미지 경로 Uri 생성
    fun createImageUri(filename: String, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        }
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
        return try {
            if (Build.VERSION.SDK_INT > 27) {
                val source: ImageDecoder.Source = ImageDecoder.createSource(this.contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // 사진 저장
    fun saveFile(fileName: String, mimeType: String, bitmap: Bitmap): Uri? {
        val CV = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, CV)
        if (uri != null) {
            contentResolver.openFileDescriptor(uri, "w")?.use { scriptor ->
                FileOutputStream(scriptor.fileDescriptor).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                CV.clear()
                CV.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, CV, null, null)
            }
        }
        return uri
    }

    // 파일명을 날짜로 해서 저장
    fun RandomFileName(): String {
        return SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis())
    }
}
