package com.example.coroutine

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //从相册读取图片  读取视频
        //从相机拍摄图片 拍摄视频
        //将拍摄的图片写入相册
        //视频写入相册
        //相册读取或者视频拍摄的图片写入文件
        //相册读取或者视频拍摄的视频写入文件
        //同时读取多张图片或者多个视频

        //注册行为
        val takePictureLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
            //imageView.setImageURI(it)
            videoView.setVideoURI(it)
            videoView.start()

            //将图片保存到文件中
            //saveImageToFile(it)
            //saveVideoToFile(it)
        }

        //从相机拍摄视频 写入相册 或者写入文件
        //implementation 'androidx.activity:activity-ktx:1.5.1'
        val takeVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()){
            if (it){
                val filePath = "${filesDir.path}/1.mov"
                val uri = Uri.parse(filePath)
                videoView.setVideoURI(uri)
                videoView.start()

                saveVideoToGallary(filePath)
            }
        }

        val takeMultyMediaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result:ActivityResult ->
            //判断是否获取成功
            if(result.resultCode == Activity.RESULT_OK){
                result.data?.let {
                    val clipData = result.data!!.clipData

                    for (i in 0 until (clipData!!.itemCount)){
                        val item = clipData.getItemAt(i)

                        Log.v("pxd","${item.uri.path}")
                    }
                }
            }
        }

        picButton.setOnClickListener {
            //takePictureLauncher.launch("video/*")

            //val file = File(filesDir,"1.mov")
            //val uri = FileProvider.getUriForFile(this,"com.example.coroutine.fileProvider",file)
            //takeVideoLauncher.launch(uri)

            val intent = Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                type = "image/*"  //video/*
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
            }
            takeMultyMediaLauncher.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun saveVideoToGallary(filePath: String){
        val video_directory_uri = if (Build.VERSION.SDK_INT >= 29){
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }else{
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME,getVideoNameFromTime())
            put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
        }

        val video_uri = contentResolver.insert(video_directory_uri, contentValues)
        video_uri?.let {
            val os = contentResolver.openOutputStream(video_uri)
            BufferedOutputStream(os).use { bos ->
                BufferedInputStream(FileInputStream(filePath)).use { bis ->
                    val buffer = ByteArray(1024)
                    var len = bis.read(buffer,0,1024)
                    while (len != -1){
                        bos.write(buffer,0,len)
                        len = bis.read(buffer,0,1024)
                    }
                    bos.flush()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getVideoNameFromTime():String{
        val time = SimpleDateFormat("yyyyMMdd-hhmmss", Locale.CHINESE).format(Date())
        return "$time.mov"
    }

    /***
     * 将系统(相册 相机 外部)提供的uri图片解析为对应的bitmap
     */
    fun getBitmapFromContentUri(uri:Uri):Bitmap{
        var bitmap:Bitmap
        if (Build.VERSION.SDK_INT < 28){
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver,uri)
        }else{
            //获取资源类型
            val source = ImageDecoder.createSource(contentResolver,uri)
            bitmap = ImageDecoder.decodeBitmap(source)
        }
        return bitmap
    }

    /**获取uri的最后一部分(文件的名字)*/
    fun getUriFileName(uri: Uri,type:String):String{
        uri.path?.let {
            val nameIndex = uri.path!!.lastIndexOf("/")+1
            val name = uri.path!!.substring(nameIndex)
            return "$name.$type"
        }
        return ""
    }

    fun saveImageToFile(uri: Uri){
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = getBitmapFromContentUri(uri)
            val file = File(filesDir,getUriFileName(uri,"jpg"))
            if (!file.exists()){
                file.createNewFile()
            }
            FileOutputStream(file).use { fos ->
                val result = bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos)
                withContext(Dispatchers.Main){
                    if (result){
                        Toast.makeText(this@MainActivity,"save ok", Toast.LENGTH_LONG).show()
                    }else{
                        Toast.makeText(this@MainActivity,"save error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun saveVideoToFile(uri: Uri){
        val directoryUri = if (Build.VERSION.SDK_INT >= 29){
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }else{
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val conentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME,"myname.mp4")
            put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
        }

        val medisUri = contentResolver.insert(directoryUri,conentValues)
        FileInputStream(medisUri?.path).use {  fis ->
            val file = File(filesDir,getUriFileName(medisUri!!,"mp4"))
            if (!file.exists()){
                file.createNewFile()
            }
            FileOutputStream(file).use {  fos ->
                val buffer = ByteArray(1024)
                var len = fis.read(buffer,0,1024)
                while (len != -1){
                    fos.write(buffer,0,len)
                    len = fis.read(buffer,0,1024)
                }
                fos.flush()
            }
        }

    }
}







