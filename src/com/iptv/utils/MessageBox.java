package com.iptv.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

import com.iptv.player.R;

public class MessageBox {
	public static final int MB_OK = 0;
	public static final int MB_YESNO = 1;
	public static final int MB_ABORTRETRYIGNORE = 2;
	public static final int MB_YESNOCANCEL = 3;
	public static final int MB_RETRYCANCEL = 4;
	public static final int MB_OKCANCEL = 5;

	public MessageBox(final Context ctx, String title, String msg, int type,
			OnClickListener... listeners) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(title);
		builder.setMessage(msg);
		switch (type) {
		case MB_YESNO: {
			OnClickListener lstners[] = formatListener(2, listeners);
			builder.setPositiveButton(R.string.btn_yes, lstners[0]);
			builder.setNegativeButton(R.string.btn_no, lstners[1]);
			break;
		}
		case MB_ABORTRETRYIGNORE: {
			OnClickListener lstners[] = formatListener(3, listeners);
			builder.setPositiveButton(R.string.btn_abort, lstners[0]);
			builder.setNegativeButton(R.string.btn_retry, lstners[1]);
			builder.setNeutralButton(R.string.btn_ignore, lstners[2]);
			break;
		}
		case MB_YESNOCANCEL: {
			OnClickListener lstners[] = formatListener(3, listeners);
			builder.setPositiveButton(R.string.btn_yes, lstners[0]);
			builder.setNegativeButton(R.string.btn_no, lstners[1]);
			builder.setNeutralButton(R.string.btn_cancel, lstners[2]);
			break;
		}
		case MB_RETRYCANCEL: {
			OnClickListener lstners[] = formatListener(2, listeners);
			builder.setPositiveButton(R.string.btn_retry, lstners[0]);
			builder.setNegativeButton(R.string.btn_cancel, lstners[1]);
			break;
		}
		case MB_OKCANCEL: {
			OnClickListener lstners[] = formatListener(2, listeners);
			builder.setPositiveButton(R.string.btn_ok, lstners[0]);
			builder.setNegativeButton(R.string.btn_cancel, lstners[1]);
			break;
		}
		default: {
			OnClickListener lstners[] = formatListener(1, listeners);
			builder.setPositiveButton(R.string.btn_ok, lstners[0]);
			break;
		}
		}
		AlertDialog msgBox = builder.create();
		msgBox.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				if (ctx instanceof Activity) {
					((Activity) ctx).finish();
				}
			}
		});
		msgBox.show();
	}

	private final OnClickListener[] formatListener(int totalnum,
			OnClickListener... listeners) {
		OnClickListener lstner[] = new OnClickListener[totalnum];
		for (int i = 0; i < listeners.length && i < totalnum; i++) {
			lstner[i] = listeners[i];
		}
		return lstner;
	}
}
