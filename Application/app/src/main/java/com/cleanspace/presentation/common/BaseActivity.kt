package com.cleanspace.presentation.common

import android.content.Intent
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.cleanspace.R
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity(@LayoutRes contentLayoutId: Int) : AppCompatActivity(contentLayoutId) {

    protected fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is com.cleanspace.presentation.dashboard.DashboardActivity) {
                        startActivity(Intent(this, com.cleanspace.presentation.dashboard.DashboardActivity::class.java))
                        finish()
                    }
                    true
                }

                R.id.nav_scan -> {
                    if (this !is com.cleanspace.presentation.scan.ScanActivity) {
                        startActivity(Intent(this, com.cleanspace.presentation.scan.ScanActivity::class.java))
                    }
                    true
                }

                R.id.nav_clean -> {
                    if (this !is com.cleanspace.presentation.clean.CleanActivity) {
                        startActivity(Intent(this, com.cleanspace.presentation.clean.CleanActivity::class.java))
                    }
                    true
                }

                R.id.nav_stats -> {
                    if (this !is com.cleanspace.presentation.stats.StatsActivity) {
                        startActivity(Intent(this, com.cleanspace.presentation.stats.StatsActivity::class.java))
                    }
                    true
                }

                R.id.nav_settings -> {
                    if (this !is com.cleanspace.presentation.settings.SettingsActivity) {
                        startActivity(Intent(this, com.cleanspace.presentation.settings.SettingsActivity::class.java))
                    }
                    true
                }

                else -> false
            }
        }

        bottomNavigation.selectedItemId = when (this) {
            is com.cleanspace.presentation.dashboard.DashboardActivity -> R.id.nav_home
            is com.cleanspace.presentation.scan.ScanActivity -> R.id.nav_scan
            is com.cleanspace.presentation.clean.CleanActivity -> R.id.nav_clean
            is com.cleanspace.presentation.stats.StatsActivity -> R.id.nav_stats
            is com.cleanspace.presentation.settings.SettingsActivity -> R.id.nav_settings
            else -> R.id.nav_home
        }
    }
}

