package com.hijiyam_koubou.kaodori;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class FdActivity extends Activity {

	static {
		System.loadLibrary("opencv_java3");    // opencv\_java3.so をロード	;	\jniLibsmの中のプロセッサー分、検索
	}

	public ViewGroup activityMain;
	public CameraView cameraView;
	public int sensorOrientation;    //カメラの向き
	public int displayRotation;            //端末の位置番号（上端；上＝、右=1 , 左＝,下= ）

	public boolean isCrated = false;

	public static SharedPreferences sharedPref;
	public SharedPreferences.Editor myEditor;
	public String writeFolder = "";
	public float upScale = 1.2f;
	public long haarcascadesLastModified = 0;

	/**
	 * このアプリケーションの設定ファイル読出し
	 **/
	public void readPref() {
		final String TAG = "readPref[MA]";
		String dbMsg = "許諾済み";//////////////////
		try {
			if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {                //(初回起動で)全パーミッションの許諾を取る
				dbMsg = "許諾確認";
				String[] PERMISSIONS = {Manifest.permission.CAMERA , Manifest.permission.WRITE_EXTERNAL_STORAGE , Manifest.permission.READ_EXTERNAL_STORAGE};
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
			MyPreferenceFragment prefs = new MyPreferenceFragment();
			prefs.readPref(this);
			writeFolder = prefs.write_folder;
			dbMsg += "," + getResources().getString(R.string.write_folder) + "=" + writeFolder;
			upScale = Float.parseFloat(prefs.up_scale);
			dbMsg += "," + getResources().getString(R.string.up_scale) + "=" + upScale;
			haarcascadesLastModified = Long.parseLong(prefs.haarcascades_last_modified);
			dbMsg += "," + getResources().getString(R.string.haarcascades_last_modified) + "=" + haarcascadesLastModified;

			sharedPref = PreferenceManager.getDefaultSharedPreferences(this);            //	getActivity().getBaseContext()
			myEditor = sharedPref.edit();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String TAG = "onCreate[MA]";
		String dbMsg = "";//////////////////
		try {
			isCrated = false;
			readPref();
			setContentView(R.layout.face_detect_surface_view);
			activityMain = ( ViewGroup ) findViewById(R.id.fd_activity_surface_view);

			try {
				copyAssets("haarcascades");                    // assetsの内容を /data/data/*/files/ にコピーします。
			} catch (IOException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
//			laterCreate();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		final String TAG = "onStart[MA]";
		String dbMsg = "";
		try {
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		final String TAG = "onResume[MA]";
		String dbMsg = "";
		try {
			laterCreate();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}                                                                 // onStart, onPauseの次

	@Override
	protected void onPause() {
		super.onPause();
		final String TAG = "onPause[MA]";
		String dbMsg = "";
		try {

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		final String TAG = "onStop[MA]";
		String dbMsg = "";
		try {
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		final String TAG = "onDestroy[MA]";
		String dbMsg = "";
		try {
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		final String TAG = "onWindowFocusChanged[MA]";
		String dbMsg = "hasFocus=" + hasFocus;
		try {
//			laterCreate();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

//	@Override
//	public void onMultiWindowChanged(boolean isInMultiWindowMode) {
//		super.onMultiWindowModeChanged(isInMultiWindowMode);
//	}

	/**
	 * 画面回転を検出
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final String TAG = "onConfigurationChanged[MA]";
		String dbMsg = "";
		try {
			displayRotation = getWindowManager().getDefaultDisplay().getRotation();
			dbMsg += ",端末の向き=" + displayRotation;    //上端； 上=0,右=1,左=3,下=0
			dbMsg = "newConfig.screenLayout=" + newConfig.screenLayout;
			dbMsg = "newConfig.orientation=" + newConfig.orientation;
			cameraView.setDig2Cam(getCameraPreveiwDeg());
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown(int keyCode , KeyEvent event) {
		final String TAG = "onKeyDown";
		String dbMsg = "開始";
		try {
			dbMsg = "keyCode=" + keyCode;//+",getDisplayLabel="+String.valueOf(MyEvent.getDisplayLabel())+",getAction="+MyEvent.getAction();////////////////////////////////
			myLog(TAG , dbMsg);
			switch ( keyCode ) {    //キーにデフォルト以外の動作を与えるもののみを記述★KEYCODE_MENUをここに書くとメニュー表示されない
				case KeyEvent.KEYCODE_BACK:            //4KEYCODE_BACK :keyCode；09SH: keyCode；4,MyEvent=KeyEvent{action=0 code=4 repeat=0 meta=0 scancode=158 mFlags=72}
//                    if (fragmentNo == mainFragmentNo) {
					callQuit();
//                    } else {
//                        callMain();
//                    }
					return true;
//				case KeyEvent.KEYCODE_HOME:            //3
////					ComponentName compNmae = startService(new Intent(MainActivity.this, NotificationChangeService.class));                           //     makeNotificationを持つクラスへ
////					dbMsg = "compNmae=" + compNmae;     //compNmae=ComponentInfo{hijiyama_koubou.com.residualquantityofthesleep/hijiyama_koubou.com.residualquantityofthesleep.NotificationChangeService}
////						NotificationManager mNotificationManager = ( NotificationManager ) mainActivity.getSystemService(NOTIFICATION_SERVICE);
////						mNotificationManager.cancel(NOTIFICATION_ID);            //サービスの停止時、通知内容を破棄する
//					myLog(TAG, dbMsg);
//					return true;
				default:
					return false;
			}
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			return false;
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

	public int getCameraPreveiwDeg() {
		final String TAG = "getCameraPreveiwDeg[MA]";
		String dbMsg = "";
		int orientationDeg = 90;
		try {
			android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
			android.hardware.Camera.getCameraInfo(0 , info);
			int rotation = getWindowManager().getDefaultDisplay().getRotation();
			dbMsg += ",rotation="+rotation;
			int degrees = 0;
			switch ( rotation ) {
				case Surface.ROTATION_0:
					degrees = 0;
					break;
				case Surface.ROTATION_90:
					degrees = 90;
					break;
				case Surface.ROTATION_180:
					degrees = 180;
					break;
				case Surface.ROTATION_270:
					degrees = 270;
					break;
			}
			dbMsg += ",degrees="+degrees;
			dbMsg += ".facing=" + info.facing;
			if ( info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
				orientationDeg = (info.orientation + degrees) % 360;
				orientationDeg = (360 - orientationDeg) % 360;  // compensate the mirror
			} else {  // back-facing
				orientationDeg = (info.orientation - degrees + 360) % 360;
			}
			dbMsg += ".orientationDeg=" + orientationDeg;
			/**
			 * rotation=0,degrees=0.facing=0.orientationDeg=90
			 * rotation=1,degrees=90.facing=0.orientationDeg=0
			 * rotation=3,degrees=270.facing=0.orientationDeg=180
			 * */
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return orientationDeg;
	}


	/**
	 * onCreateに有ったイベントなどの処理パート
	 * onCreateは終了処理後のonDestroyの後でも再度、呼び出されるので実データの割り付けなどを分離する
	 */
	public void laterCreate() {
		final String TAG = "laterCreate[MA]";
		String dbMsg = "";
		try {
//			if(! isCrated) {
//			getMyDiviceInfo();
//			displayRotation = getWindowManager().getDefaultDisplay().getRotation();
//			dbMsg += ",端末の向き=" + displayRotation;    //上端； 上=0,右=1,左=3,下=0
//			int orientation = getResources().getConfiguration().orientation;
//			dbMsg += ",orientation=" + orientation;
//			int orientationDeg = 90;
//			if ( orientation == Configuration.ORIENTATION_LANDSCAPE ) {            //2:端末の向き=1
//				dbMsg += "=横向き";
//			} else if ( orientation == Configuration.ORIENTATION_PORTRAIT ) {            //1
//				dbMsg += "=縦向き";
//				orientationDeg = 0;
//				// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);      //横向きに修正する場合
//			}
			if ( cameraView == null ) {
				cameraView = new CameraView(this , getCameraPreveiwDeg());
				activityMain.addView(cameraView);
			}
//			}
			isCrated = true;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void callQuit() {
		final String TAG = "callQuit[MA]";
		String dbMsg = "";
		try {
			if ( cameraView == null ) {
				cameraView.surfaceDestroy();
			}
			this.finish();
			if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
				finishAndRemoveTask();                      //アプリケーションのタスクを消去する事でデバッガーも停止する。
			} else {
				moveTaskToBack(true);                       //ホームボタン相当でアプリケーション全体が中断状態
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * assetsの内容を /data/data/.../files/ にコピーします。
	 */
	private void copyAssets(String dir) throws IOException {
		final String TAG = "copyAssets[MA]";
		String dbMsg = "";
		try {
			dbMsg = "dir=" + dir;
			dbMsg = ",認証ファイル最終更新日=" + haarcascadesLastModified;
			byte[] buf = new byte[8192];
			int size;
			boolean isCopy = false;    //初回使用時なと、強制的にコピーする
			File dst = new File(getFilesDir() , dir);
			if ( !dst.exists() ) {
				dst.mkdirs();
				dst.setReadable(true , false);
				dst.setWritable(true , false);
				dst.setExecutable(true , false);
				dbMsg += ">>作成";
				isCopy = true;
			}

			for ( String filename : getAssets().list(dir) ) {
				File file = new File(dst , filename);
				Long lastModified = file.lastModified();
				if ( isCopy || haarcascadesLastModified < lastModified ) {    //無ければ
					dbMsg += "," + filename + ";" + lastModified;
					haarcascadesLastModified = lastModified;
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
					dbMsg += ">>コピー";
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void getMyDiviceInfo() {
		final String TAG = "getMyDiviceInfo[MA]";
		String dbMsg = "";
		try {
			displayRotation = getWindowManager().getDefaultDisplay().getRotation();
			dbMsg += ",端末の向き=" + displayRotation;
			CameraManager cameraManager = ( CameraManager ) getSystemService(CAMERA_SERVICE);

			if ( Build.VERSION.SDK_INT >= 21 ) {

				ArrayList backIds = new ArrayList<>();
				try {
					String[] idList = cameraManager.getCameraIdList();
					for ( String id : idList ) {
						CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
						Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
						if ( lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK ) {
							dbMsg += ",id=" + id;
							backIds.add(id);
						}
					}
				} catch (CameraAccessException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}

				dbMsg += ",backIds=" + backIds.size() + "件" + backIds.get(0);
				try {
					CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(backIds.get(0).toString());
					Integer tempSO = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
					sensorOrientation = tempSO == null ? 0 : tempSO;
				} catch (CameraAccessException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					sensorOrientation = 0;
				}
			} else {
				//      http://acro-engineer.hatenablog.com/entry/20110824/1314200703
//				int numberOfCameras = Camera.getNumberOfCameras();
//  				for (int i = 0; i < numberOfCameras; i++) {      // 各カメラの情報を取得
//					CameraInfo caminfo = new CameraInfo();
//					Camera.getCameraInfo(i, caminfo);
//					int facing = caminfo.facing;         					// カメラの向きを取得
//					if (facing == CameraInfo.CAMERA_FACING_BACK) {         						// 後部についているカメラの場合
//						dbMsg += ",cameraId=" + Integer.toString(i) + ", this is a back-facing camera";
//					} else if (facing == CameraInfo.CAMERA_FACING_FRONT) {            					// フロントカメラの場合
//						dbMsg += ",cameraId=" + Integer.toString(i)+ ", this is a front-facing camera";
//					} else {
//						dbMsg += ",cameraId=" + Integer.toString(i)+ ", unknown camera?";
//					}
//					int orient = caminfo.orientation;   					// カメラのOrientation(角度) を取得
//					dbMsg += ",cameraId=" + Integer.toString(i)+ ", orientation=" + Integer.toString(orient);
//				}
			}
			/**
			 * 上端；
			 * 上；端末の向き=0,id=0,backIds=1件0,カメラの向き=90
			 * 右；端末の向き=1,id=0,backIds=1件0,カメラの向き=90
			 * 左；端末の向き=3,id=0,backIds=1件0,カメラの向き=90
			 * 下：端末の向き=0,id=0,backIds=1件0,カメラの向き=90
			 *
			 *
			 * */
			dbMsg += ",カメラの向き=" + sensorOrientation;

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
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
 2012-02-15			Androidで縦向き（Portrait）でカメラを使う方法　（主にAndroid2.x向け）		 http://dai1741.hatenablog.com/entry/2012/02/15/011114
 * <p>
 *横向きでアスペクト比崩れ
 * 下向きに追従せず
 * 廃止前メソッドの置換え
 * 終了時クラッシュ
 * 	 java.lang.RuntimeException: Camera is being used after Camera.release() was called
 * in-out切替
 */