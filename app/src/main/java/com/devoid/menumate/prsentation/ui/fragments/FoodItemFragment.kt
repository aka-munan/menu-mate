package com.devoid.menumate.prsentation.ui.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.devoid.menumate.R
import com.devoid.menumate.databinding.FoodItemFragmentBinding
import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.Order
import com.devoid.menumate.domain.model.toLikedItemInstance
import com.devoid.menumate.prsentation.ui.MainActivity
import com.devoid.menumate.prsentation.viewmodel.CheckOutFragmentSharedViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FoodItemFragment(private val itemInstance: MenuItem) : Fragment() {
    private lateinit var binding: FoodItemFragmentBinding
    private val viewModel: CheckOutFragmentSharedViewModel by viewModels()
    private var itemCount = 1
    private var imageTransitionName: String? = null
    private var rootTransitionName: String? = null
    private var titleTransitionName: String? = null
    private var priceTransitionName: String? = null
    private lateinit var rootView: View
    private val TAG = FoodItemFragment::class.java.simpleName

    constructor(
        itemInstance: MenuItem,
        rootTransitionName:String,
        imageTransitionName: String,
        titleTransitionName: String,
        priceTransitionName: String
    ) : this(itemInstance) {
        this.rootTransitionName = rootTransitionName
        this.imageTransitionName = imageTransitionName
        this.titleTransitionName = titleTransitionName
        this.priceTransitionName = priceTransitionName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FoodItemFragmentBinding.inflate(layoutInflater)
        if (imageTransitionName != null) {
            postponeEnterTransition()
            ViewCompat.setTransitionName(binding.image, imageTransitionName)
            ViewCompat.setTransitionName(binding.root, rootTransitionName)
            ViewCompat.setTransitionName(binding.title, titleTransitionName)
            ViewCompat.setTransitionName(binding.price, priceTransitionName)
        }
        init()
        setUpButtons()
        setUpToolbar()
        return binding.root
    }


    private fun init() {
        rootView = requireActivity().window.decorView.findViewById(android.R.id.content)

        binding.apply {
            title.text = itemInstance.itemName
            price.text = "${itemInstance.price} ₹"
            rating.text = itemInstance.rating.toString()
            description.text = itemInstance.description
            totalPrice.text = "${itemInstance.price} ₹"
            Glide.with(image).load(itemInstance.imageUrl).dontTransform().into(object :
                CustomViewTarget<ImageView, Drawable>(image) {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                ) {
                    image.setImageDrawable(resource)
                    startPostponedEnterTransition()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {}

                override fun onResourceCleared(placeholder: Drawable?) {}
            })

            CoroutineScope(Dispatchers.IO).launch {
                if (viewModel.getLikedItem(itemInstance.itemId) != null) {//in liked items
                    toolbar.menu.getItem(0).apply {
                        icon = AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.favorite_filled
                        )
                        isChecked = true
                    }
                }

            }
        }
    }

    private fun setUpToolbar() {
        binding.apply {
            toolbar.menu.getItem(0).setOnMenuItemClickListener { menuItem ->
                menuItem.isChecked = !menuItem.isChecked
                if (menuItem.isChecked) {
                    menuItem.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.favorite_filled)
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.addToLikedItems(itemInstance.toLikedItemInstance())
                    }
                } else {
                    menuItem.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.favorite)
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.deleteLikedItem(itemInstance.itemId)
                    }
                }

                true
            }
        }

    }

    private fun setUpButtons() {
        binding.apply {
            addItem.setOnClickListener {
                if (this@FoodItemFragment.itemCount >= 10)
                    return@setOnClickListener
                this@FoodItemFragment.itemCount++
                totalPrice.text = "${(itemInstance.price * this@FoodItemFragment.itemCount)} ₹"
                itemCount.text = this@FoodItemFragment.itemCount.toString()
            }
            subItem.setOnClickListener {
                if (this@FoodItemFragment.itemCount < 2)
                    return@setOnClickListener
                this@FoodItemFragment.itemCount--
                totalPrice.text = "${(itemInstance.price * this@FoodItemFragment.itemCount)} ₹"
                itemCount.text = this@FoodItemFragment.itemCount.toString()
            }
            addCartBtn.setOnClickListener {
                val orderInstance = Order(
                    itemInstance.itemId,
                    itemInstance.itemName,
                    itemCount.text.toString().toInt(),
                    itemInstance.price
                )
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.addToCart(orderInstance)
                }

                Snackbar.make(rootView, "Item Added To Card!", Snackbar.LENGTH_SHORT)
                    .setAnchorView(addCartBtn).apply {
                        setAction("View", {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                            (requireActivity() as MainActivity).showFragment(CheckoutFragment())
                        })
                    }.show()
            }
            orderBtn.setOnClickListener {
                it.isEnabled = false
                viewModel.performOrder(itemInstance, "", this@FoodItemFragment.itemCount) { e ->
                    it.isEnabled = true
                    Snackbar.make(rootView, "${itemInstance.itemName} ordered!", Snackbar.LENGTH_SHORT)
                        .setAnchorView(addCartBtn).apply {
                            setAction("View", {
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                                (requireActivity() as MainActivity).showFragment(MyOrderFragment())
                            })
                        }.show()
                }
            }
        }
    }
}