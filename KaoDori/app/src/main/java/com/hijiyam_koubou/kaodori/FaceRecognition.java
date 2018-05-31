package com.hijiyam_koubou.kaodori;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
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
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;

//import android.hardware.Camera;
//import android.hardware.Camera;

public class FaceRecognition extends TextureView implements TextureView.SurfaceTextureListener {
	//SurfaceHolder.Callback, Camera.PreviewCallback
//	private static final String TAG = "CameraView";
	private Context context;
	public SurfaceTexture surface;

	private int degrees;
	private Camera camera;
	private int[] rgb;
	private Bitmap bitmap;
	private Mat image;
	private CascadeClassifier detector;
	private MatOfRect objects;
	private List< RectF > faces = new ArrayList< RectF >();
	public int surfaceWidth;
	public int surfacHight;
	public SurfaceHolder holder;
	public String cameraId = "0";
	public int myPreviewWidth = 640;   //オリジナルは640 , 480	固定だった
	public int myPreviewHeight = 480;

	public FaceRecognition(Context context , int displayOrientationDegrees) {
		super(context);
		final String TAG = "CameraView[FR]";
		String dbMsg = "";
		try {
			this.context = context;
			dbMsg = "displayOrientationDegrees=" + displayOrientationDegrees;
//			setWillNotDraw(false);
//			getHolder().addCallback(this);

			String filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
			detector = new CascadeClassifier(filename);
			objects = new MatOfRect();
			degrees = displayOrientationDegrees;

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	/**
	 * TextureViewの準備ができたらCallされる
	 * surfaceCreatedから置換え
	 * https://moewe-net.com/android/2016/how-to-camera2
	 */
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface , int width , int height) {
		final String TAG = "onSurfaceTextureAvailable[FR]";
		String dbMsg = "";
		try {
			this.surface = surface;
			this.surfaceWidth = width;                //holder.getSurfaceFrame().width();
			this.surfacHight = height;                    //holder.getSurfaceFrame().height();
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]";
			if ( this.isAvailable() ) {            //	;    //mTextureView.isAvailable();
				dbMsg += ",isAvailable";
				camera = new Camera(this);            // https://qiita.com/ueder/items/16be80bd1fc9ac8b0c1a
//				  CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
//
//				  ImageReader mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 3);
//				  mImageReader.setOnImageAvailableListener(mTakePictureAvailableListener, null);
//
//				  cameraManager.openCamera(cameraId, mStateCallback, null);           //CameraManagerにオープン要求を出します。
//				  CameraDevice.StateCallback mStateCallback =new CameraDevice.StateCallback() {...};
//
//				  surface.setDefaultBufferSize(1280, 720);                //プレビュー用のSurfaceを生成します。
//				  Surface mPreviewSurface = new Surface(surface);
//
//				  camera.createCaptureSession(Arrays.asList(mPreviewSurface,mImageReader.getSurface())  mSessionCallback, null);                     //CaptureSessionを生成します。
//				  CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {...};
//
//				  mCameraDevice = camera;										//パラメータのCameraDeviceを保持しておきます。
//				  mCaptureSession = session;                                //パラメータのCameraCaptureSessionを保持しておきます。

			}
//			camera.setDisplayOrientation(degrees);
//			camera.setPreviewCallback(this);
//			try {
////				camera.setPreviewDisplay(holder);
//			} catch (IOException er) {
//				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//			}
			setMycameraParameters();

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	/**
	 * TextureViewのサイズ変更時にCallされる
	 * surfaceChangedから
	 */
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface , int width , int height) {
		final String TAG = "onSurfaceTextureSizeChanged[FR]";
		String dbMsg = "";
		try {
			dbMsg = "holder=" + holder + ", width=" + width + ", height=" + height;
			this.surface = surface;
			if ( image != null ) {
				image.release();
				image = null;
			}
			if ( bitmap != null ) {
				if ( !bitmap.isRecycled() ) {
					bitmap.recycle();
				}
				bitmap = null;
			}
			if ( rgb != null ) {
				rgb = null;
			}
			faces.clear();
//			camera.startPreview();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * TextureViewの描画が更新されたタイミングでCallされる
	 */
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		final String TAG = "onSurfaceTextureUpdated[FR]";
		String dbMsg = "";
		try {

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void surfaceDestroy() {
		final String TAG = "surfaceDestroy[FR]";
		String dbMsg = "";
		try {
			if ( camera != null ) {
//				camera.stopPreview();
//				camera.release();
				camera = null;
				dbMsg = "camera破棄";
			}
			if ( image != null ) {
				image.release();
				image = null;
				dbMsg += "image破棄";
			}
			if ( bitmap != null ) {
				if ( !bitmap.isRecycled() ) {
					bitmap.recycle();
				}
				bitmap = null;
				dbMsg += "bitmap破棄";
			}
			if ( rgb != null ) {
				rgb = null;
				dbMsg += "rgb破棄";
			}
			faces.clear();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * TextureViewが破棄されるタイミングでCallされる
	 */
	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		final String TAG = "onSurfaceTextureDestroyed[FR]";
		String dbMsg = "";
		try {
			surfaceDestroy();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return false;
	}

	/**
	 * SurfaceHolder.Callback
	 */
//	@Override
	public void onPreviewFrame(byte[] data , Camera camera) {
		final String TAG = "onPreviewFrame[FR]";
		String dbMsg = "";
		try {
			int width = myPreviewWidth;		//camera.getParameters().getPreviewSize().width;
			int height = myPreviewHeight;		//camera.getParameters().getPreviewSize().height;
			dbMsg += "width=" + width + ", height=" + height;

			Bitmap bitmap = decode(data , width , height , degrees);
			if ( degrees == 90 ) {
				int tmp = width;
				width = height;
				height = tmp;
			}

			if ( image == null ) {
				image = new Mat(height , width , CvType.CV_8U , new Scalar(4));
			}
			Utils.bitmapToMat(bitmap , image);
			detector.detectMultiScale(image , objects);

			faces.clear();
			for ( org.opencv.core.Rect rect : objects.toArray() ) {
				float left = ( float ) (1.0 * rect.x / width);
				float top = ( float ) (1.0 * rect.y / height);
				float right = left + ( float ) (1.0 * rect.width / width);
				float bottom = top + ( float ) (1.0 * rect.height / height);
				faces.add(new RectF(left , top , right , bottom));
			}
			invalidate();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

//	/**
//	 * View
//	 */
////	@Override
//	protected void onDraw(Canvas canvas) {
//		super.onDraw(canvas);
//		final String TAG = "onDraw[FR]";
//		String dbMsg = "";
//		try {
//			Paint paint = new Paint();
//			paint.setColor(Color.GREEN);
//			paint.setStyle(Paint.Style.STROKE);
//			paint.setStrokeWidth(4);
//
//			int width = getWidth();
//			int height = getHeight();
//
//			for ( RectF face : faces ) {
//				RectF r = new RectF(width * face.left , height * face.top , width * face.right , height * face.bottom);
//				canvas.drawRect(r , paint);
//			}
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}

	/**
	 * Camera.PreviewCallback.onPreviewFrame で渡されたデータを Bitmap に変換します。
	 * @param data
	 * @param width
	 * @param height
	 * @param degrees
	 * @return
	 */
	private Bitmap decode(byte[] data , int width , int height , int degrees) {
		final String TAG = "decode[FR]";
		String dbMsg = "";
		try {
			dbMsg += "[" + width + "×" + height + "]" + degrees + "dig";
			if ( rgb == null ) {
				rgb = new int[width * height];
			}

			final int frameSize = width * height;
			for ( int j = 0, yp = 0 ; j < height ; j++ ) {
				int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
				for ( int i = 0 ; i < width ; i++ , yp++ ) {
					int y = (0xff & (( int ) data[yp])) - 16;
					if ( y < 0 )
						y = 0;
					if ( (i & 1) == 0 ) {
						v = (0xff & data[uvp++]) - 128;
						u = (0xff & data[uvp++]) - 128;
					}

					int y1192 = 1192 * y;
					int r = (y1192 + 1634 * v);
					int g = (y1192 - 833 * v - 400 * u);
					int b = (y1192 + 2066 * u);

					if ( r < 0 )
						r = 0;
					else if ( r > 262143 )
						r = 262143;
					if ( g < 0 )
						g = 0;
					else if ( g > 262143 )
						g = 262143;
					if ( b < 0 )
						b = 0;
					else if ( b > 262143 )
						b = 262143;

					rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
				}
			}

			if ( degrees == 90 ) {
				int[] rotatedData = new int[rgb.length];
				for ( int y = 0 ; y < height ; y++ ) {
					for ( int x = 0 ; x < width ; x++ ) {
						rotatedData[x * height + height - y - 1] = rgb[x + y * width];
					}
				}
				int tmp = width;
				width = height;
				height = tmp;
				rgb = rotatedData;
			}

			if ( bitmap == null ) {
				bitmap = Bitmap.createBitmap(width , height , Bitmap.Config.ARGB_8888);
			}

			bitmap.setPixels(rgb , 0 , width , 0 , 0 , width , height);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return bitmap;
	}

	/**
	 * プレビューサイズなど適切なパラメータを設定する
	 **/
	public void setMycameraParameters() {
		final String TAG = "setMycameraParameters[FR]";
		String dbMsg = "";
		try {
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]degrees=" + degrees;
			int nowWidth = surfaceWidth;
			int nowHeight = surfacHight;
//			if ( degrees == 90 || degrees == 270 ) {
//				dbMsg += "；縦に入れ替え";
//				nowWidth = surfacHight;
//				nowHeight = surfaceWidth;
//			}
			int maxPictureWidth = 0;
			int maxPictureHeight = 0;
			int myPreviewWidth = 640;   //オリジナルは640 , 480	固定だった
			int myPreviewHeight = 480;
			CameraManager cameraManager = ( CameraManager ) context.getSystemService(CAMERA_SERVICE);
			try {
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
				StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if ( map != null ) {
					List< Size > previewSizes = new ArrayList<>();
					previewSizes = Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888));
					for ( Size size : previewSizes ) {
						//		dbMsg += size.width + "x" + size.height + ",";
						if ( maxPictureWidth < size.getWidth() ) {
							maxPictureWidth = size.getWidth();
							maxPictureHeight = size.getHeight();
						}
					}

					dbMsg += ">Camera2>" + maxPictureWidth + "x" + maxPictureHeight + ",";
					double cameraAspect = ( double ) maxPictureWidth / maxPictureHeight;
					dbMsg += "=" + cameraAspect;
					dbMsg += "、preview size: ";

					List< Size > pictureSizes = new ArrayList<>();
					pictureSizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
					for ( Size size : pictureSizes ) {            //params.getSupportedPreviewSizes()
						if ( myPreviewWidth < size.getWidth() ) {
							if ( size.getWidth() <= nowWidth && size.getHeight() <= nowHeight ) {
								dbMsg += "," + size.getWidth() + "x" + size.getHeight() + ",";
								double previewAspect = ( double ) size.getWidth() / size.getHeight();
								dbMsg += "=" + previewAspect;
								if ( cameraAspect == previewAspect ) {
									myPreviewWidth = size.getWidth();
									myPreviewHeight = size.getHeight();
								}
							}
						}
					}
					dbMsg += ">>[" + myPreviewWidth + "x" + myPreviewHeight + "]";
					double fitScale = 1.0;//( double ) surfacHight / myPreviewHeight;           //☆結果がfloatでint除算すると整数部分のみになり小数点が切捨てられる
////						double fitScaleH = ( double ) surfacHight / myPreviewHeight;
//////						if ( fitScale > fitScaleH ) {
//////							fitScale = fitScaleH;
//////						}
//						if ( degrees == 90 || degrees == 270 ) {
//							dbMsg += "；縦";
//							fitScale =  ( double ) surfacHight/  myPreviewWidth;
//						}
					dbMsg += fitScale + "倍";
					myPreviewWidth = ( int ) (myPreviewWidth * fitScale);
					myPreviewHeight = ( int ) (myPreviewHeight * fitScale);
					dbMsg += ">>[" + myPreviewWidth + "x" + myPreviewHeight + "]";
					ViewGroup.LayoutParams lp = ( ViewGroup.LayoutParams ) this.getLayoutParams();
					lp.width = myPreviewWidth; //横幅
					lp.height = myPreviewHeight; //縦幅
					this.setLayoutParams(lp);

					/**
					 * setMycameraParameters[FR]: [1776×1080]degrees=0>Camera2>4608x3456,=1.3333333333333333、
					 * preview size: ,1440x1080,=1.3333333333333333>>[1440x1080]1.0倍>>[1440x1080]
					 I/surfaceCreated[FR]:  holder=android.view.SurfaceView$3@eb38390[1776×1080]


					 * */
				}

			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * https://qiita.com/cattaka/items/330321cb8c258c535e07
	 * */
//	public void setMyTextureVeiw() {
//		final String TAG = "setMyTextureVeiw[FR]";
//		String dbMsg = "";
//		try {
//			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]degrees=" + degrees;
//			WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//			int rotation = windowManager.getDefaultDisplay().getRotation();
//			int viewWidth = surfaceWidth;	//textureView.getWidth();
//			int viewHeight = surfacHight;	//textureView.getHeight();
//			Matrix matrix = new Matrix();
//			matrix.postRotate(- rotation, viewWidth * 0.5f, viewHeight * 0.5f);
//			holder.setFixedSize(matrix);
//	//			textureView.setTransform(matrix);
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}


	/**
	 * カメラに方向を与える
	 */
	public void setDig2Cam(int _degrees) {
		final String TAG = "setDig2Cam[FR]";
		String dbMsg = "_degrees=" + _degrees;
		try {
			this.degrees = _degrees;
//			camera.stopPreview();
//			camera.setDisplayOrientation(degrees);
			setMycameraParameters();
//			camera.startPreview();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	///2015年12月06日	AndroidのCamera2 APIとOpenGL ESでカメラの映像を表示してみた		https://qiita.com/ueder/items/16be80bd1fc9ac8b0c1a/////////////////////////////////////////////////////
	class Camera {
		private CameraDevice mCamera;
		private TextureView mTextureView;
		private Size mCameraSize;
		private CaptureRequest.Builder mPreviewBuilder;
		private CameraCaptureSession mPreviewSession;

		private CameraDevice.StateCallback mCameraDeviceCallback = new CameraDevice.StateCallback() {
			@Override
			public void onOpened( CameraDevice camera) {
				mCamera = camera;
				createCaptureSession();
			}

			@Override
			public void onDisconnected( CameraDevice camera) {
				camera.close();
				mCamera = null;
			}

			@Override
			public void onError( CameraDevice camera , int error) {
				camera.close();
				mCamera = null;
			}
		};

		CameraCaptureSession.StateCallback mCameraCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
			@Override
			public void onConfigured( CameraCaptureSession session) {
				mPreviewSession = session;
				updatePreview();
			}

			@Override
			public void onConfigureFailed( CameraCaptureSession session) {
				Toast.makeText(context, "onConfigureFailed" , Toast.LENGTH_LONG).show();
			}
		};

		public Camera(TextureView textureView) {
			mTextureView = textureView;
		}

		@SuppressLint ( "MissingPermission" )
		public void open() {
			try {
				CameraManager manager = ( CameraManager ) context.getSystemService(Context.CAMERA_SERVICE);
				for ( String cameraId : manager.getCameraIdList() ) {
					CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
					if ( characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK ) {
						StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
						mCameraSize = map.getOutputSizes(SurfaceTexture.class)[0];
						manager.openCamera(cameraId , mCameraDeviceCallback , null);             //カメラの起動

						return;
					}
				}
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		}

		private void createCaptureSession() {
			if ( !mTextureView.isAvailable() ) {
				return;
			}

			SurfaceTexture texture = mTextureView.getSurfaceTexture();
			texture.setDefaultBufferSize(mCameraSize.getWidth() , mCameraSize.getHeight());
			Surface surface = new Surface(texture);
			try {
				mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}

			mPreviewBuilder.addTarget(surface);
			try {
				mCamera.createCaptureSession(Collections.singletonList(surface) , mCameraCaptureSessionCallback , null);
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		}

		private void updatePreview() {
			mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			HandlerThread thread = new HandlerThread("CameraPreview");
			thread.start();
			Handler backgroundHandler = new Handler(thread.getLooper());

			try {
				mPreviewSession.setRepeatingRequest(mPreviewBuilder.build() , null , backgroundHandler);
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
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
 * 2017-02-10		AndroidでOpenCV 3.2を使って顔検出をする			https://blogs.osdn.jp/2017/02/10/opencv.html
 * <p>
 * mm-camera: <STATS_AF ><ERROR> 4436: af_port_handle_pdaf_stats: Fail to init buf divert ack ctrl
 */