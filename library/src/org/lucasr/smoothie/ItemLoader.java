package org.lucasr.smoothie;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

class ItemLoader {
    private static final String LOGTAG = "SmoothieItemLoader";
    private static final boolean ENABLE_LOGGING = false;

    private final ItemEngine mItemEngine;
    private final Handler mHandler;
    private final Map<View, ItemState> mItemStates;
    private final Map<Object, ItemRequest> mItemRequests;
    private final ThreadPoolExecutor mExecutorService;

    class ItemState {
        public boolean shouldLoadItem;
        public Object itemParams;
    }

    ItemLoader(ItemEngine itemEngine, Handler handler, int threadPoolSize) {
        mItemEngine = itemEngine;
        mHandler = handler;
        mItemStates = Collections.synchronizedMap(new WeakHashMap<View, ItemState>());
        mItemRequests = Collections.synchronizedMap(new WeakHashMap<Object, ItemRequest>());
        mExecutorService = new ItemsThreadPoolExecutor(threadPoolSize, threadPoolSize, 60,
                TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
    }

    void displayItem(View itemView) {
        final Object itemParams = getItemState(itemView).itemParams;
        if (ENABLE_LOGGING) {
            Log.d(LOGTAG, "displayItem called: " + itemParams);
        }

        if (itemParams == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "No item params, bailing: " + itemParams);
            }

            return;
        }

        ItemRequest request = mItemRequests.get(itemParams);
        if (request == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Display) No pending item request, creating new: " + itemParams);
            }

            request = new ItemRequest(itemView, itemParams);
            mItemRequests.put(itemParams, request);
        } else {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Display) There's a pending item request, reusing: " + itemParams);
            }

            request.timestamp = SystemClock.uptimeMillis();
            request.itemView = new SoftReference<View>(itemView);
        }

        Object item = mItemEngine.loadItemFromMemory(itemParams);
        if (item != null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item is preloaded, quickly displaying");
            }

            cancelItemRequest(itemParams);

            request.item = new SoftReference<Object>(item);
            mHandler.post(new DisplayItemRunnable(request, true));

            return;
        }

        request.loadItemTask = mExecutorService.submit(new LoadItemRunnable(request));
    }

    long preloadItem(Object itemParams) {
        if (ENABLE_LOGGING) {
            Log.d(LOGTAG, "preloadItem called: " + itemParams);
        }

        long timestamp = SystemClock.uptimeMillis();

        if (mItemEngine.isItemInMemory(itemParams)) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item is in memory, bailing: " + itemParams);
            }

            cancelItemRequest(itemParams);
            return timestamp;
        }

        ItemRequest request = mItemRequests.get(itemParams);
        if (request == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Preload) No pending item request, creating new: " + itemParams);
            }

            request = new ItemRequest(itemParams, timestamp);
            mItemRequests.put(itemParams, request);

            request.loadItemTask = mExecutorService.submit(new LoadItemRunnable(request));
        } else {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Preload) There's a pending item request, reusing: " + itemParams);
            }

            request.timestamp = timestamp;
            request.itemView = null;
        }

        return timestamp;
    }

    void cancelObsoleteRequests(long timestamp) {
        for (Iterator<ItemRequest> i = mItemRequests.values().iterator(); i.hasNext();) {
            ItemRequest request = i.next();

            if (request.timestamp < timestamp) {
                if (ENABLE_LOGGING) {
                    Log.d(LOGTAG, "Cancelling obsolete request: " + request.itemParams);
                }

                if (request.loadItemTask != null) {
                    request.loadItemTask.cancel(true);
                }

                i.remove();
            }
        }

        mExecutorService.purge();
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

    private void cancelItemRequest(Object itemParams) {
        ItemRequest request = mItemRequests.get(itemParams);
        if (request == null) {
            return;
        }

        mItemRequests.remove(itemParams);
        if (request.loadItemTask != null) {
            request.loadItemTask.cancel(true);
        }
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
        public Long timestamp;
        public Future<?> loadItemTask;

        public ItemRequest(Object itemParams, long timestamp) {
            this.itemView = null;
            this.itemParams = itemParams;
            this.item = new SoftReference<Object>(null);
            this.timestamp = timestamp;
            this.loadItemTask = null;
        }

        public ItemRequest(View itemView, Object itemParams) {
            this.itemView = new SoftReference<View>(itemView);
            this.itemParams = itemParams;
            this.item = new SoftReference<Object>(null);
            this.timestamp = SystemClock.uptimeMillis();
            this.loadItemTask = null;
        }
    }

    private class ItemsThreadPoolExecutor extends ThreadPoolExecutor {
        public ItemsThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                long keepAliveTime, TimeUnit unit,
                BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        @Override
        public Future<?> submit(Runnable task) {
            if (task == null) {
                throw new NullPointerException();
            }

            LoadItemRunnable r = (LoadItemRunnable) task;
            LoadItemFutureTask ftask = new LoadItemFutureTask(r.mRequest);
            execute(ftask);

            return ftask;
        }
    }

    private class LoadItemFutureTask extends FutureTask<LoadItemRunnable>
            implements Comparable<LoadItemFutureTask> {
        private final ItemRequest mRequest;

        public LoadItemFutureTask(ItemRequest request) {
            super(new LoadItemRunnable(request), null);
            mRequest = request;
        }

        @Override
        public int compareTo(LoadItemFutureTask another) {
            if (mRequest.itemView != null && another.mRequest.itemView == null) {
                return -1;
            } else if (mRequest.itemView == null && another.mRequest.itemView != null) {
                return 1;
            } else {
                return mRequest.timestamp.compareTo(another.mRequest.timestamp);
            }
        }
    }

    private class LoadItemRunnable implements Runnable {
        private final ItemRequest mRequest;

        public LoadItemRunnable(ItemRequest request) {
            mRequest = request;
        }

        @Override
        public void run() {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Running: " + mRequest.itemParams);
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mItemRequests.remove(mRequest.itemParams);

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

                mHandler.post(new DisplayItemRunnable(mRequest, false));
            } else {
                if (ENABLE_LOGGING) {
                    Log.d(LOGTAG, "Done preloading: " + mRequest.itemParams);
                }
            }
        }
    }

    private class DisplayItemRunnable implements Runnable {
        private final ItemRequest mRequest;
        private final boolean mFromMemory;

        public DisplayItemRunnable(ItemRequest request, boolean fromMemory) {
            mRequest = request;
            mFromMemory = fromMemory;
        }

        @Override
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
                mItemEngine.displayItem(itemView, item, mFromMemory);
                if (itemView != null) {
                    getItemState(itemView).itemParams = null;
                }
            }
        }
    }
}
