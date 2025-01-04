package com.devoid.menumate.prsentation.ui.recycler

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.devoid.menumate.R

class RecyclerDividerDecoration(private val context: Context) :ItemDecoration() {
    private val mDivider = ContextCompat.getDrawable(context, R.drawable.divider)
    private val DIVIDER_HEIGHT = 2
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        for (i in 0 until  parent.childCount-1){
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + (mDivider?.intrinsicHeight ?: 0)
            mDivider?.setBounds(left,top, right, bottom)
            mDivider?.draw(c)
        }
    }
}