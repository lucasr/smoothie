package org.lucasr.smoothie.samples.bitmapcache;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PatternsListAdapter extends BaseAdapter {
	private final ArrayList<String> mUrls;
	private final Context mContext;

	public PatternsListAdapter(Context context, ArrayList<String> urls) {
		mUrls = urls;
		mContext = context;
	}

	@Override
	public int getCount() {
	    if (mUrls == null) {
	        return 0;
	    }

	    return mUrls.size();
	}

	@Override
	public String getItem(int position) {
		return mUrls.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    ViewHolder holder = null;

		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);

			holder = new ViewHolder();
			holder.image = (ImageView) convertView.findViewById(R.id.image);
			holder.title = (TextView) convertView.findViewById(R.id.title);

			convertView.setTag(holder);
		} else {
		    holder = (ViewHolder) convertView.getTag();
		}

        holder.image.setImageDrawable(null);
        holder.title.setText("Loading");

		return convertView;
	}

	class ViewHolder {
	    public ImageView image;
	    public TextView title;
	}
}
