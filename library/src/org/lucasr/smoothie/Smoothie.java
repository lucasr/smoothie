package org.lucasr.smoothie;

import org.lucasr.smoothie.ItemLoader.ItemState;

import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;

public class Smoothie {
    private static final int MESSAGE_UPDATE_ITEMS = 1;
    private static final int DELAY_SHOW_ITEMS = 550;

    private AbsListView mList;

    private ItemLoader mItemLoader;
    private ItemEngine mItemEngine;
    private Handler mHandler;

    private int mScrollState;
    private boolean mPendingItemsUpdate;
    private boolean mFingerUp;

    public Smoothie(AbsListView list, ItemEngine itemEngine) {
        mList = list;

        mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
        mHandler = new ItemsListHandler();
        mItemLoader = new ItemLoader(itemEngine, mHandler);
        mItemEngine = itemEngine;

        mList.setOnScrollListener(new ScrollManager());
        mList.setOnTouchListener(new FingerTracker());
        mList.setOnItemSelectedListener(new SelectionTracker());
    }

    private void updateItems() {
        mPendingItemsUpdate = false;

        final int count = mList.getChildCount();

        for (int i = 0; i < count; i++) {
            final View itemView = mList.getChildAt(i);
            final ItemState itemState = mItemLoader.getItemState(itemView);

            if (itemState.itemParams != null && itemState.shouldLoadItem) {
               mItemLoader.displayItem(itemView);
               itemState.shouldLoadItem = false;
            }
        }

        mItemLoader.clearPrefetchRequests();
/*
        int lastFetchedPosition = mList.getFirstVisiblePosition() + count - 1;
        if (lastFetchedPosition > 0) {
            Adapter adapter = mList.getAdapter();
            for (int i = lastFetchedPosition; i < lastFetchedPosition + 10 && i < adapter.getCount(); i++) {
                mItemLoader.preloadItem(adapter.getItem(i));
            }
        }
*/

        mList.invalidate();
    }

    private void postUpdateItems() {
        Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_ITEMS,
                                             Smoothie.this);

        mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);
        mPendingItemsUpdate = true;

        mHandler.sendMessage(msg);
    }

    public void loadItem(View itemView, Object itemParams) {
        ItemState itemState = mItemLoader.getItemState(itemView);

        itemState.itemParams = itemParams;
        boolean itemInMemory = mItemEngine.isItemInMemory(itemParams);

        if (!itemInMemory) {
            mItemEngine.resetItem(itemView);
        }

        boolean shouldDisplayThumbnail =
                (mScrollState != OnScrollListener.SCROLL_STATE_FLING &&
                 !mPendingItemsUpdate) || itemInMemory;

        if (shouldDisplayThumbnail) {
            mItemLoader.displayItem(itemView);
            itemState.shouldLoadItem = false;
        } else {
            itemState.shouldLoadItem = true;
        }
    }

    private class ScrollManager implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            boolean stoppedFling = mScrollState == SCROLL_STATE_FLING &&
                                   scrollState != SCROLL_STATE_FLING;

            if (stoppedFling) {
                final Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_ITEMS,
                                                           Smoothie.this);

                mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);

                int delay = (mFingerUp ? 0 : DELAY_SHOW_ITEMS);
                mHandler.sendMessageDelayed(msg, delay);

                mPendingItemsUpdate = true;
            } else if (scrollState == SCROLL_STATE_FLING) {
                mPendingItemsUpdate = false;
                mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);
            }

            mScrollState = scrollState;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
        }
    }

    private class FingerTracker implements OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final int action = event.getAction();

            mFingerUp = (action == MotionEvent.ACTION_UP ||
                         action == MotionEvent.ACTION_CANCEL);

            if (mFingerUp && mScrollState != OnScrollListener.SCROLL_STATE_FLING) {
                postUpdateItems();
            }

            return false;
        }
    }

    private class SelectionTracker implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
                postUpdateItems();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    private static class ItemsListHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_ITEMS:
                    Smoothie smoothie = (Smoothie) msg.obj;
                    smoothie.updateItems();
                    break;
            }
        }
    }
}
