package org.lucasr.smoothie.samples.gallery;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.lucasr.smoothie.ItemLoader;
import org.lucasr.smoothie.samples.gallery.GalleryAdapter.ViewHolder;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.View;
import android.widget.Adapter;

public class GalleryLoader extends ItemLoader<Long, Bitmap> {
    private final Context mContext;

    public GalleryLoader(Context context) {
        mContext = context;
    }

    @Override
    public int getItemSizeInMemory(Long id, Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    @Override
    public void resetItem(View itemView) {
        ViewHolder holder = (ViewHolder) itemView.getTag();
        holder.image.setImageDrawable(null);
    }

    @Override
    public Long getItemParams(Adapter adapter, int position) {
        Cursor c = (Cursor) adapter.getItem(position);
        return c.getLong(c.getColumnIndex(ImageColumns._ID));
    }

    @Override
    public Bitmap loadItem(Long id) {
        try {
            Uri imageUri = Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
            return Images.Media.getBitmap(mContext.getContentResolver(), imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void displayItem(View itemView, Bitmap result, boolean fromMemory) {
        if (result == null) {
            return;
        }

        ViewHolder holder = (ViewHolder) itemView.getTag();

        BitmapDrawable patternDrawable = new BitmapDrawable(itemView.getResources(), result);

        if (fromMemory) {
            holder.image.setImageDrawable(patternDrawable);
        } else {
            BitmapDrawable emptyDrawable = new BitmapDrawable(itemView.getResources());

            TransitionDrawable fadeInDrawable =
                    new TransitionDrawable(new Drawable[] { emptyDrawable, patternDrawable });

            holder.image.setImageDrawable(fadeInDrawable);
            fadeInDrawable.startTransition(200);
        }
    }
}
