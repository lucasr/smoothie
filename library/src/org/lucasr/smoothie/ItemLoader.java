package org.lucasr.smoothie;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.os.Handler;
import android.util.Log;
import android.view.View;

class ItemLoader {
    private static final String LOGTAG = "SmoothieItemLoader";
    private static final boolean ENABLE_LOGGING = false;
    
    private final ItemEngine mItemEngine;
    private final Handler mHandler;
    private final Map<View, ItemState> mItemStates;
    private final Map<Object, ItemRequest> mPrefetchRequests;
    private final ExecutorService mExecutorService;

    class ItemState {
        public boolean shouldLoadItem;
        public Object itemParams;
    }

    ItemLoader(ItemEngine itemEngine, Handler handler) {
        mItemEngine = itemEngine;
        mHandler = handler;
        mItemStates = Collections.synchronizedMap(new WeakHashMap<View, ItemState>());
        mPrefetchRequests = Collections.synchronizedMap(new WeakHashMap<Object, ItemRequest>());
        mExecutorService = Executors.newFixedThreadPool(2);
    }

    void displayItem(View itemView) {
        final Object itemParams = getItemState(itemView).itemParams;
        if (itemParams == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "No item params, bailing");
            }

            return;
        }

        ItemRequest request = mPrefetchRequests.get(itemParams);
        if (request == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "No prefetch request, creating new");
            }

            request = new ItemRequest(itemView, itemParams);
        } else {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "There's a pending prefetch request, reusing");
            }

            request.itemView = new SoftReference<View>(itemView);
        }

        mPrefetchRequests.remove(itemParams);

        Object item = mItemEngine.loadItemFromMemory(itemParams);
        if (item != null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item is prefetched, quickly displaying");
            }

            request.item = new SoftReference<Object>(item);
            mHandler.post(new DisplayItemRunnable(request));

            return;
        }

        request.loadItemTask = mExecutorService.submit(new LoadItemRunnable(request));
    }

    void preloadItem(Object itemParams) {
        if (mPrefetchRequests.containsKey(itemParams)) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Pending prefetch request, bailing");
            }

            return;
        }

        if (mItemEngine.isItemInMemory(itemParams)) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Image is in memory, bailing");
            }

            return;
        }

        ItemRequest request = new ItemRequest(itemParams);
        mPrefetchRequests.put(itemParams, request);

        request.loadItemTask = mExecutorService.submit(new LoadItemRunnable(request));
    }

    void clearPrefetchRequests() {
        for (Map.Entry<Object, ItemRequest> entry : mPrefetchRequests.entrySet()) {
            ItemRequest request = entry.getValue();
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Canceling prefetch request for pattern: " + request.itemParams);
            }

            if (request.loadItemTask != null) {
                request.loadItemTask.cancel(true);
            }
        }

        mPrefetchRequests.clear();
    }

    ItemState getItemState(View itemView) {
        ItemState itemState = mItemStates.get(itemView);

        if (itemState == null) {
            itemState = new ItemState();
            itemState.itemParams = null;
            itemState.shouldLoadItem = false;

            mItemStates.put(itemView, itemState);
        }

        return itemState;
    }

    private boolean itemViewReused(ItemRequest request) {
        if (request.itemView == null) {
            return false;
        }

        View itemView = request.itemView.get();
        if (itemView == null) {
            return true;
        }

        final Object itemParams = getItemState(itemView).itemParams;
        if (itemParams == null || !request.itemParams.equals(itemParams)) {
            return true;
        }

        return false;
    }

    private class ItemRequest {
        public SoftReference<View> itemView;
        public Object itemParams;
        public SoftReference<Object> item;
        public Future<?> loadItemTask;

        public ItemRequest(Object itemParams) {
            this.itemView = null;
            this.itemParams = itemParams;
            this.item = new SoftReference<Object>(null);
        }

        public ItemRequest(View itemView, Object itemParams) {
            this.itemView = new SoftReference<View>(itemView);
            this.itemParams = itemParams;
            this.item = new SoftReference<Object>(null);
        }
    }

    private class LoadItemRunnable implements Runnable {
        private final ItemRequest mRequest;

        public LoadItemRunnable(ItemRequest request) {
            mRequest = request;
        }

        public void run() {
            if (itemViewReused(mRequest)) {
                return;
            }

            mRequest.item = new SoftReference<Object>(mItemEngine.loadItem(mRequest.itemParams));

            if (itemViewReused(mRequest)) {
                return;
            }

            if (mRequest.itemView != null) {
                if (ENABLE_LOGGING) {
                    Log.d(LOGTAG, "Done loading image: " + mRequest.itemParams);
                }

                mHandler.post(new DisplayItemRunnable(mRequest));
            } else {
                if (ENABLE_LOGGING) {
                    Log.d(LOGTAG, "Done prefetching: " + mRequest.itemParams);
                }
            }
        }
    }

    private class DisplayItemRunnable implements Runnable {
        private final ItemRequest mRequest;

        public DisplayItemRunnable(ItemRequest request) {
            mRequest = request;
        }

        public void run() {
            View itemView = mRequest.itemView.get();

            if (itemViewReused(mRequest)) {
                if (itemView != null) {
                    getItemState(itemView).itemParams = null;
                }
                return;
            }

            Object item = mRequest.item.get();
            if (item != null) {
                mItemEngine.displayItem(itemView, item);
                if (itemView != null) {
                    getItemState(itemView).itemParams = null;
                }
            }
        }
    }
}
