package com.cleanspace.presentation.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.cleanspace.databinding.ActivityBaseBinding
import com.cleanspace.databinding.ActivityDashboardBinding
import com.cleanspace.presentation.clean.CleanActivity
import com.cleanspace.presentation.clean.CleanActivityExtras
import com.cleanspace.presentation.common.BaseActivity
import com.cleanspace.presentation.scan.ScanActivity
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.cleanspace.domain.utils.FileSizeFormatter
import com.cleanspace.data.models.FileType

@AndroidEntryPoint
class DashboardActivity : BaseActivity(com.cleanspace.R.layout.activity_base) {

    private lateinit var baseBinding: ActivityBaseBinding
    private lateinit var binding: ActivityDashboardBinding

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        binding = ActivityDashboardBinding.inflate(layoutInflater, baseBinding.contentContainer, true)

        setupBottomNavigation(baseBinding.bottomNavigation)

        setupStoragePie()
        setupClicks()
        observeUi()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupClicks() {
        binding.btnStartScan.setOnClickListener { navigateToScan() }

        // Category cards
        binding.cardCache.setOnClickListener { navigateToCleanCategory(CATEGORY_PHOTO) }
        binding.cardDuplicates.setOnClickListener { navigateToDuplicates() }
        binding.cardTemporary.setOnClickListener { navigateToCleanCategory(CATEGORY_AUDIO_VIDEO) }
        binding.cardLarge.setOnClickListener { navigateToCleanCategory(FileType.CACHE.name) }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            viewModel.lastCleanupDate.collectLatest { date ->
                binding.tvLastCleanupValue.text = if (date == null) {
                    "Немає даних"
                } else {
                    val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk"))
                    df.format(Date(date))
                }
            }
        }

        lifecycleScope.launch {
            viewModel.totalFreedBytes.collectLatest { total ->
                binding.tvTotalFreed.text = getString(
                    com.cleanspace.R.string.dashboard_total_freed,
                    FileSizeFormatter.formatBytes(total),
                )
                // Keep dashboard charts/cards in sync right after successful cleanup writes.
                viewModel.refresh()
            }
        }

        lifecycleScope.launch {
            viewModel.cleaningCount.collectLatest { count ->
                binding.tvLastCleanupLabel.text = getString(
                    com.cleanspace.R.string.dashboard_last_cleanup_with_count,
                    count,
                )
            }
        }

        lifecycleScope.launch {
            viewModel.storageInfo.collectLatest { info ->
                updateStoragePie(info.usedBytes, info.freeBytes)
            }
        }

        lifecycleScope.launch {
            viewModel.categoryCards.collectLatest { map ->
                updateCategoryCards(map)
            }
        }
    }

    private fun setupStoragePie() {
        binding.pieStorage.apply {
            description = Description().apply { text = "" }
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            legend.isEnabled = true
        }
        // initial draw
        val info = viewModel.storageInfo.value
        updateStoragePie(info.usedBytes, info.freeBytes)
    }

    private fun updateStoragePie(usedBytes: Long, freeBytes: Long) {
        val used = usedBytes.coerceAtLeast(0)
        val free = freeBytes.coerceAtLeast(0)
        val total = (used + free).coerceAtLeast(1)

        val usedPct = (used.toFloat() * 100f) / total.toFloat()
        val freePct = 100f - usedPct

        val entries = listOf(
            PieEntry(usedPct, "Зайнято"),
            PieEntry(freePct, "Вільно"),
        )

        val set = PieDataSet(entries, "Пам'ять").apply {
            colors = listOf(
                android.graphics.Color.parseColor("#1976D2"),
                android.graphics.Color.parseColor("#D6E4FF"),
            )
            valueTextSize = 12f
        }

        binding.pieStorage.data = PieData(set)
        binding.pieStorage.invalidate()
    }

    private fun updateCategoryCards(map: Map<FileType, DashboardCategorySummary>) {
        fun format(type: FileType): String {
            val s = map[type]
            val count = s?.count ?: 0
            val bytes = s?.totalBytes ?: 0L
            return getString(
                com.cleanspace.R.string.dashboard_category_summary,
                count,
                FileSizeFormatter.formatBytes(bytes),
            )
        }

        binding.tvCardCacheSummary.text = format(FileType.MEDIA_IMAGES)
        binding.tvCardDuplicatesSummary.text = format(FileType.DUPLICATE)
        binding.tvCardTemporarySummary.text = format(FileType.MEDIA_AUDIO)
        binding.tvCardLargeSummary.text = format(FileType.CACHE)
    }

    private fun navigateToScan() {
        startActivity(Intent(this, ScanActivity::class.java))
    }

    private fun navigateToCleanCategory(category: String) {
        val i = Intent(this, CleanActivity::class.java)
        i.putExtra(EXTRA_CATEGORY, category)
        startActivity(i)
    }

    private fun navigateToDuplicates() {
        val i = Intent(this, CleanActivity::class.java)
        i.putExtra(CleanActivityExtras.EXTRA_MODE, CleanActivityExtras.ModeExtra.DUPLICATES.name)
        startActivity(i)
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val CATEGORY_PHOTO = "CATEGORY_PHOTO"
        const val CATEGORY_AUDIO_VIDEO = "CATEGORY_AUDIO_VIDEO"
    }
}
