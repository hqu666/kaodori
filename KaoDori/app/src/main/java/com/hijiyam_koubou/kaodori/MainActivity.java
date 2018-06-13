package com.hijiyam_koubou.kaodori;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements View.OnClickListener {
	static {
		System.loadLibrary("opencv_java3");  // OpenCV使用クラスに必須
	}

	public CS_Util UTIL;
	public static boolean debugNow = true;

	public AutoFitTextureView mTextureView;
	public FrameLayout ma_effect_fl;        //OpenCVの呼び込み枠
	public OCVFaceRecognitionVeiw OCVFRV;            //顔検出View

	public ImageButton ma_shot_bt;      //キャプチャーボタン
	public ImageButton ma_func_bt;      //設定ボタン
	public static ImageView ma_iv;                    //撮影結果

	public static SharedPreferences sharedPref;
	public SharedPreferences.Editor myEditor;
	public String writeFolder = "";
	public float upScale = 1.2f;
	public long haarcascadesLastModified = 0;
	public boolean isRumbling = false;                        //シャッター音の鳴動

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
			if ( prefs.up_scale != null ) {
				dbMsg += ",up_scale=" + prefs.up_scale;
				CS_Util UTIL = new CS_Util();
				if ( UTIL.isFloatVal(prefs.up_scale) ) {
					upScale = Float.parseFloat(prefs.up_scale);
				} else {
					upScale = 2.0f;
				}
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
					Intent intent = new Intent();
					intent.setClass(this , this.getClass());
					this.startActivity(intent);
					this.finish();                    //http://attyo0.blog.fc2.com/blog-entry-9.html
//					readPref();        //ループする？
					break;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	//Life Cycle// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String TAG = "onCreate[MA]";
		String dbMsg = "";
		try {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_main);
			mTextureView = ( AutoFitTextureView ) findViewById(R.id.ma_aft);
			ma_effect_fl = ( FrameLayout ) findViewById(R.id.ma_effect_fl);        //OpenCVの呼び込み枠       ViewGroup

			ma_shot_bt = ( ImageButton ) findViewById(R.id.ma_shot_bt);      //キャプチャーボタン
			ma_func_bt = ( ImageButton ) findViewById(R.id.ma_func_bt);      //設定ボタン
			ma_iv = ( ImageView ) findViewById(R.id.ma_iv);                    //撮影結果
			ma_shot_bt.setOnClickListener(this);
			ma_func_bt.setOnClickListener(this);
			ma_iv.setOnClickListener(this);
//			findViewById(R.id.ma_shot_bt).setOnClickListener(this);
//			findViewById(R.id.ma_func_bt).setOnClickListener(this);
//			findViewById(R.id.ma_iv).setOnClickListener(this);
			mFile = new File(writeFolder , "pic.jpg");                 //getActivity().getExternalFilesDir(null)
			dbMsg += ",mFile=" + mFile.getParent();

			readPref();                    //同期させないとインストール時にパーミッションエラー発生

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
			startBackgroundThread();
			if ( mTextureView != null ) {
				if ( mTextureView.isAvailable() ) {
					int TVWIdht = mTextureView.getWidth();
					int TVHight = mTextureView.getHeight();
					dbMsg = "[" + TVWIdht + "×" + TVHight + "]";
//  openCamera(TVWIdht , TVHight);         //  onSurfaceTextureAvailable　へ
				} else {
					mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
				}
			} else {
				dbMsg += "mTextureView== null";
			}

			if ( isRumbling ) {            //=false;						//シャッター音の鳴動
				mSound = new MediaActionSound();                //撮影音をロードする
				mSound.load(MediaActionSound.SHUTTER_CLICK);
			}

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onPause() {
		final String TAG = "onPause[MA]";
		String dbMsg = "";
		try {
//			closeCamera();
//			stopBackgroundThread();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		super.onPause();
	}


	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		final String TAG = "onWindowFocusChanged[MA}";
		String dbMsg = "hasFocus=" + hasFocus;
		try {
			if ( hasFocus ) {
				setViewState();
//				if(mTextureView!= null){
//					if ( mTextureView.isAvailable() ) {
//						//Attempt to invoke virtual method 'boolean com.hijiyam_koubou.kaodori.AutoFitTextureView.isAvailable()' on a null object reference
//						openCamera(mTextureView.getWidth() , mTextureView.getHeight());
//					} else {
//						mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
//					}
//				} else{
//					dbMsg += "mTextureView== null";
//				}
			}
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
		final String TAG = "onMultiWindowChanged[MA}";
		String dbMsg = "isInMultiWindowMode=" + isInMultiWindowMode;
		try {
			setViewState();
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
		final String TAG = "onConfigurationChanged[FdA}";
		String dbMsg = "";
		try {
			setViewState();
			dbMsg += ",screenLayout=" + newConfig.screenLayout;
			dbMsg += ",orientation=" + newConfig.orientation;
//			if ( isTextureView ) {
////				faceRecognition.setDig2Cam(dispDegrees);
//			} else {
//				if ( isC2 ) {
//					if ( c2SufaceView.camera != null ) {
//						c2SufaceView.camera.setPreviewSize();
//					}
////					c2SufaceView.setDig2Cam(dispDegrees);
//				} else {
//					mySurfaceView.setDig2Cam(dispDegrees);
//				}
//			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown(int keyCode , KeyEvent event) {
		final String TAG = "onKeyDown[MA]";
		String dbMsg = "";
		try {
//			if ( keyCode == KeyEvent.KEYCODE_BACK ) {          // && mImageView.getVisibility() == View.VISIBLE
//				mTextureView.setVisibility(View.VISIBLE);
////				mImageView.setVisibility(View.INVISIBLE);
//				return false;
//			}
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
			//Attempt to invoke virtual method 'int android.widget.ImageView.getVisibility()' on a null object reference
		}
		return super.onKeyDown(keyCode , event);
	}

	@Override
	public void onClick(View view) {
		final String TAG = "onClick[MA]";
		String dbMsg = "";
		try {
			dbMsg += ",getId=" + view.getId();
			switch ( view.getId() ) {
				case R.id.ma_shot_bt: {
					dbMsg += "=ma_shot_bt";
					takePicture();
					break;
				}
				case R.id.ma_func_bt: {
					dbMsg += "=ma_func_bt";
					Activity activity = MainActivity.this;            //getActivity();
					if ( null != activity ) {
						Intent settingsIntent = new Intent(activity , MyPreferencesActivty.class);
						startActivityForResult(settingsIntent , REQUEST_PREF);                    //    startActivity( settingsIntent );

//					new AlertDialog.Builder(activity).setMessage(R.string.intro_message).setPositiveButton(android.R.string.ok , null).show();
					}
					break;
				}
				case R.id.ma_iv: {
					dbMsg += "=ma_iv";
					Activity activity = MainActivity.this;            //getActivity();
					if ( null != activity ) {
						Intent fIntent = new Intent(Intent.ACTION_GET_CONTENT);
						String fName = mFile.getPath();                    //フルパスファイル名
						dbMsg += ",fName=" + fName;
						fIntent.setData(Uri.parse(fName));       //これだけでは開かない
						fIntent.setType("image/*"); //fIntent.setDataAndType(Uri.parse(fName), "image/*");では関連無い処まで開く
						if ( fIntent.resolveActivity(getPackageManager()) != null ) {
							startActivity(fIntent);
						}
					}
				}
				break;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	//アプリケーション動作////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * プレビューの幅と角度を更新する
	 */
	public void setViewState() {
		final String TAG = "setViewState[MA}";
		String dbMsg = "";
		try {
			PREVIEW_WIDTH = mTextureView.getWidth();
			PREVIEW_HEIGHT = mTextureView.getHeight();
			dbMsg += "[" + PREVIEW_WIDTH + "×" + PREVIEW_HEIGHT + "]";
			if ( UTIL == null ) {
				UTIL = new CS_Util();
			}
			DISP_DEGREES = UTIL.getDisplayOrientation(this);
			dbMsg += ",Disp=" + DISP_DEGREES + "dig";

			/**
			 * 上；,Disp=0dig,camera=90dig,screenLayout=268435794,orientation=1
			 * 右；Disp=90dig,camera=0dig,screenLayout=268435794,orientation=2
			 * 左；Disp=270dig,camera=180dig,screenLayout=268435794,orientation=2
			 * */
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 終了時の清掃
	 */
	public void callQuit() {
		final String TAG = "callQuit[MA}";
		String dbMsg = "";
		try {
			if ( OCVFRV != null ) {
				OCVFRV.canvasRecycle();
			}

			closeCamera();
			stopBackgroundThread();
			this.finish();
			if ( Build.VERSION.SDK_INT >= 21 ) {
				finishAndRemoveTask();                      //アプリケーションのタスクを消去する事でデバッガーも停止する。
			} else {
				moveTaskToBack(true);                       //ホームボタン相当でアプリケーション全体が中断状態
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	//エフェクト////////////////////////////////////////////////////////////////////////////////////アプリケーション動作//

	/***/
	public void setEffectView() {
		final String TAG = "setEffectView[MA}";
		String dbMsg = "";
		try {
			//		shotBitmap = _shotBitmap;
			// 別スレッドを実行
			MainActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final String TAG = "setEffectView[MA]";
					String dbMsg = "";
					try {
						if ( ma_effect_fl != null ) {
							if ( OCVFRV == null ) {
								int cCount = ma_effect_fl.getChildCount();
								dbMsg += ",getChildCount=" + cCount;
								dbMsg += ",PREVIEW[" + PREVIEW_WIDTH + "×" + PREVIEW_HEIGHT + "]";  //表示サイズに変更する必要有り

								OCVFRV = new OCVFaceRecognitionVeiw(MainActivity.this , haarcascadesLastModified);            //顔検出View
								LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(PREVIEW_WIDTH , PREVIEW_HEIGHT);
								OCVFRV.setLayoutParams(layoutParams);
								ma_effect_fl.addView(OCVFRV);
								setEffectViewSize();
								dbMsg += ">>ChildCount=" + ma_effect_fl.getChildCount();
							} else {
								dbMsg += ",作成済み";
							}
						} else {
							dbMsg += ",ma_effect_fl = null";
						}
						myLog(TAG , dbMsg);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
				}
			});
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void setEffectViewSize() {
		final String TAG = "setEffectViewSize[MA}";
		String dbMsg = "";
		try {
			MainActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final String TAG = "setEffectViewSize[MA]";
					String dbMsg = "";
					try {
						dbMsg += ",ma_effect_fl(" + ma_effect_fl.getLeft() + "," + ma_effect_fl.getTop() + ")[" + ma_effect_fl.getWidth() + "×" + ma_effect_fl.getHeight() + "]";
						int sLeft = (ma_effect_fl.getWidth() - PREVIEW_WIDTH);
						if ( 0 < sLeft ) {
							sLeft = sLeft / 2;
						}
						int sTop = (ma_effect_fl.getHeight() - PREVIEW_HEIGHT);
						if ( 0 < sTop ) {
							sTop = sTop / 2;
						}
						dbMsg += ",shift(" + sLeft + "," + sTop + ")";
						dbMsg += "、現在[" + OCVFRV.getWidth() + "×" + OCVFRV.getHeight() + "]";
						ViewGroup.MarginLayoutParams layoutParams = ( ViewGroup.MarginLayoutParams ) OCVFRV.getLayoutParams();
						dbMsg += ",layoutParams[" + layoutParams.width + "×" + layoutParams.height + "]";
						layoutParams.leftMargin = sLeft;
						layoutParams.topMargin = sTop;
						layoutParams.width = PREVIEW_WIDTH;
						layoutParams.height = PREVIEW_HEIGHT;
						OCVFRV.setLayoutParams(layoutParams);
						OCVFRV.requestLayout();
						layoutParams = ( ViewGroup.MarginLayoutParams ) OCVFRV.getLayoutParams();
						dbMsg += ",>layoutParams(" + layoutParams.leftMargin + "×" + layoutParams.topMargin + ")[" + layoutParams.width + "×" + layoutParams.height + "]";
						dbMsg += ">>OCVFRV(" + OCVFRV.getLeft() + "," + OCVFRV.getTop() + ")s[" + OCVFRV.getWidth() + "×" + OCVFRV.getHeight() + "]";
						dbMsg += ",camera=" + mSensorOrientation + "dig";
						OCVFRV.setCondition(mSensorOrientation);
						myLog(TAG , dbMsg);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
				}
			});
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	//プレビュー////////////////////////////////////////////////////////////////////////////////////エフェクト//
	/**
	 * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
	 * {@link TextureView}.
	 */
	private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture texture , int width , int height) {
			final String TAG = "onSurfaceTextureAvailable[MA]";
			String dbMsg = "";
			try {
				dbMsg = "[" + width + "×" + height + "]";
				openCamera(width , height);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture texture , int width , int height) {
			final String TAG = "onSurfaceTextureSizeChanged[MA]";
			String dbMsg = "";
			try {
				PREVIEW_WIDTH = width;                    //mTextureView.getWidth();
				PREVIEW_HEIGHT = height;                //mTextureView.getHeight();
				dbMsg = "[" + width + "×" + height + "]";
				configureTransform(width , height);
				if ( OCVFRV != null ) {
					setEffectViewSize();
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
			final String TAG = "onSurfaceTextureDestroyed[MA]";
			String dbMsg = "";
			try {
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			return true;
		}

		private final Object mCaptureSync = new Object();
		private Bitmap mTempBitmap;
		private boolean mReqesutCaptureStillImage;

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture texture) {
			final String TAG = "onSurfaceTextureUpdated[MA]";
			String dbMsg = "";
			try {
				dbMsg = "onSurfaceTextureUpdated";
// Surface surface = new Surface(texture);  //までは出来る

//				surface.
//				synchronized (mCaptureSync) {
//					if (mReqesutCaptureStillImage) {
//						mReqesutCaptureStillImage = false;
//						if (mTempBitmap == null)
//							mTempBitmap = getBitmap(mPreviewSize.getWidth(),mPreviewSize.getHeight());
//						else
//							getBitmap(mTempBitmap);
//						mCaptureSync.notifyAll();
//					}
//				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

	};
	///カメラ/////////////////////////////////////////////////////////////////////////////////プレビュー//////
	private String mCameraId;        //ID of the current {@link CameraDevice}.
	private Surface surface;
	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
	private static final int REQUEST_CAMERA_PERMISSION = 1;

	static {
		ORIENTATIONS.append(Surface.ROTATION_0 , 90);
		ORIENTATIONS.append(Surface.ROTATION_90 , 0);
		ORIENTATIONS.append(Surface.ROTATION_180 , 270);
		ORIENTATIONS.append(Surface.ROTATION_270 , 180);
	}

	private static final String TAG = "Camera2BasicFragment";
	public boolean isPhotography = false;
	private static final int STATE_PREVIEW = 0;    //Camera state: Showing camera preview.
	private static final int STATE_WAITING_LOCK = 1;        // Camera state: Waiting for the focus to be locked.
	/**
	 * Camera state: Waiting for the exposure to be precapture state.
	 */
	private static final int STATE_WAITING_PRECAPTURE = 2;

	/**
	 * Camera state: Waiting for the exposure state to be something other than precapture.
	 */
	private static final int STATE_WAITING_NON_PRECAPTURE = 3;

	/**
	 * Camera state: Picture was taken.
	 */
	private static final int STATE_PICTURE_TAKEN = 4;
	/**
	 * 使用しているカメラの現状
	 * The current state of camera state for taking pictures.
	 * @see #mCaptureCallback
	 */
	private int mState = STATE_PREVIEW;

	/**
	 * Max preview width that is guaranteed by Camera2 API
	 */
	private static final int MAX_PREVIEW_WIDTH = 1920;

	/**
	 * Max preview height that is guaranteed by Camera2 API
	 */
	private static final int MAX_PREVIEW_HEIGHT = 1080;

	/**
	 * 実際に配置できたプレビュー幅
	 */
	private static int PREVIEW_WIDTH = MAX_PREVIEW_WIDTH;

	/**
	 * 実際に配置できたプレビュー高さ
	 */
	private static int PREVIEW_HEIGHT = MAX_PREVIEW_HEIGHT;

	/**
	 * 現在の端末の向き
	 */
	private static int DISP_DEGREES;


	/**
	 * A {@link CameraCaptureSession } for camera preview.
	 */
	private CameraCaptureSession mCaptureSession;
	private CameraDevice mCameraDevice;            // A reference to the opened {@link CameraDevice}.
	/**
	 * The {@link android.util.Size} of camera preview.
	 */
	private Size mPreviewSize;
	/**
	 * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
	 * manager.openCamera() メソッドで指定
	 */
	private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

		@Override
		public void onOpened(CameraDevice cameraDevice) {
			final String TAG = "mStateCallback.onOpened[MA]";
			String dbMsg = "";
			try {
				// This method is called when the camera is opened.  We start camera preview here.
				mCameraOpenCloseLock.release();
				mCameraDevice = cameraDevice;
				dbMsg += ",mCameraDevice = " + mCameraDevice;
				createCameraPreviewSession();
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onDisconnected(CameraDevice cameraDevice) {
			final String TAG = "mStateCallback.onDisconnected[MA]";
			String dbMsg = "";
			try {
				mCameraOpenCloseLock.release();
				cameraDevice.close();
				mCameraDevice = null;
				dbMsg += ",mCameraDevice = " + mCameraDevice;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onError(CameraDevice cameraDevice , int error) {
			final String TAG = "mStateCallback.onError[MA]";
			String dbMsg = "";
			mCameraOpenCloseLock.release();
			cameraDevice.close();
			mCameraDevice = null;
			Activity activity = MainActivity.this;    // getActivity();
			if ( null != activity ) {
				activity.finish();
			}
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + error);
		}
	};

	/**
	 * An additional thread for running tasks that shouldn't block the UI.
	 */
	private HandlerThread mBackgroundThread;
	private Handler mBackgroundHandler;        // A {@link Handler} for running tasks in the background.

	/**
	 * An {@link ImageReader} that handles still image capture.
	 */
	private ImageReader mImageReader;

	/**
	 * This is the output file for our picture.
	 */
	private File mFile;

	/**
	 * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a still image is ready to be saved.
	 * setUpCameraOutputsで設定
	 */
	private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

		@Override
		public void onImageAvailable(ImageReader reader) {
			final String TAG = "mOnImageAvailableListener[MA]";
			String dbMsg = "";
			try {
				dbMsg = "mState=" + mState;   //撮影時も STATE_PREVIEWになっている
				dbMsg += ",isPhotography=" + isPhotography;   //撮影時も falseになっている
				if ( !isPhotography ) {     //撮影中で無ければ
					SendPreview SP = new SendPreview(reader.acquireLatestImage());
					mBackgroundHandler.post(SP);
//					sendPreview(reader);
				} else {
					dbMsg += ",writeFolder=" + writeFolder;
					long timestamp = System.currentTimeMillis();
					dbMsg += ",timestamp=" + timestamp;
					File saveFolder = new File(writeFolder);
					if ( !saveFolder.exists() ) {
						saveFolder.mkdir();
						dbMsg += "作成";
					} else {
						dbMsg += "有り";
					}
					java.text.DateFormat df = new SimpleDateFormat("yyyy/MM/dd/HH:mm:ssZ" , Locale.JAPAN);
					String dtStr = df.format(timestamp);
					dbMsg += ",dtStr=" + dtStr;
					String[] dtStrs = dtStr.split("/");
					int lCount = 0;
					String pFolderName = saveFolder.getPath();
					for ( String rStr : dtStrs ) {
						dbMsg += "(" + lCount + ")" + rStr;
						if ( lCount < 3 ) {
							File pFolder = new File(pFolderName , rStr);
							if ( !pFolder.exists() ) {
								pFolder.mkdir();
								dbMsg += "作成";
							} else {
								dbMsg += "有り";
							}
							pFolderName = pFolder.toString();
							dbMsg += ",dtStr=" + pFolderName;
						}
						lCount++;
					}
					dtStr = dtStr.replaceAll("/" , "");
					dtStr = dtStr.replaceAll(":" , "");
					dtStr = dtStr.substring(0 , dtStr.length() - 5) + ".jpg";
					mFile = new File(pFolderName , dtStr);                 //getActivity().getExternalFilesDir(null)
					dbMsg += ",mFile=" + mFile.toString();
					//maxImages (2) has already been acquired, call #close before acquiring more.
					ImageSaver IS = new ImageSaver(reader.acquireNextImage() , pFolderName , ma_iv , dtStr);//第二引数以降は追加     /
					mBackgroundHandler.post(IS);
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	//プレビューの画像取得 ///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	private final ImageReader.OnImageAvailableListener mOnPreviwListener = new ImageReader.OnImageAvailableListener() {
//		@Override
//		public void onImageAvailable(ImageReader reader) {
//			final String TAG = "mOnPreviwListener[MA]";
//			String dbMsg = "";
//			try {
//				if ( OCVFRV != null ) {
//					dbMsg += ",completion=" + OCVFRV.getCompletion() ;
//					if ( OCVFRV.getCompletion() ) {
////			if( camera.mImageReader != null) {
//						Image image = reader.acquireLatestImage();
//						if ( image != null ) {
//							int width = image.getWidth();
//							int height = image.getHeight();
//							long timestamp = image.getTimestamp();
//							dbMsg += ",image[" + width + "×" + height + "]Format=" + image.getFormat();
//							dbMsg += ",=" + timestamp + "," + image.getPlanes().length + "枚";
//							ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
//							final byte[] imageBytes = new byte[imageBuf.remaining()];        //直接渡すと.ArrayIndexOutOfBoundsException: length=250,095; index=15,925,248
//							dbMsg += ",imageBytes=" + imageBytes.length;
//							imageBuf.get(imageBytes);
//
//							final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes , 0 , imageBytes.length);
//							ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//							bitmap.compress(Bitmap.CompressFormat.JPEG , 100 , byteArrayOutputStream);
//							dbMsg += ",bitmap[" + bitmap.getWidth() + "×" + bitmap.getHeight() + "]";
//							int byteCount = bitmap.getByteCount();
//							dbMsg += "" + byteCount + "バイト";
//
////					degrees = camera.getCameraRotation();
////					dbMsg += "," + degrees + "dig";
////						sendPreview(bitmap);
//							byteArrayOutputStream.close();
////						if ( bitmap != null ) {
////							bitmap.recycle();
////							byteCount = bitmap.getByteCount();
////							dbMsg += ">>" + byteCount + "バイト";
////						}
//						} else {
//							dbMsg += ",image = null ";
//						}
//						image.close();
////			}
//					} else {
//						dbMsg += ",getCompletion = false ";
//					}
//				} else {
//					dbMsg += ",OCVFRV = null ";
//				}
//				myLog(TAG , dbMsg);
//			} catch (Exception er) {
//				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//			}
//		}
//	};


	//	private	CaptureRequest copyPreviewRequest;
	//	private CaptureRequest.Builder mCopyPreviewRequestBuilder ;                    // 静止画を送ってもらうためのリクエストのビルダーですよ
	private void copyPreview() {
		final String TAG = "copyPreview[MA]";
		String dbMsg = "";
		try {
//			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);            // This is how to tell the camera to lock focus.

//			 ImageViewへ静止画を送るためのCaptureRequestを作る 	http://woshidan.hatenadiary.jp/entry/2017/09/10/022510
			CaptureRequest.Builder mCopyPreviewRequestBuilder = null;                    // 静止画を送ってもらうためのリクエストのビルダーですよ
			try {
				dbMsg += ",createCaptureRequest";
				mCopyPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);   //静止画像キャプチャに適した要求を作成
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			mCopyPreviewRequestBuilder.addTarget(mImageReader.getSurface());                        // 送り先はImageReaderにしてね
			CaptureRequest copyPreviewRequest = mCopyPreviewRequestBuilder.build();
			try {
				dbMsg += ",capture";
//				int retInt = mCaptureSession.capture(mPreviewRequestBuilder.build() , mCaptureCallback , mBackgroundHandler);    //
//				int retInt = mCaptureSession.capture(mPreviewRequestBuilder.build() ,null , null);	// 1から順次増加
				int retInt = mCaptureSession.capture(copyPreviewRequest , mCaptureCallback , mBackgroundHandler);                        // (プレビュー時にセッションは開いたままで、)追加で静止画送ってくれリクエストを送る
				dbMsg += ",retInt=" + retInt;
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

//	private final ImageReader.OnImageAvailableListener mOnPreviwListener = new ImageReader.OnImageAvailableListener() {
//
//		@Override
//		public void onImageAvailable(ImageReader reader) {
//			final String TAG = "mOnPreviwListener[MA]";
//			String dbMsg = "";
//			try {
//					sendPreview(reader);
//				myLog(TAG , dbMsg);
//			} catch (Exception er) {
//				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//			}
//		}
//	};

//	public ImageReader prevReader;
//	public void sendPreview(ImageReader _prevReader) {
//		prevReader = _prevReader;
////		shotBitmap = _shotBitmap;
//		// 別スレッドを実行
//		MainActivity.this.runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				final String TAG = "sendPreview[MA]";
//				String dbMsg = "";
//				try {
////					final Bitmap shotBitmap = null;
////					if ( OCVFRV != null ) {
////			if( camera.mImageReader != null) {
//					Image image = prevReader.acquireLatestImage();
//					if ( image != null ) {
//						int width = image.getWidth();
//						int height = image.getHeight();
//						long timestamp = image.getTimestamp();
//						dbMsg += ",image[" + width + "×" + height + "]Format=" + image.getFormat();
//						dbMsg += ",=" + timestamp + "," + image.getPlanes().length + "枚";
//						ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
//						final byte[] imageBytes = new byte[imageBuf.remaining()];        //直接渡すと.ArrayIndexOutOfBoundsException: length=250,095; index=15,925,248
//						dbMsg += ",imageBytes=" + imageBytes.length;
//						imageBuf.get(imageBytes);
//
//						final Bitmap shotBitmap = BitmapFactory.decodeByteArray(imageBytes , 0 , imageBytes.length);
//						ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//						shotBitmap.compress(Bitmap.CompressFormat.JPEG , 100 , byteArrayOutputStream);
//						dbMsg += ",bitmap[" + shotBitmap.getWidth() + "×" + shotBitmap.getHeight() + "]";
//						int byteCount = shotBitmap.getByteCount();
//						dbMsg += "" + byteCount + "バイト";
//						//			}
//						OCVFRV.readFrameRGB(shotBitmap);
//
//						if ( shotBitmap != null ) {
//							shotBitmap.recycle();
//							byteCount = shotBitmap.getByteCount();
//							dbMsg += ">>" + byteCount + "バイト";
//						}
//						image.close();
//					} else {
//						dbMsg += ",image = null ";
//					}
////					} else {
////						dbMsg += ",OCVFRV = null ";
////					}
//					myLog(TAG , dbMsg);
//				} catch (Exception er) {
//					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//				}
//			}
//		});
//	}


	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////プレビューの画像取得 ///

	private CaptureRequest.Builder mPreviewRequestBuilder;        // {@link CaptureRequest.Builder} for the camera preview
	private CaptureRequest mPreviewRequest;    //{@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}

	private Semaphore mCameraOpenCloseLock = new Semaphore(1);        // A {@link Semaphore} to prevent the app from exiting before closing the camera.

	/**
	 * フラッシュが使えるか否か
	 * Whether the current camera device supports Flash or not.
	 */
	private boolean mFlashSupported;

	/**
	 * カメラの角度
	 * Orientation of the camera sensor
	 */
	public int mSensorOrientation;
	public int fpsCount = 0;
	public int fpsLimi = 30;
	private MediaActionSound mSound;    //撮影音のためのMediaActionSound

	/**
	 * JPEG捕獲に関連したそのハンドル・イベント。
	 * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
	 */
	private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
		private void process(CaptureResult result) {
			final String TAG = "process[MA]";
			String dbMsg = "";
			try {
//				dbMsg += "result=" + result;
				dbMsg += ",mState=" + mState;
				switch ( mState ) {
					case STATE_PREVIEW: {                //0 ＜＜初期値とunlockFocus() 、We have nothing to do when the camera preview is working normally.
						dbMsg += "(" + fpsCount + "/" + fpsLimi + ")";
						if ( fpsCount < fpsLimi ) {
							fpsCount++;
							dbMsg = "";
						} else {
							if ( OCVFRV != null ) {
								dbMsg += ",completion=" + OCVFRV.getCompletion();
								if ( OCVFRV.getCompletion() ) {    //onDrawが終了するまでfalseが返る
									copyPreview();
//							} else {
//								dbMsg += ",getCompletion = false ";
								}
							} else {
								dbMsg += ",OCVFRV = null ";
								int TVWIdht = mTextureView.getWidth();
								int TVHight = mTextureView.getHeight();
								dbMsg += "[" + TVWIdht + "×" + TVHight + "]";
								setEffectView();
							}
							fpsCount = 0;
						}
						break;
					}
					case STATE_WAITING_LOCK: {        //1<<lockFocus()から
						/**
						 *  lockFocus() で
						 mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);            // This is how to tell the camera to lock focus.
						 mState = STATE_WAITING_LOCK;            // Tell #mCaptureCallback to wait for the lock.
						 mCaptureSession.capture(mPreviewRequestBuilder.build() , mCaptureCallback , mBackgroundHandler);
						 */
						Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
						dbMsg += ",afState=" + afState;
						if ( afState == null ) {
							captureStillPicture();
						} else if ( CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ) {
							// CONTROL_AE_STATE can be null on some devices
							Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
							if ( aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED ) {
								mState = STATE_PICTURE_TAKEN;  //4;
								captureStillPicture();
							} else {
								runPrecaptureSequence();
							}
						}
						isPhotography = true;
						break;
					}
					case STATE_WAITING_PRECAPTURE: {    //2	<runPrecaptureSequence// CONTROL_AE_STATE can be null on some devices
						Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
						dbMsg += ",aeState=" + aeState;
						if ( aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ) {
							mState = STATE_WAITING_NON_PRECAPTURE;     //3
						}
						break;
					}
					case STATE_WAITING_NON_PRECAPTURE: {    //3<STATE_WAITING_PRECAPTURE	// CONTROL_AE_STATE can be null on some devices
						Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
						dbMsg += ",aeState=" + aeState;
						if ( aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE ) {
							mState = STATE_PICTURE_TAKEN;
							captureStillPicture();
						}
						break;
					}
				}
				if ( !dbMsg.equals("") ) {
					dbMsg += ">>mState=" + mState;
					myLog(TAG , dbMsg);
				}
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onCaptureProgressed(CameraCaptureSession session , CaptureRequest request , CaptureResult partialResult) {
			final String TAG = "onCaptureProgressed[MA]";
			String dbMsg = "";
			try {
				process(partialResult);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}

		}

		@Override
		public void onCaptureCompleted(CameraCaptureSession session , CaptureRequest request , TotalCaptureResult result) {
			final String TAG = "MCC.onCaptureCompleted[MA]";
			String dbMsg = "";
			try {
				dbMsg += ",CaptureRequest=" + request.toString();    //CaptureRequest=android.hardware.camera2.CaptureRequest@ed0bb9ae,TotalCaptureResult=android.hardware.camera2
				dbMsg += ",TotalCaptureResult=" + result.toString();   // TotalCaptureResult@17e91b3
				process(result);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};
	//機器の状況取得////////////////////////////////////////////////////////////////////// /

	/**
	 * スクリーン回転の角度からカメラの角度を返す
	 * Retrieves the JPEG orientation from the specified screen rotation.
	 * @param rotation The screen rotation.
	 * @return The JPEG orientation (one of 0, 90, 270, and 360)
	 */
	private int getOrientation(int rotation) {
		final String TAG = "getOrientation[MA]";
		String dbMsg = "";
		int retInt = 0;
		try {
			dbMsg = "rotation=" + rotation;
			dbMsg += "、mSensorOrientation=" + mSensorOrientation;
			// Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
			// We have to take that into account and rotate JPEG properly.
			// For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
			// For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
	}

	/**
	 * Compares two {@code Size}s based on their areas.
	 */
	static class CompareSizesByArea implements Comparator< Size > {

		@Override
		public int compare(Size lhs , Size rhs) {
			final String TAG = "compare[MA]";
			String dbMsg = "";
			try {
				dbMsg += "lhs[" + lhs.getWidth() + "×" + lhs.getHeight() + "]";
				dbMsg += ",rhs[" + rhs.getWidth() + "×" + rhs.getHeight() + "]";
				// We cast here to ensure the multiplications won't overflow
				if ( debugNow ) {
					Log.i(TAG , dbMsg + "");
				}
			} catch (Exception er) {
				Log.e(TAG , dbMsg + "");
			}
			return Long.signum(( long ) lhs.getWidth() * lhs.getHeight() - ( long ) rhs.getWidth() * rhs.getHeight());
		}
	}

	/**
	 * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
	 * is at least as large as the respective texture view size, and that is at most as large as the
	 * respective max size, and whose aspect ratio matches with the specified value. If such size
	 * doesn't exist, choose the largest one that is at most as large as the respective max size,
	 * and whose aspect ratio matches with the specified value.
	 * @param choices           The list of sizes that the camera supports for the intended output
	 *                          class
	 * @param textureViewWidth  The width of the texture view relative to sensor coordinate
	 * @param textureViewHeight The height of the texture view relative to sensor coordinate
	 * @param maxWidth          The maximum width that can be chosen
	 * @param maxHeight         The maximum height that can be chosen
	 * @param aspectRatio       The aspect ratio
	 * @return The optimal {@code Size}, or an arbitrary one if none were big enough
	 */
	private static Size chooseOptimalSize(Size[] choices , int textureViewWidth , int textureViewHeight , int maxWidth , int maxHeight , Size aspectRatio) {
		final String TAG = "chooseOptimalSize[MA]";
		String dbMsg = "";
		Size retSize = null;
		try {
			// Collect the supported resolutions that are at least as big as the preview Surface
			List< Size > bigEnough = new ArrayList<>();
			// Collect the supported resolutions that are smaller than the preview Surface
			List< Size > notBigEnough = new ArrayList<>();
			int w = aspectRatio.getWidth();
			int h = aspectRatio.getHeight();
			for ( Size option : choices ) {
				if ( option.getWidth() <= maxWidth && option.getHeight() <= maxHeight && option.getHeight() == option.getWidth() * h / w ) {
					if ( option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight ) {
						bigEnough.add(option);
					} else {
						notBigEnough.add(option);
					}
				}
			}

			// Pick the smallest of those big enough. If there is no one big enough, pick the
			// largest of those not big enough.
			if ( bigEnough.size() > 0 ) {
				retSize = Collections.min(bigEnough , new CompareSizesByArea());
			} else if ( notBigEnough.size() > 0 ) {
				retSize = Collections.max(notBigEnough , new CompareSizesByArea());
			} else {
				dbMsg = "Couldn't find any suitable preview size";
				retSize = choices[0];
			}
			dbMsg += ",retSize=" + retSize;
			if ( debugNow ) {
				Log.i(TAG , dbMsg + "");
			}
		} catch (Exception er) {
			Log.e(TAG , dbMsg + "");
		}
		return retSize;
	}

	/**
	 * Sets up member variables related to camera.
	 * @param width  The width of available size for camera preview
	 * @param height The height of available size for camera preview
	 */
	@SuppressWarnings ( "SuspiciousNameCombination" )
	private void setUpCameraOutputs(int width , int height) {
		final String TAG = "setUpCameraOutputs[MA]";
		String dbMsg = "";
		try {
			Activity activity = this;                //getActivity();
			CameraManager manager = ( CameraManager ) activity.getSystemService(Context.CAMERA_SERVICE);
			try {
				for ( String cameraId : manager.getCameraIdList() ) {
					dbMsg += ",cameraId=" + cameraId;
					CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

					Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);                        // We don't use a front facing camera in this sample.
					dbMsg += ",facing=" + facing + "0;FRONT";
					if ( facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT ) {
						continue;
					}

					StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
					if ( map == null ) {
						continue;
					}

					// For still image captures, we use the largest available size.
					Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)) , ( Comparator< ? super Size > ) new CompareSizesByArea());
					dbMsg += "m,largest[" + largest.getWidth() + "×" + largest.getHeight() + "]";
					mImageReader = ImageReader.newInstance(largest.getWidth() , largest.getHeight() , ImageFormat.JPEG , /*maxImages*/2);
					mImageReader.setOnImageAvailableListener(mOnImageAvailableListener , mBackgroundHandler);
//					mImageReader.setOnImageAvailableListener(mOnPreviwListener , mBackgroundHandler);            //プレビューの画像取得

					// Find out if we need to swap dimension to get the preview size relative to sensor coordinate.
					int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
					dbMsg += ",displayRotation=" + displayRotation;
					//noinspection ConstantConditions
					mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
					dbMsg += ",mSensorOrientation=" + mSensorOrientation;
					boolean swappedDimensions = false;
					switch ( displayRotation ) {
						case Surface.ROTATION_0:
						case Surface.ROTATION_180:
							if ( mSensorOrientation == 90 || mSensorOrientation == 270 ) {
								swappedDimensions = true;
							}
							break;
						case Surface.ROTATION_90:
						case Surface.ROTATION_270:
							if ( mSensorOrientation == 0 || mSensorOrientation == 180 ) {
								swappedDimensions = true;
							}
							break;
						default:
							dbMsg += "Display rotation is invalid: " + displayRotation;
					}

					Point displaySize = new Point();
					activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
					int rotatedPreviewWidth = width;
					int rotatedPreviewHeight = height;
					int maxPreviewWidth = displaySize.x;
					int maxPreviewHeight = displaySize.y;

					if ( swappedDimensions ) {
						rotatedPreviewWidth = height;
						rotatedPreviewHeight = width;
						maxPreviewWidth = displaySize.y;
						maxPreviewHeight = displaySize.x;
					}

					if ( maxPreviewWidth > MAX_PREVIEW_WIDTH ) {
						maxPreviewWidth = MAX_PREVIEW_WIDTH;
					}

					if ( maxPreviewHeight > MAX_PREVIEW_HEIGHT ) {
						maxPreviewHeight = MAX_PREVIEW_HEIGHT;
					}
					dbMsg += ",rotatedPreview[" + rotatedPreviewWidth + "×" + rotatedPreviewHeight + "]";
					dbMsg += ",maxPreview[" + maxPreviewWidth + "×" + maxPreviewHeight + "]";

					// Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
					// bus' bandwidth limitation, resulting in gorgeous previews but the storage of garbage capture data.
					mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class) , rotatedPreviewWidth , rotatedPreviewHeight , maxPreviewWidth , maxPreviewHeight , largest);

					// We fit the aspect ratio of TextureView to the size of preview we picked.
					int orientation = getResources().getConfiguration().orientation;
					dbMsg += ",orientation=" + orientation;
					if ( orientation == Configuration.ORIENTATION_LANDSCAPE ) {
						mTextureView.setAspectRatio(mPreviewSize.getWidth() , mPreviewSize.getHeight());
					} else {
						mTextureView.setAspectRatio(mPreviewSize.getHeight() , mPreviewSize.getWidth());
					}
					dbMsg += ",Preview[" + mPreviewSize.getWidth() + "×" + mPreviewSize.getHeight() + "]";

					// Check if the flash is supported.
					Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
					mFlashSupported = available == null ? false : available;
					dbMsg += ",mFlashSupported=" + mFlashSupported;

					mCameraId = cameraId;
					return;
				}
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			} catch (NullPointerException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				// Currently an NPE is thrown when the Camera2API is used but not supported on the
				// device this code runs.
//				ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager() , FRAGMENT_DIALOG);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	// Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.

	/**
	 * mCameraIdで指定されたカメラを開けます
	 * onResumeから呼ばれる
	 */
	@SuppressLint ( "MissingPermission" )
	private void openCamera(int width , int height) {
		final String TAG = "openCamera[MA]";
		String dbMsg = "";
		try {
			dbMsg = "[" + width + "×" + height + "]";
			setUpCameraOutputs(width , height);
			configureTransform(width , height);
			Activity activity = MainActivity.this;            //getActivity();
			CameraManager manager = ( CameraManager ) activity.getSystemService(Context.CAMERA_SERVICE);
			try {
				if ( !mCameraOpenCloseLock.tryAcquire(2500 , TimeUnit.MILLISECONDS) ) {
					throw new RuntimeException("Time out waiting to lock camera opening.");
				}
				dbMsg += ",mCameraId=" + mCameraId;
				dbMsg += ",mStateCallback=" + mStateCallback;
				dbMsg += ",mBackgroundHandler=" + mBackgroundHandler;
				manager.openCamera(mCameraId , mStateCallback , mBackgroundHandler);
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			} catch (InterruptedException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	/**
	 * Closes the current {@link CameraDevice}.
	 */
	private void closeCamera() {
		final String TAG = "closeCamera[MA]";
		String dbMsg = "";
		try {
			try {
				dbMsg += "mCaptureSession=" + mCaptureSession;
				mCameraOpenCloseLock.acquire();
				dbMsg = "";
				if ( null != mCaptureSession ) {
					mCaptureSession.close();
					mCaptureSession = null;
				}
				if ( null != mCameraDevice ) {
					mCameraDevice.close();
					mCameraDevice = null;
				}
				if ( null != mImageReader ) {
					mImageReader.close();
					mImageReader = null;
				}
				myLog(TAG , dbMsg);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while trying to lock camera closing." , e);
			} finally {
				mCameraOpenCloseLock.release();
			}
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	/**
	 * Starts a background thread and its {@link Handler}.
	 * onResumeでスタート
	 */
	private void startBackgroundThread() {
		final String TAG = "startBackgroundThread[MA]";
		String dbMsg = "";
		try {
			mBackgroundThread = new HandlerThread("CameraBackground");
			mBackgroundThread.start();
			mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	/**
	 * Stops the background thread and its {@link Handler}.
	 * Quit処理
	 */
	private void stopBackgroundThread() {
		final String TAG = "stopBackgroundThread[MA]";
		String dbMsg = "";
		try {
			mBackgroundThread.quitSafely();
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
			myLog(TAG , dbMsg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	/**
	 * Creates a new {@link CameraCaptureSession} for camera preview.
	 * onOpened	から
	 */
	private void createCameraPreviewSession() {
		final String TAG = "createCameraPreviewSession[MA]";
		String dbMsg = "";
		try {
			SurfaceTexture texture = mTextureView.getSurfaceTexture();
			assert texture != null;
			int tWidth = mPreviewSize.getWidth();
			int tHight = mPreviewSize.getHeight();
			dbMsg = "PreviewSize[" + tWidth + "×" + tHight + "]";
			texture.setDefaultBufferSize(tWidth , tHight);     // バッファサイズを、プレビューサイズに合わせる
			surface = new Surface(texture);   // プレビューが描画されるSurface	This is the output Surface we need to start preview.
			mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);    //(5)CaptureRequest作成	 カメラのプレビューウィンドウに適した;We set up a CaptureRequest.Builder with the output Surface.
			mPreviewRequestBuilder.addTarget(surface);
			dbMsg += ",surface=" + surface.toString();
			// (6)プレビュー用のセッション生成を要求する// Here, we create a CameraCaptureSession for camera preview.
			mCameraDevice.createCaptureSession(Arrays.asList(surface , mImageReader.getSurface()) , new CameraCaptureSession.StateCallback() {
				@Override
				public void onConfigured(CameraCaptureSession cameraCaptureSession) {
					final String TAG = "CCPS.onConfigured[MA]";
					String dbMsg = "";
					try {
						if ( null != mCameraDevice ) {  // カメラが閉じていなければ	// The camera is already closed
							mCaptureSession = cameraCaptureSession;        // When the session is ready, we start displaying the preview.
							dbMsg += ",getId=" + mCaptureSession.getDevice().getId();
							try {
								mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
								// オートフォーカスを設定する// Auto focus should be continuous for camera preview.
								setAutoFlash(mPreviewRequestBuilder);        // Flash is automatically enabled when necessary.
								mPreviewRequest = mPreviewRequestBuilder.build();        // リクエスト作成// Finally, we start displaying the camera preview.
								mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler);
								//(7)RepeatSession作成 カメラプレビューを表示する
								//APIL21;このキャプチャセッションで、イメージのキャプチャを無限に繰り返すように要求:ここの他は unlockFocus()
							} catch (CameraAccessException er) {
								myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
							}
						} else {
							dbMsg += "mCameraDevice = null";
						}
						myLog(TAG , dbMsg);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
				}

				@Override
				public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
					final String TAG = "CCPS.onConfigureFailed[MA]";
					String dbMsg = "";
					dbMsg += ",getId=" + cameraCaptureSession.getDevice().getId();
					showToast("Failed");
					myErrorLog(TAG , dbMsg + "発生；");
				}
			} , null);
			dbMsg += ",mPreviewRequestBuilder=" + mPreviewRequestBuilder.toString();

			DisplayMetrics metrics = MainActivity.this.getResources().getDisplayMetrics();
			int r = ( int ) (4 * metrics.density);

			PointF[] focusPoints = {new PointF(tWidth / 2 /r, tHight / 2/r)};
			dbMsg += ",focusPoints(" + focusPoints[0].x + "," + focusPoints[0].x + ")";
			startAutoFocus(focusPoints , MainActivity.this);
			myLog(TAG , dbMsg);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 『mTextureView』への変化に応じて必要なものを構成します。
	 * カメラ・プレビュー・サイズが中で測定されたあと、このmethodが呼ばれなければなりません
	 * setUpCameraOutputs、更には『mTextureView』のサイズは、固定されます。
	 * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
	 * This method should be called after the camera preview size is determined in
	 * setUpCameraOutputs and also the size of `mTextureView` is fixed.
	 * @param viewWidth  The width of `mTextureView`
	 * @param viewHeight The height of `mTextureView`
	 *                   onSurfaceTextureSizeChanged、openCameraから呼ばれる
	 */
	private void configureTransform(int viewWidth , int viewHeight) {
		final String TAG = "configureTransform[MA]";
		String dbMsg = "";
		try {
			Activity activity = MainActivity.this;                //getActivity();
			if ( null == mTextureView || null == mPreviewSize || null == activity ) {
				return;
			}
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			dbMsg += ",rotation=" + rotation;
			Matrix matrix = new Matrix();
			RectF viewRect = new RectF(0 , 0 , viewWidth , viewHeight);
			RectF bufferRect = new RectF(0 , 0 , mPreviewSize.getHeight() , mPreviewSize.getWidth());
			float centerX = viewRect.centerX();
			float centerY = viewRect.centerY();
			dbMsg += ",center(" + centerY + "," + centerY + ")";
			if ( Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation ) {
				bufferRect.offset(centerX - bufferRect.centerX() , centerY - bufferRect.centerY());
				matrix.setRectToRect(viewRect , bufferRect , Matrix.ScaleToFit.FILL);
				float scale = Math.max(( float ) viewHeight / mPreviewSize.getHeight() , ( float ) viewWidth / mPreviewSize.getWidth());
				matrix.postScale(scale , scale , centerX , centerY);
				matrix.postRotate(90 * (rotation - 2) , centerX , centerY);
			} else if ( Surface.ROTATION_180 == rotation ) {
				matrix.postRotate(180 , centerX , centerY);
			}
			mTextureView.setTransform(matrix);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	///フォーカス設定////////////////////////////////////////////////////////////////////
	//https://moewe-net.com/android/2016/camera2-af
	private static final int STATE_INIT = -1;
	private static final int STATE_WAITING_LOCK_FUCUS = 0;                    //STATE_WAITING_LOCK
	private static final int STATE_WAITING_PRE_CAPTURE = 11;            //1
	private static final int STATE_WAITING_NON_PRE_CAPTURE = 12;        //2
	private static final int AF_SAME_STATE_REPEAT_MAX = 20;

	private int fState;
	private int mSameAFStateCount;
	private int mPreAFState;

	/**
	 * focusPointsにオートフォーカスする
	 **/
	public void startAutoFocus(PointF[] focusPoints , Context context) {
		final String TAG = "startAutoFocus[MA]";
		String dbMsg = "";
		try {
			int maxRegionsAF = 0;
			Rect activeArraySize = null;
			CameraManager cameraManager = ( CameraManager ) context.getSystemService(Context.CAMERA_SERVICE);
			try {
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraId);
				maxRegionsAF = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
				activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			if ( activeArraySize == null ) {
				activeArraySize = new Rect();
			}

			if ( maxRegionsAF <= 0 ) {
				return;
			}
			if ( focusPoints == null ) {
				return;
			}

			// フォーカス範囲
			DisplayMetrics metrics = context.getResources().getDisplayMetrics();
			int r = ( int ) (4 * metrics.density);
			dbMsg += ",r=" + r +"分割？";
			dbMsg += ",focusPoints=" + focusPoints.length + "件";
			dbMsg += ",activeArraySize[" + activeArraySize.width() + "×" + activeArraySize.height() + "]";

			MeteringRectangle[] afRegions = new MeteringRectangle[focusPoints.length];
			for ( int i = 0 ; i < focusPoints.length ; i++ ) {
				dbMsg += "(" + i + ")[" + focusPoints[i].x + "×" + focusPoints[i].y + "]";
				int x = ( int ) (activeArraySize.width() * focusPoints[i].x);
				int y = ( int ) (activeArraySize.height() * focusPoints[i].y);
				dbMsg += ",>activeArray(" + x + "," + y + ")";
				int rectLeft = Math.max(activeArraySize.bottom , x - r);
				int rectTop = Math.max(activeArraySize.top , y - r);
				int rectRight = Math.min(x + r , activeArraySize.right);
				int rectBottom = Math.min(y + r , activeArraySize.bottom);
				dbMsg += ",rect(" + rectLeft + "," + rectTop + ")～(" + rectRight + "×" + rectBottom + ")";
				//(3363840,1892160)(3363828,1892148)～(4680×3512)
				Rect p = new Rect(rectTop , rectLeft , rectRight , rectBottom);
				afRegions[i] = new MeteringRectangle(p , MeteringRectangle.METERING_WEIGHT_MAX);
			}
			dbMsg += ",afRegions=" + afRegions.length + "件";

			// 状態初期化
			fState = STATE_WAITING_LOCK_FUCUS;
			mSameAFStateCount = 0;
			mPreAFState = -1;
			try {
				CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
				if ( null != surface ) {                                     //   if (null != mPreviewSurface) {
					captureBuilder.addTarget(surface);  //        if (null != mPreviewSurface) {
				}
				captureBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_AUTO);
				if ( 0 < afRegions.length ) {
					captureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS , afRegions);
				}
				captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);
				mCaptureSession.setRepeatingRequest(captureBuilder.build() , mAFListener , mBackgroundHandler);
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	CameraCaptureSession.CaptureCallback mAFListener = new CameraCaptureSession.CaptureCallback() {
		@Override
		public void onCaptureCompleted(CameraCaptureSession session , CaptureRequest request , TotalCaptureResult result) {
			super.onCaptureCompleted(session , request , result);
			final String TAG = "FL.onCaptureCompleted[MA]";
			String dbMsg = "";
			try {
				if ( fState == STATE_WAITING_LOCK_FUCUS ) {
					Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
					if ( afState == null ) {
						Log.w(TAG , "onCaptureCompleted AF STATE is null");
						fState = STATE_INIT;
						autoFocusEnd(false);
						return;
					}

					if ( afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED ) {
						Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
						Log.i(TAG , "onCaptureCompleted AF STATE = " + afState + ", AE STATE = " + aeState);
						if ( (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) ) {        //mCancel ||
							fState = STATE_INIT;
							autoFocusEnd(false);
							return;
						}
					}

					if ( afState != CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN && afState == mPreAFState ) {
						mSameAFStateCount++;
						// 同一状態上限
						if ( mSameAFStateCount >= AF_SAME_STATE_REPEAT_MAX ) {
							fState = STATE_INIT;
							autoFocusEnd(false);
							return;
						}
					} else {
						mSameAFStateCount = 0;
					}
					mPreAFState = afState;
					return;
				}

				if ( fState == STATE_WAITING_PRE_CAPTURE ) {
					Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
					Log.i(TAG , "WAITING_PRE_CAPTURE AE STATE = " + aeState);
					if ( aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ) {
						fState = STATE_WAITING_NON_PRE_CAPTURE;
					} else if ( aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED ) {
						fState = STATE_INIT;
						autoFocusEnd(true);
					}
					return;
				}

				if ( fState == STATE_WAITING_NON_PRE_CAPTURE ) {
					Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
					Log.i(TAG , "WAITING_NON_PRE_CAPTURE AE STATE = " + aeState);
					if ( aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE ) {
						fState = STATE_INIT;
						autoFocusEnd(true);
					}
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		private void autoFocusEnd(boolean isSuccess) {
			final String TAG = "FL.autoFocusEnd[MA]";
			String dbMsg = "";
			try {
				// フォーカス完了/失敗時の処理
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	///撮影操作////////////////////////////////////////////////////////////////フォーカス設定////

	/**
	 * レリースボタンクリックで呼ばれる
	 * Initiate a still image capture.
	 */
	private void takePicture() {
		final String TAG = "takePicture[MA]";
		String dbMsg = "開始";
		try {
			isPhotography = true;
			lockFocus();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 静止画像捕獲のための第一歩として、焦点をロックしてください。
	 * Lock the focus as the first step for a still image capture.
	 */
	private void lockFocus() {
		final String TAG = "lockFocus[MA]";
		String dbMsg = "開始";
		try {
			if ( mPreviewRequestBuilder != null ) {
				mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);            // This is how to tell the camera to lock focus.
				mState = STATE_WAITING_LOCK;            // Tell #mCaptureCallback to wait for the lock.
				mCaptureSession.capture(mPreviewRequestBuilder.build() , mCaptureCallback , mBackgroundHandler);
			} else {
				dbMsg = "mPreviewRequestBuilder== null";
			}
			dbMsg += ",mState= " + mState;
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 静止画像を捕えるために、プレ捕獲シーケンスを走らせてください。
	 * 我々が反応を中で｛@link #mCaptureCallback｝得る｛@link #lockFocus（）｝とき、この方法は呼ばれなければなりません。
	 * processで STATE_WAITING_LOCK の時に呼ばれる  <<  lockFocus()で　mState = STATE_WAITING_LOCK
	 * Run the precapture sequence for capturing a still image.
	 * This method should be called when we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
	 */
	private void runPrecaptureSequence() {
		final String TAG = "runPrecaptureSequence[MA]";
		String dbMsg = "";
		try {
			// This is how to tell the camera to trigger.
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER , CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
			// Tell #mCaptureCallback to wait for the precapture sequence to be set.
			mState = STATE_WAITING_PRECAPTURE;
			mCaptureSession.capture(mPreviewRequestBuilder.build() , mCaptureCallback , mBackgroundHandler);
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * スチール写真を捕えてください。 我々が反応を入れるとき、この方法は呼ばれなければなりません
	 * Capture a still picture. This method should be called when we get a response in
	 * {@link #mCaptureCallback} from both {@link #lockFocus()}.
	 */
	private void captureStillPicture() {
		final String TAG = "captureStillPicture[MA]";
		String dbMsg = "";
		try {
			final Activity activity = MainActivity.this;                //getActivity();
			if ( null == activity || null == mCameraDevice ) {
				return;
			}
			// 撮影用のCaptureRequestを設定する	// This is the CaptureRequest.Builder that we use to take a picture.
			final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);    //静止画像キャプチャに適した
			captureBuilder.addTarget(mImageReader.getSurface());  // キャプチャ結果をImageReaderに渡す

			// オートフォーカス// Use the same AE and AF modes as the preview.
			captureBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			setAutoFlash(captureBuilder);
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			dbMsg += ",端末の回転角=" + rotation;
			int camLotation = getOrientation(rotation);
			dbMsg += ",カメラセンサーの方向=" + camLotation;
			captureBuilder.set(CaptureRequest.JPEG_ORIENTATION , camLotation);        // JPEG画像の方向を設定する。
			// 撮影が終わったら、フォーカスのロックを外すためのコールバック
			CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
				@Override
				public void onCaptureCompleted(CameraCaptureSession session , CaptureRequest request , TotalCaptureResult result) {
					final String TAG = "CSP.onCaptureCompleted[MA]";
					String dbMsg = "";
					try {
						dbMsg += "Saved: " + mFile.getPath();
						Thread.sleep(2000);            //暫定；このタイミングではmOnImageAvailableListenerに到達していないので待たせる
						unlockFocus();
						dbMsg += "保存終了";
						myLog(TAG , dbMsg);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
				}
			};
			if ( isRumbling ) {                                //シャッター音の鳴動
				mSound.play(MediaActionSound.SHUTTER_CLICK);            // 撮影音を鳴らす
			}
			mCaptureSession.stopRepeating();
			mCaptureSession.abortCaptures();
			mCaptureSession.capture(captureBuilder.build() , CaptureCallback , null);  // 撮影する
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * lockFocusを解除する。
	 * 静止画撮影が終わる時、このmethodが呼ばれなければなりません。
	 * Unlock the focus. This method should be called when still image capture sequence is finished.
	 */
	private void unlockFocus() {
		final String TAG = "unlockFocus[MA]";
		String dbMsg = "";
		try {
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
			// オートフォーカストリガーを外す// Reset the auto-focus trigger
			setAutoFlash(mPreviewRequestBuilder);
			mCaptureSession.capture(mPreviewRequestBuilder.build() , mCaptureCallback , mBackgroundHandler);
			mState = STATE_PREVIEW;
			mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler);
			// プレビューに戻る// After this, the camera will go back to the normal state of preview.
			//APIL21;このキャプチャセッションで、イメージのキャプチャを無限に繰り返すように要求:ここの他は onConfigured
//			isPhotography = false;
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
		final String TAG = "setAutoFlash[MA]";
		String dbMsg = "";
		try {
			if ( mFlashSupported ) {
				requestBuilder.set(CaptureRequest.CONTROL_AE_MODE , CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 指定された名称でJPEGファイルを保存する
	 * Saves a JPEG {@link Image} into the specified {@link File}.
	 */
	private class ImageSaver implements Runnable {        //static ?必要？
		/**
		 * The JPEG image
		 */
		private Image mImage;
		/**
		 * The file we save the image into.
		 */
		private File mFile;
		private File saveFolder;
		private String saveFolderName;
		private ImageView ma_iv;
		private String saveFileName;

		ImageSaver(Image image , String _saveFolderName , ImageView _ma_iv , String _saveFileName) {                //static
			final String TAG = "ImageSaver[MA]";
			String dbMsg = "";
			try {
				mImage = image;
				saveFolderName = _saveFolderName;
				ma_iv = _ma_iv;
				saveFileName = _saveFileName;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				isPhotography = false;
			}
		}

		@Override
		public void run() {
			final String TAG = "ImageSaver.run[MA]";
			String dbMsg = "";
			try {

				FileOutputStream output = null;
				try {
					dbMsg += ",saveFolder=" + saveFolderName;
					mFile = new File(saveFolderName , saveFileName);                 //getActivity().getExternalFilesDir(null)
					dbMsg += ",mFile=" + mFile.toString();
					output = new FileOutputStream(mFile);
					int width = mImage.getWidth();
					int height = mImage.getHeight();
					dbMsg += ",image[" + width + "×" + height + "]Format=" + mImage.getFormat() + "," + mImage.getPlanes().length + "枚";
					ByteBuffer imageBuf = mImage.getPlanes()[0].getBuffer();
					byte[] bytes = new byte[imageBuf.remaining()];
					dbMsg += ",bytes=" + bytes.length + "バイト";
					imageBuf.get(bytes);

					output.write(bytes);                    //書込み

					Bitmap shotBitmap = BitmapFactory.decodeByteArray(bytes , 0 , bytes.length);
//					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					shotBitmap.compress(Bitmap.CompressFormat.JPEG , 100 , output);       //output
					Double bmWidth = shotBitmap.getWidth() * 1.0;
					Double bmHeigh = shotBitmap.getHeight() * 1.0;
					dbMsg += ",bitmap[" + bmWidth + "×" + bmHeigh + "]";
					int byteCount = shotBitmap.getByteCount();
					dbMsg += "" + byteCount + "バイト";
					setThumbnail(shotBitmap);
				} catch (IOException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					isPhotography = false;
				} finally {
					mImage.close();
					if ( null != output ) {
						try {
							output.close();
						} catch (IOException er) {
							myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
						}
					}
				}
				isPhotography = false;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				isPhotography = false;
			}
		}
	}

	private static Bitmap shotBitmap;

	public void setThumbnail(Bitmap _shotBitmap) {
		shotBitmap = _shotBitmap;
		// 別スレッドを実行
		MainActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final String TAG = "setThumbnail[MA]";
				String dbMsg = "";
				try {
					int ivWidth = ma_iv.getWidth();
					int ivHeight = ma_iv.getHeight();
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

	///プレビューデータ操作//////////////////////////////////////////////////////////////////撮影操作//
	private class SendPreview implements Runnable {        //static ?必要？
		/**
		 * The JPEG image
		 */
		private Image mImage;

		/**
		 * The file we save the image into.
		 */

		SendPreview(Image image) {                //static                , String _saveFolderName , ImageView _ma_iv , String _saveFileName
			final String TAG = "SendPreview[MA]";
			String dbMsg = "";
			try {
				mImage = image;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void run() {
			final String TAG = "SendPreview.run[MA]";
			String dbMsg = "";
			try {
//				if ( mImage != null ) {
				int width = mImage.getWidth();
				int height = mImage.getHeight();
				long timestamp = mImage.getTimestamp();
				dbMsg += ",image[" + width + "×" + height + "]Format=" + mImage.getFormat();
				dbMsg += ",=" + timestamp + "," + mImage.getPlanes().length + "枚";
				ByteBuffer imageBuf = mImage.getPlanes()[0].getBuffer();
				final byte[] imageBytes = new byte[imageBuf.remaining()];        //直接渡すと.ArrayIndexOutOfBoundsException: length=250,095; index=15,925,248
				dbMsg += ",imageBytes=" + imageBytes.length;
				imageBuf.get(imageBytes);

				final Bitmap shotBitmap = BitmapFactory.decodeByteArray(imageBytes , 0 , imageBytes.length);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				shotBitmap.compress(Bitmap.CompressFormat.JPEG , 100 , byteArrayOutputStream);
				dbMsg += ",bitmap[" + shotBitmap.getWidth() + "×" + shotBitmap.getHeight() + "]";
				int byteCount = shotBitmap.getByteCount();
				dbMsg += "" + byteCount + "バイト";
				//			}
				OCVFRV.readFrameRGB(shotBitmap);

				if ( shotBitmap != null ) {
					shotBitmap.recycle();
					byteCount = shotBitmap.getByteCount();
					dbMsg += ">>" + byteCount + "バイト";
				}
				mImage.close();
//				} else {
//					dbMsg += ",image = null ";
//				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	}
	/////////////////////////////////////////////////////////////////////プレビューデータ操作//


	private void showToast(final String text) {
		final String TAG = "showToast[MA]";
		String dbMsg = "";
		try {
			final Activity activity = this;    //getActivity();
			if ( activity != null ) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(activity , text , Toast.LENGTH_SHORT).show();
					}
				});
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Shows an error message dialog.
	 */
	public static class ErrorDialog extends DialogFragment {

		private static final String ARG_MESSAGE = "message";

		public static ErrorDialog newInstance(String message) {
			ErrorDialog dialog = new ErrorDialog();
			Bundle args = new Bundle();
			args.putString(ARG_MESSAGE , message);
			dialog.setArguments(args);
			return dialog;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Activity activity = getActivity();
			return new AlertDialog.Builder(activity).setMessage(getArguments().getString(ARG_MESSAGE)).setPositiveButton(android.R.string.ok , new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface , int i) {
					activity.finish();
				}
			}).create();
		}

	}

	/**
	 * Shows OK/Cancel confirmation dialog about camera permission.
	 */
	public static class ConfirmationDialog extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Fragment parent = getParentFragment();
			return new AlertDialog.Builder(getActivity()).setMessage(R.string.request_permission).setPositiveButton(android.R.string.ok , new DialogInterface.OnClickListener() {
				@TargetApi ( Build.VERSION_CODES.M )
				@Override
				public void onClick(DialogInterface dialog , int which) {
					parent.requestPermissions(new String[]{Manifest.permission.CAMERA} , REQUEST_CAMERA_PERMISSION);
				}
			}).setNegativeButton(android.R.string.cancel , new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog , int which) {
					Activity activity = parent.getActivity();
					if ( activity != null ) {
						activity.finish();
					}
				}
			}).create();
		}
	}

	///////////////////////////////////////////////////////////////////////////////////
	public void messageShow(String titolStr , String mggStr) {
		if ( UTIL == null ) {
			UTIL = new CS_Util();
		}
		UTIL.messageShow(titolStr , mggStr , MainActivity.this);
	}

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

//参照 	 11 Oct 2017	https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java
//参照		 http://blog.kotemaru.org/2015/05/23/android-camera2-sample.html
/**
 * 2016/10/3		AndroidでOpenCV3.1をする		 http://www.autumn-color.com/archives/169
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
 * これが消えるとクラッシュする　＞＞　AFの呼び方か？
 * <p>
 * in-out切替
 * <p>
 * 残留問題
 * toolbarは組み込めない
 * <p>
 * <p>
 * org.opencv.android.JavaCameraView
 * 保存ファイル名
 * <p>
 * E/mm-camera: <STATS_AF ><ERROR> 4436: af_port_handle_pdaf_stats: Fail to init buf divert ack ctrl
 * <p>
 * 6/9	 runOnUiThreadで内部クラス作成　　>>　　OCVFRV.readFrameRGB(bitmap);へ
 * <p>
 * 6/8
 * コンテキストをコントロールパネルに
 * 年月日フォルダ
 * 撮影された写真のsサムネイル
 * ファイルビュー連携
 * 情報表示
 * <p>
 * オーバーレイのveiwクラスで機能実行
 * 位置とサイズをプレビューに合わせ
 */