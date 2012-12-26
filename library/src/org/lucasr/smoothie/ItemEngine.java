package org.lucasr.smoothie;

import android.view.View;
import android.widget.Adapter;

public abstract class ItemEngine {
    public void resetItem(View itemView) {
    }

    public boolean isItemInMemory(Object itemParams) {
        return false;
    }

    public Object loadItemFromMemory(Object itemParams) {
        return null;
    }

    public Object preloadItem(Object itemParams) {
        return loadItem(itemParams);
    }

    public Object getPreloadItemParams(Adapter adapter, int position) {
        return null;
    }

    public abstract Object loadItem(Object itemParams);

    public abstract void displayItem(View itemView, Object item, boolean fromMemory);
}
