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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements View.OnClickListener, View.OnLongClickListener {
	static {
		System.loadLibrary("opencv_java3");  // OpenCV使用クラスに必須
	}

	public CS_Util UTIL;
	public static boolean debugNow = false;

	public FrameLayout ma_preview_fl;        //pereviewVの呼び込み枠       ViewGroup
	public AutoFitTextureView mTextureView;
	public int mTextureViewID = -1;
	public SurfaceView ma_sarface_view;        //  プレビュー用サーフェス
	public SurfaceHolder ma_sarfaceeHolder;

	public FrameLayout ma_effect_fl;        //OpenCVの呼び込み枠
	public OCVFaceRecognitionVeiw OCVFRV;            //顔検出View

	public ImageButton ma_shot_bt;      //キャプチャーボタン
	public ImageButton ma_func_bt;      //設定ボタン
	public ImageButton ma_detecter_bt;      //検出ボタン
	public static ImageView ma_iv;                    //撮影結果

	public static SharedPreferences sharedPref;
	public SharedPreferences.Editor myEditor;
	public String writeFolder = "";
	public float upScale = 1.2f;
	public long haarcascadesLastModified = 0;
	public boolean isReWriteNow = true;                        //リソース書き換え中
	public boolean isPrevieSending = false;     //プレビュー画面処理中

	public boolean isSubCamera = false;                        //サブカメラに切り替え
	public boolean isAutoFlash = false;                        //オートフラッシュ
	public boolean isRumbling = false;                        //シャッター音の鳴動
	public boolean isFaceRecognition = true;                 //顔検出実行中
	public boolean isChaseFocus = false;                 //追跡フォーカス
	public boolean isTexturView = true;                 //高速プレビュー

	public Map< CharSequence, Boolean > detectosSelect;
	public boolean is_detector_frontal_face_alt = true;   //顔検出(標準)</string>
	public boolean is_detector_profileface = true;               //横顔
	public boolean is_detector_upperbody = true;                //上半身
	public boolean is_detector_fullbody = true;                //全身
	public boolean is_detector_eye = true;               //目(標準)</string>
	public boolean is_detector_righteye_2splits = true;        //右目
	public boolean is_detector_lefteye_2splits = true;                //左目
	public boolean is_detector_eyeglasses = true;                //眼鏡
	public boolean is_detector_frontalcatface = false;               //正面のみ？
	public boolean is_detector_frontalcatface_extended = false;                //正面(拡張)？string>
	public boolean is_detector_frontalface_alt_tree = false;               //正面の顔高い木？
	public boolean is_detector_frontalface_alt2 = false;                //正面顔全体2
	public boolean is_detector_frontalface_default = false;                //正面デフォルト
	public boolean is_detector_lowerbody = false;                // 下半身
	public boolean is_detector_smile = false;               //笑顔
	public boolean is_detector_russian_plate_number = false;                //ナンバープレート・ロシア
	public boolean is_detector_ricence_plate_rus_16stages = false;     //ナンバープレートRUS
	public boolean is_overlap_rejection = true;     //重複棄却

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
			isTexturView = prefs.isTexturView;     // = true;                 //
			dbMsg += ",高速プレビュー=" + isTexturView;
			isTexturView = prefs.isTexturView;     // = true;                 //
			dbMsg += ",高速プレビュー=" + isTexturView;

			isFaceRecognition = prefs.isFaceRecognition;
			dbMsg += ",顔検出実行中=" + isFaceRecognition;
			is_overlap_rejection = prefs.is_overlap_rejection;
			dbMsg += ",重複棄却=" + is_overlap_rejection;
			isChaseFocus = prefs.isChaseFocus;
			dbMsg += ",追跡フォーカス=" + isChaseFocus;

			is_detector_frontal_face_alt = prefs.is_detector_frontal_face_alt;
			dbMsg += ",顔検出(標準)=" + is_detector_frontal_face_alt;
			is_detector_profileface = prefs.is_detector_profileface;
			dbMsg += ",横顔=" + is_detector_profileface;
			is_detector_fullbody = prefs.is_detector_fullbody;
			dbMsg += ",全身=" + is_detector_fullbody;
			is_detector_upperbody = prefs.is_detector_upperbody;
			dbMsg += ",上半身=" + is_detector_upperbody;
			is_detector_lowerbody = prefs.is_detector_lowerbody;
			dbMsg += ",下半身=" + is_detector_lowerbody;
			is_detector_smile = prefs.is_detector_smile;
			dbMsg += ",笑顔=" + is_detector_smile;
			is_detector_russian_plate_number = prefs.is_detector_russian_plate_number;
			is_detector_frontalcatface = prefs.is_detector_frontalcatface;
			dbMsg += ",正面のみ=" + is_detector_frontalcatface;
			is_detector_frontalcatface_extended = prefs.is_detector_frontalcatface_extended;
			dbMsg += ",正面(拡張)=" + is_detector_frontalcatface_extended;
			is_detector_frontalface_alt_tree = prefs.is_detector_frontalface_alt_tree;
			dbMsg += ",正面の顔高い木=" + is_detector_frontalface_alt_tree;
			is_detector_frontalface_alt2 = prefs.is_detector_frontalface_alt2;
			dbMsg += ",正面顔全体2=" + is_detector_frontalface_alt2;
			is_detector_frontalface_default = prefs.is_detector_frontalface_default;
			dbMsg += ",ナンバープレート・ロシア=" + is_detector_russian_plate_number;
			is_detector_ricence_plate_rus_16stages = prefs.is_detector_ricence_plate_rus_16stages;
			dbMsg += ",ナンバープレートRUS=" + is_detector_ricence_plate_rus_16stages;

			is_detector_eye = prefs.is_detector_eye;
			dbMsg += ",目(標準)=" + is_detector_eye;
			is_detector_righteye_2splits = prefs.is_detector_righteye_2splits;
			dbMsg += ",右目=" + is_detector_righteye_2splits;
			is_detector_lefteye_2splits = prefs.is_detector_lefteye_2splits;
			dbMsg += ",左目=" + is_detector_lefteye_2splits;
			is_detector_eyeglasses = prefs.is_detector_eyeglasses;
			dbMsg += ",眼鏡=" + is_detector_eyeglasses;
			dbMsg += ",正面デフォルト=" + is_detector_frontalface_default;
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
		super.onCreate(savedInstanceState);                 //savedInstanceStateは初回のみ null
		final String TAG = "onCreate[MA]";
		String dbMsg = "";
		try {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_main);
			ma_preview_fl = ( FrameLayout ) findViewById(R.id.ma_preview_fl);        //pereviewVの呼び込み枠       ViewGroup
			ma_effect_fl = ( FrameLayout ) findViewById(R.id.ma_effect_fl);        //OpenCVの呼び込み枠       ViewGroup
			float maxID = Math.max(( float ) R.id.ma_preview_fl , ( float ) R.id.ma_effect_fl);

			ma_shot_bt = ( ImageButton ) findViewById(R.id.ma_shot_bt);      //キャプチャーボタン
			maxID = Math.max(( float ) R.id.ma_shot_bt , ( float ) maxID);
			ma_func_bt = ( ImageButton ) findViewById(R.id.ma_func_bt);      //設定ボタン
			maxID = Math.max(( float ) R.id.ma_func_bt , ( float ) maxID);
			ma_detecter_bt = ( ImageButton ) findViewById(R.id.ma_detecter_bt);      //検出ボタン
			maxID = Math.max(( float ) R.id.ma_detecter_bt , ( float ) maxID);
			ma_iv = ( ImageView ) findViewById(R.id.ma_iv);                    //撮影結果
			maxID = Math.max(( float ) R.id.ma_iv , ( float ) maxID);
			ma_shot_bt.setOnClickListener(this);
			ma_func_bt.setOnClickListener(this);
			ma_detecter_bt.setOnClickListener(this);
			ma_iv.setOnClickListener(this);
			ma_shot_bt.setOnLongClickListener(this);
			ma_func_bt.setOnLongClickListener(this);
			ma_detecter_bt.setOnLongClickListener(this);
			ma_iv.setOnLongClickListener(this);
//			findViewById(R.id.ma_shot_bt).setOnClickListener(this);
//			findViewById(R.id.ma_func_bt).setOnClickListener(this);
//			findViewById(R.id.ma_iv).setOnClickListener(this);
			dbMsg += "savedInstanceState=" + savedInstanceState;
			if ( savedInstanceState != null ) {                               //初回起動以外で
				if ( OCVFRV != null || mCameraDevice != null ) {            //再設定が必要なリソースが残っていたら
					laterDestroy();                                        //破棄動作に入る
				}
			} else {                                                        //2階目以降は
//				ma_preview_fl.removeAllViews();                                //プレビュー削除
			}
			mFile = new File(writeFolder , "pic.jpg");                 //getActivity().getExternalFilesDir(null)
			dbMsg += ",mFile=" + mFile.getParent();

			readPref();                    //同期させないとインストール時にパーミッションエラー発生 ?
			copyAssets("haarcascades");                    // assetsの内容を /data/data/*/files/ にコピーします。
			dbMsg += ",haarcascadesLastModified=" + haarcascadesLastModified;
			FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT , ViewGroup.LayoutParams.MATCH_PARENT);
