package com.hijiyam_koubou.kaodori;

// Copyright 2015 kotemaru.org. (http://www.apache.org/licenses/LICENSE-2.0)


import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

public class Camera2StateMachine {
	private static final String TAG = Camera2StateMachine.class.getSimpleName();
	private Activity activity;


	private CameraManager mCameraManager;

	private CameraDevice mCameraDevice;
	private CameraCaptureSession mCaptureSession;
	private ImageReader mImageReader;
	private CaptureRequest.Builder mPreviewRequestBuilder;

	private AutoFitTextureView mTextureView;
	private Handler mHandler = null; // default current thread.
	private State mState = null;
	private ImageReader.OnImageAvailableListener mTakePictureListener;

	public void open(Activity activity , AutoFitTextureView textureView) {
		final String TAG = "open[C2Ma}";
		String dbMsg = "";
		try {
			this.activity = activity;
			if ( mState != null )
				throw new IllegalStateException("Alrady started state=" + mState);
			mTextureView = textureView;
			mCameraManager = ( CameraManager ) activity.getSystemService(Context.CAMERA_SERVICE);
			nextState(mInitSurfaceState);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public boolean takePicture(ImageReader.OnImageAvailableListener listener) {
		final String TAG = "takePicture[C2Ma}";
		String dbMsg = "";
		try {
			if ( mState != mPreviewState )
				return false;
			mTakePictureListener = listener;
			nextState(mAutoFocusState);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return true;
	}


	public void close() {
		final String TAG = "close[C2Ma}";
		String dbMsg = "";
		try {
			nextState(mAbortState);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	// ----------------------------------------------------------------------------------------
	// The following private
	private void shutdown() {
		final String TAG = "shutdown[C2Ma}";
		String dbMsg = "";
		try {
			if ( null != mCaptureSession ) {
				mCaptureSession.close();
				mCaptureSession = null;
			}
			if ( null != mCameraDevice ) {
				mCameraDevice.close();
				mCameraDevice = null;
			}
			if ( null != mImageReader ) {
				mImageReader.close();
				mImageReader = null;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	private void nextState(State nextState) {
		final String TAG = "nextState[C2Ma}";
		String dbMsg = "";
		try {
			dbMsg = "state: " + mState + "->" + nextState;
			if ( mState != null )
				mState.finish();
			mState = nextState;
			if ( mState != null )
				mState.enter();
			myLog(TAG , dbMsg);
		} catch (CameraAccessException e) {
			dbMsg = "next(" + nextState + ")";            //, e);
			shutdown();
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	private abstract class State {
		private String mName;

		public State(String name) {
			mName = name;
		}

		//@formatter:off
		public String toString() {
			return mName;
		}

		public void enter() throws CameraAccessException {
		}

		public void onSurfaceTextureAvailable(int width , int height) {
		}

		public void onCameraOpened(CameraDevice cameraDevice) {
		}

		public void onSessionConfigured(CameraCaptureSession cameraCaptureSession) {
		}

		public void onCaptureResult(CaptureResult result , boolean isCompleted) throws CameraAccessException {
		}

		public void finish() throws CameraAccessException {
		}
		//@formatter:on
	}

	// ===================================================================================
	// State Definition
	private final State mInitSurfaceState = new State("InitSurface") {
		public void enter() throws CameraAccessException {
			final String TAG = "mInitSurfaceState[C2Ma}";
			String dbMsg = "";
			try {
				if ( mTextureView.isAvailable() ) {
					nextState(mOpenCameraState);
				} else {
					mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		public void onSurfaceTextureAvailable(int width , int height) {
			final String TAG = "onStart[C2Ma}";
			String dbMsg = "";
			try {
				dbMsg = "[" + width + "×" + height + "]";
				nextState(mOpenCameraState);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture texture , int width , int height) {
				final String TAG = "onSurfaceTextureAvailable[C2Ma}";
				String dbMsg = "";
				try {
					if ( mState != null )
						mState.onSurfaceTextureAvailable(width , height);
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture texture , int width , int height) {
				final String TAG = "onSurfaceTextureSizeChanged[C2Ma}";
				String dbMsg = "";
				try {
					// TODO: ratation changed.
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
				final String TAG = "onSurfaceTextureDestroyed[C2Ma}";
				String dbMsg = "";
				try {
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
				return true;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture texture) {
				final String TAG = "onSurfaceTextureUpdated[C2Ma}";
				String dbMsg = "";
				try {
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}
		};
	};
	// -----------------------------------------------------------------------------------
	private final State mOpenCameraState = new State("OpenCamera") {
		@SuppressLint ( "MissingPermission" )
		public void enter() throws CameraAccessException {
			final String TAG = "mOpenCameraState[C2Ma}";
			String dbMsg = "";
			try {
				// configureTransform(width, height);
				String cameraId = Camera2Util.getCameraId(mCameraManager , CameraCharacteristics.LENS_FACING_BACK);
				CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
				StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

				mImageReader = Camera2Util.getMaxSizeImageReader(map , ImageFormat.JPEG);
				Size previewSize = Camera2Util.getBestPreviewSize(map , mImageReader);
				mTextureView.setPreviewSize(previewSize.getHeight() , previewSize.getWidth());

				mCameraManager.openCamera(cameraId , mStateCallback , mHandler);
				dbMsg += "openCamera:" + cameraId;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		public void onCameraOpened(CameraDevice cameraDevice) {
			final String TAG = "onCameraOpened[C2Ma}";
			String dbMsg = "";
			try {
				mCameraDevice = cameraDevice;
				nextState(mCreateSessionState);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
			@Override
			public void onOpened(CameraDevice cameraDevice) {
				final String TAG = "onOpened[C2Ma}";
				String dbMsg = "";
				try {
					if ( mState != null )
						mState.onCameraOpened(cameraDevice);
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			@Override
			public void onDisconnected(CameraDevice cameraDevice) {
				final String TAG = "onDisconnected[C2Ma}";
				String dbMsg = "";
				try {
					nextState(mAbortState);
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			@Override
			public void onError(CameraDevice cameraDevice , int error) {
				final String TAG = "onError[C2Ma}";
				String dbMsg = "";
				dbMsg = "CameraDevice:onError:" + error;
				nextState(mAbortState);
				myErrorLog(TAG , dbMsg + ";のエラー発生；");
			}
		};
	};
	// -----------------------------------------------------------------------------------
	private final State mCreateSessionState = new State("CreateSession") {
		public void enter() throws CameraAccessException {
			final String TAG = "mCreateSessionState[C2Ma}";
			String dbMsg = "";
			try {
				mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
				SurfaceTexture texture = mTextureView.getSurfaceTexture();
				texture.setDefaultBufferSize(mTextureView.getPreviewWidth() , mTextureView.getPreviewHeight());
				Surface surface = new Surface(texture);
				mPreviewRequestBuilder.addTarget(surface);
				List< Surface > outputs = Arrays.asList(surface , mImageReader.getSurface());
				mCameraDevice.createCaptureSession(outputs , mSessionCallback , mHandler);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		public void onSessionConfigured(CameraCaptureSession cameraCaptureSession) {
			final String TAG = "onStart[C2Ma}";
			String dbMsg = "";
			try {
				mCaptureSession = cameraCaptureSession;
				nextState(mPreviewState);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		private final CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
			@Override
			public void onConfigured(CameraCaptureSession cameraCaptureSession) {
				final String TAG = "onConfigured[C2Ma}";
				String dbMsg = "";
				try {

					if ( mState != null )
						mState.onSessionConfigured(cameraCaptureSession);
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}

			@Override
			public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
				final String TAG = "onStart[C2Ma}";
				String dbMsg = "";
				try {

					nextState(mAbortState);
					myLog(TAG , dbMsg);
				} catch (Exception er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
				}
			}
		};
	};

	// -----------------------------------------------------------------------------------
	private final State mPreviewState = new State("Preview") {
		public void enter() throws CameraAccessException {
			final String TAG = "onStart[C2Ma}";
			String dbMsg = "";
			try {

				mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
				mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE , CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
				mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build() , mCaptureCallback , mHandler);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
		@Override
		public void onCaptureProgressed(CameraCaptureSession session , CaptureRequest request , CaptureResult partialResult) {
			final String TAG = "onStart[C2Ma}";
			String dbMsg = "";
			try {
				onCaptureResult(partialResult , false);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		@Override
		public void onCaptureCompleted(CameraCaptureSession session , CaptureRequest request , TotalCaptureResult result) {
			final String TAG = "onCaptureCompleted[C2Ma}";
			String dbMsg = "";
			try {
				onCaptureResult(result , true);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		private void onCaptureResult(CaptureResult result , boolean isCompleted) {
			final String TAG = "onCaptureResult[C2Ma}";
			String dbMsg = "";
			try {
				try {
					if ( mState != null )
						mState.onCaptureResult(result , isCompleted);
				} catch (CameraAccessException er) {
					myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);//			Log.e(TAG, "handle():", e);
					nextState(mAbortState);
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};
	// -----------------------------------------------------------------------------------
	private final State mAutoFocusState = new State("AutoFocus") {
		public void enter() throws CameraAccessException {
			final String TAG = "onStart[C2Ma}";
			String dbMsg = "";
			try {
				mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_START);
				mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build() , mCaptureCallback , mHandler);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		public void onCaptureResult(CaptureResult result , boolean isCompleted) throws CameraAccessException {
			final String TAG = "onStart[C2Ma}";
			String dbMsg = "";
			try {
				dbMsg = "isCompleted=" + isCompleted;
				Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
				boolean isAfReady = afState == null || afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED;
				dbMsg += ",isAeReady=" + isAfReady;
				if ( isAfReady ) {
					nextState(mAutoExposureState);
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	// -----------------------------------------------------------------------------------
	private final State mAutoExposureState = new State("AutoExposure") {
		public void enter() throws CameraAccessException {
			final String TAG = "mAutoExposureState[C2Ma}";
			String dbMsg = "";
			try {
				mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER , CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
				mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build() , mCaptureCallback , mHandler);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}


		public void onCaptureResult(CaptureResult result , boolean isCompleted) throws CameraAccessException {
			final String TAG = "onCaptureResult[C2Ma}";
			String dbMsg = "";
			try {
				dbMsg = "isCompleted=" + isCompleted;
				Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
				boolean isAeReady = aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED;
				dbMsg += ",isAeReady=" + isAeReady;
				if ( isAeReady ) {
					nextState(mTakePictureState);
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};
	// -----------------------------------------------------------------------------------
	private final State mTakePictureState = new State("TakePicture") {
		public void enter() throws CameraAccessException {
			final String TAG = "mTakePictureState[C2Ma}";
			String dbMsg = "";
			try {
				final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
				captureBuilder.addTarget(mImageReader.getSurface());
				captureBuilder.set(CaptureRequest.CONTROL_AF_MODE , CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
				captureBuilder.set(CaptureRequest.CONTROL_AE_MODE , CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
				captureBuilder.set(CaptureRequest.JPEG_ORIENTATION , 90); // portraito
				mImageReader.setOnImageAvailableListener(mTakePictureListener , mHandler);

				mCaptureSession.stopRepeating();
				mCaptureSession.capture(captureBuilder.build() , mCaptureCallback , mHandler);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		public void onCaptureResult(CaptureResult result , boolean isCompleted) throws CameraAccessException {
			final String TAG = "onCaptureResult[C2Ma}";
			String dbMsg = "";
			try {
				dbMsg = "isCompleted=" + isCompleted;
				if ( isCompleted ) {
					nextState(mPreviewState);
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}

		public void finish() throws CameraAccessException {
			final String TAG = "finish[C2Ma}";
			String dbMsg = "";
			try {
				mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER , CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
				mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE , CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
				mCaptureSession.capture(mPreviewRequestBuilder.build() , mCaptureCallback , mHandler);
				mTakePictureListener = null;
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};
	// -----------------------------------------------------------------------------------
	private final State mAbortState = new State("Abort") {
		public void enter() throws CameraAccessException {
			final String TAG = "mAbortState[C2Ma}";
			String dbMsg = "";
			try {
				shutdown();
				nextState(null);
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	///////////////////////////////////////////////////////////////////////////////////
	public void messageShow(String titolStr , String mggStr) {
		CS_Util UTIL = new CS_Util();
		UTIL.messageShow(titolStr , mggStr , activity);
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