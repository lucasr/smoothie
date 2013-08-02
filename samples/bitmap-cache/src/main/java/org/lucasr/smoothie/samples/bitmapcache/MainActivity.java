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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

public class MainActivity extends Activity {
    private AsyncListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        mListView = (AsyncListView) findViewById(R.id.list);

        BitmapLruCache cache = App.getInstance(this).getBitmapCache();
        PatternsListLoader loader = new PatternsListLoader(cache);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(5);
        builder.setThreadPoolSize(4);

        mListView.setItemManager(builder.build());

        new LoadPatternsListTask().execute();
    }

    private class LoadPatternsListTask extends AsyncTask<Void, Void, ArrayList<String>> {
        static final int NUM_PATTERNS = 100;
        static final String URL = "http://www.colourlovers.com/api/patterns/new?format=json&numResults=" + NUM_PATTERNS;

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            ArrayList<String> urls = new ArrayList<String>();

            JSONArray patternsArray = HttpHelper.loadJSON(URL);
            final int nPatterns = patternsArray.length();

            try {
                for (int i = 0; i < nPatterns; i++) {
                    JSONObject patternInfo = (JSONObject) patternsArray.get(i);
                    urls.add(patternInfo.getString("imageUrl"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return urls;
       }

       @Override
       protected void onPostExecute(ArrayList<String> urls) {
           PatternsListAdapter adapter = new PatternsListAdapter(MainActivity.this, urls);
           mListView.setAdapter(adapter);
       }
   }
}
