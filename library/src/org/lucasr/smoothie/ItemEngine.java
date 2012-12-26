package org.lucasr.smoothie;

import android.view.View;

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

    public abstract Object loadItem(Object itemParams);

    public abstract void displayItem(View itemView, Object item);
}
