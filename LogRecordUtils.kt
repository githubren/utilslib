package com.example.test2

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import androidx.annotation.StringDef
import java.util.concurrent.Executors


/**
 * author:renbing
 * date:2021/2/23
 * des:日志记录工具，在需要记录的地方调用此类中的方法record()，分I、E、W三个级别
 */
class LogRecordUtils private constructor(){

    //进程id
    private var mPid = 0
    //日志存放根目录
    private var PATH_LOGCAT: String? = null
    //线程池
    private val mThreadPool by lazy {
        Executors.newCachedThreadPool { r ->
            val thread = Thread(r)
            thread.setUncaughtExceptionHandler { t, e ->
                println(t.name)
                e.printStackTrace()
            }
            return@newCachedThreadPool thread
        }
    }

    /**
     * 日志等级
     */
    @Retention(AnnotationRetention.RUNTIME)
    @StringDef(LogLevelType.I, LogLevelType.E, LogLevelType.W)
    annotation class LogLevelType {
        companion object {
            const val I = "I"
            const val E = "E"
            const val W = "W"
        }
    }

    companion object{
        private var instance: LogRecordUtils? = null
            get() {
                if (field == null){
                    field = LogRecordUtils()
                }
                return field
            }
        private var context:Context? = null
        //删除几天前的日志 每次调用get获取实例的时候传入 默认两天
        private var day: Int = 0

        @Synchronized
        fun get(cxt: Context,day: Int = 2):LogRecordUtils{
            context = cxt
            this.day = day
            return instance!!
        }
    }

    init {
        mPid = android.os.Process.myPid()
        val logRootPath = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED){
            Environment.getExternalStorageDirectory().absolutePath+File.separator+"Rivamed_logs_record_manual"
        }else{
            context?.filesDir?.absolutePath+File.separator+"Rivamed_logs_record_manual"
        }
        val logRootFile = File(logRootPath)
        if (!logRootFile.exists()){
            logRootFile.mkdirs()
        }
        val logParentFile = File(logRootPath,getPackageName())
        if (!logParentFile.exists()){
            logParentFile.mkdirs()
        }
        PATH_LOGCAT = logParentFile.absolutePath
    }

    private fun newThread(r: ()->Unit){
        mThreadPool.execute(r)
    }

    /**
     * 记录日志
     * @param message 需要存入的信息
     * @param logLevel 日志等级 LogLevelType.I, LogLevelType.E, LogLevelType.W
     * @param tag 日志标签 可不传 默认值为包名
     */
    fun record(tag:String = getPackageName(), message:String, @LogLevelType logLevel:String) {
        //删除两天前的日志
        deletePreviousLog(day)
        newThread {
            val logMessage = "${getCurrentTime()} $mPid/${getPackageName()} $logLevel/$tag: $message\n\n"
            val logFile = File(PATH_LOGCAT?:getPackageName(),"Setp-${getCurrentDate()}.log")
            if (!logFile.exists()){
                logFile.createNewFile()
            }
            var fos : FileOutputStream? = null
            try {
                fos = FileOutputStream(logFile,true)
                fos.write(logMessage.toByteArray())
            }catch (e:Exception){
                e.printStackTrace()
            }finally {
                fos?.close()
            }
        }
    }

    /**
     * 删除前几天的日志 天数可自行设置
     *
     * @param day 删除第${day}天前的日志
     */
    fun deletePreviousLog(day: Int) {
        val logFile = File(PATH_LOGCAT ?: "")
        if (!logFile.exists()) return
        //删除两天前的日志记录
        if (!logFile.isDirectory) return
        val files = logFile.listFiles() ?: arrayOf()
        if (files.isNotEmpty()) {
            val pattern = "\\d{4}-\\d{2}-\\d{2}"
            files.forEach {
                Regex(pattern).findAll(it.name).toList().flatMap(MatchResult::groupValues).forEach { date ->
                    val todayC = Calendar.getInstance()
                    todayC.time = Date()
                    todayC.set(Calendar.HOUR, 0)
                    todayC.set(Calendar.SECOND, 0)
                    todayC.set(Calendar.MILLISECOND, 0)
                    val today = todayC.timeInMillis
                    val logC = Calendar.getInstance()
                    logC.time = SimpleDateFormat("yyyy-MM-dd").parse(date)
                    val logDate = logC.timeInMillis
                    if ((today - logDate) / (24 * 60 * 60 * 1000) > day) {
                        it.delete()
                    }
                }
            }
        }
    }

    /**
     * 获取包名
     * @return 包名
     */
    private fun getPackageName() : String{
        val am = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = am.runningAppProcesses
        processes.forEach {
            return if (it.pid == mPid) {
                it.processName
            } else {
                return@forEach
            }
        }
        return ""
    }

    /**
     * 获取当前具体时间 格式：yyyy-MM-dd HH:mm:ss.SSS
     * @return 具体时间
     */
    private fun getCurrentTime() : String{
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        val curDate = Date(System.currentTimeMillis())// 获取当前时间
        return formatter.format(curDate)
    }

    /**
     * 获取当前日期 格式: yyyy-MM-dd
     * @return 日期
     */
    private fun getCurrentDate() : String{
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        val curDate = Date(System.currentTimeMillis())// 获取当前时间
        return formatter.format(curDate)
    }
}