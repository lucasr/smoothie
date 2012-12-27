package org.lucasr.smoothie.samples;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lucasr.smoothie.Smoothie;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ListView;

public class MainActivity extends Activity {
    private ListView mListView;
    private Smoothie mSmoothie;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        mListView = (ListView) findViewById(R.id.list);

        BitmapLruCache cache = App.getInstance(this).getBitmapCache();
        PatternsListEngine engine = new PatternsListEngine(cache);

        Smoothie.Builder builder = new Smoothie.Builder(mListView, engine);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(5);
        builder.setThreadPoolSize(4);
        mSmoothie = builder.build();

        new LoadPatternsListTask().execute();
    }

    private class LoadPatternsListTask extends AsyncTask<Void, Void, ArrayList<String>> {
        static final int NUM_PATTERNS = 40;
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
           PatternsListAdapter adapter = new PatternsListAdapter(MainActivity.this, urls, mSmoothie);
           mListView.setAdapter(adapter);
       }
   }
}
