package com.hijiyam_koubou.kaodori;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
//import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class OCVFaceRecognitionVeiw extends View {
	static {
		System.loadLibrary("opencv_java3");  // OpenCV使用クラスに必須
	}

	private Context context;
	//	private ViewGroup VG;   					//読み込まれる土台
	private int[] rgb;
	private Bitmap bitmap;
	private Mat imageMat;
	private CascadeClassifier detectorFrontalFaceAlt;                //標準顔検出
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
	private CascadeClassifier detectorUpperbody;                    //上半身";
	private CascadeClassifier detectorLowerbody;                    //下半身";
	private CascadeClassifier detectorProfileface;                    //横顔";
	private CascadeClassifier detectorSmile;                        //笑顔";
	private CascadeClassifier detectorRussian_plate_number;        //ナンバープレート・ロシア";
	private CascadeClassifier detectorLicence_plate_rus_16stages;    //ナンバープレートRUS";

	public static SharedPreferences sharedPref;
	public SharedPreferences.Editor myEditor;
	//	public String writeFolder = "";
//	public float upScale = 1.2f;
	public long haarcascadesLastModified = 0;
	public boolean is_detector_frontal_face_alt = false;   //顔検出(標準)</string>
	public boolean is_detector_fullbody = false;                //全身
	public boolean is_detector_upperbody = false;                //上半身
	public boolean is_detector_lowerbody = false;                // 下半身
	public boolean is_detector_profileface = true;               //横顔
	public boolean is_detector_smile = false;               //笑顔
	public boolean is_detector_russian_plate_number = false;                //ナンバープレート・ロシア
	public boolean is_detector_ricence_plate_rus_16stages = false;     //ナンバープレートRUS

	public boolean is_detector_eye = false;               //目(標準)</string>
	public boolean is_detector_righteye_2splits = false;        //右目
	public boolean is_detector_lefteye_2splits = false;                //左目
	public boolean is_detector_eyeglasses = false;                //眼鏡
	public boolean is_detector_frontalcatface = false;               //正面のみ？
	public boolean is_detector_frontalcatface_extended = false;                //正面(拡張)？string>
	public boolean is_detector_frontalface_alt_tree = false;               //正面の顔高い木？
	public boolean is_detector_frontalface_alt2 = false;                //正面顔全体2
	public boolean is_detector_frontalface_default = false;                //正面デフォルト


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

	Map< String, CascadeClassifier > detectosFIles;
	Map< String, CascadeClassifier > detectosDetaileFIles;

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
		List< RectF > faces;
		ArrayList< android.graphics.Rect > andriodtArray;
	}

	public List< detectos > detectionList;            //検出リスト
	public List< pasonInfo > pasonInfoList;            //個人情報リスト
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
	public void readPref() {
		final String TAG = "readPref[OCVFR]";
		String dbMsg = "許諾済み";//////////////////
		try {
//		if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {                //(初回起動で)全パーミッションの許諾を取る
//			dbMsg = "許諾確認";
//			String[] PERMISSIONS = {Manifest.permission.CAMERA , Manifest.permission.WRITE_EXTERNAL_STORAGE , Manifest.permission.READ_EXTERNAL_STORAGE};
//			boolean isNeedParmissionReqest = false;
//			for ( String permissionName : PERMISSIONS ) {
//				dbMsg += "," + permissionName;
//				int checkResalt = checkSelfPermission(permissionName);
//				dbMsg += "=" + checkResalt;
//				if ( checkResalt != PackageManager.PERMISSION_GRANTED ) {
//					isNeedParmissionReqest = true;
//				}
//			}
//			if ( isNeedParmissionReqest ) {
//				dbMsg += "許諾処理へ";
//				requestPermissions(PERMISSIONS , REQUEST_PREF);
//				return;
//			}
//		}
//			dbMsg += ",isReadPref=" + isReadPref;
			MyPreferenceFragment prefs = new MyPreferenceFragment();
			prefs.readPref(context);

			is_detector_frontal_face_alt = prefs.is_detector_frontal_face_alt;
			dbMsg += ",顔検出(標準)=" + is_detector_frontal_face_alt;
			is_detector_profileface = prefs.is_detector_profileface;
			dbMsg += ",横顔=" + is_detector_profileface;
			is_detector_fullbody = prefs.is_detector_fullbody;
			dbMsg += ",全身=" + is_detector_fullbody;
			is_detector_upperbody = prefs.is_detector_upperbody;
			dbMsg += ",上半身=" + is_detector_upperbody;
			is_detector_lowerbody = prefs.is_detector_lowerbody;
			dbMsg += ",下半身=" + is_detector_lowerbody;
			is_detector_smile = prefs.is_detector_smile;
			dbMsg += ",笑顔=" + is_detector_smile;
			is_detector_russian_plate_number = prefs.is_detector_russian_plate_number;
			dbMsg += ",ナンバープレート・ロシア=" + is_detector_russian_plate_number;
			is_detector_ricence_plate_rus_16stages = prefs.is_detector_ricence_plate_rus_16stages;
			dbMsg += ",ナンバープレートRUS=" + is_detector_ricence_plate_rus_16stages;

			is_detector_eye = prefs.is_detector_eye;
			dbMsg += ",目(標準)=" + is_detector_eye;
			is_detector_righteye_2splits = prefs.is_detector_righteye_2splits;
			dbMsg += ",右目=" + is_detector_righteye_2splits;
			is_detector_lefteye_2splits = prefs.is_detector_lefteye_2splits;
			dbMsg += ",左目=" + is_detector_lefteye_2splits;
			is_detector_eyeglasses = prefs.is_detector_eyeglasses;
			dbMsg += ",眼鏡=" + is_detector_eyeglasses;
			is_detector_frontalcatface = prefs.is_detector_frontalcatface;
			dbMsg += ",正面のみ=" + is_detector_frontalcatface;
			is_detector_frontalcatface_extended = prefs.is_detector_frontalcatface_extended;
			dbMsg += ",正面(拡張)=" + is_detector_frontalcatface_extended;
			is_detector_frontalface_alt_tree = prefs.is_detector_frontalface_alt_tree;
			dbMsg += ",正面の顔高い木=" + is_detector_frontalface_alt_tree;
			is_detector_frontalface_alt2 = prefs.is_detector_frontalface_alt2;
			dbMsg += ",正面顔全体2=" + is_detector_frontalface_alt2;
			is_detector_frontalface_default = prefs.is_detector_frontalface_default;
			dbMsg += ",正面デフォルト=" + is_detector_frontalface_default;
//		writeFolder = prefs.write_folder;
//		dbMsg += "," + getResources().getString(R.string.write_folder) + "=" + writeFolder;
//		if ( prefs.up_scale != null ) {
//			dbMsg += ",up_scale=" + prefs.up_scale;
//			CS_Util UTIL = new CS_Util();
//			if ( UTIL.isFloatVal(prefs.up_scale) ) {
//				upScale = Float.parseFloat(prefs.up_scale);
//			} else {
//				upScale = 2.0f;
//			}
//			dbMsg += "," + getResources().getString(R.string.up_scale) + "=" + upScale;
//		}
			haarcascadesLastModified = Long.parseLong(prefs.haarcascades_last_modified);
			dbMsg += "," + getResources().getString(R.string.haarcascades_last_modified) + "=" + haarcascadesLastModified;

			sharedPref = PreferenceManager.getDefaultSharedPreferences(context);            //	getActivity().getBaseContext()
			myEditor = sharedPref.edit();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}


	public void constractCommon(Context context) {
		final String TAG = "constractCommon[OCVFR]";
		String dbMsg = "";
		try {
			this.context = context;
			readPref();
			/**基本的な検出*/
			if ( detectosFIles == null ) {
				detectosFIles = new LinkedHashMap< String, CascadeClassifier >();             //追加する順番を優先順位にdetector登録
				detectosFIles.put("haarcascade_frontalface_alt" , detectorFrontalFaceAlt);         //標準顔検出
				detectosFIles.put("haarcascade_profileface" , detectorProfileface);                       //横顔";
				detectosFIles.put("haarcascade_fullbody" , detectorFullbody);                //全身";
				detectosFIles.put("haarcascade_upperbody" , detectorUpperbody);                       //上半身";
				detectosFIles.put("haarcascade_lowerbody" , detectorLowerbody);                       //下半身";
				detectosFIles.put("haarcascade_smile.xml" , detectorSmile);                                       //笑顔";
				detectosFIles.put("haarcascade_frontalcatface" , detectorFrontalcatface);         //正面か";
				detectosFIles.put("haarcascade_frontalcatface_extended" , detectorFrontalcatface_extended);        //正面(拡張)";
				detectosFIles.put("haarcascade_frontalface_alt_tree" , detectorFrontalface_alt_tree);       //正面の顔高い木";
				detectosFIles.put("haarcascade_frontalface_alt2" , detectorFrontalface_alt2);      //正面顔全体2";
				detectosFIles.put("haarcascade_frontalface_default" , detectorFrontalface_default);   //正面デフォルト";
				detectosFIles.put("haarcascade_russian_plate_number" , detectorRussian_plate_number);         //ナンバープレート・ロシア";
				detectosFIles.put("haarcascade_licence_plate_rus_16stages" , detectorLicence_plate_rus_16stages);         //ナンバープレートRUS";

//				String[] assetFilees =context.getResources().getAssets().list("haarcascades");      //  /assets/
////				String[] assetFilees = context.getApplicationContext().getAssets().list("/assets/");
//				File rFile = new File(assetFilees[0]);    //haarcascade_eye が拾える
//				String passwRoot = context.getFilesDir().getAbsolutePath() + "/assets";             //.getParentFile()=getParent=null  ,getAbsolutePath=getName=getPath=haarcascade_eye.xml
//				dbMsg += ",passwRoot=" + passwRoot;     //getCanonicalPath=/haarcascade_eye.xml,   getParentFile().getPath()=null
				int setCount = 0;
				for ( Map.Entry< String, CascadeClassifier > entry : detectosFIles.entrySet() ) {
					String rDetectorFile = entry.getKey();
					CascadeClassifier cfs = entry.getValue();
					dbMsg += "\n(" + setCount + ")";
					String path = context.getFilesDir().getAbsolutePath() + "/haarcascades/" + rDetectorFile + ".xml";    //Aprcationがインストールされているフォルダの場合
					dbMsg += path;
					File rFile = new File(path);
					dbMsg += " ,exists=" + rFile.exists();
					if ( rFile.exists() ) {
						cfs = new CascadeClassifier(path);
						dbMsg += "=" + cfs;
						if ( rDetectorFile.equals("haarcascade_frontalface_alt_tree") ) {                 //正面の顔高い木";
							detectorFrontalface_alt_tree = new CascadeClassifier(path);
							dbMsg += "=" + detectorFrontalface_alt_tree;
						} else if ( rDetectorFile.equals("haarcascade_frontalface_alt2") ) {                //正面顔全体2";
							detectorFrontalface_alt2 = new CascadeClassifier(path);
							dbMsg += "=" + detectorFrontalface_alt2;
						} else if ( rDetectorFile.equals("haarcascade_frontalface_alt") ) {      //標準顔検出
							detectorFrontalFaceAlt = cfs;            // new CascadeClassifier(path);
//							dbMsg += "=" + detectorFrontalFaceAlt;
						} else if ( rDetectorFile.equals("haarcascade_profileface") ) {                         //横顔";
							detectorProfileface = new CascadeClassifier(path);
							dbMsg += "=" + detectorProfileface;
						} else if ( rDetectorFile.equals("haarcascade_fullbody") ) {                       //全身";
							detectorFullbody = new CascadeClassifier(path);
							dbMsg += "=" + detectorFullbody;
						} else if ( rDetectorFile.equals("haarcascade_upperbody") ) {                            //上半身";
							detectorUpperbody = new CascadeClassifier(path);
							dbMsg += "=" + detectorUpperbody;
						} else if ( rDetectorFile.equals("haarcascade_lowerbody") ) {                           //下半身";
							detectorLowerbody = new CascadeClassifier(path);
							dbMsg += "=" + detectorLowerbody;
						} else if ( rDetectorFile.equals("haarcascade_smile") ) {                                             //笑顔";
							detectorSmile = new CascadeClassifier(path);
							dbMsg += "=" + detectorSmile;
						} else if ( rDetectorFile.equals("haarcascade_frontalcatface") ) {                //正面か";
							detectorFrontalcatface = new CascadeClassifier(path);
							dbMsg += "=" + detectorFrontalcatface;
						} else if ( rDetectorFile.equals("haarcascade_frontalcatface_extended") ) {                 //正面(拡張)";
							detectorFrontalcatface_extended = new CascadeClassifier(path);
							dbMsg += "=" + detectorFrontalcatface_extended;
						} else if ( rDetectorFile.equals("haarcascade_frontalface_default") ) {             //正面デフォルト";
							detectorFrontalface_default = new CascadeClassifier(path);
							dbMsg += "=" + detectorFrontalface_default;
						} else if ( rDetectorFile.equals("haarcascade_russian_plate_number") ) {               //ナンバープレート・ロシア";
							detectorRussian_plate_number = new CascadeClassifier(path);
							dbMsg += "=" + detectorRussian_plate_number;
						} else if ( rDetectorFile.equals("haarcascade_licence_plate_rus_16stages") ) {              //ナンバープレートRUS";
							detectorLicence_plate_rus_16stages = new CascadeClassifier(path);
							dbMsg += "=" + detectorLicence_plate_rus_16stages;
						}
					} else {
						dbMsg += "," + rDetectorFile + "読み込めず";
						if ( rDetectorFile.equals("haarcascade_frontalface_alt_tree") ) {                 //正面の顔高い木";
							is_detector_frontalface_alt_tree = false;
						} else if ( rDetectorFile.equals("haarcascade_frontalface_alt2") ) {                //正面顔全体2";
							is_detector_frontalface_alt2 = false;
						} else if ( rDetectorFile.equals("haarcascade_frontalface_alt") ) {      //標準顔検出
							is_detector_frontal_face_alt = false;
						} else if ( rDetectorFile.equals("haarcascade_profileface") ) {                         //横顔";
							is_detector_profileface = false;
						} else if ( rDetectorFile.equals("haarcascade_fullbody") ) {                       //全身";
							is_detector_fullbody = false;
						} else if ( rDetectorFile.equals("haarcascade_upperbody") ) {                            //上半身";
							is_detector_upperbody = false;
						} else if ( rDetectorFile.equals("haarcascade_lowerbody") ) {                           //下半身";
							is_detector_lowerbody = false;
						} else if ( rDetectorFile.equals("haarcascade_smile") ) {                                             //笑顔";
							is_detector_smile = false;
						} else if ( rDetectorFile.equals("haarcascade_frontalcatface") ) {                //正面か";
							is_detector_frontalcatface = false;
						} else if ( rDetectorFile.equals("haarcascade_frontalcatface_extended") ) {                 //正面(拡張)";
							is_detector_frontalcatface_extended = false;
						} else if ( rDetectorFile.equals("haarcascade_frontalface_default") ) {             //正面デフォルト";
							is_detector_frontalface_default = false;
						} else if ( rDetectorFile.equals("haarcascade_russian_plate_number") ) {               //ナンバープレート・ロシア";
							is_detector_russian_plate_number = false;
						} else if ( rDetectorFile.equals("haarcascade_licence_plate_rus_16stages") ) {              //ナンバープレートRUS";
							is_detector_ricence_plate_rus_16stages = false;
						}
					}
					setCount++;
				}

				/**部分など詳細な検出*/
				detectosDetaileFIles = new LinkedHashMap< String, CascadeClassifier >();     //String, CascadeClassifierでCascadeClassifierは引き渡せない
				detectosDetaileFIles.put("haarcascade_eye" , detectorEye);       //目
				detectosDetaileFIles.put("haarcascade_righteye_2splits" , detectorRighteye_2splits);      //右目";
				detectosDetaileFIles.put("haarcascade_lefteye_2splits" , detectorLefteye_2splits);         //左目";
				detectosDetaileFIles.put("haarcascade_eye_tree_eyeglasses" , detectorEyeglasses);         //眼鏡";
				setCount = 0;
				for ( Map.Entry< String, CascadeClassifier > entry : detectosDetaileFIles.entrySet() ) {
					String rDetectorFile = entry.getKey();
					CascadeClassifier cfs = entry.getValue();
					dbMsg += "\n(" + setCount + ")";
					String path = context.getFilesDir().getAbsolutePath() + "/haarcascades/" + rDetectorFile + ".xml";    //Aprcationがインストールされているフォルダの場合
//					String path = context.getApplicationContext().getAssets() + rDetectorFile;//						String path = context.getFilesDir().getAbsolutePath() + "/" + rDetectorFile;
					dbMsg += path;
					File rFile = new File(path);
					dbMsg += " ,exists=" + rFile.exists();
					if ( rFile.exists() ) {
//						cfs = new CascadeClassifier(path);
//						dbMsg += "=" + cfs;
						if ( rDetectorFile.equals("haarcascade_eye_tree_eyeglasses") ) {          //眼鏡";
							detectorEyeglasses = new CascadeClassifier(path);
							dbMsg += "=" + detectorEyeglasses;
						} else if ( rDetectorFile.equals("haarcascade_eye") ) {                         //目
							detectorEye = new CascadeClassifier(path);
							dbMsg += "=" + detectorEye;
						} else if ( rDetectorFile.equals("haarcascade_righteye_2splits") ) {            //右目";
							detectorRighteye_2splits = new CascadeClassifier(path);
							dbMsg += "=" + detectorRighteye_2splits;
						} else if ( rDetectorFile.equals("haarcascade_lefteye_2splits") ) {            //左目";
							detectorLefteye_2splits = new CascadeClassifier(path);
							dbMsg += "=" + detectorLefteye_2splits;
						}
					} else {
						dbMsg += "," + rDetectorFile + "読み込めず";
						if ( rDetectorFile.equals("haarcascade_eye") ) {                         //目
							is_detector_eye = false;
						} else if ( rDetectorFile.equals("haarcascade_righteye_2splits") ) {            //右目";
							is_detector_righteye_2splits = false;
						} else if ( rDetectorFile.equals("haarcascade_lefteye_2splits") ) {            //左目";
							is_detector_lefteye_2splits = false;
						} else if ( rDetectorFile.equals("haarcascade_eye_tree_eyeglasses") ) {          //眼鏡";
							is_detector_eyeglasses = false;
						}
					}
					setCount++;
				}
//				filename = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
//				dbMsg += "filename=" + filename;       //filename=/data/user/0/com.hijiyam_koubou.kaodori/files/haarcascades/haarcascade_frontalface_alt.xml
//				File rFile = new File(filename);
//				dbMsg += ";exists=" + rFile.exists();
//				detectorFrontalFaceAlt = new CascadeClassifier(filename);
//				dbMsg += ",正面顔全体";
//				String path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_eye.xml";    //		Environment.getExternalStorageDirectory()+"/DCIM/100ANDRO/haarcascade_eye.xml";
//				detectorEye = new CascadeClassifier(path);
//				dbMsg += ",目";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_righteye_2splits.xml";
//				detectorRighteye_2splits = new CascadeClassifier(path);
//				dbMsg += ",右目";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_lefteye_2splits.xml";
//				detectorLefteye_2splits = new CascadeClassifier(path);
//				dbMsg += ",左目";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_eye_tree_eyeglasses.xml";
//				detectorEyeglasses = new CascadeClassifier(path);
//				dbMsg += ",眼鏡";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalcatface.xml";
//				detectorFrontalcatface = new CascadeClassifier(path);
//				dbMsg += ",正面か";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalcatface_extended.xml";
//				detectorFrontalcatface_extended = new CascadeClassifier(path);
//				dbMsg += ",正面(拡張)";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt_tree.xml";
//				detectorFrontalface_alt_tree = new CascadeClassifier(path);
//				dbMsg += ",正面の顔高い木";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt2.xml";
//				detectorFrontalface_alt2 = new CascadeClassifier(path);
//				dbMsg += ",正面顔全体2";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_default.xml";
//				detectorFrontalface_default = new CascadeClassifier(path);
//				dbMsg += ",正面デフォルト";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_fullbody.xml";
//				detectorFullbody = new CascadeClassifier(path);
//				dbMsg += ",全身";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_lowerbody.xml";
//				detectorLowerbody = new CascadeClassifier(path);
//				dbMsg += ",下半身";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_profileface.xml";
//				detectorProfileface = new CascadeClassifier(path);
//				dbMsg += ",横顔";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_smile.xml";
//				detectorSmile = new CascadeClassifier(path);
//				dbMsg += ",笑顔";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_upperbody.xml";
//				detectorUpperbody = new CascadeClassifier(path);
//				dbMsg += ",下半身";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_russian_plate_number.xml";
//				detectorRussian_plate_number = new CascadeClassifier(path);
//				dbMsg += ",ナンバープレート・ロシア";
//				path = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_licence_plate_rus_16stages.xml";
//				detectorLicence_plate_rus_16stages = new CascadeClassifier(path);
//				dbMsg += ",ナンバープレートRUS";
			} else {
				dbMsg += "探知器作成済み";
			}
			isCompletion = true;
			// 			setCondition();     この時点ではレイアウトが拾えない
			faces.clear();
			faces.add(new RectF(0 , 0 , 1 , 1));            //APIL1
			invalidate();                                                //onDrawへ
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生でエラー発生；" + er);
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
			if ( objects == null ) {
				objects = new MatOfRect();
				dbMsg += ";object作成";
			}
			if ( viewAspect == null ) {
				setCondition();
			}
			detectionList = new ArrayList< detectos >();
			for ( Map.Entry< String, CascadeClassifier > entry : detectosFIles.entrySet() ) {
				String rDetectorFile = entry.getKey();
				CascadeClassifier cfs = entry.getValue();
				detectos dInfo = new detectos();
				dInfo.note = "";
				if ( rDetectorFile.equals("haarcascade_frontalface_alt_tree") && is_detector_frontalface_alt_tree ) {                 //正面の顔高い木";
					dInfo.note = getResources().getString(R.string.mm_detector_frontalface_alt_tree);                    //正面の顔高い木？
					dInfo.detector = detectorFrontalface_alt_tree;
				} else if ( rDetectorFile.equals("haarcascade_frontalface_alt2") && is_detector_frontalface_alt2 ) {                //正面顔全体2";
					dInfo.note = getResources().getString(R.string.mm_detector_frontalface_alt2);
					dInfo.detector = detectorFrontalface_alt2;
				} else if ( rDetectorFile.equals("haarcascade_frontalface_alt") && is_detector_frontal_face_alt ) {      //標準顔検出
					dInfo.note = getResources().getString(R.string.mm_detector_frontal_face_alt);
					dInfo.detector = detectorFrontalFaceAlt;
				} else if ( rDetectorFile.equals("haarcascade_profileface") && is_detector_profileface ) {                         //横顔";
					dInfo.note = getResources().getString(R.string.mm_detector_profileface);
					dInfo.detector = detectorProfileface;
				} else if ( rDetectorFile.equals("haarcascade_fullbody") && is_detector_fullbody ) {                       //全身";
					dInfo.note = getResources().getString(R.string.mm_detector_fullbody);
					dInfo.detector = detectorFullbody;
				} else if ( rDetectorFile.equals("haarcascade_upperbody") && is_detector_upperbody ) {                            //上半身";
					dInfo.note = getResources().getString(R.string.mm_detector_upperbody);
					dInfo.detector = detectorUpperbody;
				} else if ( rDetectorFile.equals("haarcascade_lowerbody") && is_detector_lowerbody ) {                           //下半身";
					dInfo.note = getResources().getString(R.string.mm_detector_lowerbody);
					dInfo.detector = detectorLowerbody;
				} else if ( rDetectorFile.equals("haarcascade_smile") && is_detector_smile ) {                                             //笑顔";
					dInfo.note = getResources().getString(R.string.mm_detector_smile);
					dInfo.detector = detectorSmile;
				} else if ( rDetectorFile.equals("haarcascade_frontalcatface") && is_detector_frontalcatface ) {                //正面か";
					dInfo.note = getResources().getString(R.string.mm_detector_frontalcatface);
					dInfo.detector = detectorFrontalcatface;
				} else if ( rDetectorFile.equals("haarcascade_frontalcatface_extended") && is_detector_frontalcatface_extended ) {                 //正面(拡張)";
					dInfo.note = getResources().getString(R.string.mm_detector_frontalcatface_extended);
					dInfo.detector = detectorFrontalcatface_extended;
				} else if ( rDetectorFile.equals("haarcascade_frontalface_default") && is_detector_frontalface_default ) {             //正面デフォルト";
					dInfo.note = getResources().getString(R.string.mm_detector_frontalface_default);                    //正面デフォルト
					dInfo.detector = detectorFrontalface_default;
				} else if ( rDetectorFile.equals("haarcascade_russian_plate_number") && is_detector_russian_plate_number ) {               //ナンバープレート・ロシア";
					dInfo.note = getResources().getString(R.string.mm_detector_russian_plate_number);
					dInfo.detector = detectorRussian_plate_number;
				} else if ( rDetectorFile.equals("haarcascade_licence_plate_rus_16stages") && is_detector_ricence_plate_rus_16stages ) {              //ナンバープレートRUS";
					dInfo.note = getResources().getString(R.string.mm_detector_ricence_plate_rus_16stages);
					dInfo.detector = detectorLicence_plate_rus_16stages;
				}
				if ( !dInfo.note.equals("") && dInfo.detector != null ) {        //検出対象確定    /
					dbMsg += "\n" + rDetectorFile + "=" + dInfo.note + "=" + dInfo.detector;
					dInfo.andriodtArray = new ArrayList< android.graphics.Rect >();
					dInfo.faces = new ArrayList< RectF >();
					detectionList.add(dInfo);
				}
			}
			int dlistSize = detectionList.size();
			dbMsg += ">detect>" + dlistSize + "通り";
			if ( dlistSize == 0 ) {
				return null;
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
			double correctionSH = 1 / dAspect;                        //x方向シフト
			double correctionSV = 1 / dAspect;                        //ｙ方向シフト
			if ( degrees == 90 || degrees == 270 ) {                    //270追加<
				dbMsg += ";縦";
				int tmp = dWidth;                         //入れ替えてMatにしないとaspectが狂う
				dWidth = dHight;
				dHight = tmp;
				correctionH = 1 / dAspect;
				correctionV = dAspect;
				correctionSH = 1.0;
				correctionSV = correctionV;
				dbMsg += ">>[" + dWidth + "×" + dHight + "]";
			} else {
				dbMsg += ";横";
				Matrix matrix = new Matrix();
				matrix.postRotate(270);                // <90で動作せず；回転マトリックス作成
				bitmap = Bitmap.createBitmap(bitmap , 0 , 0 , dWidth , dHight , matrix , true);
				dbMsg += "[" + bitmap.getWidth() + "×" + bitmap.getHeight() + "]" + bitmap.getByteCount() + "バイト";
//				imageMat = new Mat(dWidth , dHight , CvType.CV_8U , new Scalar(4));     //1バイトのチャンネル0　、
			}
			imageMat = new Mat(dHight , dWidth , CvType.CV_8U , new Scalar(4));     //1バイトのチャンネル0　、
			Utils.bitmapToMat(bitmap , imageMat);                                    //openCV； 画像データを変換（BitmapのMatファイル変換
			dbMsg += ",imageMat=" + imageMat.size() + ",elemSize=" + imageMat.elemSize();
			dbMsg += ",最終位置情報[" + dWidth + "×" + dHight + "]" + "、H;" + correctionH + "、V" + correctionV + ",SH=" + correctionSH + "、SV " + correctionSV;


			Map< String, org.opencv.core.Rect > facesList = new LinkedHashMap< String, org.opencv.core.Rect >();
			for ( detectos tInfo : detectionList ) {
				dbMsg += "," + tInfo.note;
				tInfo.detector.detectMultiScale(imageMat , objects);
				if ( objects != null ) {
					int detectionCount = objects.toArray().length;
					dbMsg += ";=" + detectionCount + "件検出";
					if ( 0 < detectionCount ) {
						int oCount =0;
						for ( org.opencv.core.Rect rect : objects.toArray() ) {
							oCount++;
							dbMsg += "("+oCount+"(" + rect.x + "," + rect.y + ")[" + rect.width + "×" + rect.height + "]";
							//  new org.opencv.core.Rect( rect.x , rect.y, rect.width, rect.height)
							facesList.put(tInfo.note+oCount , rect);   //keyにする側はユニーク名が必要
						}
					}
				} else {
					dbMsg += ";=null";
				}
			}
			int facesSize = facesList.size();
			dbMsg += ",検出合計=" + facesSize + "件";
			faces.clear();
			if ( 0 == facesSize ) {                            //顔が検出できない時は
				faces.add(new RectF(0 , 0 , 1 , 1));            //プレビュー全体選択に戻す
			} else {
				facesList = deleteOverlapp(facesList);
				facesSize = facesList.size();
				dbMsg += ">重複確認後>=" + facesSize + "件";
			}

			List< detectos > rInfo = makedetectionList(facesList , dWidth , dHight , correctionH , correctionV , correctionSH , correctionSV);
			for ( detectos tInfo : rInfo ) {
				dbMsg += "(" + tInfo.note + ")" + tInfo.faces.size() + "件";
				faces.addAll(tInfo.faces);
				retArray.addAll(tInfo.andriodtArray);
			}
			if ( 1 == facesSize ) {
				int pX = ( int ) faces.get(0).left;    //- rect.width ;
				int pY = ( int ) faces.get(0).top;    //+  rect.height;
				int pWidth = ( int ) (faces.get(0).right - faces.get(0).left);    //*5 ;
				int pHight = ( int ) (faces.get(0).bottom - faces.get(0).top);    //*8;
				org.opencv.core.Rect wRect = new org.opencv.core.Rect(pX , pY , pWidth , pHight);
				pasonInfoList = new ArrayList< pasonInfo >();
				pasonInfoList = detailedPersonFace(imageMat , wRect);
				retArray.add(new android.graphics.Rect(pX , pY , pX + pWidth , pY + pHight));

			}
			invalidate();            //onDrawへ

			bitmap.recycle();
			if ( imageMat != null ) {
				imageMat.release();
				imageMat = null;
				dbMsg += ",imageMat破棄";
			}
			if ( objects != null ) {
				objects.release();
				objects = null;
				dbMsg += ";object破棄";
			}

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
		}
		return retArray;
	}

	/**
	 * 重複領域の削除
	 */
	public Map< String, org.opencv.core.Rect > deleteOverlapp(Map< String, org.opencv.core.Rect > retArray) {
		final String TAG = "deleteOverlapp[OCVFR]";
		String dbMsg = "";
		try {
			int facesSize = retArray.size();
			dbMsg += ",合計=" + facesSize + "件";
			List<String> removeKeys =new ArrayList<String>();
			for ( Iterator< Map.Entry< String, org.opencv.core.Rect > > iterator = retArray.entrySet().iterator() ; iterator.hasNext() ; ) {
				Map.Entry< String, org.opencv.core.Rect > entry = iterator.next();
				String faceName = entry.getKey();
				dbMsg += "\n" + faceName;
				org.opencv.core.Rect face = entry.getValue();//retArray.get(fCount).;
				int faceLeft = face.x;
				int faceTop = face.y;
				int facerRight = faceLeft + face.width;
				int faceBottom = faceTop + face.height;
				dbMsg +=  "(" + faceLeft + "," + faceTop + ")～（" + facerRight + "," + faceBottom + "）と";
				for(Map.Entry<String, org.opencv.core.Rect> cEntry: retArray.entrySet()) {
//					Map.Entry< String, org.opencv.core.Rect > cEntry = iterator.next();
//					dCount++;
					String comparName = cEntry.getKey();
					if(! faceName.equals(comparName)) {
						org.opencv.core.Rect comparR = cEntry.getValue();//retArray.get(dCount);
						int comparLeft = comparR.x;
						int comparTop = comparR.y;
						int comparRight = comparLeft + comparR.width;
						int comparBottom = comparTop + comparR.height;
						int delPatarn = 0;
						if ( ((faceLeft <= comparLeft && comparLeft <= facerRight) && (faceTop <= comparBottom && comparBottom <= faceBottom)) ) {
							delPatarn += 1;
							dbMsg += ",左下";
						}
						if ( ((faceLeft <= comparLeft && comparLeft <= facerRight) && (faceTop <= comparTop && comparTop <= faceBottom)) ) {
							delPatarn += 2;
							dbMsg += ",左上";
						}
						if ( ((faceLeft <= comparRight && comparRight <= facerRight) && (faceTop <= comparBottom && comparBottom <= faceBottom)) ) {
							delPatarn += 4;
							dbMsg += ",右下";
						}
						if ( ((faceLeft <= comparRight && comparRight <= facerRight) && (faceTop <= comparTop && comparTop <= faceBottom)) ) {
							delPatarn += 8;
							dbMsg += ",右上";
						}
//						if ( ((comparLeft <= faceLeft && faceLeft <= comparRight) && (comparTop <= faceTop && faceTop <= comparBottom)) && ((comparLeft <= faceLeft && faceLeft <= comparRight) && (comparTop <= faceBottom && faceBottom <= comparBottom)) ) {
//							delPatarn += 16;
//							dbMsg += ",左内側";
//						}
//						if ( ((comparLeft <= facerRight && facerRight <= comparRight) && (comparTop <= faceTop && faceTop <= comparBottom)) && ((comparLeft <= facerRight && facerRight <= comparRight) && (comparTop <= faceBottom && faceBottom <= comparBottom)) ) {
//							delPatarn += 32;
//							dbMsg += ",右内側";
//						}
//						if ( ((comparLeft <= faceLeft && faceLeft <= comparRight) && (comparTop <= faceTop && faceTop <= comparBottom)) && ((comparLeft <= facerRight && facerRight <= comparRight) && (comparTop <= faceTop && faceTop <= comparBottom)) ) {
//							delPatarn += 64;
//							dbMsg += ",上内側";            //top が　faceTop～ <faceBottom　の中   でcomparBottom が　faceTop～ <faceBottom　の中
//						}
//						if ( ((comparLeft <= faceLeft && faceLeft <= comparRight) && (comparTop <= faceTop && faceTop <= comparBottom)) && ((comparLeft <= facerRight && facerRight <= comparRight) && (comparTop <= faceTop && faceTop <= comparBottom)) ) {
//							delPatarn += 128;
//							dbMsg += ",下内側";            //top が　faceTop～ <faceBottom　の中   でcomparBottom が　faceTop～ <faceBottom　の中
//						}
//						if ( ((comparLeft <= faceLeft && faceLeft <= comparRight) && (comparTop <= faceBottom && faceBottom <= comparBottom)) && ((comparLeft <= facerRight && facerRight <= comparRight) && (comparTop <= faceBottom && faceBottom <= comparBottom)) ) {
//							delPatarn += 256;
//							dbMsg += ",全点内側";
//						}

						if ( 0 < delPatarn ) {
							dbMsg += "で" + comparName+ "(" + comparLeft + "," + comparTop + ")～（" + comparRight + "," + comparBottom + "）delPatarn=" + delPatarn;
							removeKeys.add(comparName);
						}
					}
				}
			}
			dbMsg += ",removeKeys=" + removeKeys.size() + "件";
			for(String dalName: removeKeys) {
				dbMsg += ",=" + dalName;
				retArray.remove(dalName);
			}
				facesSize = retArray.size();
			dbMsg += ">重複確認後>=" + facesSize + "件";
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
		}
		return retArray;
	}

	/**
	 * 抽出結果リスト（moRect）からRectを抽出
	 * ①ディスプレイサイズ上の比率に変換したfloatの(left,top)～(right,bottom)に変換。
	 * ②そのままの座標を andriodtArrayに転記
	 * detectosクラスに格納して返す。
	 * @param facesList    MatOfRect（rectのリスト）を含む渡されるデータクラス
	 * @param dWidth       ディスプレイの幅
	 * @param dHight       ディスプレイの高さ
	 * @param correctionH  leftとwidthに積算する補正値
	 * @param correctionV  bottomに積算する補正値
	 * @param correctionSV topに積算する補正値
	 */
	public List< detectos > makedetectionList(Map< String, org.opencv.core.Rect > facesList , int dWidth , int dHight , double correctionH , double correctionV , double correctionSH , double correctionSV) {
		final String TAG = "makedetectionList[OCVFR]";           // , MatOfRect moRect
		String dbMsg = "";
		List< detectos > retDetectos = new ArrayList< detectos >();
		try {
			dbMsg += "disp[" + dWidth + "×" + dHight + "]moRect=" + facesList.size() + "件";
			for ( Map.Entry< String, org.opencv.core.Rect > entry : facesList.entrySet() ) {
				detectos rDetecto = new detectos();
				rDetecto.andriodtArray = new ArrayList< android.graphics.Rect >();
				rDetecto.faces = new ArrayList< RectF >();
				String detectorName = entry.getKey();
				dbMsg += "；" + detectorName;
				rDetecto.note = detectorName;
				org.opencv.core.Rect rect = entry.getValue();
//			for ( org.opencv.core.Rect rect : moRect.toArray() ) {
				dbMsg += "(" + rect.x + "," + rect.y + ")[" + rect.width + "×" + rect.height + "]";
				int rectX = ( int ) (correctionH * rect.x);
				int rectY = ( int ) (correctionSV * rect.y);
				int rectWidth = ( int ) (correctionH * rect.width);
				int rectHeight = ( int ) (correctionV * rect.height);
				dbMsg += "補正(" + rectX + "," + rectY + ")[" + rectWidth + "×" + rectHeight + "]";
				android.graphics.Rect rRect = new android.graphics.Rect(rectX , rectY , rectWidth , rectHeight);//顔の位置（X座標）,顔の位置（Y座標）,顔の横幅,顔の縦幅     /
				rDetecto.andriodtArray.add(rRect);
				dbMsg += rDetecto.andriodtArray.size() + "エリア";

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
				float left = ( float ) (1.0 * rectX / dWidth);                             //  (correctionH * rectX/ dWidth);
				float top = ( float ) (1.0 * rectY / dHight);                              // (correctionSV *rectY/ dHight);
				float right = left + ( float ) (1.0 * rectWidth / dWidth);            //(correctionH * rectWidth / dWidth)
				float bottom = top + ( float ) (1.0 * rectHeight / dHight);            // (correctionV * rectHeight / dHight);
				dbMsg += ";画面比率(" + left + "," + top + ")～(" + right + "×" + bottom + ")";
				rDetecto.faces.add(new RectF(left , top , right , bottom));
				retDetectos.add(rDetecto);
				dbMsg += retDetectos.size() + "件目";
//				if(detectionCount ==1){
//					int pX = rect.x ;	//- rect.width ;
//					int pY =rect.y ;	//+  rect.height;
//					int pWidth =rect.width;	//*5 ;
//					int pHight =rect.height;	//*8;
//					org.opencv.core.Rect wRect = new org.opencv.core.Rect(pX,pY,pWidth,pHight);
//					detailedPersonFace(imageMat, wRect);
//				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			isCompletion = true;
		}
		return retDetectos;
	}


	public List< pasonInfo > detailedPersonFace(Mat pasonMat , org.opencv.core.Rect pRect) {
		final String TAG = "detailedPersonFace[OCVFR]";
		String dbMsg = "";
		List< pasonInfo > retArray = new ArrayList();
		try {
			dbMsg += ",pasonMat=" + pasonMat.size() + ",elemSize=" + pasonMat.elemSize();
			dbMsg += "(" + pRect.x + "," + pRect.y + ")[" + pRect.width + "×" + pRect.height + "]";
//			//検索用submat切り出し
//			Mat sub = new Mat();
//			pasonMat.submat(pRect.y , pRect.y + pRect.height , pRect.x , pRect.x + pRect.width).copyTo(sub);

			ArrayList< detectos > detectoList = new ArrayList< detectos >();
			for ( Map.Entry< String, CascadeClassifier > entry : detectosDetaileFIles.entrySet() ) {
				String rDetectorFile = entry.getKey();
				CascadeClassifier cfs = entry.getValue();
				detectos dInfo = new detectos();
				dInfo.note = "";

				if ( rDetectorFile.equals("haarcascade_eye_tree_eyeglasses") && is_detector_eyeglasses ) {          //眼鏡";
					dInfo.note = getResources().getString(R.string.mm_detector_eyeglasses);
					dInfo.detector = detectorEyeglasses;
				} else if ( rDetectorFile.equals("haarcascade_eye") && is_detector_eye ) {                         //目
					dInfo.note = getResources().getString(R.string.mm_detector_eye);
					dInfo.detector = detectorEye;
				} else if ( rDetectorFile.equals("haarcascade_righteye_2splits") && is_detector_righteye_2splits ) {            //右目";
					dInfo.note = getResources().getString(R.string.mm_detector_righteye_2splits);
					dInfo.detector = detectorRighteye_2splits;
				} else if ( rDetectorFile.equals("haarcascade_lefteye_2splits") && is_detector_lefteye_2splits ) {            //左目";
					dInfo.note = getResources().getString(R.string.mm_detector_lefteye_2splits);
					dInfo.detector = detectorLefteye_2splits;
				}
				if ( !dInfo.note.equals("") && dInfo.detector != null ) {
					dbMsg += "\n" + rDetectorFile + "=" + dInfo.note + "=" + dInfo.detector;
					dInfo.andriodtArray = new ArrayList< android.graphics.Rect >();
					dInfo.faces = new ArrayList< RectF >();
					detectoList.add(dInfo);
				}
			}
			int dlistSize = detectoList.size();
			dbMsg += ">detectionList>" + dlistSize + "項目";
			if ( dlistSize == 0 ) {
				return null;
			}

			Map< String, org.opencv.core.Rect > facesList = new LinkedHashMap< String, org.opencv.core.Rect >();

			MatOfRect moRect = new MatOfRect();
			for ( detectos rInfo : detectoList ) {//detectoList       /
				rInfo.detector.detectMultiScale(pasonMat , moRect);
				if ( moRect != null ) {
					int detectionCount = moRect.toArray().length;
					dbMsg += "(" + retArray.size() + ")" + rInfo.note + "=" + detectionCount + "件";
					if ( 0 < detectionCount ) {

						//重複


						retArray = detailList(retArray , moRect , rInfo.note);
					}
				} else {
					dbMsg += ";=null";
				}
			}
			dbMsg += ",retArray=" + retArray.size() + "個所";

//			int facesSize = retArray.size();
//			if ( 0 == facesSize ) {                            //顔が検出できない時は
//			} else {
//				retArray = deleteOverlapp(retArray);
//			}


			if ( moRect != null ) {
				moRect.release();
				moRect = null;
				dbMsg += "、moRect破棄";
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
						if ( fLeft == 0 && fTop == 0 ) {
							canvas.drawText("検出できない時は検出対象を変えてお試しください。" , width * face.left + 8 , height * face.top + 40 , paint);
						} else {
							canvas.drawText(fLeft + " , " + fTop , width * face.left + 8 , height * face.top + 40 , paint);
						}
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
			if ( layoutParams != null ) {

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
			}

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
 * 2012/6/23			目を検出する　ついでに口・鼻も				http://nobotta.dazoo.ne.jp/blog/?p=503
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
