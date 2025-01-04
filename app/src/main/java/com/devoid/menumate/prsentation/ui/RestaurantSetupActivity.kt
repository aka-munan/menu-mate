package com.devoid.menumate.prsentation.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.HorizontalScrollView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.devoid.menumate.R
import com.devoid.menumate.databinding.KitchenMenuItemBinding
import com.devoid.menumate.databinding.ResturantSetup1Binding
import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.Restaurant
import com.devoid.menumate.prsentation.state.RestaurantUiState
import com.devoid.menumate.prsentation.viewmodel.RestaurantSetupViewModel
import com.devoid.menumate.utils.ResultCallback
import com.devoid.menumate.utils.saveImageToTmpFile
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RestaurantSetupActivity : AppCompatActivity() {

    private var currentUser: FirebaseUser = Firebase.auth.currentUser!!
    private val viewModel: RestaurantSetupViewModel by viewModels()
    private final val TAG = "RestaurantSetupActivity"
    private val categories: MutableList<String> = mutableListOf()
    private lateinit var adapter: ArrayAdapter<String>
    private var menuItemAdapter = MenuItemAdapter()
    private lateinit var getImage: ActivityResultLauncher<String>
    private lateinit var resultCallback: ResultCallback<Uri?>
    private lateinit var binding: ResturantSetup1Binding
    private var restaurant: Restaurant = Restaurant()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getImage = registerForActivityResult(
            GetContent()
        ) { uri: Uri? ->
            resultCallback.onResult(uri)
        }
        binding = ResturantSetup1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setBtnListeners()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.restaurantUiState.collect {
                    when (it) {
                        RestaurantUiState.Loading -> {

                        }

                        is RestaurantUiState.LocalRestaurant -> {

                        }

                        is RestaurantUiState.RemoteRestaurant -> {
                            restaurant = it.restaurant
                            displayRemoteRestaurant(restaurant = it.restaurant)
                            if (viewModel.itemsLoaded)
                                menuItemAdapter.submitList(
                                    it.restaurant.menu_items!!.toMutableList().map { it.copy() })
                            else
                                viewModel.loadMenuItems(it.restaurant)
                        }
                    }
                }
            }
        }
    }

    private fun init() {
        binding.itemsRecycler.adapter = menuItemAdapter
        adapter = ArrayAdapter<String>(this, R.layout.auto_comp_textview, ArrayList<String>())
        binding.categoriesEdittext.setOnEditorActionListener(
            OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE
                ) {
                    val text = binding.categoriesEdittext.text!!
                    val chip = getChipView(text.toString())
                    categories.add(text.toString())
                    adapter.add(text.toString())
                    adapter.notifyDataSetChanged()
                    binding.chipsGroup.addView(chip, binding.chipsGroup.childCount - 1)
                    text.clear()
                    Log.i(TAG, "init: $categories")
                    Handler(mainLooper).postDelayed({
                        binding.scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
                    }, 100)
                    return@OnEditorActionListener true
                }
                false
            })
        //setUpMenuItemsRecycler()
    }

    private fun displayRemoteRestaurant(restaurant: Restaurant) {
        binding.apply {
            resName.setText(restaurant.name)
            description.setText(restaurant.desc)
            categories.clear()
            categories.addAll(restaurant.cats)
            chipsGroup.removeViews(0, chipsGroup.childCount - 1)
            for (category in categories) {
                chipsGroup.apply {
                    addView(getChipView(category), childCount - 1)
                }
            }
            adapter.addAll(categories)
            adapter.notifyDataSetChanged()
        }
    }


    private fun getChipView(text: String): Chip {
        val chip: Chip = layoutInflater.inflate(
            R.layout.input_chip_item,
            binding.chipsGroup,
            false
        ) as Chip
        chip.text = text
        chip.setOnCloseIconClickListener {
            categories.remove(text)
            binding.chipsGroup.removeView(chip)
            adapter.remove(text)
            adapter.notifyDataSetChanged()

        }
        return chip
    }

    private fun setBtnListeners() {
        binding.saveBtn.setOnClickListener {
            binding.apply {
                restaurant.name = resName.text.toString()
                restaurant.cats = categories
                restaurant.desc = description.text.toString()
            }
            showSaveToCloudBottomSheet()
        }
        binding.addItemBtn.setOnClickListener {
            addMenuItem()
        }
    }

    private fun addMenuItem() {
        val itemId = Firebase.database.reference.child("users/${currentUser.uid}").push().key!!
        Log.d(TAG, "setBtnListeners: new item added , id: $itemId")
        val newMenuItem = MenuItem(itemId = itemId, isRemote = false)
        viewModel.addMenuItem(newMenuItem)
        val updatedList = menuItemAdapter.currentList.toMutableList()
        updatedList.add(newMenuItem)
        menuItemAdapter.submitList(updatedList)
        binding.itemsRecycler.smoothScrollToPosition(updatedList.size - 1)
    }

    private fun removeMenuItem(position: Int) {
        val updatedList = menuItemAdapter.currentList.toMutableList()
        viewModel.removeMenuItem(updatedList[position])
        updatedList.removeAt(position)
        menuItemAdapter.submitList(updatedList)
    }

    private fun showSaveToCloudBottomSheet() {
        val dialog = BottomSheetDialog()
        dialog.apply {
            icon = R.drawable.upload
            title = "Save To Cloud!"
            secondaryText = "Save your data to cloud. Press Save to Continue."

        }
        dialog.listener = BottomSheetDialog.OnShowListener {
            dialog.primaryBtn("Save") {
                dialog.binding.apply {
                    secondaryBtn.isEnabled = false
                    primaryBtn.isEnabled = false
                    dialog.isCancelable = false
                    primaryBtnText.text = "Processing..."
                    dialog.updateAnimation(R.raw.cloud_uploading, 3f, 999)
                    viewModel.saveToRemoteRestaurant(menuItemAdapter.currentList) { e, progress ->
                        e?.let {
                            it.printStackTrace()
                            return@saveToRemoteRestaurant
                        }
                        when (progress) {
                            30 -> {
                                Log.i(TAG, "showSaveToCloudBottomSheet: 30")
                                primaryBtnText.text = "Syncing with cloud..."
                            }

                            70 -> {
                                Log.i(TAG, "showSaveToCloudBottomSheet: 70")
                                primaryBtnText.text = "Almost there!"
                            }

                            100 -> {
                                Log.i(TAG, "showSaveToCloudBottomSheet: 100")
                                dialog.updateAnimation(R.raw.done, 1.2f, 0)
                                primaryBtnText.text = "Done"
                                primaryBtn.isEnabled = true
                                dialog.dismissOnPrimaryBtnClick {
                                    startActivity(
                                        Intent(
                                            this@RestaurantSetupActivity,
                                            KitchenDashboardActivity::class.java
                                        )
                                    )
                                    finish()
                                }
                                Log.i(TAG, "setBtnListeners: Items synced successfully")
                            }
                        }
                    }
                }
            }
            dialog.secondaryBtn("Cancel") {
                dialog.dismiss()
            }
        }

        dialog.show(supportFragmentManager, BottomSheetDialog.TAG)
    }


    private inner class MenuItemAdapter :
        ListAdapter<MenuItem, MenuItemViewHolder>(DiffUtilCallback()) {
        private val TAG = "MenuIItemAdapter"
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
            return MenuItemViewHolder(
                KitchenMenuItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onViewRecycled(holder: MenuItemViewHolder) {
            super.onViewRecycled(holder)
            holder.binding.image.apply {
                setImageResource(R.drawable.upload)
                scaleX = 0.5f
                scaleY = 0.5f
                imageTintList = ColorStateList(
                    arrayOf(IntArray(1)),
                    IntArray(1) { getColor(R.color.colorOnSurface) })

            }
        }

    }

    private inner class MenuItemViewHolder(val binding: KitchenMenuItemBinding) :
        ViewHolder(binding.root) {
        fun bind(menuItem: MenuItem) {
            Log.i(TAG, "bind: ${menuItem.imageUrl}")
            if (menuItem.imageUrl.toString().isNotEmpty()) {
                binding.image.apply {
                    scaleX = 1f
                    scaleY = 1f
                    imageTintList = null
                    Glide.with(this)
                        .load(menuItem.imageUrl)
                        .into(this)
                }
                binding.categoryEdittext.threshold = 1
            }
            //display data
            displayData(menuItem)
            listenForUserInput(menuItem)
            binding.apply {
                removeItemBtn.setOnClickListener {
                    removeMenuItem(adapterPosition)
                }
            }
            binding.image.setOnClickListener {
                pickImage(menuItem)
            }
        }

        private fun displayData(menuItem: MenuItem) {
            binding.apply {
                itemName.setText(menuItem.itemName)
                time.setText(menuItem.time)
                description.setText(menuItem.description)
                priceEdittext.setText("${menuItem.price}")
                categoryEdittext.setAdapter(adapter)
                categoryEdittext.setText(menuItem.category)
            }
        }

        private fun listenForUserInput(menuItem: MenuItem) {
            binding.apply {
                itemName.setOnFocusChangeListener { _, _ ->
                    menuItem.itemName = itemName.text.toString()
                }
                time.setOnFocusChangeListener { _, _ ->
                    menuItem.time = time.text.toString()
                }
                description.setOnFocusChangeListener { _, _ ->
                    menuItem.description = description.text.toString()
                }
                categoryEdittext.setOnFocusChangeListener { _, _ ->
                    menuItem.category = categoryEdittext.text.toString()
                }
                priceEdittext.apply {
                    setOnFocusChangeListener { _, _ ->
                        menuItem.price =
                            if (text.toString() == "") 1 else text.toString().toInt()
                    }
                }
            }
        }

        private fun pickImage(menuItem: MenuItem) {
            resultCallback = object : ResultCallback<Uri?> {
                override fun onResult(result: Uri?) {
                    if (result == null)
                        return
                    menuItem.imageUrl = saveImageToTmpFile(
                        this@RestaurantSetupActivity,
                        "item${adapterPosition}",
                        "jpg",
                        result,
                        50
                    )!!
                    binding.image.apply {
                        scaleX = 1f
                        scaleY = 1f
                        imageTintList = null
                        setImageURI(menuItem.imageUrl)
                    }
                    Log.i(TAG, "onBindViewHolder: ${menuItem.imageUrl}")
                }
            }
            getImage.launch("image/*")
        }
    }

    private class DiffUtilCallback : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return true
        }

    }
}