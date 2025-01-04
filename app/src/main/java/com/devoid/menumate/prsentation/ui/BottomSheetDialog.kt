package com.devoid.menumate.prsentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.devoid.menumate.databinding.BottomSheetDialogBinding
import com.devoid.menumate.prsentation.ui.BottomSheetDialog.OnShowListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetDialog : BottomSheetDialogFragment() {
    lateinit var binding: BottomSheetDialogBinding
    var listener: OnShowListener = OnShowListener {}
    var title=""
    var icon : Int? =null
    var primaryBtnAnimation:Int?=null
    var animationViewScale: Float? = null
    var secondaryText = ""


    companion object {
        const val TAG = "ModalBottomSheetDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetDialogBinding.inflate(inflater, container, false)
        binding.secondaryBtn.setOnClickListener { dismiss() }
        listener.onShow()
        binding.apply {
            title.text = this@BottomSheetDialog.title
            this@BottomSheetDialog.icon?.let {
                icon.setImageResource(it)
            }
            this@BottomSheetDialog.animationViewScale?.let {
                lottieView.scaleX = it
                lottieView.scaleY = it
            }
            this@BottomSheetDialog.primaryBtnAnimation?.let {
                lottieView.setAnimation(it)
                lottieView.repeatCount= 9999
                lottieView.playAnimation()
            }
            secondaryText.text = this@BottomSheetDialog.secondaryText
        }
        return binding.root
    }

    fun interface OnShowListener {
        fun onShow()
    }
   fun primaryBtn(text:String,onClick:(View)-> Unit){
       binding.primaryBtnText.text = text
       binding.primaryBtn.setOnClickListener(onClick)
   }

    fun secondaryBtn(text:String,onClick:(View)-> Unit){
        binding.secondaryBtn.text = text
        binding.secondaryBtn.setOnClickListener(onClick)
    }
    fun dismissOnPrimaryBtnClick(onClick: (View) -> Unit){
        binding.primaryBtn.setOnClickListener {
            dialog?.dismiss()
            onClick(it)
        }
    }
    fun updateAnimation(animation:Int, scale:Float,repeatCount:Int){
        binding.lottieView.apply {
            scaleX = scale
            scaleY = scale
            setAnimation(animation)
            playAnimation()
            this.repeatCount = repeatCount
        }
    }

}