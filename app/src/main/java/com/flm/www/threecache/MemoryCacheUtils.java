package com.flm.www.threecache;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.util.LruCache;
import android.util.Log;

//内存缓存工具类

public class MemoryCacheUtils {
	private LruCache<String, BitmapDrawable> mCache;

	public MemoryCacheUtils() {
		int maxMemory = (int) Runtime.getRuntime().maxMemory();// 获取虚拟机分配的最大内存
		// 16M
		mCache = new LruCache<String, BitmapDrawable>(maxMemory / 8) {
			@Override
			protected int sizeOf(String key, BitmapDrawable drawable) {
				// 计算一个bitmap的大小
				return drawable.getBitmap().getByteCount();
			}
		};
	}

	public BitmapDrawable getBitmapFromMemoryCache(String url) {
		return mCache.get(url);
	}

	public void addBitmapToMemoryCache(String url, BitmapDrawable drawable) {
		if (getBitmapFromMemoryCache(url) == null ) {
			mCache.put(url,drawable);
		}
	}

}
