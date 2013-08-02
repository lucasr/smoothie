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

import org.lucasr.smoothie.AsyncGridView;
import org.lucasr.smoothie.ItemManager;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int GALLERY_LOADER = 0;

    private AsyncGridView mGridView;
    private GalleryAdapter mGalleryAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        mGridView = (AsyncGridView) findViewById(R.id.grid);

        mGalleryAdapter = new GalleryAdapter(MainActivity.this);
        mGridView.setAdapter(mGalleryAdapter);

        getSupportLoaderManager().initLoader(GALLERY_LOADER, null, this);

        GalleryLoader loader = new GalleryLoader(this);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(12);
        builder.setThreadPoolSize(4);

        mGridView.setItemManager(builder.build());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        switch (loaderID) {
        case GALLERY_LOADER:
            return new CursorLoader(
                        this,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[] { ImageColumns._ID,
                                       ImageColumns.DATE_TAKEN },
                        null,
                        null,
                        ImageColumns.DATE_TAKEN + " DESC"
        );
        default:
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        mGalleryAdapter.changeCursor(c);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mGalleryAdapter.changeCursor(null);
    }
}
