package com.cleanspace.presentation.scan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.cleanspace.R
import com.cleanspace.databinding.ActivityBaseBinding
import com.cleanspace.databinding.ActivityScanBinding
import com.cleanspace.presentation.clean.CleanActivity
import com.cleanspace.presentation.common.BaseActivity
import com.cleanspace.utils.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScanActivity : BaseActivity(com.cleanspace.R.layout.activity_base) {

    private lateinit var baseBinding: ActivityBaseBinding
    private lateinit var binding: ActivityScanBinding

    private val viewModel: ScanViewModel by viewModels()

    private lateinit var permissionHelper: PermissionHelper

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.all { it }
        if (granted) {
            viewModel.startScan()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        binding = ActivityScanBinding.inflate(layoutInflater, baseBinding.contentContainer, true)
        setupBottomNavigation(baseBinding.bottomNavigation)

        binding.btnCancel.setOnClickListener { viewModel.cancelScan() }

        lifecycleScope.launch {
            viewModel.progress.collect { progress ->
                binding.progressBar.progress = progress.percent
                binding.tvPercent.text = "${progress.percent}%"
                binding.tvCurrentDir.text = getString(R.string.current_directory, progress.currentDirectory)
                binding.tvFilesFound.text = getString(R.string.files_found, progress.filesFound)
            }
        }

        lifecycleScope.launch {
            viewModel.scanState.collect { state ->
                when (state) {
                    is ScanViewModel.ScanState.Completed -> {
                        // Force UI to show completion state
                        binding.progressBar.progress = 100
                        binding.tvPercent.text = "100%"
                        binding.tvCurrentDir.text = getString(R.string.scan_completed)

                        val scanned = ScanResultHolder.lastScan
                        if (scanned == null || scanned.items.isEmpty()) {
                            Toast.makeText(
                                this@ScanActivity,
                                "Сканування завершено, але файлів не знайдено. Перевір дозволи та SAF папки.",
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            Toast.makeText(this@ScanActivity, getString(R.string.scan_completed), Toast.LENGTH_SHORT).show()
                        }

                        startActivity(Intent(this@ScanActivity, CleanActivity::class.java))
                        finish()
                    }

                    is ScanViewModel.ScanState.Cancelled -> {
                        Toast.makeText(this@ScanActivity, getString(R.string.scan_cancelled), Toast.LENGTH_SHORT).show()
                    }

                    is ScanViewModel.ScanState.Error -> {
                        Toast.makeText(
                            this@ScanActivity,
                            getString(R.string.scan_error, state.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }

                    else -> Unit
                }
            }
        }

        permissionHelper = PermissionHelper(this)
    }

    override fun onStart() {
        super.onStart()
        if (permissionHelper.checkPermissions()) {
            viewModel.startScan()
        } else {
            permissionHelper.showPermissionExplanationDialog {
                permissionHelper.requestPermissions(permissionLauncher)
            }
        }
    }
}
