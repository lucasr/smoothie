package org.lucasr.smoothie;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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

/**
 * ItemLoader is responsible for loading and displaying items
 * in {@link AsyncListView} or {@link AsyncGridView}. This is the class
 * you should subclass to implement your app-specific item loading
 * and displaying logic.
 *
 * <h2>Usage</h2>
 * <p>ItemLoader must be subclassed to be used. The subclass will override at least
 * three methods: {@link #loadItem(Object)}, {@link #displayItem(View, Object, boolean)},
 * and {@link #getItemParams(Adapter, int)}. You can override more methods if you want
 * to have custom item loading from memory ({@link #loadItemFromMemory(Object)},
 * {@link #getItemSizeInMemory(Object, Object)}) and more.</p>
 *
 * <p>Here is an example of subclassing:</p>
 * <pre>
 * public class YourItemLoader extends ItemLoader<Long, Bitmap> {
 *     private final Context mContext;
 *
 *     public YourItemLoader(Context context) {
 *         mContext = context;
 *     }
 *
 *     @Override
 *     public Long getItemParams(Adapter adapter, int position) {
 *        Cursor c = (Cursor) adapter.getItem(position);
 *        return c.getLong(c.getColumnIndex(ImageColumns._ID));
 *     }
 *
 *     @Override
 *     public Bitmap loadItem(Long id) {
 *         Uri uri = Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
 *         return MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
 *     }
 *
 *     @Override
 *     public void displayItem(View itemView, Bitmap result, boolean fromMemory) {
 *         ImageView image = (ImageView) itemView.findViewById(R.id.image);
 *         image.setImageBitmap(result);
 *     }
 * }
 * </pre>
 *
 * <p>The ItemLoader should be passed to {@link ItemManager.Builder Builder} constructor:</p>
 * <pre>
 * ItemManager.Builder = new ItemManager.Builder(new YourItemLoader(context));
 * </pre>
 *
 * <h2>ItemLoader's generic types</h2>
 * <p>The two types used by an ItemLoader are the following:</p>
 * <ol>
 *     <li><code>Params</code>, the type of the parameters sent to {@link #loadItem(Object)}.</li>
 *     <li><code>Result</code>, the type of the result returned by {@link #loadItem(Object)}
 *     which will be sent to {@link #displayItem(View, Object, boolean)}.</li>
 * </ol>
 *
 * <h2>The 4 steps</h2>
 * <p>When an ItemLoader is in action, each item will go through 4 steps:</p>
 * <ol>
 *     <li>{@link #getItemParams(Adapter, int)}, invoked on the UI thread before the
 *     item is loaded. This step should return all the parameters necessary for
 *     loading the item. This is necessary to avoid touching the Adapter in a
 *     background thread.</li>
 *     <li>{@link #loadItemFromMemory(Object)}, invoked on the UI thread before actually
 *     loading the item. If the item is already in memory, skip the next step and
 *     display the item immediately in the last step.</li>
 *     <li>{@link #loadItem(Object)}, invoked on a background thread. This call
 *     should return the item data that needs to be loaded asynchronously such
 *     as images or online data.</li>
 *     <li>{@link #displayItem(View, Object, boolean)}, invoked on the UI thread
 *     after the item finishes loading.</li>
 * </ol>
 *
 * <h2>ItemLoader and Adapter</h2>
 * <p>Once you have an {@link ItemManager} set in an {@link AsyncListView} or
 * {@link AsyncGridView}, your Adapter will behave exactly the same. In your
 * {@link android.widget.Adapter #getView(int, View, android.view.ViewGroup)},
 * you should display all the elements that are directly available from the
 * Adapter's backing data. e.g. the backing data structure or database
 * Cursor.</p>
 *
 * <p>The ItemLoader should handle the item data that needs to be loaded
 * asynchronously in a background thread. e.g. downloading images from
 * the cloud or loading files from disk.</p>
 *
 * <p>It's assumed that your
 * {@link android.widget.Adapter #getView(int, View, android.view.ViewGroup)}
 * will reset the item view to placeholder state regarding the data that
 * the ItemLoader will load. For example, if your item has images that will
 * be loaded asynchronously, your adapter should set the placeholder state
 * in the target ImageView that will be shown until the image is actually
 * loaded.</p>
 *
 * @param <Params> - The parameters for loading an item.
 * @param <Result> - The result of the item loading operation.
 *
 * @author Lucas Rocha <lucasr@lucasr.org>
 */
public abstract class ItemLoader<Params, Result> {
    private static final String LOGTAG = "SmoothieItemLoader";
    private static final boolean ENABLE_LOGGING = false;

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
        mItemRequests = new ConcurrentHashMap<Params, ItemRequest<Params, Result>>(8, 0.9f, 1);
        mExecutorService = new ItemsThreadPoolExecutor<Params, Result>(threadPoolSize, threadPoolSize, 60,
                TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
    }

