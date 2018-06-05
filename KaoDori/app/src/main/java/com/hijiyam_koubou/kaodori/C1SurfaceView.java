package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class C1SurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private static final String TAG = "CameraView";
	private Context context;
	public FaceRecognitionView faceRecognitionView = null;

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

	public C1SurfaceView(Context context , int displayOrientationDegrees) {
		super(context);
		final String TAG = "MySurfaceView[Surface]";
		String dbMsg = "";
		try {
			this.context = context;
			dbMsg = "displayOrientationDegrees=" + displayOrientationDegrees;
			this.degrees = getCameraPreveiwDeg(displayOrientationDegrees);  //			degrees = displayOrientationDegrees;
			setWillNotDraw(false);
			getHolder().addCallback(this);

			String filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
			detector = new CascadeClassifier(filename);
			objects = new MatOfRect();

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
		final String TAG = "surfaceCreated[Surface]";
		String dbMsg = "";
		try {
			dbMsg = " holder=" + holder;
			this.holder = holder;
			this.surfaceWidth = holder.getSurfaceFrame().width();
			this.surfacHight = holder.getSurfaceFrame().height();
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]degrees=" + degrees +"dig";
			if ( camera == null ) {
				camera = Camera.open(0);              //APIL9
			}
			dbMsg += "," + degrees + "dig";
			camera.setDisplayOrientation(degrees);
			camera.setPreviewCallback(this);
			try {
				camera.setPreviewDisplay(holder);					//APIL1
			} catch (IOException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
//			setMycameraParameters();

//			faceRecognitionView = new FaceRecognitionView(context , degrees);

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder , int format , int width , int height) {
		final String TAG = "surfaceChanged[Surface]";
		String dbMsg = "";
		try {
			dbMsg = "holder=" + holder + ", format=" + format + ", width=" + width + ", height=" + height;
			this.holder = holder;
			if ( faceRecognitionView != null ) {
				faceRecognitionView.canvasRecycle();
				dbMsg += "認証クラス破棄";
			}
			canvasRecycle();

			camera.startPreview();     //APIL1
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void surfaceDestroy() {
		final String TAG = "surfaceDestroy[Surface]";
		String dbMsg = "";
		try {
			if ( camera != null ) {
				camera.stopFaceDetection();
				camera.stopPreview();
				camera.release();
				camera = null;
				dbMsg = "camera破棄";
			}
			if ( faceRecognitionView != null ) {
				faceRecognitionView.canvasRecycle();
				dbMsg += "認証クラス破棄";
			}
			canvasRecycle();

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		final String TAG = "surfaceDestroyed[Surface]";
		String dbMsg = "holder=" + holder;
		try {
			surfaceDestroy();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * プレビュー更新時のフレーム情報
	 * Camera.PreviewCallback.
	 */
	@Override
	public void onPreviewFrame(byte[] data , Camera camera) {
		final String TAG = "onPreviewFrame[Surface]";
		String dbMsg = "";
		try {
			dbMsg = "data=" + data.length;
			int width = 640;        //	this.getWidth();        //camera.getParameters().getPreviewSize().width;
			int height = 480;        //this.getHeight();        //camera.getParameters().getPreviewSize().height;
			if ( camera != null ) {
				width = camera.getParameters().getPreviewSize().width;
				height = camera.getParameters().getPreviewSize().height;
			}
			dbMsg += "{" + width + "×" + height + "]";
			readFrame(data , width , height);       //faceRecognitionView
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
		final String TAG = "decode[FR]";
		String dbMsg = "";
		try {
			dbMsg += "data=" + data.length;
			dbMsg += "[" + width + "×" + height + "]" + degrees + "dig";
			//.ArrayIndexOutOfBoundsException: length=786432; index=786432
			// y + height must be <= bitmap.height()
			//	width--;
			if ( rgb == null ) {
				rgb = new int[width * height];
			}

			final int frameSize = width * height;
			for ( int j = 0, yp = 0 ; j < height ; j++ ) {
//				dbMsg += "," + j + ")";
				int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
//				dbMsg += uvp;
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
				bitmap = Bitmap.createBitmap(width , height , Bitmap.Config.ARGB_8888);   //  APIL1
			}

			bitmap.setPixels(rgb , 0 , width , 0 , 0 , width , height);			//APIL1
			dbMsg += ",bitmap=" +  bitmap.getByteCount();

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return bitmap;
	}

	/**
	 * viewのデータを受け取り認証処理を開始する
	 * 元は onPreviewFrame
	 */
	public void readFrame(byte[] data ,	int previewWidth ,int previewHeight ) {        //, Camera camera      /
		final String TAG = "readFrame[FR]";
		String dbMsg = "";
		try {
			dbMsg += "data=" + data.length ;
			dbMsg += "[" + previewWidth + "×" + previewHeight +"]" + degrees +"dig";
			Bitmap bitmap = decode(data , previewWidth , previewHeight , degrees);
			if ( bitmap != null ) {
				dbMsg += ",bitmap=" + bitmap.getByteCount();
				if ( degrees == 90 ) {
					int tmp = previewWidth;
					previewWidth = previewHeight;
					previewHeight = tmp;
				}
				if ( image == null ) {
					image = new Mat(previewHeight , previewWidth , CvType.CV_8U , new Scalar(4));
				}
				Utils.bitmapToMat(bitmap , image);									//openCV
				dbMsg += ",image=" + image.size();
				detector.detectMultiScale(image , objects); ;						//openCV
				dbMsg += ",objects=" + objects.size();
				faces.clear();
				for ( org.opencv.core.Rect rect : objects.toArray() ) {
					float left = ( float ) (1.0 * rect.x / previewWidth);
					float top = ( float ) (1.0 * rect.y / previewHeight);
					float right = left + ( float ) (1.0 * rect.width / previewWidth);
					float bottom = top + ( float ) (1.0 * rect.height / previewHeight);
					faces.add(new RectF(left , top , right , bottom));
				}
				dbMsg += ",faces=" + faces.size();
				 //ata=3110400[1920×1080]0dig,bitmap=8294400,image=1920x1080,objects=1x0,faces=0
				//I/onPreviewFrame[Surface]: data=3110400{1920×1080]
				//data=3110400[1920×1080]0dig,bitmap=8294400,image=1920x1080,objects=1x11,faces=11

				invalidate();												//onDrawへ
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 　認証枠の書き込み
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		final String TAG = "onDraw[FR]";
		String dbMsg = "";
		try {
			Paint paint = new Paint();
			paint.setColor(Color.GREEN);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(4);

			int width = getWidth();
			int height = getHeight();
			dbMsg += "[" + width + "×" + height +"]faces="+faces.size();

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
	 * このクラスで作成したリソースの破棄
	 */
	public void canvasRecycle() {
		final String TAG = "setDig2Cam[FR]";
		String dbMsg = "";
		try {
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
	 * プレビューサイズなど適切なパラメータを設定する
	 **/
	public void setMycameraParameters() {
		final String TAG = "setMycameraParameters[Surface]";
		String dbMsg = "";
		try {
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]degrees=" + degrees;
			int nowWidth = this.getWidth()	;	//surfaceWidth;
			int nowHeight = this.getHeight();	//surfacHight;
			dbMsg += ",now[" + nowWidth + "×" + nowHeight + "]" ;
			if ( degrees == 90 || degrees == 270 ) {
				dbMsg += "；縦に入れ替え";
				int temp =  nowWidth;
				nowWidth = nowHeight;
				nowHeight = temp;
				dbMsg += ">>[" + nowWidth + "×" + nowHeight + "]" ;
			}
			int maxPictureWidth = 0;
			int maxPictureHeight = 0;
			int maxPreviewWidth = 640;   //オリジナルは640 , 480	固定だった
			int maxPreviewHeight = 480;
//			if ( Build.VERSION.SDK_INT >= 21 ) {
//				CameraManager cameraManager = ( CameraManager ) context.getSystemService(CAMERA_SERVICE);
//				try {
//					CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
//					StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//					if ( map != null ) {
//						List< Size > previewSizes = new ArrayList<>();
//						previewSizes = Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888));
//						for ( Size size : previewSizes ) {
//							//		dbMsg += size.width + "x" + size.height + ",";
//							if ( maxPictureWidth < size.getWidth() ) {
//								maxPictureWidth = size.getWidth();
//								maxPictureHeight = size.getHeight();
//							}
//						}
//
//						dbMsg += ">Camera2>" + maxPictureWidth + "x" + maxPictureHeight + ",";
//						double cameraAspect = ( double ) maxPictureWidth / maxPictureHeight;
//						dbMsg += "=" + cameraAspect;
//						dbMsg += "、preview size: ";
//
//						List< Size > pictureSizes = new ArrayList<>();
//						pictureSizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
//						for ( Size size : pictureSizes ) {            //params.getSupportedPreviewSizes()
//							if ( maxPreviewWidth < size.getWidth() ) {
//								if ( size.getWidth() <= nowWidth && size.getHeight() <= nowHeight ) {
//									dbMsg += "," + size.getWidth() + "x" + size.getHeight() + ",";
//									double previewAspect = ( double ) size.getWidth() / size.getHeight();
//									dbMsg += "=" + previewAspect;
//									if ( cameraAspect == previewAspect ) {
//										maxPreviewWidth = size.getWidth();
//										maxPreviewHeight = size.getHeight();
//									}
//								}
//							}
//						}
//						dbMsg += ">>[" + maxPreviewWidth + "x" + maxPreviewHeight + "]";
//						double fitScale = ( double ) surfacHight / maxPreviewHeight;           //☆結果がfloatでint除算すると整数部分のみになり小数点が切捨てられる
//////						double fitScaleH = ( double ) surfacHight / maxPreviewHeight;
////////						if ( fitScale > fitScaleH ) {
////////							fitScale = fitScaleH;
////////						}
//						if ( degrees == 90 || degrees == 270 ) {
//							dbMsg += "；縦";
//							fitScale = ( double ) surfacHight / maxPreviewWidth;
//						}
//						dbMsg += fitScale + "倍";
//						maxPreviewWidth = ( int ) (maxPreviewWidth * fitScale);
//						maxPreviewHeight = ( int ) (maxPreviewHeight * fitScale);
//						dbMsg += ">>[" + maxPreviewWidth + "x" + maxPreviewHeight + "]";
//						ViewGroup.LayoutParams lp = ( ViewGroup.LayoutParams ) this.getLayoutParams();
//						lp.width = maxPreviewWidth; //横幅
//						lp.height = maxPreviewHeight; //縦幅
//						this.setLayoutParams(lp);
//
//						/**
//						 * setMycameraParameters[Surface]: [1776×1080]degrees=0>Camera2>4608x3456,=1.3333333333333333、
//						 * preview size: ,1440x1080,=1.3333333333333333>>[1440x1080]1.0倍>>[1440x1080]
//						 I/surfaceCreated[Surface]:  holder=android.view.SurfaceView$3@eb38390[1776×1080]
//
//
//						 * */
//					}
//
//				} catch (CameraAccessException er) {
//					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
//				}
//			} else {
				Camera.Parameters params = camera.getParameters();
				dbMsg += "、picture size: ";
				List< Camera.Size > pictureSizes = params.getSupportedPictureSizes();
				dbMsg += ",camera;" +pictureSizes.size()+"種中";
				for ( Camera.Size size : pictureSizes ) {
					//		dbMsg += size.width + "x" + size.height + ",";
					if ( maxPictureWidth < size.width ) {
						maxPictureWidth = size.width;
						maxPictureHeight = size.height;
					}
				}
				dbMsg += ",camera[" + maxPictureWidth + "x" + maxPictureHeight + "]";
				double cameraAspect = ( double ) maxPictureWidth / maxPictureHeight;
				dbMsg += "=" + cameraAspect;
				dbMsg += "、preview size: " + params.getSupportedPreviewSizes().size() + "種中";
				for ( Camera.Size size : params.getSupportedPreviewSizes() ) {            //params.getSupportedPreviewSizes()
					if ( maxPreviewWidth < size.width ) {
						if ( size.width <= nowWidth && size.height <= nowHeight ) {
//							dbMsg += "," + size.width + "x" + size.height + ",";
							double previewAspect = ( double ) size.width / size.height;
//							dbMsg += ";" + previewAspect;
							if ( cameraAspect == previewAspect ) {
								maxPreviewWidth = size.width;
								maxPreviewHeight = size.height;
//								if ( degrees == 90 || degrees == 270 ) {
//									maxPreviewHeight= ( int ) (maxPreviewHeight/cameraAspect);
//								} else{
//									maxPreviewWidth= ( int ) (maxPreviewWidth/cameraAspect);
//								}
							}
//						holder.setFixedSize(maxPreviewWidth , maxPreviewHeight);      //viewのサイズを変えても扁平する
						}
					}
				}
				dbMsg += ">>" + maxPreviewWidth + "x" + maxPreviewHeight + ",";
				params.setPreviewSize(maxPreviewWidth , maxPreviewHeight);
				camera.setParameters(params);
//			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * https://qiita.com/cattaka/items/330321cb8c258c535e07
	 * */
//	public void setMyTextureVeiw() {
//		final String TAG = "setMyTextureVeiw[Surface]";
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
		final String TAG = "setDig2Cam[Surface]";
		String dbMsg = "_degrees=" + _degrees;
		try {
			this.degrees = getCameraPreveiwDeg(_degrees);
			camera.stopPreview(); 									//APIL1
			camera.setDisplayOrientation(degrees);    				//APIL8
//			setMycameraParameters();
			camera.startPreview();									//APIL1
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * 端末のどこが上端になっているかを検出し、カメラにプレビュー角度を与える
	 */
	public int getCameraPreveiwDeg(int dispDegrees) {
		final String TAG = "getCameraPreveiwDeg[MA]";
		String dbMsg = "";
		int orientationDeg = 90;
		try {
			dbMsg += ",画面；rotation=" + dispDegrees + "dig";
			Integer lensFacing;
			int lensFacingFront;
			Integer comOrientation;
//			if ( Build.VERSION.SDK_INT >= 21 ) {
//				CameraManager cameraManager = ( CameraManager ) getSystemService(CAMERA_SERVICE);
//				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
//				comOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);       // 0, 90, 180, 270などの角度になっている
//				dbMsg += ",カメラ2；=" + comOrientation + "dig";
//				lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
//				lensFacingFront = CameraCharacteristics.LENS_FACING_FRONT;
//			} else {
			android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
			android.hardware.Camera.getCameraInfo(0 , info);
			comOrientation = info.orientation;                // 0, 90, 180, 270などの角度になっている
			dbMsg += ",カメラ1；=" + comOrientation + "dig";
			lensFacing = info.facing;
			lensFacingFront = Camera.CameraInfo.CAMERA_FACING_FRONT;
//			}
			dbMsg += ",内外=" + lensFacing;
			dbMsg += ",CAMERA_FACING_FRONT=" + lensFacingFront;
			if ( lensFacing == lensFacingFront ) {
				orientationDeg = (comOrientation + dispDegrees) % 360;
				orientationDeg = (360 - orientationDeg) % 360;  // compensate the mirror
			} else {  // back-facing
				orientationDeg = (comOrientation - dispDegrees + 360) % 360;
			}
			dbMsg += ".orientationDeg=" + orientationDeg;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return orientationDeg;
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