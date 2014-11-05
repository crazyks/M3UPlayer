package com.iptv.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.iptv.parser.M3UFile;
import com.iptv.parser.M3UItem;
import com.iptv.parser.M3UToolSet;
import com.iptv.utils.BaseActivity;
import com.iptv.utils.FileBrowser;
import com.iptv.utils.FileBrowser.OnFileSelectedListener;
import com.iptv.utils.Interlude;
import com.iptv.utils.Interlude.Callback;
import com.iptv.utils.Interlude.Task;
import com.iptv.utils.MessageBox;
import com.iptv.utils.SystemProperties;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class Player extends BaseActivity {
	private static final String TAG = "M3U";
	private static final boolean DEBUGON = true;
	private static final String KEY_PATH = "ro.playlist.default";
	private static final String DEFAULT_PATH = "/system/etc/playlist.m3u";
	private static final String SP_NAME = "data";
	private static final String SP_KEY = "path";

	private VideoView mVideoView;
	private RelativeLayout mMenu;
	private TextView mGroupTitle;
	private Button mPrevPage;
	private Button mNextPage;
	private GridView mGridView;
	private Button mBrowse;
	private Button mReset;
	private AlertDialog mBrowser = null;
	private ArrayList<Pair<String, ChannelAdapter>> mCatalogs = new ArrayList<Pair<String,ChannelAdapter>>();
	private String[] mGroups = null;
	private int mPosition = 0;
	private String mCurrentPath = null;

	private ImageLoader mImgLoader = ImageLoader.getInstance();
	private DisplayImageOptions mOpt;
	private View.OnClickListener mOnClicklistener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (v == mPrevPage) {
				prev();
			} else if (v == mNextPage) {
				next();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initViews();
		prepareData(loadPath(SystemProperties.get(KEY_PATH, DEFAULT_PATH)));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (mMenu.getVisibility() != View.VISIBLE) {
				mMenu.setVisibility(View.VISIBLE);
				mMenu.requestLayout();
				mMenu.requestFocus();
			} else {
				choose(mPosition);
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_ESCAPE) {
			if (mMenu.getVisibility() == View.VISIBLE) {
				mMenu.setVisibility(View.INVISIBLE);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void initViews() {
		mVideoView = (VideoView) findViewById(R.id.video);
		mMenu = (RelativeLayout) findViewById(R.id.menu_bg);
		mGroupTitle = (TextView) findViewById(R.id.group_title);
		mPrevPage = (Button) findViewById(R.id.prev_page);
		mNextPage = (Button) findViewById(R.id.next_page);
		mGridView = (GridView) findViewById(R.id.grid);
		mBrowse = (Button) findViewById(R.id.browse);
		mReset = (Button) findViewById(R.id.reset);

		mPrevPage.setOnClickListener(mOnClicklistener);
		mNextPage.setOnClickListener(mOnClicklistener);
		mVideoView.setMediaController(new MediaController(this));
		mVideoView.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				new MessageBox(Player.this, getString(R.string.error),
						getString(R.string.error_cannot_play, what),
						MessageBox.MB_OK);
				return true;
			}
		});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				play((M3UItem) ((ChannelAdapter) parent.getAdapter()).getItem(position));
			}
		});
		mBrowse.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				browse();
			}
		});
		mReset.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				reset();
			}
		});
		
		mOpt = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.default_logo)
		.showImageForEmptyUri(R.drawable.default_logo)
		.showImageOnFail(R.drawable.default_logo)
		.cacheInMemory(true)
		.cacheOnDisk(true)
		.considerExifParams(true)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
				.threadPriority(Thread.NORM_PRIORITY - 2)
				.denyCacheImageMultipleSizesInMemory()
				.diskCacheFileNameGenerator(new Md5FileNameGenerator())
				.diskCacheSize(50 * 1024 * 1024) // 50 Mb
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				.build();
		mImgLoader.init(config);
	}

	private void prepareData(final String path) {
		if (path.equals(mCurrentPath)) {
			return;
		}
		final ArrayList<Pair<String, M3UItem[]>> data = new ArrayList<Pair<String,M3UItem[]>>();
		Interlude interlude = new Interlude(this, new Task(false) {
			final HashMap<String, ArrayList<M3UItem>> map = new HashMap<String, ArrayList<M3UItem>>();

			@Override
			public void execute() {
				M3UFile m3ufile = M3UToolSet.load(path);
				if (DEBUGON) {
					Log.d(TAG, "Load [" + path + "]");
					Log.d(TAG, m3ufile.toString());
				}
				if (m3ufile.getItems().isEmpty()) {
					return;
				}
				data.add(new Pair<String, M3UItem[]>(
						getString(R.string.default_catalog), m3ufile.getItems()
								.toArray(new M3UItem[0])));
				for (M3UItem item : m3ufile.getItems()) {
					if (item.getGroupTitle() != null) {
						ArrayList<M3UItem> list = null;
						if (map.containsKey(item.getGroupTitle())) {
							list = map.get(item.getGroupTitle());
						} else {
							list = new ArrayList<M3UItem>();
							map.put(item.getGroupTitle(), list);
						}
						list.add(item);
					}
				}
				Set<String> keyset = map.keySet();
				for (Iterator<String> it = keyset.iterator(); it.hasNext();) {
					String key = it.next();
					data.add(new Pair<String, M3UItem[]>(key, map.get(key)
							.toArray(new M3UItem[0])));
				}
			}
		});
		interlude.setCallback(new Callback() {
			
			@Override
			public void onCompleted() {
				savePath(mCurrentPath = path);
				ArrayList<String> groups = new ArrayList<String>();
				mCatalogs.clear();
				for (Pair<String, M3UItem[]> x : data) {
					mCatalogs.add(new Pair<String, ChannelAdapter>(x.first,
							new ChannelAdapter(x.second)));
					groups.add(x.first);
				}
				mGroups = groups.toArray(new String[0]);
				if (mCatalogs.isEmpty()) {
					new MessageBox(Player.this, getString(R.string.error),
							getString(R.string.error_empty_playlist2),
							MessageBox.MB_YESNO, new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									browse();
								}
							}, new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							});
				} else {
					if (mCatalogs.size() == 1) {
						mPrevPage.setVisibility(View.INVISIBLE);
						mNextPage.setVisibility(View.INVISIBLE);
					} else {
						mPrevPage.setVisibility(View.VISIBLE);
						mNextPage.setVisibility(View.VISIBLE);
					}
					load(0);
				}
			}
		});
		interlude.excute();
	}

	private void load(int position) {
		if (position < 0 || position >= mCatalogs.size()) {
			return;
		}
		Pair<String,ChannelAdapter> info = mCatalogs.get(position);
		mPosition = position;
		mGroupTitle.setText(info.first);
		mGridView.setAdapter(info.second);
		mMenu.setVisibility(View.VISIBLE);
		mGridView.requestFocus();
	}

	private void browse() {
		if (mBrowser == null) {
			mBrowser = FileBrowser.createFileBrowser(this, "/", ".m3u", new OnFileSelectedListener() {
				
				@Override
				public void onFileSelected(String path) {
					prepareData(path);
					if (mBrowser != null && mBrowser.isShowing()) {
						mBrowser.dismiss();
					}
				}
			});
			mBrowser.setOnDismissListener(new OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface dialog) {
					mBrowse.requestFocus();
				}
			});
		}
		mBrowser.show();
	}

	private void prev() {
		mPosition--;
		if (mPosition < 0) {
			mPosition = mCatalogs.size() - 1;
		}
		load(mPosition);
	}

	private void next() {
		mPosition++;
		if (mPosition >= mCatalogs.size()) {
			mPosition = 0;
		}
		load(mPosition);
	}

	private void play(final M3UItem item) {
		if (item.getStreamURL() != null) {
			Interlude loading = new Interlude(this, new Task(false) {
				
				@Override
				public void execute() {
					mVideoView.stopPlayback();
				}
			});
			loading.setCallback(new Callback() {
				
				@Override
				public void onCompleted() {
					mVideoView.setVideoURI(Uri.parse(item.getStreamURL()));
					mVideoView.start();
				}
			});
			loading.excute();
		} else {
			new MessageBox(this, getString(R.string.error),
					getString(R.string.error_invalid_url), MessageBox.MB_OK);
		}
	}

	private void choose(final int defaultPos) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_choose_group)
				.setSingleChoiceItems(mGroups, defaultPos,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (which != defaultPos) {
									load(which);
								}
								dialog.dismiss();
							}
						}).create().show();
	}

	private void reset() {
		prepareData(SystemProperties.get(KEY_PATH, DEFAULT_PATH));
	}

	private void savePath(String path) {
		getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit()
				.putString(SP_KEY, path).commit();
	}

	private String loadPath(String defaultPath) {
		return getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).getString(
				SP_KEY, defaultPath);
	}

	static class ViewHolder {
		ImageView mLogo;
		TextView mName;
	}

	public class ChannelAdapter extends BaseAdapter {
		private final M3UItem[] mItems;

		public ChannelAdapter(M3UItem[] items) {
			super();
			mItems = items;
		}

		@Override
		public int getCount() {
			return mItems.length;
		}

		@Override
		public Object getItem(int position) {
			return mItems[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			View view = convertView;
			if (view == null) {
				view = getLayoutInflater()	.inflate(R.layout.item, parent, false);
				holder = new ViewHolder();
				assert view != null;
				holder.mLogo = (ImageView) view.findViewById(R.id.logo);
				holder.mName = (TextView) view.findViewById(R.id.name);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}
			mImgLoader.displayImage(mItems[position].getLogoURL(),
					holder.mLogo, mOpt, new SimpleImageLoadingListener(),
					new ImageLoadingProgressListener() {

						@Override
						public void onProgressUpdate(String imageUri,
								View view, int current, int total) {
						}
					});
			holder.mName.setText(mItems[position].getChannelName());
			return view;
		}
	}

}
