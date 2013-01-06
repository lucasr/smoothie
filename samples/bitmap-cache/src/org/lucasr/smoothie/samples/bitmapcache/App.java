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

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.content.Context;

public class App extends Application {
	private BitmapLruCache mCache;

	@Override
	public void onCreate() {
		super.onCreate();

        File cacheDir = new File(getCacheDir(), "smoothie");
		cacheDir.mkdirs();

		BitmapLruCache.Builder builder = new BitmapLruCache.Builder();
		builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
		builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheDir);

		mCache = builder.build();
	}

	public BitmapLruCache getBitmapCache() {
		return mCache;
	}

	public static App getInstance(Context context) {
		return (App) context.getApplicationContext();
	}
}
