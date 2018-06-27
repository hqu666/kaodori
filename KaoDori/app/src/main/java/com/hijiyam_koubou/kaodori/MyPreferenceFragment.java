package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ListAdapter;

import java.io.File;
import java.util.Arrays;
import java.util.Map;


public class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	private OnFragmentInteractionListener mListener;

	public PreferenceScreen sps;
	public static final String DEFAULT = "未設定";
	public Context context;
	public static SharedPreferences sharedPref;
	public SharedPreferences.Editor myEditor;
	public CheckBoxPreference is_face_recognition_key;                 //顔検出実行中

	public CheckBoxPreference is_detector_frontal_face_alt_key;                 //顔検出(標準)
	public CheckBoxPreference is_detector_profileface_key;                 //横顔
	public CheckBoxPreference is_detector_upperbody_key;                 //上半身
	public CheckBoxPreference is_detector_fullbody_key;                 //全身
	public CheckBoxPreference is_detector_lowerbody_key;                 //下半身
	public CheckBoxPreference is_detector_eye_key;                 //目(標準)
	public CheckBoxPreference is_detector_righteye_2splits_key;                 //右目
	public CheckBoxPreference is_detector_lefteye_2splitss_key;                 //左目
	public CheckBoxPreference is_detector_eyeglasses_key;                 //眼鏡
	public CheckBoxPreference is_detector_frontalcatface_key;                 //正面のみ？
	public CheckBoxPreference is_detector_frontalcatface_extended_key;                 //正面(拡張)？
	public CheckBoxPreference is_detector_frontalface_alt_tree_key;                 //正面の顔高い木？
	public CheckBoxPreference is_detector_frontalface_alt2_key;                 //正面顔全体2
	public CheckBoxPreference is_detector_frontalface_default_key;                 //正面デフォルト
	public CheckBoxPreference is_detector_smile_key;                 //笑顔
	public CheckBoxPreference is_detector_russian_plate_number_key;                 //ナンバープレート・ロシア
	public CheckBoxPreference is_detector_ricence_plate_rus_16stages_key;                 //ナンバープレートRUS
	public CheckBoxPreference is_overlap_rejection_key ;     //重複棄却

	public CheckBoxPreference is_chase_focus_key ;                 //追跡フォーカス
	public EditTextPreference write_folder_key;                    //書込みルートフォルダ
	public EditTextPreference up_scale_key;                        //顔から何割増しの枠で保存するか
	public Preference haarcascades_last_modified_key;            //顔認証プロファイルの最新更新日

	public boolean isFaceRecognition = true;                 //顔検出実行中
	public boolean is_overlap_rejection = true;     //重複棄却
	public boolean isChaseFocus = false;                 //追跡フォーカス
	public String write_folder="";            //書込みルートフォルダ
	public String up_scale = "1.2";            //顔から何割増しの枠で保存するか
	public String haarcascades_last_modified = "0";

	public boolean isSubCamera = false;                        //サブカメラに切り替え
	public boolean isAutoFlash = false;                        //オートフラッシュ
	public boolean isRumbling = false;                        //シャッター音の鳴動

	public CheckBoxPreference isSubCamera_key;                 //サブカメラに切り替え
	public CheckBoxPreference isAutoFlash_key;                 //オートフラッシュ
	public CheckBoxPreference isRumbling_key;                 //シャッター音の鳴動


	public boolean is_detector_frontal_face_alt = true;   //顔検出(標準)
	public boolean is_detector_profileface = true;               //横顔
	public boolean is_detector_upperbody = false;                //上半身
	public boolean is_detector_fullbody = false;                //全身
	public boolean is_detector_lowerbody = false;                // 下半身
	public boolean is_detector_eye = true;               //目(標準)
	public boolean is_detector_righteye_2splits = false; 		//右目
	public boolean is_detector_lefteye_2splits = false;                //左目
	public boolean is_detector_eyeglasses = false;                //眼鏡
	public boolean is_detector_frontalcatface = false;               //正面のみ？
	public boolean is_detector_frontalcatface_extended = false;                //正面(拡張)？string>
	public boolean is_detector_frontalface_alt_tree = false;               //正面の顔高い木？
	public boolean is_detector_frontalface_alt2 = false;                //正面顔全体2
	public boolean is_detector_frontalface_default = false;                //正面デフォルト
	public boolean is_detector_smile = false;               //笑顔
	public boolean is_detector_russian_plate_number = false;                //ナンバープレート・ロシア
	public boolean is_detector_ricence_plate_rus_16stages = false;     //ナンバープレートRUS


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String TAG = "onCreate[MPF]";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			MyPreferenceFragment.this.addPreferencesFromResource(R.xml.preferences);
			//		Log.i(TAG, dbMsg);
		} catch (Exception er) {
			Log.e(TAG , dbMsg + "で" + er.toString());
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
		final String TAG = "onAttach[MPF]";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			this.context = this.getActivity().getApplicationContext();    //( MainActivity ) context;
			readPref(context);
			myLog(TAG , dbMsg);
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
		} catch (Exception er) {
			myLog(TAG , dbMsg + "で" + er.toString());
		}
	}

	/**
	 * 初期表示
	 * */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final String TAG = "onActivityCreated[MPF]";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			sps = this.getPreferenceScreen();            //☆PreferenceFragmentなら必要  .
			dbMsg = dbMsg + ",sps=" + sps;

			isSubCamera_key = ( CheckBoxPreference ) sps.findPreference("isSubCamera_key");        //サブカメラに切り替え
			dbMsg = dbMsg + ",顔検出実行中=" + isSubCamera;
			if ( findPreference("isSubCamera_key") != null ) {
				isSubCamera_key.setChecked(isSubCamera);
			}

			isAutoFlash_key = ( CheckBoxPreference ) sps.findPreference("isAutoFlash_key");        //サブカメラに切り替え
			dbMsg = dbMsg + ",オートフラッシュ=" + isAutoFlash;
			if ( findPreference("isAutoFlash_key") != null ) {
				isAutoFlash_key.setChecked(isAutoFlash);
			}

			isRumbling_key = ( CheckBoxPreference ) sps.findPreference("isRumbling_key");        //サブカメラに切り替え
			dbMsg = dbMsg + ",シャッター音の鳴動=" + isRumbling;
			if ( findPreference("isRumbling_key") != null ) {
				isRumbling_key.setChecked(isRumbling);
			}

			is_face_recognition_key = ( CheckBoxPreference ) sps.findPreference("is_face_recognition_key");        //顔検出実行中
			dbMsg = dbMsg + ",顔検出実行中=" + isFaceRecognition;
			if ( findPreference("is_face_recognition_key") != null ) {
				is_face_recognition_key.setChecked(isFaceRecognition);
			}

			is_overlap_rejection_key = ( CheckBoxPreference ) sps.findPreference("is_overlap_rejection_key");        // = true;     //
			dbMsg = dbMsg + ",重複棄却=" + is_overlap_rejection;
			if ( findPreference("is_overlap_rejection_key") != null ) {
				is_overlap_rejection_key.setChecked(is_overlap_rejection);
			}

			is_detector_frontal_face_alt_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontal_face_alt_key");
			dbMsg = dbMsg + ",顔検出(標準)=" + is_detector_frontal_face_alt;
			if ( findPreference("is_detector_frontal_face_alt_key") != null ) {
				is_detector_frontal_face_alt_key.setChecked(is_detector_frontal_face_alt);
			}

			is_detector_profileface_key = ( CheckBoxPreference ) sps.findPreference("is_detector_profileface_key");
			dbMsg = dbMsg + ",横顔=" + is_detector_profileface;
			if ( findPreference("is_detector_profileface_key") != null ) {
				is_detector_profileface_key.setChecked(is_detector_profileface);
			}

			is_detector_upperbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_upperbody_key");
			dbMsg = dbMsg + ",上半身=" + is_detector_upperbody;
			if ( findPreference("is_detector_upperbody_key") != null ) {
				is_detector_upperbody_key.setChecked(is_detector_upperbody);
			}

			is_detector_fullbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_fullbody_key");
			dbMsg = dbMsg + ",全身=" + is_detector_fullbody;
			if ( findPreference("is_detector_fullbody_key") != null ) {
				is_detector_fullbody_key.setChecked(is_detector_fullbody);
			}

			is_detector_lowerbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_lowerbody_key");
			dbMsg = dbMsg + ",下半身=" + is_detector_lowerbody;
			if ( findPreference("is_detector_lowerbody_key") != null ) {
				is_detector_lowerbody_key.setChecked(is_detector_lowerbody);
			}

			is_detector_eye_key = ( CheckBoxPreference ) sps.findPreference("is_detector_eye_key");
			dbMsg = dbMsg + ",目(標準)=" + is_detector_eye;
			if ( findPreference("is_detector_eye_key") != null ) {
				is_detector_eye_key.setChecked(is_detector_eye);
			}

			is_detector_righteye_2splits_key = ( CheckBoxPreference ) sps.findPreference("is_detector_righteye_2splits_key");
			dbMsg = dbMsg + ",右目=" + is_detector_righteye_2splits;
			if ( findPreference("is_detector_righteye_2splits_key") != null ) {
				is_detector_righteye_2splits_key.setChecked(is_detector_righteye_2splits);
			}

			is_detector_lefteye_2splitss_key = ( CheckBoxPreference ) sps.findPreference("is_detector_lefteye_2splitss_key");
			dbMsg = dbMsg + ",左目=" + is_detector_lefteye_2splits;
			if ( findPreference("is_detector_lefteye_2splitss_key") != null ) {
				is_detector_lefteye_2splitss_key.setChecked(is_detector_lefteye_2splits);
			}

			is_detector_eyeglasses_key = ( CheckBoxPreference ) sps.findPreference("is_detector_eyeglasses_key");
			dbMsg = dbMsg + ",眼鏡=" + is_detector_eyeglasses;
			if ( findPreference("is_detector_eyeglasses_key") != null ) {
				is_detector_eyeglasses_key.setChecked(is_detector_eyeglasses);
			}

			is_detector_frontalcatface_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalcatface_key");
			dbMsg = dbMsg + ",正面のみ？=" + is_detector_frontalcatface;
			if ( findPreference("is_detector_frontalcatface_key") != null ) {
				is_detector_frontalcatface_key.setChecked(is_detector_frontalcatface);
			}

			is_detector_frontalcatface_extended_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalcatface_extended_key");
			dbMsg = dbMsg + ",正面(拡張)？=" + is_detector_frontalcatface_extended;
			if ( findPreference("is_detector_frontalcatface_extended_key") != null ) {
				is_detector_frontalcatface_extended_key.setChecked(is_detector_frontalcatface_extended);
			}

			is_detector_frontalface_alt_tree_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_alt_tree_key");
			dbMsg = dbMsg + ",正面の顔高い木？=" + is_detector_frontalface_alt_tree;
			if ( findPreference("is_detector_frontalface_alt_tree_key") != null ) {
				is_detector_frontalface_alt_tree_key.setChecked(is_detector_frontalface_alt_tree);
			}

			is_detector_frontalface_alt2_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_alt2_key");
			dbMsg = dbMsg + ",正面顔全体2？=" + is_detector_frontalface_alt2;
			if ( findPreference("is_detector_frontalface_alt2_key") != null ) {
				is_detector_frontalface_alt2_key.setChecked(is_detector_frontalface_alt2);
			}

			is_detector_frontalface_default_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_default_key");
			dbMsg = dbMsg + ",正面デフォルト？=" + is_detector_frontalface_default;
			if ( findPreference("is_detector_frontalface_default_key") != null ) {
				is_detector_frontalface_default_key.setChecked(is_detector_frontalface_default);
			}

			is_detector_smile_key = ( CheckBoxPreference ) sps.findPreference("is_detector_smile_key");
			dbMsg = dbMsg + ",笑顔？=" + is_detector_smile;
			if ( findPreference("is_detector_smile_key") != null ) {
				is_detector_smile_key.setChecked(is_detector_smile);
			}

			is_detector_russian_plate_number_key = ( CheckBoxPreference ) sps.findPreference("is_detector_russian_plate_number_key");
			dbMsg = dbMsg + ",ナンバープレート・ロシア=" + is_detector_russian_plate_number;
			if ( findPreference("is_detector_russian_plate_number_key") != null ) {
				is_detector_russian_plate_number_key.setChecked(is_detector_russian_plate_number);
			}

			is_detector_ricence_plate_rus_16stages_key = ( CheckBoxPreference ) sps.findPreference("is_detector_ricence_plate_rus_16stages_key");
			dbMsg = dbMsg + ",ナンバープレートRUS=" + is_detector_ricence_plate_rus_16stages;
			if ( findPreference("is_detector_ricence_plate_rus_16stages_key") != null ) {
				is_detector_ricence_plate_rus_16stages_key.setChecked(is_detector_ricence_plate_rus_16stages);
			}

			is_chase_focus_key = ( CheckBoxPreference ) sps.findPreference("is_chase_focus_key");        //追跡フォーカス
			dbMsg = dbMsg + ",追跡フォーカス=" + isChaseFocus;
			if ( findPreference("is_chase_focus_key") != null ) {
				is_chase_focus_key.setChecked(isChaseFocus);
			}

			write_folder_key = ( EditTextPreference ) sps.findPreference("service_id_key");        //書込みルートフォルダ
			dbMsg = dbMsg + ",書込みルートフォルダ=" + write_folder;
			if ( findPreference("write_folder_key") != null ) {
				write_folder_key.setDefaultValue(write_folder);
				write_folder_key.setSummary(write_folder);
			} else {
				(( EditTextPreference ) findPreference("write_folder_key")).setText(write_folder);
			}
			up_scale_key = ( EditTextPreference ) sps.findPreference("up_scale_key");            //顔から何割増しの枠で保存するか
			dbMsg = dbMsg + ",顔から何割増しの枠で保存するか=" + up_scale;
			if ( findPreference("up_scale_key") != null ) {
				up_scale_key.setDefaultValue(up_scale);
				up_scale_key.setSummary(up_scale);
			} else {
				(( EditTextPreference ) findPreference("up_scale_key")).setText(up_scale);
			}
			dbMsg = dbMsg + ",顔認証プロファイルの最新更新日=" + haarcascades_last_modified;
			if ( findPreference("haarcascades_last_modified_key") != null ) {
				haarcascades_last_modified_key.setDefaultValue(haarcascades_last_modified);
				haarcascades_last_modified_key.setSummary(haarcascades_last_modified);
			} else {
				(( Preference ) findPreference("haarcascades_last_modified_key")).setSummary(haarcascades_last_modified);
			}
			myLog(TAG , dbMsg);
		} catch (Exception e) {
			Log.e(TAG , dbMsg + "で" + e.toString());
		}
	}

	/**
	 * 保存時の処理
	 * http://libro.tuyano.com/index3?id=306001&page=4
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		final String TAG = "onSaveInstanceState";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			myLog(TAG , dbMsg);
		} catch (Exception e) {
			Log.e(TAG , dbMsg + "で" + e.toString());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		final String TAG = "onResume[MPF]";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			sharedPref.registerOnSharedPreferenceChangeListener(this);   //Attempt to invoke virtual method 'android.content.SharedPreferences android.preference.PreferenceScreen.getSharedPreferences()
			reloadSummary();
			myLog(TAG , dbMsg);
		} catch (Exception e) {
			Log.e(TAG , dbMsg + "で" + e.toString());
			//      myLog(TAG, dbMsg + "で" + e.toString(), "e");
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		final String TAG = "onPause[MPF]";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			sharedPref.unregisterOnSharedPreferenceChangeListener(this);
			myLog(TAG , dbMsg);
		} catch (Exception e) {
			Log.e(TAG , dbMsg + "で" + e.toString());
			//      myLog(TAG, dbMsg + "で" + e.toString(), "e");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		final String TAG = "onDestroy[MPF]";
		String dbMsg = "開始";
		try {

			myLog(TAG , dbMsg);
		} catch (Exception e) {
			Log.e(TAG , dbMsg + "で" + e.toString());
			//      myLog(TAG, dbMsg + "で" + e.toString(), "e");
		}
	}                                                            //切替時⑥

	/**
	 * 変更された項目の実変更の取得
	 * @param key 変更された項目のkey
	 *            照合の為に文字列定数を使う
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences , String key) {
		final String TAG = "onSharedPrefChd[MPF]";
		String dbMsg = "設定が変更された";/////////////////////////////////////////////////
		try {
			reloadSummary();
//            dbMsg = "key=" + key;/////////////////////////////////////////////////
//            String setStr = "";
//            Map<String, ?> keys = sharedPreferences.getAll();
//            if (key.equals("student_id_key")) {         //実施者ID（学籍番号）
//                setStr = student_id_ed.getText();
//                MA.student_id = setStr;
//                setStudentId(setStr);
//            } else if (key.equals("accountName_key")) {
//                setStr = accountName_ed.getText();
//                MA.accountName = accountName_ed.getText();
//                setAccountName(setStr);
//            } else if (key.equals("stock_count_key")) {
//                setStr = stock_count_ed.getText();
//                if (!setStockCount(setStr)) {
//                    return;
//                }
//                MA.stock_count = setStr;
//            } else if (key.equals("waiting_scond_key")) {
//                setStr = waiting_scond_et.getText();
//                if (!setWaitingScond(setStr)) {
//                    return;
//                }
//                MA.waiting_scond = setStr;
//            }
//            dbMsg = dbMsg + ",setStr=" + setStr;
//            MA.myEditor.remove(key);      //「ContentValues」クラスのオブジェクトに追加されたキーと値のペアを、キーを指定して削除する
//            MA.myEditor.putString(key, setStr);
//            boolean kakikomi = MA.myEditor.commit();
//            dbMsg = dbMsg + ",書込み=" + kakikomi;

			myLog(TAG , dbMsg);
		} catch (Exception e) {
			Log.e(TAG , dbMsg + "で" + e.toString());
			//      myLog(TAG, dbMsg + "で" + e.toString(), "e");
		}
	}

	//各項目のプリファレンス上の設定///////////////////////////////////////////////////////////////////////////

	/**
	 * https://qiita.com/noppefoxwolf/items/18e785f4d760f7cc4314
	 */
	private void reloadSummary() {
		String TAG = "reloadSummary[MPF]";
		String dbMsg = "開始";
		try {
			sps = this.getPreferenceScreen();            //☆PreferenceFragmentなら必要  .
			ListAdapter adapter = sps.getRootAdapter();      //            var adapter = this.preferenceScreen.RootAdapter;
//			for ( int i = 0 ; i < adapter.getCount() ; i++ ) {               //ListとEdit連携用のインデックス取得
//				Object item = adapter.getItem(i);
//				if ( item instanceof EditTextPreference ) {
//					EditTextPreference pref = ( EditTextPreference ) item;
//					String key = pref.getKey();
//					if ( key.equals("waiting_scond_key") ) {
//						waiting_scond_index = i;
//					}
//				} else if ( item instanceof ListPreference ) {
//					ListPreference pref = ( ListPreference ) item;
//					pref.setSummary(pref.getEntry() == null ? "" : pref.getEntry());
//
//					String key = pref.getKey();
//					if ( key.equals("waiting_scond_list") ) {
//						waiting_scond_list_index = i;
//					}
//				}
//			}
//			dbMsg += "waiting_scond_index=" + waiting_scond_index + ",_list_index=" + waiting_scond_list_index;
			sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
			myEditor = sharedPref.edit();

			for ( int i = 0 ; i < adapter.getCount() ; i++ ) {               //ListとEdit連携用のインデックス取得
				dbMsg += "\n" + i + ")";
				Object item = adapter.getItem(i);
				dbMsg += item;

				if ( item instanceof EditTextPreference ) {
					EditTextPreference pref = ( EditTextPreference ) item;
					String key = pref.getKey();
					String val = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getString(key , "");
					dbMsg += ";" + key + ";" + val;
					if (key.equals(up_scale_key) ) {
						dbMsg += ",up_scale=" + val;
						CS_Util UTIL = new CS_Util();
						if ( UTIL.isFloatVal(val) ) {
							val = Float.parseFloat(val)+"";
						} else {
							val = "1.2";
						}
						dbMsg += "," + getResources().getString(R.string.up_scale) + "=" + val;
					}
					pref.setSummary(val);
					//if (key.Equals("waiting_scond_key")) {
					//	if (stock_count != val) {
					//		stock_count = val + "";
					//		myEditor.PutString("waiting_scond_key", val);
					//		myEditor.Commit();
					//		var tItem = adapter.GetItem(waiting_scond_list_index);
					//		var tPref = (ListPreference)tItem;
					//		string tA = tPref.Entry;
					//		int tIndex = tPref.FindIndexOfValue(val);
					//		if(-1< tIndex) {
					//			tPref.SetValueIndex(tIndex);
					//		}
					//	}
					//}
				} else if ( item instanceof CheckBoxPreference ) {
					CheckBoxPreference pref = ( CheckBoxPreference ) item;
					String key = pref.getKey();
					boolean pVal = pref.isChecked();
					dbMsg += ";" + key + ";" + pVal;
					pref.setSummaryOn("現在 On");      					// CheckBox が On の時のサマリーを設定
					pref.setSummaryOff("現在 Off");      					// CheckBox が Off の時のサマリーを設定
				} else if ( item instanceof ListPreference ) {
					ListPreference pref = ( ListPreference ) item;
					String key = pref.getKey();
					String pVal = pref.getValue();
					dbMsg += ";" + key + ";" + pVal;
					pref.setSummary(pVal);
//					if ( key.equals("waiting_scond_list") ) {
//						if ( stock_count != pVal ) {
//							stock_count = pVal + "";
//							myEditor.putString("waiting_scond_key", pVal);
//							myEditor.commit();
//							Object tItem = adapter.getItem(waiting_scond_index);
//							EditTextPreference tPref = ( EditTextPreference ) tItem;
//							tPref.setDefaultValue(pVal);
//							tPref.setText(pVal);
//						}
//					}
				} else if ( item instanceof Preference ) {                    //PreferenceScreenもここに来る
					Preference pref = ( Preference ) item;
					String key = pref.getKey();
					String val = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getString(key , "");
//					if ( key.equals("drive_setting_key") ) {
//						dbMsg += ",accountName=" + accountName + ",rootFolderID=" + rootFolderID + ",lastFolderID=" + lastFolderID + ",lastFolderPath=" + lastFolderPath;
//						val = getResources().getString(R.string.account_Name) + "=" + accountName + "\n" +
//									  getResources().getString(R.string.root_folder_id) + "=" + rootFolderID + "\n" +
//									  getResources().getString(R.string.last_folder_id) + "=" + lastFolderID + "\n" +
//									  getResources().getString(R.string.last_folder_path) + "=" + lastFolderPath;
//						accountName_key.setSummary(accountName);
//						root_folder_id_key.setSummary(rootFolderID);
//						last_folder_id_key.setSummary(lastFolderID);
//						last_folder_path_key.setSummary(lastFolderPath);
//					} else if ( key.equals("now_condition_key") ) {
//						dbMsg += ",local_dir=" + local_dir + ",local_dir_size=" + local_dir_size + ",max_file_size=" + max_file_size;
//						val = getResources().getString(R.string.Destination_in_the_terminal) + "=" + local_dir + "\n" + getResources().getString(R.string.free_space_of_the_destination) + "=" + local_dir_size + "\n" + getResources().getString(R.string.past_biggest_file_size) + "=" + max_file_size;
//						local_dir_key.setSummary(local_dir);
//						local_dir_size_key.setSummary(local_dir_size);
//						max_file_size_key.setSummary(max_file_size);
//					}
//					dbMsg += ";" + key + ";" + val;
//					pref.setSummary(val);
				} else if ( item instanceof PreferenceScreen ) {
					PreferenceScreen pref = ( PreferenceScreen ) item;
					String key = pref.getKey();
					//     List<String> vals = new ArrayList<String>();
//					List< String > keyList = new ArrayList< String >();
					String wrString = "";
					if ( key.equals("skyway_setting_key") ) {
//						skyway_setting_key = ( PreferenceScreen ) sps.findPreference("skyway_setting_key");	//SkyWayの接続状況
//						dbMsg = dbMsg + ",peer_id=" + peer_id;
//						peer_id_key = ( Preference ) sps.findPreference("peer_id_key");						//SkyWayで取得しているこの端末のID
//						peer_id_key.setSummary(peer_id);
//						wrString = getResources().getString(R.string.peer_id_titol) + " : "+ peer_id;
//
//						partner_id_key = ( Preference ) sps.findPreference("partner_id_key");								//SkyWayに接続要求する相手端末のID
//						dbMsg = dbMsg + ",partner_id=" + partner_id;
//						partner_id_key.setSummary(partner_id);
//						wrString += "\n"+getResources().getString(R.string.partner_id_titol) + " : "+ partner_id;
//
//						skyway_api_key = ( Preference ) sps.findPreference("skyway_api_key");								//SkyWayに接続する為のAPIキー
//						dbMsg = dbMsg + ",API_KEY=" + API_KEY;
//						skyway_api_key.setSummary(API_KEY);
//						wrString += "\n"+getResources().getString(R.string.skyway_api_key_titol) + " : "+ API_KEY;
//
//						skyway_secret_key = ( Preference ) sps.findPreference("skyway_secret_key");							//SkyWayでAPIキーと合わせて発行されるシークレットキー
//						dbMsg = dbMsg + ",sw_secret_key=" + sw_secret_key;
//						skyway_secret_key.setSummary(sw_secret_key);
//						wrString += "\n"+getResources().getString(R.string.skyway_secret_key_titol) + " : "+ skyway_secret_key;
//
//						skyway_available_domain_key = ( Preference ) sps.findPreference("skyway_available_domain_key");		//SkyWayに登録した利用可能ドメイン
//						dbMsg = dbMsg + ",DOMAIN=" + DOMAIN;
//						skyway_available_domain_key.setSummary(DOMAIN);
//						wrString += "\n"+getResources().getString(R.string.available_domain_titol) + " : "+ DOMAIN;

//			skyway_setting_key.setSummary(summaryStr);


//						dbMsg += ",accountName=" + accountName + ",client_id=" + client_id + ",drive_id=" + drive_id;
//						keyList.add(accountName);
//						keyList.add(client_id);
//						keyList.add(drive_id);
//						accountName_key.setSummary(accountName);
//						client_id_key.setSummary(client_id);
//						drive_id_key.setSummary(drive_id);
//					} else if ( key.equals("now_condition_key") ) {
//						dbMsg += ",local_dir=" + local_dir + ",local_dir_size=" + local_dir_size + ",max_file_size=" + max_file_size;
//						keyList.add(local_dir);
//						keyList.add(local_dir_size);
//						keyList.add(max_file_size);
//						local_dir_key.setSummary(local_dir);
//						local_dir_size_key.setSummary(local_dir_size);
//						max_file_size_key.setSummary(max_file_size);
					}

//					//var grAdapter = this.PreferenceScreen.GetPreference(i);
//					//int grCount = 0;
//					for ( String rKey : keyList ) {
//						String rStr = ( String ) PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getString(rKey, "");
//						if ( !rStr.equals("") ) {
//							String keyName = rKey.replace("_key", "");
//							wrString += "" + keyName + "=" + rStr + "\n";
//							//var gItem = grAdapter.GetItem(grCount);
//							//gPref.Summary = rStr;
//							//grCount++;
//						}
//					}
//					pref.setSummary(wrString);
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
		}
	}

	/**
	 * 全項目読み
	 */
	public void readPref(Context context) {
		String TAG = "readPref[MPF]";
		String dbMsg = "開始";
		try {
			this.context = context;
			sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			myEditor = sharedPref.edit();
			setSaveParameter(context);
			setHaarcascadesParameter(context);
			Map< String, ? > keys = sharedPref.getAll();                          //System.collections.Generic.IDictionary<string, object>
			dbMsg = dbMsg + ",読み込み開始;keys=" + keys.size() + "件";        // keys.size()
			int i = 0;
			for ( String key : keys.keySet() ) {
				i++;
				dbMsg +=  "\n" + i + "/" + keys.size() + ")" + key;// + "は" + rStr;
				if ( key.equals("is_face_recognition_key") ) {
					isFaceRecognition = sharedPref.getBoolean(key , isFaceRecognition);
					dbMsg += ",顔検出実行中=" + isFaceRecognition;
				}else if ( key.equals("is_overlap_rejection_key") ) {
					is_overlap_rejection = sharedPref.getBoolean(key , is_overlap_rejection);
					dbMsg +=  ",重複棄却=" + is_overlap_rejection;
				}else if ( key.equals("is_detector_frontal_face_alt_key") ) {
					is_detector_frontal_face_alt = sharedPref.getBoolean(key , is_detector_frontal_face_alt);
					dbMsg +=  ",顔検出(標準)=" + is_detector_frontal_face_alt;
				}else if ( key.equals("is_detector_profileface_key") ) {
					is_detector_profileface = sharedPref.getBoolean(key , is_detector_profileface);
					dbMsg +=  ",横顔=" + is_detector_profileface;
				}else if ( key.equals("is_detector_upperbody_key") ) {
					is_detector_upperbody = sharedPref.getBoolean(key , is_detector_upperbody);
					dbMsg +=  ",上半身=" + is_detector_upperbody;
				}else if ( key.equals("is_detector_fullbody_key") ) {
					is_detector_fullbody = sharedPref.getBoolean(key , is_detector_fullbody);
					dbMsg +=  ",全身=" + is_detector_fullbody;
				}else if ( key.equals("is_detector_lowerbody_key") ) {
					is_detector_lowerbody = sharedPref.getBoolean(key , is_detector_lowerbody);
					dbMsg +=  ",下半身=" + is_detector_lowerbody;
				}else if ( key.equals("is_detector_eye_key") ) {
					is_detector_eye = sharedPref.getBoolean(key , is_detector_eye);
					dbMsg +=  ",目(標準)=" + is_detector_eye;
				}else if ( key.equals("is_detector_righteye_2splits_key") ) {
					is_detector_righteye_2splits = sharedPref.getBoolean(key , is_detector_righteye_2splits);
					dbMsg +=  ",右目=" + is_detector_righteye_2splits;
				}else if ( key.equals("is_detector_lefteye_2splitss_key") ) {
					is_detector_lefteye_2splits = sharedPref.getBoolean(key , is_detector_lefteye_2splits);
					dbMsg +=  ",左目=" + is_detector_lefteye_2splits;
				}else if ( key.equals("is_detector_eyeglasses_key") ) {
					is_detector_eyeglasses = sharedPref.getBoolean(key , is_detector_eyeglasses);
					dbMsg +=  ",眼鏡=" + is_detector_eyeglasses;
				}else if ( key.equals("is_detector_frontalcatface_key") ) {
					is_detector_frontalcatface = sharedPref.getBoolean(key , is_detector_frontalcatface);
					dbMsg +=  ",正面のみ=" + is_detector_frontalcatface;
				}else if ( key.equals("is_detector_frontalcatface_extended_key") ) {
					is_detector_frontalcatface_extended = sharedPref.getBoolean(key , is_detector_frontalcatface_extended);
					dbMsg +=  ",正面(拡張)=" + is_detector_frontalcatface_extended;
				}else if ( key.equals("is_detector_frontalface_alt_tree_key") ) {
					is_detector_frontalface_alt_tree = sharedPref.getBoolean(key , is_detector_frontalface_alt_tree);
					dbMsg +=  ",正面の顔高い木？)=" + is_detector_frontalface_alt_tree;
				}else if ( key.equals("is_detector_frontalface_alt2_key") ) {
					is_detector_frontalface_alt2 = sharedPref.getBoolean(key , is_detector_frontalface_alt2);
					dbMsg +=  ",正面顔全体2=" + is_detector_frontalface_alt2;
				}else if ( key.equals("is_detector_frontalface_default_key") ) {
					is_detector_frontalface_default = sharedPref.getBoolean(key , is_detector_frontalface_default);
					dbMsg +=  ",正面デフォルト=" + is_detector_frontalface_default;
				}else if ( key.equals("is_detector_smile_key") ) {
					is_detector_smile = sharedPref.getBoolean(key , is_detector_smile);
					dbMsg +=  ",笑顔=" + is_detector_smile;
				}else if ( key.equals("is_detector_russian_plate_number_key") ) {
					is_detector_russian_plate_number = sharedPref.getBoolean(key , is_detector_russian_plate_number);
					dbMsg +=  ",ナンバープレート・ロシア=" + is_detector_russian_plate_number;
				}else if ( key.equals("is_detector_ricence_plate_rus_16stages_key") ) {
					is_detector_ricence_plate_rus_16stages = sharedPref.getBoolean(key , is_detector_ricence_plate_rus_16stages);
					dbMsg +=  ",ナンバープレートRUS=" + is_detector_ricence_plate_rus_16stages;
				}else if ( key.equals("is_chase_focus_key") ) {
					isChaseFocus = sharedPref.getBoolean(key , isChaseFocus);
					dbMsg +=  ",追跡フォーカス=" + isChaseFocus;
				}else if ( key.equals("isSubCamera_key") ) {
					isSubCamera = sharedPref.getBoolean(key , isSubCamera);
					dbMsg +=  ",サブカメラに切り替え=" + isSubCamera;
				}else if ( key.equals("isAutoFlash_key") ) {
					isAutoFlash = sharedPref.getBoolean(key , isAutoFlash);
					dbMsg +=  ",オートフラッシュ=" + isAutoFlash;
				}else if ( key.equals("isRumbling_key") ) {
					isRumbling = sharedPref.getBoolean(key , isRumbling);
					dbMsg +=  ",シャッター音の鳴動=" + isRumbling;
				}else if ( key.equals("write_folder_key") ) {
					write_folder = sharedPref.getString(key , write_folder);
					dbMsg +=  ",書込みルートフォルダ=" + write_folder;
				} else if ( key.equals("up_scale_key") ) {
					up_scale = sharedPref.getString(key , up_scale);
					dbMsg += ",顔から何割増しの枠で保存するか=" + up_scale;
				} else if ( key.equals("haarcascades_last_modified_key") ) {
					haarcascades_last_modified = sharedPref.getString(key , haarcascades_last_modified);
					dbMsg += ",顔認証プロファイルの最新更新日=" + haarcascades_last_modified;
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
		}
	}                                                                     //プリファレンスの読込み

	/**
	 * 端末内にファイル保存する為のパラメータ調整
	 */
	public void setSaveParameter(Context context) {
		final String TAG = "setSaveParameter[MPF]";
		String dbMsg = "開始";
		try {
			dbMsg = ",端末内の保存先=" + write_folder;
			if (write_folder.isEmpty()) {  
				File photDir =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				//              //自分のアプリ用の内部ディレクトリ    context.getFilesDir();
				dbMsg += ",photDir=" + photDir.getPath() + File.separator;      //pathSeparatorは：
				write_folder = photDir.getPath() + File.separator+ context.getResources().getString(R.string.app_name);
				dbMsg +=  ",>>" + write_folder;
				myEditor.putString("write_folder_key" , write_folder);
				dbMsg += ",更新";
				myEditor.commit();
				dbMsg += "完了";
			}
//			String local_dir_size = userDir.getFreeSpace() + "";// "5000000";
//			dbMsg = dbMsg + ",保存先の空き容量=" + local_dir_size;
//			if ( local_dir_size.isEmpty() ) {
//				local_dir_size = "5000000";
//				dbMsg = dbMsg + ">>" + local_dir_size;
//			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}            //端末内にファイル保存する為のパラメータ調整

	/**
	 * 顔認証プロファイルの情報
	 */
	public void setHaarcascadesParameter(Context context) {
		final String TAG = "setHaarcascadesParameter[MPF]";
		String dbMsg = "開始";
		try {
			long haarcascadesLastMdified = Long.parseLong(haarcascades_last_modified);
			dbMsg += "現在の最終=" + haarcascades_last_modified;
			File userDir = context.getFilesDir();
			File dst = new File(userDir , "haarcascades");
			if ( !dst.exists() ) {
				dbMsg += "未作成";
				haarcascades_last_modified = "0";
			} else {
				for ( String filename : context.getAssets().list("haarcascades") ) {
					dbMsg += "," + filename;
					File file = new File(dst , filename);
					Long lastModified = file.lastModified();
					dbMsg += ",更新日=" + lastModified;
					if ( haarcascadesLastMdified < lastModified ) {
						haarcascadesLastMdified = lastModified;
					}
				}
				haarcascades_last_modified = haarcascadesLastMdified + "";
			}
			dbMsg = dbMsg + ",>最終更新日>" + haarcascades_last_modified;
			myEditor.putString("haarcascades_last_modified_key" , haarcascades_last_modified);
			dbMsg += ",更新";
			myEditor.commit();
			dbMsg += "完了";
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}            //顔認証プロファイルの情報


	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		void onFragmentInteraction(Uri uri);
	}

	public static void myLog(String TAG , String dbMsg) {
		CS_Util UTIL = new CS_Util();
		UTIL.myLog(TAG , dbMsg);
	}

	public static void myErrorLog(String TAG , String dbMsg) {
		CS_Util UTIL = new CS_Util();
		UTIL.myErrorLog(TAG , dbMsg);
	}

}