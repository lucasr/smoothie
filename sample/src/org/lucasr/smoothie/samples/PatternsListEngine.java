package org.lucasr.smoothie.samples;

import org.lucasr.smoothie.ItemEngine;
import org.lucasr.smoothie.samples.PatternsListAdapter.ViewHolder;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapWrapper;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;

public class PatternsListEngine extends ItemEngine {
    final BitmapLruCache mCache;

    public PatternsListEngine(BitmapLruCache cache) {
        mCache = cache;
    }

    @Override
    public boolean isItemInMemory(Object itemParams) {
        return (mCache.getFromMemoryCache((String) itemParams) != null);
    }

    @Override
    public Object loadItemFromMemory(Object itemParams) {
        return mCache.getFromMemoryCache((String) itemParams);
    }

    @Override
    public void resetItem(View itemView) {
        ViewHolder holder = (ViewHolder) itemView.getTag();
        holder.image.setImageDrawable(null);
        holder.title.setText("Loading");
    }

    @Override
    public Object loadItem(Object itemParams) {
        String url = (String) itemParams;

        CacheableBitmapWrapper wrapper = mCache.get(url);
        if (wrapper == null) {
            wrapper = mCache.put(url, HttpHelper.loadImage(url));
        }

        return wrapper;
    }

    @Override
    public void displayItem(View itemView, Object item, boolean fromMemory) {
        ViewHolder holder = (ViewHolder) itemView.getTag();

        if (item == null) {
            holder.title.setText("Failed");
            return;
        }

        CacheableBitmapWrapper wrapper = (CacheableBitmapWrapper) item;

        BitmapDrawable patternDrawable = new BitmapDrawable(itemView.getResources(), wrapper.getBitmap());
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
