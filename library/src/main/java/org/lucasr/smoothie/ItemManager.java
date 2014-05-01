/*
 * Copyright (C) 2008 Romain Guy, 2012 Lucas Rocha
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
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

/**
 * <p>ItemManager ties the user interaction (scroll, touch,
 * item selection, etc) on the target {@link AsyncAbsListView}
 * with its associated {@link ItemLoader}. Once an ItemManager
 * is set on an {@link AsyncAbsListView}, the {@link ItemLoader}
 * hooks will be called as needed to asynchronously load and
 * display items.</p>
 *
 * <p>ItemManager instances should be created using its
 * {@link ItemManager.Builder Builder} class. An example call:</p>
 *
 * <pre>
 * ItemManager.Builder builder = new ItemManager.Builder();
 * builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
 *
 * ItemManager itemManager = builder.build();
 * </pre>
 *
 * @author Lucas Rocha <lucasr@lucasr.org>
 */
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

        final AbsListView absListView = mManaged.getAbsListView();
        final ListAdapter adapter = mManaged.getAdapter();
        mPendingItemsUpdate = false;

        // Nothing worth doing if the adapter is null, just bail.
        if (adapter == null) {
            return;
        }

        long timestamp = SystemClock.uptimeMillis();

        // Perform display routine on each of the visible items
        // in the list view.
        final int count = absListView.getChildCount();
        for (int i = 0; i < count; i++) {
            final View itemView = absListView.getChildAt(i);
            mItemLoader.performDisplayItem(absListView, adapter, itemView, timestamp++);
        }

        if (mPreloadItemsEnabled) {
            // Preload items beyond the visible viewport with a lower
            // request priority. See ItemLoader for details.
            final int lastFetchedPosition = absListView.getLastVisiblePosition() + 1;
            if (lastFetchedPosition > 0) {
                final int adapterCount = adapter.getCount();

                for (int i = lastFetchedPosition;
                     i < lastFetchedPosition + mPreloadItemsCount && i < adapterCount;
                     i++) {
                    mItemLoader.performPreloadItem(absListView, adapter, i, timestamp++);
                }
            }
        }

        // Cancel all pending item requests that haven't got their timestamps
        // updated in this round. In practice, this means requests for items
        // that are not relevant anymore for the current scroll position.
        mItemLoader.cancelObsoleteRequests(mLastPreloadTimestamp);
        mLastPreloadTimestamp = timestamp;

        absListView.invalidate();
    }

    void postUpdateItems() {
        final Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_ITEMS,
                                                   ItemManager.this);

        mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);
        mPendingItemsUpdate = true;

        mHandler.sendMessage(msg);
    }

    void setItemManaged(ItemManaged itemManaged) {
        mManaged = itemManaged;

        if (mManaged != null) {
            final AbsListView absListView = mManaged.getAbsListView();

            // These listeners will still run the current list view
            // listeners as delegates. See ItemManaged.
            absListView.setOnScrollListener(new ScrollManager());
            absListView.setOnTouchListener(new FingerTracker());
            absListView.setOnItemSelectedListener(new SelectionTracker());
        }
    }

    void cancelAllRequests() {
        if (mManaged == null) {
            throw new IllegalStateException("Cannot cancel requests with no managed view");
        }

        mItemLoader.cancelRequestsForContainer(mManaged.getAbsListView());
    }

    void loadItem(View itemContainer, View itemView, int position) {
        final AbsListView absListView = mManaged.getAbsListView();
        final ListAdapter adapter = mManaged.getAdapter();

        final boolean shouldDisplayItem =
                (mScrollState != OnScrollListener.SCROLL_STATE_FLING && !mPendingItemsUpdate);

        // This runs on each Adapter.getView() call. Will only trigger an
        // actual item loading request if the view is not being flung or finger
        // is down scrolling the view.
        mItemLoader.performLoadItem(itemContainer, itemView, adapter, position, shouldDisplayItem);
    }

    private class ScrollManager implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            boolean stoppedFling = mScrollState == SCROLL_STATE_FLING &&
                                   scrollState != SCROLL_STATE_FLING;

            // Stopped flinging, trigger a round of item updates (after
            // a small delay, just in case).
            if (stoppedFling) {
                final Message msg = mHandler.obtainMessage(MESSAGE_UPDATE_ITEMS,
                                                           ItemManager.this);

                mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);

                final int delay = (mFingerUp ? 0 : DELAY_SHOW_ITEMS);
                mHandler.sendMessageDelayed(msg, delay);

                mPendingItemsUpdate = true;
            } else if (scrollState == SCROLL_STATE_FLING) {
                mPendingItemsUpdate = false;
                mHandler.removeMessages(MESSAGE_UPDATE_ITEMS);
            }

            mScrollState = scrollState;

            final OnScrollListener l = mManaged.getOnScrollListener();
            if (l != null) {
                l.onScrollStateChanged(view, scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            final OnScrollListener l = mManaged.getOnScrollListener();
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

            // If finger is up and view is not flinging, trigger a new round
            // of item updates.
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

            final OnItemSelectedListener l = mManaged.getOnItemSelectedListener();
            if (l != null) {
                l.onItemSelected(adapterView, view, position, id);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            final OnItemSelectedListener l = mManaged.getOnItemSelectedListener();
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
                    final ItemManager smoothie = (ItemManager) msg.obj;
                    smoothie.updateItems();
                    break;
            }
        }
    }


    /**
    * Builder class for {@link ItemManager}.
    *
    * @author Lucas Rocha <lucasr@lucasr.org>
    */
    public final static class Builder {
        private static final boolean DEFAULT_PRELOAD_ITEMS_ENABLED = false;
        private static final int DEFAULT_PRELOAD_ITEMS_COUNT = 4;
        private static final int DEFAULT_THREAD_POOL_SIZE = 2;

        private final ItemLoader<?, ?> mItemLoader;

        private boolean mPreloadItemsEnabled;
        private int mPreloadItemsCount;
        private int mThreadPoolSize;

        /**
         * @param itemLoader - Your {@link ItemLoader} subclass implementation.
         */
        public Builder(ItemLoader<?, ?> itemLoader) {
            mItemLoader = itemLoader;

            mPreloadItemsEnabled = DEFAULT_PRELOAD_ITEMS_ENABLED;
            mPreloadItemsCount = DEFAULT_PRELOAD_ITEMS_COUNT;
            mThreadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        }

        /**
         * Sets whether offscreen item preloading should be enabled.
         * Defaults to {@value #DEFAULT_PRELOAD_ITEMS_ENABLED}.
         *
         * @param preloadItemsEnabled - {@code true} to enable offscreen item
         *        preloading.
         *
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setPreloadItemsEnabled(boolean preloadItemsEnabled) {
            mPreloadItemsEnabled = preloadItemsEnabled;
            return this;
        }

        /**
         * Sets the maximum number of offscreen items to be preloaded after
         * the visible items finish loading. Defaults to
         * {@value #DEFAULT_PRELOAD_ITEMS_COUNT}.
         *
         * @param preloadItemsCount - Number of offscreen items to preload.
         *
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setPreloadItemsCount(int preloadItemsCount) {
            mPreloadItemsCount = preloadItemsCount;
            return this;
        }

        /**
         * Sets the number of background threads available to asynchronously
         * load items in the target view. Defaults to
         * {@value #DEFAULT_THREAD_POOL_SIZE}.
         *
         * @param threadPoolSize - Number of background threads to be available
         *        in the pool.
         *
         * @return This Builder object to allow for chaining of calls to set
         *         methods.
         */
        public Builder setThreadPoolSize(int threadPoolSize) {
            mThreadPoolSize = threadPoolSize;
            return this;
        }

        /**
         * @return A new {@link ItemManager} created with the arguments
         *         supplied to this builder.
         */
        public ItemManager build() {
            return new ItemManager(mItemLoader, mPreloadItemsEnabled,
                    mPreloadItemsCount, mThreadPoolSize);
        }
    }
}
