package com.hijiyam_koubou.kaodori;


import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.util.Size;

public class Camera2Util {

	public static String getCameraId(CameraManager cameraManager, int facing) throws CameraAccessException {
		final String TAG = "getCameraId[C2U]";
		String dbMsg = "";
		String retStr =null;
		try {
			dbMsg = "facing="+facing;
			for (String cameraId : cameraManager.getCameraIdList()) {
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
				dbMsg += "cameraId="+cameraId;
				if (characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
					retStr =cameraId;
					break;
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return retStr;
	}

	public static ImageReader getMaxSizeImageReader(StreamConfigurationMap map, int imageFormat) throws CameraAccessException {
		final String TAG = "getMaxSizeImageReader[C2U]";
		String dbMsg = "";
		ImageReader imageReader = null;
		try {
			Size[] sizes = map.getOutputSizes(imageFormat);
			Size maxSize = sizes[0];
			for (Size size:sizes) {
				if (size.getWidth() > maxSize.getWidth()) {
					maxSize = size;
				}
			}
			imageReader = ImageReader.newInstance(
					//maxSize.getWidth(), maxSize.getHeight(), // for landscape.
					maxSize.getHeight(), maxSize.getWidth(), // for portrait.
					imageFormat, /*maxImages*/1);			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return imageReader;
	}

	public static Size getBestPreviewSize(StreamConfigurationMap map, ImageReader imageSize) throws CameraAccessException {
		final String TAG = "getBestPreviewSize[C2U]";
		String dbMsg = "";
		Size previewSize = null;
		try {
			//float imageAspect = (float) imageSize.getWidth() / imageSize.getHeight(); // for landscape.
			float imageAspect = (float) imageSize.getHeight() / imageSize.getWidth(); // for portrait
			float minDiff = 1000000000000F;
			Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
			previewSize = previewSizes[0];
			for (Size size : previewSizes) {
				float previewAspect = (float) size.getWidth() / size.getHeight();
				float diff = Math.abs(imageAspect - previewAspect);
				if (diff < minDiff) {
					previewSize = size;
					minDiff = diff;
				}
				if (diff == 0.0F) break;
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return previewSize;
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

//http://blog.kotemaru.org/2015/05/23/android-camera2-sample.html