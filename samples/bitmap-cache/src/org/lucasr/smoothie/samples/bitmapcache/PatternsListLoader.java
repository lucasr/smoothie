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

package org.lucasr.smoothie.samples.bitmapcache;

import org.lucasr.smoothie.SimpleItemLoader;
import org.lucasr.smoothie.samples.bitmapcache.PatternsListAdapter.ViewHolder;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.widget.Adapter;

public class PatternsListLoader extends SimpleItemLoader<String, CacheableBitmapDrawable> {
    final BitmapLruCache mCache;

    public PatternsListLoader(BitmapLruCache cache) {
        mCache = cache;
    }

    @Override
    public CacheableBitmapDrawable loadItemFromMemory(String url) {
        return mCache.getFromMemoryCache(url);
    }

    @Override
    public String getItemParams(Adapter adapter, int position) {
        return (String) adapter.getItem(position);
    }

    @Override
    public CacheableBitmapDrawable loadItem(String url) {
        CacheableBitmapDrawable wrapper = mCache.get(url);
        if (wrapper == null) {
            wrapper = mCache.put(url, HttpHelper.loadImage(url));
        }

        return wrapper;
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapDrawable result, boolean fromMemory) {
        ViewHolder holder = (ViewHolder) itemView.getTag();

        if (result == null) {
            holder.title.setText("Failed");
            return;
        }

        result.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);

        if (fromMemory) {
            holder.image.setImageDrawable(result);
        } else {
            BitmapDrawable emptyDrawable = new BitmapDrawable(itemView.getResources());

            TransitionDrawable fadeInDrawable =
                    new TransitionDrawable(new Drawable[] { emptyDrawable, result });

            holder.image.setImageDrawable(fadeInDrawable);
            fadeInDrawable.startTransition(200);
        }

        holder.title.setText("Loaded");
    }
}
