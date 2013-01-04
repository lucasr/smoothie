package org.lucasr.smoothie;

/**
 * <p>The interface for an {@link android.widget.AbsListView AbsListView}
 * that can have an {@link ItemManager} associated with it. This is
 * implemented by {@link AsyncListView} and {@link AsyncGridView}.</p>
 *
 * @author Lucas Rocha <lucasr@lucasr.org>
 */
public interface AsyncAbsListView {

    /**
     * Sets an {@link ItemManager} to the view.
     *
     * @param itemManager - The {@link ItemManager}.
     */
    public void setItemManager(ItemManager itemManager);
}
