/*
 * Copyright (C) 2012 Lucas Rocha
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
import android.widget.AdapterView;

/**
 * {@code ItemLoader} is responsible for loading and displaying items
 * in {@link AsyncListView} or {@link AsyncGridView}. This is the class
 * you should subclass to implement your app-specific item loading
 * and displaying logic.
 *
 * <p>You should use {@link SimpleItemLoader} if your
 * {@link AsyncListView} or {@link AsyncGridView} only needs one
 * asynchronous loading operation per item as it hides the unnecessary
 * multi-part elements of the API.</p>
 *
 * <h2>Usage</h2>
 * <p>ItemLoader must be subclassed to be used. The subclass will override four
 * methods: {@link #loadItemPart(Object, int)}, @{@link #loadItemPartFromMemory(Object, int)},
 * {@link #displayItemPart(View, Object, int, boolean)}, and
 * {@link #getItemParams(Adapter, int)}.</p>
 *
 * <p>Here is an example of subclassing:</p>
 * <pre>
 * public class YourItemLoader extends ItemLoader<Long, Bitmap> {
 *     private final Context mContext;
 *     private final LruCache<Long, Bitmap> mMemCache;
 *
 *     public YourItemLoader(Context context) {
 *         mContext = context;
 *
 *         mMemCache = new LruCache<Long, Bitmap>(1024) {
 *             @Override
 *             protected int sizeOf(Long id, Bitmap bitmap) {
 *                 return bitmap.getRowBytes() * bitmap.getHeight();
 *             }
 *         };
 *     }
 *
 *     @Override
 *     public Long getItemParams(Adapter adapter, int position) {
 *        Cursor c = (Cursor) adapter.getItem(position);
 *        return c.getLong(c.getColumnIndex(ImageColumns._ID));
 *     }
 *
 *     @Override
 *     public Bitmap loadItemPart(Long id, int itemPart) {
 *         Uri uri = Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
 *         Bitmap b = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
 *         if (b != null) {
 *             mMemCache.put(id, b);
 *         }
 *
 *         return b;
 *     }
 *
 *     @Override
 *     public Bitmap loadItemPartFromMemory(Long id, int itemPart) {
 *         return mMemCache.get(id);
 *     }
 *
 *     @Override
 *     public void displayItemPart(View itemView, Bitmap result, int itemPart, boolean fromMemory) {
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
 *     <li><code>Params</code>, the type of the parameters sent to {@link #loadItemPart(Object, int)}.</li>
 *     <li><code>Result</code>, the type of the result returned by {@link #loadItemPart(Object, int)}
 *     which will be sent to {@link #displayItemPart(View, Object, int, boolean)}.</li>
 * </ol>
 *
 * <h2>The 4 steps</h2>
 * <p>When an ItemLoader is in action, each item will go through 4 steps:</p>
 * <ol>
 *     <li>{@link #getItemParams(Adapter, int)}, invoked on the UI thread before the
 *     item is loaded. This step should return all the parameters necessary for
 *     loading the item. This is necessary to avoid touching the Adapter in a
 *     background thread.</li>
 *     <li>{@link #loadItemPartFromMemory(Object, int)}, invoked on the UI thread before actually
 *     loading the item. If the item part is already in memory, skip the next step and
 *     display the item part immediately in the last step.</li>
 *     <li>{@link #loadItemPart(Object, int)}, invoked on a background thread. This call
 *     should return the item data that needs to be loaded asynchronously such
 *     as images or other online data.</li>
 *     <li>{@link #displayItemPart(View, Object, int, boolean)}, invoked on the UI thread
 *     after the item part finishes loading.</li>
 * </ol>
 *
 * <h2>Multi-part items</h2>
 * <p>There are cases where the items of your {@link AsyncListView} or
 * {@link AsyncGridView} are composed by more than one part requiring multiple
 * asynchronous requests per item e.g. items composed by two images that have
 * to be downloaded from the cloud. These parts usually have different priorities
 * in the UI e.g. the main image have higher priority than the author's avatar
 * image in a list of photos.</p>
 *
 * <p>By default, {@link ItemLoader} handles single-part items but you can
 * make it handle multi-part items by overriding
 * {@link #getItemPartCount(Adapter, int, int)} and returning the number of parts
 * a given position in the Adapter has. Then you should handle the {@code itemPart}
 * argument in {@link #loadItemPartFromMemory(Object, int)},
 * {@link #loadItemPart(Object, int)}, and {@link #displayItemPart(View, Object, int, boolean)}
 * accordingly. These methods will be called once for each item part. The item parts
 * will have indexes starting from zero. e.g. for items with 2 parts, the part indexes
 * will be 0 and 1. The indexes also define the relative priority between parts. Smoothie
 * will load the part with index 0 for all visible items before loading part with index 1.</p>
 *
 * <p>Here is an example of {@link #loadItemPart(Object, int)} handling multi-part items:</p>
 * <pre>
 * private static final int PART_MAIN_IMAGE = 0;
 * private static final int PART_AVATAR = 1;
 *
 * private static class MyParams {
 *     public String imageUrl;
 *     public String avatarUrl;
 * }
 *
 * public Bitmap loadItemPart(MyParams params, int itemPart) {
 *     final String url;
 *     if (itemPart == PART_MAIN_IMAGE) {
 *          url = params.imageUrl;
 *     } else if (itemPart == PART_AVATAR) {
 *          url = params.avatarUrl;
 *     }
 *
 *     return loadBitmapFromUrl(url);
 * }
 * </pre>
 *
 * <p>In the example above, the item contains two parts with the same type ({@code Bitmap}).
 * If your item contains parts with different types, your {@link ItemLoader}'s {@code Result}
 * type will have to be compatible with both. e.g. you could use {@code Object} as your
 * {@code Result} type if that's the only way to achieve this.</p>
 *
 * <p>You can disable preloading on specific item parts by overriding
 * {@link #shouldPreloadItemPart(Adapter, int, int)}. Preloading is enabled for
 * all item parts by default.
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
 * <h2>Other implementation notes</h2>
 * <p>It's assumed that your implementation of {@link #loadItemPart(Object, int)}
 * will result in the item data being cached in memory on success. Which
 * means that a subsequent {@link #loadItemPartFromMemory(Object, int)} call will
 * return the previously loaded item. You can easily implement memory
 * caching using the Android support library's {@code LruCache}</p>
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
    private Map<String, ItemRequest<Params, Result>> mItemRequests;
    private ThreadPoolExecutor mExecutorService;

    static final class ItemState<Params> {
        public boolean shouldLoadItem;
        public Params itemParams;
        public int position;
    }

    void init(Handler handler, int threadPoolSize) {
        mHandler = handler;
        mItemStates = Collections.synchronizedMap(new WeakHashMap<View, ItemState<Params>>());
        mItemRequests = new ConcurrentHashMap<String, ItemRequest<Params, Result>>(8, 0.9f, 1);
        mExecutorService = new ItemsThreadPoolExecutor<Params, Result>(threadPoolSize, threadPoolSize, 60,
                TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
    }

    void performDisplayItem(View itemContainer, Adapter adapter, View itemView, long timestamp) {
        final ItemState<Params> itemState = getItemState(itemView);
        if (!itemState.shouldLoadItem) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item should not load, bailing: " + itemState.itemParams);
            }

            return;
        }

        final Params itemParams = itemState.itemParams;
        if (itemParams == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "No item params, bailing");
            }

            return;
        }

        final int position = itemState.position;
        if (position == AdapterView.INVALID_POSITION) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Undefined position, bailing");
            }

            return;
        }

        final int partCount = getItemPartCount(adapter, position);
        for (int itemPart = 0; itemPart < partCount; itemPart++) {
            performDisplayItemPart(itemContainer, itemView, itemState, itemPart, timestamp);
        }
    }

    private void performDisplayItemPart(View itemContainer, View itemView, ItemState<Params> itemState,
                                        int itemPart, long timestamp) {
        final int position = itemState.position;
        final Params itemParams = itemState.itemParams;

        final String id = generateItemRequestId(position, itemPart);
        ItemRequest<Params, Result> request = mItemRequests.get(id);
        if (request == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Display) No pending item request, creating new: " + itemParams);
            }

            // No existing item request, create a new one
            request = new ItemRequest<Params, Result>(id, itemContainer, itemView, itemParams, position, itemPart, timestamp);
            mItemRequests.put(id, request);
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

        Result result = loadItemPartFromMemory(itemParams, itemPart);
        if (result != null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item is preloaded, quickly displaying");
            }

            cancelItemRequest(position, itemPart);

            // The item is in memory, no need to asynchronously load it
            // Run the final item display routine straight away.
            request.result = new SoftReference<Result>(result);
            mHandler.post(new DisplayItemRunnable<Params, Result>(this, request, true));

            return;
        }

        request.loadItemTask = mExecutorService.submit(new LoadItemRunnable<Params, Result>(this, request));
    }

    void performLoadItem(View itemContainer, View itemView, Adapter adapter, int position, boolean shouldDisplayItem) {
        // Loader returned no parameters for the item, just bail
        final Params itemParams = getItemParams(adapter, position);
        if (itemParams == null) {
            return;
        }

        final ItemState<Params> itemState = getItemState(itemView);
        itemState.itemParams = itemParams;
        itemState.position = position;

        // Mark the view for loading
        itemState.shouldLoadItem = true;

        final int partCount = getItemPartCount(adapter, position);
        for (int itemPart = 0; itemPart < partCount; itemPart++) {
            if (shouldDisplayItem || isItemPartInMemory(itemParams, itemPart)) {
                performDisplayItemPart(itemContainer, itemView, itemState, itemPart, SystemClock.uptimeMillis());
            }
        }
    }

    void performPreloadItem(View itemContainer, Adapter adapter, int position, long timestamp) {
        final Params itemParams = getItemParams(adapter, position);
        if (itemParams == null) {
            return;
        }

        final int partCount = getItemPartCount(adapter, position);
        for (int itemPart = 0; itemPart < partCount; itemPart++) {
            if (!shouldPreloadItemPart(adapter, position, itemPart)) {
                continue;
            }

            performPreloadItemPart(itemContainer, itemParams, adapter, position,
                    itemPart, SystemClock.uptimeMillis());
        }
    }

    private void performPreloadItemPart(View itemContainer, Params itemParams, Adapter adapter, int position,
            int itemPart, long timestamp) {
        // If item is memory, just cancel any pending requests for
        // this item and return as the item has already been loaded.
        if (isItemPartInMemory(itemParams, itemPart)) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "Item is in memory, bailing: " + itemParams);
            }

            cancelItemRequest(position, itemPart);
            return;
        }

        final String id = generateItemRequestId(position, itemPart);
        ItemRequest<Params, Result> request = mItemRequests.get(id);
        if (request == null) {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Preload) No pending item request, creating new: " + id);
            }

            // No pending item preload request, create a new one
            request = new ItemRequest<Params, Result>(id, itemContainer, itemParams, position, itemPart, timestamp);
            mItemRequests.put(id, request);

            request.loadItemTask = mExecutorService.submit(new LoadItemRunnable<Params, Result>(this, request));
        } else {
            if (ENABLE_LOGGING) {
                Log.d(LOGTAG, "(Preload) There's a pending item request, reusing: " + id);
            }

            // There's a pending item request for these parameters, demote the
            // existing request with loader priority as it's just a preloading
            // request. See LoadItemFutureTask for details on request priorities.
            request.timestamp = timestamp;
            request.itemView = null;
        }
    }

    boolean isItemPartInMemory(Params itemParams, int itemPart) {
        return (loadItemPartFromMemory(itemParams, itemPart) != null);
    }

    void cancelObsoleteRequests(long timestamp) {
        for (Iterator<ItemRequest<Params, Result>> i = mItemRequests.values().iterator(); i.hasNext();) {
            final ItemRequest<Params, Result> request = i.next();

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

    void cancelRequestsForContainer(View itemContainer) {
        if (itemContainer == null) {
            throw new IllegalArgumentException("Null itemContainer in cancelRequestsForContainer");
        }

        for (Iterator<ItemRequest<Params, Result>> i = mItemRequests.values().iterator(); i.hasNext();) {
            final ItemRequest<Params, Result> request = i.next();
            final View requestContainer = request.itemContainer.get();

            if (requestContainer == itemContainer) {
                if (ENABLE_LOGGING) {
                    Log.d(LOGTAG, "Cancelling request for container: " + request.itemParams);
                }

                if (request.loadItemTask != null) {
                    request.loadItemTask.cancel(true);
                }

                i.remove();
            }
        }
    }

    private ItemState<Params> getItemState(View itemView) {
        ItemState<Params> itemState = mItemStates.get(itemView);

        if (itemState == null) {
            itemState = new ItemState<Params>();
            itemState.itemParams = null;
            itemState.shouldLoadItem = false;
            itemState.position = AdapterView.INVALID_POSITION;

            mItemStates.put(itemView, itemState);
        }

        return itemState;
    }

    private static String generateItemRequestId(int position, int itemPart) {
        return String.valueOf(position) + String.valueOf(itemPart);
    }

    private void cancelItemRequest(int position, int itemPart) {
        final String id = generateItemRequestId(position, itemPart);
        final ItemRequest<Params, Result> request = mItemRequests.get(id);
        if (request == null) {
            return;
        }

        mItemRequests.remove(id);
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
        final View itemView = request.itemView.get();
        if (itemView == null) {
            return true;
        }

        // If the parameters associated with the view doesn't match the ones
        // in the matching request, this means the view has been recycled to
        // display something else.
        final int position = getItemState(itemView).position;
        if (position == AdapterView.INVALID_POSITION || request.position != position) {
            return true;
        }

        return false;
    }

    /**
     * Override this method if your {@link ItemLoader} has to deal with
     * multi-part items.
     *
     * @param adapter - The {@link Adapter} associated with the target
     *        {@link AsyncListView} or {@link AsyncGridView}.
     * @param position - The position in the Adapter from which the
     *        number of parts should be retrieved.
     *
     * @return The number of parts the item in the given position contains.
     */
    public int getItemPartCount(Adapter adapter, int position) {
        return 1;
    }

    /**
     * Override this method if you want to disable preloading for specific
     * item parts.
     *
     * @param adapter - The {@link Adapter} associated with the target
     *        {@link AsyncListView} or {@link AsyncGridView}.
     * @param position - The position in the Adapter from which the
     *        number of parts should be retrieved.
     * @param itemPart - The item part for which preloading should be enabled
     *                   or disabled.
     *
     * @return Whether the part of the item in the given position should be
     *         preloaded or not.
     *
     * @see ItemManager.Builder#setPreloadItemsEnabled(boolean)
     */
    public boolean shouldPreloadItemPart(Adapter adapter, int position, int itemPart) {
        return true;
    }

    /**
     * Retrieves the necessary parameters to load the item's data. This
     * method is called in the UI thread.
     *
     * @param adapter - The {@link Adapter} associated with the target
     *        {@link AsyncListView} or {@link AsyncGridView}.
     * @param position - The position in the Adapter from which the
     *        parameters should be retrieved.
     *
     * @return The parameters necessary to load an item which will be
     *         passed to {@link #loadItemPart(Object, int)}.
     */
    public abstract Params getItemParams(Adapter adapter, int position);

    /**
     * Loads the item data. This method is called in a background thread.
     * Hence you can make blocking calls (I/O, heavy computing) in your
     * implementation.
     *
     * @param itemParams - The parameters generated by
     *        {@link #getItemParams(Adapter, int)}.
     * @param itemPart - The target item part to be loaded.
     *
     * @return The loaded item data.
     */
    public abstract Result loadItemPart(Params itemParams, int itemPart);

    /**
     * Attempts to load the item data from memory. This method is called
     * in the UI thread. In most implementations, this method will simply
     * query a memory cache using the item parameters as a key.
     *
     * @param itemParams - The parameters generated by
     *        {@link #getItemParams(Adapter, int)}
     * @param itemPart - The target item part to be loaded.
     *
     * @return The cached item data.
     */
    public abstract Result loadItemPartFromMemory(Params itemParams, int itemPart);

    /**
     * Displays the loaded item data in the target view. This method is called
     * in the UI thread.
     *
     * @param itemView - The target item view returned by your Adapter's
     *        {@link android.widget.Adapter #getView(int, View, android.view.ViewGroup)}
     *        implementation.
     * @param result - The item data loaded from {@link #loadItemPart(Object, int)} or
     *        {@link #loadItemPartFromMemory(Object)}.
     * @param itemPart - The target item part to be displayed.
     * @param fromMemory - {@code True} if the item data has been loaded from
     *        {@link #loadItemPartFromMemory(Object, int)}. {@code False} if it has been
     *        loaded from {@link #loadItemPart(Object, int)}. This argument is usually used
     *        to skip animations when displaying preloaded items.
     */
    public abstract void displayItemPart(View itemView, Result result, int itemPart, boolean fromMemory);

    private static final class ItemRequest<Params, Result> {
        public SoftReference<View> itemContainer;
        public SoftReference<View> itemView;
        public SoftReference<Result> result;
        public Future<?> loadItemTask;
        public Long timestamp;

        final public String id;
        final public Params itemParams;
        final public int position;
        final public Integer itemPart;

        public ItemRequest(String id, View itemContainer, Params itemParams, int position, int itemPart,
                long timestamp) {
            this(id, itemContainer, null, itemParams, position, itemPart, timestamp);
        }

        public ItemRequest(String id, View itemContainer, View itemView, Params itemParams, int position,
                int itemPart, long timestamp) {
            this.id = id;
            this.itemContainer = new SoftReference<View>(itemContainer);
            this.itemView = (itemView != null ? new SoftReference<View>(itemView) : null);
            this.itemParams = itemParams;
            this.position = position;
            this.itemPart = itemPart;
            this.result = null;
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
            // that are visible on screen. Parts with lower indexes have priority
            // over higher ones. Request priorities are dynamically updated as the
            // user scroll the list view. See performDisplayItem() and
            // performPreloadItem() for details.
            if (r1.itemView != null && r2.itemView == null) {
                return -1;
            } else if (r1.itemView == null && r2.itemView != null) {
                return 1;
            } else if (!r1.itemPart.equals(r2.itemPart)) {
                return r1.itemPart.compareTo(r2.itemPart);
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
                Log.d(LOGTAG, "Running: " + ItemLoader.generateItemRequestId(mRequest.position, mRequest.itemPart));
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mItemLoader.mItemRequests.remove(mRequest.id);

            if (mItemLoader.itemViewReused(mRequest)) {
                return;
            }

            Result result = mItemLoader.loadItemPart(mRequest.itemParams, mRequest.itemPart);
            mRequest.result = new SoftReference<Result>(result);

            // If itemView is not null, this is a request for an item
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
            if (mItemLoader.itemViewReused(mRequest)) {
                return;
            }

            // We should have set the result to a non-null value at this point
            if (mRequest.result == null) {
                throw new IllegalStateException("Result should not be null when displaying an item part");
            }

            // Simply bail if the view has been garbage collected
            final View itemView = mRequest.itemView.get();
            if (itemView == null) {
                return;
            }

            // Deliver the result to display the item part
            final Result result = mRequest.result.get();
            mItemLoader.displayItemPart(itemView, result, mRequest.itemPart, mFromMemory);
        }
    }
}