//			layoutParams.weight = 1.0f;
			layoutParams.gravity = Gravity.CENTER;           //17;効いてない？
			dbMsg += ",isTexturView=" + isTexturView;                 //高速プレビュー
			if ( isTexturView ) {
				ma_preview_fl.removeAllViews();                                //プレビュー削除
				mTextureView = new AutoFitTextureView(this);
				mTextureView.setLayoutParams(layoutParams);
				ma_preview_fl.addView(mTextureView);
				mTextureView.setId(( int ) (maxID + 7));       //生成したViewのIDは-1なので付与が必要
				mTextureViewID = mTextureView.getId();
				dbMsg += ",mTextureView生成=" + mTextureViewID;
			} else {
//				ma_sarface_view = ( SurfaceView ) findViewById(R.id.ma_sarface_view);
				ma_sarface_view = new SurfaceView(this);       //  プレビュー用サーフェス
				ma_sarface_view.setLayoutParams(layoutParams);
				Display display = getWindowManager().getDefaultDisplay();                // 画面サイズ;HardwareSize;を取得する
				Point p = new Point();
				display.getSize(p);
				int hsWidth = p.x;
				int hsHeight = p.y;
				dbMsg += ",this[" + hsWidth + "×" + hsHeight + "]";
				ViewGroup.LayoutParams svlp = ma_sarface_view.getLayoutParams();
//				dbMsg += ",LayoutParams[" + svlp.width + "×" + svlp.height + "]";
				svlp.width = hsWidth;    //ma_sarface_view.getWidth();
				svlp.height = hsHeight;        // ma_sarface_view.getWidth() * PREVIEW_HEIGHT / PREVIEW_WIDTH;
				if ( hsHeight < hsWidth ) {
					hsWidth = hsHeight * 4 / 3;
					svlp.width = hsWidth;
				} else {
					hsHeight = hsWidth * 4 / 3;
					svlp.height = hsHeight;
				}
				dbMsg += ">>[" + hsWidth + "×" + hsHeight + "]";
				dbMsg += ">LayoutParams>[" + svlp.width + "×" + svlp.height + "]";
				ma_sarface_view.setLayoutParams(svlp);                //ここではviewにサイズを与えるだけ。   Holderはカメラセッション開始以降で設定
				svlp = ma_sarface_view.getLayoutParams();
				ma_preview_fl.addView(ma_sarface_view);
				ma_sarface_view.setId(( int ) (maxID + 8));            //生成時のみ付与する必要有り
//				ma_sarface_view.getHolder().setFixedSize(hsWidth , hsHeight);            // SurfaceViewにプレビューサイズを設定する(サンプルなので適当な値です)
				// 画面縦横比設定のためViewTreeObserverにリスナー設定    	https://qiita.com/fslasht/items/be41e84cfbc4bbb91af7
//				ma_preview_fl.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//					// レイアウト完了時
//					@Override
//					public void onGlobalLayout() {
//						final String TAG = "onGlobalLayout[MA}";
//						String dbMsg = "";
//						try {
////							boolean isLandscape = ma_preview_fl.getWidth() > ma_preview_fl.getHeight();   // 横画面か?
////							dbMsg += "isLandscape=" + isLandscape;
//							Display display = getWindowManager().getDefaultDisplay();                // 画面サイズ;HardwareSize;を取得する
//							Point p = new Point();
//							display.getSize(p);
//							int pvWidth = p.x;
//							int pvHeight = p.y;
//							dbMsg += ",this[" + pvWidth + "×" + pvHeight + "]";
//							if ( mPreviewSize != null ) {
//								pvWidth = mPreviewSize.getWidth();
//								pvHeight = mPreviewSize.getHeight();
//							}
//							dbMsg += ",最大プレビューサイズ[" + pvWidth + "×" + pvHeight + "]";
//							ViewGroup.LayoutParams svlp = ma_sarface_view.getLayoutParams();
//							dbMsg += "[" + svlp.width + "×" + svlp.height + "]";
//							if ( pvHeight < pvWidth ) {
//								dbMsg += ">横画面";
//								pvWidth = pvHeight * 4 / 3;
//								dbMsg += ">>[" + pvWidth + "×" + pvHeight + "]";
//								svlp.width = pvWidth;        // ma_sarface_view.getHeight() * PREVIEW_WIDTH / PREVIEW_HEIGHT;
//								svlp.height = pvHeight;// ma_sarface_view.getHeight();
//							} else {
//								dbMsg += ">縦画面";
//								pvHeight = pvWidth * 4 / 3;
//								dbMsg += ">>[" + pvWidth + "×" + pvHeight + "]";
//								svlp.width = pvHeight;    //ma_sarface_view.getWidth();
//								svlp.height =pvWidth ;        // ma_sarface_view.getWidth() * PREVIEW_HEIGHT / PREVIEW_WIDTH;
//							}
////							dbMsg += ">>[" + pvWidth + "×" + pvHeight + "]";
////							dbMsg += ",PREVIEW[" + PREVIEW_WIDTH + "×" + PREVIEW_HEIGHT + "]";
////							ViewGroup.LayoutParams svlp = ma_sarface_view.getLayoutParams();
////							dbMsg += "[" + svlp.width + "×" + svlp.height + "]";
////							if ( isLandscape ) {
////								dbMsg += ">横画面";
////								svlp.width = pvWidth;        // ma_sarface_view.getHeight() * PREVIEW_WIDTH / PREVIEW_HEIGHT;
////								svlp.height = pvHeight;// ma_sarface_view.getHeight();
////							} else {
////								dbMsg += ">縦画面";
////								svlp.width = pvHeight;    //ma_sarface_view.getWidth();
////								svlp.height =pvWidth ;        // ma_sarface_view.getWidth() * PREVIEW_HEIGHT / PREVIEW_WIDTH;
////							}
//							dbMsg += ">>[" + svlp.width + "×" + svlp.height + "]";
//							ma_sarface_view.setLayoutParams(svlp);
//							myLog(TAG , dbMsg);
//						} catch (Exception er) {
//							myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//						}
//					}
//				});
				dbMsg += ",ma_sarface_view生成=" + ma_sarface_view.getId();
			}

			setViewState();
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
			isReWriteNow = false;                        //書き換え終了
			dbMsg += ",mBackgroundThread=" + mBackgroundThread;
			if ( mBackgroundThread == null ) {
				startBackgroundThread();        //org
			} else {
				dbMsg += ",mBackgroundThread=" + mBackgroundThread.isAlive();
			}

			//ここに来るときはどちらも未稼働で、登録したリスナーからカメラを起動する
			if ( mTextureView != null ) {
				dbMsg += ",mTextureView;isAvailable=" + mTextureView.isAvailable();
				if ( mTextureView.isAvailable() ) {                //orgでは既にプレビューが機能していたら    openCamera
					int TVWIdht = mTextureView.getWidth();
					int TVHight = mTextureView.getHeight();
					dbMsg += "[" + TVWIdht + "×" + TVHight + "]";
					openCamera(TVWIdht , TVHight);                  //org このタイミングで起動出来ず onSurfaceTextureAvailable　へ
				} else {                                            //org プレビューが機能していなければリスナー設定
					mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
				}
			} else if ( ma_sarface_view != null ) {
				dbMsg += ",ma_sarface_view.isActivated=" + ma_sarface_view.isActivated();
				if ( ma_sarface_view.isActivated() ) {
					int TVWIdht = ma_sarface_view.getWidth();
					int TVHight = ma_sarface_view.getHeight();
					dbMsg += "[" + TVWIdht + "×" + TVHight + "]";
					openCamera(TVWIdht , TVHight);                  //org このタイミングで起動出来ず onSurfaceTextureAvailable　へ
				} else {
					ma_sarfaceeHolder = ma_sarface_view.getHolder();                    //SurfaceHolder(SVの制御に使うInterface）
					ma_sarfaceeHolder.addCallback(sarfacCallback);                        //コールバックを設定
				}
			} else {
				dbMsg += "Camera View== null";
			}
			// When the screen is turned off and turned back on, the SurfaceTexture is already available,
			//  and "onSurfaceTextureAvailable" will not be called. In that case, we can open a camera and start preview from here
			// (otherwise, we wait until the surface is ready in the SurfaceTextureListener).
			if ( isRumbling ) {            //=false;						//シャッター音の鳴動
				mSound = new MediaActionSound();                //撮影音をロードする
				mSound.load(MediaActionSound.SHUTTER_CLICK);
			}
			if ( isFaceRecognition ) {
				ma_detecter_bt.setImageResource(android.R.drawable.star_on);
			} else {
				ma_detecter_bt.setImageResource(android.R.drawable.star_off);
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
			dbMsg += "isReWriteNow=" + isReWriteNow;
			if ( !isReWriteNow ) {
				laterDestroy();
				dbMsg += ">>" + isReWriteNow;
			}

//			 closeCamera();                   //orgではここで破棄
//			stopBackgroundThread();            //prg
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		final String TAG = "onStop[MA}";
		String dbMsg = "";
		try {
			dbMsg += "isReWriteNow=" + isReWriteNow;
			if ( !isReWriteNow ) {
				laterDestroy();
				dbMsg += ">>" + isReWriteNow;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		final String TAG = "onDestroy[MA}";
		String dbMsg = "";
		try {
			dbMsg += "isReWriteNow=" + isReWriteNow;
			if ( !isReWriteNow ) {
				laterDestroy();
				dbMsg += ">>" + isReWriteNow;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
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
	 * AndroidManifest.xml の <activity> タグに
	 * android:configChanges="orientation|screenSize" などで指定した変化にonDestroy→onCreateの代わりに、実行される
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
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
//		super.onConfigurationChanged(newConfig);
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
						showMenuDialog(menuID_root);
//						Intent settingsIntent = new Intent(activity , MyPreferencesActivty.class);
//						startActivityForResult(settingsIntent , REQUEST_PREF);                    //    startActivity( settingsIntent );

//					new AlertDialog.Builder(activity).setMessage(R.string.intro_message).setPositiveButton(android.R.string.ok , null).show();
					}
					break;
				}
				case R.id.ma_detecter_bt: {
					dbMsg += "=ma_detecter_bt";
					detecterBTClick();
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

	@Override
	public boolean onLongClick(View view) {
		final String TAG = "onLongClick[MA]";
		String dbMsg = "";
		try {
			String titolStr = "作成中です";
			String mggStr = "次回リリースまでお待ちください。";
			dbMsg += ",getId=" + view.getId();
			switch ( view.getId() ) {
				case R.id.ma_shot_bt: {
					dbMsg += "=ma_shot_bt";
					messageShow(titolStr , mggStr);
					break;
				}
				case R.id.ma_func_bt: {
					dbMsg += "=ma_func_bt";
					Activity activity = MainActivity.this;            //getActivity();
					if ( null != activity ) {
						Intent settingsIntent = new Intent(MainActivity.this , MyPreferencesActivty.class);
						startActivityForResult(settingsIntent , REQUEST_PREF);  //startActivity(settingsIntent);      //
					}
					break;
				}
				case R.id.ma_detecter_bt: {
					dbMsg += "=ma_detecter_bt";
					showMenuDialog(menuID_detector_select);
					break;
				}

				case R.id.ma_iv: {
					dbMsg += "=ma_iv";
					messageShow(titolStr , mggStr);
				}
				break;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return false;
	}

	@Override
	protected void onActivityResult(final int requestCode , final int resultCode , final Intent data) {
		final String TAG = "onActivityResult";
		String dbMsg = "requestCode=" + requestCode + ",resultCode=" + resultCode;
		try {
			switch ( requestCode ) {
				case REQUEST_PREF:                                //Prefarensからの戻り
					laterDestroy();
					reStart();
//					readPref();
					break;
			}


		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		myLog(TAG , dbMsg);
	}

	//メニュー機構////////////////////////////////////////////////////////////////////////
	public AlertDialog carentDlog;
	public CharSequence[] menuItems;
	public boolean[] menuItemChecks;
	static int menuID_root = 200;
	static int menuID_phot = menuID_root + 1;
	static int menuID_phot_onoff = menuID_phot + 1;
	static int menuID_effect = menuID_phot_onoff + 1;
	static int menuID_effect_onnoff = menuID_effect + 1;
	static int menuID_detector_select = menuID_effect_onnoff + 1;

	public void detectersPref() {
		final String TAG = "detectersPref[MA}";
		String dbMsg = "";
		try {
			dbMsg += ",顔検出(標準)=" + is_detector_frontal_face_alt;
			myEditor.putBoolean("is_detector_frontal_face_alt_key" , is_detector_frontal_face_alt);
			dbMsg += ",横顔=" + is_detector_profileface;
			myEditor.putBoolean("is_detector_profileface_key" , is_detector_profileface);
			dbMsg += ",全身=" + is_detector_fullbody;
			myEditor.putBoolean("is_detector_fullbody_key" , is_detector_fullbody);
			dbMsg += ",上半身=" + is_detector_upperbody;
			myEditor.putBoolean("is_detector_upperbody_key" , is_detector_upperbody);
			dbMsg += ",下半身=" + is_detector_lowerbody;
			myEditor.putBoolean("is_detector_lowerbody_key" , is_detector_lowerbody);
			dbMsg += ",笑顔=" + is_detector_smile;
			myEditor.putBoolean("is_detector_smile_key" , is_detector_smile);
			dbMsg += ",ナンバープレート・ロシア=" + is_detector_russian_plate_number;
			myEditor.putBoolean("is_detector_russian_plate_number_key" , is_detector_russian_plate_number);                    //ナンバープレート・ロシア
			dbMsg += ",ナンバープレートRUS=" + is_detector_ricence_plate_rus_16stages;
			myEditor.putBoolean("is_detector_ricence_plate_rus_16stages_key" , is_detector_ricence_plate_rus_16stages);                               //ナンバープレートRUS

			dbMsg += ",目(標準)=" + is_detector_eye;
			myEditor.putBoolean("is_detector_eye_key" , is_detector_eye);                  //目(標準)
			dbMsg += ",右目=" + is_detector_righteye_2splits;
			myEditor.putBoolean("is_detector_righteye_2splits_key" , is_detector_righteye_2splits);                  //右目
			dbMsg += ",左目=" + is_detector_lefteye_2splits;
			myEditor.putBoolean("is_detector_lefteye_2splitss_key" , is_detector_lefteye_2splits);                 //左目
			dbMsg += ",眼鏡=" + is_detector_eyeglasses;
			myEditor.putBoolean("is_detector_eyeglasses_key" , is_detector_eyeglasses);                                   //眼鏡
			dbMsg += ",正面のみ=" + is_detector_frontalcatface;
			myEditor.putBoolean("is_detector_frontalcatface_key" , is_detector_frontalcatface);                    //正面のみ？
			dbMsg += ",正面(拡張)=" + is_detector_frontalcatface_extended;
			myEditor.putBoolean("is_detector_frontalcatface_extended_key" , is_detector_frontalcatface_extended);                            //正面(拡張)？
			dbMsg += ",正面の顔高い木？)=" + is_detector_frontalface_alt_tree;
			myEditor.putBoolean("is_detector_frontalface_alt_tree_key" , is_detector_frontalface_alt_tree);                        //正面の顔高い木？
			dbMsg += ",正面顔全体2=" + is_detector_frontalface_alt2;
			myEditor.putBoolean("is_detector_frontalface_alt2_key" , is_detector_frontalface_alt2);                     //正面顔全体2
			dbMsg += ",正面デフォルト=" + is_detector_frontalface_default;
			myEditor.putBoolean("is_detector_frontalface_default_key" , is_detector_frontalface_default);                             //正面デフォルト
			dbMsg += ",更新";
			myEditor.commit();
			dbMsg += "完了";
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * メニューをリストダイアログで表示する
	 */
	public void showMenuDialog(int menuId) {
		final String TAG = "showMenuDialog[MA}";
		String dbMsg = "";
		try {

//			List menuItems = new ArrayList();
//			menuItems.add(getResources().getString(R.string.mm_effect_face_recgnition));
//			menuItems.add(getResources().getString(R.string.mm_setting_titol));
//			menuItems.add(getResources().getString(R.string.mm_quit_titol));
			AlertDialog.Builder listDlg = new AlertDialog.Builder(this);

//			listDlg.setTitle("タイトル");
			dbMsg += ",menuId=" + menuId;
			if ( menuId == menuID_root ) {          //ルートメニュー
				menuItems = new CharSequence[]{getResources().getString(R.string.mm_phot) , getResources().getString(R.string.mm_phot) + getResources().getString(R.string.mm_onoff_only) , getResources().getString(R.string.mm_effect) , getResources().getString(R.string.mm_effect) + getResources().getString(R.string.mm_onoff_only) , getResources().getString(R.string.mm_setting_titol) , getResources().getString(R.string.mm_quit_titol)};
			} else if ( menuId == menuID_phot ) {          //撮影設定
				menuItems = new CharSequence[]{getResources().getString(R.string.mm_phot_array_size)};
			} else if ( menuId == menuID_phot_onoff ) {          //撮影設定
				menuItems = new CharSequence[]{getResources().getString(R.string.mm_phot_sub_main) , getResources().getString(R.string.mm_phot_flash) , getResources().getString(R.string.mm_phot_rumbling) , getResources().getString(R.string.mm_effect_preview_tv)};
				menuItemChecks = new boolean[]{isSubCamera , isAutoFlash , isRumbling , isTexturView};      //オートフラッシュ、シャッター音の鳴動
			} else if ( menuId == menuID_effect ) {          //エフェクト
				menuItems = new CharSequence[]{getResources().getString(R.string.mm_detector_select)};
//				menuItemChecks = new boolean[]{isChaseFocus , false};      //顔検出 ,追跡フォーカス
			} else if ( menuId == menuID_effect_onnoff ) {          //エフェクト
				menuItems = new CharSequence[]{getResources().getString(R.string.mm_effect_face_recgnition) , getResources().getString(R.string.mm_effect_overlap_rejection) , getResources().getString(R.string.mm_effect_chase_focus)};
				menuItemChecks = new boolean[]{isFaceRecognition , is_overlap_rejection , isChaseFocus};      //顔検出 ,重複棄却,追跡フォーカス  , = true;     //
			} else if ( menuId == menuID_detector_select ) {          //検出対象選択
				detectosSelect = new LinkedHashMap< CharSequence, Boolean >();
//				detectosSelect.clear();
				detectosSelect.put(getResources().getString(R.string.mm_detector_frontal_face_alt) , is_detector_frontal_face_alt);                            //顔検出(標準)</string>
				detectosSelect.put(getResources().getString(R.string.mm_detector_profileface) , is_detector_profileface);                                    //横顔
				detectosSelect.put(getResources().getString(R.string.mm_detector_frontalcatface) , is_detector_frontalcatface);                                //正面のみ？
				detectosSelect.put(getResources().getString(R.string.mm_detector_frontalcatface_extended) , is_detector_frontalcatface_extended);            //正面(拡張)？string>
				detectosSelect.put(getResources().getString(R.string.mm_detector_frontalface_alt2) , is_detector_frontalface_alt2);                            //正面顔全体2
				detectosSelect.put(getResources().getString(R.string.mm_detector_frontalface_default) , is_detector_frontalface_default);                    //正面デフォルト
				detectosSelect.put(getResources().getString(R.string.mm_detector_lowerbody) , is_detector_lowerbody);                                        //下半身
				detectosSelect.put(getResources().getString(R.string.mm_detector_upperbody) , is_detector_upperbody);                                        //上半身
				detectosSelect.put(getResources().getString(R.string.mm_detector_fullbody) , is_detector_fullbody);                                            //全身
				detectosSelect.put(getResources().getString(R.string.mm_detector_smile) , is_detector_smile);                                                //笑顔
				detectosSelect.put(getResources().getString(R.string.mm_detector_russian_plate_number) , is_detector_russian_plate_number);                    //ナンバープレート・ロシア
				detectosSelect.put(getResources().getString(R.string.mm_detector_ricence_plate_rus_16stages) , is_detector_ricence_plate_rus_16stages);         //ナンバープレートRUS

				detectosSelect.put(getResources().getString(R.string.mm_detector_eye) , is_detector_eye);                                                    //目(標準)
				detectosSelect.put(getResources().getString(R.string.mm_detector_righteye_2splits) , is_detector_righteye_2splits);                            //右目
				detectosSelect.put(getResources().getString(R.string.mm_detector_lefteye_2splits) , is_detector_lefteye_2splits);                            //左目
				detectosSelect.put(getResources().getString(R.string.mm_detector_eyeglasses) , is_detector_eyeglasses);                                        //眼鏡
				detectosSelect.put(getResources().getString(R.string.mm_detector_frontalface_alt_tree) , is_detector_frontalface_alt_tree);                    //正面の顔高い木？

				menuItems = new CharSequence[detectosSelect.size()];
				menuItemChecks = new boolean[detectosSelect.size()];
				int setCount = 0;
				for ( Map.Entry< CharSequence, Boolean > entry : detectosSelect.entrySet() ) {
					dbMsg += "(" + setCount + ")" + entry.getKey() + "=" + entry.getValue();
					menuItems[setCount] = entry.getKey();
					menuItemChecks[setCount] = entry.getValue();
					setCount++;
				}
			}

			if ( menuId == menuID_root || menuId == menuID_phot || menuId == menuID_effect ) {          //プレーンリスト
				listDlg.setItems(menuItems , new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog , int which) {
						// リスト選択時の処理
						// which は、選択されたアイテムのインデックス
						CharSequence selctItem = menuItems[which];
						myMenuSelected(( String ) selctItem);
					}
				});
			} else if ( menuId == menuID_detector_select ) {      //チェックボックス（複数選択）
				listDlg.setTitle(getResources().getString(R.string.mm_detector_select));
				listDlg.setMultiChoiceItems(menuItems , menuItemChecks , new DialogInterface.OnMultiChoiceClickListener() {
					public void onClick(DialogInterface dialog , int which , boolean flag) {
					}
				});
				listDlg.setPositiveButton("OK" , new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog , int idx) {
						final String TAG = "menuID_detector_select[MA}";
						String dbMsg = "";
						String str = null;
						for ( int i = 0 ; i < menuItemChecks.length ; i++ ) {
							String rName = ( String ) menuItems[i];
							boolean eBool = menuItemChecks[i];
							dbMsg += "(" + i + ")" + rName + "=" + eBool;
							if ( rName.equals(getResources().getString(R.string.mm_detector_frontal_face_alt)) ) {                //顔検出(標準)</string>
								is_detector_frontal_face_alt = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_profileface)) ) {                    //横顔
								is_detector_profileface = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_upperbody)) ) {                    //上半身
								is_detector_upperbody = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_fullbody)) ) {                    //全身
								is_detector_fullbody = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_lowerbody)) ) {                    //下半身
								is_detector_lowerbody = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_smile)) ) {                    //笑顔
								is_detector_smile = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_russian_plate_number)) ) {                    //ナンバープレート・ロシア
								is_detector_russian_plate_number = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_ricence_plate_rus_16stages)) ) {                    //ナンバープレートRUS
								is_detector_ricence_plate_rus_16stages = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_eye)) ) {                    //目(標準)
								is_detector_eye = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_righteye_2splits)) ) {                    //右目
								is_detector_righteye_2splits = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_lefteye_2splits)) ) {                    //左目
								is_detector_lefteye_2splits = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_eyeglasses)) ) {                    //眼鏡
								is_detector_eyeglasses = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_frontalcatface)) ) {                    //正面のみ？
								is_detector_frontalcatface = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_frontalcatface_extended)) ) {                    //正面(拡張)？
								is_detector_frontalcatface_extended = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_frontalface_alt_tree)) ) {                    //正面の顔高い木？
								is_detector_frontalface_alt_tree = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_frontalface_alt2)) ) {                    //正面顔全体2
								is_detector_frontalface_alt2 = eBool;
							} else if ( rName.equals(getResources().getString(R.string.mm_detector_frontalface_default)) ) {                    //正面デフォルト
								is_detector_frontalface_default = eBool;
							}
						}
						detectersPref();
						myLog(TAG , dbMsg);
					}
				});
			} else {      //チェックボックス（選択状況表示のみ）
				listDlg.setMultiChoiceItems(    //☆xチェックボックス付きではタップしても消えない       //setMultiChoiceItems
						menuItems , menuItemChecks , new DialogInterface.OnMultiChoiceClickListener() {
							public void onClick(DialogInterface dialog , int which , boolean flag) {
								CharSequence selctItem = menuItems[which];
								myMenuSelected(( String ) selctItem);
								carentDlog.dismiss();
								//		carentDlog.hide();				//表示しないだけで生成はされている
							}
						});

			}
			carentDlog = listDlg.create();  //内容設定をしてから
			carentDlog.show();            // 表示

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 面託されたメニューの文字列を照合して実行
	 */
	public void myMenuSelected(String selctItem) {
		final String TAG = "myMenuSelected[MA}";
		String dbMsg = "";                    //表記が返る
		try {
			String titolStr = "作成中です";
			String mggStr = "次回リリースまでお待ちください。";
//			dbMsg += ",selectNo=" + selectNo;
//			CharSequence selctItem = menuItems[selectNo];
			dbMsg += ",selctItem=" + selctItem;
			if ( selctItem.equals(getResources().getString(R.string.mm_quit_titol)) ) {
				callQuit();
			} else if ( selctItem.equals(getResources().getString(R.string.mm_setting_titol)) ) {
				Intent settingsIntent = new Intent(MainActivity.this , MyPreferencesActivty.class);
				startActivityForResult(settingsIntent , REQUEST_PREF);  //startActivity(settingsIntent);      //
			} else if ( selctItem.contains(getResources().getString(R.string.mm_effect)) ) {
				if ( selctItem.endsWith(getResources().getString(R.string.mm_onoff_only)) ) {
					showMenuDialog(menuID_effect_onnoff);
				} else {
					showMenuDialog(menuID_effect);
				}
			} else if ( selctItem.equals(getResources().getString(R.string.mm_detector_select)) ) {
				showMenuDialog(menuID_detector_select);
			} else if ( selctItem.equals(getResources().getString(R.string.mm_effect_face_recgnition)) ) {
				detecterBTClick();

			} else if ( selctItem.equals(getResources().getString(R.string.mm_effect_overlap_rejection)) ) {
				dbMsg += ",is_overlap_rejection=" + is_overlap_rejection;
				is_overlap_rejection = !is_overlap_rejection;
				dbMsg += ">>" + is_overlap_rejection;
				myEditor.putBoolean("is_is_overlap_rejection" , is_overlap_rejection);
				dbMsg += ",更新";
				myEditor.commit();
				dbMsg += "完了";
				if ( OCVFRV != null ) {
					OCVFRV.is_overlap_rejection = is_overlap_rejection;
				}
			} else if ( selctItem.equals(getResources().getString(R.string.mm_effect_chase_focus)) ) {
				dbMsg += ",isChaseFocus=" + isChaseFocus;
				isChaseFocus = !isChaseFocus;
				dbMsg += ">>" + isChaseFocus;
				myEditor.putBoolean("is_chase_focus_key" , isChaseFocus);
				dbMsg += ",更新";
				myEditor.commit();
				dbMsg += "完了";
				messageShow(titolStr , mggStr);
			} else if ( selctItem.equals(getResources().getString(R.string.mm_effect_preview_tv)) ) {
//				messageShow(titolStr , mggStr);
				dbMsg += ",isTexturView=" + isTexturView;
				isTexturView = !isTexturView;
				dbMsg += ">>" + isTexturView;
				myEditor.putBoolean("isTexturView_key" , isTexturView);
				dbMsg += ",更新";
				myEditor.commit();
				dbMsg += "完了";
				reStart();
			} else if ( selctItem.equals(getResources().getString(R.string.mm_effect_preview_sufece)) ) {
				messageShow(titolStr , mggStr);
			} else if ( selctItem.contains(getResources().getString(R.string.mm_phot)) ) {
				if ( selctItem.endsWith(getResources().getString(R.string.mm_onoff_only)) ) {
					showMenuDialog(menuID_phot_onoff);
				} else {
					showMenuDialog(menuID_phot);
				}
			} else if ( selctItem.equals(getResources().getString(R.string.mm_phot_sub_main)) ) {
				messageShow(titolStr , mggStr);
				dbMsg += ",isSubCamera=" + isSubCamera;
				isSubCamera = !isSubCamera;
				dbMsg += ">>" + isSubCamera;
			} else if ( selctItem.equals(getResources().getString(R.string.mm_phot_array_size)) ) {
				messageShow(titolStr , mggStr);
			} else if ( selctItem.equals(getResources().getString(R.string.mm_phot_flash)) ) {
				dbMsg += ",isAutoFlash=" + isAutoFlash;
				isAutoFlash = !isAutoFlash;
				dbMsg += ">>" + isAutoFlash;
				messageShow(titolStr , mggStr);
			} else if ( selctItem.equals(getResources().getString(R.string.mm_phot_rumbling)) ) {
				dbMsg += ",isRumbling=" + isRumbling;
				isRumbling = !isRumbling;
				dbMsg += ">>" + isRumbling;
				messageShow(titolStr , mggStr);
			}
			myLog(TAG , dbMsg);

//			switch ( itemId ) {
//				case R.id.mm_googlec2:
//					dbMsg += ";" + getResources().getString(R.string.googlc2);
////					Intent mIntent = new Intent(FdActivity.this , MainActivity.class);
////					startActivity( mIntent );
//					break;
//				case R.id.mm_c1surfas:
////					dbMsg += ";" + getResources().getString(R.string.c1surfas);
////					activityMain.removeAllViews();
////					c2SufaceView = null;
////					mySurfaceView = new C1SurfaceView(this , dispDegrees);        //orgは90°固定だった
////					activityMain.addView(mySurfaceView);
//					break;
//				case R.id.mm_c2sarfece:
//					dbMsg += ";" + getResources().getString(R.string.c2sarfece);
////					activityMain.removeAllViews();
////					mySurfaceView = null;
////					c2SufaceView = new C2SurfaceView(this , getDisplayOrientation() , writeFolder);            //camera2でSurfaceのプレビュークラス
////					activityMain.addView(c2SufaceView);
//					break;
//				case R.id.mm_prefarence:
//					dbMsg += ";" + getResources().getString(R.string.mm_setting_titol);
//					Intent settingsIntent = new Intent(MainActivity.this , MyPreferencesActivty.class);
//					startActivityForResult(settingsIntent , REQUEST_PREF);                    //    startActivity( settingsIntent );
//					break;
//				case R.id.mm_quit:
//					dbMsg += ";" + getResources().getString(R.string.mm_quit_titol);
//					callQuit();
//					break;
//			}
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "で" + er.toString());
		}
	}


	//アプリケーション動作//////////////////////////////////////////////////////////////////////////メニュー機構//////
	public void detecterBTClick() {
		final String TAG = "detecterBTClick[MA}";
		String dbMsg = "";
		try {
			dbMsg += ",isFaceRecognition=" + isFaceRecognition;
			isFaceRecognition = !isFaceRecognition;                 //顔検出実行中
			dbMsg += ">>" + isFaceRecognition;
			myEditor.putBoolean("is_face_recognition_key" , isFaceRecognition);
			dbMsg += ",更新";
			myEditor.commit();
			dbMsg += "完了";
			String toastText = "検出終了";
			if ( isFaceRecognition ) {
				toastText = "";
				ma_detecter_bt.setImageResource(android.R.drawable.star_on);      //    @android:drawable/star_on
				if ( is_detector_frontal_face_alt ) {                //顔検出(標準)</string>
					toastText += getResources().getString(R.string.mm_detector_frontal_face_alt);
				} else if ( is_detector_profileface ) {                    //横顔
					toastText += "," + getResources().getString(R.string.mm_detector_profileface);
				} else if ( is_detector_upperbody ) {                    //上半身
					toastText += "," + getResources().getString(R.string.mm_detector_upperbody);
				} else if ( is_detector_fullbody ) {                    //全身
					toastText += "," + getResources().getString(R.string.mm_detector_fullbody);
				} else if ( is_detector_lowerbody ) {                    //下半身
					toastText += "," + getResources().getString(R.string.mm_detector_lowerbody);
				} else if ( is_detector_smile ) {                    //笑顔
					toastText += "," + getResources().getString(R.string.mm_detector_smile);
				} else if ( is_detector_frontalcatface ) {                    //正面のみ？
					toastText += "," + getResources().getString(R.string.mm_detector_frontalcatface);
				} else if ( is_detector_frontalcatface_extended ) {                    //正面(拡張)？
					toastText += "," + getResources().getString(R.string.mm_detector_frontalcatface_extended);
				} else if ( is_detector_frontalface_alt2 ) {                    //正面顔全体2
					toastText += "," + getResources().getString(R.string.mm_detector_frontalface_alt2);
				} else if ( is_detector_frontalface_default ) {                    //正面デフォルト
					toastText += "," + getResources().getString(R.string.mm_detector_frontalface_default);
				} else if ( is_detector_russian_plate_number ) {                    //ナンバープレート・ロシア
					toastText += "," + getResources().getString(R.string.mm_detector_russian_plate_number);
				} else if ( is_detector_ricence_plate_rus_16stages ) {                    //ナンバープレートRUS
					toastText += "," + getResources().getString(R.string.mm_detector_ricence_plate_rus_16stages);
				} else if ( is_detector_eye ) {                    //目(標準)
					toastText += "\n特定した人物の詳細検出は" + getResources().getString(R.string.mm_detector_eye);
				} else if ( is_detector_righteye_2splits ) {                    //右目
					toastText += "," + getResources().getString(R.string.mm_detector_righteye_2splits);
				} else if ( is_detector_lefteye_2splits ) {                    //左目
					toastText += "," + getResources().getString(R.string.mm_detector_lefteye_2splits);
				} else if ( is_detector_eyeglasses ) {                    //眼鏡
					toastText += "," + getResources().getString(R.string.mm_detector_eyeglasses);
				} else if ( is_detector_frontalface_alt_tree ) {                    //正面の顔高い木？
					toastText += "," + getResources().getString(R.string.mm_detector_frontalface_alt_tree);
				}
//					setEffectView();
//					is_detector_frontal_face_alt = true;   //顔検出(標準)</string>
//					is_detector_profileface = true;               //横顔
			} else {
				ma_detecter_bt.setImageResource(android.R.drawable.star_off);      //    @android:drawable/star_on
//					is_detector_frontal_face_alt = false;   //顔検出(標準)</string>
//					is_detector_profileface = false;               //横顔
//					is_detector_upperbody = false;                //上半身
//					is_detector_fullbody = false;                //全身
//					is_detector_lowerbody = false;                // 下半身
//					is_detector_smile = false;               //笑顔
//					is_detector_russian_plate_number = false;                //ナンバープレート・ロシア
//					is_detector_ricence_plate_rus_16stages = false;     //ナンバープレートRUS
//
//					is_detector_eye = false;               //目(標準)</string>
//					is_detector_righteye_2splits = false;        //右目
//					is_detector_lefteye_2splits = false;                //左目
//					is_detector_eyeglasses = false;                //眼鏡
//					is_detector_frontalcatface = false;               //正面のみ？
//					is_detector_frontalcatface_extended = false;                //正面(拡張)？string>
//					is_detector_frontalface_alt_tree = false;               //正面の顔高い木？
//					is_detector_frontalface_alt2 = false;                //正面顔全体2
//					is_detector_frontalface_default = false;                //正面デフォルト
//					removetEffectView();
			}
//				detectersPref();
			showToast(toastText);
			reStart();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	/**
	 * プレビューの幅と角度を更新する
	 */
	public void setViewState() {
		final String TAG = "setViewState[MA}";
		String dbMsg = "";
		try {
			if ( mTextureView != null ) {
				PREVIEW_WIDTH = mTextureView.getWidth();
				PREVIEW_HEIGHT = mTextureView.getHeight();
				dbMsg += "mTextureView";
			} else if ( ma_sarface_view != null ) {
				PREVIEW_WIDTH = ma_sarface_view.getWidth();
				PREVIEW_HEIGHT = ma_sarface_view.getHeight();
				dbMsg += "ma_sarface_view";

			} else {
				PREVIEW_WIDTH = ma_preview_fl.getWidth();
				PREVIEW_HEIGHT = ma_preview_fl.getHeight();
				dbMsg += "ma_preview_fl";
			}
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
	 * 終了/再描画前の破棄処理
	 */
	public void laterDestroy() {
		final String TAG = "laterDestroy[MA}";
		String dbMsg = "";
		try {
			if ( OCVFRV != null ) {
				dbMsg += ",ma_effect_flに" + ma_effect_fl.getChildCount() + "件";
				OCVFRV.canvasRecycle();
				ma_effect_fl.removeView(OCVFRV);
				dbMsg += ">>" + ma_effect_fl.getChildCount();
			}
			dbMsg += ",mCameraDevice=" + mCameraDevice;
			if ( mCameraDevice != null ) {
				closeCamera();
				stopBackgroundThread();
				dbMsg += ">>" + mCameraDevice;
			}
			isReWriteNow = true;                        //書き換え発生
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void reStart() {
		final String TAG = "reStart[MA}";
		String dbMsg = "";
		try {
			Intent intent = new Intent();
			intent.setClass(this , this.getClass());
			this.startActivity(intent);
			this.finish();
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
			laterDestroy();
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

	/**
	 * 追加したエフェクトviewの削除
	 */
	public void removetEffectView() {
		final String TAG = "removetEffectView[MA}";
		String dbMsg = "";
		try {
			dbMsg += ",顔検出実行中" + isFaceRecognition;
			if ( !isFaceRecognition ) {                      //顔検出実行中でなければ
				if ( ma_effect_fl != null ) {
					if ( OCVFRV != null ) {
						OCVFRV.canvasRecycle();
						new Thread(new Runnable() {                    //MainActivity.this.runOnUiThread
							@Override
							public void run() {
								final String TAG = "removetEffectView.run[MA]";
								String dbMsg = "";
								try {
									dbMsg += ",ChildCount=" + ma_effect_fl.getChildCount();
									ma_effect_fl.removeView(OCVFRV);
									dbMsg += ">>" + ma_effect_fl.getChildCount();
									OCVFRV = null;
									myLog(TAG , dbMsg);
								} catch (Exception er) {
									myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
								}
							}
						}).start();
					}
				}
//				if(mCaptureSession != null){
//					mCaptureSession.abortCaptures();	//現在保留中で進行中のすべてのキャプチャをできるだけ速く破棄
////					if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
////						Surface rSurface = mCaptureSession.getInputSurface();
////					}
//					mCaptureSession .stopRepeating();
//				}
			}
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * OpenCV用カスタムビューの追加
	 * processで  mState = STATE_PREVIEW:    OCVFRV = null の時
	 */
	public void setEffectView() {
		final String TAG = "setEffectView[MA}";
		String dbMsg = "開始";
		try {
			if ( ma_effect_fl != null ) {
				dbMsg += ",OCVFRV=" + OCVFRV;
				if ( OCVFRV == null ) {
//					String filename = getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
//					dbMsg += "filename=" + filename;       //filename=/data/user/0/com.hijiyam_koubou.kaodori/files/haarcascades/haarcascade_frontalface_alt.xml
//					File rFile = new File(filename);
//					dbMsg += ";exists=" + rFile.exists();
//					if ( !rFile.exists() ) {
//						copyAssets("haarcascades" , haarcascadesLastModified);                    // assetsの内容を /data/data/*/files/ にコピーします。
//						dbMsg += ">>" + rFile.exists();
//					}
					OCVFRV = new OCVFaceRecognitionVeiw(MainActivity.this);            //顔検出View
					EffectAddTask EAT = new EffectAddTask();
//					EAT.setOnCallBack(new CallBackTask(){
//
////						@Override
//						public void CallBack(View result) {
//							super.CallBack(result);
//							// ※１
//							// resultにはdoInBackgroundの返り値が入ります。
//							// ここからAsyncTask処理後の処理を記述します。
//							Log.i("AsyncTaskCallback", "非同期処理が終了しました。");
//							setEffectViewSize();
//						}
//
//					});
					EAT.execute(OCVFRV);
					setEffectViewSize();
////					//		shotBitmap = _shotBitmap;
////					// 別スレッドを実行
//					//new Thread(new Runnable() { なら回転時にクラッシュしない
//					new Thread(new Runnable() {                    //MainActivity.this.runOnUiThread
//						@Override
//						public void run() {
//							final String TAG = "setEffectView.run[MA]";
//							String dbMsg = "";
//							try {
//								int cCount = ma_effect_fl.getChildCount();
//								dbMsg += ",getChildCount=" + cCount;
//								if ( cCount == 0 ) {
//									dbMsg += ",PREVIEW[" + PREVIEW_WIDTH + "×" + PREVIEW_HEIGHT + "]";  //表示サイズに変更する必要有り
//									if ( OCVFRV == null ) {
//										OCVFRV = new OCVFaceRecognitionVeiw(MainActivity.this);            //顔検出View
//									} else {
//										dbMsg += ",作成済み";
//									}
//									LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(PREVIEW_WIDTH , PREVIEW_HEIGHT);
//									OCVFRV.setLayoutParams(layoutParams);
//									ma_effect_fl.addView(OCVFRV);
//									setEffectViewSize();
//									dbMsg += ">>ChildCount=" + ma_effect_fl.getChildCount();
////
////								} else {
////									dbMsg += ",ma_effect_fl = null";
//								}
//								myLog(TAG , dbMsg);
//							} catch (Exception er) {
//								myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//							}
//						}
//					}).start();
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

	public void setEffectViewSize() {
		final String TAG = "setEffectViewSize[MA}";
		String dbMsg = "";
		try {
			new Thread(new Runnable() {                                                        //で　
				//			MainActivity.this.runOnUiThread(new Runnable() {   // でクラッシュ
				@Override
				public void run() {
					final String TAG = "setEffectViewSize.run[MA]";
					String dbMsg = "";
					try {
						if ( OCVFRV != null ) {
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
							dbMsg += "、現在(" + OCVFRV.getScaleX() + "×" + OCVFRV.getScaleY() + "%)";
							FrameLayout.LayoutParams layoutParams = ( FrameLayout.LayoutParams ) OCVFRV.getLayoutParams();// ViewGroup.MarginLayoutParams だとyoutParams.width' on a null object reference
							if ( layoutParams != null ) {
								dbMsg += ",layoutParams(" + layoutParams.leftMargin + "×" + layoutParams.topMargin + ")[" + layoutParams.width + "×" + layoutParams.height + "]";
								dbMsg += ",gravity=" + layoutParams.gravity;
								layoutParams.leftMargin = sLeft;
								layoutParams.topMargin = sTop;
								layoutParams.width = PREVIEW_WIDTH;
								layoutParams.height = PREVIEW_HEIGHT;
								OCVFRV.setLayoutParams(layoutParams);
								OCVFRV.requestLayout();
								layoutParams = ( FrameLayout.LayoutParams ) OCVFRV.getLayoutParams();
								dbMsg += ",>>(" + layoutParams.leftMargin + "×" + layoutParams.topMargin + ")[" + layoutParams.width + "×" + layoutParams.height + "]";
							}

							dbMsg += ">>OCVFRV(" + OCVFRV.getLeft() + "," + OCVFRV.getTop() + ")[" + OCVFRV.getWidth() + "×" + OCVFRV.getHeight() + "]";
//			dbMsg += ",camera=" + mSensorOrientation + "dig";
							OCVFRV.setCondition();
						} else {
							dbMsg = " OCVFRV = null";
						}
						myLog(TAG , dbMsg);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
				}
			}).start();
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	private class EffectAddTask extends AsyncTask< View, Void, View > {
//		private CallBackTask callbacktask;

		/**
		 * The system calls this to perform work in a worker thread and
		 * delivers it the parameters given to AsyncTask.execute()
		 */
		protected View doInBackground(View... pram) {
			final String TAG = "EffectAddTask.DIB[MA]";
			String dbMsg = "";
			View rView = pram[0];
			try {
				int cCount = ma_effect_fl.getChildCount();
				dbMsg += ",ma_effect_fl(" + ma_effect_fl.getLeft() + "," + ma_effect_fl.getTop() + ")[" + ma_effect_fl.getWidth() + "×" + ma_effect_fl.getHeight() + "]";
				dbMsg += ",getChildCount=" + cCount;
				if ( cCount == 0 ) {
					dbMsg += ",PREVIEW[" + PREVIEW_WIDTH + "×" + PREVIEW_HEIGHT + "]";  //表示サイズに変更する必要有り
					int sLeft = (ma_effect_fl.getWidth() - PREVIEW_WIDTH);
					if ( 0 < sLeft ) {
						sLeft = sLeft / 2;
					}
					int sTop = (ma_effect_fl.getHeight() - PREVIEW_HEIGHT);
					if ( 0 < sTop ) {
						sTop = sTop / 2;
					}
					dbMsg += ",shift(" + sLeft + "," + sTop + ")";
					FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(PREVIEW_WIDTH , PREVIEW_HEIGHT);
					layoutParams.leftMargin = sLeft;
					layoutParams.topMargin = sTop;
					rView.setLayoutParams(layoutParams);
// ma_effect_fl.addView(rView);   //ここで追加できない
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			return rView;
		}

		/**
		 * The system calls this to perform work in the UI thread and delivers
		 * the result from doInBackground()
		 */
		protected void onPostExecute(View result) {       //doInBackgroundの  returnを受け取る
			final String TAG = "EffectAddTask.ope[MA]";
			String dbMsg = "";
			try {
				dbMsg += "ChildCount=" + ma_effect_fl.getChildCount();
				ma_effect_fl.addView(result);
				dbMsg += ">>" + ma_effect_fl.getChildCount();
				FrameLayout.MarginLayoutParams layoutParams = ( FrameLayout.MarginLayoutParams ) OCVFRV.getLayoutParams();
				dbMsg += ",layoutParams(" + layoutParams.leftMargin + "," + layoutParams.topMargin + ")[" + layoutParams.width + "×" + layoutParams.height + "]";
//				setEffectViewSize();
//				callbacktask.CallBack(result);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

//		public void setOnCallBack(CallBackTask _cbj) {
//			callbacktask = _cbj;
//		}
//
//		/**
//		 * コールバック用のstaticなclass
//		 */
//		public static class CallBackTask {         		//
//			public void CallBack(View result) {
//			}
//		}
	}

	///エフェクト更新処理//////////////////////////////////////////////////////////////
	public int fpsCount = 0;
	public int fpsLimi = 30;

	/**
	 * 受け取ったIDのViewからBitmapを抽出しエフェクトビューへ送る。
	 * エフェクトビューが無ければ作成して、動作指定が無くなった時点でViewを破棄する
	 */
	public void sendPreviewBitMap(int targetViewID) {
		final String TAG = "sendPreviewBitMap[MA]";
		String dbMsg = "";
		try {
			dbMsg += ",顔検出実行中=" + isFaceRecognition;
			dbMsg += ",targetViewID=" + targetViewID;
			if ( -1 < targetViewID ) {               // ;                 //
				if ( isFaceRecognition ) {               // ;                 //
//				dbMsg += "isReWriteNow=" + isReWriteNow;
//				if ( !isReWriteNow ) {                                    // //書き換え終了(onResume～onPause)
					if ( OCVFRV != null ) {
						fpsCount++;
						dbMsg += "(" + fpsCount + "/" + fpsLimi + "フレーム)";          //実測 8回で送信
						dbMsg += ",前回処理終了=" + OCVFRV.getCompletion();
						if ( OCVFRV.getCompletion() ) {    //onDrawが終了するまでfalseが返る     && fpsLimi < fpsCount
							fpsCount = 0;
							shotBitmap = null;
							if ( targetViewID == mTextureViewID ) {
								shotBitmap = (( TextureView ) findViewById(targetViewID)).getBitmap();
							} else {
//								if ( ma_sarfaceeHolder != null ) {
//									Canvas canvas = ma_sarfaceeHolder.lockCanvas();
//									int surfaceWidth = ma_sarfaceeHolder.getSurfaceFrame().width();
//									int surfaceHeight = ma_sarfaceeHolder.getSurfaceFrame().height();
//									dbMsg += "[" + surfaceWidth + "×" + surfaceHeight + "]";
//									shotBitmap = Bitmap.createBitmap(surfaceWidth , surfaceHeight , Bitmap.Config.ARGB_8888);          //別途BitmapとCanvasを用意する
////								Canvas tmpCanvas = new Canvas(shotBitmap);
////								canvas.drawBitmap(shotBitmap, null, mScreenRect, null); 								//TODO tmpCanvasに対して描画処理を行う
////								ma_sarfaceeHolder.unlockCanvasAndPost(canvas); //反映
//								}
							}
							if ( shotBitmap != null ) {
								dbMsg += ",bitmap[" + shotBitmap.getWidth() + "×" + shotBitmap.getHeight() + "]";
								int byteCount = shotBitmap.getByteCount();
								dbMsg += "" + byteCount + "バイト";
								dbMsg += ",disp=" + DISP_DEGREES + "dig";
								mSensorOrientation = getOrientation(DISP_DEGREES);
								dbMsg += ",camera=" + mSensorOrientation + "dig";
//							EffectSendData ESD= new EffectSendData();
//							ESD.sendBitmap = shotBitmap;
//							ESD.sensorOrientation =  mSensorOrientation;
//							new EffectSendTask().execute(ESD);

								//	MainActivity.this.runOnUiThread(new Runnable() {   だとプレビューに干渉
// 	new Thread(new Runnable() {   だと縦持ちで縦にプレビューが引き延ばされる
//								//   	だと重複生成とクラッシュ発生
//								@Override
//								public void run() {
//									final String TAG = "sendPreviewBitMap.run[MA]";
//									String dbMsg = "";
//									try {
								List< Rect > retArray = OCVFRV.readFrameRGB(shotBitmap , mSensorOrientation);
								if ( retArray != null ) {
									dbMsg += ">結果>" + retArray.size() + "箇所検出";
								}
//										myLog(TAG , dbMsg);
//									} catch (Exception er) {
//										myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//									}
//								}
//							}).start();
							} else {
								dbMsg += ",shotBitmap = null";
							}
						} else {
							dbMsg = "";    //余計なコメントを出さない
						}
					} else {
						dbMsg += ",OCVFRV = null>>view追加";
						setEffectView();
					}
				} else {                            //顔検出中で無ければ
					removetEffectView();            //viewを破棄
				}
			}
			if ( !dbMsg.equals("") ) {
				myLog(TAG , dbMsg);
			}
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	private class EffectSendData {
		Bitmap sendBitmap;
		int sensorOrientation;
	}

	private class EffectSendTask extends AsyncTask< EffectSendData, Void, EffectSendData > {
		/**
		 * The system calls this to perform work in a worker thread and
		 * delivers it the parameters given to AsyncTask.execute()
		 */
		protected EffectSendData doInBackground(EffectSendData... pram) {
			final String TAG = "EffectSendTask.DIB[MA]";
			String dbMsg = "";
			try {
				Bitmap shotBitmap = pram[0].sendBitmap;
				dbMsg += ",bitmap[" + shotBitmap.getWidth() + "×" + shotBitmap.getHeight() + "]" + shotBitmap.getByteCount();
				int mSensorOrientation = pram[0].sensorOrientation;
				dbMsg += ",camera=" + mSensorOrientation + "dig";
				OCVFRV.readFrameRGB(shotBitmap , mSensorOrientation);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			return pram[0];
		}

		/**
		 * The system calls this to perform work in the UI thread and delivers
		 * the result from doInBackground()
		 */
		protected void onPostExecute(EffectSendData result) {       //doInBackgroundの  returnを受け取る
			final String TAG = "EffectAddTask.ope[MA]";
			String dbMsg = "";
			try {

				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	}

	/**
	 * assetsのdirフォルダに置かれたDetegerデータの内容を /data/data/.../files/ にコピーします。
	 * ☆assetsのフルパス名は拾えないのでアプリケーションがリ利用可能なエリアに作成
	 */
	private void copyAssets(String dir) throws IOException {
		final String TAG = "copyAssets[MA}";      // , long haarcascadesLastModified
		String dbMsg = "";
		try {
			dbMsg = "dir=" + dir;
//			MainActivity MA = new MainActivity();
			//			dbMsg += ",認証ファイル最終更新日=" + haarcascadesLastModified;
			byte[] buf = new byte[8192];
			int size;
			boolean isCopy = false;    //初回使用時なと、強制的にコピーする
			File dst = new File(getApplicationContext().getFilesDir() , dir);
			if ( !dst.exists() ) {                   //作成されていない場合；インストール直後のみ
				dst.mkdirs();
				dst.setReadable(true , false);
				dst.setWritable(true , false);
				dst.setExecutable(true , false);
				dbMsg += ">>作成";
				isCopy = true;
//			}
				int readedCount = dst.list().length;
				dbMsg += ",読込み済み=" + readedCount + "件";
				if ( readedCount < 10 ) {
					isCopy = true;
				}
				for ( String filename : getApplicationContext().getAssets().list(dir) ) {
					File file = new File(dst , filename);
					Long lastModified = file.lastModified();
//				if ( isCopy || haarcascadesLastModified < lastModified ) {    //無ければ
					dbMsg += "," + filename + ";" + lastModified;
//					haarcascadesLastModified = lastModified;
					OutputStream out = new FileOutputStream(file);
					InputStream in = getApplicationContext().getAssets().open(dir + "/" + filename);
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
//				}
				}
			}
			myLog(TAG , dbMsg);
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
				dbMsg = "[" + width + "×" + height + "]";                   // [1920×1080]
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
				dbMsg = "[" + width + "×" + height + "]DISP_DEGREES=" + DISP_DEGREES;    // [810×1080]DISP_DEGREES=0
//				configureTransform(width , height);
				if ( OCVFRV != null ) {
					dbMsg += ",camera=" + mSensorOrientation + "dig";
//					OCVFRV.setCondition();
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
			String dbMsg = "発生";
			try {
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture texture) {
			final String TAG = "onSurfaceTextureUpdated[MA]";
			String dbMsg = "";
			try {
// Surface surface = new Surface(texture);  //までは出来る　がBitmap取得の方法不明
////				synchronized (mCaptureSync) {                //http://serenegiant.com/blog/?p=2074&page=3
// sendPreviewBitMap(mTextureView.getId());		//ここで送るとプレビューに干渉
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

	};
	//プレビュー ; Surfac////////////////////////////////////////////////////////////////////////////////////
	//		https://qiita.com/zaburo/items/d9d07eb4d87d21308124
	//		http://tech.pjin.jp/blog/2014/02/20/androidtips-surfaceview%E3%82%92%E4%BD%BF%E3%81%A3%E3%81%A6%E3%81%BF%E3%81%BE%E3%81%97%E3%81%9F/
//		https://qiita.com/fslasht/items/be41e84cfbc4bbb91af7
	private SurfaceHolder.Callback sarfacCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder surfaceHolder) {
			final String TAG = "surfaceCreated[MA}";
			String dbMsg = "";
			try {
				dbMsg += "surfaceHolder=" + surfaceHolder;
				ma_sarfaceeHolder = surfaceHolder;
				int surfaceWidth = surfaceHolder.getSurfaceFrame().width();
				int surfaceHeight = surfaceHolder.getSurfaceFrame().height();
				dbMsg += "[" + surfaceWidth + "×" + surfaceHeight + "]";
				openCamera(surfaceWidth , surfaceHeight);

				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder surfaceHolder , int format , int surfaceWidth , int surfaceHeight) {
			final String TAG = "surfaceChanged[MA}";
			String dbMsg = "";
			try {
				dbMsg += "surfaceHolder=" + surfaceHolder;
				dbMsg += ",format=" + format;
				ma_sarfaceeHolder = surfaceHolder;
				PREVIEW_WIDTH = surfaceWidth;                    //mTextureView.getWidth();
				PREVIEW_HEIGHT = surfaceHeight;                //mTextureView.getHeight();
				dbMsg += "[" + surfaceWidth + "×" + surfaceHeight + "]DISP_DEGREES=" + DISP_DEGREES;    // [810×1080]DISP_DEGREES=0
//				int pvWidth = surfaceWidth;
//				int pvHeight = surfaceHeight;
//
//				ViewGroup.LayoutParams svlp = ma_sarface_view.getLayoutParams();
//				dbMsg += "[" + svlp.width + "×" + svlp.height + "]";
				if ( surfaceHeight < surfaceWidth ) {
					dbMsg += ">横画面";
////					pvWidth = surfaceHeight * 4 / 3;
//////					dbMsg += ">>[" + pvWidth + "×" + pvHeight + "]";
//////					svlp.width = pvWidth;        // ma_sarface_view.getHeight() * PREVIEW_WIDTH / PREVIEW_HEIGHT;
//////					svlp.height = pvHeight;// ma_sarface_view.getHeight();
				} else {
					dbMsg += ">縦画面";
////					pvHeight = surfaceWidth * 4 / 3;
//////					dbMsg += ">>[" + pvWidth + "×" + pvHeight + "]";
//////					svlp.width = pvHeight;    //ma_sarface_view.getWidth();
//////					svlp.height =pvWidth ;        // ma_sarface_view.getWidth() * PREVIEW_HEIGHT / PREVIEW_WIDTH;
				}
//				dbMsg += ">pv>[" + pvWidth + "×" + pvHeight + "]";
//				svlp.width = pvWidth;        //pvWidth;        // ma_sarface_view.getHeight() * PREVIEW_WIDTH / PREVIEW_HEIGHT;
//				svlp.height = pvHeight;        //pvHeight;// ma_sarface_view.getHeight();
//				dbMsg += ">svlp>[" + svlp.width + "×" + svlp.height + "]";
////				ma_sarface_view.setLayoutParams(svlp);          //Viewサイズを合わせる
//// 		ma_sarfaceeHolder.setFixedSize(PREVIEW_WIDTH , PREVIEW_HEIGHT);	//	ma_sarface_view.getHolder().setFixedSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);			// SurfaceViewにプレビューサイズを設定する(サンプルなので適当な値です)
				configureTransform(surfaceWidth , surfaceHeight);

				if ( OCVFRV != null ) {
					dbMsg += ",camera=" + mSensorOrientation + "dig";
					setEffectViewSize();
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
			final String TAG = "surfaceDestroyed[MA}";
			String dbMsg = "";
			try {
				dbMsg += "surfaceHolder=" + surfaceHolder;
				laterDestroy();
//				myCamera.release();    				//片付け
//				myCamera = null;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};


	///カメラ/////////////////////////////////////////////////////////////////////////////////プレビュー//////
	/**
	 * A {@link CameraCaptureSession } for camera preview.
	 * createCameraPreviewSessionのonConfiguredで取得
	 */
	private CameraCaptureSession mCaptureSession;
	private CameraDevice mCameraDevice;            // A reference to the opened {@link CameraDevice}.
	private String mCameraId;        //ID of the current {@link CameraDevice}.
	private Surface surface;
	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
	private MediaActionSound mSound;    //撮影音のためのMediaActionSound

	private CaptureRequest.Builder mPreviewRequestBuilder;        // {@link CaptureRequest.Builder} for the camera preview
	private CaptureRequest mPreviewRequest;    //{@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}

	private Semaphore mCameraOpenCloseLock = new Semaphore(1);        // A {@link Semaphore} to prevent the app from exiting before closing the camera.

	/**
	 * An additional thread for running tasks that shouldn't block the UI.
	 */
	private HandlerThread mBackgroundThread;
	private Handler mBackgroundHandler;        // A {@link Handler} for running tasks in the background.

	/**
	 * An {@link ImageReader} that handles still image capture.
	 */
	private ImageReader mImageReader;
	private int mMaxImages = 2;                                    //読込み枚数
	/**
	 * This is the output file for our picture.
	 */
	private File mFile;

	static {
		ORIENTATIONS.append(Surface.ROTATION_0 , 90);
		ORIENTATIONS.append(Surface.ROTATION_90 , 0);
		ORIENTATIONS.append(Surface.ROTATION_180 , 270);
		ORIENTATIONS.append(Surface.ROTATION_270 , 180);
	}

	private static final int REQUEST_CAMERA_PERMISSION = 1;
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
	 * @see #
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
	 * 保存処理中
	 */
	public boolean isPhotography = false;

	/**
	 * The {@link android.util.Size} of camera preview.
	 */
	private Size mPreviewSize;
	public int mSensorOrientation;

	/**
	 * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
	 * manager.openCamera() メソッドで指定
	 */
	private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

		@Override
		public void onOpened(CameraDevice cameraDevice) {
			final String TAG = "onOpened[MA]";
			String dbMsg = "";
			try {
				// This method is called when the camera is opened.  We start camera preview here.
				mCameraOpenCloseLock.release();
				mCameraDevice = cameraDevice;
				dbMsg += ",mCameraDevice = " + mCameraDevice.getId();
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
				String titolStr = "再起動してください";
				String mggStr = "エラーが発生しました\n" + error;
				messageShow(titolStr , mggStr);
				activity.finish();
			}
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + error);
		}
	};

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
				Image rImage = reader.acquireLatestImage();     //キューから 最新のものを取得し、古いものを削除します
				// ;2枚保持させて acquireNextImage ?  キューから次のImageを取得
				if ( rImage != null ) {
					dbMsg += ",rImage;Timestamp=" + rImage.getTimestamp();
					dbMsg += ",isPhotography=" + isPhotography;   //撮影時も falseになっている
					if ( !isPhotography ) {     //撮影中で無ければ
//						SendPreview SP = new SendPreview(rImage);
//						mBackgroundHandler.post(SP);
						dbMsg += "プレビュー取得";
					} else {
						dbMsg += ",静止画撮影処理；writeFolder=" + writeFolder;
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

						ImageSaver IS = new ImageSaver(rImage , pFolderName , ma_iv , dtStr);//acquireNextImage ?	第二引数以降は追加     /
						mBackgroundHandler.post(IS);
					}
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

	/**
	 * フラッシュが使えるか否か
	 * Whether the current camera device supports Flash or not.
	 */
	private boolean mFlashSupported;

	/**
	 * カメラの角度
	 * Orientation of the camera sensor
	 */

	/**
	 * JPEG捕獲に関連したそのハンドル・イベント。
	 * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
	 * <p>
	 * createCameraPreviewSessionのonConfiguredで	CONTROL_AF_MODE_CONTINUOUS_PICTURE
	 * lockFocus()で 								CameraMetadata.CONTROL_AF_TRIGGER_START
	 * unlockFocus()で  							CameraMetadata.CONTROL_AF_TRIGGER_CANCEL				mState = STATE_PREVIEW;
	 * mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler); 	 // プレビューに戻る
	 * runPrecaptureSequenceで						CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START		mState = STATE_WAITING_PRECAPTURE;
	 * 追加
	 * copyPreview()で CameraDevice.TEMPLATE_STILL_CAPTURE
	 * SendPreview.runで mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler);    //プレビュ再開
	 */
	private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
		private void process(CaptureResult result) {
			final String TAG = "process[MA]";
			String dbMsg = "";
			try {
				///6/18	この時点で破棄動作に入っていないか

//				dbMsg += "result=" + result;
				dbMsg += "mState=" + mState;
				switch ( mState ) {
					case STATE_PREVIEW: {                //0 ＜＜初期値とunlockFocus() 、We have nothing to do when the camera preview is working normally.
						dbMsg = "";    //余計なコメントを出さない
						if ( mTextureView != null ) {
							sendPreviewBitMap(mTextureView.getId());     //ここから送ると回転動作にストレス発生？ ？
						} else if ( ma_sarface_view != null ) {
							sendPreviewBitMap(ma_sarface_view.getId());
						}
						break;
					}
					case STATE_WAITING_LOCK: {        //1<<lockFocus()から
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

		/**
		 * 撮影完了
		 * */
		@Override
		public void onCaptureCompleted(CameraCaptureSession session , CaptureRequest request , TotalCaptureResult result) {
			final String TAG = "MCC.onCaptureCompleted[MA]";
			String dbMsg = "";
			try {
				dbMsg += ",CaptureRequest=" + request.getKeys().size() + "件";    //CaptureRequest=android.hardware.camera2.CaptureRequest@ed0bb9ae,TotalCaptureResult=android.hardware.camera2
				dbMsg += ",TotalCaptureResult=" + result.getRequest().getKeys().size() + "件";   // TotalCaptureResult@17e91b3
				process(result);

//				Bitmap shotBitmap = mTextureView.getBitmap();
//				dbMsg += ",bitmap[" + shotBitmap.getWidth() + "×" + shotBitmap.getHeight() + "]";
//				int byteCount = shotBitmap.getByteCount();
//				dbMsg += "" + byteCount + "バイト";
//				sendPreviewbitMap( shotBitmap);
//
//				myLog(TAG , dbMsg);
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
//		int retInt = 0;
		try {
			dbMsg = "Disp=" + rotation;
			dbMsg += "、camera=" + mSensorOrientation;
			mSensorOrientation = ORIENTATIONS.get(rotation) % 360;   //			retInt = (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
			dbMsg += ">>=" + mSensorOrientation;

			// Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
			// We have to take that into account and rotate JPEG properly.
			// For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
			// For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return mSensorOrientation;
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
	 * 回転方向によって変化する利用可能な撮影サイズとプレビューサイズを取得する
	 * Sets up member variables related to camera.
	 * @param width  The width of available size for camera preview
	 * @param height The height of available size for camera preview
	 *               openCamera　から呼ばれる
	 */
	@SuppressWarnings ( "SuspiciousNameCombination" )
	private void setUpCameraOutputs(int width , int height) {
		final String TAG = "setUpCameraOutputs[MA]";
		String dbMsg = "";
		try {
			Activity activity = MainActivity.this;                //getActivity();
			CameraManager manager = ( CameraManager ) activity.getSystemService(Context.CAMERA_SERVICE);
//			try {
			for ( String cameraId : manager.getCameraIdList() ) {
				dbMsg += ",cameraId=" + cameraId;
				CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

				Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);                        // We don't use a front facing camera in this sample.
				dbMsg += ",facing=" + facing + "(0;FRONT)";
				if ( facing != null) {
					if ( (! isSubCamera && facing == CameraCharacteristics.LENS_FACING_FRONT ) ||  				//0
								 ( isSubCamera && facing == CameraCharacteristics.LENS_FACING_BACK )	) { 	//1
						continue;
					}
				}

				StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if ( map == null ) {
					continue;
				}

				// For still image captures, we use the largest available size.
				Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)) , ( Comparator< ? super Size > ) new CompareSizesByArea());
				dbMsg += "m,largest[" + largest.getWidth() + "×" + largest.getHeight() + "]";
				mImageReader = ImageReader.newInstance(largest.getWidth() , largest.getHeight() , ImageFormat.JPEG , mMaxImages);
				//目的のサイズ（利用可能な最大撮影サイズ）とフォーマットの画像用の新しいリーダーを作成
				mImageReader.setOnImageAvailableListener(mOnImageAvailableListener , mBackgroundHandler);
				//ImageReaderから新しいイメージが利用可能になったときに呼び出されるリスナーを登録
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
				mCameraId = cameraId;

				dbMsg += ",rotatedPreview[" + rotatedPreviewWidth + "×" + rotatedPreviewHeight + "]";
				dbMsg += ",maxPreview[" + maxPreviewWidth + "×" + maxPreviewHeight + "]";

				// Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
				// bus' bandwidth limitation, resulting in gorgeous previews but the storage of garbage capture data.
				mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class) , rotatedPreviewWidth , rotatedPreviewHeight , maxPreviewWidth , maxPreviewHeight , largest);
				// We fit the aspect ratio of TextureView to the size of preview we picked.
				int setWidth = mPreviewSize.getWidth();
				int setHeight = mPreviewSize.getHeight();
				dbMsg += ",最大プレビューサイズ[" + setWidth + "×" + setHeight + "]";
				int orientation = getResources().getConfiguration().orientation;
				dbMsg += ",orientation=" + orientation;
				if ( orientation == Configuration.ORIENTATION_LANDSCAPE ) {
				} else {
					int retention = setWidth;
					setWidth = setHeight;
					setHeight = retention;
					mPreviewSize = new Size(setWidth,setHeight);
					dbMsg += ">mPreviewSize>[" + mPreviewSize.getWidth() + "×" + mPreviewSize.getHeight() + "]";
				}
				if ( mTextureView != null ) {
					mTextureView.setAspectRatio(setWidth , setHeight);
				} else if ( ma_sarface_view != null ) {
//					ma_sarfaceeHolder.setFixedSize(setWidth , setHeight);
				}
				Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);                // Check if the flash is supported.
				mFlashSupported = available == null ? false : available;
				dbMsg += ",mFlashSupported=" + mFlashSupported;

				mCameraId = cameraId;
//				myLog(TAG , dbMsg);
//				return;
			}
		//縦;displayRotation=0,mSensorOrientation=90,rotatedPreview[1440×1080],maxPreview[1776×1080],orientation=1,mFlashSupported=true
		//横;displayRotation=1,mSensorOrientation=90,rotatedPreview[1440×1080],maxPreview[1776×1080],orientation=2,mFlashSupported=true
			//どちらも   largest[4608×3456],  最大プレビューサイズ[1440×1080],
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (NullPointerException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			// Currently an NPE is thrown when the Camera2API is used but not supported on the
			// device this code runs.
//				ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager() , FRAGMENT_DIALOG);
//			}
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	// Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.

	/**
	 * 指定されたサイズと設定されているCameraIdで指定されたカメラの動作を開始させる
	 * onSurfaceTextureAvailable、onResumeから呼ばれる
	 * 受け取るサイズは意味込み初期値
	 */
	private void openCamera(int width , int height) {
		final String TAG = "openCamera[MA]";
		String dbMsg = "";
		try {
			dbMsg = "DISP[" + width + "×" + height + "]" + DISP_DEGREES;
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
				if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
					if ( checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
						dbMsg += ",permission.CAMERA取得できず";
						return;
					}
				}
				manager.openCamera(mCameraId , mStateCallback , mBackgroundHandler);    //SecurityException;validateClientPermissionsLocked

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
	 * カメラの終了動作
	 * Session、Session ,ImageReaderと使用したリソースの破棄
	 * Closes the current {@link CameraDevice}.
	 */
	private void closeCamera() {
		final String TAG = "closeCamera[MA]";
		String dbMsg = "";
		try {
			try {
				dbMsg += "mCaptureSession=" + mCaptureSession;
				mCameraOpenCloseLock.acquire();
				if ( null != mCaptureSession ) {         //org①
					mCaptureSession.stopRepeating();    //追加
					mCaptureSession.abortCaptures();    //追加
					mCaptureSession.close();
					mCaptureSession = null;
					dbMsg += ",mCaptureSession 破棄";
				}
				if ( null != mCameraDevice ) {          //org②
					mCameraDevice.close();
					mCameraDevice = null;
					dbMsg += ",mCameraSession 破棄";
				}
				if ( null != mImageReader ) {                  //org
					mImageReader.close();         // ImageReaderに関連するすべてのリソースを解放
					mImageReader = null;
					dbMsg += ",mImageReader 破棄";
				}
//				if ( surface != null ) {
//					surface.release();
//					surface = null;
//					dbMsg += ",surface 破棄";
//				}
				if ( mSound != null ) {
					mSound.release();
					mSound = null;
					dbMsg += ",mSound 破棄";
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
			dbMsg = "mBackgroundThread=" + mBackgroundThread;
			if ( mBackgroundThread == null ) {
				mBackgroundThread = new HandlerThread("CameraBackground");
				mBackgroundThread.start();
				dbMsg += ">>=" + mBackgroundThread;
			}
			dbMsg += " , mBackgroundHandler=" + mBackgroundHandler;
			if ( mBackgroundHandler == null ) {
				mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
				dbMsg += ">>=" + mBackgroundHandler;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	/**
	 * Stops the background thread and its {@link Handler}.
	 * <p>
	 * Quit処理 , onDestroy
	 */
	private void stopBackgroundThread() {
		final String TAG = "stopBackgroundThread[MA]";
		String dbMsg = "";
		try {
			dbMsg = "mBackgroundThread=" + mBackgroundThread;
			if ( mBackgroundThread != null ) {
				mBackgroundThread.quitSafely();
				mBackgroundThread.join();
				mBackgroundThread = null;
				dbMsg += ">>=" + mBackgroundThread;
			}
			dbMsg += " , mBackgroundHandler=" + mBackgroundHandler;
			if ( mBackgroundHandler != null ) {
				mBackgroundHandler = null;
				dbMsg += ">>=" + mBackgroundHandler;
			}
			myLog(TAG , dbMsg);
		} catch (InterruptedException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	/**
	 * プレビューの生成
	 * Creates a new {@link CameraCaptureSession} for camera preview.
	 * 　onOpene	から呼ばれる
	 * 各ViewはonCreateで追加する
	 */
	private void createCameraPreviewSession() {
		final String TAG = "createCameraPreviewSession[MA]";
		String dbMsg = "";
		try {
			int tWidth = mPreviewSize.getWidth();
			int tHight = mPreviewSize.getHeight();
			dbMsg = "PreviewSize[" + tWidth + "×" + tHight + "]mSensorOrientation=" + mSensorOrientation;
			dbMsg += ",isTexturView=" + isTexturView;                 //高速プレビュー
			if ( mTextureView != null ) {
				SurfaceTexture texture = mTextureView.getSurfaceTexture();
				assert texture != null;
				texture.setDefaultBufferSize(tWidth , tHight);     // バッファサイズを、プレビューサイズに合わせる
				surface = new Surface(texture);   // プレビューが描画されるSurface	This is the output Surface we need to start preview.
			} else if ( ma_sarface_view != null ) {
				ArrayList< Surface > surfaceList = new ArrayList();
				surfaceList.add(ma_sarface_view.getHolder().getSurface());                // プレビュー用のSurfaceViewをリストに登録
				surface = ma_sarface_view.getHolder().getSurface();
//				try {
//					// プレビューリクエストの設定（SurfaceViewをターゲットに）
//					mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//					mPreviewRequestBuilder.addTarget(ma_sarface_view.getHolder().getSurface());
//
//					// キャプチャーセッションの開始(セッション開始後に第2引数のコールバッククラスが呼ばれる)
//					mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
//						@Override
//						public void onConfigured(CameraCaptureSession cameraCaptureSession) {
//							final String TAG = "CCPS.onConfigured[MA]";
//							String dbMsg = "";
//							try {
//								if ( null != mCameraDevice ) {  // カメラが閉じていなければ	// The camera is already closed
//									mCaptureSession = cameraCaptureSession;        // When the session is ready, we start displaying the preview.
//									dbMsg += ",getId=" + mCaptureSession.getDevice().getId();
//									try {
//										PointF[] focusPoints = {new PointF(mPreviewSize.getWidth() / 2 , mPreviewSize.getHeight() / 2)};
//										dbMsg += ",focusPoints(" + focusPoints[0].x + "," + focusPoints[0].y + ")";
//										startAutoFocus(focusPoints , MainActivity.this);
//										mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//										// オートフォーカスを設定する// Auto focus should be continuous for camera preview.
//										setAutoFlash(mPreviewRequestBuilder);        // Flash is automatically enabled when necessary.
//										mPreviewRequest = mPreviewRequestBuilder.build();        // リクエスト作成// Finally, we start displaying the camera preview.
//										mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler);
//										//(7)RepeatSession作成 カメラプレビューを表示する	//APIL21;このキャプチャセッションで、イメージのキャプチャを無限に繰り返すように要求:ここの他は unlockFocus()
//									} catch (CameraAccessException er) {
//										myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//									}
//								} else {
//									dbMsg += "mCameraDevice = null";
//								}
//								myLog(TAG , dbMsg);
//							} catch (Exception er) {
//								myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//							}
//						}
//
//						@Override
//						public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
//							final String TAG = "CCPS.onConfigureFailed[MA]";
//							String dbMsg = "";
//							dbMsg += ",getId=" + cameraCaptureSession.getDevice().getId();
//							showToast("Failed");
//							myErrorLog(TAG , dbMsg + "発生；");
//						}
//					} , null);
//				} catch (CameraAccessException e) {
//					// エラー時の処理を記載
//				}

			}
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
								PointF[] focusPoints = {new PointF(mPreviewSize.getWidth() / 2 , mPreviewSize.getHeight() / 2)};
								dbMsg += ",focusPoints(" + focusPoints[0].x + "," + focusPoints[0].y + ")";
								startAutoFocus(focusPoints , MainActivity.this);
								mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
								// オートフォーカスを設定する// Auto focus should be continuous for camera preview.
								setAutoFlash(mPreviewRequestBuilder);        // Flash is automatically enabled when necessary.
								mPreviewRequest = mPreviewRequestBuilder.build();        // リクエスト作成// Finally, we start displaying the camera preview.
								mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler);
								//(7)RepeatSession作成 カメラプレビューを表示する	//APIL21;このキャプチャセッションで、イメージのキャプチャを無限に繰り返すように要求:ここの他は unlockFocus()
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
			dbMsg += ",view[" + viewWidth + "×" + viewHeight + "]";   //正しい値が与えられていない
			int targetViewId;
			View targetView;        //pereviewVの呼び込み枠       ViewGroup
			if ( isTexturView ) {
				dbMsg += ",TextureView";
				targetViewId = mTextureView.getId();
				targetView = ( AutoFitTextureView ) findViewById(targetViewId);        //pereviewVの呼び込み枠       ViewGroup
			} else {
				dbMsg += ",sarfaceView";
				targetViewId = ma_sarface_view.getId();
				targetView = ( SurfaceView ) findViewById(targetViewId);        //pereviewVの呼び込み枠       ViewGroup
//								ma_sarface_view.getHolder().setFixedSize(viewWidth, viewHeight);			// SurfaceViewにプレビューサイズを設定する(サンプルなので適当な値です)

			}
			dbMsg += ";Id=" + targetViewId;
			Activity activity = MainActivity.this;                //getActivity();
			ViewGroup.LayoutParams svlp = targetView.getLayoutParams();
			dbMsg += ",変更前LayoutParams[" + svlp.width + "×" + svlp.height + "]";

			if ( null != targetView && null != mPreviewSize && null != activity ) {
				int targetViewLeft = targetView.getLeft();
				int targetViewTop = targetView.getTop();
				int targetViewWidth = targetView.getWidth();
				int targetViewHeight = targetView.getHeight();
				dbMsg += ",変更前(" + targetViewLeft + "×" + targetViewTop + ")[" + targetViewWidth + "×" + targetViewHeight + "]";
				int vgWIDTH = ma_preview_fl.getWidth();
				int vgHEIGHT = ma_preview_fl.getHeight();
				dbMsg += ",読込みViewGroup[" + vgWIDTH + "×" + vgHEIGHT + "]";
				int pvWidth = mPreviewSize.getWidth();
				int pvHeight = mPreviewSize.getHeight();
				dbMsg += ",最大プレビューサイズ[" + pvWidth + "×" + pvHeight + "]";
				int orientation = getResources().getConfiguration().orientation;
				dbMsg += ",orientation=" + orientation;
				if ( mTextureView != null ) {
					if ( orientation == Configuration.ORIENTATION_LANDSCAPE ) {
						dbMsg += ";横";
						int retention = pvWidth;
						pvWidth = pvHeight;
						pvHeight = retention;
					} else {
						dbMsg += ";縦";
						int retention = vgWIDTH;
						vgWIDTH = vgHEIGHT;
						vgHEIGHT = retention;
					}
					dbMsg += ",読込みViewGroup[" + vgWIDTH + "×" + vgHEIGHT + "]";
					dbMsg += ",>>プレビューサイズ[" + pvWidth + "×" + pvHeight + "]";
					dbMsg += ",isAvailable=" + mTextureView.isAvailable();
					Matrix matrix = new Matrix();            //org
					RectF viewRect = new RectF(0 , 0 , viewWidth , viewHeight);        //org viewWidth , viewHeight        vgWIDTH , vgHEIGHT
					RectF bufferRect = new RectF(0 , 0 , pvWidth , pvHeight);
					float centerX = viewRect.centerX();
					float centerY = viewRect.centerY();
					dbMsg += ",center;ViewGrupe(" + centerX + "," + centerY + ")とpreview(" + bufferRect.centerX() + "," + bufferRect.centerY() + ")";
					float dx = centerX - bufferRect.centerX();
					float dy = centerY - bufferRect.centerY();
					dbMsg += ",shift(" + dx + "," + dy + ")";
					int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
					dbMsg += ",rotation=" + rotation;
					if ( Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation ) {   //1||3
						bufferRect.offset(dx , dy);
						matrix.setRectToRect(viewRect , bufferRect , Matrix.ScaleToFit.FILL);       //org;	FILL		START   ,    CENTER,    END
						float scale = Math.max(( float ) viewHeight / pvHeight , ( float ) viewWidth / pvWidth);        //	org
						dbMsg += ",scale=" + scale;
						matrix.postScale(scale , scale , centerX , centerY);
						matrix.postRotate(90 * (rotation - 2) , centerX , centerY);                    //  270 || 90
					} else if ( Surface.ROTATION_0 == rotation || Surface.ROTATION_180 == rotation ) {            //    0 || 2                                               //org
						matrix.postRotate(180 * (rotation - 2) , centerX , centerY);                    // -180 || 0
					}
					mTextureView.setTransform(matrix);
				} else if ( ma_sarfaceeHolder != null ) {      //ma_sarfaceeHolder	    ma_sarface_view
					if ( orientation == Configuration.ORIENTATION_LANDSCAPE ) {
						dbMsg += ";横";
//						int retention = pvWidth;
//						pvWidth = pvHeight;
//						pvHeight = retention;
					} else {
						dbMsg += ";縦";
						int retention = pvWidth;
						pvWidth = pvHeight;
						pvHeight = retention;
					}
					dbMsg += ",>>プレビューサイズ[" + pvWidth + "×" + pvHeight + "]";
//					ma_sarfaceeHolder.setFixedSize(pvWidth , pvHeight);
					dbMsg += ",ScaleX[" + ma_sarface_view.getScaleX() + "×" + ma_sarface_view.getScaleY() + "]";

				}
				dbMsg += ",>変更結果>(" + targetViewLeft + "×" + targetViewTop + ")[" + targetViewWidth + "×" + targetViewHeight + "]";
				FrameLayout.LayoutParams sParams = ( FrameLayout.LayoutParams ) targetView.getLayoutParams();
				dbMsg += "=(" + sParams.leftMargin + "×" + sParams.topMargin + ")[" + sParams.width + "×" + sParams.height + "]";
				dbMsg += ",gravity=" + sParams.gravity;
				dbMsg += "=(" + targetView.getLeft() + "×" + targetView.getTop() + ")[" + targetView.getWidth() + "×" + targetView.getHeight() + "]";
			}
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
	 * オートフォーカスの動作リスナー
	 */
	CameraCaptureSession.CaptureCallback mAFListener = new CameraCaptureSession.CaptureCallback() {
		@Override
		public void onCaptureCompleted(CameraCaptureSession session , CaptureRequest request , TotalCaptureResult result) {
			super.onCaptureCompleted(session , request , result);
			final String TAG = "FL.onCaptureCompleted[MA]";
			String dbMsg = "";
			try {
				dbMsg += "fState=" + fState;
				if ( fState == STATE_WAITING_LOCK_FUCUS ) {
					Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
					dbMsg += ",afState=" + afState;
					if ( afState == null ) {
						dbMsg += "onCaptureCompleted AF STATE is null";
						fState = STATE_INIT;
						autoFocusEnd(false);
						return;
					}

					if ( afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED ) {
						Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
						dbMsg += "onCaptureCompleted AF STATE = " + afState + ", AE STATE = " + aeState;
						if ( (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) ) {        //mCancel ||
							fState = STATE_INIT;
							autoFocusEnd(false);
							return;
						}
					}

					if ( afState != CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN && afState == mPreAFState ) {
						mSameAFStateCount++;
						// 同一状態上限
						dbMsg += ",mSameAFStateCount=" + mSameAFStateCount;
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
					dbMsg += "WAITING_PRE_CAPTURE AE STATE = " + aeState;
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
					dbMsg += "WAITING_NON_PRE_CAPTURE AE STATE = " + aeState;
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
				dbMsg = "isSuccess=" + isSuccess;
				// フォーカス完了/失敗時の処理
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	/**
	 * プレビュー画面上で指定したfocusPointsにオートフォーカスする
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
				activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);  //カメラ最大出力サイズ
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			if ( activeArraySize == null ) {
				activeArraySize = new Rect();
				//追加；カメラ最大出力サイズ
			}
			dbMsg += ",activeArraySize[" + activeArraySize.width() + "×" + activeArraySize.height() + "]"; //,[4672×3504
			dbMsg += ",maxRegionsAF=" + maxRegionsAF;                                                        // 1
			if ( maxRegionsAF <= 0 ) {
				return;
			}
			if ( focusPoints == null ) {
				return;
			}
			DisplayMetrics metrics = context.getResources().getDisplayMetrics();
			int r = ( int ) (4 * metrics.density);
			dbMsg += ",r=" + r + "分割？";
			dbMsg += ",focusPoints=" + focusPoints.length + "件";
			Double scaleX = 1.0 * mPreviewSize.getWidth() / activeArraySize.width();
			Double scaleY = 1.0 * mPreviewSize.getHeight() / activeArraySize.height();
			dbMsg += ",scaleY=" + scaleX + ":" + scaleY;
			Double scaleXY = scaleX;
			if ( scaleX < scaleY ) {
				scaleXY = scaleY;
			}
			dbMsg += ">>scale=" + scaleXY;

			int ariaW = ( int ) (activeArraySize.width() * scaleXY) / 3;                //追加
			int ariaH = ( int ) (activeArraySize.height() * scaleXY) / 3;                //追加
			MeteringRectangle[] afRegions = new MeteringRectangle[focusPoints.length];
			for ( int i = 0 ; i < focusPoints.length ; i++ ) {
				dbMsg += "(" + i + ")[" + focusPoints[i].x + "×" + focusPoints[i].y + "]";
				int centerX = ( int ) (focusPoints[i].x / scaleXY);            //( int ) (activeArraySize.width() * focusPoints[i].x);
				int centerY = ( int ) (focusPoints[i].y / scaleXY);        // ( int ) (activeArraySize.height() * focusPoints[i].y);
				dbMsg += ",>center(" + centerX + "," + centerY + ")";
				int rectLeft = centerX - ariaW;                        //Math.max(activeArraySize.bottom , centerX - r);
				int rectTop = centerY - ariaH;                        //Math.max(activeArraySize.top , centerY - r);
				int rectRight = centerX + ariaW;                        //Math.min(centerX + r , activeArraySize.right);
				int rectBottom = centerY + ariaH;                        //Math.min(centerY + r , activeArraySize.bottom);
				dbMsg += ",rect(" + rectLeft + "," + rectTop + ")～(" + rectRight + "×" + rectBottom + ")";
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
					captureBuilder.addTarget(surface);
					dbMsg += ",addTarget";
				}
				captureBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_AUTO);
				if ( 0 < afRegions.length ) {
					captureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS , afRegions);
					dbMsg += ",CONTROL_AF_REGIONS";
				}
				captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);  //lockFocus()はここからスタート
				mCaptureSession.setRepeatingRequest(captureBuilder.build() , mAFListener , mBackgroundHandler);//mBackgroundHandler   /
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}
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
			mSensorOrientation = getOrientation(rotation);//int camLotation         /
			dbMsg += ",カメラセンサーの方向=" + mSensorOrientation;
			captureBuilder.set(CaptureRequest.JPEG_ORIENTATION , mSensorOrientation);        // JPEG画像の方向を設定する。
			CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
				/**
				 * 撮影が終わったら、フォーカスのロックを外すためのコールバック
				 * */
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
			mCaptureSession.stopRepeating();          //現在のプレビューを停止；setRepeatingRequest または いずれかで進行中の繰り返しキャプチャをキャンセルします 。
			mCaptureSession.abortCaptures();            //Repeating requestsも止まる。
			mCaptureSession.capture(captureBuilder.build() , CaptureCallback , null);  // 撮影する。終了コールバックはメソッド内
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
	private void copyPreview() {
		final String TAG = "copyPreview[MA]";
		String dbMsg = "";
		try {
			if ( !isReWriteNow ) {                                    // //onResume～onPause以外
				if ( mCameraDevice != null ) {
					if ( mCaptureSession != null ) {
						mCaptureSession.stopRepeating();          //プレビューの更新を止める
						mCaptureSession.abortCaptures();            //Repeating requestsも止まる。☆これを加えると300>>200フレームに間隔短縮
						dbMsg += "stopRepeating";
						CaptureRequest.Builder mCopyPreviewRequestBuilder = null;                    // 静止画を送ってもらうためのリクエストのビルダーですよ
						try {
							dbMsg += ",createCaptureRequest;mCameraDevice=" + mCameraDevice;
							mCopyPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);   //静止画像キャプチャに適した要求を作成
						} catch (CameraAccessException er) {
							myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
						}
						mCopyPreviewRequestBuilder.addTarget(mImageReader.getSurface());                        // 送り先はImageReaderにしてね
						CaptureRequest copyPreviewRequest = mCopyPreviewRequestBuilder.build();
						try {
							dbMsg += ",capture";
							int retInt = mCaptureSession.capture(copyPreviewRequest , mCaptureCallback , mBackgroundHandler);
							// (プレビュー時にセッションは開いたままで、)追加で静止画送ってくれリクエストを送る
//				List<CaptureRequest> requestList = new ArrayList<CaptureRequest>();
//				// キャプチャーの指示一覧を作成
//				requestList.add(mCopyPreviewRequestBuilder.build());	//	requestList.add(captureBuilder.build());
//				int retInt = mCaptureSession.captureBurst(requestList, mCaptureCallback, mBackgroundHandler); 				// 登録した指示通りに連写で撮影
							/**
							 * キャプチャ方法４通り
							 * • CameraCaptureSession#captureBurst() 　　　　　　・・・撮影条件を変えながら複数枚撮影する
							 * • CameraCaptureSession#setRepeatingRequest() 　　　　　　・・・同一条件で連続撮影する (Preview 用 )
							 * */
							dbMsg += ",retInt=" + retInt;    //unique capture sequence IDが戻される
						} catch (CameraAccessException er) {
							myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
						}
					} else {
						dbMsg += "mCaptureSession = null ";
					}
				} else {
					dbMsg += "mCameraDevice = null ";
				}
			} else {
				dbMsg += "書き換え中 ";
			}

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

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
				if ( isReWriteNow ) {                                    // //onResume～onPause以外
					dbMsg += "書き換え中 ";
					return;                  //Fragmentなら  isDetached  とgetActivity
				}
				if ( OCVFRV == null ) {                    ///6/18	この時点でViewがまだ存在しているか
					dbMsg += ",OCVFRV=null";
					return;
				}

				if ( mCaptureSession != null ) {  //回転時クラッシュ；CAMERA_DISCONNECTED (2): checkPidStatus:1493: The camera device has been disconnected
					dbMsg += ",mPreviewRequest=" + mPreviewRequest;
					dbMsg += ",mCaptureCallback=" + mCaptureCallback;
					dbMsg += ",mBackgroundHandler=" + mBackgroundHandler;
					int retInt = mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler);    //プレビュ再開
					dbMsg += ",プレビュ再開=" + retInt;
					isPrevieSending = false;
				} else {
					dbMsg += ",mCaptureSession = null ";
//					createCameraPreviewSession();
					return;
				}
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

				mSensorOrientation = getOrientation(DISP_DEGREES);
				dbMsg += ",camera=" + mSensorOrientation + "dig";
				OCVFRV.readFrameRGB(shotBitmap , mSensorOrientation);

				if ( shotBitmap != null ) {
					shotBitmap.recycle();
					byteCount = shotBitmap.getByteCount();
					dbMsg += ">>" + byteCount + "バイト";
				}
				imageBuf.clear();
				mImage.close();
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
		isPrevieSending = false;
		isPhotography = false;
		if ( UTIL == null ) {
			UTIL = new CS_Util();
		}
		UTIL.myErrorLog(TAG , dbMsg);
	}

}

//参照 	 11 Oct 2017	https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java
// Fragmentは support.v4が必要な為、Actvtyに書き換え
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
 * <p>
 * <p>
 * <p>
 * <p>
 * 回転時クラッシュ
 * ///6
 * /18
 * この時点でLooperとThred
 * https://www.slideshare.net/fumihikoshiroyama/ss-47472394
 * <p>
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