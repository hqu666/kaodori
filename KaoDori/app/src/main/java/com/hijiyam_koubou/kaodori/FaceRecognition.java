package com.hijiyam_koubou.kaodori;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
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
import android.util.AttributeSet;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
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

	//SurfaceHolder.Callback,
//	private static final String TAG = "CameraView";
	private Context context;
	private WindowManager windowManager;

	public SurfaceTexture surface;
	public int surfaceWidth;            //プレビューエリアのサイズ
	public int surfacHight;


	private String cameraId;
	private Camera frCamera;
	public CameraManager frCameraManager;
	public CameraCharacteristics frCharacteristics;
	public CaptureRequest.Builder frPreviewBuilder;


	private int degrees;
	private int[] rgb;
	private Bitmap bitmap;
	private Mat image;
	private CascadeClassifier detector;
	private MatOfRect objects;
	private List< RectF > faces = new ArrayList< RectF >();

	public SurfaceHolder holder;
	//	public String cameraId = "0";
	public int myPreviewWidth = 640;   //オリジナルは640 , 480	固定だった
	public int myPreviewHeight = 480;

	public FaceRecognition(Context context , int degrees) {
		super(context);
		final String TAG = "FaceRecognition[FR]";
		String dbMsg = "";
		try {
			dbMsg += "degrees=" + degrees;
			this.degrees = degrees;
			this.context = context;
			String filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
			detector = new CascadeClassifier(filename);
			objects = new MatOfRect();
			//mTextureView.isAvailable(); だとして
			frCamera = new Camera(this);            // プレビュー（このクラス自身）を渡しておく
			setSurfaceTextureListener(this);                    //   Listenerを設定して、availableになるのを待ちます。
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

// http://yonayona.biz/yonayona/blog/archives/camera_35.html　の残り
//	private void startPreview() {
//		final String TAG = "startPreview[FR]";
//		String dbMsg = "";
//		try {
//			mCamera = Camera.open(this.cameraId);
//			try {
//				mCamera.setPreviewTexture(this.surface);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			mCamera.setDisplayOrientation(getCameraDisplayOrientation());
//			Camera.Parameters parameters = mCamera.getParameters();
//			if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
//				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//				mCamera.setParameters(parameters);
//			}
//
//			mCamera.startPreview();
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}
//
//	private void stopPreview(){
//		final String TAG = "stopPreview[FR]";
//		String dbMsg = "";
//		try {
//			if (mCamera != null) {
//				mCamera.stopPreview();
//			}
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}
//
//	public void onPause(){
//		final String TAG = "onPause[FR]";
//		String dbMsg = "";
//		try {
//			if(mCamera != null) {
//				mCamera.release();
//				mCamera = null;
//			}
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}
//
//	public int getCameraDisplayOrientation() {
//		final String TAG = "getCameraDisplayOrientation[FR]";
//		String dbMsg = "";
//		int result=0;
//		try {
//			Camera.CameraInfo info = new Camera.CameraInfo();
//			Camera.getCameraInfo(cameraId, info);
//			int rotation = ((Activity)getContext()).getWindowManager().getDefaultDisplay().getRotation();
//			int degrees = 0;
//			switch (rotation) {
//				case Surface.ROTATION_0: degrees = 0; break;
//				case Surface.ROTATION_90: degrees = 90; break;
//				case Surface.ROTATION_180: degrees = 180; break;
//				case Surface.ROTATION_270: degrees = 270; break;
//			}
//
//			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//				result = (info.orientation + degrees) % 360;
//				result = (360 - result) % 360;
//			} else {
//				result = (info.orientation - degrees + 360) % 360;
//			}
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//		return result;
//	}

	////LifeCicle///////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * TextureViewの準備ができたらCallされる（起動時のみ呼ばれる）
	 * surfaceCreatedから置換え
	 * https://moewe-net.com/android/2016/how-to-camera2
	 * stopPreview(); とstartPreview();
	 */
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface , int width , int height) {
		final String TAG = "onSurfaceTextureAvailable[FR]";
		String dbMsg = "";
		try {
			this.surface = surface;
			this.surfaceWidth = width;                //holder.getSurfaceFrame().width();
			this.surfacHight = height;                    //holder.getSurfaceFrame().height();
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]";            //右が上端[1776×1080]        /
//			setMycameraParameters();
			frCamera.open();

// camera.setDisplayOrientation(degrees);		回転処理して
//camera.setPreviewCallback(this);     		コールバックセット
//			try {
//		camera.setPreviewDisplay(holder);       		プレビュースタート
//			} catch (IOException er) {
//				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//			}

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	/**
	 * TextureViewのサイズ変更時にCallされる
	 * カメラの出力に合うよう、matrix の計算を行うためにonSurfaceTextureSizeChangedメソッドをoverride
	 */
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface , int width , int height) {
		final String TAG = "onSurfaceTextureSizeChanged[FR]";
		String dbMsg = "";
		try {
			dbMsg = "[" + width + "×" + height + "]";
			if ( width < height ) {
				dbMsg = "縦";
			}
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
//			frCamera.startPreview();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/***
	 * このビューの終了処理
	 stopPreview();
	 * */
	public void surfaceDestroy() {
		final String TAG = "surfaceDestroy[FR]";
		String dbMsg = "";
		try {
			if ( frCamera != null ) {
//				frCamera.stopPreview();
				frCamera.camera2Dispose();//				frCamera.release();
				frCamera = null;
				dbMsg += "camera破棄";
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
	 * TextureViewの描画が更新されたタイミングでCallされる
	 */
	@Override
	public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
		final String TAG = "onSurfaceTextureUpdated[FR]";
		String dbMsg = "";
		try {
			dbMsg = "onSurfaceTextureUpdated: ";
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	////2016-05-19		Android6.0でもカメラを使いたい 1	http://mslgt.hatenablog.com/entry/2016/05/19/192841	////////////
	private int ratioWidth = 0;
	private int ratioHeight = 0;
//	public AutoFitTextureView(Context context) {
//		this(context, null);
//		final String TAG = "AutoFitTextureView[FR]";
//		String dbMsg = "";
//		try {
//			inConstructor( context);
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}
//
//	public AutoFitTextureView(Context context, AttributeSet attrs) {
//		this(context, attrs, 0);
//		final String TAG = "AutoFitTextureView[FR]";
//		String dbMsg = "";
//		try {
//			inConstructor( context);
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}
//
//	public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
//		super(context, attrs, defStyle);
//		final String TAG = "AutoFitTextureView[FR]";
//		String dbMsg = "";
//		try {
//			dbMsg = "defStyle="+defStyle;
//			inConstructor( context);
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}


	public void setAspectRatio(int width , int height) {
		final String TAG = "setAspectRatio[FR]";
		String dbMsg = "";
		try {
			dbMsg = "[" + width + "×" + height + "]";
			if ( width < 0 || height < 0 ) {
				throw new IllegalArgumentException("Size cannot be negative.");
			}
			ratioWidth = width;
			ratioHeight = height;
			requestLayout();                                        //APIL1  ; サイズを指定してViewを更新する(onMeasure実行).
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * View自体の再レイアウトが必要なとき呼ばれる
	 * 自分自身の幅高さを確定させるもの、onLayoutは子Viewの位置を決めるもの
	 * 複数回呼ばれる
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec , int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec , heightMeasureSpec);
		final String TAG = "onMeasure[FR]";
		String dbMsg = "";
		try {
			dbMsg += "[" + widthMeasureSpec + "×" + heightMeasureSpec + "]";                // [1073743600×-2147482568]
			surfaceWidth = MeasureSpec.getSize(widthMeasureSpec);            // プレビューエリアのサイズ；Viewのサイズを確定させる.
			surfacHight = MeasureSpec.getSize(heightMeasureSpec);
			dbMsg += ">surfac>[" + surfaceWidth + "×" + surfacHight + "]";

			dbMsg += "、ratioWidth[" + ratioWidth + "×" + ratioHeight + "]";
			if ( ratioWidth == 0 || ratioHeight == 0 ) {
				setMeasuredDimension(surfaceWidth , surfacHight);    //引数や子Viewから、自分自身のサイズを setMeasuredDimension で確定させる
			} else {
				if ( surfaceWidth < surfacHight * ratioWidth / ratioHeight ) {
					setMeasuredDimension(surfaceWidth , surfaceWidth * ratioHeight / ratioWidth);
				} else {
					setMeasuredDimension(surfacHight * ratioWidth / ratioHeight , surfacHight);
				}
			}
			if ( surfaceWidth < surfacHight ) {
				dbMsg += "縦";
//				setAspectRatio(surfaceWidth , surfacHight);
//			} else {
//				setAspectRatio(surfacHight , surfaceWidth);
			}
//			setAspectRatio(surfaceWidth , surfacHight);

//			setTextureVeiwRotation();
			followingRotation();

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void followingRotation() {
		final String TAG = "followingRotation[FR]";
		String dbMsg = "";
		try {
//			if ( windowManager == null ) {
			WindowManager frWindowManager = (( Activity ) getContext()).getWindowManager();
//			}
			int rotation = frWindowManager.getDefaultDisplay().getRotation();
			dbMsg += ",rotation=" + rotation;
			if ( frCamera != null ) {
				frCamera.setCameraRotation(rotation);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	/**
	 * 参照	https://qiita.com/cattaka/items/330321cb8c258c535e07
	 */
	public void setTextureVeiwRotation() {
		final String TAG = "setTextureVeiwRotation[FR]";
		String dbMsg = "";
		try {
			if ( context != null ) {
				if ( windowManager != null ) {
					windowManager = (( Activity ) getContext()).getWindowManager();
				}
				int rotation = windowManager.getDefaultDisplay().getRotation();
				dbMsg += ",rotation=" + rotation;
				int viewWidth = this.getWidth();
				int viewHeight = this.getHeight();
				dbMsg += "[" + viewWidth + "×" + viewHeight + "]";
				Matrix matrix = new Matrix();
				matrix.postRotate(-rotation , viewWidth * 0.5f , viewHeight * 0.5f);
				this.setTransform(matrix);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

//////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * SurfaceHolder.Callback
	 */
//	@Override
	public void onPreviewFrame(byte[] data , Camera camera) {
		final String TAG = "onPreviewFrame[FR]";
		String dbMsg = "";
		try {
			int width = myPreviewWidth;        //camera.getParameters().getPreviewSize().width;
			int height = myPreviewHeight;        //camera.getParameters().getPreviewSize().height;
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
//	@Override
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

			if ( frCamera == null ) {
				frCameraManager = ( CameraManager ) context.getSystemService(Context.CAMERA_SERVICE);
			} else {
				frCameraManager = frCamera.mCameraManager;
			}
			try {
				CameraCharacteristics characteristics = frCameraManager.getCameraCharacteristics(cameraId);
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
	 * カメラに方向を与える
	 */
	public void setDig2Cam(int _degrees) {
		final String TAG = "setDig2Cam[FR]";
		String dbMsg = "_degrees=" + _degrees;
		try {
			this.degrees = _degrees;
//			camera.stopPreview();
//			camera.setDisplayOrientation(degrees);
//			setMycameraParameters();
//			camera.startPreview();

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	///2015年12月06日	AndroidのCamera2 APIとOpenGL ESでカメラの映像を表示してみた		https://qiita.com/ueder/items/16be80bd1fc9ac8b0c1a/////////////////////////////////////////////////////
	class Camera {
		private CameraDevice mCamera;
		public CameraManager mCameraManager;
		public CameraCharacteristics mCharacteristics;
		private TextureView mTextureView;
		private Size mCameraSize;
		public CaptureRequest.Builder mPreviewBuilder;
		private CameraCaptureSession mPreviewSession;
		private Handler previewHandler;
		private HandlerThread previewThread;

		private CameraCaptureSession.CaptureCallback mCaptureCallback;

		private CameraDevice.StateCallback mCameraDeviceCallback = new CameraDevice.StateCallback() {
			@Override
			public void onOpened(CameraDevice camera) {
				final String TAG = "onOpened[FR]";
				String dbMsg = "";
				try {
					mCamera = camera;
					createCaptureSession();            //プレビュー設定
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			@Override
			public void onDisconnected(CameraDevice camera) {
				final String TAG = "onDisconnected[FR]";
				String dbMsg = "";
				try {
					camera.close();
					mCamera = null;
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			@Override
			public void onError(CameraDevice camera , int error) {
				final String TAG = "onError[FR]";
				String dbMsg = "";
				try {
					camera.close();
					mCamera = null;
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}
		};

		/**
		 * カメラが起動して使える状態になったら呼ばれるコールバック
		 */
		CameraCaptureSession.StateCallback mCameraCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
			@Override
			public void onConfigured(CameraCaptureSession session) {
				final String TAG = "onConfigured[FR]";
				String dbMsg = "";
				try {
					mPreviewSession = session;
					mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);                             // オートフォーカスの設定
					updatePreview();
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			@Override
			public void onConfigureFailed(CameraCaptureSession session) {
				final String TAG = "onConfigureFailed[FR]";
				String dbMsg = "";
				try {
					dbMsg = "onConfigureFailed";
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}
		};

		/**
		 * コンストラクタでプレビューを受け取る
		 **/
		public Camera(TextureView textureView) {
			final String TAG = "Camera[FR]";
			String dbMsg = "";
			try {
				mTextureView = textureView;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		/**
		 * CameraManagerにオープン要求
		 */
		@SuppressLint ( "MissingPermission" )
		public void open() {
			final String TAG = "open[FR]";
			String dbMsg = "";
			try {
//				if ( mCameraManager != null ) {
				CameraManager mCameraManager = ( CameraManager ) context.getSystemService(Context.CAMERA_SERVICE);
//				}
				for ( String cameraId : mCameraManager.getCameraIdList() ) {
					dbMsg += "cameraId=" + cameraId;
//					if ( mCharacteristics != null ) {
						mCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
//					}
					if ( mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK ) {
						dbMsg += ";LENS_FACING_BACK";
						StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
						mCameraSize = map.getOutputSizes(SurfaceTexture.class)[0];
						dbMsg += ">mCameraSize>[" + mCameraSize.getWidth() + "x" + mCameraSize.getHeight() + "]";
						/**
						 ImageReader mImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 3);       //    ImageReaderを生成
						 mImageReader.setOnImageAvailableListener(mTakePictureAvailableListener, null);
						 ImageReader.OnImageAvailableListener mTakePictureAvailableListener =new ImageReader.OnImageAvailableListener() {
						 Image image = reader.acquireNextImage();								//または	Image image = reader.acquireLatestImage();	で	Image(android.media.Image)を取得
						 //取得したImageを処理します。(保存など)
						 image.close();					// Imageを解放します。これを忘れるとバースト撮影などで失敗します。
						 };
						 * */
						mCameraManager.openCamera(cameraId , mCameraDeviceCallback , null);             //CameraManagerにオープン要求を出します。
						break;                    //	return;
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
		 * CaptureSessionを生成  ;プレビュー
		 */
		private void createCaptureSession() {
			final String TAG = "createCaptureSession[FR]";
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
		}

		/**
		 * プレビューを開始
		 */
		private void updatePreview() {
			final String TAG = "updatePreview[FR]";
			String dbMsg = "";
			try {
				mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
				previewThread = new HandlerThread("CameraPreview");
				previewThread.start();
				previewHandler = new Handler(previewThread.getLooper());

				try {
					mPreviewSession.setRepeatingRequest(mPreviewBuilder.build() , null , previewHandler); //プレビューを開始
				} catch (CameraAccessException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		public void setCameraRotation(int rotation) {
			final String TAG = "setCameraRotation[FR]";
			String dbMsg = "";
			try {
				dbMsg += ",rotation=" + rotation;
				if ( mCharacteristics != null && mPreviewBuilder != null ) {
					Integer sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
					dbMsg += ",sensorOrientation=" + sensorOrientation;
					 SparseIntArray ORIENTATIONS = new SparseIntArray();
					{
						ORIENTATIONS.append(Surface.ROTATION_0 , 90);
						ORIENTATIONS.append(Surface.ROTATION_90 , 0);
						ORIENTATIONS.append(Surface.ROTATION_180 , 270);
						ORIENTATIONS.append(Surface.ROTATION_270 , 180);
					}
					int comDegrees = (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360;
					dbMsg += "=" + comDegrees + "dig";
//					CaptureRequest.Builder mPreviewBuilder =mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
					mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION, comDegrees);
					mPreviewSession.capture(mPreviewBuilder.build(), mCaptureCallback, null);
					sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
					dbMsg += ",sensorOrientation=" + sensorOrientation;
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		/***
		 *   camera2Dの静止画撮影
		 * */
		public void camera2Shot() {
			final String TAG = "camera2Shot[FR]";
			String dbMsg = "";
			try {
				mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);            //撮影用のCaptureRequest.Builderを生成
				ImageReader mImageReader = ImageReader.newInstance(1920 , 1080 , ImageFormat.JPEG , 3);        //   ImageReaderを生成
//				mImageReader.setOnImageAvailableListener(mTakePictureAvailableListener, null);
				mPreviewBuilder.addTarget(mImageReader.getSurface());                                                                //CaptureRequest.Builderに撮影用のSurfaceを設定
				mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);                // 必要なパラメータを設定します。(サンプル)
//				mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);
				mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE , CaptureRequest.CONTROL_AE_MODE_ON);
				mPreviewSession.stopRepeating();                                                                                    //現在のプレビューを停止します。
				mPreviewSession.capture(mPreviewBuilder.build() , mCaptureCallback , null);                                    // 撮影を開始します
				mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
					//					@Override
					public void onCaptureCompleted() {                                                                                //撮影完了のタイミングで通知
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
			final String TAG = "camera2Dispose[FR]";
			String dbMsg = "";
			try {
				mPreviewSession.close();            //CameraCaptureSessionをクローズ
				mCamera.close();                      //CameraDeviceをクローズ
//		mTextureView.close();                   // ImageReaderをクローズ
				//		mCameraDeviceCallback                 //破棄は不要？
				if ( previewHandler != null ) {
					previewThread.quitSafely();        //APIL18
					previewHandler = null;
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
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
 * 2018年1月12日	今更だけどandroid.hardware.Cameraを使う			  http://yonayona.biz/yonayona/blog/archives/camera_1.html
 * <p>
 * E/mm-camera: <STATS ><ERROR> 2991: stats_port_check_caps_reserve: Invalid Port capability type!
 * /mm-camera: <IMGLIB><ERROR> 175: module_depth_map_handle_ctrl_parm: E
 * <IMGLIB><ERROR> 335: set_depth_map_config_param: X
 * <IMGLIB><ERROR> 278: module_depth_map_handle_ctrl_parm: X
 * <p>
 * ②onSurfaceTextureUpdatedの度に
 * E/mm-camera: <STATS_AF ><ERROR> 4436: af_port_handle_pdaf_stats: Fail to init buf divert ack ctrl
 */