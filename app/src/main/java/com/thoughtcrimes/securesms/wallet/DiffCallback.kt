package com.thoughtcrimes.securesms.wallet

import androidx.recyclerview.widget.DiffUtil

abstract class DiffCallback<T>(protected val mOldList: List<T>, protected val mNewList: List<T>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return mOldList.size
    }

    override fun getNewListSize(): Int {
        return mNewList.size
    }

    abstract override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
    abstract override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
}