package com.abhishek.cellularlab.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.abhishek.cellularlab.ui.ResultHistoryFragment
import com.abhishek.cellularlab.ui.RunTestFragment
import com.abhishek.cellularlab.ui.SavedProfilesFragment

class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RunTestFragment()
            1 -> ResultHistoryFragment()
            2 -> SavedProfilesFragment()
            else -> throw IllegalStateException("Invalid tab index")
        }
    }
}
