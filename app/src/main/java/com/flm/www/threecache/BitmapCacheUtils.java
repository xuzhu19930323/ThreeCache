package com.flm.www.threecache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;

import com.flm.www.threecache.DiskLruCache.Snapshot;

public class BitmapCacheUtils {
	private Set<BitmapWorkerTask> taskCollection;
	private DiskLruCache mDiskLruCache;
	private MemoryCacheUtils mMemoryCacheUtils;
	private Context mconext;
	private Bitmap mLoadingBitmap;
	public BitmapCacheUtils(Context context) {
		mconext = context;
		mLoadingBitmap = BitmapFactory.decodeResource(context.getResources(),
				R.mipmap.empty_photo);
		taskCollection = new HashSet<BitmapWorkerTask>();
		mMemoryCacheUtils = new MemoryCacheUtils();
		try {
			// 获取图片缓存路径
			File cacheDir = getDiskCacheDir(context, "thumb");
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}
			// 创建DiskLruCache实例，初始化缓存数据
			mDiskLruCache = DiskLruCache.open(cacheDir, getAppVersion(context),
					1, 10 * 1024 * 1024);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据传入的uniqueName获取硬盘缓存的路径地址。
	 */
	@SuppressLint("NewApi")
	public File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + uniqueName);
	}

	/**
	 * 获取当前应用程序的版本号。
	 */
	public int getAppVersion(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}

	/**
	 * 使用MD5算法对传入的key进行加密并返回。
	 */
	public String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	private String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
	
	
	public void fluchCache() {
		if (mDiskLruCache != null) {
			try {
				mDiskLruCache.flush();
				Log.i("syso", "mDiskLruCache.flush()");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	

	
	public void display(ImageView imageView, String imageUrl) {
		try {
			BitmapDrawable drawable = mMemoryCacheUtils.getBitmapFromMemoryCache(imageUrl);
			if (drawable != null) {
				if (imageView != null && drawable != null) {
					imageView.setImageDrawable(drawable);
				}
			} else if(cancelPotentialWork(imageUrl,imageView)){
				BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				AsyncDrawable asyncDrawable = new AsyncDrawable(mconext
						.getResources(), mLoadingBitmap, task);
				imageView.setImageDrawable(asyncDrawable);
				task.execute(imageUrl);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 *
	 * 检查复用的ImageView中是否存在其他图片的下载任务，如果存在就取消并且返回ture 否则返回 false
	 */
	private boolean cancelPotentialWork(String url, ImageView imageView) {
		BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
		if (bitmapWorkerTask != null) {
			String imageUrl = bitmapWorkerTask.mimageUrl;
			if (imageUrl == null || !imageUrl.equals(url)) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}


	/**
	 * 获取传入的ImageView它所对应的BitmapWorkerTask。
	 */
	private BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	/**
	 * 自定义的一个Drawable，让这个Drawable持有BitmapWorkerTask的弱引用。
	 */
	class AsyncDrawable extends BitmapDrawable {

		private WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap,
							 BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
					bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}

	}







	/**
	 * 异步下载图片的任务。
	 * 
	 * @author 舒小替
	 */
	class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {

		/**
		 * 图片的URL地址
		 */
		private String mimageUrl;
		private ListView mlistview;
		private ImageView image;
		private WeakReference<ImageView> imageViewReference;
//		public BitmapWorkerTask(ListView listview) {
//			mlistview = listview;
//		}

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		private ImageView getAttachedImageView() {
			ImageView imageView = imageViewReference.get();
			BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
			if (this == bitmapWorkerTask) {
				return imageView;
			}
			return null;
		}


		@Override
		protected BitmapDrawable doInBackground(String... params) {
			mimageUrl = params[0];
			FileDescriptor fileDescriptor = null;
			FileInputStream fileInputStream = null;
			Snapshot snapShot = null;
			try {
				// 生成图片URL对应的key
				final String key = hashKeyForDisk(mimageUrl);
				// 查找key对应的缓存
				snapShot = mDiskLruCache.get(key);
				if (snapShot == null) {
					// 如果没有找到对应的缓存，则准备从网络上请求数据，并写入缓存
					DiskLruCache.Editor editor = mDiskLruCache.edit(key);
					if (editor != null) {
						OutputStream outputStream = editor.newOutputStream(0);
						if (downloadUrlToStream(mimageUrl, outputStream)) {
							editor.commit();
						} else {
							editor.abort();
						}
					}
					// 缓存被写入后，再次查找key对应的缓存
					snapShot = mDiskLruCache.get(key);
				}
				if (snapShot != null) {
					fileInputStream = (FileInputStream) snapShot.getInputStream(0);
					fileDescriptor = fileInputStream.getFD();
				}
				// 将缓存数据解析成Bitmap对象
				BitmapDrawable drawable = null;
				if (fileDescriptor != null) {
					Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
					drawable = new BitmapDrawable(mconext.getResources(), bitmap);
				}
				if (drawable != null) {
					// 将Bitmap对象添加到内存缓存当中
					mMemoryCacheUtils.addBitmapToMemoryCache(params[0], drawable);
				}
				return drawable;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (fileDescriptor == null && fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (IOException e) {
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(BitmapDrawable drawable) {
			// 根据Tag找到相应的ImageView控件，将下载好的图片显示出来。
//			ImageView i = (ImageView) mlistview.findViewWithTag(mimageUrl);
			ImageView i = getAttachedImageView();
			if (i != null && drawable != null) {
				i.setImageDrawable(drawable);
			}
			//taskCollection.remove(this);
		}

		/**
		 * 建立HTTP请求，并获取Bitmap对象。
		 * 
		 */
		private boolean downloadUrlToStream(String urlString,
				OutputStream outputStream) {
			HttpURLConnection urlConnection = null;
			BufferedOutputStream out = null;
			BufferedInputStream in = null;
			try {
				final URL url = new URL(urlString);
				urlConnection = (HttpURLConnection) url.openConnection();
				in = new BufferedInputStream(urlConnection.getInputStream(),
						8 * 1024);
				out = new BufferedOutputStream(outputStream, 8 * 1024);
				int b;
				while ((b = in.read()) != -1) {
					out.write(b);
				}

				return true;
			} catch (final IOException e) {
				e.printStackTrace();
			} finally {
				if (urlConnection != null) {
					urlConnection.disconnect();
				}
				try {
					if (out != null) {
						out.close();
					}
					if (in != null) {
						in.close();
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			return false;
		}

	}

	//	public Bitmap getBitmapFromDisk(String url) {
//
//		try {
//			String key = hashKeyForDisk(url);
//
//			Snapshot snapshot = mDiskLruCache.get(key);
//			if (snapshot != null) {
//				InputStream is = snapshot.getInputStream(0);
//				Bitmap bitmap = BitmapFactory.decodeStream(is);
//				return bitmap;
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return null;
//
//	}
}
