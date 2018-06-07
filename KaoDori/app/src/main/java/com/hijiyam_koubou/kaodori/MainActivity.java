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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
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
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements View.OnClickListener {
	private AutoFitTextureView mTextureView;
	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
	private static final int REQUEST_CAMERA_PERMISSION = 1;
	private static final String FRAGMENT_DIALOG = "dialog";

	static {
		ORIENTATIONS.append(Surface.ROTATION_0 , 90);
		ORIENTATIONS.append(Surface.ROTATION_90 , 0);
		ORIENTATIONS.append(Surface.ROTATION_180 , 270);
		ORIENTATIONS.append(Surface.ROTATION_270 , 180);
	}

	private static final String TAG = "Camera2BasicFragment";
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
	 * Max preview width that is guaranteed by Camera2 API
	 */
	private static final int MAX_PREVIEW_WIDTH = 1920;

	/**
	 * Max preview height that is guaranteed by Camera2 API
	 */
	private static final int MAX_PREVIEW_HEIGHT = 1080;



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
					readPref();        //ループする？
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
			readPref();
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_main);
			mTextureView = ( AutoFitTextureView ) findViewById(R.id.ma_aft);

			findViewById(R.id.picture).setOnClickListener(this);
			findViewById(R.id.info).setOnClickListener(this);
			mFile = new File(writeFolder, "pic.jpg");                 //getActivity().getExternalFilesDir(null)
			dbMsg += ",mFile=" + mFile.getAbsolutePath();
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
			if(mTextureView!= null){
				if ( mTextureView.isAvailable() ) {
					//Attempt to invoke virtual method 'boolean com.hijiyam_koubou.kaodori.AutoFitTextureView.isAvailable()' on a null object reference
					openCamera(mTextureView.getWidth() , mTextureView.getHeight());
				} else {
					mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

				}
			} else{
				dbMsg += "mTextureView== null";
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
			closeCamera();
			stopBackgroundThread();
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
			if(hasFocus){
				if(mTextureView!= null){
					if ( mTextureView.isAvailable() ) {
						//Attempt to invoke virtual method 'boolean com.hijiyam_koubou.kaodori.AutoFitTextureView.isAvailable()' on a null object reference
						openCamera(mTextureView.getWidth() , mTextureView.getHeight());
					} else {
						mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
					}
				} else{
					dbMsg += "mTextureView== null";
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
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
		switch ( view.getId() ) {
			case R.id.picture: {
				takePicture();
				break;
			}
			case R.id.info: {
				Activity activity = MainActivity.this;            //getActivity();
				if ( null != activity ) {
					new AlertDialog.Builder(activity).setMessage(R.string.intro_message).setPositiveButton(android.R.string.ok , null).show();
				}
				break;
			}
		}
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void callQuit() {
		final String TAG = "callQuit[MA}";
		String dbMsg = "";
		try {
//			if ( isTextureView ) {
//				if ( myTextureView == null ) {
//					myTextureView.surfaceDestroy();
//				}
//			} else if ( isC2 ) {
//				if ( c2SufaceView == null ) {
//					c2SufaceView.surfaceDestroy();
//				}
//			} else {
//				if ( mySurfaceView == null ) {
//					mySurfaceView.surfaceDestroy();
//				}
//			}
			closeCamera();
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

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
				dbMsg = "[" + width + "×"+ height+"]";
				openCamera(width, height);
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
				dbMsg = "[" + width + "×"+ height+"]";
				configureTransform(width , height);
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

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture texture) {
			final String TAG = "onSurfaceTextureUpdated[MA]";
			String dbMsg = "";
			try {
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

	};
	///////////////////////////////////////////////////////////////////////////////////////////////
	private String mCameraId;        //ID of the current {@link CameraDevice}.


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
				createCameraPreviewSession();
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onDisconnected(CameraDevice cameraDevice) {
			final String TAG = "onDisconnected[MA]";
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
		public void onError(CameraDevice cameraDevice , int error) {
			final String TAG = "onError[MA]";
			String dbMsg = "";
			try {
				mCameraOpenCloseLock.release();
				cameraDevice.close();
				mCameraDevice = null;
				Activity activity = MainActivity.this;    // getActivity();
				if ( null != activity ) {
					activity.finish();
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
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
	 */
	private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

		@Override
		public void onImageAvailable(ImageReader reader) {
			final String TAG = "onImageAvailable[MA]";
			String dbMsg = "";
			try {
				mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage() , mFile));
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	private CaptureRequest.Builder mPreviewRequestBuilder;        // {@link CaptureRequest.Builder} for the camera preview
	private CaptureRequest mPreviewRequest;    //{@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}


	/**
	 * The current state of camera state for taking pictures.
	 * @see #mCaptureCallback
	 */
	private int mState = STATE_PREVIEW;
	private Semaphore mCameraOpenCloseLock = new Semaphore(1);        // A {@link Semaphore} to prevent the app from exiting before closing the camera.

	/**
	 * Whether the current camera device supports Flash or not.
	 */
	private boolean mFlashSupported;

	/**
	 * Orientation of the camera sensor
	 */
	private int mSensorOrientation;

	/**
	 * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
	 */
	private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
		private void process(CaptureResult result) {
			final String TAG = "process[MA]";
			String dbMsg = "";
			try {
				switch ( mState ) {
					case STATE_PREVIEW: {
						// We have nothing to do when the camera preview is working normally.
						break;
					}
					case STATE_WAITING_LOCK: {
						Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
						if ( afState == null ) {
							captureStillPicture();
						} else if ( CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ) {
							// CONTROL_AE_STATE can be null on some devices
							Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
							if ( aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED ) {
								mState = STATE_PICTURE_TAKEN;
								captureStillPicture();
							} else {
								runPrecaptureSequence();
							}
						}
						break;
					}
					case STATE_WAITING_PRECAPTURE: {
						// CONTROL_AE_STATE can be null on some devices
						Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
						if ( aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ) {
							mState = STATE_WAITING_NON_PRECAPTURE;
						}
						break;
					}
					case STATE_WAITING_NON_PRECAPTURE: {
						// CONTROL_AE_STATE can be null on some devices
						Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
						if ( aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE ) {
							mState = STATE_PICTURE_TAKEN;
							captureStillPicture();
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
			final String TAG = "onCaptureCompleted[MA]";
			String dbMsg = "";
			try {
				process(result);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

	};


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
		Size retSize=null;
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
				retSize=  Collections.min(bigEnough , new CompareSizesByArea());
			} else if ( notBigEnough.size() > 0 ) {
				retSize= Collections.max(notBigEnough , new CompareSizesByArea());
			} else {
				dbMsg = "Couldn't find any suitable preview size";
				retSize= choices[0];
			}
			dbMsg += ",retSize=" + retSize;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
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
					dbMsg += ",cameraId=" +cameraId;
					CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

					Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);    					// We don't use a front facing camera in this sample.
					dbMsg += ",facing=" +facing +"0;FRONT";
					if ( facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT ) {
						continue;
					}

					StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
					if ( map == null ) {
						continue;
					}

					// For still image captures, we use the largest available size.
					Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)) , ( Comparator< ? super Size > ) new CompareSizesByArea());
					dbMsg += "m,largest[" + largest.getWidth() + "×" + largest.getHeight()  + "]";
					mImageReader = ImageReader.newInstance(largest.getWidth() , largest.getHeight() , ImageFormat.JPEG , /*maxImages*/2);
					mImageReader.setOnImageAvailableListener(mOnImageAvailableListener , mBackgroundHandler);

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
					dbMsg += ",rotatedPreview[" + rotatedPreviewWidth + "×" + rotatedPreviewHeight  + "]";
					dbMsg += ",maxPreview[" + maxPreviewWidth + "×" + maxPreviewHeight  + "]";

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
					dbMsg += ",Preview[" + mPreviewSize.getWidth() + "×" + mPreviewSize.getHeight()  + "]";

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

	//
	// Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.

	@SuppressLint ( "MissingPermission" )
	private void openCamera(int width , int height) {
		final String TAG = "openCamera[MA]";
		String dbMsg = "";
		try {
			dbMsg = "[" + width + "×" + height  + "]";
			setUpCameraOutputs(width , height);
			configureTransform(width , height);
			Activity activity = MainActivity.this;            //getActivity();
			CameraManager manager = ( CameraManager ) activity.getSystemService(Context.CAMERA_SERVICE);
			try {
				if ( !mCameraOpenCloseLock.tryAcquire(2500 , TimeUnit.MILLISECONDS) ) {
					throw new RuntimeException("Time out waiting to lock camera opening.");
				}
				dbMsg = ",mCameraId=" + mCameraId;
				dbMsg = ",mStateCallback=" + mStateCallback;
				dbMsg = ",mBackgroundHandler=" + mBackgroundHandler;
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
				mCameraOpenCloseLock.acquire();
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
	 */
	private void createCameraPreviewSession() {
		final String TAG = "onPause[MA]";
		String dbMsg = "";
		try {
			SurfaceTexture texture = mTextureView.getSurfaceTexture();
			assert texture != null;

			// We configure the size of default buffer to be the size of camera preview we want.
			texture.setDefaultBufferSize(mPreviewSize.getWidth() , mPreviewSize.getHeight());

			// This is the output Surface we need to start preview.
			Surface surface = new Surface(texture);

			// We set up a CaptureRequest.Builder with the output Surface.
			mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			mPreviewRequestBuilder.addTarget(surface);

			// Here, we create a CameraCaptureSession for camera preview.
			mCameraDevice.createCaptureSession(Arrays.asList(surface , mImageReader.getSurface()) , new CameraCaptureSession.StateCallback() {

				@Override
				public void onConfigured(CameraCaptureSession cameraCaptureSession) {
					final String TAG = "onPause[MA]";
					String dbMsg = "";
					try {
						// The camera is already closed
						if ( null == mCameraDevice ) {
							return;
						}

						// When the session is ready, we start displaying the preview.
						mCaptureSession = cameraCaptureSession;
						try {
							// Auto focus should be continuous for camera preview.
							mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
							// Flash is automatically enabled when necessary.
							setAutoFlash(mPreviewRequestBuilder);

							// Finally, we start displaying the camera preview.
							mPreviewRequest = mPreviewRequestBuilder.build();
							mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler);
						} catch (CameraAccessException er) {
							myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
						}
						myLog(TAG , dbMsg);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
				}

				@Override
				public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
					final String TAG = "onPause[MA]";
					String dbMsg = "";
					try {
						dbMsg = "";
						showToast("Failed");
						myLog(TAG , dbMsg);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
				}
			} , null);

			myLog(TAG , dbMsg);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
	 * This method should be called after the camera preview size is determined in
	 * setUpCameraOutputs and also the size of `mTextureView` is fixed.
	 * @param viewWidth  The width of `mTextureView`
	 * @param viewHeight The height of `mTextureView`
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
			dbMsg += ",center(" + centerY +","+ centerY+ ")";
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

	/**
	 * Initiate a still image capture.
	 */
	private void takePicture() {
		final String TAG = "takePicture[MA]";
		String dbMsg = "";
		try {
			lockFocus();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Lock the focus as the first step for a still image capture.
	 */
	private void lockFocus() {
		final String TAG = "lockFocus[MA]";
		String dbMsg = "";
		try {
			if(mPreviewRequestBuilder!= null) {
				mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);            // This is how to tell the camera to lock focus.
				mState = STATE_WAITING_LOCK;            // Tell #mCaptureCallback to wait for the lock.
				mCaptureSession.capture(mPreviewRequestBuilder.build() , mCaptureCallback , mBackgroundHandler);
			}   else{
				dbMsg = "mPreviewRequestBuilder== null";
			}
			myLog(TAG , dbMsg);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			// Attempt to invoke virtual method 'void android.hardware.camera2.CaptureRequest$Builder.
			// set(android.hardware.camera2.CaptureRequest$Key, java.lang.Object)' on a null object reference
		}

	}

	/**
	 * Run the precapture sequence for capturing a still image. This method should be called when
	 * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
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
		} catch (CameraAccessException e) {
			e.printStackTrace();
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
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
			// This is the CaptureRequest.Builder that we use to take a picture.
			final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			captureBuilder.addTarget(mImageReader.getSurface());

			// Use the same AE and AF modes as the preview.
			captureBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			setAutoFlash(captureBuilder);

			// Orientation
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			captureBuilder.set(CaptureRequest.JPEG_ORIENTATION , getOrientation(rotation));

			CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
				@Override
				public void onCaptureCompleted(CameraCaptureSession session , CaptureRequest request , TotalCaptureResult result) {
					final String TAG = "onCaptureCompleted[MA]";
					String dbMsg = "";
					try {
						dbMsg = "Saved: " + mFile;
						Log.d(TAG , mFile.toString());
						unlockFocus();
						myLog(TAG , dbMsg);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}

				}
			};

			mCaptureSession.stopRepeating();
			mCaptureSession.abortCaptures();
			mCaptureSession.capture(captureBuilder.build() , CaptureCallback , null);
			myLog(TAG , dbMsg);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Retrieves the JPEG orientation from the specified screen rotation.
	 * @param rotation The screen rotation.
	 * @return The JPEG orientation (one of 0, 90, 270, and 360)
	 */
	private int getOrientation(int rotation) {
		final String TAG = "getOrientation[MA]";
		String dbMsg = "";
		try {
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
	 * Unlock the focus. This method should be called when still image capture sequence is
	 * finished.
	 */
	private void unlockFocus() {
		final String TAG = "unlockFocus[MA]";
		String dbMsg = "";
		try {
			// Reset the auto-focus trigger
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
			setAutoFlash(mPreviewRequestBuilder);
			mCaptureSession.capture(mPreviewRequestBuilder.build() , mCaptureCallback , mBackgroundHandler);
			// After this, the camera will go back to the normal state of preview.
			mState = STATE_PREVIEW;
			mCaptureSession.setRepeatingRequest(mPreviewRequest , mCaptureCallback , mBackgroundHandler);
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
	 * Saves a JPEG {@link Image} into the specified {@link File}.
	 */
	private static class ImageSaver implements Runnable {

		/**
		 * The JPEG image
		 */
		private Image mImage;
		/**
		 * The file we save the image into.
		 */
		private File mFile;

		ImageSaver(Image image , File file) {
			final String TAG = "ImageSaver[MA]";
			String dbMsg = "";
			try {
				mImage = image;
				mFile = file;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void run() {
			final String TAG = "run[MA]";
			String dbMsg = "";
			try {
				ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
				byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				FileOutputStream output = null;
				try {
					output = new FileOutputStream(mFile);
					output.write(bytes);
				} catch (IOException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
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
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
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
				// We cast here to ensure the multiplications won't overflow
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			return Long.signum(( long ) lhs.getWidth() * lhs.getHeight() - ( long ) rhs.getWidth() * rhs.getHeight());
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
//	public void onClickShutter(View view) {
//		mCamera2.takePicture(new ImageReader.OnImageAvailableListener() {
//			@Override
//			public void onImageAvailable(ImageReader reader) {
//				final String TAG = "takePicture[MA]";
//				String dbMsg = "";
//				try {
//					final Image image = reader.acquireLatestImage();    					// 撮れた画像をImageViewに貼り付けて表示。
//					ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//					byte[] bytes = new byte[buffer.remaining()];
//					buffer.get(bytes);
//					Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//					image.close();
//
//					mImageView.setImageBitmap(bitmap);
//					mImageView.setVisibility(View.VISIBLE);
//					mTextureView.setVisibility(View.INVISIBLE);
//					myLog(TAG , dbMsg);
//				} catch (Exception er) {
//					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//				}
//			}
//		});
//	}

	///////////////////////////////////////////////////////////////////////////////////
	public void messageShow(String titolStr , String mggStr) {
		CS_Util UTIL = new CS_Util();
		UTIL.messageShow(titolStr , mggStr , MainActivity.this);
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

//参照 	 11 Oct 2017	https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java
//参照		 http://blog.kotemaru.org/2015/05/23/android-camera2-sample.html
/**
 * 保存ファイル名
 *
 * E/mm-camera: <STATS_AF ><ERROR> 4436: af_port_handle_pdaf_stats: Fail to init buf divert ack ctrl

 * */