package com.hijiyam_koubou.kaodori;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FdActivity extends Activity {

	static {
		System.loadLibrary("opencv_java3");    // opencv\_java3.so をロードしています。      ; rm64-v8a
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String TAG = "readPref[MA]";
		String dbMsg = "許諾済み";//////////////////
		try {
			readPref();

			setContentView(R.layout.face_detect_surface_view);

			try {
				copyAssets("haarcascades");                    // assetsの内容を /data/data/*/files/ にコピーします。
			} catch (IOException e) {
				e.printStackTrace();
			}

			CameraView cameraView = new CameraView(this , 90);

			ViewGroup activityMain = ( ViewGroup ) findViewById(R.id.fd_activity_surface_view);
			activityMain.addView(cameraView);

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	static final int REQUEST_PREF = 100;                          //Prefarensからの戻り
	static final int REQUEST_SWOPEN = REQUEST_PREF + 1;        //skyway接続開始

	/**
	 * Cameraパーミッションが通った時点でstartLocalStream
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode , String permissions[] , int[] grantResults) {
		final String TAG = "onRequestPermissionsResult[MA]";
		String dbMsg = "";
		try {
			dbMsg = "requestCode=" + requestCode;
			switch ( requestCode ) {
				case REQUEST_PREF:
					readPref();        //ループする？
					break;
			}

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * このアプリケーションの設定ファイル読出し
	 **/
	public void readPref() {
		final String TAG = "readPref[MA]";
		String dbMsg = "許諾済み";//////////////////
		try {
			if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {                //(初回起動で)全パーミッションの許諾を取る
				dbMsg = "許諾確認";
				String[] PERMISSIONS = {Manifest.permission.CAMERA , Manifest.permission.MODIFY_AUDIO_SETTINGS , Manifest.permission.RECORD_AUDIO , Manifest.permission.READ_EXTERNAL_STORAGE , Manifest.permission.MODIFY_AUDIO_SETTINGS};
				boolean isNeedParmissionReqest = false;
				for ( String permissionName : PERMISSIONS ) {
					dbMsg += "," + permissionName;
					int checkResalt = checkSelfPermission(permissionName);
					dbMsg += "=" + checkResalt;
					if ( checkResalt != PackageManager.PERMISSION_GRANTED ) {
						isNeedParmissionReqest = true;
					}
				}
				if ( isNeedParmissionReqest ) {
					dbMsg += "許諾処理へ";
					requestPermissions(PERMISSIONS , REQUEST_PREF);
					return;
				}
			}
//			dbMsg += ",isReadPref=" + isReadPref;
//			MyPreferenceFragment prefs = new MyPreferenceFragment();
//			prefs.readPref(this);
//			service_id = prefs.service_id;                    //サービスサーバのID
////			dbMsg = ",service_id=" + service_id;
//			peer_id = prefs.peer_id;                        //SkyWayで取得しているこの端末のID
//			dbMsg += ",peer_id=" + peer_id;
//			partner_id = prefs.partner_id;                    //SkyWayに接続要求する相手端末のID
//			dbMsg += ",partner_id=" + partner_id;
//			API_KEY = prefs.API_KEY;                        //SkyWayに接続する為のAPIキー
////			dbMsg = ",API_KEY=" + API_KEY;
//			sw_secret_key = prefs.sw_secret_key;                //SkyWayでAPIキーと合わせて発行されるシークレットキー
////			dbMsg += ",sw_secret_key=" + sw_secret_key;
//			DOMAIN = prefs.DOMAIN;    //SkyWayに登録した利用可能ドメイン
////			dbMsg += ",DOMAIN=" + DOMAIN;
//			sharedPref = PreferenceManager.getDefaultSharedPreferences(this);            //	getActivity().getBaseContext()
//			myEditor = sharedPref.edit();
//			//         setSaveParameter();                 //保存可能上限の確認と修正
//
//			isReadPref = true;
//			if ( _peer == null ) {
//				peer_id = "";
//				dbMsg += ">>" + peer_id;
//				makeNewPear(peer_id);
//			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	private void copyAssets(String dir) throws IOException {
		byte[] buf = new byte[8192];
		int size;

		File dst = new File(getFilesDir() , dir);
		if ( !dst.exists() ) {
			dst.mkdirs();
			dst.setReadable(true , false);
			dst.setWritable(true , false);
			dst.setExecutable(true , false);
		}

		for ( String filename : getAssets().list(dir) ) {
			File file = new File(dst , filename);
			OutputStream out = new FileOutputStream(file);
			InputStream in = getAssets().open(dir + "/" + filename);
			while ( (size = in.read(buf)) >= 0 ) {
				if ( size > 0 ) {
					out.write(buf , 0 , size);
				}
			}
			in.close();
			out.close();
			file.setReadable(true , false);
			file.setWritable(true , false);
			file.setExecutable(true , false);
		}
	}

	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */
//	public native String stringFromJNI();

	///////////////////////////////////////////////////////////////////////////////////
	public void messageShow(String titolStr , String mggStr) {
		CS_Util UTIL = new CS_Util();
		UTIL.messageShow(titolStr , mggStr , FdActivity.this);
	}

	public static void myLog(String TAG , String dbMsg) {
		CS_Util UTIL = new CS_Util();
		UTIL.myLog(TAG , dbMsg);
	}

	public static void myErrorLog(String TAG , String dbMsg) {
		CS_Util UTIL = new CS_Util();
		UTIL.myErrorLog(TAG , dbMsg);
	}
}

/**
 * 2017-02-10		AndroidでOpenCV 3.2を使って顔検出をする			https://blogs.osdn.jp/2017/02/10/opencv.html
 */