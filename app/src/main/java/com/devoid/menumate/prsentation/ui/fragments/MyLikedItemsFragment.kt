package com.devoid.menumate.prsentation.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devoid.menumate.prsentation.ui.MainActivity
import com.devoid.menumate.databinding.LikedItemsListItemBinding
import com.devoid.menumate.databinding.MyOrdersBinding
import com.devoid.menumate.domain.model.LikedItemInstance
import com.devoid.menumate.domain.model.Order
import com.devoid.menumate.prsentation.viewmodel.CheckOutFragmentSharedViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyLikedItemsFragment:Fragment() {
    private val viewModel :CheckOutFragmentSharedViewModel by viewModels()
    private lateinit var binding: MyOrdersBinding
    private val likedItemsAdapter = LikedItemsAdapter()
    private var removedItem: LikedItemInstance? = null
    private val TAG = MyLikedItemsFragment::class.java.simpleName
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=MyOrdersBinding.inflate(inflater,container,false)
        init()
        observeViewModel()
        return binding.root
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
                viewModel.likedItems.collect{items->
                    binding.swipeRefreshLayout.isRefreshing = false
                    likedItemsAdapter.submitList(items)
                }
        }
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.getLikedItems()
        }
    }

    private fun init(){
        binding.toolbar.title = "Favorites"
        binding.itemsRecycler.adapter = likedItemsAdapter
        binding.toolbar.setNavigationOnClickListener{
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            observeViewModel()
        }
     val swipeToDismiss = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT)  {
          override fun onMove(
              recyclerView: RecyclerView,
              viewHolder: RecyclerView.ViewHolder,
              target: RecyclerView.ViewHolder
          ): Boolean {
              return false
          }

          override fun onSwiped(
              viewHolder: RecyclerView.ViewHolder,
              direction: Int
          ) {
              removedItem = likedItemsAdapter.currentList[viewHolder.adapterPosition]
              likedItemsAdapter.submitList(likedItemsAdapter.currentList.toMutableList().apply { removeAt(viewHolder.adapterPosition)})
              CoroutineScope(Dispatchers.IO).launch {
                  viewModel.deleteLikedItem(removedItem!!.itemId)
              }
              val rootView: View = requireActivity().window.decorView.findViewById(android.R.id.content)
              Snackbar.make(rootView,"${removedItem!!.name} removed.",Snackbar.LENGTH_SHORT).setAnchorView(binding.snackbarAnchor).apply {
                  setAction("undo") {
                      likedItemsAdapter.submitList(likedItemsAdapter.currentList.toMutableList().apply { add(removedItem)})
                      CoroutineScope(Dispatchers.IO).launch {
                          viewModel.addToLikedItems(removedItem!!)
                      }
                      dismiss()
                  }
                  addCallback(object :Snackbar.Callback(){
                      override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                          super.onDismissed(transientBottomBar, event)
                          removedItem=null
                      }
                  })
              }.show()
          }
      }
        ItemTouchHelper(swipeToDismiss).attachToRecyclerView(binding.itemsRecycler)
    }

    private inner class LikedItemsAdapter: ListAdapter<LikedItemInstance,ViewHolder>(DiffCallback()){
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            return ViewHolder( LikedItemsListItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder(private val binding: LikedItemsListItemBinding): RecyclerView.ViewHolder(binding.root){
       fun bind(likedItemInstance: LikedItemInstance){
           binding.apply {
               title.text = likedItemInstance.name
               Glide.with(image).load(likedItemInstance.imageUrl).dontTransform().into(image)
               addCartBtn.setOnClickListener {
                   val orderInstance=
                       Order(likedItemInstance.itemId,likedItemInstance.name,1 ,likedItemInstance.price)
                   CoroutineScope(Dispatchers.IO).launch {
                       viewModel.addToCart(orderInstance)
                   }
                   val rootView: View = requireActivity().window.decorView.findViewById(android.R.id.content)
                   Snackbar.make(rootView,"Item Added To Card!",Snackbar.LENGTH_SHORT).setAnchorView(this@MyLikedItemsFragment.binding.snackbarAnchor).apply {
                       setAction("View") {
                           (requireActivity() as MainActivity).showFragment(CheckoutFragment())
                       }
                   }.show()
               }
           }
       }
    }
    private class DiffCallback:DiffUtil.ItemCallback<LikedItemInstance>(){
        override fun areItemsTheSame(
            oldItem: LikedItemInstance,
            newItem: LikedItemInstance
        ): Boolean {
            return newItem.itemId==oldItem.itemId
        }

        override fun areContentsTheSame(
            oldItem: LikedItemInstance,
            newItem: LikedItemInstance
        ): Boolean {
            return newItem==oldItem
        }

    }
}