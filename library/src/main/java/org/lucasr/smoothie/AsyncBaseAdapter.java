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

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

class AsyncBaseAdapter extends BaseAdapter {
    private final ItemManager mItemManager;
    private final BaseAdapter mWrappedAdapter;

    AsyncBaseAdapter(ItemManager itemManager, BaseAdapter wrappedAdapter) {
        mItemManager = itemManager;
        mWrappedAdapter = wrappedAdapter;
    }

    ListAdapter getWrappedAdapter() {
        return mWrappedAdapter;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mWrappedAdapter.areAllItemsEnabled();
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return mWrappedAdapter.getDropDownView(position, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return mWrappedAdapter.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return mWrappedAdapter.getViewTypeCount();
    }

    @Override
    public boolean hasStableIds() {
        return mWrappedAdapter.hasStableIds();
    }

    @Override
    public boolean isEmpty() {
        return mWrappedAdapter.isEmpty();
    }

    @Override
    public boolean isEnabled(int position) {
        return mWrappedAdapter.isEnabled(position);
    }

    @Override
    public void notifyDataSetChanged() {
        mWrappedAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mWrappedAdapter.notifyDataSetInvalidated();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mWrappedAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mWrappedAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public int getCount() {
        return mWrappedAdapter.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mWrappedAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mWrappedAdapter.getItemId(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = mWrappedAdapter.getView(position, convertView, parent);
        mItemManager.loadItem(parent, v, position);
        return v;
    }
}
