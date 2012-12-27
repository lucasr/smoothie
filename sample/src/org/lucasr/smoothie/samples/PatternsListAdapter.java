package org.lucasr.smoothie.samples;

import java.util.ArrayList;

import org.lucasr.smoothie.ItemManager;

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
	private final ItemManager mItemManager;

	public PatternsListAdapter(Context context, ArrayList<String> urls, ItemManager itemManager) {
		mUrls = urls;
		mContext = context;
		mItemManager = itemManager;
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

		mItemManager.loadItem(convertView, mUrls.get(position));

		return convertView;
	}

	class ViewHolder {
	    public ImageView image;
	    public TextView title;
	}
}
