package com.hijiyam_koubou.kaodori;

import android.Manifest;
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
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
//			   import android.support.v4.app.ActivityCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class VideoActivity extends Activity implements View.OnClickListener, View.OnLongClickListener {
	public CS_Util UTIL;
	public OCVFaceRecognitionVeiw OCVFRV;            //顔検出View

	public FrameLayout va_preview_fl;        //pereviewVの呼び込み枠       ViewGroup
	/**
	 * An {@link AutoFitTextureView} for camera preview.
	 */
	public AutoFitTextureView mTextureView;
	public int mTextureViewID = -1;
	public SurfaceView va_sarface_view;        //  プレビュー用サーフェス
	public SurfaceHolder va_sarfaceeHolder;
	public String saveFileName;                    //プレビュー表示されているファイル名

	public FrameLayout va_effect_fl;        //OpenCVの呼び込み枠
	public ImageButton va_shot_bt;      //キャプチャーボタン
	public ImageButton va_shot2_bt;      //キャプチャーボタン
	public ImageButton va_func_bt;      //設定ボタン
	public ImageButton va_detecter_bt;      //検出ボタン
	public static ImageView va_iv;                    //撮影結果
	public Chronometer va_chronometer;                    //撮影時間

	private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
	private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
	private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
	private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

	private static final String TAG = "Camera2VideoFragment";
	private static final int REQUEST_VIDEO_PERMISSIONS = 1;
	private static final String FRAGMENT_DIALOG = "dialog";

	private static final String[] VIDEO_PERMISSIONS = {Manifest.permission.CAMERA , Manifest.permission.RECORD_AUDIO ,};

	static {
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0 , 90);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90 , 0);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180 , 270);
		DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270 , 180);
	}

	static {
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_0 , 270);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_90 , 180);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_180 , 90);
		INVERSE_ORIENTATIONS.append(Surface.ROTATION_270 , 0);
	}


	public static SharedPreferences sharedPref;
	public SharedPreferences.Editor myEditor;
	public String writeFolder = "";                        //このアプリで生成するファイルの保存場所
	public String saveFolder = "";                        //最終保存フォルダ
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

	public int vi_audioSource = ( int )MediaRecorder.AudioSource.MIC;			//1;mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	public int vi_videoSource =( int ) MediaRecorder.VideoSource.SURFACE;		//2;mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
	public int vi_outputFormat =( int ) MediaRecorder.OutputFormat.MPEG_4;		//2;mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
	public int vi_videoEncodingBitRate = 10000000;						//mMediaRecorder.setVideoEncodingBitRate(10000000);
	public int vi_videoFrameRate = 30;									//mMediaRecorder.setVideoFrameRate(30);
	public int vi_videoEncoder= ( int )MediaRecorder.VideoEncoder.H264;		//mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
	public int vi_audioEncoder=( int ) MediaRecorder.AudioEncoder.AAC;			//mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);



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
			isTexturView = true;				//prefs.isTexturView;     // = true;                 //
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
//	@Override
//	public void onRequestPermissionsResult(int requestCode , String permissions[] , int[] grantResults) {
//		final String TAG = "onRequestPermissionsResult[MA]";
//		String dbMsg = "";
//		try {
//			dbMsg = "requestCode=" + requestCode;
//			switch ( requestCode ) {
//				case REQUEST_PREF:
//					Intent intent = new Intent();
//					intent.setClass(this , this.getClass());
//					this.startActivity(intent);
//					this.finish();                    //http://attyo0.blog.fc2.com/blog-entry-9.html
////					readPref();        //ループする？
//					break;
//			}
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}

	//Life Cycle// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);                 //savedInstanceStateは初回のみ null
		final String TAG = "onCreate[VA]";
		String dbMsg = "";
		try {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_video);
			va_preview_fl = ( FrameLayout ) findViewById(R.id.va_preview_fl);        //pereviewVの呼び込み枠       ViewGroup
			va_effect_fl = ( FrameLayout ) findViewById(R.id.va_effect_fl);        //OpenCVの呼び込み枠       ViewGroup
			float maxID = Math.max(( float ) R.id.va_preview_fl , ( float ) R.id.va_effect_fl);
			va_shot_bt = ( ImageButton ) findViewById(R.id.va_shot_bt);      //キャプチャーボタン
			maxID = Math.max(( float ) R.id.va_shot_bt , ( float ) maxID);
			va_shot2_bt = ( ImageButton ) findViewById(R.id.va_shot2_bt);      //キャプチャーボタン
			maxID = Math.max(( float ) R.id.va_shot2_bt , ( float ) maxID);
			va_func_bt = ( ImageButton ) findViewById(R.id.va_func_bt);      //設定ボタン
			maxID = Math.max(( float ) R.id.va_func_bt , ( float ) maxID);
			va_detecter_bt = ( ImageButton ) findViewById(R.id.va_detecter_bt);      //検出ボタン
			maxID = Math.max(( float ) R.id.va_detecter_bt , ( float ) maxID);
			va_iv = ( ImageView ) findViewById(R.id.va_iv);                    //撮影結果
			maxID = Math.max(( float ) R.id.va_iv , ( float ) maxID);
			va_chronometer = ( Chronometer ) findViewById(R.id.va_chronometer);                    //撮影時間
			maxID = Math.max(( float ) R.id.va_iv , ( float ) maxID);
			va_shot_bt.setOnClickListener(this);
			va_shot2_bt.setOnClickListener(this);
			va_func_bt.setOnClickListener(this);
			va_detecter_bt.setOnClickListener(this);
			va_iv.setOnClickListener(this);
			va_shot_bt.setOnLongClickListener(this);
			va_func_bt.setOnLongClickListener(this);
			va_detecter_bt.setOnLongClickListener(this);
			va_iv.setOnLongClickListener(this);
			readPref();

			FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT , ViewGroup.LayoutParams.MATCH_PARENT);
