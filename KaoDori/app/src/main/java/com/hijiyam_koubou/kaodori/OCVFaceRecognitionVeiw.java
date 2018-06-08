package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.SurfaceView;
import android.view.View;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class OCVFaceRecognitionVeiw extends SurfaceView {
	private Context context;
	private int[] rgb;
	private Bitmap bitmap;
	private Mat image;
	private CascadeClassifier detector;
	private MatOfRect objects;
	private List< RectF > faces = new ArrayList< RectF >();
	private int degrees;
	private int viewWidth;
	private int viewHight;


	public OCVFaceRecognitionVeiw(Context context , long haarcascadesLastModified) {
		super(context);
		final String TAG = "OCVFaceRecognitionVeiw[OCVFR]";
		String dbMsg = "";
		try {
			this.context = context;
			try {
				copyAssets("haarcascades" , haarcascadesLastModified);                    // assetsの内容を /data/data/*/files/ にコピーします。
			} catch (IOException er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
			String filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
			dbMsg = "filename=" + filename;       //filename=/data/user/0/com.hijiyam_koubou.kaodori/files/haarcascades/haarcascade_frontalface_alt.xml
			File rFile = new File(filename);
			dbMsg += ";exists=" + rFile.exists();
			myLog(TAG , dbMsg);

			detector = new CascadeClassifier(filename);
			objects = new MatOfRect();

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	/**
	 * Bitmapデータを受け取り認証処理を開始する
	 */
	public void readFrameRGB(Bitmap bitmap) {        //, Camera camera      /   , int previewWidth , int previewHeight
		final String TAG = "readFrameRGB[OCVFR]";
		String dbMsg = "";
		try {
			int dWidth = bitmap.getWidth();
			int dHight = bitmap.getHeight();
			dbMsg += ",bitmap[" + dWidth + "×" + dHight + "]";
			int byteCount = bitmap.getByteCount();
			dbMsg += "" + byteCount + "バイト";
			dbMsg += "[" + dWidth + "×" + dHight + "]" + degrees + "dig";
//			Bitmap bitmap = decode(data , previewWidth , previewHeight , degrees);
//			if ( bitmap != null ) {
			dbMsg += ",bitmap=" + bitmap.getByteCount();
			if ( degrees == 90 ) {
				int tmp = dWidth;
				dWidth = dHight;
				dHight = tmp;
			}
			if ( image == null ) {
				image = new Mat(dHight , dWidth , CvType.CV_8U , new Scalar(4));
			}
			Utils.bitmapToMat(bitmap , image);                                    //openCV
			dbMsg += ",image=" + image.size();
			detector.detectMultiScale(image , objects);
			;                        //openCV
			dbMsg += ",objects=" + objects.size();
			faces.clear();
			for ( org.opencv.core.Rect rect : objects.toArray() ) {
				float left = ( float ) (1.0 * rect.x / dWidth);
				float top = ( float ) (1.0 * rect.y / dHight);
				float right = left + ( float ) (1.0 * rect.width / dWidth);
				float bottom = top + ( float ) (1.0 * rect.height / dHight);
				faces.add(new RectF(left , top , right , bottom));
			}
			dbMsg += ",faces=" + faces.size();
			invalidate();                                                //onDrawへ
//			}

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
		final String TAG = "onDraw[OCVFR]";
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


	/**
	 * assetsの内容を /data/data/.../files/ にコピーします。
	 */
	private void copyAssets(String dir , long haarcascadesLastModified) throws IOException {
		final String TAG = "copyAssets[OCVFR}";
		String dbMsg = "";
		try {
			dbMsg = "dir=" + dir;
//			MainActivity MA = new MainActivity();

			dbMsg += ",認証ファイル最終更新日=" + haarcascadesLastModified;
			byte[] buf = new byte[8192];
			int size;
			boolean isCopy = false;    //初回使用時なと、強制的にコピーする
			File dst = new File(getContext().getApplicationContext().getFilesDir() , dir);
			if ( !dst.exists() ) {
				dst.mkdirs();
				dst.setReadable(true , false);
				dst.setWritable(true , false);
				dst.setExecutable(true , false);
				dbMsg += ">>作成";
				isCopy = true;
			}
			int readedCount = dst.list().length;
			dbMsg += ",読込み済み=" + readedCount + "件";
			if ( readedCount < 10 ) {
				isCopy = true;
			}
			for ( String filename : getContext().getApplicationContext().getAssets().list(dir) ) {
				File file = new File(dst , filename);
				Long lastModified = file.lastModified();
				if ( isCopy || haarcascadesLastModified < lastModified ) {    //無ければ
					dbMsg += "," + filename + ";" + lastModified;
					haarcascadesLastModified = lastModified;
					OutputStream out = new FileOutputStream(file);
					InputStream in = getContext().getApplicationContext().getAssets().open(dir + "/" + filename);
					while ( (size = in.read(buf)) >= 0 ) {
						if ( size > 0 ) {
							out.write(buf , 0 , size);
						}
					}
					in.close();
					out.close();
					file.setReadable(true , false);
					file.setWritable(true , false);
					file.setExecutable(true , false);
					dbMsg += ">>コピー";
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	//////////////////////////////////////////////////////////////////////////////顔検出///

	public void setCondition(int _width , int _hight , int _degrees) {
		final String TAG = "setCondition[OCVFR]";
		String dbMsg = "";
		try {
			dbMsg += "[" + _width + "×" + _hight + "]_degrees=" + _degrees;

			viewWidth = _width;
			viewHight = _hight;
			degrees = _degrees;


			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * このクラスで作成したリソースの破棄
	 */
	public void canvasRecycle() {
		final String TAG = "canvasRecycle[OCVFR]";
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
