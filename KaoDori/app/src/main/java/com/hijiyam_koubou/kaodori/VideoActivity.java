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

	public int vi_audioSource = MediaRecorder.AudioSource.MIC;			//1;mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	public int vi_videoSource = MediaRecorder.VideoSource.SURFACE;		//2;mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
	public int vi_outputFormat = MediaRecorder.OutputFormat.MPEG_4;		//2;mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
	public int vi_videoEncodingBitRate = 10000000;						//mMediaRecorder.setVideoEncodingBitRate(10000000);
	public int vi_videoFrameRate = 30;									//mMediaRecorder.setVideoFrameRate(30);
	public int vi_videoEncoder= MediaRecorder.VideoEncoder.H264;		//mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
	public int vi_audioEncoder= MediaRecorder.AudioEncoder.AAC;			//mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);



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
					Activity activity = VideoActivity.this;    // getActivity();
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


				case R.id.info: {
					Activity activity = VideoActivity.this;    // getActivity();
					if ( null != activity ) {
						new AlertDialog.Builder(activity).setMessage(R.string.intro_message).setPositiveButton(android.R.string.ok , null).show();
					}
					break;
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	@Override
	public boolean onLongClick(View v) {
		return false;
	}

	//追加機能/////////////////////////////////////////////////////////////////////////////////

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

/////////////////////////////////////////////////////////////////////////////////追加機能//

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