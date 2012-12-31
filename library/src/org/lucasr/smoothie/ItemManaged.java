package org.lucasr.smoothie;

import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

class ItemManaged {
    private final AbsListView mAbsListView;
    private final DelegateListeners mListeners;
    private ItemManager mItemManager;

    ItemManaged(AbsListView absListView) {
        mAbsListView = absListView;
        mListeners = new DelegateListeners();
        mItemManager = null;
    }

    boolean hasItemManager() {
        return (mItemManager != null);
    }

    void setItemManager(ItemManager itemManager) {
        if (mItemManager != null) {
            mItemManager.setItemManaged(null);
            mItemManager = null;
        }

        if (itemManager != null) {
            itemManager.setItemManaged(this);
        } else {
            mAbsListView.setOnScrollListener(mListeners.getOnScrollListener());
            mAbsListView.setOnTouchListener(mListeners.getOnTouchListener());
            mAbsListView.setOnItemSelectedListener(mListeners.getOnItemSelectedListener());

            ListAdapter adapter = mAbsListView.getAdapter();
            if (adapter != null) {
                AsyncBaseAdapter asyncAdapter = (AsyncBaseAdapter) adapter;
                mAbsListView.setAdapter(asyncAdapter.getWrappedAdapter());
            }
        }

        mItemManager = itemManager;
    }

    AbsListView getAbsListView() {
        return mAbsListView;
    }

    DelegateListeners getListeners() {
        return mListeners;
    }

    void setOnScrollListener(OnScrollListener l) {
        if (mItemManager != null) {
            mListeners.setOnScrollListener(l);
        } else {
            mAbsListView.setOnScrollListener(l);
        }
    }

    void setOnTouchListener(OnTouchListener l) {
        if (mItemManager != null) {
            mListeners.setOnTouchListener(l);
        } else {
            mAbsListView.setOnTouchListener(l);
        }
    }

    void setOnItemSelectedListener(OnItemSelectedListener l) {
        if (mItemManager != null) {
            mListeners.setOnItemSelectedListener(l);
        } else {
            mAbsListView.setOnItemSelectedListener(l);
        }
    }

    ListAdapter wrapAdapter(ListAdapter adapter) {
        if (mItemManager != null) {
            adapter = new AsyncBaseAdapter(mItemManager, (BaseAdapter) adapter);
        }

        return adapter;
    }
}
