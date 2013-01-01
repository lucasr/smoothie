package org.lucasr.smoothie.samples.gallery;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class GalleryAdapter extends SimpleCursorAdapter {
    public GalleryAdapter(Context context) {
        super(context, -1, null, new String[] {}, new int[] {}, 0);
    }

    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
        Cursor c = (Cursor) getItem(position);
	    ViewHolder holder = null;

		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.grid_item, parent, false);

			holder = new ViewHolder();
			holder.image = (ImageView) convertView.findViewById(R.id.image);
			holder.title = (TextView) convertView.findViewById(R.id.title);

			convertView.setTag(holder);
		} else {
		    holder = (ViewHolder) convertView.getTag();
		}

		holder.title.setText(c.getString(c.getColumnIndex(ImageColumns.DATE_TAKEN)));

		return convertView;
	}

	class ViewHolder {
	    public ImageView image;
	    public TextView title;
	}
}