//			layoutParams.weight = 1.0f;
			layoutParams.gravity = Gravity.CENTER;           //17;効いてない？
			dbMsg += ",isTexturView=" + isTexturView;                 //高速プレビュー
			if ( isTexturView ) {
				va_preview_fl.removeAllViews();                                //プレビュー削除
				mTextureView = new AutoFitTextureView(this);
				mTextureView.setLayoutParams(layoutParams);
				va_preview_fl.addView(mTextureView);
				mTextureView.setId(( int ) (maxID + 7));       //生成したViewのIDは-1なので付与が必要
				mTextureViewID = mTextureView.getId();
				dbMsg += ",mTextureView生成=" + mTextureViewID;
			} else {
////				va_sarface_view = ( SurfaceView ) findViewById(R.id.va_sarface_view);
//				va_sarface_view = new SurfaceView(this);       //  プレビュー用サーフェス
//				va_sarface_view.setLayoutParams(layoutParams);
//				Display display = getWindowManager().getDefaultDisplay();                // 画面サイズ;HardwareSize;を取得する
//				Point p = new Point();
//				display.getSize(p);
//				int hsWidth = p.x;
//				int hsHeight = p.y;
//				dbMsg += ",this[" + hsWidth + "×" + hsHeight + "]";
//				ViewGroup.LayoutParams svlp = va_sarface_view.getLayoutParams();
////				dbMsg += ",LayoutParams[" + svlp.width + "×" + svlp.height + "]";
//				svlp.width = hsWidth;    //va_sarface_view.getWidth();
//				svlp.height = hsHeight;        // va_sarface_view.getWidth() * PREVIEW_HEIGHT / PREVIEW_WIDTH;
//				if ( hsHeight < hsWidth ) {
//					hsWidth = hsHeight * 4 / 3;
//					svlp.width = hsWidth;
//				} else {
//					hsHeight = hsWidth * 4 / 3;
//					svlp.height = hsHeight;
//				}
//				dbMsg += ">>[" + hsWidth + "×" + hsHeight + "]";
//				dbMsg += ">LayoutParams>[" + svlp.width + "×" + svlp.height + "]";
//				va_sarface_view.setLayoutParams(svlp);                //ここではviewにサイズを与えるだけ。   Holderはカメラセッション開始以降で設定
//				svlp = va_sarface_view.getLayoutParams();
//				va_preview_fl.addView(va_sarface_view);
//				va_sarface_view.setId(( int ) (maxID + 8));            //生成時のみ付与する必要有り
//				va_sarface_view.getHolder().setFixedSize(hsWidth , hsHeight);            // SurfaceViewにプレビューサイズを設定する(サンプルなので適当な値です)
				// 画面縦横比設定のためViewTreeObserverにリスナー設定    	https://qiita.com/fslasht/items/be41e84cfbc4bbb91af7
//				va_preview_fl.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//					// レイアウト完了時
//					@Override
//					public void onGlobalLayout() {
//						final String TAG = "onGlobalLayout[MA}";
//						String dbMsg = "";
//						try {
////							boolean isLandscape = va_preview_fl.getWidth() > va_preview_fl.getHeight();   // 横画面か?
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
//							ViewGroup.LayoutParams svlp = va_sarface_view.getLayoutParams();
//							dbMsg += "[" + svlp.width + "×" + svlp.height + "]";
//							if ( pvHeight < pvWidth ) {
//								dbMsg += ">横画面";
//								pvWidth = pvHeight * 4 / 3;
//								dbMsg += ">>[" + pvWidth + "×" + pvHeight + "]";
//								svlp.width = pvWidth;        // va_sarface_view.getHeight() * PREVIEW_WIDTH / PREVIEW_HEIGHT;
//								svlp.height = pvHeight;// va_sarface_view.getHeight();
//							} else {
//								dbMsg += ">縦画面";
//								pvHeight = pvWidth * 4 / 3;
//								dbMsg += ">>[" + pvWidth + "×" + pvHeight + "]";
//								svlp.width = pvHeight;    //va_sarface_view.getWidth();
//								svlp.height =pvWidth ;        // va_sarface_view.getWidth() * PREVIEW_HEIGHT / PREVIEW_WIDTH;
//							}
////							dbMsg += ">>[" + pvWidth + "×" + pvHeight + "]";
////							dbMsg += ",PREVIEW[" + PREVIEW_WIDTH + "×" + PREVIEW_HEIGHT + "]";
////							ViewGroup.LayoutParams svlp = va_sarface_view.getLayoutParams();
////							dbMsg += "[" + svlp.width + "×" + svlp.height + "]";
////							if ( isLandscape ) {
////								dbMsg += ">横画面";
////								svlp.width = pvWidth;        // va_sarface_view.getHeight() * PREVIEW_WIDTH / PREVIEW_HEIGHT;
////								svlp.height = pvHeight;// va_sarface_view.getHeight();
////							} else {
////								dbMsg += ">縦画面";
////								svlp.width = pvHeight;    //va_sarface_view.getWidth();
////								svlp.height =pvWidth ;        // va_sarface_view.getWidth() * PREVIEW_HEIGHT / PREVIEW_WIDTH;
////							}
//							dbMsg += ">>[" + svlp.width + "×" + svlp.height + "]";
//							va_sarface_view.setLayoutParams(svlp);
//							myLog(TAG , dbMsg);
//						} catch (Exception er) {
//							myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//						}
//					}
//				});
				dbMsg += ",va_sarface_view生成=" + va_sarface_view.getId();
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Button to record video
	 */
	private Button mButtonVideo;

	/**
	 * A reference to the opened {@link android.hardware.camera2.CameraDevice}.
	 */
	private CameraDevice mCameraDevice;

	/**
	 * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for
	 * preview.
	 */
	private CameraCaptureSession mPreviewSession;

	/**
	 * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
	 * {@link TextureView}.
	 */
	private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture , int width , int height) {
			final String TAG = "onSurfaceTextureAvailable[VA]";
			String dbMsg = "";
			try {
				openCamera(width , height);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture , int width , int height) {
			final String TAG = "onSurfaceTextureSizeChanged[VA]";
			String dbMsg = "";
			try {
				configureTransform(width , height);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
			final String TAG = "onSurfaceTextureDestroyed[VA]";
			String dbMsg = "";
			try {
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
			final String TAG = "onSurfaceTextureUpdated[VA]";
			String dbMsg = "";
			try {
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	/**
	 * The {@link android.util.Size} of camera preview.
	 */
	private Size mPreviewSize;

	/**
	 * The {@link android.util.Size} of video recording.
	 */
	private Size mVideoSize;

	/**
	 * MediaRecorder
	 */
	private MediaRecorder mMediaRecorder;

	/**
	 * Whether the app is recording video now
	 */
	private boolean mIsRecordingVideo;

	/**
	 * An additional thread for running tasks that shouldn't block the UI.
	 */
	private HandlerThread mBackgroundThread;

	/**
	 * A {@link Handler} for running tasks in the background.
	 */
	private Handler mBackgroundHandler;

	/**
	 * A {@link Semaphore} to prevent the app from exiting before closing the camera.
	 */
	private Semaphore mCameraOpenCloseLock = new Semaphore(1);

	/**
	 * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
	 */
	private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

		@Override
		public void onOpened(@NonNull CameraDevice cameraDevice) {
			final String TAG = "onOpened[VA]";
			String dbMsg = "";
			try {
				mCameraDevice = cameraDevice;
				startPreview();
				mCameraOpenCloseLock.release();
				if ( null != mTextureView ) {
					configureTransform(mTextureView.getWidth() , mTextureView.getHeight());
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onDisconnected(@NonNull CameraDevice cameraDevice) {
			final String TAG = "onDisconnected[VA]";
			String dbMsg = "";
			try {
				mCameraOpenCloseLock.release();
				cameraDevice.close();
				mCameraDevice = null;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onError(@NonNull CameraDevice cameraDevice , int error) {
			final String TAG = "onError[VA]";
			String dbMsg = "";
			try {
				mCameraOpenCloseLock.release();
				cameraDevice.close();
				mCameraDevice = null;
				Activity activity = VideoActivity.this;    // this.getActivity();
				if ( null != activity ) {
					activity.finish();
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

	};
	private Integer mSensorOrientation;
	private String mNextVideoAbsolutePath;
	private CaptureRequest.Builder mPreviewBuilder;

	public static VideoActivity newInstance() {
		return new VideoActivity();
	}

	/**
	 * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
	 * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
	 * @param choices The list of available sizes
	 * @return The video size
	 */
	private static Size chooseVideoSize(Size[] choices) {
		for ( Size size : choices ) {
			if ( size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080 ) {
				return size;
			}
		}
		Log.e(TAG , "Couldn't find any suitable video size");
		return choices[choices.length - 1];
	}

	/**
	 * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
	 * width and height are at least as large as the respective requested values, and whose aspect
	 * ratio matches with the specified value.
	 * @param choices     The list of sizes that the camera supports for the intended output class
	 * @param width       The minimum desired width
	 * @param height      The minimum desired height
	 * @param aspectRatio The aspect ratio
	 * @return The optimal {@code Size}, or an arbitrary one if none were big enough
	 */
	private static Size chooseOptimalSize(Size[] choices , int width , int height , Size aspectRatio) {
		// Collect the supported resolutions that are at least as big as the preview Surface
		List< Size > bigEnough = new ArrayList<>();
		int w = aspectRatio.getWidth();
		int h = aspectRatio.getHeight();
		for ( Size option : choices ) {
			if ( option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height ) {
				bigEnough.add(option);
			}
		}

		// Pick the smallest of those, assuming we found any
		if ( bigEnough.size() > 0 ) {
			return Collections.min(bigEnough , new CompareSizesByArea());
		} else {
			Log.e(TAG , "Couldn't find any suitable preview size");
			return choices[0];
		}
	}
//
//	@Override
//	public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState) {
//		return inflater.inflate(R.layout.fragment_camera2_video , container , false);
//	}
//
//	@Override
//	public void onViewCreated(final View view , Bundle savedInstanceState) {
//		mTextureView = ( AutoFitTextureView ) view.findViewById(R.id.texture);
//		mButtonVideo = ( Button ) view.findViewById(R.id.video);
//		mButtonVideo.setOnClickListener(this);
//		view.findViewById(R.id.info).setOnClickListener(this);
//	}

	@Override
	public void onResume() {
		super.onResume();
		final String TAG = "onResume[VA]";
		String dbMsg = "";
		try {
			startBackgroundThread();
			if ( mTextureView.isAvailable() ) {
				openCamera(mTextureView.getWidth() , mTextureView.getHeight());
			} else {
				mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
			}
			writeFolder += File.separator + "vodeo";
			dbMsg += "writeFolder=" + writeFolder;
			if ( UTIL == null ) {
				UTIL = new CS_Util();
			}
			mNextVideoAbsolutePath = UTIL.getSaveFiles(writeFolder);
			dbMsg += ",mNextVideoAbsolutePath=" + mNextVideoAbsolutePath;
			File dFile = new File(mNextVideoAbsolutePath);
			if(dFile.exists()){
				if(dFile.isFile()){
					setLastThumbnail(mNextVideoAbsolutePath);
				}else{
					dbMsg += ";ファイルでは無い" ;
				}
			}else{
				dbMsg += ";無い" ;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	public void onPause() {
		final String TAG = "onPause[VA]";
		String dbMsg = "";
		try {
			closeCamera();
			stopBackgroundThread();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		super.onPause();
	}

	@Override
	public void onClick(View view) {
		final String TAG = "onClick[VA]";
		String dbMsg = "";
		try {
			Activity activity = VideoActivity.this;    // getActivity();
			switch ( view.getId() ) {
				case R.id.va_shot_bt: {
					dbMsg = "va_shot_bt;mIsRecordingVideo=" + mIsRecordingVideo;
					if ( mIsRecordingVideo ) {
						stopRecordingVideo();
					} else {
						startRecordingVideo();
					}
					break;
				}
				case R.id.va_iv: {     //プレビュー
					dbMsg += "=va_iv";
					if ( null != activity ) {
						Intent fIntent = new Intent(Intent.ACTION_GET_CONTENT);
//						String fName = mFile.getPath();                    //フルパスファイル名
						dbMsg += ",mNextVideoAbsolutePath=" + mNextVideoAbsolutePath;
						fIntent.setData(Uri.parse(mNextVideoAbsolutePath));       //これだけでは開かない
						fIntent.setType("video/*"); //fIntent.setDataAndType(Uri.parse(fName), "image/*");では関連無い処まで開く
						if ( fIntent.resolveActivity(getPackageManager()) != null ) {
							startActivity(fIntent);
						}
					}
					break;
				}


				case R.id.va_func_bt: {
					if ( null != activity ) {
						showMenuDialog(menuID_root);
//						Intent settingsIntent = new Intent(activity , MyPreferencesActivty.class);
//						startActivityForResult(settingsIntent , REQUEST_PREF);                    //    startActivity( settingsIntent );

//					new AlertDialog.Builder(activity).setMessage(R.string.intro_message).setPositiveButton(android.R.string.ok , null).show();
					}
//					if ( null != activity ) {
//						new AlertDialog.Builder(activity).setMessage(R.string.intro_message).setPositiveButton(android.R.string.ok , null).show();
//					}
					break;
				}
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
			Activity activity = VideoActivity.this;            //getActivity();
			String titolStr = "作成中です";
			String mggStr = "次回リリースまでお待ちください。";
			dbMsg += ",getId=" + view.getId();
			switch ( view.getId() ) {
//				case R.id.ma_shot_bt: {
//					dbMsg += "=ma_shot_bt";
////					showMenuDialog(menuID_cameara_mode_select);
//					break;
//				}
				case R.id.va_func_bt: {
					dbMsg += "=va_func_bt";
					if ( null != activity ) {
						Intent settingsIntent = new Intent(activity , MyPreferencesActivty.class);
						startActivityForResult(settingsIntent , REQUEST_PREF);  //startActivity(settingsIntent);      //
					}
					break;
				}
//				case R.id.ma_detecter_bt: {
//					dbMsg += "=ma_detecter_bt";
//					showMenuDialog(menuID_detector_select);
//					break;
//				}
//				case R.id.ma_iv: {
//					dbMsg += "=ma_iv";
//					messageShow(titolStr , mggStr);
//				}
//				break;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return false;
	}
	//メニュー機構////////////////////////////////////////////////////////////////////////
	public AlertDialog carentDlog;
	public CharSequence[] menuItems;
	public boolean[] menuItemChecks;
	static int menuID_root = 400;
	static int menuID_phot = menuID_root + 1;
	static int menuID_phot_onoff = menuID_phot + 1;
	static int menuID_effect = menuID_phot_onoff + 1;
	static int menuID_effect_onnoff = menuID_effect + 1;
	static int menuID_detector_select = menuID_effect_onnoff + 1;
	static int menuID_cameara_mode_select = menuID_detector_select + 1;

	public void detectersPref() {
		final String TAG = "detectersPref[VA}";
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
			} else if ( menuId == menuID_cameara_mode_select ) {
				menuItems = new CharSequence[]{getResources().getString(R.string.mm_mode_still) , getResources().getString(R.string.mm_mode_move) , getResources().getString(R.string.mm_mode_preview_save)};
			}

			if ( menuId == menuID_root || menuId == menuID_phot || menuId == menuID_effect || menuId == menuID_cameara_mode_select ) {          //プレーンリスト
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
		final String TAG = "myMenuSelected[VA}";
		String dbMsg = "";                    //表記が返る
		try {
			Activity activity = VideoActivity.this;            //getActivity();
			String titolStr = "作成中です";
			String mggStr = "次回リリースまでお待ちください。";
//			dbMsg += ",selectNo=" + selectNo;
//			CharSequence selctItem = menuItems[selectNo];
			dbMsg += ",selctItem=" + selctItem;
			if ( selctItem.equals(getResources().getString(R.string.mm_setting_titol)) ) {
				Intent settingsIntent = new Intent(activity, MyPreferencesActivty.class);
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
//			} else if ( selctItem.equals(getResources().getString(R.string.mm_mode_still)) ) {
//				dbMsg += ",静止画";
//			} else if ( selctItem.equals(getResources().getString(R.string.mm_mode_move)) ) {
//				dbMsg += ",動画";
//				laterDestroy();
//				Intent settingsIntent = new Intent(MainActivity.this , VideoActivity.class);
//				startActivityForResult(settingsIntent , REQUEST_VIDEO);  //startActivity(settingsIntent);      //
//			} else if ( selctItem.equals(getResources().getString(R.string.mm_mode_preview_save)) ) {
//				dbMsg += ",プレビュー保存";
//				if ( mTextureView != null ) {
//					dbMsg += ",mTextureView=" + mTextureView.getId();
//					savePreviewBitMap(mTextureView.getId());     //ここから送ると回転動作にストレス発生？ ？
//				} else if ( ma_sarface_view != null ) {
//					dbMsg += ",ma_sarface_view=" + ma_sarface_view.getId();
//					savePreviewBitMap(ma_sarface_view.getId());
//				}
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
				va_detecter_bt.setImageResource(android.R.drawable.star_on);      //    @android:drawable/star_on
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
				va_detecter_bt.setImageResource(android.R.drawable.star_off);      //    @android:drawable/star_on
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
			reStart();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void reStart() {
		final String TAG = "reStart[VA}";
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
	 * 最後に保存されたサムネイルを表示する
	 */
	public void setLastThumbnail(String _mNextVideoAbsolutePath) {
		final String TAG = "setLastThumbnail[VA]";
		String dbMsg = "";
		try {
			mNextVideoAbsolutePath = _mNextVideoAbsolutePath;
			dbMsg += "、mNextVideoAbsolutePath=" + mNextVideoAbsolutePath;
			ThumbnailUtils tu = new ThumbnailUtils();
			Bitmap shotBitmap = tu.createVideoThumbnail(mNextVideoAbsolutePath , MediaStore.Images.Thumbnails.MINI_KIND);                // sample.3gpのサムネイルを作成して表示する
			dbMsg += "[" + shotBitmap.getWidth() + "×" + shotBitmap.getHeight() + "]" + shotBitmap.getByteCount() + "バイト";
			ThumbnailControl TC = new ThumbnailControl(VideoActivity.this);
			TC.setThumbnail(shotBitmap , va_iv);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

/////////////////////////////////////////////////////////////////////////////////アプリケーション動作//

	/**
	 * Starts a background thread and its {@link Handler}.
	 */
	private void startBackgroundThread() {
		final String TAG = "startBackgroundThread[VA]";
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
	 */
	private void stopBackgroundThread() {
		final String TAG = "stopBackgroundThread[VA]";
		String dbMsg = "";
		mBackgroundThread.quitSafely();
		try {
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
			myLog(TAG , dbMsg);
		} catch (InterruptedException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Gets whether you should show UI with rationale for requesting permissions.
	 * @param permissions The permissions your app wants to request.
	 * @return Whether you can show permission rationale UI.
	 */
//	private boolean shouldShowRequestPermissionRationale(String[] permissions) {
//		for ( String permission : permissions ) {
//			if ( FragmentCompat.shouldShowRequestPermissionRationale(this , permission) ) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	/**
//	 * Requests permissions needed for recording video.
//	 */
//	private void requestVideoPermissions() {
//		if ( shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS) ) {
//			new ConfirmationDialog().show(getChildFragmentManager() , FRAGMENT_DIALOG);
//		} else {
//			FragmentCompat.requestPermissions(this , VIDEO_PERMISSIONS , REQUEST_VIDEO_PERMISSIONS);
//		}
//	}
//
//	@Override
//	public void onRequestPermissionsResult(int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults) {
//		Log.d(TAG , "onRequestPermissionsResult");
//		if ( requestCode == REQUEST_VIDEO_PERMISSIONS ) {
//			if ( grantResults.length == VIDEO_PERMISSIONS.length ) {
//				for ( int result : grantResults ) {
//					if ( result != PackageManager.PERMISSION_GRANTED ) {
//						ErrorDialog.newInstance(getString(R.string.permission_request)).show(getChildFragmentManager() , FRAGMENT_DIALOG);
//						break;
//					}
//				}
//			} else {
//				ErrorDialog.newInstance(getString(R.string.permission_request)).show(getChildFragmentManager() , FRAGMENT_DIALOG);
//			}
//		} else {
//			super.onRequestPermissionsResult(requestCode , permissions , grantResults);
//		}
//	}
//
//	private boolean hasPermissionsGranted(String[] permissions) {
//		for ( String permission : permissions ) {
//			if ( ActivityCompat.checkSelfPermission(getActivity() , permission) != PackageManager.PERMISSION_GRANTED ) {
//				return false;
//			}
//		}
//		return true;
//	}

	/**
	 * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
	 */
	@SuppressWarnings ( "MissingPermission" )
	private void openCamera(int width , int height) {
		final String TAG = "openCamera[VA]";
		String dbMsg = "";
		try {

//		if ( !hasPermissionsGranted(VIDEO_PERMISSIONS) ) {
//			requestVideoPermissions();
//			return;
//		}
			final Activity activity = VideoActivity.this;    // this.getActivity();
			if ( null == activity || activity.isFinishing() ) {
				return;
			}
			CameraManager manager = ( CameraManager ) activity.getSystemService(Context.CAMERA_SERVICE);
			dbMsg = "tryAcquire";
			if ( !mCameraOpenCloseLock.tryAcquire(2500 , TimeUnit.MILLISECONDS) ) {
				throw new RuntimeException("Time out waiting to lock camera opening.");
			}
			String cameraId = manager.getCameraIdList()[0];

			// Choose the sizes for camera preview and video recording
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
			StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
			if ( map == null ) {
				throw new RuntimeException("Cannot get available preview/video sizes");
			}
			mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
			mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class) , width , height , mVideoSize);

			int orientation = getResources().getConfiguration().orientation;
			if ( orientation == Configuration.ORIENTATION_LANDSCAPE ) {
				mTextureView.setAspectRatio(mPreviewSize.getWidth() , mPreviewSize.getHeight());
			} else {
				mTextureView.setAspectRatio(mPreviewSize.getHeight() , mPreviewSize.getWidth());
			}
			configureTransform(width , height);
			mMediaRecorder = new MediaRecorder();
			manager.openCamera(cameraId , mStateCallback , null);
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			Toast.makeText(VideoActivity.this , "Cannot access the camera." , Toast.LENGTH_SHORT).show();
			VideoActivity.this.finish();
		} catch (NullPointerException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);

			// Currently an NPE is thrown when the Camera2API is used but not supported on the
			// device this code runs.
//			ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager() , FRAGMENT_DIALOG);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while trying to lock camera opening.");
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	private void closeCamera() {
		final String TAG = "closeCamera[VA]";
		String dbMsg = "";
		try {
			mCameraOpenCloseLock.acquire();
			closePreviewSession();
			if ( null != mCameraDevice ) {
				mCameraDevice.close();
				mCameraDevice = null;
			}
			if ( null != mMediaRecorder ) {
				mMediaRecorder.release();
				mMediaRecorder = null;
			}
			myLog(TAG , dbMsg);
		} catch (InterruptedException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			throw new RuntimeException("Interrupted while trying to lock camera closing.");
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} finally {
			mCameraOpenCloseLock.release();
		}
	}

	/**
	 * Start the camera preview.
	 */
	private void startPreview() {
		final String TAG = "startPreview[VA]";
		String dbMsg = "";
		try {
			if ( null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize ) {
				return;
			}
			closePreviewSession();
			SurfaceTexture texture = mTextureView.getSurfaceTexture();
			assert texture != null;
			texture.setDefaultBufferSize(mPreviewSize.getWidth() , mPreviewSize.getHeight());
			mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

			Surface previewSurface = new Surface(texture);
			mPreviewBuilder.addTarget(previewSurface);

			mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface) , new CameraCaptureSession.StateCallback() {

				@Override
				public void onConfigured(@NonNull CameraCaptureSession session) {
					mPreviewSession = session;
					updatePreview();
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession session) {
					Activity activity = VideoActivity.this;    // this.getActivity();
					if ( null != activity ) {
						Toast.makeText(activity , "Failed" , Toast.LENGTH_SHORT).show();
					}
				}
			} , mBackgroundHandler);
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Update the camera preview. {@link #startPreview()} needs to be called in advance.
	 */
	private void updatePreview() {
		final String TAG = "updatePreview[VA]";
		String dbMsg = "";
		try {

			if ( null == mCameraDevice ) {
				return;
			}
			setUpCaptureRequestBuilder(mPreviewBuilder);
			HandlerThread thread = new HandlerThread("CameraPreview");
			thread.start();
			mPreviewSession.setRepeatingRequest(mPreviewBuilder.build() , null , mBackgroundHandler);
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
		builder.set(CaptureRequest.CONTROL_MODE , CameraMetadata.CONTROL_MODE_AUTO);
	}

	/**
	 * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
	 * This method should not to be called until the camera preview size is determined in
	 * openCamera, or until the size of `mTextureView` is fixed.
	 * @param viewWidth  The width of `mTextureView`
	 * @param viewHeight The height of `mTextureView`
	 */
	private void configureTransform(int viewWidth , int viewHeight) {
		final String TAG = "configureTransform[VA]";
		String dbMsg = "";
		try {
			Activity activity = VideoActivity.this;    // this.getActivity();
			if ( null == mTextureView || null == mPreviewSize || null == activity ) {
				return;
			}
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			Matrix matrix = new Matrix();
			RectF viewRect = new RectF(0 , 0 , viewWidth , viewHeight);
			RectF bufferRect = new RectF(0 , 0 , mPreviewSize.getHeight() , mPreviewSize.getWidth());
			float centerX = viewRect.centerX();
			float centerY = viewRect.centerY();
			if ( Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation ) {
				bufferRect.offset(centerX - bufferRect.centerX() , centerY - bufferRect.centerY());
				matrix.setRectToRect(viewRect , bufferRect , Matrix.ScaleToFit.FILL);
				float scale = Math.max(( float ) viewHeight / mPreviewSize.getHeight() , ( float ) viewWidth / mPreviewSize.getWidth());
				matrix.postScale(scale , scale , centerX , centerY);
				matrix.postRotate(90 * (rotation - 2) , centerX , centerY);
			}
			mTextureView.setTransform(matrix);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * mMediaRecorderの設定と保存先
	 */
	private void setUpMediaRecorder() throws IOException {
		final String TAG = "setUpMediaRecorder[VA]";
		String dbMsg = "";
		try {
			final Activity activity = VideoActivity.this;    // this.getActivity();
			if ( null == activity ) {
				return;
			}
			dbMsg += ",audioSource=" + vi_audioSource + ",videoSource=" + vi_videoSource;
			mMediaRecorder.setAudioSource(vi_audioSource);     	        //setUpMediaRecorder.VideoSource	;1;MediaRecorder.AudioSource.MIC
			mMediaRecorder.setVideoSource(vi_videoSource); 	        //setUpMediaRecorder.VideoSource	;2;MediaRecorder.VideoSource.SURFACE
			dbMsg += ",outputFormat=" + vi_outputFormat + ",BitRate=" + vi_videoEncodingBitRate + ",FrameRate=" + vi_videoFrameRate;
			mMediaRecorder.setOutputFormat(vi_outputFormat);           //setUpMediaRecorder.OutputFormat 	;2;MediaRecorder.OutputFormat.MPEG_4
			mMediaRecorder.setVideoEncodingBitRate(vi_videoEncodingBitRate);      //setUpMediaRecorder.setVideoEncodingBitRate 			;10000000
			mMediaRecorder.setVideoFrameRate(vi_videoFrameRate);                             //setUpMediaRecorder.setVideoFrameRate 		; 30
			dbMsg += "[" + mVideoSize.getWidth() + "×" + mVideoSize.getHeight() + "]";
			mMediaRecorder.setVideoSize(mVideoSize.getWidth() , mVideoSize.getHeight());
			dbMsg += ",videoEncoder=" + vi_videoEncoder + ",audioEncoder=" + vi_audioEncoder;
			mMediaRecorder.setVideoEncoder(vi_videoEncoder);             //setUpMediaRecorder.setVideoEncoder 	;2; MediaRecorder.VideoEncoder.H264
			mMediaRecorder.setAudioEncoder(vi_audioEncoder);             //setUpMediaRecorder.AudioEncoder  		;3; MediaRecorder.AudioEncoder.AAC
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			dbMsg += "、rotation=" + rotation;
			switch ( mSensorOrientation ) {
				case SENSOR_ORIENTATION_DEFAULT_DEGREES:
					mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
					break;
				case SENSOR_ORIENTATION_INVERSE_DEGREES:
					mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
					break;
			}
			mMediaRecorder.prepare();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

//	private String getVideoFilePath(Context context) {
//		final File dir = context.getExternalFilesDir(null);
//		return (dir == null ? "" : (dir.getAbsolutePath() + "/")) + System.currentTimeMillis() + ".mp4";
//	}

	/**
	 * ここで録画開始
	 */
	private void startRecordingVideo() {
		final String TAG = "startRecordingVideo[VA]";
		String dbMsg = "";
		try {
//			dbMsg += "mCameraDevice=" + mCameraDevice;

			if ( null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize ) {
				return;
			}
			dbMsg += ",writeFolder=" + writeFolder;
			dbMsg += ",mNextVideoAbsolutePath=" + mNextVideoAbsolutePath;
			int point = mNextVideoAbsolutePath.lastIndexOf("/");  //	String[] fNames = fName.split(".");が効かない
			saveFolder = "";
			if ( point != -1 ) {
				saveFolder = mNextVideoAbsolutePath.substring(0 , point);
			}
			dbMsg += ",saveFolder=" + saveFolder;
			final SimpleDateFormat cdf = new SimpleDateFormat("yyyy/MM/dd HHmmss");
			final Date date = new Date(System.currentTimeMillis());
			String currenTime = cdf.format(date);
			String[] currenDTs = currenTime.split(" ");
			String currenDataStr = currenDTs[0];
			String currenTimeStr = currenDTs[1];
			dbMsg += ",curren=" + currenDataStr + " の　" + currenTimeStr;
			if ( saveFolder.contains(currenDataStr) ) {
				dbMsg += "；フォルダ作成済み";
			} else {
				dbMsg += "；フォルダ作成";
				saveFolder=	writeFolder;	// + File.separator + "vodeo";
				dbMsg += "saveFolder=" + saveFolder;
				if ( UTIL == null ) {
					UTIL = new CS_Util();
				}
				UTIL.maikOrgPass(saveFolder);
				String[] currenDatas = currenDataStr.split("/");
				for ( String cDay : currenDatas ) {
					saveFolder += File.separator + cDay;
					UTIL.maikOrgPass(saveFolder);
				}
			}
			mNextVideoAbsolutePath = saveFolder + File.separator + currenTimeStr + ".mp4";
			dbMsg += ">これから撮るのは>=" + mNextVideoAbsolutePath;
			mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
			dbMsg += "[" + mPreviewSize.getWidth() + "×" + mPreviewSize.getHeight() + "]";
			closePreviewSession();
			setUpMediaRecorder();
			SurfaceTexture texture = mTextureView.getSurfaceTexture();
			dbMsg += "texture=" + texture;
			assert texture != null;
			texture.setDefaultBufferSize(mPreviewSize.getWidth() , mPreviewSize.getHeight());
			mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
			List< Surface > surfaces = new ArrayList<>();

			// Set up Surface for the camera preview
			Surface previewSurface = new Surface(texture);
			dbMsg += "previewSurface=" + previewSurface;
			surfaces.add(previewSurface);
			mPreviewBuilder.addTarget(previewSurface);

			// Set up Surface for the MediaRecorder
			Surface recorderSurface = mMediaRecorder.getSurface();
			surfaces.add(recorderSurface);
			mPreviewBuilder.addTarget(recorderSurface);

			// Start a capture session
			// Once the session starts, we can update the UI and start recording
			mCameraDevice.createCaptureSession(surfaces , new CameraCaptureSession.StateCallback() {
				/**
				 * ここで録画開始
				 * */
				@Override
				public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
					mPreviewSession = cameraCaptureSession;
					updatePreview();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// UI
//							mButtonVideo.setText(R.string.stop);
							mIsRecordingVideo = true;
							mMediaRecorder.start();                                        // Start recording
							va_shot_bt.setImageResource(android.R.drawable.ic_media_pause);
							va_chronometer.setBase(SystemClock.elapsedRealtime());                // リセット
							va_chronometer.start();
						}
					});
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
					Activity activity = VideoActivity.this;    // this.getActivity();
					if ( null != activity ) {
						Toast.makeText(activity , "Failed" , Toast.LENGTH_SHORT).show();
					}
				}
			} , mBackgroundHandler);
			myLog(TAG , dbMsg);
		} catch (CameraAccessException | IOException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	private void closePreviewSession() {
		final String TAG = "closePreviewSession[VA]";
		String dbMsg = "";
		try {
			if ( mPreviewSession != null ) {
				mPreviewSession.close();
				mPreviewSession = null;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 保存動作
	 */
	private void stopRecordingVideo() {
		final String TAG = "stopRecordingVideo[VA]";
		String dbMsg = "";
		try {

			// UI
			mIsRecordingVideo = false;
//		mButtonVideo.setText(R.string.record);
			va_shot_bt.setImageResource(android.R.drawable.presence_video_busy);
			mMediaRecorder.stop();                // Stop recording
			mMediaRecorder.reset();
			va_chronometer.stop();
			Activity activity = VideoActivity.this;    // this.getActivity();
			if ( null != activity ) {
				if ( UTIL == null ) {
					UTIL = new CS_Util();
				}
				UTIL.setContentValues(activity , "video/mp4" , mNextVideoAbsolutePath);
				setLastThumbnail(mNextVideoAbsolutePath);


				Toast.makeText(activity , "Video saved: " + mNextVideoAbsolutePath , Toast.LENGTH_SHORT).show();
				dbMsg = ",Video saved: " + mNextVideoAbsolutePath;
			}
//			mNextVideoAbsolutePath = null;
			va_chronometer.setBase(SystemClock.elapsedRealtime());                // リセット

			startPreview();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	/**
	 * Compares two {@code Size}s based on their areas.
	 */
	static class CompareSizesByArea implements Comparator< Size > {

		@Override
		public int compare(Size lhs , Size rhs) {
			// We cast here to ensure the multiplications won't overflow
			return Long.signum(( long ) lhs.getWidth() * lhs.getHeight() - ( long ) rhs.getWidth() * rhs.getHeight());
		}

	}

//	public static class ErrorDialog extends DialogFragment {
//
//		private static final String ARG_MESSAGE = "message";
//
//		public static ErrorDialog newInstance(String message) {
//
//			ErrorDialog dialog = new ErrorDialog();
//			Bundle args = new Bundle();
//			args.putString(ARG_MESSAGE , message);
//			dialog.setArguments(args);
//			return dialog;
//		}
//
//		@Override
//		public Dialog onCreateDialog(Bundle savedInstanceState) {
//			final Activity activity = getActivity();
//			return new AlertDialog.Builder(activity).setMessage(getArguments().getString(ARG_MESSAGE)).setPositiveButton(android.R.string.ok , new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface dialogInterface , int i) {
//					activity.finish();
//				}
//			}).create();
//		}
//
//	}

//	public static class ConfirmationDialog extends DialogFragment {
//
//		@Override
//		public Dialog onCreateDialog(Bundle savedInstanceState) {
//			final Fragment parent = getParentFragment();
//			return new AlertDialog.Builder(getActivity()).setMessage(R.string.permission_request).setPositiveButton(android.R.string.ok , new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface dialog , int which) {
//					FragmentCompat.requestPermissions(parent , VIDEO_PERMISSIONS , REQUEST_VIDEO_PERMISSIONS);
//				}
//			}).setNegativeButton(android.R.string.cancel , new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface dialog , int which) {
//					parent.getActivity().finish();
//				}
//			}).create();
//		}
//	}

	///////////////////////////////////////////////////////////////////////////////////
	public void messageShow(String titolStr , String mggStr) {
		if ( UTIL == null ) {
			UTIL = new CS_Util();
		}
		UTIL.messageShow(titolStr , mggStr , VideoActivity.this);
	}

	public void myLog(String TAG , String dbMsg) {
		if ( UTIL == null ) {
			UTIL = new CS_Util();
		}
		UTIL.myLog(TAG , dbMsg);
	}

	public void myErrorLog(String TAG , String dbMsg) {
		isPrevieSending = false;
//		isPhotography = false;
		if ( UTIL == null ) {
			UTIL = new CS_Util();
		}
		UTIL.myErrorLog(TAG , dbMsg);
	}

}

/**
 * android-Camera2Video		 https://github.com/googlesamples/android-Camera2Video/tree/master/Application/src/main/java/com/example/android/camera2video
 * <p>
 * バックグラウンド常時録画		     https://qiita.com/sckzw/items/a2a8e59d8eed19b77905
 **/