package org.lucasr.smoothie;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;
import android.widget.ListAdapter;

/**
 * <p>A {@link android.widget.GridView GridView} that can asynchronously load the
 * content of its items through an {@link ItemManager}.</p>
 *
 * <p>AsyncGridView behaves exactly like an ordinary
 * {@link android.widget.GridView GridView} when it has no {@link ItemManager}
 * set. Once an {@link ItemManager} is set, it will you use the associated
 * {@link ItemLoader} to asynchronously load the content of its items as the
 * the user scrolls the view.</p>
 *
 * <p>Use {@link #setItemManager(ItemManager)} to set an {@link ItemManager} on
 * the view. An example call:</p>
 *
 * <pre>
 * ItemManager.Builder builder = new ItemManager.Builder(new YourItemLoader());
 * builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
 * ItemManager itemManager = builder.build();
 *
 * AsyncGridView gridView = (AsyncGridView) findViewById(R.id.grid);
 * gridView.setItemManager(itemManager);
 * </pre>
 *
 * @author Lucas Rocha <lucasr@lucasr.org>
 */
public class AsyncGridView extends GridView implements AsyncAbsListView {
    private final ItemManaged mItemManaged;

    @SuppressWarnings("javadoc")
    public AsyncGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mItemManaged = new ItemManaged(this);
    }

    /**
     * Sets an {@link ItemManager} on the {@link AsyncGridView}. Once this is
     * set, the {@link AsyncGridView} will use its associated {@link ItemLoader}
     * to asynchronously load the content its items in a background thread.
     *
     * @param itemManager - {@link ItemManager} to associate with the view.
     */
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
