package org.lucasr.smoothie;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;
import android.widget.ListAdapter;

public class AsyncGridView extends GridView implements AsyncAbsListView {
    private final ItemManaged mItemManaged;

    public AsyncGridView(Context context, AttributeSet attrs) {
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
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        mItemManaged.setOnTouchListener(l);
    }

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener l) {
        mItemManaged.setOnItemSelectedListener(l);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(mItemManaged.wrapAdapter(adapter));
    }
}
