/*
 * Copyright (C) 2012 Lucas Rocha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lucasr.smoothie;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.WrapperListAdapter;

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
            setAdapterOnView(wrapAdapter(itemManager, mWrappedAdapter));
        } else {
            // Restore the listeners set on the view before the item
            // manager was installed.
            mAbsListView.setOnScrollListener(mOnScrollListener);
            mAbsListView.setOnTouchListener(mOnTouchListener);
            mAbsListView.setOnItemSelectedListener(mOnItemSelectedListener);

            // Remove wrapper adapter and re-apply the original one
            setAdapterOnView(mWrappedAdapter);
        }

        mItemManager = itemManager;
        mInstallingManager = false;

        triggerUpdate();
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

    void cancelAllRequests() {
        if (mItemManager != null) {
            mItemManager.cancelAllRequests();
        }
    }

    ListAdapter getAdapter() {
        final ListAdapter adapter = mAbsListView.getAdapter();
        if (adapter instanceof WrapperListAdapter) {
            WrapperListAdapter wrapperAdapter = (WrapperListAdapter) adapter;
            return wrapperAdapter.getWrappedAdapter();
        }

        return adapter;
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

    @TargetApi(11)
    void setAdapterOnView(ListAdapter adapter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mAbsListView.setAdapter(adapter);
        } else if (mAbsListView instanceof ListView) {
            ((ListView) mAbsListView).setAdapter(adapter);
        } else if (mAbsListView instanceof GridView) {
            ((GridView) mAbsListView).setAdapter(adapter);
        }
    }

    void triggerUpdate() {
        if (hasItemManager() && mWrappedAdapter != null) {
            mItemManager.postUpdateItems();
        }
    }
}
