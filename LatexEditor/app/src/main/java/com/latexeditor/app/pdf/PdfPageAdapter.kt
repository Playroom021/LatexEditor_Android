package com.latexeditor.app.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * Renders each page of the compiled PDF into a Bitmap using the platform
 * PdfRenderer (API 21+, no external native PDF library needed) and shows
 * them in a vertically-scrolling RecyclerView, which gives us "continuous
 * scroll" preview behavior similar to desktop LaTeX editors.
 */
class PdfPageAdapter(private var pdfFile: File?) : RecyclerView.Adapter<PdfPageAdapter.PageVH>() {

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pageCount = 0
    var renderWidthPx = 1080

    init {
        openPdf()
    }

    private fun openPdf() {
        close()
        val file = pdfFile ?: return
        if (!file.exists()) return
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(fileDescriptor!!)
        pageCount = renderer?.pageCount ?: 0
    }

    fun swapPdf(newFile: File) {
        pdfFile = newFile
        openPdf()
        notifyDataSetChanged()
    }

    fun close() {
        renderer?.close()
        fileDescriptor?.close()
        renderer = null
        fileDescriptor = null
        pageCount = 0
    }

    class PageVH(val imageView: ZoomableImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val iv = ZoomableImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            adjustViewBounds = true
        }
        return PageVH(iv)
    }

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        val r = renderer ?: return
        synchronized(r) {
            val page = r.openPage(position)
            val ratio = page.height.toFloat() / page.width.toFloat()
            val w = renderWidthPx
            val h = (w * ratio).toInt()
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            holder.imageView.setImageBitmap(bitmap)
            page.close()
        }
    }

    override fun getItemCount() = pageCount
}
