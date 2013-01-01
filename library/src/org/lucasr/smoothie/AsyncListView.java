package org.lucasr.smoothie;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListAdapter;
import android.widget.ListView;

public class AsyncListView extends ListView implements AsyncAbsListView {
    private final ItemManaged mItemManaged;

    public AsyncListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mItemManaged = new ItemManaged(this);
    }

    @Override
    public void setItemManager(ItemManager itemManager) {
        mItemManaged.setItemManager(itemManager);
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mItemManaged.setOnScrollListener(l);
        if (!mItemManaged.hasItemManager()) {
            super.setOnScrollListener(l);
        }
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        mItemManaged.setOnTouchListener(l);
        if (!mItemManaged.hasItemManager()) {
            super.setOnTouchListener(l);
        }
    }

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener l) {
        mItemManaged.setOnItemSelectedListener(l);
        if (!mItemManaged.hasItemManager()) {
            super.setOnItemSelectedListener(l);
        }
    }

    @Override
    public ListAdapter getAdapter() {
        if (mItemManaged != null) {
            return mItemManaged.getWrappedAdapter();
        } else {
            return super.getAdapter();
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(mItemManaged.wrapAdapter(adapter));
    }
}
