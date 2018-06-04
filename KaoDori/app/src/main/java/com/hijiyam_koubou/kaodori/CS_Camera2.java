package com.hijiyam_koubou.kaodori;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;


public class CS_Camera2 {
	public Context context;
	public CameraDevice mCamera;
	public CameraManager mCameraManager = null;
	public CameraCharacteristics mCharacteristics;
	private TextureView mTextureView;
	private Size mCameraSize;
	public CaptureRequest.Builder mPreviewBuilder;
	private CameraCaptureSession mPreviewSession;
	private Handler previewHandler;
	private HandlerThread previewThread;
	public ImageReader jpegImageReader;
	public WindowManager mWindowManager;

	/**
	 * コンストラクタでプレビューを受け取る
	 **/
	public CS_Camera2(Context context , TextureView textureView) {
		final String TAG = "CS_Camera2[C2]";
		String dbMsg = "";
		try {
			this.context = context;
			mTextureView = textureView;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	private CameraCaptureSession.CaptureCallback mCaptureCallback;                    // =new CameraCaptureSession.CaptureCallback(){ 		};

	/**
	 * 起動/終了時に呼ばれるコールバック；起動時一回；
	 */
	private CameraDevice.StateCallback mCameraDeviceCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(CameraDevice camera) {
			final String TAG = "onOpened[C2]";
			String dbMsg = "";
			try {
				//			mCameraOpenCloseLock.release();
				mCamera = camera;
				createCaptureSession();            //プレビュー設定
				//		surfaceList.add(jpegImageReader.getSurface());      				// 写真保存用のイメージリーダをリストに追加
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onDisconnected(CameraDevice camera) {
			final String TAG = "onDisconnected[C2]";
			String dbMsg = "";
			try {
				//			mCameraOpenCloseLock.release();
				camera.close();
				mCamera = null;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onError(CameraDevice camera , int error) {
			final String TAG = "onError[C2]";
			String dbMsg = "";
			try {
				//			mCameraOpenCloseLock.release();
				camera.close();
				mCamera = null;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};
	//起動/終了時に呼ばれるコールバック

	/**
	 * カメラが起動して使える状態になったら呼ばれるコールバック
	 */
	CameraCaptureSession.StateCallback mCameraCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
		/**
		 * プレビューの更新
		 * **/
		@Override
		public void onConfigured(CameraCaptureSession session) {
			final String TAG = "onConfigured[C2]";
			String dbMsg = "";
			try {
				mPreviewSession = session;
				mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);                             // オートフォーカスの設定
				updatePreview();
				mPreviewSession.setRepeatingRequest(mPreviewBuilder.build() , mCaptureCallback , null);                //ここでプレビューの更新イベント
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onConfigureFailed(CameraCaptureSession session) {
			final String TAG = "onConfigureFailed[C2]";
			String dbMsg = "";
			try {
				dbMsg = "onConfigureFailed";
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};
	//カメラが起動して使える状態になったら呼ばれるコールバック		プレビューの更新；

	/**
	 * CameraManagerにオープン要求
	 */
	@SuppressLint ( "MissingPermission" )
	public void open() {
		final String TAG = "open[C2]";
		String dbMsg = "";
		try {
			if ( mCameraManager == null ) {
				mCameraManager = ( CameraManager ) context.getSystemService(Context.CAMERA_SERVICE);      //逐次取り直さないとダメ？
			}

			dbMsg += "、getCameraIdList=" + mCameraManager.getCameraIdList().length + "件";
			for ( String cameraId : mCameraManager.getCameraIdList() ) {
				dbMsg += "cameraId=" + cameraId;
				if ( mCharacteristics == null ) {
					mCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
				}

				if ( mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK ) { 	//1;メインカメラ
					dbMsg += ";LENS_FACING_BACK";
					StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
					mCameraSize = map.getOutputSizes(SurfaceTexture.class)[0];     												//APIL21
					dbMsg += ">mCameraSize>[" + mCameraSize.getWidth() + "x" + mCameraSize.getHeight() + "]";
					/**
					 ImageReader mImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 3);       //    ImageReaderを生成
					 mImageReader.setOnImageAvailableListener(mTakePictureAvailableListener, null);
					 ImageReader.OnImageAvailableListener mTakePictureAvailableListener =new ImageReader.OnImageAvailableListener() {
					 Image image = reader.acquireNextImage();
					 //または	Image image = reader.acquireLatestImage();	で	Image(android.media.Image)を取得
					 //取得したImageを処理します。(保存など)
					 image.close();					// Imageを解放します。これを忘れるとバースト撮影などで失敗します。
					 };
					 * */
					mCameraManager.openCamera(cameraId , mCameraDeviceCallback , null);             //CameraManagerにオープン要求を出します。
					break;                    //	return;
				}else if ( mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT ) {		//2;サブカメラ

				}
			}
			myLog(TAG , dbMsg);
		} catch (CameraAccessException er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * CaptureSessionを生成  ;起動時一回
	 */
	private void createCaptureSession() {
		final String TAG = "createCaptureSession[C2]";
		String dbMsg = "";
		try {
			dbMsg = "isAvailable=" + mTextureView.isAvailable();
			if ( mTextureView.isAvailable() ) {
				SurfaceTexture texture = mTextureView.getSurfaceTexture();
				texture.setDefaultBufferSize(mCameraSize.getWidth() , mCameraSize.getHeight());                     //プレビュー用のSurfaceを生成します。
				Surface surface = new Surface(texture);
				try {
					mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);   // プレビュー用のCaptureRequest.Builderを生成
				} catch (CameraAccessException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}

				mPreviewBuilder.addTarget(surface);   //CaptureRequest.Builderにプレビュー用のSurfaceを設定
				try {
					mCamera.createCaptureSession(Collections.singletonList(surface) , mCameraCaptureSessionCallback , null);        //キャプチャーセッションの開始(セッション開始後に第2引数のコールバッククラスが呼ばれる)
				} catch (CameraAccessException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			/**
			 // プレビュー用のSurfaceViewをリストに登録
			 SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
			 ArrayList<Surface> surfaceList = new ArrayList();
			 surfaceList.add(surfaceView.getHolder().getSurface());

			 try {
			 // プレビューリクエストの設定（SurfaceViewをターゲットに）
			 mPreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			 mPreviewRequestBuilder.addTarget(surfaceView.getHolder().getSurface());

			 // キャプチャーセッションの開始(セッション開始後に第2引数のコールバッククラスが呼ばれる)
			 cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSessionCallback(), null);

			 } catch (CameraAccessException e) {
			 // エラー時の処理を記載
			 }
			 * **/
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}                                                                                                //CaptureSessionを生成

	/**
	 * プレビューを開始
	 */
	public void updatePreview() {
		final String TAG = "updatePreview[C2]";
		String dbMsg = "";
		try {
			if(mCameraSize!=null){
				if(0<mCameraSize.getWidth() && 0<mCameraSize.getHeight()){
					jpegImageReader = ImageReader.newInstance(mCameraSize.getWidth(), mCameraSize.getHeight() , ImageFormat.JPEG , 1);    //640 , 480
				}
			}
			// キャプチャ取得用のイメージリーダを作成
     			mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION , setCameraRotation());
			mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			previewThread = new HandlerThread("CameraPreview");
			previewThread.start();
			previewHandler = new Handler(previewThread.getLooper());

			try {
				mPreviewSession.setRepeatingRequest(mPreviewBuilder.build() , null , previewHandler); //プレビューを開始
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			Integer sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
			dbMsg += ">sensor=" + sensorOrientation +"dig";
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}                                                            //プレビューを開始

	/***
	 * 回転補正
	 * 参照		https://moewe-net.com/android/2016/camera2-jpeg-orientation
	 * */
	public int setCameraRotation() {
		final String TAG = "setCameraRotation[C2]";
		String dbMsg = "";
		int comDegrees = 0;
		try {
			if ( mWindowManager == null ) {
				mWindowManager = ( WindowManager ) context.getSystemService(Context.WINDOW_SERVICE);
			}
			int displayRotation = mWindowManager.getDefaultDisplay().getRotation();
			dbMsg += ",Window=" + displayRotation;
			switch ( displayRotation ) {
				case Surface.ROTATION_0:
					dbMsg += "=0;上";
					break;
				case Surface.ROTATION_90:
					dbMsg += "=90;右";
					break;
				case Surface.ROTATION_180:
					dbMsg += "=180;下";
					break;
				case Surface.ROTATION_270:
					dbMsg += "=270;左";
					break;
			}
			Configuration config = context.getResources().getConfiguration();
			switch ( config.orientation ) {
				case Configuration.ORIENTATION_PORTRAIT:
					dbMsg += ";縦方向";
					break;
				case Configuration.ORIENTATION_LANDSCAPE:
					dbMsg += ";横方向";
					break;
				default:
					dbMsg += ";デフォルト";
					break;
			}
			/**
			 *  ,Window=0=0;上;縦方向;;sensor=90>>90dig>sensor=90
			 *, ,Window=1=90;右;横方向;;sensor=90>>0dig>sensor=90
			 * Window=3=270;左;横方向,sensorOrientation=90>>180dig>>90
			 * */
			if ( mCharacteristics != null && mPreviewBuilder != null ) {
				Integer sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
				dbMsg += ";;sensor=" + sensorOrientation;
				SparseIntArray ORIENTATIONS = new SparseIntArray();
				{
					ORIENTATIONS.append(Surface.ROTATION_0 , 90);
					ORIENTATIONS.append(Surface.ROTATION_90 , 0);
					ORIENTATIONS.append(Surface.ROTATION_180 , 270);
					ORIENTATIONS.append(Surface.ROTATION_270 , 180);
				}
				comDegrees = (ORIENTATIONS.get(displayRotation));
				dbMsg += ">>" + (ORIENTATIONS.get(displayRotation)) + "dig";
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return comDegrees;
	}

	/***
	 *   camera2Dの静止画撮影
	 * */
	public void camera2Shot() {
		final String TAG = "camera2Shot[C2]";
		String dbMsg = "";
		try {
			mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);            //撮影用のCaptureRequest.Builderを生成
			jpegImageReader = ImageReader.newInstance(640 , 480 , ImageFormat.JPEG , 1);        // キャプチャ取得用のイメージリーダを作成
			//				ImageReader mImageReader = ImageReader.newInstance(1920 , 1080 , ImageFormat.JPEG , 3);        //   ImageReaderを生成
//				jpegImageReader.setOnImageAvailableListener(mTakePictureAvailableListener, null);
			mPreviewBuilder.addTarget(jpegImageReader.getSurface());                                                                //CaptureRequest.Builderに撮影用のSurfaceを設定
			mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);                // 必要なパラメータを設定します。(サンプル)
//				mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);
			mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE , CaptureRequest.CONTROL_AE_MODE_ON);
			mPreviewSession.stopRepeating();                                                                                    //現在のプレビューを停止します。
			mPreviewSession.capture(mPreviewBuilder.build() , mCaptureCallback , null);                                    // 撮影を開始します
			mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
				@Override
				public void onCaptureCompleted(CameraCaptureSession session , CaptureRequest request , TotalCaptureResult result) {
					final String TAG = "onCaptureCompleted[C2]";
					String dbMsg = "";
					jpegImageReader = null;
					ByteBuffer buffer = jpegImageReader.acquireLatestImage().getPlanes()[0].getBuffer();                    // 画像バイナリの取得
					byte[] bytes = new byte[buffer.capacity()];
					buffer.get(bytes);

					// 画像の書き込み
					OutputStream output = null;
					try {
						output = new FileOutputStream(new File("filename"));
						output.write(bytes);
					} catch (Exception er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
					jpegImageReader.close();                    // ImageReaderのクローズ
				}
			};
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/***
	 *   camera2Dの破棄処理
	 * */
	public void camera2Dispose() {
		final String TAG = "camera2Dispose[C2]";
		String dbMsg = "";
		try {
//				mCameraOpenCloseLock.acquire();
			if ( null != mPreviewSession ) {
				mPreviewSession.close();            //CameraCaptureSessionをクローズ
				mPreviewSession = null;
			}
			mCamera.close();                      //CameraDeviceをクローズ
//		mTextureView.close();                   // ImageReaderをクローズ
			//		mCameraDeviceCallback                 //破棄は不要？
			if ( previewHandler != null ) {
				previewThread.quitSafely();        //APIL18
				previewHandler = null;
			}

			if ( null != mCamera ) {
				mCamera.close();
				mCamera = null;
			}
			if ( null != jpegImageReader ) {
				jpegImageReader.close();
				jpegImageReader = null;
			}
//			} catch (InterruptedException e) {
//				throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
////			} finally {
//	//				mCameraOpenCloseLock.release();
//			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////
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
 * E/mm-camera: <STATS_AEC ><ERROR> 1954: cip_aec_wrapper_pack_output: invalid input: 0xe387b000,0x0
 * E/mm-camera: <IFACE ><ERROR> 2260: iface_util_set_chromatix: iface_util_set_chromatix:2260 failed: iface_hvx_open rc 0
 * E/mm-camera: <ISP   ><ERROR> 281: module_linearization40_update_sudmod_enable: failed !! linearization40_update_base_tables
 *
 * E/mm-camera: <IMGLIB><ERROR> 175: module_depth_map_handle_ctrl_parm: E
 <IMGLIB><ERROR> 335: set_depth_map_config_param: X
 <IMGLIB><ERROR> 278: module_depth_map_handle_ctrl_parm: X
 E/ANDR-PERF-MPCTL: Invalid profile no. 0, total profiles 0 only
 *
 *
 * <p>
 * <p>
 * 2015年12月06日	AndroidのCamera2 APIとOpenGL ESでカメラの映像を表示してみた		https://qiita.com/ueder/items/16be80bd1fc9ac8b0c1a/////////////////////////////////////////////////////
 */