    void performDisplayItem(View itemView, long timestamp) {
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

            // No existing item request, create a new one
            request = new ItemRequest<Params, Result>(itemView, itemParams, timestamp);
            mItemRequests.put(itemParams, request);
        } else {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Display) There's a pending item request, reusing: " + itemParams);
            }

            // There's a pending item request for these parameters, promote the
            // existing request with higher priority. See LoadItemFutureTask
            // for details on request priorities.
            request.timestamp = timestamp;
            request.itemView = new SoftReference<View>(itemView);
        }

        // We're actually running this item request, make sure
        // this item is not requested again.
        itemState.shouldLoadItem = false;

        Result result = loadItemFromMemory(itemParams);
        if (result != null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item is preloaded, quickly displaying");
            }

            cancelItemRequest(itemParams);

            // The item is in memory, no need to asynchronously load it
            // Run the final item display routine straight away.
            request.result = new SoftReference<Result>(result);
            mHandler.post(new DisplayItemRunnable<Params, Result>(this, request, true));

            return;
        }

        request.loadItemTask = mExecutorService.submit(new LoadItemRunnable<Params, Result>(this, request));
    }

    void performLoadItem(View itemView, Adapter adapter, int position, boolean shouldDisplayItem) {
        // Loader returned no parameters for the item, just bail
        Params itemParams = getItemParams(adapter, position);
        if (itemParams == null) {
            return;
        }

        ItemState<Params> itemState = getItemState(itemView);
        itemState.itemParams = itemParams;

        // Mark the view for loading
        itemState.shouldLoadItem = true;

        if (shouldDisplayItem || isItemInMemory(itemParams)) {
            performDisplayItem(itemView, SystemClock.uptimeMillis());
        }
    }

    void performPreloadItem(Adapter adapter, int position, long timestamp) {
        Params itemParams = getItemParams(adapter, position);
        if (itemParams == null) {
            return;
        }

        // If item is memory, just cancel any pending requests for
        // this item and return as the item has already been loaded.
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

            // No pending item preload request, create a new one
            request = new ItemRequest<Params, Result>(itemParams, timestamp);
            mItemRequests.put(itemParams, request);

            request.loadItemTask = mExecutorService.submit(new LoadItemRunnable<Params, Result>(this, request));
        } else {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Preload) There's a pending item request, reusing: " + itemParams);
            }

            // There's a pending item request for these parameters, demote the
            // existing request with loader priority as it's just a preloading
            // request. See LoadItemFutureTask for details on request priorities.
            request.timestamp = timestamp;
            request.itemView = null;
        }
    }

    boolean isItemInMemory(Params itemParams) {
        return (loadItemFromMemory(itemParams) != null);
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

        // Actually remove any cancelled tasks from the queue
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
        // If itemView is null, this means this is a preload request
        // with no target view to display. No view to be possibly recycled
        // in this case.
        if (request.itemView == null) {
            return false;
        }

        // If the request's soft reference to the view is now null, this means
        // the view has been disposed from memory. Just bail.
        View itemView = request.itemView.get();
        if (itemView == null) {
            return true;
        }

        // If the parameters associated with the view doesn't match the ones
        // in the matching request, this means the view has been recycled to
        // display something else.
        final Params itemParams = getItemState(itemView).itemParams;
        if (itemParams == null || !request.itemParams.equals(itemParams)) {
            return true;
        }

        return false;
    }

    public abstract Params getItemParams(Adapter adapter, int position);

    public abstract Result loadItem(Params itemParams);

    public abstract Result loadItemFromMemory(Params itemParams);

    public abstract void displayItem(View itemView, Result result, boolean fromMemory);

    private static final class ItemRequest<Params, Result> {
        public SoftReference<View> itemView;
        public Params itemParams;
        public SoftReference<Result> result;
        public Long timestamp;
        public Future<?> loadItemTask;

        public ItemRequest(Params itemParams, long timestamp) {
            this.itemView = null;
            this.itemParams = itemParams;
            this.result = new SoftReference<Result>(null);
            this.timestamp = timestamp;
            this.loadItemTask = null;
        }

        public ItemRequest(View itemView, Params itemParams, long timestamp) {
            this.itemView = new SoftReference<View>(itemView);
            this.itemParams = itemParams;
            this.result = new SoftReference<Result>(null);
            this.timestamp = timestamp;
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

            // A null itemView here means that the requests has no target view
            // to display the loaded content, which means it's a preload request.
            // Preloading requests always have lower priority than requests for items
            // that are visible on screen. Request priorities are dynamically updated
            // as the user scroll the list view. See performDisplayItem() and
            // performPreloadItem() for details.
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

            Result result = mItemLoader.loadItem(mRequest.itemParams);
            mRequest.result = new SoftReference<Result>(result);

            // If itemView is not null, this is a requests for an item
            // that is currently visible on screen.
            if (mRequest.itemView != null) {
                if (ENABLE_LOGGING) {
                    Log.d(LOGTAG, "Done loading image: " + mRequest.itemParams);
                }

                if (mItemLoader.itemViewReused(mRequest)) {
                    return;
                }

                // Item is now loaded, run the display routine
                mItemLoader.mHandler.post(new DisplayItemRunnable<Params, Result>(mItemLoader, mRequest, false));
            } else {
                // This is just a preload request, we're done here
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
