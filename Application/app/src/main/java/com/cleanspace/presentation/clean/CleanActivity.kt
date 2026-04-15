package com.cleanspace.presentation.clean

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cleanspace.databinding.ActivityBaseBinding
import com.cleanspace.databinding.ActivityCleanBinding
import com.cleanspace.presentation.common.BaseActivity
import com.cleanspace.domain.utils.FileSizeFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.cleanspace.data.models.FileType
import com.cleanspace.presentation.dashboard.DashboardActivity
import androidx.core.view.isVisible
import com.cleanspace.presentation.scan.ScanResultHolder
import com.cleanspace.data.repository.DeleteRequestException

@AndroidEntryPoint
class CleanActivity : BaseActivity(com.cleanspace.R.layout.activity_base) {

    private lateinit var baseBinding: ActivityBaseBinding
    private lateinit var binding: ActivityCleanBinding

    private val viewModel: CleanViewModel by viewModels()
    private lateinit var adapter: CleanFileAdapter

    private val deletePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                val finalize = viewModel.finalizePendingDeletionAfterSystemConfirmation()
                if (finalize.isSuccess) {
                    val freed = finalize.getOrDefault(0L)
                    if (freed > 0L) {
                        Toast.makeText(
                            this@CleanActivity,
                            getString(com.cleanspace.R.string.deleted_ok, FileSizeFormatter.formatBytes(freed)),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Видалення скасовано", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        binding = ActivityCleanBinding.inflate(layoutInflater, baseBinding.contentContainer, true)
        setupBottomNavigation(baseBinding.bottomNavigation)

        setupList()
        setupObservers()

        val category = intent.getStringExtra(DashboardActivity.EXTRA_CATEGORY)
        when (category) {
            DashboardActivity.CATEGORY_PHOTO -> {
                viewModel.loadFromLastScan(
                    filterTypes = setOf(FileType.MEDIA_IMAGES, FileType.SCREENSHOTS),
                )
            }

            DashboardActivity.CATEGORY_AUDIO_VIDEO -> {
                viewModel.loadFromLastScan(
                    filterTypes = setOf(FileType.MEDIA_AUDIO, FileType.MEDIA_VIDEO),
                )
            }

            else -> {
                val filterType = category?.let { runCatching { FileType.valueOf(it) }.getOrNull() }
                viewModel.loadFromLastScan(filterType = filterType)
            }
        }

        val modeExtra = intent.getStringExtra(CleanActivityExtras.EXTRA_MODE)
        if (modeExtra == CleanActivityExtras.ModeExtra.DUPLICATES.name) {
            // We’ll attempt to enter duplicates mode, but ViewModel will keep ALL mode until duplicates are computed.
            viewModel.setMode(CleanViewModel.Mode.DUPLICATES)
        }

        binding.btnSelectAll.setOnClickListener { viewModel.selectAll() }
        binding.btnDeselectAll.setOnClickListener { viewModel.deselectAll() }

        binding.btnClean.setOnClickListener { showDeleteConfirmation() }

        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is DeletePermissionEvent.Request -> {
                        val request = IntentSenderRequest.Builder(event.intentSender).build()
                        deletePermissionLauncher.launch(request)
                    }
                }
            }
        }

        binding.btnDuplicatesMode.setOnClickListener {
            // Dedicated duplicates search with progress.
            viewModel.startFindDuplicates()
        }
        binding.btnSelectDuplicatesKeepOne.setOnClickListener {
            viewModel.selectDuplicatesKeepOne(groupId = null)
        }
    }

    private fun setupList() {
        adapter = CleanFileAdapter(
            onToggleFile = { id -> viewModel.toggleFileSelection(id) },
            onGroupHeaderClick = { groupId -> viewModel.selectDuplicatesKeepOne(groupId) },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.listItems.collectLatest { adapter.submitList(it) }
        }

        // When duplicates screen is opened, we show the duplicates button. The list stays in ALL mode until
        // duplicates are computed.
        lifecycleScope.launch {
            viewModel.mode.collectLatest { mode ->
                val isDuplicatesMode = mode == CleanViewModel.Mode.DUPLICATES

                // Keep only one compact action in header; hide the long secondary button.
                binding.btnDuplicatesMode.isVisible = !isDuplicatesMode
                binding.btnSelectDuplicatesKeepOne.isVisible = false

                val hasDuplicates = (ScanResultHolder.lastScan?.duplicatesGroups?.isNotEmpty() == true)
                binding.tvDuplicatesHint.isVisible = !hasDuplicates && !isDuplicatesMode
            }
        }

        lifecycleScope.launch {
            viewModel.isFindingDuplicates.collectLatest { finding ->
                binding.duplicatesProgress.isVisible = finding
                binding.tvDuplicatesProgress.isVisible = finding
                binding.btnDuplicatesMode.isEnabled = !finding
                binding.tvDuplicatesHint.isVisible = !finding
            }
        }

        lifecycleScope.launch {
            viewModel.duplicatesProgress.collectLatest { p ->
                if (p == null) return@collectLatest
                binding.duplicatesProgress.progress = p.percent
                binding.tvDuplicatesProgress.text = getString(
                    com.cleanspace.R.string.duplicates_progress_format,
                    p.stage,
                    p.percent,
                )
            }
        }

        lifecycleScope.launch {
            viewModel.selectedCount.collectLatest { count ->
                val size = viewModel.selectedSize.value
                binding.tvSummary.text = getString(
                    com.cleanspace.R.string.selected_summary,
                    count,
                    FileSizeFormatter.formatBytes(size),
                )
            }
        }

        lifecycleScope.launch {
            viewModel.deleteProgress.collectLatest { p ->
                // MVP: show progress in summary
                if (viewModel.isDeleting.value) {
                    binding.tvSummary.text = getString(com.cleanspace.R.string.deleting_progress, p)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isDeleting.collectLatest { deleting ->
                if (!deleting) {
                    val count = viewModel.selectedCount.value
                    val size = viewModel.selectedSize.value
                    binding.tvSummary.text = getString(
                        com.cleanspace.R.string.selected_summary,
                        count,
                        FileSizeFormatter.formatBytes(size),
                    )
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        val count = viewModel.selectedCount.value
        val size = viewModel.selectedSize.value
        if (count <= 0) return

        AlertDialog.Builder(this)
            .setTitle(getString(com.cleanspace.R.string.confirm_deletion))
            .setMessage(
                getString(
                    com.cleanspace.R.string.confirm_delete_message,
                    count,
                    FileSizeFormatter.formatBytes(size),
                )
            )
            .setPositiveButton(getString(com.cleanspace.R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    val res = viewModel.deleteSelectedFiles()

                    if (res.isSuccess) {
                        val freed = res.getOrDefault(0L)
                        if (freed > 0L) {
                            Toast.makeText(
                                this@CleanActivity,
                                getString(com.cleanspace.R.string.deleted_ok, FileSizeFormatter.formatBytes(freed)),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    } else {
                        val ex = res.exceptionOrNull()
                        if (ex is DeleteRequestException) {
                            try {
                                val request = IntentSenderRequest.Builder(ex.intentSender).build()
                                deletePermissionLauncher.launch(request)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    this@CleanActivity,
                                    "Не вдалося відкрити системне підтвердження видалення",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        } else {
                            val msg = ex?.message ?: "Delete failed"
                            Toast.makeText(this@CleanActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(com.cleanspace.R.string.cancel), null)
            .show()
    }
}
