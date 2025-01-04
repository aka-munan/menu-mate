package com.devoid.menumate.prsentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.MutableIntList
import androidx.collection.mutableIntListOf
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.TransitionManager
import com.devoid.menumate.R
import com.devoid.menumate.databinding.KitchenDashItemBinding
import com.devoid.menumate.databinding.KitchenDashboardFoodItemBinding
import com.devoid.menumate.databinding.KitchenDashbordBinding
import com.devoid.menumate.domain.model.KitchenOrder
import com.devoid.menumate.domain.model.ORDERSTATUS_PENDING
import com.devoid.menumate.domain.model.ORDERSTATUS_PREPARING
import com.devoid.menumate.domain.model.ORDERSTATUS_READY
import com.devoid.menumate.domain.model.RemoteOrder
import com.devoid.menumate.prsentation.ui.fragments.QRGenerateDialogFragment
import com.devoid.menumate.prsentation.ui.recycler.RecyclerDividerDecoration
import com.devoid.menumate.prsentation.viewmodel.KitchenDashboardViewModel
import com.devoid.menumate.prsentation.viewmodel.showLogoutDialog
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class KitchenDashboardActivity : AppCompatActivity() {
    private val TAG = KitchenDashboardActivity::class.simpleName
    private val viewModel: KitchenDashboardViewModel by viewModels()
    private lateinit var binding: KitchenDashbordBinding
    private val parentAdapter = RecyclerAdapter()
    private val dateFormater = SimpleDateFormat("dd/MM/yy , hh:mm aa", Locale.getDefault())
    private val statusList = listOf("new", "preparing", "done")
    private val expandedItems: MutableIntList = mutableIntListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KitchenDashbordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.orders, viewModel.isLoading) { orders, isLoading ->
                    binding.swipeRefreshLayout.isRefreshing = isLoading
                    parentAdapter.submitList(orders)
                }.collect {}
            }

        }
    }


    private fun setupUI() {
        binding.apply {
            itemsRecycler.adapter = parentAdapter
            itemsRecycler.addItemDecoration(RecyclerDividerDecoration(this@KitchenDashboardActivity))
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadOrders()
            }
            setupToolbar()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.log_out -> {
                    showLogoutDialog(this@KitchenDashboardActivity)
                }

                R.id.generate_scanner -> {
                    val dialog = QRGenerateDialogFragment()
                    dialog.show(supportFragmentManager, null)
                }

                R.id.edit -> {
                    startActivity(Intent(this, RestaurantSetupActivity::class.java))
                }
            }
            true
        }
    }


    private inner class RecyclerAdapter :
        ListAdapter<KitchenOrder, ParentItemViewHolder>(KitchenOrderDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentItemViewHolder {
            return ParentItemViewHolder(
                KitchenDashItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }


        override fun onBindViewHolder(holder: ParentItemViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

    }

    private inner class ParentItemViewHolder(private val binding: KitchenDashItemBinding) :
        ViewHolder(binding.root) {

        fun bind(order: KitchenOrder) {
            binding.apply {
                title.text = "Table ${order.table_no}"
                orderTime.hint = order.time?.toDate()?.let { dateFormater.format(it) }
                status.text = statusList[order.status]
                prepareBtn.text = if (order.status == 0) "Start preparing" else "Mark as Ready"
                if (order.spi.trim().isEmpty()) specialInst.hint = "none" else specialInst.text =
                    order.spi
                if (adapterPosition in expandedItems)
                    expandLayout(order)
                binding.root.setOnClickListener {
                    if (adapterPosition in expandedItems) {//collapse
                        collapseLayout()
                    } else {//expand
                        expandLayout(order)
                    }
                }
                prepareBtn.setOnClickListener {
                    when (order.status) {
                        ORDERSTATUS_PENDING -> {
                            order.status = ORDERSTATUS_PREPARING
                        }

                        ORDERSTATUS_PREPARING -> {
                            order.status = ORDERSTATUS_READY
                        }
                    }
                    viewModel.updateOrder(order)
                }
            }
        }

        private fun expandLayout(order: KitchenOrder) {
            binding.apply {
                expandedItems.add(adapterPosition)
                TransitionManager.beginDelayedTransition(binding.root)
                expandIconBtn.animate().rotation(-90f).setDuration(300L).start()
                expandableLayout.isVisible = true
                if (foodItems.adapter == null) {
                    val orderItemRecyclerAdapter = OrderItemRecyclerAdapter(layoutPosition)
                    foodItems.adapter = orderItemRecyclerAdapter
                    orderItemRecyclerAdapter.submitList(order.items)
                }
            }
        }

        private fun collapseLayout() {
            binding.apply {
                expandedItems.remove(adapterPosition)
                foodItems.adapter = null
                expandableLayout.visibility = View.GONE
                expandIconBtn.animate().rotation(90f).setDuration(300L).start()
            }

        }
    }

    class KitchenOrderDiffCallback : DiffUtil.ItemCallback<KitchenOrder>() {
        override fun areItemsTheSame(oldItem: KitchenOrder, newItem: KitchenOrder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: KitchenOrder, newItem: KitchenOrder): Boolean {
            return oldItem == newItem
        }
    }

    //child recycler adapter
    private inner class OrderItemRecyclerAdapter(private val parentAdapterPosition: Int) :
        ListAdapter<RemoteOrder, OrderItemViewHolder>(OrderDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
            return OrderItemViewHolder(
                KitchenDashboardFoodItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                parentAdapterPosition
            )
        }


        override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

    }

    private inner class OrderItemViewHolder(
        private val binding: KitchenDashboardFoodItemBinding,
        private val parentAdapterPosition: Int
    ) :
        ViewHolder(binding.root) {

        fun bind(order: RemoteOrder) {
            binding.apply {
                title.text = order.name
                description.hint = "${order.itemCount}x ${order.name}"
                stateSelector.selectTab(stateSelector.getTabAt(order.status))
                root.setOnClickListener {
                    stateSelector.apply {
                        order.status = selectedTabPosition + 1
                        selectTab(getTabAt(selectedTabPosition + 1))

                    }
                    viewModel.updateOrder(parentAdapter.currentList[parentAdapterPosition])
                }
                stateSelector.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        order.status = tab!!.position
                        viewModel.updateOrder(parentAdapter.currentList[parentAdapterPosition])
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {}

                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<RemoteOrder>() {
        override fun areItemsTheSame(oldItem: RemoteOrder, newItem: RemoteOrder): Boolean {
            return newItem.id==oldItem.id
        }

        override fun areContentsTheSame(oldItem: RemoteOrder, newItem: RemoteOrder): Boolean {
            return oldItem == newItem
        }
    }
}