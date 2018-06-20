package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
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
	private Mat imageMat;
	private CascadeClassifier detector;
	private MatOfRect objects;
	private List< RectF > faces = new ArrayList< RectF >();
	//	private int degrees;
	private int viewLeft;
	private int viewTop;
	private int viewWidth;
	private int viewHight;
	private Double viewAspect;
	private String filename;
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
			detector = null;
			objects = null;
			constractCommon(context);
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
			detector = null;
			objects = null;
			constractCommon(context);
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

	public void constractCommon(Context context) {
		final String TAG = "constractCommon[OCVFR]";
		String dbMsg = "";
		try {
			this.context = context;
			if ( detector == null ) {
				filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
				dbMsg += "filename=" + filename;       //filename=/data/user/0/com.hijiyam_koubou.kaodori/files/haarcascades/haarcascade_frontalface_alt.xml
				File rFile = new File(filename);
				dbMsg += ";exists=" + rFile.exists();
				detector = new CascadeClassifier(filename);
				dbMsg += "detector生成";
			} else {
				dbMsg += "detector既存";
			}
			objects = new MatOfRect();
			isCompletion = true;
			// 			setCondition();     この時点ではレイアウトが拾えない
			faces.clear();
			faces.add(new RectF(0 , 0 , 1 , 1));            //APIL1
			invalidate();                                                //onDrawへ
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	//ViewのLifeCycle ////////////////////////////////////////////////////////////
	@Override
	protected void onVisibilityChanged(View changedView , int visibility) {
		super.onVisibilityChanged(changedView , visibility);
		final String TAG = "onVisibilityChanged[OCVFR]";
		String dbMsg = "開始";
		try {
			dbMsg = "visibility=" + visibility;     //	読み込まれた時に一度呼ばれて0
			dbMsg += ",changedView=" + changedView.getId() + "/this;" + this.getId();
// setCondition();    ここから呼んでも0,0のまま
//			if (visibility == View.VISIBLE) //onResume called
//    else // onPause() called
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		final String TAG = "onWindowFocusChanged[OCVFR]";
		String dbMsg = "開始";
		try {
			dbMsg += ",hasWindowFocus=" + hasWindowFocus;  //読み込み時true ,破棄時false
			if ( hasWindowFocus ) {
				//				 setCondition();
			}
			//			if (hasWindowFocus) //onresume() called   else // onPause() called
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		final String TAG = "onDetachedFromWindow[OCVFR]";
		String dbMsg = "開始";
		try {
			// onDestroy() called
			myLog(TAG , dbMsg);        //拾えず
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		final String TAG = "onAttachedToWindow[OCVFR]";
		String dbMsg = "開始";
		try {
			// onCreate() called
			myLog(TAG , dbMsg);   //拾えず
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}
	// //////////////////////////////////////////////////////////ViewのLifeCycle//

	/**
	 * Bitmapデータを受け取り認証処理を開始する
	 */
	public List< Rect > readFrameRGB(Bitmap bitmap , int degrees) {        //, Camera camera      /   , int previewWidth , int previewHeight
		final String TAG = "readFrameRGB[OCVFR]";
		String dbMsg = "";
		List< Rect > retArray = new ArrayList();
		try {
			isCompletion = false;
			if ( detector == null ) {
				dbMsg += "filename=" + filename;       //filename=/data/user/0/com.hijiyam_koubou.kaodori/files/haarcascades/haarcascade_frontalface_alt.xml
				File rFile = new File(filename);
				dbMsg += ";exists=" + rFile.exists();
				detector = new CascadeClassifier(filename);
				dbMsg += ";detector作成";
			}
			if ( objects == null ) {
				objects = new MatOfRect();
				dbMsg += ";object作成";
			}
			if ( viewAspect == null ) {
				setCondition();
			}
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
			dbMsg += ",degrees=" + degrees;
			double correctionH = dAspect;
			double correctionV =  1.0;
			if ( degrees == 90 || degrees == 270 ) {                    //270追加
				dbMsg += ";縦";
				int tmp = dWidth;                         //入れ替えてMatにしないとaspectが狂う
				dWidth = dHight;
				dHight = tmp;
				 correctionH = 1 / dAspect;
				 correctionV =  dAspect;
				dbMsg += ">>[" + dWidth + "×" + dHight + "]";
			}else{
				dbMsg += ";横";
				Matrix mat = new Matrix();
				mat.postRotate(270);   				// 回転マトリックス作成（90度回転）
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, dWidth, dHight, mat, true);
				dbMsg += "[" +  bitmap.getWidth() + "×" + bitmap.getHeight() + "]" + bitmap.getByteCount() + "バイト";
//				imageMat = new Mat(dWidth , dHight , CvType.CV_8U , new Scalar(4));     //1バイトのチャンネル0　、
			}
			imageMat = new Mat(dHight , dWidth , CvType.CV_8U , new Scalar(4));     //1バイトのチャンネル0　、
			Utils.bitmapToMat(bitmap , imageMat);                                    //openCV； 画像データを変換（BitmapのMatファイル変換
			dbMsg += ",imageMat=" + imageMat.size() + ",elemSize=" + imageMat.elemSize();
			detector.detectMultiScale(imageMat , objects);                       //openCV；カスケード分類器に画像データを与え顔認識
			dbMsg += ",objects=" + objects.size();
			faces.clear();
			for ( org.opencv.core.Rect rect : objects.toArray() ) {
				Rect rRect = new Rect(rect.x , rect.y , rect.width , rect.height);//顔の位置（X座標）,顔の位置（Y座標）,顔の横幅,顔の縦幅     /

//				float left = ( float ) (1.0 * rect.x / dWidth);                             //1 / dAspect
//				float top = ( float ) (1.0 * rect.y / dHight);                              //dAspect
//				float right = left + ( float ) (1.0 * rect.width / dWidth);            //1 / dAspect
//				float bottom = top + ( float ) (1.0 * rect.height / dHight);            //dAspect
//
//
//				 left = ( float ) (1 / dAspect * left);                             //
//				 top = ( float ) (dAspect *top);                              //
//				 right = left + ( float ) (1 / dAspect * right);            //
//				 bottom = top + ( float ) (dAspect * bottom);            //
				float left = ( float ) (correctionH * rect.x / dWidth);                             //
				float top = ( float ) (correctionV * rect.y / dHight);                              //
				float right = left + ( float ) (correctionH* rect.width / dWidth);            //
				float bottom = top + ( float ) (correctionV * rect.height / dHight);            //

				faces.add(new RectF(left , top , right , bottom));
				retArray.add(rRect);
			}
			dbMsg += ",faces=" + faces.size();
			if ( 0 == faces.size() ) {                            //顔が検出できない時は
//				faces.clear();
				faces.add(new RectF(0 , 0 , 1 , 1));            //プレビュー全体選択
			}
			invalidate();            //onDrawへ
			bitmap.recycle();
			if ( imageMat != null ) {
				imageMat.release();
				imageMat = null;
				dbMsg += "imageMat破棄";
			}
			;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
		}
		return retArray;
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
					float sRight = fRight - 250;
					if ( fRight < sRight ) {
						sRight = fLeft + (fRight - fLeft) / 2;
					}
					canvas.drawText(fRight + " , " + fBottom , sRight , fBottom - 32 , paint);
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


	//////////////////////////////////////////////////////////////////////////////顔検出///

	/**
	 * 実装直後に呼び出しサイズ調整
	 */
	public void setCondition() {
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
			ViewGroup.MarginLayoutParams layoutParams = ( ViewGroup.MarginLayoutParams ) taregetView.getLayoutParams();
			viewLeft = layoutParams.leftMargin;
			viewTop = layoutParams.topMargin;
			viewWidth = layoutParams.width;
			viewHight = layoutParams.height;
			viewAspect = 1.0 * viewWidth / viewHight;
			dbMsg += ",layoutParams(" + viewLeft + "," + viewTop + ")[" + viewWidth + "×" + viewHight + "]Aspect=" + viewAspect;
			if ( viewAspect < 1 ) {
				dbMsg += ";縦";                    //[1080×1440]Aspect=0.75
			} else {
				dbMsg += ";横";                    //[1440×1080]Aspect=1.3333333333333333
			}
			float left = 0;//viewLeft;                // float ) (1.0 * rect.x / _width);
			dbMsg += ">描画>(" + left;

//			if ( 0 < left ) {
//				left = ( float ) (1.0 * left /  layoutParams.width);     //?2
//			}
			float top = 0;//layoutParams.topMargin;                    // float ) (1.0 * rect.y / _hight);
			dbMsg += "," + top;
//			if ( 0 < top ) {
//				top = ( float ) (1.0 * top / layoutParams.height);
//			}
			float right = left + viewWidth;            //( float ) (1.0 * rect.width / _width);
			dbMsg += ")～(" + right;
//			if ( 0 < top ) {
			if ( 0 < right ) {
				right = ( float ) (1.0 * right / viewWidth);
			}
			float bottom = top + viewHight;            // top + ( float ) (1.0 * rect.height / _hight);
			dbMsg += "," + bottom + "）";
			if ( 0 < bottom ) {
				bottom = ( float ) (1.0 * bottom / viewHight);
			}
			dbMsg += ">比率>(" + left + "," + top + ")～(" + right + "," + bottom + "）";

			faces.add(new RectF(left , top , right , bottom));            //APIL1
//			}
			dbMsg += ",faces=" + faces.size();
			invalidate();                                                //onDrawへ

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
			if ( imageMat != null ) {
				imageMat.release();
				imageMat = null;
				dbMsg += "imageMat破棄";
			}
			if ( bitmap != null ) {
				if ( !bitmap.isRecycled() ) {
					bitmap.recycle();
				}
				bitmap = null;
				dbMsg += ",bitmap破棄";
			} else {
				dbMsg += ",既にbitmap破棄";
			}
			if ( rgb != null ) {
				rgb = null;
				dbMsg += ",rgb破棄";
			} else {
				dbMsg += "," + "既にrgb破棄";
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


/**
 * Face Detection using Haar Cascades			https://docs.opencv.org/trunk/d7/d8b/tutorial_py_face_detection.html
 *2017年12月20日		OpenCVを使って顔を検出する					http://bkmts.xsrv.jp/opencv-face/
 * 2015年11月15日		OpenCVで顔認識など特徴ある領域の検出		https://qiita.com/yokobonbon/items/f7ff24cc449a1fe6ba4b
  *OpenCV 3.0.0			OpenCV 3.0.0 による顔検出処理				https://yamsat.wordpress.com/2015/09/13/opencv-3-0-0-%E3%81%AB%E3%82%88%E3%82%8B%E9%A1%94%E6%A4%9C%E5%87%BA%E5%87%A6%E7%90%86/
 * 2012年10月28日		[OpenCV] 顔を検出する						http://google-os.blog.jp/archives/50736832.html
 *  OpenCV 2.3.1		アンドロイドでOpenCV（お顔検出				http://foonyan.sakura.ne.jp/wisteriahill/opencv_android/index.html
 *  2012/6/23			目を検出する　ついでに口・鼻も				http://nobotta.dazoo.ne.jp/blog/?p=503
 *  2012年10月28日		[OpenCV] 目を検出する						http://google-os.blog.jp/archives/50736850.html
 *ファイルから読込み
 * 2016-09-13			Opencv3.1で顔検出							http://garapon.hatenablog.com/entry/2016/09/13/Opencv3.1%E3%81%A7%E9%A1%94%E6%A4%9C%E5%87%BA
 *機械学習
 * 2016年05月23日		機械学習のためのOpenCV入門					https://qiita.com/icoxfog417/items/53e61496ad980c41a08e
 * 2012年03月01日		ARに使えるOpenCVで作る画像認識Androidアプリ	http://www.atmarkit.co.jp/ait/articles/1203/01/news159.html
 * SurfaceView の使用例
 *  2017-02-10		 	AndroidでOpenCV 3.2を使って顔検出をする		https://blogs.osdn.jp/2017/02/10/opencv.html
 * Gppgle PlayのFace API
 * 2016-01-11			Androidで顔検出APIが使えるようになった		https://www.gesource.jp/weblog/?p=7316
 * Camera.Faceクラス
 * 2014/04/12			Android で顔認識を試してみた				http://blog.kotemaru.org/2014/04/12/android-face-detection.html
 *  2012年09月26日		FaceDetectorで Bitmap から顔を検出する		https://dev.classmethod.jp/smartphone/android-tips-15-facedetector/
 * 2011年11月22日		カメラプレビューで顔検出を行う				https://techbooster.org/android/multimedia/10375/
 * **/
