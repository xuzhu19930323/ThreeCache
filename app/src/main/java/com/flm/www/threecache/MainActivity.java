package com.flm.www.threecache;


import com.flm.www.threecache.ImageAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

public class MainActivity extends Activity {

	private ListView mlistview;
	private ImageAdapter mAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		///cache = new BitmapCacheUtils();
		mlistview = (ListView)findViewById(R.id.listview);
		mAdapter = new ImageAdapter(this,Images.imageThumbUrls,mlistview);
		mlistview.setAdapter(mAdapter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mAdapter.getCache().fluchCache();
	}
	
}
