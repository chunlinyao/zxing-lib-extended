/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.camera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import com.google.zxing.client.android.PreferencesActivity;

import java.util.*;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";

    // This is bigger than the size of a small screen, which is still supported. The routine
    // below will still select the default (presumably 320x240) size for these. This prevents
    // accidental selection of very low resolution on some devices.

    private final Context context;
    private final Activity activity;
    private Point screenResolution;
    private Point cameraResolution;

    CameraConfigurationManager(Context context) {
        this.context = context.getApplicationContext();
        this.activity = (Activity) context;
    }

    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();

        screenResolution = new Point();
        display.getSize(screenResolution);

        //May be portrait mode
        Point rotatedScreen = screenResolution;
        if(screenResolution.x < screenResolution.y) {
            rotatedScreen = new Point(screenResolution.y, screenResolution.x);
        }
        Log.i(TAG, "Screen resolution: " + screenResolution);
        cameraResolution =  CameraConfigurationUtils.findBestPreviewSizeValue(parameters, rotatedScreen);
        Log.i(TAG, "Camera resolution: " + cameraResolution);
    }

    void setDesiredCameraParameters(Camera camera, boolean safeMode) {
        int result = caculateDisplayRotation();
        camera.setDisplayOrientation(result);


        Camera.Parameters parameters = camera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        initializeTorch(parameters, prefs, safeMode);

        String focusMode = null;
        if (prefs.getBoolean(PreferencesActivity.KEY_AUTO_FOCUS, true)) {
            if (safeMode || prefs.getBoolean(PreferencesActivity.KEY_DISABLE_CONTINUOUS_FOCUS, false)) {
                focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                        Camera.Parameters.FOCUS_MODE_AUTO);
            } else {
                focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                        "continuous-picture", // Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE in 4.0+
                        "continuous-video",   // Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO in 4.0+
                        Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }
        // Maybe selected auto-focus but not available, so fall through here:
        if (!safeMode && focusMode == null) {
            focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                    Camera.Parameters.FOCUS_MODE_MACRO,
                    "edof"); // Camera.Parameters.FOCUS_MODE_EDOF in 2.2+
        }
        if (focusMode != null) {
            parameters.setFocusMode(focusMode);
        }

        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
        camera.setParameters(parameters);
    }

    private int caculateDisplayRotation() {
        // Checkout of screen orientation
        WindowManager manager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
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

        int result;
        Camera.CameraInfo info = getCameraInfo();
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * zxing use first backing camera, so we return the first backing camera.
     * @return
     */
    private Camera.CameraInfo getCameraInfo() {

        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for(
        int i = 0;
        i<numberOfCameras;i++)

        {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return cameraInfo;
            }
        }

        return null;
    }

    Point getCameraResolution() {
    return cameraResolution;
  }

  Point getScreenResolution() {
    return screenResolution;
  }

  void setTorch(Camera camera, boolean newSetting) {
    Camera.Parameters parameters = camera.getParameters();
    doSetTorch(parameters, newSetting, false);
    camera.setParameters(parameters);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean currentSetting = prefs.getBoolean(PreferencesActivity.KEY_FRONT_LIGHT, false);
    if (currentSetting != newSetting) {
      SharedPreferences.Editor editor = prefs.edit();
      editor.putBoolean(PreferencesActivity.KEY_FRONT_LIGHT, newSetting);
      editor.commit();
    }
  }

  private void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs, boolean safeMode) {
    boolean currentSetting = prefs.getBoolean(PreferencesActivity.KEY_FRONT_LIGHT, false);
    doSetTorch(parameters, currentSetting, safeMode);
  }

  private void doSetTorch(Camera.Parameters parameters, boolean newSetting, boolean safeMode) {
    String flashMode;
    if (newSetting) {
      flashMode = findSettableValue(parameters.getSupportedFlashModes(),
                                    Camera.Parameters.FLASH_MODE_TORCH,
                                    Camera.Parameters.FLASH_MODE_ON);
    } else {
      flashMode = findSettableValue(parameters.getSupportedFlashModes(),
                                    Camera.Parameters.FLASH_MODE_OFF);
    }
    if (flashMode != null) {
      parameters.setFlashMode(flashMode);
    }

    /*
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (!prefs.getBoolean(PreferencesActivity.KEY_DISABLE_EXPOSURE, false)) {
      if (!safeMode) {
        ExposureInterface exposure = new ExposureManager().build();
        exposure.setExposure(parameters, newSetting);
      }
    }
     */
  }

  private static String findSettableValue(Collection<String> supportedValues,
                                          String... desiredValues) {
    Log.i(TAG, "Supported values: " + supportedValues);
    String result = null;
    if (supportedValues != null) {
      for (String desiredValue : desiredValues) {
        if (supportedValues.contains(desiredValue)) {
          result = desiredValue;
          break;
        }
      }
    }
    Log.i(TAG, "Settable value: " + result);
    return result;
  }

  private int statusBarHeight() {
      int result = 0;
      if (this.activity != null) {
          Window window = this.activity.getWindow();
          Rect rect = new Rect();
          window.getDecorView().getWindowVisibleDisplayFrame(rect);
          result = rect.top;
      }

      return result;
  }
}
