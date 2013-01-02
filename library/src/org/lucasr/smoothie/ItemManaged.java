package org.lucasr.smoothie;

import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

class ItemManaged {
    private final AbsListView mAbsListView;
    private ListAdapter mWrappedAdapter;
    private ItemManager mItemManager;

    private boolean mInstallingManager;

    private OnScrollListener mOnScrollListener;
    private OnTouchListener mOnTouchListener;
    private OnItemSelectedListener mOnItemSelectedListener;

    ItemManaged(AbsListView absListView) {
        mAbsListView = absListView;
        mWrappedAdapter = null;
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
        // Ensure the whatever current manager is detached
        // from this managed component.
        if (mItemManager != null) {
            mItemManager.setItemManaged(null);
            mItemManager = null;
        }

        // This is to avoid holding a reference to ItemManager's
        // listeners here while installing the new manager.
        mInstallingManager = true;

        if (itemManager != null) {
            // It's important that mItemManager is null at this point so
            // that its listeners are set properly.
            itemManager.setItemManaged(this);

            // Make sure that we wrap whatever adapter has been set
            // before the item manager was installed.
            mAbsListView.setAdapter(wrapAdapter(itemManager, mWrappedAdapter));
        } else {
            // Restore the listeners set on the view before the item
            // manager was installed.
            mAbsListView.setOnScrollListener(mOnScrollListener);
            mAbsListView.setOnTouchListener(mOnTouchListener);
            mAbsListView.setOnItemSelectedListener(mOnItemSelectedListener);

            // Remove wrapper adapter and re-apply the original one
            mAbsListView.setAdapter(mWrappedAdapter);
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

    ListAdapter getWrappedAdapter() {
        return mWrappedAdapter;
    }

    ListAdapter wrapAdapter(ListAdapter adapter) {
        return wrapAdapter(mItemManager, adapter);
    }

    ListAdapter wrapAdapter(ItemManager itemManager, ListAdapter adapter) {
        mWrappedAdapter = adapter;

        if (itemManager != null && adapter != null) {
            adapter = new AsyncBaseAdapter(itemManager, (BaseAdapter) adapter);
        }

        return adapter;
    }
}
