package com.hijiyam_koubou.kaodori;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FdActivity extends Activity {

	static {
		System.loadLibrary("opencv_java3");    // opencv\_java3.so をロード	;	\jniLibsmの中のプロセッサー分、検索
	}

	public ViewGroup activityMain;            //プレビュー読込み場所
	public ImageButton fda_capture_bt;      //キャプチャーボタン
	public ImageButton fda_setting_bt ;      //設定ボタン

	public boolean isTextureView = false;            //プレビューにTextureViewを使用する
	public boolean isC2 = true;            //camera2を使用する
	public C1SurfaceView mySurfaceView;                //Surfaceのプレビュークラス
	public MyTextureView myTextureView;            //TextureViewのプレビュークラス
	public C2SurfaceView c2SufaceView;                //camera2でSurfaceのプレビュークラス
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
			if(prefs.up_scale != null ) {
				dbMsg += ",up_scale=" + prefs.up_scale;
				upScale = Float.parseFloat(prefs.up_scale);

				dbMsg += "," + getResources().getString(R.string.up_scale) + "=" + upScale;
			}
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
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			if ( isTextureView ) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);        //縦画面で止めておく	横	ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
				//方向固定するとonConfigurationChangedも一切発生しなくなる
			}
			setContentView(R.layout.face_detect_surface_view);
			activityMain = ( ViewGroup ) findViewById(R.id.fd_activity_surface_view);
			 fda_capture_bt = ( ImageButton ) findViewById(R.id.fda_capture_bt);      //キャプチャーボタン
			 fda_setting_bt = ( ImageButton ) findViewById(R.id.fda_setting_bt);      //設定ボタン

			try {
				copyAssets("haarcascades");                    // assetsの内容を /data/data/*/files/ にコピーします。
			} catch (IOException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
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
			if ( myTextureView != null ) {
//				camera2Dispose();
//				faceRecognition.frCameraManager.stopBackgroundThread();
			}
			if ( c2SufaceView.camera.mPreviewSession != null ) {
					try {
						c2SufaceView.camera.mPreviewSession.stopRepeating();
					} catch (CameraAccessException  er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
				c2SufaceView.camera.mPreviewSession.close();


				// カメラデバイスとの切断
				if (c2SufaceView.camera.mCamera != null) {
					c2SufaceView.camera.mCamera.close();
				}
			}
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
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Android7以降のマルチウインドウ
	 */
	@TargetApi ( Build.VERSION_CODES.N )
//	@Override
	public void onMultiWindowChanged(boolean isInMultiWindowMode) {
		super.onMultiWindowModeChanged(isInMultiWindowMode);
		final String TAG = "onMultiWindowChanged[MA]";
		String dbMsg = "isInMultiWindowMode=" + isInMultiWindowMode;
		try {
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 画面回転を検出
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final String TAG = "onConfigurationChanged[MA]";
		String dbMsg = "";
		try {

			int dispDegrees = getDisplayOrientation();
			dbMsg += ",Disp=" + dispDegrees + "dig";
			/**
			 * 上；,Disp=0dig,camera=90dig,screenLayout=268435794,orientation=1
			 * 右；Disp=90dig,camera=0dig,screenLayout=268435794,orientation=2
			 * 左；Disp=270dig,camera=180dig,screenLayout=268435794,orientation=2
			 * */
			dbMsg += ",screenLayout=" + newConfig.screenLayout;
			dbMsg += ",orientation=" + newConfig.orientation;
			if ( isTextureView ) {
//				faceRecognition.setDig2Cam(dispDegrees);
			} else {
				if ( isC2 ) {
					if(c2SufaceView.camera != null){
						c2SufaceView.camera.setPreviewSize();
					}
//					c2SufaceView.setDig2Cam(dispDegrees);
				} else {
					mySurfaceView.setDig2Cam(dispDegrees);
				}
			}
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


	public int getDisplayOrientation() {
		final String TAG = "getDisplayOrientation[MA]";
		String dbMsg = "";
		int degrees = 0;
		try {
			int rotation = getWindowManager().getDefaultDisplay().getRotation();   //helperからは((Activity)getContext()).
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
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return degrees;
	}

	/**
	 * onCreateに有ったイベントなどの処理パート
	 * onCreateは終了処理後のonDestroyの後でも再度、呼び出されるので実データの割り付けなどを分離する
	 */
	public void laterCreate() {
		final String TAG = "laterCreate[MA]";
		String dbMsg = "";
		try {
			fda_capture_bt.setOnClickListener(new View.OnClickListener() {         //キャプチャーボタン
				@Override
				public void onClick(View v) {
					final String TAG = "fda_capture_bt[MA]";
					String dbMsg = "";
					myLog(TAG , dbMsg);
					if ( isTextureView ) {
						if ( myTextureView != null ) {
						}
					} else if ( isC2 ) {
						if ( c2SufaceView.camera != null ) {
//							c2SufaceView.camera.copyPreview();
							c2SufaceView.captureStart();
						}
					} else {
						if ( mySurfaceView != null ) {
						}
					}
				}
			});
			
			fda_setting_bt.setOnClickListener(new View.OnClickListener() {       //設定ボタン
				@Override
				public void onClick(View v) {
					Intent settingsIntent = new Intent(FdActivity.this , MyPreferencesActivty.class);
					startActivityForResult(settingsIntent , REQUEST_PREF);					//    startActivity( settingsIntent );
				}
			});

			int dispDegrees = getDisplayOrientation();
			dbMsg += ",Disp=" + dispDegrees + "dig";
			/**
			 * 上；Disp=0dig,	camera=90dig
			 * 右；Disp=90dig,	camera=0dig
			 * 左；Disp=270dig,	camera=180dig
			 * */
			if ( isTextureView ) {
				if ( myTextureView == null ) {
					myTextureView = new MyTextureView(this , getDisplayOrientation());
					activityMain.addView(myTextureView);
				}
			} else if ( isC2 ) {
				if ( c2SufaceView == null ) {
					c2SufaceView = new C2SurfaceView(this , getDisplayOrientation(),writeFolder);            //camera2でSurfaceのプレビュークラス
					activityMain.addView(c2SufaceView);
				}
			} else {
				if ( mySurfaceView == null ) {
					mySurfaceView = new C1SurfaceView(this , dispDegrees);        //orgは90°固定だった
					activityMain.addView(mySurfaceView);
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void callQuit() {
		final String TAG = "callQuit[MA]";
		String dbMsg = "";
		try {
			if ( isTextureView ) {
				if ( myTextureView == null ) {
					myTextureView.surfaceDestroy();
				}
			} else if ( isC2 ) {
				if ( c2SufaceView == null ) {
					c2SufaceView.surfaceDestroy();
				}
			} else {
				if ( mySurfaceView == null ) {
					mySurfaceView.surfaceDestroy();
				}
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
			dbMsg += ",認証ファイル最終更新日=" + haarcascadesLastModified;
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
			int readedCount = dst.list().length;
			dbMsg += ",読込み済み=" + readedCount + "件";
			if ( readedCount < 10 ) {
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
 * 2017年01月02日	Androidデバイスのカメラの向き					 https://qiita.com/cattaka/items/330321cb8c258c535e07
 * 2012-02-15			Androidで縦向き（Portrait）でカメラを使う方法　（主にAndroid2.x向け）		 http://dai1741.hatenablog.com/entry/2012/02/15/011114
 * OpenCV 3.0.0 による顔検出処理		https://yamsat.wordpress.com/2015/09/13/opencv-3-0-0-%E3%81%AB%E3%82%88%E3%82%8B%E9%A1%94%E6%A4%9C%E5%87%BA%E5%87%A6%E7%90%86/
 * FaceDetectorで Bitmap から顔を検出する  	 https://dev.classmethod.jp/smartphone/android-tips-15-facedetector/
 * カメラプレビューで顔を検出する		https://dev.classmethod.jp/smartphone/android-tips-16-facedetectionlistener/
 * <p>
 * <p>
 * * 下向きに追従せず
 * 廃止前メソッドの置換え
 * 終了時クラッシュ     	Camera is being used after Camera.release() was called
 * java.lang.RuntimeException: Camera is being used after Camera.release() was called
 * <p>
 * E/mm-camera: <STATS_AF ><ERROR> 4436: af_port_handle_pdaf_stats: Fail to init buf divert ack ctrl
 * <p>
 * in-out切替
 * <p>
 * 残留問題
 * toolbarは組み込めない
 * <p>
 * <p>
 * org.opencv.android.JavaCameraView
 */