package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
//import android.graphics.Rect;
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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

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
	private CascadeClassifier detectorFrontalFaceAlt;                //正面顔全体
	private CascadeClassifier detectorEye;                        //目
	private CascadeClassifier detectorRighteye_2splits;                //右目";
	private CascadeClassifier detectorLefteye_2splits;                //左目";
	private CascadeClassifier detectorEyeglasses;                    //眼鏡";
	private CascadeClassifier detectorFrontalcatface;                //正面か";
	private CascadeClassifier detectorFrontalcatface_extended;        //正面(拡張)";
	private CascadeClassifier detectorFrontalface_alt_tree;            //正面の顔高い木";
	private CascadeClassifier detectorFrontalface_alt2;                //正面顔全体2";
	private CascadeClassifier detectorFrontalface_default;            //正面デフォルト";
	private CascadeClassifier detectorFullbody;                        //全身";
	private CascadeClassifier detectorLowerbody;                    //下半身";
	private CascadeClassifier detectorProfileface;                    //横顔";
	private CascadeClassifier detectorSmile;                        //笑顔";
	private CascadeClassifier detectorUpperbody;                    //下半身";
	private CascadeClassifier detectorRussian_plate_number;        //ナンバープレート・ロシア";
	private CascadeClassifier detectorLicence_plate_rus_16stages;    //ナンバープレートRUS";

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
	public List< pasonInfo > pasonInfoList;
	/**
	 * 書き換え終了
	 */
	private boolean isCompletion = true;

	public OCVFaceRecognitionVeiw(Context context) {
		super(context);
		final String TAG = "OCVFaceRecognitionVeiw[OCVFR]";
		String dbMsg = "class読込み";
		try {
			detectorFrontalFaceAlt = null;
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
			detectorFrontalFaceAlt = null;
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
			if ( detectorFrontalFaceAlt == null ) {
				filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
				dbMsg += "filename=" + filename;       //filename=/data/user/0/com.hijiyam_koubou.kaodori/files/haarcascades/haarcascade_frontalface_alt.xml
				File rFile = new File(filename);
				dbMsg += ";exists=" + rFile.exists();
				detectorFrontalFaceAlt = new CascadeClassifier(filename);
				dbMsg += ",正面顔全体";
				String path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_eye.xml";    //		Environment.getExternalStorageDirectory()+"/DCIM/100ANDRO/haarcascade_eye.xml";
				detectorEye = new CascadeClassifier(path);
				dbMsg += ",目";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_righteye_2splits.xml";
				detectorRighteye_2splits = new CascadeClassifier(path);
				dbMsg += ",右目";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_lefteye_2splits.xml";
				detectorLefteye_2splits = new CascadeClassifier(path);
				dbMsg += ",左目";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_eye_tree_eyeglasses.xml";
				detectorEyeglasses = new CascadeClassifier(path);
				dbMsg += ",眼鏡";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalcatface.xml";
				detectorFrontalcatface = new CascadeClassifier(path);
				dbMsg += ",正面か";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalcatface_extended.xml";
				detectorFrontalcatface_extended = new CascadeClassifier(path);
				dbMsg += ",正面(拡張)";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt_tree.xml";
				detectorFrontalface_alt_tree = new CascadeClassifier(path);
				dbMsg += ",正面の顔高い木";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt2.xml";
				detectorFrontalface_alt2 = new CascadeClassifier(path);
				dbMsg += ",正面顔全体2";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_default.xml";
				detectorFrontalface_default = new CascadeClassifier(path);
				dbMsg += ",正面デフォルト";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_fullbody.xml";
				detectorFullbody = new CascadeClassifier(path);
				dbMsg += ",全身";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_lowerbody.xml";
				detectorLowerbody = new CascadeClassifier(path);
				dbMsg += ",下半身";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_profileface.xml";
				detectorProfileface = new CascadeClassifier(path);
				dbMsg += ",横顔";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_smile.xml";
				detectorSmile = new CascadeClassifier(path);
				dbMsg += ",笑顔";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_upperbody.xml";
				detectorUpperbody = new CascadeClassifier(path);
				dbMsg += ",下半身";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_russian_plate_number.xml";
				detectorRussian_plate_number = new CascadeClassifier(path);
				dbMsg += ",ナンバープレート・ロシア";
				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_licence_plate_rus_16stages.xml";
				detectorLicence_plate_rus_16stages = new CascadeClassifier(path);
				dbMsg += ",ナンバープレートRUS";
			} else {
				dbMsg += "探知器作成済み";
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
	public List< android.graphics.Rect > readFrameRGB(Bitmap bitmap , int degrees) {        //, Camera camera      /   , int previewWidth , int previewHeight
		final String TAG = "readFrameRGB[OCVFR]";
		String dbMsg = "";
		List< android.graphics.Rect > retArray = new ArrayList();
		try {
			isCompletion = false;
			if ( detectorFrontalFaceAlt == null ) {
				dbMsg += "filename=" + filename;       //filename=/data/user/0/com.hijiyam_koubou.kaodori/files/haarcascades/haarcascade_frontalface_alt.xml
				File rFile = new File(filename);
				dbMsg += ";exists=" + rFile.exists();
				detectorFrontalFaceAlt = new CascadeClassifier(filename);
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

			dbMsg += "、viewScale(" + this.getScaleX() + "×" + this.getScaleY() + "%)";

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
			double correctionV = dAspect;
			double correctionSV = 1 / dAspect;
			if ( degrees == 90 || degrees == 270 ) {                    //270追加
				dbMsg += ";縦";
				int tmp = dWidth;                         //入れ替えてMatにしないとaspectが狂う
				dWidth = dHight;
				dHight = tmp;
				correctionH = 1 / dAspect;
				correctionV = dAspect;
				correctionSV = correctionV;
				dbMsg += ">>[" + dWidth + "×" + dHight + "]";
			} else {
				dbMsg += ";横";
				Matrix matrix = new Matrix();
				matrix.postRotate(270);                // 回転マトリックス作成（90度回転）
				bitmap = Bitmap.createBitmap(bitmap , 0 , 0 , dWidth , dHight , matrix , true);
				dbMsg += "[" + bitmap.getWidth() + "×" + bitmap.getHeight() + "]" + bitmap.getByteCount() + "バイト";
//				imageMat = new Mat(dWidth , dHight , CvType.CV_8U , new Scalar(4));     //1バイトのチャンネル0　、
			}
			imageMat = new Mat(dHight , dWidth , CvType.CV_8U , new Scalar(4));     //1バイトのチャンネル0　、
			Utils.bitmapToMat(bitmap , imageMat);                                    //openCV； 画像データを変換（BitmapのMatファイル変換
			dbMsg += ",imageMat=" + imageMat.size() + ",elemSize=" + imageMat.elemSize();
			detectorFrontalFaceAlt.detectMultiScale(imageMat , objects);                       //openCV；カスケード分類器に画像データを与え顔認識
			int detectionCount = objects.toArray().length;
			dbMsg += ",検出=" + detectionCount + "件";
			faces.clear();
			for ( org.opencv.core.Rect rect : objects.toArray() ) {
				android.graphics.Rect rRect = new android.graphics.Rect(rect.x , rect.y , rect.width , rect.height);//顔の位置（X座標）,顔の位置（Y座標）,顔の横幅,顔の縦幅     /

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
				float top = ( float ) (correctionSV * rect.y / dHight);                              //
				float right = left + ( float ) (correctionH * rect.width / dWidth);            //
				float bottom = top + ( float ) (correctionV * rect.height / dHight);            //

				faces.add(new RectF(left , top , right , bottom));
				retArray.add(rRect);
//				if(detectionCount ==1){
//					int pX = rect.x ;	//- rect.width ;
//					int pY =rect.y ;	//+  rect.height;
//					int pWidth =rect.width;	//*5 ;
//					int pHight =rect.height;	//*8;
//					org.opencv.core.Rect wRect = new org.opencv.core.Rect(pX,pY,pWidth,pHight);
//					detailedPersonFace(imageMat, wRect);
//				}
			}
			pasonInfoList=new ArrayList<pasonInfo>();
			dbMsg += ",faces=" + faces.size();
			if ( 0 == faces.size() ) {                            //顔が検出できない時は
//				faces.clear();
				faces.add(new RectF(0 , 0 , 1 , 1));            //プレビュー全体選択
			} else if ( 1 == faces.size() ) {
				int pX = ( int ) faces.get(0).left;    //- rect.width ;
				int pY = ( int ) faces.get(0).top;    //+  rect.height;
				int pWidth = ( int ) (faces.get(0).right - faces.get(0).left);    //*5 ;
				int pHight = ( int ) (faces.get(0).bottom - faces.get(0).top);    //*8;
				org.opencv.core.Rect wRect = new org.opencv.core.Rect(pX , pY , pWidth , pHight);
				pasonInfoList = detailedPersonFace(imageMat , wRect);
			}
			invalidate();            //onDrawへ
			bitmap.recycle();
			if ( imageMat != null ) {
				imageMat.release();
				imageMat = null;
				dbMsg += ",imageMat破棄";
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
	 * 検出情報リスト
	 */
	public class pasonInfo {
		String division;
		Rect area;
		String note;
	}

	/**
	 * 検出器リスト
	 */
	public class detectos {
		CascadeClassifier detector;
		String note;
	}

	public List< pasonInfo > detailedPersonFace(Mat pasonMat , org.opencv.core.Rect pRect) {
		final String TAG = "detailedPersonFace[OCVFR]";
		String dbMsg = "";
		List< pasonInfo > retArray = new ArrayList();
		try {
			dbMsg += ",pasonMat=" + pasonMat.size() + ",elemSize=" + pasonMat.elemSize();
			dbMsg += "(" + pRect.x + "," + pRect.y + ")[" + pRect.width + "×" + pRect.height + "]";
			//検索用submat切り出し
			Mat sub = new Mat();
			pasonMat.submat(pRect.y , pRect.y + pRect.height , pRect.x , pRect.x + pRect.width).copyTo(sub);
			MatOfRect moRect = new MatOfRect();

			ArrayList< detectos > detectoList = new ArrayList< detectos >();
			detectos dInfo = new detectos();
			dInfo.detector = detectorEye;
			dInfo.note = "目";
			detectoList.add(dInfo);

			dInfo = new detectos();
			dInfo.detector = detectorRighteye_2splits;
			dInfo.note = "右目";
			detectoList.add(dInfo);

			dInfo = new detectos();
			dInfo.detector = detectorLefteye_2splits;
			dInfo.note = "左目";
			detectoList.add(dInfo);


			dInfo = new detectos();
			dInfo.detector = detectorEyeglasses;
			dInfo.note = "眼鏡";
			detectoList.add(dInfo);

//			private CascadeClassifier detectorFrontalcatface;                //正面か";
//			private CascadeClassifier detectorFrontalcatface_extended;        //正面(拡張)";
//			private CascadeClassifier detectorFrontalface_alt_tree;            //正面の顔高い木";
//			private CascadeClassifier detectorFrontalface_alt2;                //正面顔全体2";
//			private CascadeClassifier detectorFrontalface_default;            //正面デフォルト";


			for ( detectos rInfo : detectoList ) {
				rInfo.detector.detectMultiScale(pasonMat , moRect);                        //目
				int detectionCount = moRect.toArray().length;
				dbMsg += ",検出=" + detectionCount + "件";
				if ( 0 < detectionCount ) {
					retArray = detailList(retArray , moRect , rInfo.note);
				}
			}
			dbMsg += ",retArray=" + retArray.size();
			if ( sub != null ) {
				sub.release();
				sub = null;
				dbMsg += "sub破棄";
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
		}
		return retArray;
	}


	public List< pasonInfo > detailList(List< pasonInfo > retArray , MatOfRect moRect , String division) {
		final String TAG = "detailList[OCVFR]";
		String dbMsg = "";
		try {
			int detectionCount = moRect.toArray().length;
			dbMsg += ",検出=" + detectionCount + "件";
			int infCount = 0;
			for ( org.opencv.core.Rect rect : moRect.toArray() ) {
				infCount++;
				pasonInfo PI = new pasonInfo();
				;
				PI.division = infCount + ")" + division + ";";
				PI.area = new Rect(rect.x , rect.y , rect.width , rect.height);//顔の位置（X座標）,顔の位置（Y座標）,顔の横幅,顔の縦幅     /
				PI.note = "(" + rect.x + "," + rect.y + ")[" + rect.width + "×" + rect.height;
				retArray.add(PI);
			}
			dbMsg += ",retArray=" + retArray.size();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
		}
		return retArray;
	}


	/**
	 *
	 *  2012/6/23			目を検出する　ついでに口・鼻も				http://nobotta.dazoo.ne.jp/blog/?p=503
	 * */
//	private void fncDetectEye(Mat mat,Mat gray, org.opencv.core.Rect Rct){
//		String cascade_eye_path=filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_eye.xml";   	//		Environment.getExternalStorageDirectory()+"/DCIM/100ANDRO/haarcascade_eye.xml";
//		CascadeClassifier cascade_eye = new CascadeClassifier();
//		cascade_eye.load(cascade_eye_path);
//
//		Mat sub = new Mat();
//		gray.submat(Rct.y, Rct.y + Rct.height, Rct.x, Rct.x + Rct.width).copyTo(sub);     //検索用submat切り出し
//		List geteyelist = new ArrayList();     //検索結果格納領域
//		cascade_eye.detectMultiScale(sub, geteyelist, 1.1, 3, Objdetect.CASCADE_SCALE_IMAGE);
//		//検索処理
//
//		for (int i=0; i < geteyelist.size(); i++){     //検索結果表示処理
//			Rect rct = geteyelist.get(i);
//			Point center = new Point(Rct.x + rct.x + rct.width / 2 ,Rct.y + rct.y + rct.height / 2);
//			int radius = rct.width / 2;
//			Core.circle(mat, center, radius, new Scalar(0,255,255), 2);
//		}
//	}


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
				paint.setStrokeWidth(2);
				if ( faces.size() < 2 ) {
					if ( pasonInfoList != null ) {
						int drX = 5;
						float objWidhtHalf = (fRight - fLeft) / 2;
						dbMsg += ",objWidhtHalf=" + objWidhtHalf;
						float objCenterY = width / 2 - (fLeft + objWidhtHalf);
						dbMsg += ",objCenterY=" + objCenterY;
						if ( 0 < objCenterY ) {
							drX = ( int ) (fRight + 5);
						}
						dbMsg += ",drX=" + drX;
						int drY = 5;
						for ( pasonInfo info : pasonInfoList ) {
							mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
							final int objColor = COLOR_CHOICES[mCurrentColorIndex];
//							paint = new Paint();
							paint.setColor(objColor);
//							paint.setStyle(Paint.Style.STROKE);
							canvas.drawText(info.division , drX , drY , paint);
							drY += 32;
							canvas.drawText(info.note , drX , drY , paint);
							drY += 32;
							fLeft = 1.0f * info.area.x;
							fTop = 1.0f * info.area.y;
							fRight = 1.0f * fLeft + info.area.width;
							fBottom = 1.0f * fTop + info.area.height;
							RectF foRect = new RectF(fLeft , fTop , fRight , fBottom);
							dbMsg += ",r(" + foRect.left + "," + foRect.top + ")～（" + foRect.right + "," + foRect.bottom + "）objColor=" + objColor;
							canvas.drawRect(foRect , paint);
						}
					} else {
						canvas.drawText(fLeft + " , " + fTop , width * face.left + 8 , height * face.top + 40 , paint);
						float sRight = fRight - 250;
						if ( fRight < sRight ) {
							sRight = fLeft + (fRight - fLeft) / 2;
						}
						canvas.drawText(fRight + " , " + fBottom , sRight , fBottom - 32 , paint);
					}
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
 * 2017年12月20日		OpenCVを使って顔を検出する					http://bkmts.xsrv.jp/opencv-face/
 * 2015年11月15日		OpenCVで顔認識など特徴ある領域の検出		https://qiita.com/yokobonbon/items/f7ff24cc449a1fe6ba4b
 * OpenCV 3.0.0			OpenCV 3.0.0 による顔検出処理				https://yamsat.wordpress.com/2015/09/13/opencv-3-0-0-%E3%81%AB%E3%82%88%E3%82%8B%E9%A1%94%E6%A4%9C%E5%87%BA%E5%87%A6%E7%90%86/
 * 2012年10月28日		[OpenCV] 顔を検出する						http://google-os.blog.jp/archives/50736832.html
 * OpenCV 2.3.1		アンドロイドでOpenCV（お顔検出				http://foonyan.sakura.ne.jp/wisteriahill/opencv_android/index.html
 * 2012年10月28日		[OpenCV] 目を検出する						http://google-os.blog.jp/archives/50736850.html
 * ファイルから読込み
 * 2016-09-13			Opencv3.1で顔検出							http://garapon.hatenablog.com/entry/2016/09/13/Opencv3.1%E3%81%A7%E9%A1%94%E6%A4%9C%E5%87%BA
 * 機械学習
 * 2016年05月23日		機械学習のためのOpenCV入門					https://qiita.com/icoxfog417/items/53e61496ad980c41a08e
 * 2012年03月01日		ARに使えるOpenCVで作る画像認識Androidアプリ	http://www.atmarkit.co.jp/ait/articles/1203/01/news159.html
 * SurfaceView の使用例
 * 2017-02-10		 	AndroidでOpenCV 3.2を使って顔検出をする		https://blogs.osdn.jp/2017/02/10/opencv.html
 * Gppgle PlayのFace API
 * 2016-01-11			Androidで顔検出APIが使えるようになった		https://www.gesource.jp/weblog/?p=7316
 * Camera.Faceクラス
 * 2014/04/12			Android で顔認識を試してみた				http://blog.kotemaru.org/2014/04/12/android-face-detection.html
 * 2012年09月26日		FaceDetectorで Bitmap から顔を検出する		https://dev.classmethod.jp/smartphone/android-tips-15-facedetector/
 * 2011年11月22日		カメラプレビューで顔検出を行う				https://techbooster.org/android/multimedia/10375/
 **/
