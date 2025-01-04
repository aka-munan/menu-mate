package com.devoid.menumate.prsentation.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.TransitionManager
import com.devoid.menumate.R
import com.devoid.menumate.data.remote.TABLENO
import com.devoid.menumate.databinding.MyOrdersBinding
import com.devoid.menumate.databinding.MyOrdersFoodItemBinding
import com.devoid.menumate.databinding.MyOrdersRecyclerItemBinding
import com.devoid.menumate.domain.model.KitchenOrder
import com.devoid.menumate.domain.model.ORDERSTATUS_PREPARING
import com.devoid.menumate.domain.model.ORDERSTATUS_READY
import com.devoid.menumate.domain.model.RemoteOrder
import com.devoid.menumate.prsentation.state.OrderState
import com.devoid.menumate.prsentation.viewmodel.MyOrdersViewModel
import com.google.firebase.database.DatabaseReference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyOrderFragment : Fragment() {
    private lateinit var binding: MyOrdersBinding
    private val viewModel: MyOrdersViewModel by viewModels()
    private val orderStatusList = listOf("Pending", "Preparing", "Finished")
    private lateinit var databaseReference: DatabaseReference
    private val TAG = MyOrderFragment::class.java.simpleName
    private val ordersAdapter = RecyclerAdapter()
    private val expandedItemPositions: MutableList<Int> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MyOrdersBinding.inflate(inflater, container, false)
        init()
        observeViewModel()
        return binding.root
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orderState.collect { state ->
                    when (state) {
                        OrderState.Loading -> {
                            binding.swipeRefreshLayout.isRefreshing = true
                        }

                        is OrderState.Success<*> -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            ordersAdapter.submitList(state.orders as List<KitchenOrder>)
                        }

                        is OrderState.Error -> {
                            state.exception.printStackTrace()
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    private fun init() {
        binding.itemsRecycler.adapter = ordersAdapter
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.observeOrders()
        }
    }

    private fun loadData() {
        databaseReference.orderByChild("table_no").equalTo(TABLENO.toDouble()).get()
            .addOnCompleteListener { task ->
                binding.swipeRefreshLayout.isRefreshing = false
                if (task.isSuccessful) {
                    if (!task.result.hasChildren()) {
                        binding.notFound.isVisible = true
                        return@addOnCompleteListener
                    }
                    //  orders.clear()
                    //orders.addAll((task.result.value as HashMap<*, *>).values as Collection<java.util.HashMap<String,Any>>)
                    binding.itemsRecycler.adapter!!.notifyDataSetChanged()
                } else {
                    Log.e(TAG, "init: ", task.exception)
                }
            }
    }

    private inner class RecyclerAdapter : ListAdapter<KitchenOrder, MyViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(
                MyOrdersRecyclerItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

    }

    private class DiffCallback : DiffUtil.ItemCallback<KitchenOrder>() {
        override fun areItemsTheSame(oldItem: KitchenOrder, newItem: KitchenOrder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: KitchenOrder, newItem: KitchenOrder): Boolean {
            return oldItem == newItem
        }


    }

    private inner class MyViewHolder(private val binding: MyOrdersRecyclerItemBinding) :
        ViewHolder(binding.root) {

        fun bind(order: KitchenOrder) {
            if (expandedItemPositions.contains(adapterPosition))
                expandLayout(order)
            binding.root.setOnClickListener {
                binding.apply {
                    if (expandedItemPositions.contains(adapterPosition))
                        collapseLayout()
                    else
                        expandLayout(order)

                }
            }
            binding.apply {
                orderdTime.hint = order.time?.toDate()?.time?.let {
                    DateUtils.getRelativeTimeSpanString(it)
                }
                title.text = "Order ${adapterPosition + 1}"
                if (order.status == ORDERSTATUS_PREPARING) {//order status = preparing
                    preparingImg.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorSuccess
                        )
                    )
                } else if (order.status == ORDERSTATUS_READY) {
                    preparingImg.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorSuccess
                        )
                    )
                    receivedImg.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorSuccess
                        )
                    )
                }
            }
        }

        fun expandLayout(order: KitchenOrder) {
            binding.apply {
                expandedItemPositions.add(adapterPosition)
                dropdownBtn.animate().rotation(-90f).setDuration(300).start()
                TransitionManager.beginDelayedTransition(binding.root)
                itemsRecycler.isVisible = true
                if (itemsRecycler.adapter == null) {
                    itemsRecycler.adapter = ChildRecyclerItemsAdapter(order.items)
                }
            }
        }

        fun collapseLayout() {
            binding.apply {
                expandedItemPositions.remove(adapterPosition)
                dropdownBtn.animate().rotation(90f).setDuration(300).start()
                itemsRecycler.isVisible = false
                itemsRecycler.adapter = null
            }
        }
    }

    private class ChildViewHolder(itemView: View) : ViewHolder(itemView) {
        val binding = MyOrdersFoodItemBinding.bind(itemView)
    }

    private inner class ChildRecyclerItemsAdapter(private val orderItems: List<RemoteOrder>) :
        Adapter<ChildViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
            val binding =
                MyOrdersFoodItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ChildViewHolder(binding.root)
        }

        override fun getItemCount(): Int {
            return orderItems.size
        }

        override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
            val orderInstance = orderItems[position]
            holder.binding.apply {
                title.text = orderInstance.name
                description.hint = "${orderInstance.itemCount}x${orderInstance.name}"
                holder.binding.status.text = orderStatusList[orderInstance.status]
            }
        }

    }
}