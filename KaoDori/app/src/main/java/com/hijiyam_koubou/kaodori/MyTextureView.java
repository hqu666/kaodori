package com.hijiyam_koubou.kaodori;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyTextureView extends TextureView implements TextureView.SurfaceTextureListener {      //TextureView

	//SurfaceHolder.Callback,
//	private static final String TAG = "CameraView";
	private Context context;
	private WindowManager windowManager;

	public SurfaceTexture surface;
	public int surfaceWidth;            //プレビューエリアのサイズ
	public int surfacHight;


	private String cameraId;
	private CS_Camera2 frCamera;        //	private Camera frCamera;
	public CameraManager frCameraManager;
	public CameraCharacteristics frCharacteristics;
	public CaptureRequest.Builder frPreviewBuilder;
	public FaceRecognitionView faceRecognitionView =null;

	private int degrees;

	public SurfaceHolder holder;
	//	public String cameraId = "0";
	public int myPreviewWidth = 640;   //オリジナルは640 , 480	固定だった
	public int myPreviewHeight = 480;

	public MyTextureView(Context context  , int displayOrientationDegrees) {
		super(context);
		final String TAG = "MyTextureView[textuer]";
		String dbMsg = "";
		try {
			this.context = context;
			dbMsg += "degrees=" + degrees;
			this.degrees = displayOrientationDegrees;

			//mTextureView.isAvailable(); だとして
//			frCamera = new CS_Camera2(context , this);            // プレビュー（このクラス自身）を渡しておく
			setSurfaceTextureListener(this);                    //   Listenerを設定して、availableになるのを待ちます
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

// http://yonayona.biz/yonayona/blog/archives/camera_35.html　の残り
//	private void startPreview() {
//		final String TAG = "startPreview[textuer]";
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
//		final String TAG = "stopPreview[textuer]";
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
//		final String TAG = "onPause[textuer]";
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


	////LifeCicle///////////////////////////////////////////////////////////////////////////////////////////
	//Life Cycle:変更終息は　onSurfaceTextureSizeChanged　のみ/////////////////////////////////////

	/**
	 * TextureViewの準備ができたらCallされる（起動時のみ呼ばれる）
	 * surfaceCreatedから置換え
	 * https://moewe-net.com/android/2016/how-to-camera2
	 * stopPreview(); とstartPreview();
	 */
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface , int width , int height) {
		final String TAG = "onSurfaceTextureAvailable[textuer]";
		String dbMsg = "";
		try {
			this.surface = surface;
			this.surfaceWidth = width;                //holder.getSurfaceFrame().width();
			this.surfacHight = height;                    //holder.getSurfaceFrame().height();
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]";            //右が上端[1776×1080]        /
//			setMycameraParameters();
			if ( frCamera == null ) {
				frCamera = new CS_Camera2(context , this);            // プレビュー（このクラス自身）を渡しておく
				frCamera.open();
			}

// camera.setDisplayOrientation(degrees);		回転処理して
//camera.setPreviewCallback(this);     		コールバックセット
//			try {
//		camera.setPreviewDisplay(holder);       		プレビュースタート
//			} catch (IOException er) {
//				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//			}
			faceRecognitionView = new FaceRecognitionView(context  , degrees );

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}            //TextureViewの準備ができたらCallされる（起動時のみ呼ばれる）


	/**
	 * TextureViewのサイズ変更終息時にCallされる
	 * カメラの出力に合うよう、matrix の計算を行うためにonSurfaceTextureSizeChangedメソッドをoverride
	 */
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface , int width , int height) {
		final String TAG = "onSurfaceTextureSizeChanged[textuer]";
		String dbMsg = "";
		try {
			dbMsg = "[" + width + "×" + height + "]";
			if ( width < height ) {
				dbMsg = "縦";
			}
			this.surface = surface;
			if ( faceRecognitionView != null ) {
				faceRecognitionView.canvasRecycle();
				faceRecognitionView=null;
				faceRecognitionView = new FaceRecognitionView(context , degrees  );
			}
			if ( frCamera != null ) {
				dbMsg += "、プレビュー更新";
				frCamera.updatePreview();   //			frCamera.startPreview();
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}    // TextureViewのサイズ変更終息時にCallされる

	/***
	 * このビューの終了処理
	 stopPreview();
	 * */
	public void surfaceDestroy() {
		final String TAG = "surfaceDestroy[textuer]";
		String dbMsg = "";
		try {
			if ( frCamera != null ) {
//				frCamera.stopPreview();
				frCamera.camera2Dispose();//				frCamera.release();
				frCamera = null;
				dbMsg += "camera破棄";
			}
			if ( faceRecognitionView != null ) {
				faceRecognitionView.canvasRecycle();
				dbMsg += "認証クラス破棄";
			}
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
		final String TAG = "onSurfaceTextureDestroyed[textuer]";
		String dbMsg = "";
		try {
			surfaceDestroy();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return false;
	}        //TextureViewが破棄されるタイミングでCallされる

	/**
	 * TextureViewの描画が更新されたタイミングでCallされる
	 * 収録するまで繰り返しイベントは連続発生
	 */
	@Override
	public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
		final String TAG = "onSurfaceTextureUpdated[textuer]";
		String dbMsg = "";
		try {
			dbMsg = "onSurfaceTextureUpdated: ";
			if ( frCamera != null ) {
				if ( frCamera.jpegImageReader != null ) {
					ImageReader imageReader = frCamera.jpegImageReader;
					dbMsg += "[" + imageReader.getWidth() + "×" + imageReader.getHeight() + "]";
//						ByteBuffer buffer = imageReader.acquireLatestImage().getPlanes()[0].getBuffer();                    // 画像バイナリの取得
////						dbMsg += "、"+buffer. +"バイト";
//						byte[] bytes = new byte[buffer.capacity()];
//						buffer.get(bytes);
//						dbMsg += "、"+bytes.length +"バイト";
//

//						OutputStream output = null;
//						try {
//							output = new FileOutputStream(new File("filename"));   						// 画像の書き込み
//							output.write(bytes);
//						} catch (Exception er) {
//							myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//						}
				}
				if(faceRecognitionView != null){
					Bitmap bitmap = this.getBitmap();
					dbMsg += "、bitmap=" + bitmap.getByteCount() + "バイト";
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.PNG , 100 , byteArrayOutputStream);
					//	Bitmap.CompressFormat.PNG	;	PNG, クオリティー100としてbyte配列にデータを格納
					byte[] data = byteArrayOutputStream.toByteArray();
					dbMsg += "、data=" + data.length + "バイト";
					int width = 640;        //	this.getWidth();        //camera.getParameters().getPreviewSize().width;
					int height = 480;        //this.getHeight();        //camera.getParameters().getPreviewSize().height;
					dbMsg += "{" + width + "×" + height + "]";
					faceRecognitionView.readFrame(data,width,height);
					byteArrayOutputStream.close();
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}            // TextureViewの描画が更新されたタイミングでCallされる


	////2016-05-19		Android6.0でもカメラを使いたい 1	http://mslgt.hatenablog.com/entry/2016/05/19/192841	////////////
	private int ratioWidth = 0;
	private int ratioHeight = 0;
//	public AutoFitTextureView(Context context) {
//		this(context, null);
//		final String TAG = "AutoFitTextureView[textuer]";
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
//		final String TAG = "AutoFitTextureView[textuer]";
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
//		final String TAG = "AutoFitTextureView[textuer]";
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
		final String TAG = "setAspectRatio[textuer]";
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
		final String TAG = "onMeasure[textuer]";
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
			//			followingRotation();
//  			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 参照	https://qiita.com/cattaka/items/330321cb8c258c535e07
	 */
//	public void setTextureVeiwRotation() {
//		final String TAG = "setTextureVeiwRotation[textuer]";
//		String dbMsg = "";
//		try {
//			if ( context != null ) {
//				if ( windowManager != null ) {
//					windowManager = (( Activity ) getContext()).getWindowManager();
//				}
//				int rotation = windowManager.getDefaultDisplay().getRotation();
//				dbMsg += ",rotation=" + rotation;
//				int viewWidth = this.getWidth();
//				int viewHeight = this.getHeight();
//				dbMsg += "[" + viewWidth + "×" + viewHeight + "]";
//				Matrix matrix = new Matrix();
//				matrix.postRotate(-rotation , viewWidth * 0.5f , viewHeight * 0.5f);
//				this.setTransform(matrix);
//			}
//			myLog(TAG , dbMsg);
//		} catch (Exception er) {
//			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//		}
//	}
	/**
	 * プレビューサイズなど適切なパラメータを設定する
	 **/
	public void setMycameraParameters() {
		final String TAG = "setMycameraParameters[textuer]";
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
					 * setMycameraParameters[textuer]: [1776×1080]degrees=0>Camera2>4608x3456,=1.3333333333333333、
					 * preview size: ,1440x1080,=1.3333333333333333>>[1440x1080]1.0倍>>[1440x1080]
					 I/surfaceCreated[textuer]:  holder=android.view.SurfaceView$3@eb38390[1776×1080]


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
		final String TAG = "setDig2Cam[textuer]";
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
 * quit後のイベント	認証のトラッキングは？
 * <p>
 * ②onSurfaceTextureUpdatedの度に
 * E/mm-camera: <STATS_AF ><ERROR> 4436: af_port_handle_pdaf_stats: Fail to init buf divert ack ctrl
 */