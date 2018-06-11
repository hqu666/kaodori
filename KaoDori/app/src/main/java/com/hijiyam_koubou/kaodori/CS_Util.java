package com.hijiyam_koubou.kaodori;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Surface;


public class CS_Util {
	Context context;

//	public CS_Util(Context context){
//		context = context;
//	}

	public String  getAplPathName(Context context) {
		final String TAG = "getAplPathName[util]";
		String dbMsg = "開始";
		String local_dir="";
		try {
			java.io.File wrDir = context.getFilesDir();//自分のアプリ用の内部ディレクトリ
			String wrDirName = wrDir.getPath();
			dbMsg += ",wrDir=" + wrDirName;            //wrDir=/data/user/0/com.example.hkuwayama.nuloger/files
			java.io.File  file = new java.io.File(wrDir, wrDirName);
			local_dir = wrDir.getAbsolutePath();
			dbMsg = dbMsg + ",local_dir=" +local_dir;
			myLog(TAG, dbMsg);
		} catch (Exception er) {
			Log.e(TAG, dbMsg + ";でエラー発生；" + er);
		}
		return local_dir;
	}

	public int getNowFileCount(Context context) {
		final String TAG = "getNowFileCount[util]";
		String dbMsg = "開始";
		int nowCount = 0;
		try {
			java.io.File[] files;
			String local_dir = getAplPathName( context);
			files = new java.io.File(local_dir).listFiles();
			if ( files != null ) {
				int fCount = files.length;
				for ( int i = 0 ; i < fCount ; i++ ) {
					dbMsg += "," + i + "/" + fCount;
					java.io.File rFile = files[i];
					String fName = rFile.getName();
					dbMsg += ")" + fName;
					if ( fName.endsWith(".csv") ) {
						nowCount++;
						dbMsg += ">>" + nowCount;
					}
				}
//							long wSize = file.length();
//							long maxFileSize = Long.parseLong(max_file_size);
//							dbMsg = dbMsg + ",wSize=" + wSize + "/max_file_size=" + maxFileSize;
//							if ( maxFileSize < wSize ) {
//								max_file_size = wSize + "";
//								dbMsg = dbMsg + ">>max_file_size=" + max_file_size;
//								//            setSaveParameter();                //端末内にファイル保存する為のパラメータ調整
//								myEditor.putString("max_file_size_key", max_file_size);
//								boolean kakikomi = myEditor.commit();
//								dbMsg = dbMsg + ",プリファレンス更新=" + kakikomi;
//							}
			}
			dbMsg = dbMsg + ",now_count=" + nowCount + "件";
			myLog(TAG, dbMsg);
		} catch (Exception er) {
			Log.e(TAG, dbMsg + ";でエラー発生；" + er);
		}
		return nowCount;
	}


	public int getDisplayOrientation(Activity activity) {
		final String TAG = "getDisplayOrientation[util}";
		String dbMsg = "";
		int degrees = 0;
		try {
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();   //helperからは((Activity)getContext()).
			switch ( rotation ) {
				case Surface.ROTATION_0:
					degrees = 0;
					break;
				case Surface.ROTATION_90:
					degrees = 90;
					break;
				case Surface.ROTATION_180:
					degrees = 180;
					break;
				case Surface.ROTATION_270:
					degrees = 270;
					break;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return degrees;
	}


	////汎用関数///////////////////////////////////////////////////////////////////////
	public boolean isIntVar(String val) {
		try {
			Integer.parseInt(val);
			return true;
		} catch (NumberFormatException nfex) {
			return false;
//			myErrorLog(TAG, dbMsg + ";でエラー発生；" + er);
		}
	}

	public boolean isLongVal(String val) {
		try {
			Long.parseLong(val);
			return true;
		} catch (NumberFormatException nfex) {
			return false;
		}
	}

	public boolean isFloatVal(String val) {
		try {
			Float.parseFloat(val);
			return true;
		} catch (NumberFormatException nfex) {
			return false;
		}
	}


	public boolean isDoubleVal(String val) {
		try {
			Double.parseDouble(val);
			return true;
		} catch (NumberFormatException nfex) {
			return false;
		}
	}

//保留；入力ダイアログ
//    public String retStr = "";
//     public void inputShow(String titolStr, String mggStr, String defaultStr) {
//         retStr = defaultStr;
//         LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
//         final View layout = inflater.inflate( R.layout.step_setting,(ViewGroup) findViewById(R.id.ss_root));
//         EditText ss_stet_et = (EditText) layout.findViewById(R.id.ss_stet_et);
//         TextView ss_msg_tv = (TextView) layout.findViewById(R.id.ss_msg_tv);
//         // アラーとダイアログ を生成
//         AlertDialog.Builder builder = new AlertDialog.Builder(this);
//         builder.setTitle(titolStr);
//         builder.setMessage(mggStr);
//         builder.setView(layout);
//         ss_stet_et.setText(defaultStr);
//
//         builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//             public void onClick(DialogInterface dialog, int which) {
//                 // OK ボタンクリック処理
//                 EditText text = (EditText) layout.findViewById(R.id.ss_stet_et);
//                 retStr = text.getText().toString();
//             }
//         });
//         builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//             public void onClick(DialogInterface dialog, int which) {
//               }
//         });
//
//         // 表示
//         builder.create().show();
//    }

	public void messageShow(String titolStr, String mggStr, Context context) {
		new AlertDialog.Builder(context).setTitle(titolStr).setMessage(mggStr).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		}).create().show();
	}

	public static boolean debugNow = true;

	public static void myLog(String TAG, String dbMsg) {
		try {
			if ( debugNow ) {
				Log.i(TAG, dbMsg + "");
			}
		} catch (Exception er) {
			Log.e(TAG, dbMsg + ";でエラー発生；" + er);
		}
	}

	public static boolean errorCheckNow = true;

	public static void myErrorLog(String TAG, String dbMsg) {
		if ( errorCheckNow ) {
			Log.e(TAG, dbMsg + "");
		}
	}


}
