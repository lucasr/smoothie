package org.lucasr.smoothie;

import org.lucasr.smoothie.ItemLoader.ItemState;

import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.AdapterView;

public final class Smoothie {
    private static final int MESSAGE_UPDATE_ITEMS = 1;
    private static final int DELAY_SHOW_ITEMS = 550;

    private final AbsListView mList;

    private final ItemLoader mItemLoader;
    private final ItemEngine mItemEngine;
    private final Handler mHandler;

    private final boolean mPreloadItemsEnabled;
    private final int mPreloadItemsCount;

    private int mScrollState;
    private boolean mPendingItemsUpdate;
    private boolean mFingerUp;

    private Smoothie(AbsListView list, ItemEngine itemEngine, boolean preloadItemsEnabled,
            int preloadItemsCount, int threadPoolSize) {
        mList = list;

        mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
        mHandler = new ItemsListHandler();

        mItemLoader = new ItemLoader(itemEngine, mHandler, threadPoolSize);
        mItemEngine = itemEngine;

        mPreloadItemsEnabled = preloadItemsEnabled;
        mPreloadItemsCount = preloadItemsCount;

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

        if (mPreloadItemsEnabled) {
            mItemLoader.clearPreloadRequests();

            int lastFetchedPosition = mList.getFirstVisiblePosition() + count - 1;
            if (lastFetchedPosition > 0) {
                Adapter adapter = mList.getAdapter();
                final int adapterCount = adapter.getCount();

                for (int i = lastFetchedPosition;
                     i < lastFetchedPosition + mPreloadItemsCount && i < adapterCount;
                     i++) {
                    Object itemParams = mItemEngine.getPreloadItemParams(adapter, i);
                    if (itemParams != null) {
                        mItemLoader.preloadItem(itemParams);
                    }
                }
            }
        }

        mList.invalidate();
    }

    private void postUpdateItems() {
        Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_ITEMS,
                                             Smoothie.this);

        mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);
        mPendingItemsUpdate = true;

        mHandler.sendMessage(msg);
    }

    public final void loadItem(View itemView, Object itemParams) {
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

    public final static class Builder {
        private static final boolean DEFAULT_PRELOAD_ITEMS_ENABLED = false;
        private static final int DEFAULT_PRELOAD_ITEMS_COUNT = 4;
        private static final int DEFAULT_THREAD_POOL_SIZE = 2;

        private final AbsListView mAbsListView;
        private final ItemEngine mItemEngine;

        private boolean mPreloadItemsEnabled;
        private int mPreloadItemsCount;
        private int mThreadPoolSize;

        public Builder(AbsListView absListView, ItemEngine itemEngine) {
            mAbsListView = absListView;
            mItemEngine = itemEngine;

            mPreloadItemsEnabled = DEFAULT_PRELOAD_ITEMS_ENABLED;
            mPreloadItemsCount = DEFAULT_PRELOAD_ITEMS_COUNT;
            mThreadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        }

        public Builder setPreloadItemsEnabled(boolean preloadItemsEnabled) {
            mPreloadItemsEnabled = preloadItemsEnabled;
            return this;
        }

        public Builder setPreloadItemsCount(int preloadItemsCount) {
            mPreloadItemsCount = preloadItemsCount;
            return this;
        }

        public Builder setThreadPoolSize(int threadPoolSize) {
            mThreadPoolSize = threadPoolSize;
            return this;
        }

        public Smoothie build() {
            return new Smoothie(mAbsListView, mItemEngine, mPreloadItemsEnabled,
                    mPreloadItemsCount, mThreadPoolSize);
        }
    }
}
