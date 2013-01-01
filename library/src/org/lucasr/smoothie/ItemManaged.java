package org.lucasr.smoothie;

import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

class ItemManaged {
    private final AbsListView mAbsListView;
    private ItemManager mItemManager;

    private boolean mInstallingManager;

    private OnScrollListener mOnScrollListener;
    private OnTouchListener mOnTouchListener;
    private OnItemSelectedListener mOnItemSelectedListener;

    ItemManaged(AbsListView absListView) {
        mAbsListView = absListView;
        mItemManager = null;

        mInstallingManager = false;

        mOnScrollListener = null;
        mOnTouchListener = null;
        mOnItemSelectedListener = null;
    }

    boolean hasItemManager() {
        return (mItemManager != null);
    }

    void setItemManager(ItemManager itemManager) {
        if (mItemManager != null) {
            mItemManager.setItemManaged(null);
            mItemManager = null;
        }

        mInstallingManager = true;

        if (itemManager != null) {
            itemManager.setItemManaged(this);
        } else {
            mAbsListView.setOnScrollListener(mOnScrollListener);
            mAbsListView.setOnTouchListener(mOnTouchListener);
            mAbsListView.setOnItemSelectedListener(mOnItemSelectedListener);

            ListAdapter adapter = mAbsListView.getAdapter();
            if (adapter != null) {
                AsyncBaseAdapter asyncAdapter = (AsyncBaseAdapter) adapter;
                mAbsListView.setAdapter(asyncAdapter.getWrappedAdapter());
            }
        }

        mItemManager = itemManager;
        mInstallingManager = false;
    }

    AbsListView getAbsListView() {
        return mAbsListView;
    }

    OnScrollListener getOnScrollListener() {
        return mOnScrollListener;
    }

    void setOnScrollListener(OnScrollListener l) {
        if (mInstallingManager) {
            return;
        }

        mOnScrollListener = l;
    }

    OnTouchListener getOnTouchListener() {
        return mOnTouchListener;
    }

    void setOnTouchListener(OnTouchListener l) {
        if (mInstallingManager) {
            return;
        }

        mOnTouchListener = l;
    }

    OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    void setOnItemSelectedListener(OnItemSelectedListener l) {
        if (mInstallingManager) {
            return;
        }

        mOnItemSelectedListener = l;
    }

    ListAdapter wrapAdapter(ListAdapter adapter) {
        if (mItemManager != null) {
            adapter = new AsyncBaseAdapter(mItemManager, (BaseAdapter) adapter);
        }

        return adapter;
    }
}
