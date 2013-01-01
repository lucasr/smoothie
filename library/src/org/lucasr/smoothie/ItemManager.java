package org.lucasr.smoothie;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

public final class ItemManager {
    private static final int MESSAGE_UPDATE_ITEMS = 1;
    private static final int DELAY_SHOW_ITEMS = 550;

    private ItemManaged mManaged;

    private final ItemLoader<?, ?> mItemLoader;
    private final Handler mHandler;

    private final boolean mPreloadItemsEnabled;
    private final int mPreloadItemsCount;
    private long mLastPreloadTimestamp;

    private int mScrollState;
    private boolean mPendingItemsUpdate;
    private boolean mFingerUp;

    private ItemManager(ItemLoader<?, ?> itemLoader, boolean preloadItemsEnabled,
            int preloadItemsCount, int threadPoolSize) {
        mManaged = null;

        mHandler = new ItemsListHandler();
        mItemLoader = itemLoader;
        mItemLoader.init(mHandler, threadPoolSize);

        mPreloadItemsEnabled = preloadItemsEnabled;
        mPreloadItemsCount = preloadItemsCount;
        mLastPreloadTimestamp = SystemClock.uptimeMillis();

        mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    }

    private void updateItems() {
        if (mManaged == null) {
            return;
        }

        AbsListView absListView = mManaged.getAbsListView();
        mPendingItemsUpdate = false;

        final int count = absListView.getChildCount();

        for (int i = 0; i < count; i++) {
            final View itemView = absListView.getChildAt(i);
            mItemLoader.performDisplayItem(itemView);
        }

        if (mPreloadItemsEnabled) {
            int lastFetchedPosition = absListView.getFirstVisiblePosition() + count;
            if (lastFetchedPosition > 0) {
                Adapter adapter = absListView.getAdapter();
                final int adapterCount = adapter.getCount();

                for (int i = lastFetchedPosition;
                     i < lastFetchedPosition + mPreloadItemsCount && i < adapterCount;
                     i++) {
                    mItemLoader.performPreloadItem(adapter, i);
                }
            }
        }

        mItemLoader.cancelObsoleteRequests(mLastPreloadTimestamp);
        mLastPreloadTimestamp = SystemClock.uptimeMillis();

        absListView.invalidate();
    }

    private void postUpdateItems() {
        Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_ITEMS,
                                             ItemManager.this);

        mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);
        mPendingItemsUpdate = true;

        mHandler.sendMessage(msg);
    }

    void setItemManaged(ItemManaged itemManaged) {
        mManaged = itemManaged;

        if (mManaged != null) {
            AbsListView absListView = mManaged.getAbsListView();
            absListView.setOnScrollListener(new ScrollManager());
            absListView.setOnTouchListener(new FingerTracker());
            absListView.setOnItemSelectedListener(new SelectionTracker());
        }
    }

    void loadItem(View itemView, int position) {
        AbsListView absListView = mManaged.getAbsListView();
        Adapter adapter = absListView.getAdapter();

        boolean shouldDisplayItem =
                (mScrollState != OnScrollListener.SCROLL_STATE_FLING && !mPendingItemsUpdate);

        mItemLoader.performLoadItem(itemView, adapter, position, shouldDisplayItem);
    }

    private class ScrollManager implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            boolean stoppedFling = mScrollState == SCROLL_STATE_FLING &&
                                   scrollState != SCROLL_STATE_FLING;

            if (stoppedFling) {
                final Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_ITEMS,
                                                           ItemManager.this);

                mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);

                int delay = (mFingerUp ? 0 : DELAY_SHOW_ITEMS);
                mHandler.sendMessageDelayed(msg, delay);

                mPendingItemsUpdate = true;
            } else if (scrollState == SCROLL_STATE_FLING) {
                mPendingItemsUpdate = false;
                mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);
            }

            mScrollState = scrollState;

            OnScrollListener l = mManaged.getOnScrollListener();
            if (l != null) {
                l.onScrollStateChanged(view, scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            OnScrollListener l = mManaged.getOnScrollListener();
            if (l != null) {
                l.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            }
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

            OnTouchListener l = mManaged.getOnTouchListener();
            if (l != null) {
                return l.onTouch(view, event);
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

            OnItemSelectedListener l = mManaged.getOnItemSelectedListener();
            if (l != null) {
                l.onItemSelected(adapterView, view, position, id);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            OnItemSelectedListener l = mManaged.getOnItemSelectedListener();
            if (l != null) {
                l.onNothingSelected(adapterView);
            }
        }
    }

    private static class ItemsListHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_ITEMS:
                    ItemManager smoothie = (ItemManager) msg.obj;
                    smoothie.updateItems();
                    break;
            }
        }
    }

    public final static class Builder {
        private static final boolean DEFAULT_PRELOAD_ITEMS_ENABLED = false;
        private static final int DEFAULT_PRELOAD_ITEMS_COUNT = 4;
        private static final int DEFAULT_THREAD_POOL_SIZE = 2;

        private final ItemLoader<?, ?> mItemLoader;

        private boolean mPreloadItemsEnabled;
        private int mPreloadItemsCount;
        private int mThreadPoolSize;

        public Builder(ItemLoader<?, ?> itemLoader) {
            mItemLoader = itemLoader;

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

        public ItemManager build() {
            return new ItemManager(mItemLoader, mPreloadItemsEnabled,
                    mPreloadItemsCount, mThreadPoolSize);
        }
    }
}
