package com.freegang.douyin.logic

import android.app.ProgressDialog
import android.content.Context
import com.freegang.base.BaseHook
import com.freegang.xpler.utils.net.KHttpUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class BatchDownloadManager(private val context: Context) {
    private val taskQueue = LinkedList<DownloadTask>()
    private var isRunning = false
    private var progressDialog: ProgressDialog? = null
    private var currentProgress = 0
    private var totalTasks = 0

    data class DownloadTask(
        val url: String,
        val file: File,
        val taskType: String // "video", "image", "music"
    )

    fun addTask(url: String, file: File, taskType: String) {
        taskQueue.add(DownloadTask(url, file, taskType))
    }

    fun start() {
        if (isRunning || taskQueue.isEmpty()) return

        isRunning = true
        totalTasks = taskQueue.size
        currentProgress = 0

        showProgressDialog()
        processNextTask()
    }

    private fun processNextTask() {
        if (taskQueue.isEmpty()) {
            finishDownload()
            return
        }

        val task = taskQueue.poll()
        downloadTask(task)
    }

    private fun downloadTask(task: DownloadTask) {
        BaseHook().launch {
            withContext(Dispatchers.IO) {
                val outputStream = FileOutputStream(task.file)
                KHttpUtils.download(task.url, outputStream) { real, total ->
                    val progress = (real * 100 / total).toInt()
                    updateProgress(progress)
                }
                
                currentProgress++
                updateProgress(100)
                processNextTask()
            }
        }
    }

    private fun showProgressDialog() {
        progressDialog = ProgressDialog(context).apply {
            setTitle("批量下载")
            setMessage("正在下载...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setMax(100)
            setCancelable(false)
            show()
        }
    }

    private fun updateProgress(progress: Int) {
        progressDialog?.let {
            val overallProgress = (currentProgress * 100 + progress) / totalTasks
            it.progress = overallProgress
            it.setMessage("正在下载 ${currentProgress + 1}/${totalTasks}")
        }
    }

    private fun finishDownload() {
        isRunning = false
        progressDialog?.dismiss()
        progressDialog = null
    }

    fun pause() {
        isRunning = false
    }

    fun cancel() {
        isRunning = false
        taskQueue.clear()
        progressDialog?.dismiss()
        progressDialog = null
    }

    fun getProgress(): Int {
        if (totalTasks == 0) return 0
        return (currentProgress * 100) / totalTasks
    }

    fun isRunning(): Boolean {
        return isRunning
    }
}
