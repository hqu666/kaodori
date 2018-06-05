package com.hijiyam_koubou.kaodori;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;


public class C2SurfaceView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "CameraView";
	private Context context;
	public FaceRecognitionView faceRecognitionView = null;

	private int degrees;

	public MyC2 camera;
	public boolean isMainCamsera = true;
//	public String cameraId = "0";        //メインカメラID
//	public String cameraId_sub = "1";  //サブカメラID

	private int[] rgb;
	private Bitmap bitmap;
	private Mat image;
	private CascadeClassifier detector;
	private MatOfRect objects;
	private List< RectF > faces = new ArrayList< RectF >();
	public int surfaceWidth;
	public int surfacHight;
	public SurfaceHolder holder;

	public C2SurfaceView(Context context , int displayOrientationDegrees) {
		super(context);
		final String TAG = "MySurfaceView[C2S]";
		String dbMsg = "";
		try {
			this.context = context;
			dbMsg = "displayOrientationDegrees=" + displayOrientationDegrees;
//			setWillNotDraw(false);
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
		final String TAG = "surfaceCreated[C2S]";
		String dbMsg = "";
		try {
			dbMsg = " holder=" + holder;
			this.holder = holder;
			this.surfaceWidth = holder.getSurfaceFrame().width();
			this.surfacHight = holder.getSurfaceFrame().height();
			dbMsg += "[" + surfaceWidth + "×" + surfacHight + "]degrees=" + degrees + "dig";
			if ( camera == null ) {
				camera = new MyC2(context , this);                //Camera.open(0);              //APIL9
				camera.open();
			}
			this.degrees = camera.getCameraRotation();
			dbMsg += "," + degrees + "dig";
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	@Override
	public void surfaceChanged(SurfaceHolder holder , int format , int width , int height) {
		final String TAG = "surfaceChanged[C2S]";
		String dbMsg = "";
		try {
			dbMsg = ", format=" + format + "[" + width + "×" + height + "]";
			this.holder = holder;
//			Canvas canvas = holder.lockCanvas(); // ロックをかける
//			dbMsg += "canvas[" + canvas.getWidth() + "×" + canvas.getHeight() + "]";

			this.setDrawingCacheEnabled(true);      // View の描画キャッシュキャッシュを取得する設定にする
			this.destroyDrawingCache();             // 既存のキャッシュをクリアする
			Bitmap bitmap = this.getDrawingCache();    // キャッシュを作成して取得する  	http://blog.lciel.jp/blog/2013/12/16/android-capture-view-image/

//			Bitmap bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
			dbMsg += "bitmap[" + bitmap.getWidth() + "×" + bitmap.getHeight() + "]";
			dbMsg += "" + bitmap.getByteCount() + "バイト";
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG , 100 , byteArrayOutputStream);
			//	Bitmap.CompressFormat.PNG	;	PNG, クオリティー100としてbyte配列にデータを格納
			byteArrayOutputStream.flush();
			byte[] data = byteArrayOutputStream.toByteArray();
			if ( data != null ) {
				dbMsg += "、data=" + data.length + "バイト";
				Bitmap bmp = BitmapFactory.decodeByteArray(data , 0 , data.length);
				dbMsg += "、bmp=" + bmp.getByteCount() + "バイト";
			}
			this.degrees = camera.getCameraRotation();
			dbMsg += "," + degrees + "dig";
			readFrame(data , width , height);
			byteArrayOutputStream.close();

//			holder.unlockCanvasAndPost(canvas); // ロックを解除
			if ( camera.jpegImageReader != null ) {
				ImageReader imageReader = camera.jpegImageReader;
				dbMsg += ",ImageReader[" + imageReader.getWidth() + "×" + imageReader.getHeight() + "]";
				ByteBuffer buffer = imageReader.acquireLatestImage().getPlanes()[0].getBuffer();                    // 画像バイナリの取得
//						dbMsg += "、"+buffer. +"バイト";
				byte[] bytes = new byte[buffer.capacity()];
				buffer.get(bytes);
				dbMsg += "、" + bytes.length + "バイト";


				OutputStream output = null;
				try {
					output = new FileOutputStream(new File("filename"));                        // 画像の書き込み
					output.write(bytes);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}
//			canvasRecycle();
//			camera.startPreview();     //APIL1
			// format=4[1776×1080]bitmap[1776×1080]7672320バイト、data=7544バイト、bmp=7672320バイト,0dig
			
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void surfaceDestroy() {
		final String TAG = "surfaceDestroy[C2S]";
		String dbMsg = "";
		try {
			if ( camera != null ) {
				if ( camera.mPreviewSession != null ) {
					try {
						camera.mPreviewSession.stopRepeating();
					} catch (CameraAccessException er) {
						myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
					}
					camera.mPreviewSession.close();
				}

				if ( camera.mCamera != null ) {
					camera.mCamera.close();                    // カメラデバイスとの切断
				}
//				camera.stopFaceDetection();
//				camera.stopPreview();
//				camera.release();
				camera = null;
				dbMsg = "camera破棄";
			}
//			if ( faceRecognitionView != null ) {
//				faceRecognitionView.canvasRecycle();
//				dbMsg += "認証クラス破棄";
//			}
			canvasRecycle();

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		final String TAG = "surfaceDestroyed[C2S]";
		String dbMsg = "holder=" + holder;
		try {
			surfaceDestroy();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	///顔検出//////////////////////////////////////////////////////////////////////////////

	/**
	 * 渡されたデータを Bitmap に変換します。
	 * @param data
	 * @param width
	 * @param height
	 * @param degrees
	 * @return
	 */
	private Bitmap decode(byte[] data , int width , int height , int degrees) {
		final String TAG = "decode[FR]";
		String dbMsg = "";
		String dbMsg2 = "";
		try {
			dbMsg += "data=" + data.length;
			dbMsg += "[" + width + "×" + height + "]" + degrees + "dig";
			//java.lang.ArrayIndexOutOfBoundsException: length=7545; index=1918080
			if ( rgb == null ) {
				rgb = new int[width * height];
			}
			dbMsg += ",rgb=" + rgb.length;
			final int frameSize = width * height;
			for ( int j = 0, yp = 0 ; j < height ; j++ ) {
				dbMsg2 = ", " + j;
				int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
//				dbMsg2 += uvp;
				for ( int i = 0 ; i < width ; i++ , yp++ ) {
					dbMsg2 += "×" + i + ")" + yp;
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

			bitmap.setPixels(rgb , 0 , width , 0 , 0 , width , height);            //APIL1
			dbMsg += ",bitmap=" + bitmap.getByteCount();

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + dbMsg2 + ";でエラー発生；" + er);
		}
		return bitmap;
	}

	/**
	 * viewのデータを受け取り認証処理を開始する
	 * 元は onPreviewFrame
	 */
	public void readFrame(byte[] data , int previewWidth , int previewHeight) {        //, Camera camera      /
		final String TAG = "readFrame[FR]";
		String dbMsg = "";
		try {
			dbMsg += "data=" + data.length;
			dbMsg += "[" + previewWidth + "×" + previewHeight + "]" + degrees + "dig";
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
				Utils.bitmapToMat(bitmap , image);                                    //openCV
				dbMsg += ",image=" + image.size();
				detector.detectMultiScale(image , objects);
				;                        //openCV
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
				invalidate();                                                //onDrawへ
			}

			//	 data=3110400[1920×1080]0dig,bitmap=8294400,image=1920x1080,objects=1x0,faces=0
			//I/onPreviewFrame[Surface]: data=3110400{1920×1080]

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
			dbMsg += "[" + width + "×" + height + "]faces=" + faces.size();

			for ( RectF face : faces ) {
				RectF r = new RectF(width * face.left , height * face.top , width * face.right , height * face.bottom);
				canvas.drawRect(r , paint);
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}
	//////////////////////////////////////////////////////////////////////////////顔検出///

	/**
	 * このクラスで作成したリソースの破棄
	 */
	public void canvasRecycle() {
		final String TAG = "canvasRecycle[FR]";
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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public class MyC2 {
		public Context context;
		public CameraDevice mCamera;
		public String cameraId = "0";        //メインカメラID
		public String cameraId_sub = "1";  //サブカメラID
		public CameraManager mCameraManager = null;
		public CameraCharacteristics mCharacteristics;
		public SurfaceView mSurfaceView;
		public Size mCameraSize;
		public CaptureRequest.Builder mPreviewBuilder;
		public CameraCaptureSession mPreviewSession;
		public Handler previewHandler;
		public HandlerThread previewThread;
		public ImageReader jpegImageReader;
		public WindowManager mWindowManager;
		public int baceWidth;
		public int bacHight;

		/**
		 * コンストラクタでプレビューを受け取る
		 **/
		public MyC2(Context context , SurfaceView surfaceView) {
			final String TAG = "CS_Camera2[C2]";
			String dbMsg = "";
			try {
				this.context = context;
				mSurfaceView = surfaceView;
				baceWidth = mSurfaceView.getWidth();
				bacHight = mSurfaceView.getHeight();
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}


		// http://woshidan.hatenadiary.jp/entry/2017/09/10/022510
		private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
			// Surfaceから画像が利用できるようになった時に呼び出される
			@Override
			public void onImageAvailable(ImageReader reader) {
				final String TAG = "onImageAvailable[C2]";
				String dbMsg = "";
				try {				// Imageは各種コーデック(圧縮方法みたいなもの)で圧縮したりする、画像のByteBufferを扱うためのオブジェクト
				Image image = reader.acquireLatestImage();
				// 何枚か画像を扱うことができて(?)、それぞれはPlanesに入っている
				// この辺のコードは https://developer.android.com/things/training/doorbell/camera-input.html のサンプルより
				ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
				final byte[] imageBytes = new byte[imageBuf.remaining()];
				imageBuf.get(imageBytes);
				image.close();

				final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

//				getAc	runOnUiThread(new Runnable() {
//					@Override
//					public void run() {
////						ImageView imageView = (ImageView) findViewById(R.id.picture);
////						imageView.setImageBitmap(bitmap);
//					}
//				});
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}
		};

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

					ArrayList< Surface > surfaceList = new ArrayList();
					surfaceList.add(mSurfaceView.getHolder().getSurface());                        // プレビュー用のSurfaceViewをリストに登録
					dbMsg += "surfaceList=" + surfaceList.size() + "件";
//					mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//					mPreviewBuilder.addTarget(mSurfaceView.getHolder().getSurface());                    // プレビューリクエストの設定（SurfaceViewをターゲットに）
					// キャプチャーセッションの開始(セッション開始後に第2引数のコールバッククラスが呼ばれる)
//					mSurfaceView.createCaptureSession(surfaceList, new CameraCaptureSessionCallback(), null);

					createMyCaptureSession();            //プレビュー設定

					myLog(TAG , dbMsg);
//				} catch (CameraAccessException er) {
//					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
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
					mCameraManager = ( CameraManager ) context.getSystemService(CAMERA_SERVICE);      //逐次取り直さないとダメ？
				}

				dbMsg += "、getCameraIdList=" + mCameraManager.getCameraIdList().length + "件";
				for ( String cId : mCameraManager.getCameraIdList() ) {
					dbMsg += "cameraId=" + cId;
//					if ( mCharacteristics == null ) {
					mCharacteristics = mCameraManager.getCameraCharacteristics(cId);
//					}
					StreamConfigurationMap map;
					if ( mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK ) {    //1;メインカメラ
						dbMsg += ";LENS_FACING_BACK";
						this.cameraId = cId;
						map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
						mCameraSize = map.getOutputSizes(SurfaceTexture.class)[0];                                                    //APIL21
						dbMsg += "、mCameraSize>[" + mCameraSize.getWidth() + "x" + mCameraSize.getHeight() + "]";
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
//						mCameraManager.openCamera(cameraId , mCameraDeviceCallback , null);             //CameraManagerにオープン要求を出します。
						//		break;                    //	return;
					} else if ( mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT ) {        //2;サブカメラ
						dbMsg += "、サブ=" + cId;
						cameraId_sub = cId;
						map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
						Size mCameraSize_sub = map.getOutputSizes(SurfaceTexture.class)[0];                                                    //APIL21
						dbMsg += "、mCameraSize>[" + mCameraSize_sub.getWidth() + "x" + mCameraSize_sub.getHeight() + "]";
					}
				}
				dbMsg += "；isMainCamsera=" + isMainCamsera;

				if ( isMainCamsera ) {
					dbMsg += "、メイン=" + cameraId + "で起動";
					mCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
					mCameraManager.openCamera(cameraId , mCameraDeviceCallback , null);             //CameraManagerにオープン要求を出します。
				} else {
					dbMsg += "、サブ=" + cameraId_sub + "で起動";
					mCharacteristics = mCameraManager.getCameraCharacteristics(cameraId_sub);
					mCameraManager.openCamera(cameraId_sub , mCameraDeviceCallback , null);
				}
				myLog(TAG , dbMsg);
			} catch (CameraAccessException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		/**
		 * プレビューサイズ設定
		 * 課題；縦方向が反映されないので調整保留
		 */
		public void setPreviewSize() {
			final String TAG = "setPreviewSize[C2]";
			String dbMsg = "";
			try {
				Double widht = mCameraSize.getWidth() * 1.0;
				Double hight = mCameraSize.getHeight() * 1.0;
				Double camAspect = widht / hight;
				dbMsg += ",camAspect=" + camAspect;
				dbMsg += "[" + baceWidth + "×" + bacHight + "]";
				int setWidth = ( int ) (baceWidth / camAspect);
				int setHight = bacHight;
				dbMsg += "[" + setWidth + "×" + setHight + "]";
				///Viewサイズを入力のアスペクト比に合わせる///////////////////////////////////////////////////////////////
//				ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) mSurfaceView.getLayoutParams();
//				lp.width = setWidth; //横幅
//				lp.height = setHight; //縦幅
//				mSurfaceView.setLayoutParams(lp);
////////////////////////////////////////////////////////////////Viewサイズを入力のアスペクト比に合わせる//

				int camDig = getCameraRotation();
				dbMsg += ",camDig=" + camDig;
				if ( camDig == 90 || camDig == 270 ) {
					dbMsg += "縦";
					int temp = setWidth;
					setWidth = setHight;
					setHight = temp;
				}
				dbMsg += ">[" + setWidth + "×" + setHight + "]";
//				mSurfaceView.getHolder().setFixedSize(setWidth , setHight);                    // SurfaceViewにプレビューサイズを設定する(サンプルなので適当な値です)
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		/**
		 * CaptureSessionを生成  ;
		 * onOpenedから起動時一回
		 */
		private void createMyCaptureSession() {
			final String TAG = "createMyCaptureSession[C2]";
			String dbMsg = "";
			try {
				setPreviewSize();
//				dbMsg = "isAvailable=" + mSurfaceView.isAvailable();
//				if ( mSurfaceView.isAvailable() ) {
//					SurfaceTexture texture = mSurfaceView.getSurfaceTexture();
//					texture.setDefaultBufferSize(mCameraSize.getWidth() , mCameraSize.getHeight());                     //プレビュー用のSurfaceを生成します。
//					Surface surface = new Surface(texture);
				try {
					mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);   // プレビュー用のCaptureRequest.Builderを生成
				} catch (CameraAccessException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}

				mPreviewBuilder.addTarget(mSurfaceView.getHolder().getSurface());   //CaptureRequest.Builderにプレビュー用のSurfaceを設定
				try {
					mCamera.createCaptureSession(Collections.singletonList(mSurfaceView.getHolder().getSurface()) , mCameraCaptureSessionCallback , null);        //キャプチャーセッションの開始(セッション開始後に第2引数のコールバッククラスが呼ばれる)
				} catch (CameraAccessException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
//				}
				if ( mCameraSize != null ) {
					if ( 0 < mCameraSize.getWidth() && 0 < mCameraSize.getHeight() ) {
						jpegImageReader = ImageReader.newInstance(mCameraSize.getWidth() , mCameraSize.getHeight() , ImageFormat.JPEG , 1);    //640 , 480
						jpegImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
								}
				}
				// キャプチャ取得用のイメージリーダを作成
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
		 * onConfiguredから呼ばれる
		 */
		public void updatePreview() {
			final String TAG = "updatePreview[C2]";
			String dbMsg = "";
			try {

				mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION , getCameraRotation());
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
				dbMsg += ">sensor=" + sensorOrientation + "dig";
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}                                                            //プレビューを開始

		/***
		 * 回転補正
		 * 参照		https://moewe-net.com/android/2016/camera2-jpeg-orientation
		 * */
		public int getCameraRotation() {
			final String TAG = "getCameraRotation[C2]";
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

				CameraManager cameraManager = ( CameraManager ) context.getSystemService(CAMERA_SERVICE);
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
				comOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);       // 0, 90, 180, 270などの角度になっている
				dbMsg += ",カメラ2；=" + comOrientation + "dig";
				lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
				lensFacingFront = CameraCharacteristics.LENS_FACING_FRONT;

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
 * SurfaceViewでもCamera2でプレビューを表示できる		http://mussyu1204.myhome.cx/wordpress/it/?p=127
 * <p>
 * mm-camera: <STATS_AF ><ERROR> 4436: af_port_handle_pdaf_stats: Fail to init buf divert ack ctrl
 */