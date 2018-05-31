package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private static final String TAG = "CameraView";
	private Context context;
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

	public CameraView(Context context , int displayOrientationDegrees) {
		super(context);
		final String TAG = "CameraView[CV]";
		String dbMsg = "";
		try {
			this.context = context;
			dbMsg = "displayOrientationDegrees=" + displayOrientationDegrees;
			setWillNotDraw(false);
			getHolder().addCallback(this);

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
	 * SurfaceHolder.Callback
	 * 一度だけ発生
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		final String TAG = "surfaceCreated[CV]";
		String dbMsg = "";
		try {
			dbMsg = " holder=" + holder;
			this.holder = holder;
			this.surfaceWidth = holder.getSurfaceFrame().width();
			this.surfacHight = holder.getSurfaceFrame().height();
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]";
			if ( camera == null ) {
				camera = Camera.open(0);
			}
			camera.setDisplayOrientation(degrees);
			camera.setPreviewCallback(this);
			try {
				camera.setPreviewDisplay(holder);
			} catch (IOException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			setMycameraParameters();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder , int format , int width , int height) {
		final String TAG = "surfaceChanged[CV]";
		String dbMsg = "";
		try {
			dbMsg = "holder=" + holder + ", format=" + format + ", width=" + width + ", height=" + height;
			this.holder = holder;
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
			camera.startPreview();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void surfaceDestroy() {
		final String TAG = "surfaceDestroy[CV]";
		String dbMsg = "";
		try {
			if ( camera != null ) {
				camera.stopPreview();
				camera.release();
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

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		final String TAG = "surfaceDestroyed[CV]";
		String dbMsg = "holder=" + holder;
		try {
			surfaceDestroy();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * SurfaceHolder.Callback
	 */
	@Override
	public void onPreviewFrame(byte[] data , Camera camera) {
		final String TAG = "onPreviewFrame[CV]";
		String dbMsg = "";
		try {
			int width = camera.getParameters().getPreviewSize().width;
			int height = camera.getParameters().getPreviewSize().height;
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

	/**
	 * View
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		//もともとSurfaceView は setWillNotDraw(true) なので super.onDraw(canvas) を呼ばなくてもよい。
		//super.onDraw(canvas);
		final String TAG = "onDraw[CV]";
		String dbMsg = "";
		try {
			Paint paint = new Paint();
			paint.setColor(Color.GREEN);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(4);

			int width = getWidth();
			int height = getHeight();

			for ( RectF face : faces ) {
				RectF r = new RectF(width * face.left , height * face.top , width * face.right , height * face.bottom);
				canvas.drawRect(r , paint);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * Camera.PreviewCallback.onPreviewFrame で渡されたデータを Bitmap に変換します。
	 * @param data
	 * @param width
	 * @param height
	 * @param degrees
	 * @return
	 */
	private Bitmap decode(byte[] data , int width , int height , int degrees) {
		final String TAG = "decode[CV]";
		String dbMsg = "";
		try {
			dbMsg += "["+width+"×"+height +"]"+degrees + "dig";
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
		final String TAG = "setMycameraParameters[CV]";
		String dbMsg = "";
		try {
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]degrees=" + degrees;
			int nowWidth = surfaceWidth;
			int nowHeight = surfacHight;
			if ( degrees == 0 || degrees == 180 ) {
				dbMsg += "；縦横入れ替え";
				nowWidth = surfacHight;
				nowHeight = surfaceWidth;
			}
			Camera.Parameters params = camera.getParameters();
			dbMsg += "、picture size: ";
			int maxPictureWidth = 0;
			int maxPictureHeight = 0;
			//	List pictureSizes = params.getSupportedPictureSizes();
			for ( Camera.Size size : params.getSupportedPictureSizes() ) {
				//		dbMsg += size.width + "x" + size.height + ",";
				if ( maxPictureWidth < size.width ) {
					maxPictureWidth = size.width;
					maxPictureHeight = size.height;
				}
			}
			dbMsg += ">>" + maxPictureWidth + "x" + maxPictureHeight + ",";
			double cameraAspect = ( double ) maxPictureWidth / maxPictureHeight;
			dbMsg += "=" + cameraAspect;
			dbMsg += "、preview size: ";
			int maxPreviewWidth = 640;   //オリジナルは640 , 480	固定だった
			int maxPreviewHeight = 480;
			for ( Camera.Size size : params.getSupportedPreviewSizes() ) {            //params.getSupportedPreviewSizes()
				if ( maxPreviewWidth < size.width ) {
					if ( size.width <= nowWidth && size.height <= nowHeight ) {
						dbMsg += "," + size.width + "x" + size.height + ",";
						double previewAspect = ( double ) size.width / size.height;
						dbMsg += "=" + previewAspect;
						if ( cameraAspect == previewAspect ) {
							maxPreviewWidth = size.width;
							maxPreviewHeight = size.height;
						}
//						holder.setFixedSize(maxPreviewWidth , maxPreviewHeight);      //viewのサイズを変えても扁平する
					}
				}
			}
			dbMsg += ">>" + maxPreviewWidth + "x" + maxPreviewHeight + ",";
			params.setPreviewSize(maxPreviewWidth , maxPreviewHeight);
			camera.setParameters(params);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	   /**
		* https://qiita.com/cattaka/items/330321cb8c258c535e07
		* */
//	public void setMyTextureVeiw() {
//		final String TAG = "setMyTextureVeiw[CV]";
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
		final String TAG = "setDig2Cam[CV]";
		String dbMsg = "_degrees=" + _degrees;
		try {
			this.degrees = _degrees;
			camera.stopPreview();
			camera.setDisplayOrientation(degrees);
			setMycameraParameters();
			camera.startPreview();
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
 * <p>
 * mm-camera: <STATS_AF ><ERROR> 4436: af_port_handle_pdaf_stats: Fail to init buf divert ack ctrl
 */