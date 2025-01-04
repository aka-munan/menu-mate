package com.devoid.menumate.prsentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devoid.menumate.databinding.CheckoutFragmentBinding
import com.devoid.menumate.databinding.CheckoutRecyclerItemBinding
import com.devoid.menumate.domain.model.Order
import com.devoid.menumate.prsentation.state.OrderState
import com.devoid.menumate.prsentation.ui.MainActivity
import com.devoid.menumate.prsentation.viewmodel.CheckOutFragmentSharedViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CheckoutFragment : Fragment() {
    private val TAG = CheckoutFragment::class.java.simpleName
    private val viewModel: CheckOutFragmentSharedViewModel by viewModels()
    private lateinit var rootView: View
    private val cartItemAdapter = CartItemAdapter()
    private var removedItem: Order? = null
    private lateinit var binding: CheckoutFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CheckoutFragmentBinding.inflate(inflater, container, false)
        init()
        observeViewModel()
        return binding.root
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cartItems.collect { state ->
                    when (state) {
                        is OrderState.Error -> {
                            binding.refreshLayout.isRefreshing = false
                        }

                        OrderState.Loading -> {
                            binding.refreshLayout.isRefreshing = true
                        }

                        is OrderState.Success<*> -> {
                            binding.refreshLayout.isRefreshing = false
                            cartItemAdapter.submitList(state.orders as List<Order>)
                            binding.itemsRecycler.smoothScrollToPosition(0)
                            updateTotalPrice()
                        }
                    }
                }
            }
        }
        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
                viewModel.getCartItems()
            }
        }
    }

    private fun init() {
        cartItemAdapter.submitList(null)
        rootView = requireActivity().window.decorView.findViewById(android.R.id.content)
        binding.itemsRecycler.adapter = cartItemAdapter
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.checkoutBtn.setOnClickListener {
            checkout()
        }
        binding.refreshLayout.setOnRefreshListener {
            observeViewModel()
        }
        val swipeToDismiss = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
                removedItem = cartItemAdapter.currentList[viewHolder.adapterPosition]
                cartItemAdapter.submitList(
                    cartItemAdapter.currentList.toMutableList()
                        .apply { removeAt(viewHolder.adapterPosition) })
                updateTotalPrice()
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.deleteFromCart(removedItem!!)
                }
                Snackbar.make(rootView, "${removedItem!!.name} removed.", Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.checkoutBtn).apply {
                        setAction("undo") {
                            cartItemAdapter.submitList(
                                cartItemAdapter.currentList.toMutableList()
                                    .apply { add(removedItem!!) })
                            updateTotalPrice()
                            CoroutineScope(Dispatchers.IO).launch {
                                viewModel.addToCart(removedItem!!)
                            }
                            dismiss()
                        }
                        addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                removedItem = null
                            }
                        })
                    }.show()
            }
        }
        ItemTouchHelper(swipeToDismiss).attachToRecyclerView(binding.itemsRecycler)

    }

    private fun checkout() {
        val spi = binding.specialInst.text.toString()
        viewModel.performOrder(spi = spi, orders = cartItemAdapter.currentList, onResult = { e ->
            e?.let {
                Snackbar.make(rootView, "Failed to place your order!", Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.checkoutBtn).show()
                return@performOrder
            }
            Snackbar.make(rootView, "Order Placed.", Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.checkoutBtn).apply {
                    setAction("View", {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        (requireActivity() as MainActivity).showFragment(MyOrderFragment())
                    })
                }.show()
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.clearCart()
            }
            cartItemAdapter.submitList(null)

        })
    }

    private fun updateTotalPrice() {
        binding.totalPrice.text =
            "₹ ${cartItemAdapter.currentList.sumOf { (it.price * it.itemCount).toDouble() }} "
    }

    private inner class CartItemAdapter :
        ListAdapter<Order, ViewHolder>(DiffCallBack()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                CheckoutRecyclerItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

    }

    private inner class ViewHolder(private val binding: CheckoutRecyclerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.apply {
                title.text = order.name
                price.hint = "₹ ${order.price} "
                totalPrice.text = "₹ ${order.price * order.itemCount}"
                itemCount.text = order.itemCount.toString()
                addItem.setOnClickListener {
                    if (order.itemCount >= 10)
                        return@setOnClickListener
                    order.itemCount++
                    totalPrice.text = "${(order.price * order.itemCount)} ₹"
                    itemCount.text = order.itemCount.toString()
                    updateTotalPrice()
                }
                subItem.setOnClickListener {
                    if (order.itemCount < 2)
                        return@setOnClickListener
                    order.itemCount--
                    totalPrice.text = "${(order.price * order.itemCount)} ₹"
                    itemCount.text = order.itemCount.toString()
                    updateTotalPrice()
                }
                Glide.with(image).load(order.imageUrl).dontTransform().into(image)
            }
        }
    }

    private class DiffCallBack : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }

    }
}