package com.hijiyam_koubou.kaodori;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CS_Util {
	Context context;

	//	public CS_Util(Context context){
//		context = context;
//	}
	//ファイル操作///////////////////

	/**
	 * 指定されたパスが無ければ新規作成する
	 */
	public void maikOrgPass(String writeFolder) {
		final String TAG = "maikOrgPass[util]";
		String dbMsg = "開始";
		String local_dir = "";
		try {
			dbMsg += "writeFolder=" + writeFolder;
			File saveFolder = new File(writeFolder);
			if ( !saveFolder.exists() ) {
				saveFolder.mkdir();
				dbMsg += "作成";
			} else {
				dbMsg += "既存";
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			Log.e(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * アンドロイドのデータベースへ登録
	 * // (登録しないとギャラリーなどにすぐに反映されないため)
	 * @param mimeType No.	Format	MimeType
	 *                 1	MP3	audio/mpeg
	 *                 2	M4A	audio/mp4
	 *                 3	WAV	audio/x-wav
	 *                 4	AMR	audio/amr
	 *                 5	AWB	audio/amr-wb
	 *                 6	WMA(※1）	audio/x-ms-wma
	 *                 7	OGG	application/ogg
	 *                 8	OGA	application/ogg
	 *                 9	AAC	audio/aac
	 *                 10	MK	audio/x-matroska
	 *                 11	MID	audio/midi
	 *                 12	MDI	audio/midi
	 *                 13	XMF	audio/midi
	 *                 14	RTTTL	audio/midi
	 *                 15	SMF	audio/sp-midi
	 *                 16	IMY	audio/imelody
	 *                 17	RTX	audio/midi
	 *                 18	OTA	audio/midi
	 *                 19	M3U	audio/x-mpegurl
	 *                 20	PLS	audio/x-scpls
	 *                 21	WPL	application/vnd.ms-wpl
	 *                 22	MPEG	video/mpeg
	 *                 23	MP4	video/mp4
	 *                 24	M4V	video/mp4
	 *                 25	3GP	video/3gpp
	 *                 26	3GPP	video/3gpp
	 *                 27	3G2		video/3gpp2
	 *                 28	3GPP2	video/3gpp2
	 *                 29	MKV	video/x-matroska
	 *                 30	WEBM	video/x-matroska
	 *                 31	TS	video/mp2ts
	 *                 32	WMV(※1）	video/x-ms-wmv
	 *                 33	ASF(※1）	video/x-ms-asf
	 *                 34	JPG	image/jpeg
	 *                 35	JPEG	image/jpeg
	 *                 36	GIF	image/gif
	 *                 37	PNG	image/png
	 *                 38	BMP	image/x-ms-bmp
	 *                 39	WBMP	image/vnd.wap.wbmp
	 */
	public void setContentValues(Context activity , String mimeType , String saveFileName) {
		final String TAG = "setContentValues[util]";
		String dbMsg = "開始";
		String local_dir = "";
		try {
			dbMsg += saveFileName + "を" + mimeType + "に";
			ContentValues values = new ContentValues();
			ContentResolver contentResolver = activity.getContentResolver();
			values.put(MediaStore.Images.Media.MIME_TYPE , mimeType);
			values.put("_data" , saveFileName);
			Uri rUrl = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI , values);
			dbMsg += ">>" + rUrl;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			Log.e(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	/**
	 * このアプリケーション用のデータフォルダーを返す
	 */
	public String getAplPathName(Context context) {
		final String TAG = "getAplPathName[util]";
		String dbMsg = "開始";
		String local_dir = "";
		try {
			java.io.File wrDir = context.getFilesDir();//自分のアプリ用の内部ディレクトリ
			String wrDirName = wrDir.getPath();
			dbMsg += ",wrDir=" + wrDirName;            //wrDir=/data/user/0/com.example.hkuwayama.nuloger/files
			java.io.File file = new java.io.File(wrDir , wrDirName);
			local_dir = wrDir.getAbsolutePath();
			dbMsg = dbMsg + ",local_dir=" + local_dir;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			Log.e(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return local_dir;
	}

	public int getNowFileCount(Context context) {
		final String TAG = "getNowFileCount[util]";
		String dbMsg = "開始";
		int nowCount = 0;
		try {
			java.io.File[] files;
			String local_dir = getAplPathName(context);
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
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			Log.e(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return nowCount;
	}


	/**
	 * 保存フォルダーを検索して最新の保存ファイルをサムネイルに表示する
	 */
	public String getSaveFiles(String findFileName) {
		final String TAG = "getSaveFiles[util]";
		String dbMsg = "";
		String retStr = "0";
		try {
			dbMsg += "findFileName=" + findFileName;
			File[] files = new File(findFileName).listFiles();
			String extentionStr = "";
			for ( File fObj : files ) {
				dbMsg += "\nretStr=" + retStr;
				String fName = fObj.getName();
				dbMsg += ",fName=" + fName;
				if ( fObj.isDirectory() ) {
					if ( Integer.parseInt(retStr) < Integer.parseInt(fName) ) {
						retStr = fName;
					}
				} else {
					int point = fName.lastIndexOf(".");  //	String[] fNames = fName.split(".");が効かない
					String cName = "";
					if ( point != -1 ) {
						cName = fName.substring(0,point );
						extentionStr = fName.substring(point + 1);
					}
					dbMsg += ",cName=" + cName + ",extentionStr=" + extentionStr;
					if ( Integer.parseInt(retStr) < Integer.parseInt(cName) ) {
						retStr = cName;
						dbMsg += ">>retStr=" + retStr;
					}
				}
			}
			retStr = findFileName + File.separator + retStr;
			dbMsg += ">>retStr=" + retStr;
			if ( extentionStr.equals("") ) {
				dbMsg += ";フォルダ";
				files = new File(findFileName).listFiles();
				if ( 0 < files.length ) {
					retStr = getSaveFiles(retStr);
				}
			} else {
				dbMsg += ";最終ファイル";
				retStr += "." + extentionStr;
			}
			dbMsg += "=" + retStr;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return retStr;
	}

	//////////////////ファイル操作///

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
	public String retDateStr(long dateTimeVar , String patten) {
		final String TAG = "retDateStr[util}";
		String dbMsg = "";
		String retStr = "";
		try {
			dbMsg = "dateTimeVar=" + dateTimeVar;
			dbMsg += ",patten=" + patten;
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patten);
			retStr = simpleDateFormat.format(dateTimeVar);
			dbMsg += ">>" + retStr;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return retStr;
	}


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

	public void messageShow(String titolStr , String mggStr , Context context) {
		new AlertDialog.Builder(context).setTitle(titolStr).setMessage(mggStr).setPositiveButton(android.R.string.ok , new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog , int which) {
			}
		}).create().show();
	}

	public static boolean debugNow = true;

	public static void myLog(String TAG , String dbMsg) {
		try {
			if ( debugNow ) {
				Log.i(TAG , dbMsg + "");
			}
		} catch (Exception er) {
			Log.e(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public static boolean errorCheckNow = true;

	public static void myErrorLog(String TAG , String dbMsg) {
		if ( errorCheckNow ) {
			Log.e(TAG , dbMsg + "");
		}
	}


}
