package com.devoid.menumate.prsentation.ui.fragments

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.devoid.menumate.R
import com.devoid.menumate.databinding.HomepageFoodItemBinding
import com.devoid.menumate.databinding.HomepageFragmentBinding
import com.devoid.menumate.databinding.HomepageParentFoodItemBinding
import com.devoid.menumate.databinding.HomepageRecomendedRecyclerBinding
import com.devoid.menumate.databinding.LikedItemsListItemBinding
import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.Restaurant
import com.devoid.menumate.prsentation.state.RestaurantUiState
import com.devoid.menumate.prsentation.ui.KitchenDashboardActivity
import com.devoid.menumate.prsentation.ui.MainActivity
import com.devoid.menumate.prsentation.ui.RestaurantSetupActivity
import com.devoid.menumate.prsentation.viewmodel.HomePageViewModel
import com.devoid.menumate.prsentation.viewmodel.showLogoutDialog
import com.devoid.menumate.prsentation.shimmer.Shimmer
import com.devoid.menumate.prsentation.shimmer.ShimmerDrawable
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomepageFragment : Fragment() {
    private val TAG = HomepageFragment::class.java.simpleName
    private val viewmodel: HomePageViewModel by viewModels()
    private val currentUser = Firebase.auth.currentUser
    private lateinit var binding: HomepageFragmentBinding
    lateinit var rootLayout:View
    private val parentItemAdapter = ParentItemAdapter()
    private val childRecyclerAdapter = ChildRecyclerAdapter()
    private val searchViewRecyclerAdapter = SearchViewRecyclerAdapter()
    private val shimmer =
        Shimmer.AlphaHighlightBuilder()// The attributes for a ShimmerDrawable is set by this builder
            .setDuration(1800) // how long the shimmering animation takes to do one full sweep
            .setBaseAlpha(0.7f) //the alpha of the underlying children
            .setHighlightAlpha(0.9f) // the shimmer alpha amount
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HomepageFragmentBinding.inflate(layoutInflater, container, false)
        init()
        setUpSearchView()
        observeViewModel()
        return binding.root
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewmodel.state.collect { uiState ->
                        when (uiState) {
                            RestaurantUiState.Loading -> binding.refreshLayout.isRefreshing = true
                            is RestaurantUiState.LocalRestaurant -> {}
                            is RestaurantUiState.RemoteRestaurant -> {
                                if (viewmodel.itemsLoaded) {
                                    binding.refreshLayout.isRefreshing = false
                                    val menuItems = uiState.restaurant.menu_items!!.toMutableList()
                                    if (menuItems.size >= 3)
                                        menuItems.add(
                                            2,
                                            MenuItem()
                                        ) //empty menu item for recommended recycler
                                    parentItemAdapter.submitList(null)
                                    parentItemAdapter.submitList(menuItems)
                                } else {
                                    displayData(uiState.restaurant)
                                    viewmodel.loadMenuItems(uiState.restaurant)
                                    viewmodel.loadMenuItemsByRating()
                                }
                            }
                        }
                    }
                }
                launch {
                    viewmodel.itemsByRating.collect {
                        childRecyclerAdapter.submitList(it)
                    }
                }
            }
        }
    }

    private fun setUpSearchView() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.searchResults.collect { items ->
                    searchViewRecyclerAdapter.submitList(items)
                    binding.searchResultsRecycler.smoothScrollToPosition(0)
                }
            }
        }
        binding.searchResultsRecycler.adapter = searchViewRecyclerAdapter
        binding.searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                viewmodel.submitQuery(s.toString())
            }
        })

    }


    private fun init() {
         rootLayout= requireActivity().window.decorView.findViewById(android.R.id.content)
        binding.categoriesRecycler.adapter = parentItemAdapter
        if (currentUser?.uid == viewmodel.tableInfo.restaurantId) {
            binding.searchBar.menu.findItem(R.id.register_rest).isVisible = false
            binding.searchBar.menu.findItem(R.id.restaurant_dash).isVisible = true
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, true) {
        }
        binding.cartBtn.setOnClickListener {
            (requireActivity() as MainActivity).showFragment(CheckoutFragment())
        }
        binding.searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.my_orders -> {
                    (requireActivity() as MainActivity).showFragment(MyOrderFragment())
                }

                R.id.register_rest -> {
                    requireActivity().startActivity(
                        Intent(
                            requireActivity(),
                            RestaurantSetupActivity::class.java
                        )
                    )
                }

                R.id.restaurant_dash -> {
                    requireActivity().startActivity(
                        Intent(
                            requireActivity(),
                            KitchenDashboardActivity::class.java
                        )
                    )
                }

                R.id.log_out -> {
                    showLogoutDialog(requireActivity() as AppCompatActivity)
                }

                R.id.liked_page_btn -> {
                    (requireActivity() as MainActivity).showFragment(MyLikedItemsFragment())
                }
            }
            true
        }
        binding.chipGroup.apply {
            post {
                check(R.id.all_chip)
                setOnCheckedStateChangeListener { _, checkedIds ->
                   val selectedChip:Chip= findViewById(checkedIds[0])
                    categorySelected(selectedChip.text.toString())
                }
            }
        }
        binding.refreshLayout.setOnRefreshListener {
            parentItemAdapter.submitList(null)
            val selectedChip:Chip= binding.chipGroup.findViewById(binding.chipGroup.checkedChipId)
            categorySelected(selectedChip.text.toString())
        }
    }

    private fun displayData(restaurant: Restaurant) {
        restaurant.cats.forEach { category ->
            (layoutInflater.inflate(
                R.layout.filled_chip,
                binding.chipGroup,
                false
            ) as Chip).apply {
                text = category
                binding.chipGroup.addView(this)
            }
        }
        binding.searchBar.hint = restaurant.name
    }

    private fun categorySelected(category:String) {
        binding.refreshLayout.isRefreshing = true
        binding.chipGroup.apply {
            val restaurant =
                (viewmodel.state.value as RestaurantUiState.RemoteRestaurant).restaurant
            if (category.equals("All",true) ) {//all
                viewmodel.loadMenuItems(restaurant)
            } else {
                viewmodel.loadMenuItemsByCategory(category)
            }
        }
    }

    private fun loadImage(imageView: ImageView, url: String) {
        val shimmerDrawable = ShimmerDrawable().apply {
            shimmer = this@HomepageFragment.shimmer
        }
        Glide.with(imageView).load(url)
            .placeholder(shimmerDrawable)
            .error(R.drawable.broken_image).dontTransform()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    p0: GlideException?,
                    p1: Any?,
                    p2: Target<Drawable>,
                    p3: Boolean
                ): Boolean {
                    Log.e(TAG, "onLoadFailed: $p0")
                    imageView.setPadding(400)
                    return false
                }

                override fun onResourceReady(
                    p0: Drawable,
                    p1: Any,
                    p2: Target<Drawable>?,
                    p3: DataSource,
                    p4: Boolean
                ): Boolean {
                    return false
                }

            })
            .into(imageView)
    }

    fun showFoodItemWithTransition(
        menuItem: MenuItem,
        root:ViewGroup,
        imageView: ImageView,
        title: TextView,
        price: TextView
    ) {
        (requireActivity() as MainActivity).showFragmentTransition(
            menuItem,
            root,
            imageView,
            title,
            price
        )
    }

    private inner class ParentItemAdapter : ListAdapter<MenuItem, ViewHolder>(DiffCallback()) {
        private val VIEWTYPE_NORMAL = 0
        private val VIEWTYPE_RECYCLER = 1


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return when (viewType) {
                VIEWTYPE_NORMAL -> {
                    ParentViewHolder(
                        HomepageParentFoodItemBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                }

                VIEWTYPE_RECYCLER -> {
                    RecommendedViewHolder(
                        HomepageRecomendedRecyclerBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                }

                else -> throw IllegalArgumentException("Invalid view type")
            }

        }

        override fun getItemViewType(position: Int): Int {
            if (position == 2) return VIEWTYPE_RECYCLER
            return VIEWTYPE_NORMAL
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                VIEWTYPE_NORMAL -> {
                    (holder as ParentViewHolder).bind(getItem(position))
                }

                VIEWTYPE_RECYCLER -> {
                    (holder as RecommendedViewHolder).bind()
                }
            }
        }

        override fun onViewRecycled(holder: ViewHolder) {
            holder.itemView.findViewById<ImageView>(R.id.image).apply {
                setImageDrawable(null)
                setPadding(0)
            }
        }
    }

    inner class ParentViewHolder(private val binding: HomepageParentFoodItemBinding) :
        ViewHolder(binding.root) {
        fun bind(menuItem: MenuItem) {
            binding.apply {
                ViewCompat.setTransitionName(image, "image$adapterPosition")
                ViewCompat.setTransitionName(title, "title$adapterPosition")
                ViewCompat.setTransitionName(price, "price$adapterPosition")
                ViewCompat.setTransitionName(root, "root$adapterPosition")
                title.text = menuItem.itemName
                time.text = "${menuItem.time} mins"
                starsText.text = menuItem.rating.toString()
                price.text = "${menuItem.price} ₹"
                loadImage(image, menuItem.imageUrl.toString())

                root.setOnClickListener {
                    showFoodItemWithTransition(menuItem,root, image, title, price)
                }
            }
        }
    }

    inner class RecommendedViewHolder(private val binding: HomepageRecomendedRecyclerBinding) :
        ViewHolder(binding.root) {
        fun bind() {
            binding.itemsRecycler.apply {
                layoutManager =
                    LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = childRecyclerAdapter
            }

        }
    }


    private inner class ChildRecyclerAdapter :
        ListAdapter<MenuItem, RecommendedChildViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecommendedChildViewHolder {
            return RecommendedChildViewHolder(
                HomepageFoodItemBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: RecommendedChildViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onViewRecycled(holder: RecommendedChildViewHolder) {
            holder.itemView.findViewById<ImageView>(R.id.image).apply {
                setImageDrawable(null)
                setPadding(0)
            }
        }
    }

    inner class RecommendedChildViewHolder(private val binding: HomepageFoodItemBinding) :
        ViewHolder(binding.root) {
        fun bind(menuItem: MenuItem) {
            binding.apply {
                val transitionName = parentItemAdapter.currentList.size + adapterPosition
                ViewCompat.setTransitionName(image, "image${transitionName}")
                ViewCompat.setTransitionName(title, "title${transitionName}")
                ViewCompat.setTransitionName(price, "price${transitionName}")
                ViewCompat.setTransitionName(root, "root${transitionName}")
                title.text = menuItem.itemName
                time.text = "${menuItem.time} mins"
                starsText.text = menuItem.rating.toString()
                price.text = "${menuItem.price} ₹"
                loadImage(image, menuItem.imageUrl.toString())
                root.setOnClickListener {
                    showFoodItemWithTransition(menuItem,root, image, title, price)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return newItem.itemId == oldItem.itemId
        }

    }

    private inner class SearchViewRecyclerAdapter :
        ListAdapter<MenuItem, SearchViewRecyclerViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SearchViewRecyclerViewHolder {
            return SearchViewRecyclerViewHolder(
                LikedItemsListItemBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: SearchViewRecyclerViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

    }

    private inner class SearchViewRecyclerViewHolder(private val binding: LikedItemsListItemBinding) :
        ViewHolder(binding.root) {
        fun bind(menuItem: MenuItem) {
            binding.apply {
                title.text = menuItem.itemName
                title.setTextColor(ContextCompat.getColor(requireContext(),R.color.colorOnSurface))
                description.hint=menuItem.description
                image.setImageDrawable(AppCompatResources.getDrawable(binding.root.context,R.drawable.grocery))
                image.setColorFilter(ContextCompat.getColor(requireContext(),R.color.colorPrimary),PorterDuff.Mode.SRC_IN)
                addCartBtn.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        viewmodel.addItemToCart(menuItem,0)
                    }
                    Snackbar.make(rootLayout,"Item Added To Cart",Snackbar.LENGTH_SHORT)
                        .setAnchorView(this@HomepageFragment.binding.snackbarAnchor)
                        .setAction("View"){
                            this@HomepageFragment.binding.searchView.hide()
                            (requireActivity() as MainActivity).showFragment(CheckoutFragment())
                        }.show()
                }
                root.setOnClickListener {
                    this@HomepageFragment.binding.searchView.hide()
                    (requireActivity() as MainActivity).showFragment(FoodItemFragment(menuItem))
                }
            }
        }
    }
}
