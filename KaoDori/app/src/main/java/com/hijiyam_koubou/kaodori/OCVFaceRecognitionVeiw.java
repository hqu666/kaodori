package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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

public class OCVFaceRecognitionVeiw extends View {
	static {
		System.loadLibrary("opencv_java3");  // OpenCV使用クラスに必須
	}

	private Context context;
	//	private ViewGroup VG;   					//読み込まれる土台
	private int[] rgb;
	private Bitmap bitmap;
	private Mat image;
	private CascadeClassifier detector;
	private MatOfRect objects;
	private List< RectF > faces = new ArrayList< RectF >();
	private int degrees;
	private int viewLeft;
	private int viewTop;
	private int viewWidth;
	private int viewHight;
	private Double viewAspect;
	private static final int COLOR_CHOICES[] = {Color.WHITE , Color.GREEN , Color.MAGENTA , Color.BLUE , Color.CYAN , Color.RED , Color.YELLOW};

	/**
	 * 書き換え終了
	 */
	private boolean isCompletion = true;


	public OCVFaceRecognitionVeiw(Context context) {
		super(context);
		final String TAG = "OCVFaceRecognitionVeiw[OCVFR]";
		String dbMsg = "class読込み";
		try {
//			constractCommon( context);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public OCVFaceRecognitionVeiw(Context context , AttributeSet attrs) {
		super(context , attrs);
		final String TAG = "OCVFaceRecognitionVeiw[OCVFR]";
		String dbMsg = "view組み込み";
		try {
//			constractCommon( context);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

//	public OCVFaceRecognitionVeiw(Context context, AttributeSet attrs, int defStyle) {
//		super(context, attrs, defStyle);
//		time = 0;
//		df = new SimpleDateFormat("HH:mm:ss");
//	}

	public void constractCommon(Context context , long haarcascadesLastModified) {
		final String TAG = "constractCommon[OCVFR]";
		String dbMsg = "";
		try {
			this.context = context;
			if ( detector == null ) {
				try {
					copyAssets("haarcascades" , haarcascadesLastModified);                    // assetsの内容を /data/data/*/files/ にコピーします。
				} catch (IOException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
				String filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
				dbMsg += "filename=" + filename;       //filename=/data/user/0/com.hijiyam_koubou.kaodori/files/haarcascades/haarcascade_frontalface_alt.xml
				File rFile = new File(filename);
				dbMsg += ";exists=" + rFile.exists();
				detector = new CascadeClassifier(filename);
				objects = new MatOfRect();

				isCompletion = true;
			} else {
				dbMsg += "detector!=null";
			}
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
			isCompletion = false;
//			int dWidth = bitmapOrg.getWidth();
//			int dHight = bitmapOrg.getHeight();
//			dbMsg += ",bitmap[" + dWidth + "×" + dHight + "]";
//			Double wScale = (Double)(1.0*viewWidth/  dWidth);
//			Double hScale = (Double)(1.0*viewHight/  dHight);
//			dbMsg += ",wScale=" + wScale + ",hScale=" + hScale + "]";
//			Bitmap bitmap= Bitmap.createScaledBitmap(bitmapOrg, viewWidth, viewHight, false);
			int dWidth = bitmap.getWidth();
			int dHight = bitmap.getHeight();
			dbMsg += "[" + dWidth + "×" + dHight + "]" + degrees + "dig";
			int byteCount = bitmap.getByteCount();
			dbMsg += "," + byteCount + "バイト";
			Double dAspect = 1.0 * dWidth / dHight;
			dbMsg += ",dAspect=" + dAspect + "(" + viewAspect + ")";
			Double scaleX = 1.0 * viewWidth / dWidth;
			Double scaleY = 1.0 * viewHight / dHight;
			dbMsg += ",scale=" + scaleX + ":" + scaleY;
//			Bitmap bitmap = decode(data , previewWidth , previewHeight , degrees);
//			if ( bitmap != null ) {
			dbMsg += ",bitmap=" + bitmap.getByteCount();
			if ( degrees == 90 ) {
				int tmp = dWidth;
				dWidth = dHight;
				dHight = tmp;
			}
			if ( image == null ) {
				image = new Mat(dHight , dWidth , CvType.CV_8U , new Scalar(4));     //1バイトのチャンネル0　、
			}
			Utils.bitmapToMat(bitmap , image);                                    //openCV；MAT形式に変換
			dbMsg += ",MATimage=" + image.size();
			detector.detectMultiScale(image , objects);                       //openCV
			dbMsg += ",objects=" + objects.size();
			faces.clear();
			for ( org.opencv.core.Rect rect : objects.toArray() ) {
				float left = ( float ) (1 / dAspect * rect.x / dWidth);
				float top = ( float ) (dAspect * rect.y / dHight);
				float right = left + ( float ) (1 / dAspect * rect.width / dWidth);
				float bottom = top + ( float ) (dAspect * rect.height / dHight);
				faces.add(new RectF(left , top , right , bottom));
			}
			dbMsg += ",faces=" + faces.size();
			if ( 0 < faces.size() ) {
				invalidate();
			} else {
				isCompletion = true;
			}
			//onDrawへ
//			}
			bitmap.recycle();
			//	 data=3110400[1920×1080]0dig,bitmap=8294400,image=1920x1080,objects=1x0,faces=0
			//I/onPreviewFrame[Surface]: data=3110400{1920×1080]

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
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
			int mCurrentColorIndex = 0;

			int carentColor = Color.GREEN;

			int width = getWidth();              // canvas.getWidth()と同じ
			int height = getHeight();
			dbMsg += "canvas[" + width + "×" + height + "]faces=" + faces.size();
			int fCount = 0;
			for ( RectF face : faces ) {
				fCount++;
				dbMsg += "\n" + fCount + "(" + face.left + "," + face.top + ")～（" + face.right + "," + face.bottom + "）";
				mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
				final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];
				Paint paint = new Paint();
				paint.setColor(selectedColor);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(4);

				float fLeft = width * face.left;
				float fTop = height * face.top;
				float fRight = width * face.right;
				float fBottom = height * face.bottom;
				RectF r = new RectF(fLeft , fTop , fRight , fBottom);
				dbMsg += ",r(" + r.left + "," + r.top + ")～（" + r.right + "," + r.bottom + "）carentColor=" + carentColor;
				canvas.drawRect(r , paint);
				paint.setTextSize(32);
				if ( faces.size() < 2 ) {
					canvas.drawText(fLeft + " , " + fTop , width * face.left + 8 , height * face.top + 40 , paint);
					canvas.drawText(fRight + " , " + fBottom , fLeft + 200 , fBottom - 32 , paint);
				} else {
					canvas.drawText(fCount + "" , width * face.left + 8 , height * face.top + 40 , paint);
				}
			}
			isCompletion = true;
			dbMsg += ",isCompletion=" + isCompletion;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
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

	public void setCondition(int _degrees) {
		final String TAG = "setCondition[OCVFR]";
		String dbMsg = "";
		try {
			isCompletion = false;

//			int width = getWidth();
//			int height = getHeight();
//			dbMsg += "[" + width + "×" + height + "]" ;
			View taregetView = ( View ) this;              //親ビューでサイズを変更する
//			if(! taregetView.isFocused()){
//				dbMsg += "isFocused=false";
//				taregetView.setFocusable(true);
//				dbMsg += ">>"+taregetView.isFocused();
//			}
//			dbMsg += "、現在[" + taregetView.getWidth() + "×" + taregetView.getHeight() + "]";
//			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)taregetView.getLayoutParams();
//			dbMsg += ",layoutParams[" + layoutParams.width + "×" + layoutParams.height + "]";
//			layoutParams.width =_width;
//			layoutParams.height =_hight;
//			taregetView.setLayoutParams(layoutParams);
//			taregetView.requestLayout();
////			taregetView.setMinimumWidth(_width);
////			taregetView.setMinimumHeight(_hight);
//			dbMsg += ">>[" + taregetView.getWidth() + "×" + taregetView.getHeight() + "]";
			faces.clear();
//			for ( org.opencv.core.Rect rect : objects.toArray() ) {
			ViewGroup.MarginLayoutParams layoutParams = ( ViewGroup.MarginLayoutParams ) taregetView.getLayoutParams();
			viewLeft = layoutParams.leftMargin;
			viewTop = layoutParams.topMargin;
			viewWidth = layoutParams.width;
			viewHight = layoutParams.height;
			viewAspect = 1.0 * viewWidth / viewHight;
			dbMsg += ",layoutParams(" + viewLeft + "," + viewTop + ")[" + viewWidth + "×" + viewHight + "]Aspect=" + viewAspect;
			float left = 0;//viewLeft;                // float ) (1.0 * rect.x / _width);
//			if ( 0 < left ) {
//				left = ( float ) (1.0 * left /  layoutParams.width);     //?2
//			}
			float top = 0;//layoutParams.topMargin;                    // float ) (1.0 * rect.y / _hight);
//			if ( 0 < top ) {
//				top = ( float ) (1.0 * top / layoutParams.height);
//			}
			float right = left + viewHight;            //( float ) (1.0 * rect.width / _width);
			if ( 0 < right ) {
				right = ( float ) (1.0 * right / viewHight);
			}
			float bottom = top + viewHight;            // top + ( float ) (1.0 * rect.height / _hight);
			if ( 0 < bottom ) {
				bottom = ( float ) (1.0 * bottom / viewHight);
			}
			dbMsg += ">>(" + left + "," + top + ")～(" + right + "," + bottom + "）";

			faces.add(new RectF(left , top , right , bottom));            //APIL1
//			}
			dbMsg += ",faces=" + faces.size();
			invalidate();                                                //onDrawへ

			degrees = _degrees;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
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

	public boolean getCompletion() {
		return isCompletion;
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
