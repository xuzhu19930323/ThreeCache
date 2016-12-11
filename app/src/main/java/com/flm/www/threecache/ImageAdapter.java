package com.flm.www.threecache;


import com.flm.www.threecache.BitmapCacheUtils;
import com.flm.www.threecache.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class ImageAdapter extends BaseAdapter{
	private Context mcontext;
	private String[] mimagethumburls;
	private ListView mlistview;
	private BitmapCacheUtils bm ;
	public ImageAdapter(Context context, String[] imagethumburls, ListView listview){
		mcontext = context;
		mimagethumburls = imagethumburls;
		mlistview = listview;
		bm = new BitmapCacheUtils(mcontext);
	}
	@Override
	public int getCount() {
		return mimagethumburls.length;
	}

	@Override
	public Object getItem(int position) {
		return mimagethumburls[position];
	}

	@Override
	public long getItemId(int id) {
		return id;
	}
	
	public BitmapCacheUtils getCache(){
		return bm;
	}
	
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		final String url = (String)getItem(position);
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = LayoutInflater.from(mcontext).inflate(R.layout.photo_layout, null);
			holder.imageView = (ImageView) convertView.findViewById(R.id.photo);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}
		
		holder.imageView.setTag(url);
		bm.display(holder.imageView,url);
		return convertView;
	}
	
	
	class ViewHolder{
		private ImageView imageView;
	}
}
