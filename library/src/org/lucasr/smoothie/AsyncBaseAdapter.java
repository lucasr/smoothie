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
        mItemManager.loadItem(v, position);
        return v;
    }
}
