package com.cleanspace.presentation.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cleanspace.data.local.database.entities.WhitelistEntity
import com.cleanspace.databinding.ItemWhitelistBinding

class WhitelistAdapter(
    private val onToggle: (WhitelistEntity) -> Unit,
    private val onRemove: (WhitelistEntity) -> Unit,
) : ListAdapter<WhitelistEntity, WhitelistAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<WhitelistEntity>() {
        override fun areItemsTheSame(oldItem: WhitelistEntity, newItem: WhitelistEntity) =
            oldItem.path == newItem.path

        override fun areContentsTheSame(oldItem: WhitelistEntity, newItem: WhitelistEntity) =
            oldItem == newItem
    }

    inner class VH(private val binding: ItemWhitelistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhitelistEntity) {
            binding.tvPath.text = item.path

            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = item.isEnabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != item.isEnabled) {
                    onToggle(item)
                }
            }

            binding.btnDelete.setOnClickListener { onRemove(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWhitelistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
