package com.abhishek.cellularlab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.abhishek.cellularlab.adapter.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var versionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        viewPager = findViewById(R.id.viewPager)

        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        versionTextView = findViewById(R.id.appVersionText)

        versionTextView.text = "v${packageManager.getPackageInfo(packageName, 0).versionName}"

        tabLayout = findViewById(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Run Test"
                1 -> "History"
//                2 -> "Test Profiles"
                else -> ""
            }
        }.attach()

        // <- Set up Bottom Navigation
//        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
//
//        bottomNav.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.tab_run_test -> viewPager.currentItem = 0
//                R.id.tab_history -> viewPager.currentItem = 1
//                R.id.tab_settings -> viewPager.currentItem = 2
//            }
//            true
//        }
//
//        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                bottomNav.menu.getItem(position).isChecked = true
//            }
//        })
        // Bottom Navigation->

    }

    // region Version & Social Links
    fun onVersionClick(view: View) {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_social_links, null)

        val dialog = AlertDialog.Builder(this).setTitle("Connect with Abhishek")
            .setView(dialogView)
            .setNegativeButton("Close", null).create()

        dialogView.findViewById<ImageView>(R.id.githubIcon).setOnClickListener {
            openUrl("https://github.com/Abhi5h3k")
        }

        dialogView.findViewById<ImageView>(R.id.linkedinIcon).setOnClickListener {
            openUrl("https://www.linkedin.com/in/abhi5h3k/")
        }

        dialogView.findViewById<ImageView>(R.id.shareIcon).setOnClickListener {
            shareApp()
        }

        dialog.show()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Check out CellularLab 🚀")
            putExtra(
                Intent.EXTRA_TEXT,
                "📱 CellularLab is a powerful iPerf3-based network tester for Android!\n" + "✅ TCP/UDP\n✅ Smart ramp-up\n✅ Logs & auto testing\n\n" + "👉 Download from GitHub: https://github.com/Abhi5h3k/CellularLab/releases"
            )
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
    // endregion
}
