package com.cleanspace.presentation.settings

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cleanspace.data.storage.SafAccessManager
import com.cleanspace.databinding.ActivityBaseBinding
import com.cleanspace.databinding.ActivitySettingsBinding
import com.cleanspace.presentation.common.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : BaseActivity(com.cleanspace.R.layout.activity_base) {

    private lateinit var baseBinding: ActivityBaseBinding
    private lateinit var binding: ActivitySettingsBinding

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var adapter: WhitelistAdapter

    private val openTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            SafAccessManager.takePersistablePermission(this, uri)
            viewModel.addSafTree(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        binding = ActivitySettingsBinding.inflate(layoutInflater, baseBinding.contentContainer, true)
        setupBottomNavigation(baseBinding.bottomNavigation)

        setupWhitelist()
        binding.btnAddSafFolder.setOnClickListener { openTreeLauncher.launch(null) }
    }

    private fun setupWhitelist() {
        adapter = WhitelistAdapter(
            onToggle = { entry -> viewModel.toggle(entry) },
            onRemove = { entry -> viewModel.remove(entry) },
        )

        binding.rvWhitelist.layoutManager = LinearLayoutManager(this)
        binding.rvWhitelist.adapter = adapter

        lifecycleScope.launch {
            viewModel.whitelistDirectories.collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }
}
