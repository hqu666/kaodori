package com.hijiyam_koubou.kaodori;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import static com.hijiyam_koubou.kaodori.MainActivity.ma_iv;

public class ThumbnailControl {
	public CS_Util UTIL;
	private Context context;
	private Activity activity;
	private Bitmap shotBitmap;
	private ImageView imgView;


	public ThumbnailControl(Activity _activity) {
		final String TAG = "ThumbnailControl[TC]";
		String dbMsg = "";
		try {
//			context=_context    ;
			activity = _activity;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	public void setThumbnail(Bitmap _shotBitmap , ImageView _imgView) {
		shotBitmap = _shotBitmap;
		imgView = _imgView;
		// 別スレッドを実行
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final String TAG = "setThumbnail[TC]";
				String dbMsg = "";
				try {
					int ivWidth = imgView.getWidth();
					int ivHeight = imgView.getHeight();
					dbMsg += ",iv[" + ivWidth + "×" + ivHeight + "]";
					Bitmap thumbNail = Bitmap.createScaledBitmap(shotBitmap , ivWidth , ivHeight , false);
					int thumbNailHeight = thumbNail.getHeight();
					dbMsg += ",thumbNail[" + thumbNail.getWidth() + "×" + thumbNailHeight + "]";
					ma_iv.setImageBitmap(thumbNail);
					//findViewById(R.id.ma_iv).setImageBitmap(bitmap);;					//撮影結果
					shotBitmap.recycle();
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}
		});
	}

	///////////////////////////////////////////////////////////////////////////////////
//	public void messageShow(String titolStr , String mggStr) {
//		if ( UTIL == null ) {
//			UTIL = new CS_Util();
//		}
//		UTIL.messageShow(titolStr , mggStr , MainActivity.this);
//	}

	public void myLog(String TAG , String dbMsg) {
		if ( UTIL == null ) {
			UTIL = new CS_Util();
		}
		UTIL.myLog(TAG , dbMsg);
	}

	public void myErrorLog(String TAG , String dbMsg) {
		if ( UTIL == null ) {
			UTIL = new CS_Util();
		}
		UTIL.myErrorLog(TAG , dbMsg);
	}

}
