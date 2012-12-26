package org.lucasr.smoothie.samples;

import org.lucasr.smoothie.ItemEngine;
import org.lucasr.smoothie.samples.PatternsListAdapter.ViewHolder;

import android.graphics.Bitmap;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;

public class PatternsListEngine extends ItemEngine {
    @Override
    public void resetItem(View itemView) {
        ViewHolder holder = (ViewHolder) itemView.getTag();
        holder.image.setImageDrawable(null);
        holder.title.setText("Loading");
    }

    @Override
    public Object loadItem(Object itemParams) {
        return HttpHelper.loadImage((String) itemParams);
    }

    @Override
    public void displayItem(View itemView, Object item) {
        ViewHolder holder = (ViewHolder) itemView.getTag();

        BitmapDrawable patternDrawable = new BitmapDrawable(itemView.getResources(), (Bitmap) item);
        patternDrawable.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);

        BitmapDrawable emptyDrawable = new BitmapDrawable(itemView.getResources());

        TransitionDrawable fadeInDrawable =
                new TransitionDrawable(new Drawable[] { emptyDrawable, patternDrawable });

        holder.image.setImageDrawable(fadeInDrawable);
        fadeInDrawable.startTransition(300);

        holder.title.setText("Loaded");
    }
}
