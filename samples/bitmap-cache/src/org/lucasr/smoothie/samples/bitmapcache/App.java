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
