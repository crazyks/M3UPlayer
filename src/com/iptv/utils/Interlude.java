package com.iptv.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.iptv.player.R;

public class Interlude {
	public static interface Callback {
		public void onCompleted();
	}

	public static abstract class Task {
		public static interface Callback {
			public void onProgreeUpdate(int current, int total);
		}

		private final boolean mSupportProgress;
		protected Callback mCallback = null;

		public Task(boolean supportProgress) {
			mSupportProgress = supportProgress;
		}

		public void setCallback(Callback callback) {
			mCallback = callback;
		}

		public boolean isSupportProgress() {
			return mSupportProgress;
		}

		public abstract void execute();
	}

	private final Context mContext;
	private final Task mTask;
	private Callback mCallback;
	
	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	private final AsyncTask<Void, Integer, Void> buildTask() {
		return new AsyncTask<Void, Integer, Void>() {
			final private Task.Callback cb = new Task.Callback() {

				@Override
				public void onProgreeUpdate(int current, int total) {
					publishProgress(current, total);
				}
			};
			private ProgressDialog mDialog = null;

			@Override
			protected void onPreExecute() {
				if (mContext != null) {
					mDialog = new ProgressDialog(mContext);
					mDialog.setMessage(mContext
							.getString(R.string.tips_loading));
					mDialog.setIndeterminate(false);
					if (mTask.isSupportProgress()) {
						mTask.setCallback(cb);
						mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						mDialog.setMax(100);
						mDialog.setProgress(0);
					} else {
						mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					}
					mDialog.setCancelable(false);
					mDialog.show();
				}
				super.onPreExecute();
			}

			@Override
			protected Void doInBackground(Void... params) {
				mTask.execute();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (mCallback != null) {
					mCallback.onCompleted();
				}
				if (mDialog != null && mDialog.isShowing()) {
					mDialog.dismiss();
				}
				super.onPostExecute(result);
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				if (mDialog != null && mDialog.isShowing() && values != null
						&& values.length == 2) {
					mDialog.setMax(values[1]);
					mDialog.setProgress(values[0]);
				}
				super.onProgressUpdate(values);
			}
		};
	}

	/**
	 * Interlude UI, none-UI task can be executed with it.
	 * 
	 * @param ctx
	 *            the instance of Context, usually it should be an Activity.
	 * @param task
	 *            the instance of Task, it cannot be a null pointer.
	 */
	public Interlude(Context ctx, Task task) {
		if (task == null) {
			throw new IllegalArgumentException("Task cannot be a null pointer.");
		}
		mContext = ctx;
		mTask = task;
	}

	public void excute() {
		buildTask().execute();
	}
}
