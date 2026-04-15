package com.cleanspace.presentation.clean

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cleanspace.data.models.FileItem
import com.cleanspace.databinding.ItemDuplicateGroupHeaderBinding
import com.cleanspace.databinding.ItemFileBinding
import com.cleanspace.domain.utils.FileSizeFormatter
import com.cleanspace.presentation.common.FileTypeUi

class CleanFileAdapter(
    private val onToggleFile: (String) -> Unit,
    private val onGroupHeaderClick: (String) -> Unit,
) : ListAdapter<CleanListItem, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_FILE = 1
    }

    object Diff : DiffUtil.ItemCallback<CleanListItem>() {
        override fun areItemsTheSame(oldItem: CleanListItem, newItem: CleanListItem): Boolean {
            return when {
                oldItem is CleanListItem.DuplicateGroupHeader && newItem is CleanListItem.DuplicateGroupHeader ->
                    oldItem.groupId == newItem.groupId

                oldItem is CleanListItem.FileRow && newItem is CleanListItem.FileRow ->
                    oldItem.file.id == newItem.file.id

                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: CleanListItem, newItem: CleanListItem): Boolean = oldItem == newItem
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is CleanListItem.DuplicateGroupHeader -> TYPE_HEADER
        is CleanListItem.FileRow -> TYPE_FILE
    }

    inner class HeaderVH(private val binding: ItemDuplicateGroupHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CleanListItem.DuplicateGroupHeader) {
            binding.tvHeaderTitle.text = item.title
            binding.tvHeaderSubtitle.text = "${item.count} файлів • ${FileSizeFormatter.formatBytes(item.totalSizeBytes)}"
            binding.root.setOnClickListener { onGroupHeaderClick(item.groupId) }
        }
    }

    inner class FileVH(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: CleanListItem.FileRow) {
            val item = row.file
            binding.tvName.text = item.name
            val typeLabel = FileTypeUi.displayName(binding.root.context, item.type)
            binding.tvDetails.text = "${typeLabel} • ${FileSizeFormatter.formatBytes(item.sizeBytes)}"

            binding.tvBadge.visibility = if (row.isOriginal) android.view.View.VISIBLE else android.view.View.GONE

            binding.cbSelect.setOnCheckedChangeListener(null)
            binding.cbSelect.isChecked = item.isSelected
            binding.cbSelect.setOnCheckedChangeListener { _, _ -> onToggleFile(item.id) }
            binding.root.setOnClickListener { onToggleFile(item.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemDuplicateGroupHeaderBinding.inflate(inflater, parent, false))
            else -> FileVH(ItemFileBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CleanListItem.DuplicateGroupHeader -> (holder as HeaderVH).bind(item)
            is CleanListItem.FileRow -> (holder as FileVH).bind(item)
        }
    }
}
