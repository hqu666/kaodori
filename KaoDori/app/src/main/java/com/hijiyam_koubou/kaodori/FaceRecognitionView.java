package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionView extends View {
	public int degrees;
	public int[] rgb;
	public Bitmap bitmap;
	public Mat image;
	private MatOfRect objects;
	private CascadeClassifier detector;
	private List< RectF > faces = new ArrayList< RectF >();

	public FaceRecognitionView(Context context , int degrees) {
		super(context);
		final String TAG = "FaceRecognitionView[FR]";
		String dbMsg = "";
		try {
			dbMsg = "degrees=" + degrees;
			this.degrees = degrees;
			String filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
			dbMsg += ",filename=" + filename;
			detector = new CascadeClassifier(filename);
			dbMsg += ",detector=" + detector.getFeatureType();
			objects = new MatOfRect();
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
			width--;
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
				bitmap = Bitmap.createBitmap(width , height , Bitmap.Config.ARGB_8888);
			}

			bitmap.setPixels(rgb , 0 , width , 0 , 0 , width , height);
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
