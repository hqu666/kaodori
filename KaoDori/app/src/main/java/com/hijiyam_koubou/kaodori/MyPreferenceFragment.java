package com.hijiyam_koubou.kaodori;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
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

	public PreferenceScreen phot_key;                            //撮影設定
	public CheckBoxPreference isSubCamera_key;                    //サブカメラに切り替え
	public CheckBoxPreference isAutoFlash_key;                    //オートフラッシュ
	public CheckBoxPreference isRumbling_key;                    //シャッター音の鳴動
	public CheckBoxPreference isTexturView_key;
	public EditTextPreference up_scale_key;                        //顔から何割増しの枠で保存するか

	public PreferenceScreen video_key;                                //動画設定
	public ListPreference video_output_format_key;                    //出力フォーマット
	public ListPreference video_rencoding_bit_rate_key;                //ビットレート
	public ListPreference video_rencoding_frame_rate_key;            //フレームレート
	public ListPreference video_audio_source_key;                    //音声入力
	public ListPreference video_data_source_key;                    //取得元
	public ListPreference video_encorde_key;                        //ビデオエンコーダ
	public ListPreference video_audio_encorde_key;                    //オーディオエンコーダ

	public TypedArray videoOutputArray;                            //出力フォーマット
	public TypedArray bitRateeArray;                                //ビットレート
	public TypedArray videoFrameRateArray;                            //フレームレート
	public TypedArray videoAudioSourceArray;                        //音声入力
	public TypedArray videoDataSourceArray;                        //取得元
	public TypedArray videoEncordeArray;                            //ビデオエンコーダ
	public TypedArray audioEncordeArray;                            //オーディオエンコーダ

	public PreferenceScreen effect_key;                                    //エフェクト
	public CheckBoxPreference is_face_recognition_key;                 //顔検出実行中
	public CheckBoxPreference is_overlap_rejection_key;     //重複棄却

	public PreferenceScreen detector_select_key;        //検出対象選択
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

	public CheckBoxPreference is_chase_focus_key;                 //追跡フォーカス

	public PreferenceScreen other_key;        //その他
	public EditTextPreference write_folder_key;                    //書込みルートフォルダ
	public Preference haarcascades_last_modified_key;            //顔認証プロファイルの最新更新日

	public boolean isSubCamera = false;                        //サブカメラに切り替え
	public boolean isAutoFlash = false;                        //オートフラッシュ
	public boolean isRumbling = false;                        //シャッター音の鳴動
	public boolean isTexturView = true;                 //高速プレビュー
	public String up_scale = "1.2";            //顔から何割増しの枠で保存するか

	public boolean isFaceRecognition = true;                 //顔検出実行中
	public boolean is_overlap_rejection = true;     //重複棄却
	public boolean isChaseFocus = false;                 //追跡フォーカス

	public boolean is_detector_frontal_face_alt = true;   //顔検出(標準)
	public boolean is_detector_profileface = true;               //横顔
	public boolean is_detector_upperbody = false;                //上半身
	public boolean is_detector_fullbody = false;                //全身
	public boolean is_detector_lowerbody = false;                // 下半身
	public boolean is_detector_eye = true;               //目(標準)
	public boolean is_detector_righteye_2splits = false;        //右目
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

	public int vi_audioSource = MediaRecorder.AudioSource.MIC;            //1;mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	public int vi_videoSource = MediaRecorder.VideoSource.SURFACE;        //2;mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
	public int vi_outputFormat = MediaRecorder.OutputFormat.MPEG_4;        //2;mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
	public int vi_videoEncodingBitRate = 10000000;                        //mMediaRecorder.setVideoEncodingBitRate(10000000);
	public int vi_videoFrameRate = 30;                                    //mMediaRecorder.setVideoFrameRate(30);
	public int vi_videoEncoder = MediaRecorder.VideoEncoder.H264;        //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
	public int vi_audioEncoder = MediaRecorder.AudioEncoder.AAC;            //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);


	public String write_folder = "";            //書込みルートフォルダ
	public String haarcascades_last_modified = "0";
	public String haarcascades_last_modifiedStr = haarcascades_last_modified + "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String TAG = "onCreate[MPF]";
		String dbMsg = "";
		try {
			MyPreferenceFragment.this.addPreferencesFromResource(R.xml.preferences);
			dbMsg += "isTexturView=" + isTexturView;
			if ( Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ) {                //(初回起動で)全パーミッションの許諾を取る
				isTexturView = true;                 //高速プレビュー
			} else {
				isTexturView = false;
			}
			dbMsg += ">>" + isTexturView;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
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
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final String TAG = "onActivityCreated[MPF]";
		String dbMsg = "開始";/////////////////////////////////////////////////
		try {
			sps = this.getPreferenceScreen();            //☆PreferenceFragmentなら必要  .
			dbMsg += ",sps=" + sps;
			String summaryStr = "";

			phot_key = ( PreferenceScreen ) sps.findPreference("phot_key");        //
			isSubCamera_key = ( CheckBoxPreference ) sps.findPreference("isSubCamera_key");        //サブカメラに切り替え
			dbMsg += ",サブカメラに切り替え=" + isSubCamera;
			if ( findPreference("isSubCamera_key") != null ) {
				isSubCamera_key.setChecked(isSubCamera);
//				if(isSubCamera){
//					summaryStr +=getResources().getString(R.string.mm_phot_sub_main) ;
//				}
			}

			isAutoFlash_key = ( CheckBoxPreference ) sps.findPreference("isAutoFlash_key");        //サブカメラに切り替え
			dbMsg += ",オートフラッシュ=" + isAutoFlash;
			if ( findPreference("isAutoFlash_key") != null ) {
				isAutoFlash_key.setChecked(isAutoFlash);
//				if(isAutoFlash){
//					summaryStr +=","+ getResources().getString(R.string.mm_phot_flash) ;
//				}
			}

			isRumbling_key = ( CheckBoxPreference ) sps.findPreference("isRumbling_key");        //サブカメラに切り替え
			dbMsg += ",シャッター音の鳴動=" + isRumbling;
			if ( findPreference("isRumbling_key") != null ) {
				isRumbling_key.setChecked(isRumbling);
//				if(isRumbling){
//					summaryStr +=","+ getResources().getString(R.string.mm_phot_rumbling) ;
//				}
			}

			isTexturView_key = ( CheckBoxPreference ) sps.findPreference("isTexturView_key");        //  = true;                 //
			dbMsg += ",高速プレビュー=" + isTexturView;
			if ( findPreference("isTexturView_key") != null ) {
				isTexturView_key.setChecked(isTexturView);
//				if(isTexturView){
//					summaryStr +=","+ getResources().getString(R.string.mm_effect_preview_tv) ;
//				} else{
//					summaryStr +=","+ getResources().getString(R.string.mm_effect_preview_sufece) ;
//				}
			}

			up_scale_key = ( EditTextPreference ) sps.findPreference("up_scale_key");            //顔から何割増しの枠で保存するか
			dbMsg += ",顔から何割増しの枠で保存するか=" + up_scale;
			if ( findPreference("up_scale_key") != null ) {
				up_scale_key.setDefaultValue(up_scale);
				up_scale_key.setSummary(up_scale);
			} else {
				(( EditTextPreference ) findPreference("up_scale_key")).setText(up_scale);
			}
//			phot_key.setSummary(summaryStr);

			video_key = ( PreferenceScreen ) sps.findPreference("video_key");                                //動画設定

			video_output_format_key = ( ListPreference ) sps.findPreference("video_output_format_key");    //
			dbMsg += ",出力フォーマット=" + vi_outputFormat;
			if ( findPreference("vi_outputFormat") != null ) {
				videoOutputArray = getResources().obtainTypedArray(R.array.video_output_array);
				String outputName = videoOutputArray.getString(vi_outputFormat);
				video_output_format_key.setSummary(outputName);
				video_output_format_key.setValue(outputName);
//				summaryStr = getResources().getString(R.string.video_output_format) + ":" + outputName;
			}

			video_rencoding_bit_rate_key = ( ListPreference ) sps.findPreference("video_rencoding_bit_rate_key");    //
			dbMsg += ",ビットレート=" + vi_videoEncodingBitRate;
			if ( findPreference("vi_videoEncodingBitRate") != null ) {
				bitRateeArray = getResources().obtainTypedArray(R.array.video_bit_rate_array);
				String videoEncodingBitRateeName = vi_videoEncodingBitRate + "";
				video_rencoding_bit_rate_key.setSummary(videoEncodingBitRateeName);
				video_rencoding_bit_rate_key.setValue(videoEncodingBitRateeName);
//				summaryStr += "," + getResources().getString(R.string.mm_video_rencoding_bit_rate) + ":" + videoEncodingBitRateeName;
			}

			video_rencoding_frame_rate_key = ( ListPreference ) sps.findPreference("video_rencoding_frame_rate_key");    //
			dbMsg += ",フレームレート=" + vi_videoFrameRate;
			if ( findPreference("vi_videoFrameRate") != null ) {
				videoFrameRateArray = getResources().obtainTypedArray(R.array.video_frame_rate_array);
				String videoFreamRateeName = vi_videoFrameRate + "";
				video_rencoding_frame_rate_key.setSummary(videoFreamRateeName);
				video_rencoding_frame_rate_key.setValue(videoFreamRateeName);
//				summaryStr += "," + getResources().getString(R.string.mm_video_frame_rate) + ":" + videoFreamRateeName;
			}

			video_audio_source_key = ( ListPreference ) sps.findPreference("video_audio_source_key");        //
			dbMsg += ",音声入力=" + vi_audioSource;
			if ( findPreference("vi_audioSource") != null ) {
				videoAudioSourceArray = getResources().obtainTypedArray(R.array.video_audio_source_array);
				String audioSourceName = videoAudioSourceArray.getString(vi_audioSource);
				video_audio_source_key.setSummary(audioSourceName);
				video_audio_source_key.setValue(audioSourceName);
//				summaryStr += "," + getResources().getString(R.string.audio_source) + ":" + audioSourceName;
			}

			video_data_source_key = ( ListPreference ) sps.findPreference("video_data_source_key");    //
			dbMsg += ",取得元=" + vi_videoSource;
			if ( findPreference("vi_videoSource") != null ) {
				videoDataSourceArray = getResources().obtainTypedArray(R.array.video_data_source_array);
				String datsSourceName = videoDataSourceArray.getString(vi_videoSource);
				video_data_source_key.setSummary(datsSourceName);
				video_data_source_key.setValue(datsSourceName);
//				summaryStr += "," + getResources().getString(R.string.video_source) + ":" + datsSourceName;
			}

			video_encorde_key = ( ListPreference ) sps.findPreference("video_encorde_key");    //
			dbMsg += ",ビデオエンコーダ=" + vi_videoEncoder;
			if ( findPreference("vi_outputFormat") != null ) {
				videoEncordeArray = getResources().obtainTypedArray(R.array.video_encorde_array);
				String videoEncordeName = videoEncordeArray.getString(vi_videoEncoder);
				video_encorde_key.setSummary(videoEncordeName);
				video_encorde_key.setValue(videoEncordeName);
//				summaryStr += "," + getResources().getString(R.string.video_videdo_encorde) + ":" + videoEncordeName;
			}

			video_audio_encorde_key = ( ListPreference ) sps.findPreference("video_audio_encorde_key");    //
			dbMsg += ",オーディオエンコーダ=" + vi_audioEncoder;
			if ( findPreference("vi_outputFormat") != null ) {
				audioEncordeArray = getResources().obtainTypedArray(R.array.video_audio_encorde_array);
				String audioEncordeName = audioEncordeArray.getString(vi_audioEncoder);
				video_audio_encorde_key.setSummary(audioEncordeName);
				video_audio_encorde_key.setValue(audioEncordeName);
				summaryStr += "," + getResources().getString(R.string.video_audio_encorde) + ":" + audioEncordeName;
			}
//			video_key.setSummary(summaryStr);

			effect_key = ( PreferenceScreen ) sps.findPreference("effect_key");        //エフェクト
			is_face_recognition_key = ( CheckBoxPreference ) sps.findPreference("is_face_recognition_key");        //顔検出実行中
			dbMsg += ",顔検出実行中=" + isFaceRecognition;
			if ( findPreference("is_face_recognition_key") != null ) {
				is_face_recognition_key.setChecked(isFaceRecognition);
			}

			is_overlap_rejection_key = ( CheckBoxPreference ) sps.findPreference("is_overlap_rejection_key");        // = true;     //
			dbMsg += ",重複棄却=" + is_overlap_rejection;
			if ( findPreference("is_overlap_rejection_key") != null ) {
				is_overlap_rejection_key.setChecked(is_overlap_rejection);
			}

			detector_select_key = ( PreferenceScreen ) sps.findPreference("detector_select_key");        //検出対象選択
			is_detector_frontal_face_alt_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontal_face_alt_key");
			dbMsg += ",顔検出(標準)=" + is_detector_frontal_face_alt;
			if ( findPreference("is_detector_frontal_face_alt_key") != null ) {
				is_detector_frontal_face_alt_key.setChecked(is_detector_frontal_face_alt);
			}

			is_detector_profileface_key = ( CheckBoxPreference ) sps.findPreference("is_detector_profileface_key");
			dbMsg += ",横顔=" + is_detector_profileface;
			if ( findPreference("is_detector_profileface_key") != null ) {
				is_detector_profileface_key.setChecked(is_detector_profileface);
			}

			is_detector_upperbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_upperbody_key");
			dbMsg += ",上半身=" + is_detector_upperbody;
			if ( findPreference("is_detector_upperbody_key") != null ) {
				is_detector_upperbody_key.setChecked(is_detector_upperbody);
			}

			is_detector_fullbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_fullbody_key");
			dbMsg += ",全身=" + is_detector_fullbody;
			if ( findPreference("is_detector_fullbody_key") != null ) {
				is_detector_fullbody_key.setChecked(is_detector_fullbody);
			}

			is_detector_lowerbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_lowerbody_key");
			dbMsg += ",下半身=" + is_detector_lowerbody;
			if ( findPreference("is_detector_lowerbody_key") != null ) {
				is_detector_lowerbody_key.setChecked(is_detector_lowerbody);
			}

			is_detector_eye_key = ( CheckBoxPreference ) sps.findPreference("is_detector_eye_key");
			dbMsg += ",目(標準)=" + is_detector_eye;
			if ( findPreference("is_detector_eye_key") != null ) {
				is_detector_eye_key.setChecked(is_detector_eye);
			}

			is_detector_righteye_2splits_key = ( CheckBoxPreference ) sps.findPreference("is_detector_righteye_2splits_key");
			dbMsg += ",右目=" + is_detector_righteye_2splits;
			if ( findPreference("is_detector_righteye_2splits_key") != null ) {
				is_detector_righteye_2splits_key.setChecked(is_detector_righteye_2splits);
			}

			is_detector_lefteye_2splitss_key = ( CheckBoxPreference ) sps.findPreference("is_detector_lefteye_2splitss_key");
			dbMsg += ",左目=" + is_detector_lefteye_2splits;
			if ( findPreference("is_detector_lefteye_2splitss_key") != null ) {
				is_detector_lefteye_2splitss_key.setChecked(is_detector_lefteye_2splits);
			}

			is_detector_eyeglasses_key = ( CheckBoxPreference ) sps.findPreference("is_detector_eyeglasses_key");
			dbMsg += ",眼鏡=" + is_detector_eyeglasses;
			if ( findPreference("is_detector_eyeglasses_key") != null ) {
				is_detector_eyeglasses_key.setChecked(is_detector_eyeglasses);
			}

			is_detector_frontalcatface_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalcatface_key");
			dbMsg += ",正面のみ？=" + is_detector_frontalcatface;
			if ( findPreference("is_detector_frontalcatface_key") != null ) {
				is_detector_frontalcatface_key.setChecked(is_detector_frontalcatface);
			}

			is_detector_frontalcatface_extended_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalcatface_extended_key");
			dbMsg += ",正面(拡張)？=" + is_detector_frontalcatface_extended;
			if ( findPreference("is_detector_frontalcatface_extended_key") != null ) {
				is_detector_frontalcatface_extended_key.setChecked(is_detector_frontalcatface_extended);
			}

			is_detector_frontalface_alt_tree_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_alt_tree_key");
			dbMsg += ",正面の顔高い木？=" + is_detector_frontalface_alt_tree;
			if ( findPreference("is_detector_frontalface_alt_tree_key") != null ) {
				is_detector_frontalface_alt_tree_key.setChecked(is_detector_frontalface_alt_tree);
			}

			is_detector_frontalface_alt2_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_alt2_key");
			dbMsg += ",正面顔全体2？=" + is_detector_frontalface_alt2;
			if ( findPreference("is_detector_frontalface_alt2_key") != null ) {
				is_detector_frontalface_alt2_key.setChecked(is_detector_frontalface_alt2);
			}

			is_detector_frontalface_default_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_default_key");
			dbMsg += ",正面デフォルト？=" + is_detector_frontalface_default;
			if ( findPreference("is_detector_frontalface_default_key") != null ) {
				is_detector_frontalface_default_key.setChecked(is_detector_frontalface_default);
			}

			is_detector_smile_key = ( CheckBoxPreference ) sps.findPreference("is_detector_smile_key");
			dbMsg += ",笑顔？=" + is_detector_smile;
			if ( findPreference("is_detector_smile_key") != null ) {
				is_detector_smile_key.setChecked(is_detector_smile);
			}

			is_detector_russian_plate_number_key = ( CheckBoxPreference ) sps.findPreference("is_detector_russian_plate_number_key");
			dbMsg += ",ナンバープレート・ロシア=" + is_detector_russian_plate_number;
			if ( findPreference("is_detector_russian_plate_number_key") != null ) {
				is_detector_russian_plate_number_key.setChecked(is_detector_russian_plate_number);
			}

			is_detector_ricence_plate_rus_16stages_key = ( CheckBoxPreference ) sps.findPreference("is_detector_ricence_plate_rus_16stages_key");
			dbMsg += ",ナンバープレートRUS=" + is_detector_ricence_plate_rus_16stages;
			if ( findPreference("is_detector_ricence_plate_rus_16stages_key") != null ) {
				is_detector_ricence_plate_rus_16stages_key.setChecked(is_detector_ricence_plate_rus_16stages);
			}

			is_chase_focus_key = ( CheckBoxPreference ) sps.findPreference("is_chase_focus_key");        //追跡フォーカス
			dbMsg += ",追跡フォーカス=" + isChaseFocus;
			if ( findPreference("is_chase_focus_key") != null ) {
				is_chase_focus_key.setChecked(isChaseFocus);
			}

			other_key = ( PreferenceScreen ) sps.findPreference("other_key");        //その他
			write_folder_key = ( EditTextPreference ) sps.findPreference("write_folder_key");        //書込みルートフォルダ
			dbMsg += ",書込みルートフォルダ=" + write_folder;
			if ( findPreference("write_folder_key") != null ) {
				write_folder_key.setDefaultValue(write_folder + "");
				write_folder_key.setSummary(write_folder + "");
				dbMsg += "更新";
			} else {
				(( EditTextPreference ) findPreference("write_folder_key")).setText(write_folder);
				dbMsg += "追加";
			}
			dbMsg += ",顔認証プロファイルの最新更新日=" + haarcascades_last_modified;
			haarcascades_last_modified_key = ( Preference ) sps.findPreference("haarcascades_last_modified_key");            //顔認証プロファイルの最新更新日
			if ( findPreference("haarcascades_last_modified_key") != null ) {
				haarcascades_last_modified_key.setDefaultValue(haarcascades_last_modifiedStr);
				haarcascades_last_modified_key.setSummary(haarcascades_last_modifiedStr);
				dbMsg += "更新";
			} else {
				(( Preference ) findPreference("haarcascades_last_modified_key")).setSummary(haarcascades_last_modifiedStr);
				dbMsg += "追加";
			}
			reloadSummary();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
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
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
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
		String dbMsg = "設定変更";/////////////////////////////////////////////////
		try {
			reloadSummary();
			myLog(TAG , dbMsg);
		} catch (Exception e) {
			Log.e(TAG , dbMsg + "で" + e.toString());
			//      myLog(TAG, dbMsg + "で" + e.toString(), "e");
		}
	}

	//各項目のプリファレンス上の設定///////////////////////////////////////////////////////////////////////////

	/**
	 * https://qiita.com/noppefoxwolf/items/18e785f4d760f7cc4314
	 * 一階層目しか反映されていない
	 */
	private void reloadSummary() {
		String TAG = "reloadSummary[MPF]";
		String dbMsg = "";
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
					dbMsg += "EditTextPreference;";
					EditTextPreference pref = ( EditTextPreference ) item;
					String key = pref.getKey();
					String val = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getString(key , "");
					dbMsg += ";" + key + ";" + val;
					if ( key.equals(up_scale_key) ) {
						dbMsg += ",up_scale=" + val;
						CS_Util UTIL = new CS_Util();
						if ( UTIL.isFloatVal(val) ) {
							val = Float.parseFloat(val) + "";
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
					dbMsg += "CheckBoxPreference;";
					CheckBoxPreference pref = ( CheckBoxPreference ) item;
					String key = pref.getKey();
					boolean pVal = pref.isChecked();
					dbMsg += ";" + key + ";" + pVal;
					if ( key.equals("isTexturView_key") ) {
						pref.setSummaryOn(getResources().getString(R.string.mm_effect_preview_tv));
						pref.setSummaryOff(getResources().getString(R.string.mm_effect_preview_sufece));
					} else {
						pref.setSummaryOn("現在 On");                        // CheckBox が On の時のサマリーを設定
						pref.setSummaryOff("現在 Off");                        // CheckBox が Off の時のサマリーを設定
					}
				} else if ( item instanceof ListPreference ) {
					dbMsg += "ListPreference;";
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
				} else if ( item instanceof PreferenceScreen ) {
					dbMsg += "PreferenceScreen;";
					PreferenceScreen pref = ( PreferenceScreen ) item;
					String key = pref.getKey();
					dbMsg += ";" + key;
					//     List<String> vals = new ArrayList<String>();
//					List< String > keyList = new ArrayList< String >();
					String wrString = "";
					if ( key.equals("phot_key") ) {
//						PreferenceScreen phot_key = ( PreferenceScreen ) sps.findPreference("phot_key");        //サブカメラに切り替え
//						isSubCamera_key = ( CheckBoxPreference ) sps.findPreference("isSubCamera_key");        //サブカメラに切り替え
						dbMsg += ",サブカメラに切り替え=" + isSubCamera;
//						if ( findPreference("isSubCamera_key") != null ) {
//							isSubCamera_key.setChecked(isSubCamera);
//						}
						if ( isSubCamera ) {
							wrString += getResources().getString(R.string.sub_camera);
						} else {
							wrString += getResources().getString(R.string.main_camera);
						}
						dbMsg += ",オートフラッシュ=" + isAutoFlash;
//						isAutoFlash_key = ( CheckBoxPreference ) sps.findPreference("isAutoFlash_key");        //サブカメラに切り替え
//						if ( findPreference("isAutoFlash_key") != null ) {
//							isAutoFlash_key.setChecked(isAutoFlash);
//						}
						if ( isAutoFlash ) {
							wrString += "," + getResources().getString(R.string.mm_phot_flash) + " : " + "On";
						}
						dbMsg += ",シャッター音の鳴動=" + isRumbling;
//						isRumbling_key = ( CheckBoxPreference ) sps.findPreference("isRumbling_key");        //サブカメラに切り替え
//						if ( findPreference("isRumbling_key") != null ) {
//							isRumbling_key.setChecked(isRumbling);
//						}
						if ( isRumbling ) {
							wrString += "," + getResources().getString(R.string.mm_phot_rumbling) + " : " + "On";
						}
						dbMsg += ",高速プレビュー=" + isTexturView;
//						isTexturView_key = ( CheckBoxPreference ) sps.findPreference("isTexturView_key");        //  = true;                 //
//						if ( findPreference("isTexturView_key") != null ) {
//							isTexturView_key.setChecked(isTexturView);
//						}
						if ( isTexturView ) {
							wrString += "\n" + getResources().getString(R.string.mm_effect_preview_tv);
						} else {
							wrString += "\n" + getResources().getString(R.string.mm_effect_preview_sufece);
						}
						dbMsg += ",顔から何割増しの枠で保存するか=" + up_scale;
//						up_scale_key = ( EditTextPreference ) sps.findPreference("up_scale_key");            //顔から何割増しの枠で保存するか
//						if ( findPreference("up_scale_key") != null ) {
//							up_scale_key.setDefaultValue(up_scale);
//							up_scale_key.setSummary(up_scale);
//						} else {
//							(( EditTextPreference ) findPreference("up_scale_key")).setText(up_scale);
//						}
						wrString += "\n" + getResources().getString(R.string.up_scale) + ";" + up_scale;
// 						phot_key.setSummary(wrString);
					} else if ( key.equals("effect_key") ) {
						dbMsg += ",顔検出実行中=" + isFaceRecognition;
//						is_face_recognition_key = ( CheckBoxPreference ) sps.findPreference("is_face_recognition_key");        //顔検出実行中
//						if ( findPreference("is_face_recognition_key") != null ) {
//							is_face_recognition_key.setChecked(isFaceRecognition);
//						}
						wrString += getResources().getString(R.string.mm_effect_face_recgnition);
						if ( isFaceRecognition ) {
							wrString += getResources().getString(R.string.mm_effect_in_process);
						} else {
							wrString += getResources().getString(R.string.mm_effect_under_suspension);
						}
						dbMsg += ",重複棄却=" + is_overlap_rejection;
//						is_overlap_rejection_key = ( CheckBoxPreference ) sps.findPreference("is_overlap_rejection_key");        // = true;     //
//						if ( findPreference("is_overlap_rejection_key") != null ) {
//							is_overlap_rejection_key.setChecked(is_overlap_rejection);
//						}
						if ( is_overlap_rejection ) {
							wrString += getResources().getString(R.string.mm_effect_in_process);
						} else {
							wrString += getResources().getString(R.string.mm_effect_under_suspension);
						}
						dbMsg += "\n追跡フォーカス=" + isChaseFocus;
//						is_chase_focus_key = ( CheckBoxPreference ) sps.findPreference("is_chase_focus_key");        //追跡フォーカス
//						if ( findPreference("is_chase_focus_key") != null ) {
//							is_chase_focus_key.setChecked(isChaseFocus);
//						}
						wrString += "," + getResources().getString(R.string.mm_effect_chase_focus);
						if ( isChaseFocus ) {
							wrString += getResources().getString(R.string.mm_effect_preview_tv);
						} else {
							wrString += getResources().getString(R.string.mm_effect_preview_sufece);
						}

					} else if ( key.equals("video_key") ) {
						dbMsg += ",動画設定;出力フォーマット=" + vi_outputFormat;
						videoOutputArray = getResources().obtainTypedArray(R.array.video_output_array);
						String wrVal = videoOutputArray.getString(vi_outputFormat);
						wrString = getResources().getString(R.string.video_output_format)+ ":"+ wrVal;
						dbMsg += ",ビットレート=" + vi_videoEncodingBitRate;
						wrString += "," + vi_videoEncodingBitRate + "bps";
						dbMsg += ",フレームレート=" + vi_videoFrameRate;
						wrString += "," + vi_videoFrameRate + "fps";

						dbMsg += ",音声入力=" + vi_audioSource;
						videoAudioSourceArray = getResources().obtainTypedArray(R.array.video_audio_source_array);
						wrVal = videoAudioSourceArray.getString(vi_audioSource);
						wrString += "," + getResources().getString(R.string.audio_source)+ ":"+ wrVal;

						dbMsg += ",取得元=" + vi_videoSource;
						videoDataSourceArray = getResources().obtainTypedArray(R.array.video_data_source_array);
						wrVal= videoDataSourceArray.getString(vi_videoSource);
						wrString += "," + getResources().getString(R.string.video_source)+ ":"+ wrVal;
						dbMsg += ",ビデオエンコーダ=" + vi_videoEncoder;
						videoEncordeArray = getResources().obtainTypedArray(R.array.video_encorde_array);
						wrVal= videoEncordeArray.getString(vi_videoEncoder);
						wrString += "," + getResources().getString(R.string.video_videdo_encorde)+ ":"+ wrVal;
						dbMsg += ",オーディオエンコーダ=" + vi_audioEncoder;
						audioEncordeArray = getResources().obtainTypedArray(R.array.video_audio_encorde_array);
						wrVal = audioEncordeArray.getString(vi_audioEncoder);
						wrString += "," + getResources().getString(R.string.video_audio_encorde)+ ":"+ wrVal;

					} else if ( key.equals("detector_select_key") ) {
						dbMsg += ",顔検出(標準)=" + is_detector_frontal_face_alt;
//						detector_select_key = ( PreferenceScreen ) sps.findPreference("detector_select_key");        //検出対象選択
//						is_detector_frontal_face_alt_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontal_face_alt_key");
//						if ( findPreference("is_detector_frontal_face_alt_key") != null ) {
//							is_detector_frontal_face_alt_key.setChecked(is_detector_frontal_face_alt);
//						}
						if ( is_detector_frontal_face_alt ) {
							wrString += "," + getResources().getString(R.string.mm_detector_frontal_face_alt);
						}
						dbMsg += ",横顔=" + is_detector_profileface;
//						is_detector_profileface_key = ( CheckBoxPreference ) sps.findPreference("is_detector_profileface_key");
//						if ( findPreference("is_detector_profileface_key") != null ) {
//							is_detector_profileface_key.setChecked(is_detector_profileface);
//						}
						if ( is_detector_profileface ) {
							wrString += "," + getResources().getString(R.string.mm_detector_profileface);
						}
						dbMsg += ",上半身=" + is_detector_upperbody;
//						is_detector_upperbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_upperbody_key");
//						if ( findPreference("is_detector_upperbody_key") != null ) {
//							is_detector_upperbody_key.setChecked(is_detector_upperbody);
//						}
						if ( is_detector_upperbody ) {
							wrString += "," + getResources().getString(R.string.mm_detector_upperbody);
						}
						dbMsg += ",全身=" + is_detector_fullbody;
//						is_detector_fullbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_fullbody_key");
//						if ( findPreference("is_detector_fullbody_key") != null ) {
//							is_detector_fullbody_key.setChecked(is_detector_fullbody);
//						}
						if ( is_detector_fullbody ) {
							wrString += "," + getResources().getString(R.string.mm_detector_fullbody);
						}
						dbMsg += ",下半身=" + is_detector_lowerbody;
//						is_detector_lowerbody_key = ( CheckBoxPreference ) sps.findPreference("is_detector_lowerbody_key");
//						if ( findPreference("is_detector_lowerbody_key") != null ) {
//							is_detector_lowerbody_key.setChecked(is_detector_lowerbody);
//						}
						if ( is_detector_lowerbody ) {
							wrString += "," + getResources().getString(R.string.mm_detector_lowerbody);
						}
						dbMsg += ",正面のみ？=" + is_detector_frontalcatface;
//						is_detector_frontalcatface_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalcatface_key");
//						if ( findPreference("is_detector_frontalcatface_key") != null ) {
//							is_detector_frontalcatface_key.setChecked(is_detector_frontalcatface);
//						}
						if ( is_detector_frontalcatface ) {
							wrString += "," + getResources().getString(R.string.mm_detector_frontalcatface);
						}
						dbMsg += ",正面(拡張)？=" + is_detector_frontalcatface_extended;
//						is_detector_frontalcatface_extended_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalcatface_extended_key");
//						if ( findPreference("is_detector_frontalcatface_extended_key") != null ) {
//							is_detector_frontalcatface_extended_key.setChecked(is_detector_frontalcatface_extended);
//						}
						if ( is_detector_frontalcatface_extended ) {
							wrString += "," + getResources().getString(R.string.mm_detector_frontalcatface_extended);
						}
						dbMsg += ",正面顔全体2？=" + is_detector_frontalface_alt2;
//						is_detector_frontalface_alt2_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_alt2_key");
//						if ( findPreference("is_detector_frontalface_alt2_key") != null ) {
//							is_detector_frontalface_alt2_key.setChecked(is_detector_frontalface_alt2);
//						}
						if ( is_detector_frontalface_alt2 ) {
							wrString += "," + getResources().getString(R.string.mm_detector_frontalface_alt2);
						}
						dbMsg += ",正面デフォルト？=" + is_detector_frontalface_default;
//						is_detector_frontalface_default_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_default_key");
//						if ( findPreference("is_detector_frontalface_default_key") != null ) {
//							is_detector_frontalface_default_key.setChecked(is_detector_frontalface_default);
//						}
						if ( is_detector_frontalface_default ) {
							wrString += "," + getResources().getString(R.string.mm_detector_frontalface_default);
						}
						dbMsg += ",笑顔？=" + is_detector_smile;
//						is_detector_smile_key = ( CheckBoxPreference ) sps.findPreference("is_detector_smile_key");
//						if ( findPreference("is_detector_smile_key") != null ) {
//							is_detector_smile_key.setChecked(is_detector_smile);
//						}
						if ( is_detector_smile ) {
							wrString += "," + getResources().getString(R.string.mm_detector_frontalface_default);
						}
						dbMsg += ",ナンバープレート・ロシア=" + is_detector_russian_plate_number;
//						is_detector_russian_plate_number_key = ( CheckBoxPreference ) sps.findPreference("is_detector_russian_plate_number_key");
//						if ( findPreference("is_detector_russian_plate_number_key") != null ) {
//							is_detector_russian_plate_number_key.setChecked(is_detector_russian_plate_number);
//						}
						if ( is_detector_russian_plate_number ) {
							wrString += "," + getResources().getString(R.string.mm_detector_russian_plate_number);
						}
						dbMsg += ",ナンバープレートRUS=" + is_detector_ricence_plate_rus_16stages;
//						is_detector_ricence_plate_rus_16stages_key = ( CheckBoxPreference ) sps.findPreference("is_detector_ricence_plate_rus_16stages_key");
//						if ( findPreference("is_detector_ricence_plate_rus_16stages_key") != null ) {
//							is_detector_ricence_plate_rus_16stages_key.setChecked(is_detector_ricence_plate_rus_16stages);
//						}
						if ( is_detector_ricence_plate_rus_16stages ) {
							wrString += "," + getResources().getString(R.string.mm_detector_ricence_plate_rus_16stages);
						}
						dbMsg += ",目(標準)=" + is_detector_eye;
//						is_detector_eye_key = ( CheckBoxPreference ) sps.findPreference("is_detector_eye_key");
//						if ( findPreference("is_detector_eye_key") != null ) {
//							is_detector_eye_key.setChecked(is_detector_eye);
//						}
						if ( is_detector_eye ) {
							wrString += "," + getResources().getString(R.string.mm_detector_eye);
						}
						dbMsg += ",右目=" + is_detector_righteye_2splits;
//						is_detector_righteye_2splits_key = ( CheckBoxPreference ) sps.findPreference("is_detector_righteye_2splits_key");
//						if ( findPreference("is_detector_righteye_2splits_key") != null ) {
//							is_detector_righteye_2splits_key.setChecked(is_detector_righteye_2splits);
//						}
						if ( is_detector_righteye_2splits ) {
							wrString += "," + getResources().getString(R.string.mm_detector_righteye_2splits);
						}
						dbMsg += ",左目=" + is_detector_lefteye_2splits;
//						is_detector_lefteye_2splitss_key = ( CheckBoxPreference ) sps.findPreference("is_detector_lefteye_2splitss_key");
//						if ( findPreference("is_detector_lefteye_2splitss_key") != null ) {
//							is_detector_lefteye_2splitss_key.setChecked(is_detector_lefteye_2splits);
//						}
						if ( is_detector_lefteye_2splits ) {
							wrString += "," + getResources().getString(R.string.mm_detector_lefteye_2splits);
						}
						dbMsg += ",眼鏡=" + is_detector_eyeglasses;
//						is_detector_eyeglasses_key = ( CheckBoxPreference ) sps.findPreference("is_detector_eyeglasses_key");
//						if ( findPreference("is_detector_eyeglasses_key") != null ) {
//							is_detector_eyeglasses_key.setChecked(is_detector_eyeglasses);
//						}
						if ( is_detector_eyeglasses ) {
							wrString += "," + getResources().getString(R.string.mm_detector_eyeglasses);
						}
						dbMsg += ",正面の顔高い木？=" + is_detector_frontalface_alt_tree;
//						is_detector_frontalface_alt_tree_key = ( CheckBoxPreference ) sps.findPreference("is_detector_frontalface_alt_tree_key");
//						if ( findPreference("is_detector_frontalface_alt_tree_key") != null ) {
//							is_detector_frontalface_alt_tree_key.setChecked(is_detector_frontalface_alt_tree);
//						}
						if ( is_detector_frontalface_alt_tree ) {
							wrString += "," + getResources().getString(R.string.mm_detector_frontalface_alt_tree);
						}
					} else if ( key.equals("other_key") ) {
//						other_key = ( PreferenceScreen ) sps.findPreference("other_key");        //その他
//						write_folder_key = ( EditTextPreference ) sps.findPreference("service_id_key");        //書込みルートフォルダ
						dbMsg += ",書込みルートフォルダ=" + write_folder;
						wrString += "," + getResources().getString(R.string.write_folder) + ":" + write_folder;
						dbMsg += ",顔認証プロファイルの最新更新日=" + haarcascades_last_modified;
//						if ( findPreference("write_folder_key") != null ) {
//							write_folder_key.setDefaultValue(write_folder);
//							write_folder_key.setSummary(write_folder);
//						} else {
//							(( EditTextPreference ) findPreference("write_folder_key")).setText(write_folder);
//						}
////						if ( findPreference("haarcascades_last_modified_key") != null ) {
//							haarcascades_last_modified_key.setDefaultValue(haarcascades_last_modified);
//							haarcascades_last_modified_key.setSummary(haarcascades_last_modified);
						wrString += "\n" + getResources().getString(R.string.haarcascades_last_modified) + ":" + haarcascades_last_modifiedStr;
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
					dbMsg += ",wrString=" + wrString;
					pref.setSummary(wrString);
				} else if ( item instanceof Preference ) {
					dbMsg += "Preference;";
					Preference pref = ( Preference ) item;
					String key = pref.getKey();
					String pVal = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getString(key , "");
					dbMsg += ";" + key + ";" + pVal;
					if ( key.equals("haarcascades_last_modified_key") ) {
						pVal = haarcascades_last_modifiedStr;
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
					}
					dbMsg += ";" + key + ";" + pVal;
					pref.setSummary(pVal);
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
			dbMsg += ",読み込み開始;keys=" + keys.size() + "件";        // keys.size()
			int i = 0;
			for ( String key : keys.keySet() ) {
				i++;
				dbMsg += "\n" + i + "/" + keys.size() + ")" + key;// + "は" + rStr;
				if ( key.equals("is_face_recognition_key") ) {
					isFaceRecognition = sharedPref.getBoolean(key , isFaceRecognition);
					dbMsg += ",顔検出実行中=" + isFaceRecognition;
				} else if ( key.equals("is_overlap_rejection_key") ) {
					is_overlap_rejection = sharedPref.getBoolean(key , is_overlap_rejection);
					dbMsg += ",重複棄却=" + is_overlap_rejection;
				} else if ( key.equals("isTexturView_key") ) {
					isTexturView = sharedPref.getBoolean(key , isTexturView);
					dbMsg += ",高速プレビュー=" + isTexturView;
				} else if ( key.equals("is_detector_frontal_face_alt_key") ) {
					is_detector_frontal_face_alt = sharedPref.getBoolean(key , is_detector_frontal_face_alt);
					dbMsg += ",顔検出(標準)=" + is_detector_frontal_face_alt;
				} else if ( key.equals("is_detector_profileface_key") ) {
					is_detector_profileface = sharedPref.getBoolean(key , is_detector_profileface);
					dbMsg += ",横顔=" + is_detector_profileface;
				} else if ( key.equals("is_detector_upperbody_key") ) {
					is_detector_upperbody = sharedPref.getBoolean(key , is_detector_upperbody);
					dbMsg += ",上半身=" + is_detector_upperbody;
				} else if ( key.equals("is_detector_fullbody_key") ) {
					is_detector_fullbody = sharedPref.getBoolean(key , is_detector_fullbody);
					dbMsg += ",全身=" + is_detector_fullbody;
				} else if ( key.equals("is_detector_lowerbody_key") ) {
					is_detector_lowerbody = sharedPref.getBoolean(key , is_detector_lowerbody);
					dbMsg += ",下半身=" + is_detector_lowerbody;
				} else if ( key.equals("is_detector_eye_key") ) {
					is_detector_eye = sharedPref.getBoolean(key , is_detector_eye);
					dbMsg += ",目(標準)=" + is_detector_eye;
				} else if ( key.equals("is_detector_righteye_2splits_key") ) {
					is_detector_righteye_2splits = sharedPref.getBoolean(key , is_detector_righteye_2splits);
					dbMsg += ",右目=" + is_detector_righteye_2splits;
				} else if ( key.equals("is_detector_lefteye_2splitss_key") ) {
					is_detector_lefteye_2splits = sharedPref.getBoolean(key , is_detector_lefteye_2splits);
					dbMsg += ",左目=" + is_detector_lefteye_2splits;
				} else if ( key.equals("is_detector_eyeglasses_key") ) {
					is_detector_eyeglasses = sharedPref.getBoolean(key , is_detector_eyeglasses);
					dbMsg += ",眼鏡=" + is_detector_eyeglasses;
				} else if ( key.equals("is_detector_frontalcatface_key") ) {
					is_detector_frontalcatface = sharedPref.getBoolean(key , is_detector_frontalcatface);
					dbMsg += ",正面のみ=" + is_detector_frontalcatface;
				} else if ( key.equals("is_detector_frontalcatface_extended_key") ) {
					is_detector_frontalcatface_extended = sharedPref.getBoolean(key , is_detector_frontalcatface_extended);
					dbMsg += ",正面(拡張)=" + is_detector_frontalcatface_extended;
				} else if ( key.equals("is_detector_frontalface_alt_tree_key") ) {
					is_detector_frontalface_alt_tree = sharedPref.getBoolean(key , is_detector_frontalface_alt_tree);
					dbMsg += ",正面の顔高い木？)=" + is_detector_frontalface_alt_tree;
				} else if ( key.equals("is_detector_frontalface_alt2_key") ) {
					is_detector_frontalface_alt2 = sharedPref.getBoolean(key , is_detector_frontalface_alt2);
					dbMsg += ",正面顔全体2=" + is_detector_frontalface_alt2;
				} else if ( key.equals("is_detector_frontalface_default_key") ) {
					is_detector_frontalface_default = sharedPref.getBoolean(key , is_detector_frontalface_default);
					dbMsg += ",正面デフォルト=" + is_detector_frontalface_default;
				} else if ( key.equals("is_detector_smile_key") ) {
					is_detector_smile = sharedPref.getBoolean(key , is_detector_smile);
					dbMsg += ",笑顔=" + is_detector_smile;
				} else if ( key.equals("is_detector_russian_plate_number_key") ) {
					is_detector_russian_plate_number = sharedPref.getBoolean(key , is_detector_russian_plate_number);
					dbMsg += ",ナンバープレート・ロシア=" + is_detector_russian_plate_number;
				} else if ( key.equals("is_detector_ricence_plate_rus_16stages_key") ) {
					is_detector_ricence_plate_rus_16stages = sharedPref.getBoolean(key , is_detector_ricence_plate_rus_16stages);
					dbMsg += ",ナンバープレートRUS=" + is_detector_ricence_plate_rus_16stages;
				} else if ( key.equals("is_chase_focus_key") ) {
					isChaseFocus = sharedPref.getBoolean(key , isChaseFocus);
					dbMsg += ",追跡フォーカス=" + isChaseFocus;
				} else if ( key.equals("isSubCamera_key") ) {
					isSubCamera = sharedPref.getBoolean(key , isSubCamera);
					dbMsg += ",サブカメラに切り替え=" + isSubCamera;
				} else if ( key.equals("isAutoFlash_key") ) {
					isAutoFlash = sharedPref.getBoolean(key , isAutoFlash);
					dbMsg += ",オートフラッシュ=" + isAutoFlash;
				} else if ( key.equals("isRumbling_key") ) {
					isRumbling = sharedPref.getBoolean(key , isRumbling);
					dbMsg += ",シャッター音の鳴動=" + isRumbling;
				} else if ( key.equals("write_folder_key") ) {
					write_folder = sharedPref.getString(key , write_folder);
					dbMsg += ",書込みルートフォルダ=" + write_folder;
				} else if ( key.equals("up_scale_key") ) {
					up_scale = sharedPref.getString(key , up_scale);
					dbMsg += ",顔から何割増しの枠で保存するか=" + up_scale;

				} else if ( key.equals("video_output_format_key") ) {
					vi_outputFormat = sharedPref.getInt(key,vi_outputFormat);
//					vi_outputFormat = Integer.parseInt(sharedPref.getString(key , vi_outputFormat + ""));
					dbMsg += ",出力フォーマット=" + vi_outputFormat;
				} else if ( key.equals("video_rencoding_bit_rate_key") ) {
					vi_videoEncodingBitRate = sharedPref.getInt(key,vi_videoEncodingBitRate);
//					vi_videoEncodingBitRate = Integer.parseInt(sharedPref.getString(key , vi_videoEncodingBitRate + ""));
					dbMsg += ",ビットレート=" + vi_videoEncodingBitRate;
				} else if ( key.equals("video_rencoding_frame_rate_key") ) {
					vi_videoFrameRate = sharedPref.getInt(key,vi_videoFrameRate);
//					vi_videoFrameRate = Integer.parseInt(sharedPref.getString(key , vi_videoFrameRate + ""));
					dbMsg += ",フレームレート=" + vi_videoFrameRate;
				} else if ( key.equals("video_audio_source_key") ) {
					vi_audioSource = sharedPref.getInt(key,vi_audioSource);
//					vi_audioSource = Integer.parseInt(sharedPref.getString(key , vi_audioSource + ""));
					dbMsg += ",音声入力=" + vi_audioSource;
				} else if ( key.equals("video_data_source_key") ) {
					vi_videoSource = sharedPref.getInt(key,vi_videoSource);
//					vi_videoSource = Integer.parseInt(sharedPref.getString(key , vi_videoSource + ""));
					dbMsg += ",取得元=" + vi_videoSource;
				} else if ( key.equals("video_encorde_key") ) {
					vi_videoEncoder = sharedPref.getInt(key,vi_videoEncoder);
//					vi_videoEncoder = Integer.parseInt(sharedPref.getString(key , vi_videoEncoder + ""));
					dbMsg += ",ビデオエンコーダ=" + vi_videoEncoder;   //java.lang.NumberFormatException: For input string: "H264"
				} else if ( key.equals("video_audio_encorde_key") ) {
					vi_audioEncoder = sharedPref.getInt(key,vi_audioEncoder);
//					vi_audioEncoder = Integer.parseInt(sharedPref.getString(key , vi_audioEncoder + ""));
					dbMsg += ",オーディオエンコーダ=" + vi_audioEncoder;

				} else if ( key.equals("haarcascades_last_modified_key") ) {
					haarcascades_last_modified = sharedPref.getString(key , haarcascades_last_modified);
					dbMsg += ",顔認証プロファイルの最新更新日=" + haarcascades_last_modifiedStr;
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
			dbMsg += ",端末内の保存先=" + write_folder;
			if ( write_folder.isEmpty() ) {
				File photDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				//              //自分のアプリ用の内部ディレクトリ    context.getFilesDir();
				dbMsg += ",photDir=" + photDir.getPath() + File.separator;      //pathSeparatorは：
				write_folder = photDir.getPath() + File.separator + context.getResources().getString(R.string.app_name);
				dbMsg += ",>>" + write_folder;
				myEditor.putString("write_folder_key" , write_folder);
				dbMsg += ",更新";
				myEditor.commit();
				dbMsg += "完了";
			}
//			String local_dir_size = userDir.getFreeSpace() + "";// "5000000";
//			dbMsg +=+ ",保存先の空き容量=" + local_dir_size;
//			if ( local_dir_size.isEmpty() ) {
//				local_dir_size = "5000000";
//				dbMsg +=+ ">>" + local_dir_size;
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
				CS_Util UTIL = new CS_Util();
				haarcascades_last_modifiedStr = UTIL.retDateStr(haarcascadesLastMdified , "yyyy年MM月dd日 hh:mm:ss");
			}
			dbMsg += ",>最終更新日>" + haarcascades_last_modifiedStr;
			myEditor.putString("haarcascades_last_modified_key" , haarcascadesLastMdified + "");
			dbMsg += ",更新";
			myEditor.commit();
			dbMsg += "完了";
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}            //顔認証プロファイルの情報

	//Video/////////////////////////////////////////////////////////////////////////////////////////////////////
	public int setVideoAudioSource(int selectIndex) {
		final String TAG = "setVideoAudioSource[MPF]";
		String dbMsg = "開始";
		int retInt = MediaRecorder.AudioSource.DEFAULT;            //0
		try {
			dbMsg += ",selectIndex=" + selectIndex;
			switch ( selectIndex ) {
				case 1:
					retInt = MediaRecorder.AudioSource.MIC;
					break;
				case 2:
					retInt = MediaRecorder.AudioSource.VOICE_UPLINK;
					break;
				case 3:
					retInt = MediaRecorder.AudioSource.VOICE_DOWNLINK;
					break;
				case 4:
					retInt = MediaRecorder.AudioSource.VOICE_CALL;
					break;
				case 5:
					retInt = MediaRecorder.AudioSource.CAMCORDER;
					break;
				case 6:
					retInt = MediaRecorder.AudioSource.VOICE_RECOGNITION;
					break;
				case 7:
					retInt = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
					break;
				case 8:
					retInt = MediaRecorder.AudioSource.REMOTE_SUBMIX;
					break;
				case 9:
					retInt = MediaRecorder.AudioSource.UNPROCESSED;
					break;
			}
			dbMsg += ",retInt=" + retInt;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
		}
		return retInt;
	}

	public int setVideoVideoSource(int selectIndex) {
		final String TAG = "setVideoVideoSource[MPF]";
		String dbMsg = "開始";
		int retInt = MediaRecorder.VideoSource.DEFAULT;        //0
		try {
			dbMsg += ",selectIndex=" + selectIndex;
			switch ( selectIndex ) {
				case 1:
					retInt = MediaRecorder.VideoSource.CAMERA;
					break;
				case 2:
					retInt = MediaRecorder.VideoSource.SURFACE;
					break;
			}
			dbMsg += ",retInt=" + retInt;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
		}
		return retInt;
	}

	public int setVideOutputFormat(int selectIndex) {
		final String TAG = "setVideOutputFormat[MPF]";
		String dbMsg = "開始";
		int retInt = MediaRecorder.OutputFormat.DEFAULT;        //0
		try {
			dbMsg += ",selectIndex=" + selectIndex;
			switch ( selectIndex ) {
				case 1:
					retInt = MediaRecorder.OutputFormat.THREE_GPP;
					break;
				case 2:
					retInt = MediaRecorder.OutputFormat.MPEG_4;
					break;
				case 3:                     //3  RAW_AMRから変更
					retInt = MediaRecorder.OutputFormat.AMR_NB;
					break;
				case 4:
					retInt = MediaRecorder.OutputFormat.AMR_WB;
					break;
				case 5:
					retInt = MediaRecorder.OutputFormat.AAC_ADTS;        //6(5は欠番)
					break;
				case 6:
					retInt = MediaRecorder.OutputFormat.MPEG_2_TS;        //8(7は欠番)
					break;
				case 7:
					retInt = MediaRecorder.OutputFormat.WEBM;        //9
					break;
			}
			dbMsg += ",retInt=" + retInt;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
		}
		return retInt;
	}

	public int setVideoVideoEncoder(int selectIndex) {
		final String TAG = "setVideoVideoEncoder[MPF]";
		String dbMsg = "開始";
		int retInt = MediaRecorder.VideoEncoder.DEFAULT;        //0
		try {
			dbMsg += ",selectIndex=" + selectIndex;
			switch ( selectIndex ) {
				case 1:
					retInt = MediaRecorder.VideoEncoder.H263;
					break;
				case 2:
					retInt = MediaRecorder.VideoEncoder.H264;
					break;
				case 3:
					retInt = MediaRecorder.VideoEncoder.MPEG_4_SP;
					break;
				case 4:
					retInt = MediaRecorder.VideoEncoder.VP8;
					break;
				case 5:
					retInt = MediaRecorder.VideoEncoder.HEVC;
					break;
			}
			dbMsg += ",retInt=" + retInt;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
		}
		return retInt;
	}

	public int setVideoAudioEncoder(int selectIndex) {
		final String TAG = "setVideoAudioEncoder[MPF]";
		String dbMsg = "開始";
		int retInt = MediaRecorder.AudioEncoder.DEFAULT;            //0
		try {
			dbMsg += ",selectIndex=" + selectIndex;
			switch ( selectIndex ) {
				case 1:
					retInt = MediaRecorder.AudioEncoder.AMR_NB;
					break;
				case 2:
					retInt = MediaRecorder.AudioEncoder.AMR_WB;
					break;
				case 3:
					retInt = MediaRecorder.AudioEncoder.AAC;
					break;
				case 4:
					retInt = MediaRecorder.AudioEncoder.HE_AAC;
					break;
				case 5:
					retInt = MediaRecorder.AudioEncoder.AAC_ELD;
					break;
				case 6:
					retInt = MediaRecorder.AudioEncoder.VORBIS;
					break;
			}
			dbMsg += ",retInt=" + retInt;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + "でエラー発生；" + er);
		}
		return retInt;
	}

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