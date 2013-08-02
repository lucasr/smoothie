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

package org.lucasr.smoothie.samples.gallery;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.lucasr.smoothie.SimpleItemLoader;
import org.lucasr.smoothie.samples.gallery.GalleryAdapter.ViewHolder;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.util.LruCache;
import android.view.View;
import android.widget.Adapter;

public class GalleryLoader extends SimpleItemLoader<Long, Bitmap> {
    private final Context mContext;
    private final LruCache<Long, Bitmap> mMemCache;

    public GalleryLoader(Context context) {
        mContext = context;

        int maxSize = (int) (Runtime.getRuntime().maxMemory() * 0.4f);
        mMemCache = new LruCache<Long, Bitmap>(maxSize) {
            @Override
            protected int sizeOf(Long id, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
    }

    @Override
    public Bitmap loadItemFromMemory(Long id) {
        return mMemCache.get(id);
    }

    @Override
    public Long getItemParams(Adapter adapter, int position) {
        Cursor c = (Cursor) adapter.getItem(position);
        return c.getLong(c.getColumnIndex(ImageColumns._ID));
    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }

        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromResource(Uri imageUri,
            int reqWidth, int reqHeight) {
        InputStream is = null;
        try {
            is = mContext.getContentResolver().openInputStream(imageUri);

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;

            is = mContext.getContentResolver().openInputStream(imageUri);
            return BitmapFactory.decodeStream(is, null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Bitmap loadItem(Long id) {
        Uri imageUri = Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));

        Resources res = mContext.getResources();
        int width = res.getDimensionPixelSize(R.dimen.image_width);
        int height = res.getDimensionPixelSize(R.dimen.image_height);

        Bitmap bitmap = decodeSampledBitmapFromResource(imageUri, width, height);
        if (bitmap != null) {
            mMemCache.put(id, bitmap);
        }

        return bitmap;
    }

    @Override
    public void displayItem(View itemView, Bitmap result, boolean fromMemory) {
        if (result == null) {
            return;
        }

        ViewHolder holder = (ViewHolder) itemView.getTag();

        BitmapDrawable imageDrawable = new BitmapDrawable(itemView.getResources(), result);

        if (fromMemory) {
            holder.image.setImageDrawable(imageDrawable);
        } else {
            BitmapDrawable emptyDrawable = new BitmapDrawable(itemView.getResources());

            TransitionDrawable fadeInDrawable =
                    new TransitionDrawable(new Drawable[] { emptyDrawable, imageDrawable });

            holder.image.setImageDrawable(fadeInDrawable);
            fadeInDrawable.startTransition(200);
        }
    }
}
