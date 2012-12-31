package org.lucasr.smoothie.samples.bitmapcache;

import org.lucasr.smoothie.ItemLoader;
import org.lucasr.smoothie.samples.bitmapcache.PatternsListAdapter.ViewHolder;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapWrapper;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.widget.Adapter;

public class PatternsListLoader extends ItemLoader<String, CacheableBitmapWrapper> {
    final BitmapLruCache mCache;

    public PatternsListLoader(BitmapLruCache cache) {
        mCache = cache;
    }

    @Override
    public boolean isItemInMemory(String url) {
        return (mCache.getFromMemoryCache(url) != null);
    }

    @Override
    public CacheableBitmapWrapper loadItemFromMemory(String url) {
        return mCache.getFromMemoryCache(url);
    }

    @Override
    public void resetItem(View itemView) {
        ViewHolder holder = (ViewHolder) itemView.getTag();
        holder.image.setImageDrawable(null);
        holder.title.setText("Loading");
    }

    @Override
    public String getItemParams(Adapter adapter, int position) {
        return (String) adapter.getItem(position);
    }

    @Override
    public CacheableBitmapWrapper loadItem(String url) {
        CacheableBitmapWrapper wrapper = mCache.get(url);
        if (wrapper == null) {
            wrapper = mCache.put(url, HttpHelper.loadImage(url));
        }

        return wrapper;
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapWrapper result, boolean fromMemory) {
        ViewHolder holder = (ViewHolder) itemView.getTag();

        if (result == null) {
            holder.title.setText("Failed");
            return;
        }

        BitmapDrawable patternDrawable = new BitmapDrawable(itemView.getResources(), result.getBitmap());
        patternDrawable.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);

        if (fromMemory) {
            holder.image.setImageDrawable(patternDrawable);
        } else {
            BitmapDrawable emptyDrawable = new BitmapDrawable(itemView.getResources());

            TransitionDrawable fadeInDrawable =
                    new TransitionDrawable(new Drawable[] { emptyDrawable, patternDrawable });

            holder.image.setImageDrawable(fadeInDrawable);
            fadeInDrawable.startTransition(200);
        }

        holder.title.setText("Loaded");
    }
}
