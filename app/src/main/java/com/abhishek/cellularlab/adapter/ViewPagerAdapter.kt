package com.abhishek.cellularlab.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.abhishek.cellularlab.ui.ResultHistoryFragment
import com.abhishek.cellularlab.ui.RunTestFragment

// region: ViewPagerAdapter Definition

/**
 * Adapter for ViewPager2 to manage fragments for each tab/page.
 *
 * @param fa The FragmentActivity hosting the ViewPager2.
 */
class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    // region: Adapter Overrides

    /**
     * Returns the number of pages/tabs in the ViewPager.
     * Currently, there are 2 pages: RunTest and ResultHistory.
     */
    override fun getItemCount(): Int = 2

    /**
     * Creates and returns the Fragment associated with a specific position.
     * @param position The index of the page/tab.
     * @return The Fragment for the given position.
     * @throws IllegalStateException if the position is invalid.
     */
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RunTestFragment()         // First tab: Run Test
            1 -> ResultHistoryFragment()   // Second tab: Result History
            // 2 -> SavedProfilesFragment() // Uncomment to add a third tab in the future
            else -> throw IllegalStateException("Invalid tab index")
        }
    }
    // endregion

}
// endregion