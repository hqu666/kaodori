package com.hijiyam_koubou.kaodori;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ListAdapter;

import java.util.Arrays;
import java.util.Map;


public class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	private OnFragmentInteractionListener mListener;

	public PreferenceScreen sps;
	public static final String DEFAULT = "未設定";
	public Context context;
	public static SharedPreferences sharedPref;
	public SharedPreferences.Editor myEditor;
	public EditTextPreference write_folder_key;		//書込みルートフォルダ
	public EditTextPreference up_scale_key;			//顔から何割増しの枠で保存するか

	public String writeFolder;			//書込みルートフォルダ
	public String upScale="1.2";			//顔から何割増しの枠で保存するか


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String TAG = "onCreate[MPF]";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			MyPreferenceFragment.this.addPreferencesFromResource(R.xml.preferences);
			//		Log.i(TAG, dbMsg);
		} catch (Exception er) {
			Log.e(TAG, dbMsg + "で" + er.toString());
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
			myLog(TAG, dbMsg);
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
		} catch (Exception er) {
			myLog(TAG, dbMsg + "で" + er.toString());
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final String TAG = "onActivityCreated[MPF]";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			sps = this.getPreferenceScreen();            //☆PreferenceFragmentなら必要  .
			dbMsg = dbMsg + ",sps=" + sps;
			write_folder_key = ( EditTextPreference ) sps.findPreference("service_id_key");		//書込みルートフォルダ
			dbMsg = dbMsg + ",書込みルートフォルダ=" + writeFolder;
			if ( findPreference("write_folder_key") != null ) {
				write_folder_key.setDefaultValue(writeFolder);
				write_folder_key.setSummary(writeFolder);
			} else {
				(( EditTextPreference ) findPreference("write_folder_key")).setText(writeFolder);
			}
			up_scale_key = ( EditTextPreference ) sps.findPreference("up_scale_key");			//顔から何割増しの枠で保存するか
			dbMsg = dbMsg + ",顔から何割増しの枠で保存するか=" + upScale;
			if ( findPreference("up_scale_key") != null ) {
				up_scale_key.setDefaultValue(upScale);
				up_scale_key.setSummary(upScale);
			} else {
				(( EditTextPreference ) findPreference("up_scale_key")).setText(upScale);
			}
			myLog(TAG, dbMsg);
		} catch (Exception e) {
			Log.e(TAG, dbMsg + "で" + e.toString());
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
			myLog(TAG, dbMsg);
		} catch (Exception e) {
			Log.e(TAG, dbMsg + "で" + e.toString());
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
			myLog(TAG, dbMsg);
		} catch (Exception e) {
			Log.e(TAG, dbMsg + "で" + e.toString());
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
			myLog(TAG, dbMsg);
		} catch (Exception e) {
			Log.e(TAG, dbMsg + "で" + e.toString());
			//      myLog(TAG, dbMsg + "で" + e.toString(), "e");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		final String TAG = "onDestroy[MPF]";
		String dbMsg = "開始";
		try {

			myLog(TAG, dbMsg);
		} catch (Exception e) {
			Log.e(TAG, dbMsg + "で" + e.toString());
			//      myLog(TAG, dbMsg + "で" + e.toString(), "e");
		}
	}                                                            //切替時⑥

	/**
	 * 変更された項目の実変更の取得
	 * @param key 変更された項目のkey
	 *            照合の為に文字列定数を使う
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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

			myLog(TAG, dbMsg);
		} catch (Exception e) {
			Log.e(TAG, dbMsg + "で" + e.toString());
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
					String val = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getString(key, "");
					dbMsg += ";" + key + ";" + val;
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
					String val = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getString(key, "");
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
			myLog(TAG, dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG, dbMsg + "でエラー発生；" + er);
		}
	}

	/**
	 * 全項目読み
	 */
	public void readPref(Context context) {
		String TAG = "readPref[MPF]";
		String dbMsg = "開始";
		try {
			setSaveParameter();
			sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			myEditor = sharedPref.edit();
			Map< String, ? > keys = sharedPref.getAll();                          //System.collections.Generic.IDictionary<string, object>
			dbMsg = dbMsg + ",読み込み開始;keys=" + keys.size() + "件";        // keys.size()
			int i = 0;
			for ( String key : keys.keySet() ) {
				i++;
				dbMsg = dbMsg + "\n" + i + "/" + keys.size() + ")" + key;// + "は" + rStr;
				if ( key.equals("write_folder_key") ) {
					writeFolder = sharedPref.getString(key, writeFolder);
					dbMsg = dbMsg + ",書込みルートフォルダ=" + writeFolder;
				} else if ( key.equals("up_scale_key") ) {
					upScale = sharedPref.getString(key, "1.2");
					dbMsg +=  ",顔から何割増しの枠で保存するか=" + upScale;
				}
			}
			myLog(TAG, dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG, dbMsg + "でエラー発生；" + er);
		}
	}                                                                     //プリファレンスの読込み

	public void setSaveParameter() {                //端末内にファイル保存する為のパラメータ調整
		final String TAG = "setSaveParameter";
		String dbMsg = "開始";
		try {
			java.io.File wrDir = MyPreferenceFragment.this.getActivity().getApplicationContext().getFilesDir();				//自分のアプリ用の内部ディレクトリ
			dbMsg = ",端末内の保存先=" + writeFolder;
			if ( writeFolder.isEmpty() ) {
				writeFolder = wrDir.getPath()+ getResources().getString(R.string.app_name);
				dbMsg = dbMsg + ",>>" + writeFolder;
			}
			String local_dir_size = wrDir.getFreeSpace() + "";// "5000000";
			dbMsg = dbMsg + ",保存先の空き容量=" + local_dir_size;
			if ( local_dir_size.isEmpty() ) {
				local_dir_size = "5000000";
				dbMsg = dbMsg + ">>" + local_dir_size;
			}
				myLog(TAG, dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG, dbMsg + ";でエラー発生；" + er);
		}
	}            //端末内にファイル保存する為のパラメータ調整

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