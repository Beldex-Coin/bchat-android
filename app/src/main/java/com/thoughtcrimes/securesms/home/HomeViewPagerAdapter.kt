package com.thoughtcrimes.securesms.home

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomeViewPagerAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {
    private val mFragmentList: MutableList<Fragment> = ArrayList()
    private val mFragmentTitleList: MutableList<String> = ArrayList()

    fun getTabTitle(position : Int): String{
        return mFragmentTitleList[position]
    }

    fun addFragment(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    override fun getItemCount(): Int {
        return mFragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return mFragmentList[position]
    }
}