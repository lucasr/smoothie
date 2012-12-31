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
import android.widget.Adapter;

public abstract class ItemLoader<Params, Result> {
    private static final String LOGTAG = "SmoothieItemLoader";
    private static final boolean ENABLE_LOGGING = true;

    private Handler mHandler;
    private Map<View, ItemState<Params>> mItemStates;
    private Map<Params, ItemRequest<Params, Result>> mItemRequests;
    private ThreadPoolExecutor mExecutorService;

    static final class ItemState<Params> {
        public boolean shouldLoadItem;
        public Params itemParams;
    }

    void init(Handler handler, int threadPoolSize) {
        mHandler = handler;
        mItemStates = Collections.synchronizedMap(new WeakHashMap<View, ItemState<Params>>());
        mItemRequests = Collections.synchronizedMap(new WeakHashMap<Params, ItemRequest<Params, Result>>());
        mExecutorService = new ItemsThreadPoolExecutor<Params, Result>(threadPoolSize, threadPoolSize, 60,
                TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
    }

    void performDisplayItem(View itemView) {
        ItemState<Params> itemState = getItemState(itemView);
        if (!itemState.shouldLoadItem) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item should not load, bailing: " + itemState.itemParams);
            }

            return;
        }

        Params itemParams = itemState.itemParams;
        if (itemParams == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "No item params, bailing: " + itemParams);
            }

            return;
        }

        ItemRequest<Params, Result> request = mItemRequests.get(itemParams);
        if (request == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Display) No pending item request, creating new: " + itemParams);
            }

            request = new ItemRequest<Params, Result>(itemView, itemParams);
            mItemRequests.put(itemParams, request);
        } else {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Display) There's a pending item request, reusing: " + itemParams);
            }

            request.timestamp = SystemClock.uptimeMillis();
            request.itemView = new SoftReference<View>(itemView);
        }

        itemState.shouldLoadItem = false;

        Result result = loadItemFromMemory(itemParams);
        if (result != null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item is preloaded, quickly displaying");
            }

            cancelItemRequest(itemParams);

            request.result = new SoftReference<Result>(result);
            mHandler.post(new DisplayItemRunnable<Params, Result>(this, request, true));

            return;
        }

        request.loadItemTask = mExecutorService.submit(new LoadItemRunnable<Params, Result>(this, request));
    }

    void performLoadItem(View itemView, Adapter adapter, int position, boolean shouldDisplayItem) {
        Params itemParams = getItemParams(adapter, position);
        if (itemParams == null) {
            return;
        }

        ItemState<Params> itemState = getItemState(itemView);
        itemState.itemParams = itemParams;

        boolean itemInMemory = isItemInMemory(itemParams);
        if (!itemInMemory) {
            resetItem(itemView);
        }

        itemState.shouldLoadItem = true;

        if (shouldDisplayItem || itemInMemory) {
            performDisplayItem(itemView);
        }
    }

    void performPreloadItem(Adapter adapter, int position) {
        Params itemParams = getItemParams(adapter, position);
        if (itemParams == null) {
            return;
        }

        if (isItemInMemory(itemParams)) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item is in memory, bailing: " + itemParams);
            }

            cancelItemRequest(itemParams);
            return;
        }

        ItemRequest<Params, Result> request = mItemRequests.get(itemParams);
        if (request == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Preload) No pending item request, creating new: " + itemParams);
            }

            request = new ItemRequest<Params, Result>(itemParams);
            mItemRequests.put(itemParams, request);

            request.loadItemTask = mExecutorService.submit(new LoadItemRunnable<Params, Result>(this, request));
        } else {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Preload) There's a pending item request, reusing: " + itemParams);
            }

            request.timestamp = SystemClock.uptimeMillis();
            request.itemView = null;
        }
    }

    void cancelObsoleteRequests(long timestamp) {
        for (Iterator<ItemRequest<Params, Result>> i = mItemRequests.values().iterator(); i.hasNext();) {
            ItemRequest<Params, Result> request = i.next();

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

    private ItemState<Params> getItemState(View itemView) {
        ItemState<Params> itemState = mItemStates.get(itemView);

        if (itemState == null) {
            itemState = new ItemState<Params>();
            itemState.itemParams = null;
            itemState.shouldLoadItem = false;

            mItemStates.put(itemView, itemState);
        }

        return itemState;
    }

    private void cancelItemRequest(Params itemParams) {
        ItemRequest<Params, Result> request = mItemRequests.get(itemParams);
        if (request == null) {
            return;
        }

        mItemRequests.remove(itemParams);
        if (request.loadItemTask != null) {
            request.loadItemTask.cancel(true);
        }
    }

    private boolean itemViewReused(ItemRequest<Params, Result> request) {
        if (request.itemView == null) {
            return false;
        }

        View itemView = request.itemView.get();
        if (itemView == null) {
            return true;
        }

        final Params itemParams = getItemState(itemView).itemParams;
        if (itemParams == null || !request.itemParams.equals(itemParams)) {
            return true;
        }

        return false;
    }

    public void resetItem(View itemView) {
    }

    public boolean isItemInMemory(Params itemParams) {
        return false;
    }

    public Result loadItemFromMemory(Params itemParams) {
        return null;
    }

    public Result preloadItem(Params itemParams) {
        return loadItem(itemParams);
    }

    public abstract Params getItemParams(Adapter adapter, int position);

    public abstract Result loadItem(Params itemParams);

    public abstract void displayItem(View itemView, Result result, boolean fromMemory);

    private static final class ItemRequest<Params, Result> {
        public SoftReference<View> itemView;
        public Params itemParams;
        public SoftReference<Result> result;
        public Long timestamp;
        public Future<?> loadItemTask;

        public ItemRequest(Params itemParams) {
            this.itemView = null;
            this.itemParams = itemParams;
            this.result = new SoftReference<Result>(null);
            this.timestamp = SystemClock.uptimeMillis();
            this.loadItemTask = null;
        }

        public ItemRequest(View itemView, Params itemParams) {
            this.itemView = new SoftReference<View>(itemView);
            this.itemParams = itemParams;
            this.result = new SoftReference<Result>(null);
            this.timestamp = SystemClock.uptimeMillis();
            this.loadItemTask = null;
        }
    }

    private static final class ItemsThreadPoolExecutor<Params, Result> extends ThreadPoolExecutor {
        public ItemsThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        @Override
        public Future<?> submit(Runnable task) {
            if (task == null) {
                throw new NullPointerException();
            }

            @SuppressWarnings("unchecked")
            LoadItemFutureTask<Params, Result> ftask =
                    new LoadItemFutureTask<Params, Result>((LoadItemRunnable<Params, Result>) task);

            execute(ftask);

            return ftask;
        }
    }

    private static final class LoadItemFutureTask<Params, Result> extends FutureTask<LoadItemRunnable<Params, Result>>
            implements Comparable<LoadItemFutureTask<Params, Result>> {
        private final LoadItemRunnable<Params, Result> mRunnable;

        public LoadItemFutureTask(LoadItemRunnable<Params, Result> runnable) {
            super(runnable, null);
            mRunnable = runnable;
        }

        @Override
        public int compareTo(LoadItemFutureTask<Params, Result> another) {
            ItemRequest<Params, Result> r1 = mRunnable.getItemRequest();
            ItemRequest<Params, Result> r2 = another.mRunnable.getItemRequest();

            if (r1.itemView != null && r2.itemView == null) {
                return -1;
            } else if (r1.itemView == null && r2.itemView != null) {
                return 1;
            } else {
                return r1.timestamp.compareTo(r2.timestamp);
            }
        }
    }

    private static final class LoadItemRunnable<Params, Result> implements Runnable {
        private final ItemLoader<Params, Result> mItemLoader;
        private final ItemRequest<Params, Result> mRequest;

        public LoadItemRunnable(ItemLoader<Params, Result> itemLoader, ItemRequest<Params, Result> request) {
            mItemLoader = itemLoader;
            mRequest = request;
        }

        public ItemRequest<Params, Result> getItemRequest() {
            return mRequest;
        }

        @Override
        public void run() {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Running: " + mRequest.itemParams);
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mItemLoader.mItemRequests.remove(mRequest.itemParams);

            if (mItemLoader.itemViewReused(mRequest)) {
                return;
            }

            if (mRequest.itemView != null) {
                Result result = mItemLoader.loadItem(mRequest.itemParams);
                mRequest.result = new SoftReference<Result>(result);

                if (ENABLE_LOGGING) {
                    Log.d(LOGTAG, "Done loading image: " + mRequest.itemParams);
                }

                if (mItemLoader.itemViewReused(mRequest)) {
                    return;
                }

                mItemLoader.mHandler.post(new DisplayItemRunnable<Params, Result>(mItemLoader, mRequest, false));
            } else {
                Result result = mItemLoader.preloadItem(mRequest.itemParams);
                mRequest.result = new SoftReference<Result>(result);

                if (ENABLE_LOGGING) {
                    Log.d(LOGTAG, "Done preloading: " + mRequest.itemParams);
                }
            }
        }
    }

    private static final class DisplayItemRunnable<Params, Result> implements Runnable {
        private final ItemLoader<Params, Result> mItemLoader;
        private final ItemRequest<Params, Result> mRequest;
        private final boolean mFromMemory;

        public DisplayItemRunnable(ItemLoader<Params, Result> itemLoader,
                ItemRequest<Params, Result> request, boolean fromMemory) {
            mItemLoader = itemLoader;
            mRequest = request;
            mFromMemory = fromMemory;
        }

        @Override
        public void run() {
            View itemView = mRequest.itemView.get();

            if (mItemLoader.itemViewReused(mRequest)) {
                if (itemView != null) {
                    mItemLoader.getItemState(itemView).itemParams = null;
                }

                return;
            }

            Result result = mRequest.result.get();
            if (result != null) {
                mItemLoader.displayItem(itemView, result, mFromMemory);
                if (itemView != null) {
                    mItemLoader.getItemState(itemView).itemParams = null;
                }
            }
        }
    }
}
