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

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
public class CameraConfigurationManager {

  private static final String TAG = "CameraConfiguration";

  // This is bigger than the size of a small screen, which is still supported. The routine
  // below will still select the default (presumably 320x240) size for these. This prevents
  // accidental selection of very low resolution on some devices.
  private static final int MIN_PREVIEW_PIXELS = 470 * 320; // normal screen
  private static final int MAX_PREVIEW_PIXELS = 1280 * 720;

  private final Context context;
  private Resolution screenResolution;
  private Resolution cameraResolution;

  public CameraConfigurationManager(Context context) {
    this.context = context;
  }

  /**
   * Reads, one time, values from the camera that are needed by the app.
   */
  void initFromCameraParameters(Camera camera) {
    Camera.Parameters parameters = camera.getParameters();
    WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = manager.getDefaultDisplay();
    int width = display.getWidth();
    int height = display.getHeight();
    // We're landscape-only, and have apparently seen issues with display thinking it's portrait 
    // when waking from sleep. If it's not landscape, assume it's mistaken and reverse them:
    if (width < height) {
      Log.i(TAG, "Display reports portrait orientation; assuming this is incorrect");
      int temp = width;
      width = height;
      height = temp;
    }
    screenResolution = new Resolution(width, height);
    Log.i(TAG, "Screen resolution: " + screenResolution);
    cameraResolution = findBestPreviewSizeValue(parameters, screenResolution);
    Log.i(TAG, "Camera resolution: " + cameraResolution);
  }

  void setDesiredCameraParameters(Camera camera, boolean safeMode) {
    Camera.Parameters parameters = camera.getParameters();

    if (parameters == null) {
      Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
      return;
    }

    Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

    if (safeMode) {
      Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
    }

    initializeTorch(parameters, safeMode);

    String focusMode = null;
    if (safeMode) {
      focusMode = findSettableValue(parameters.getSupportedFocusModes(),
          Camera.Parameters.FOCUS_MODE_AUTO);
    } else {
      focusMode = findSettableValue(parameters.getSupportedFocusModes(),
          "continuous-picture", // Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE in 4.0+
          "continuous-video",   // Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO in 4.0+
          Camera.Parameters.FOCUS_MODE_AUTO);
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

    parameters.setPreviewSize(cameraResolution.getX(), cameraResolution.getY());
    camera.setParameters(parameters);
  }

  Resolution getCameraResolution() {
    return cameraResolution;
  }

  Resolution getScreenResolution() {
    return screenResolution;
  }

  void setTorch(Camera camera, boolean newSetting) {
    Camera.Parameters parameters = camera.getParameters();
    doSetTorch(parameters, newSetting);
    camera.setParameters(parameters);
  }

  private void initializeTorch(Camera.Parameters parameters, boolean safeMode) {
    doSetTorch(parameters, safeMode);
  }

  private void doSetTorch(Camera.Parameters parameters, boolean on) {
    String flashMode;
    if (on) {
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
  }

  private Resolution findBestPreviewSizeValue(Camera.Parameters parameters, Resolution screenResolution) {

    List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
    if (rawSupportedSizes == null) {
      Log.w(TAG, "Device returned no supported preview sizes; using default");
      Camera.Size defaultSize = parameters.getPreviewSize();
      return new Resolution(defaultSize.width, defaultSize.height);
    }

    // Sort by size, descending
    List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(rawSupportedSizes);
    Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
      @Override
      public int compare(Camera.Size a, Camera.Size b) {
        int aPixels = a.height * a.width;
        int bPixels = b.height * b.width;
        if (bPixels < aPixels) {
          return -1;
        }
        if (bPixels > aPixels) {
          return 1;
        }
        return 0;
      }
    });

    if (Log.isLoggable(TAG, Log.INFO)) {
      StringBuilder previewSizesString = new StringBuilder();
      for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
        previewSizesString.append(supportedPreviewSize.width).append('x')
            .append(supportedPreviewSize.height).append(' ');
      }
      Log.i(TAG, "Supported preview sizes: " + previewSizesString);
    }

    Resolution bestSize = null;
    float screenAspectRatio = screenResolution.getRatio();

    float diff = Float.POSITIVE_INFINITY;
    for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
      Resolution originalCameraResolution = new Resolution(supportedPreviewSize.width, supportedPreviewSize.height);
      Resolution cameraResolution;
      int pixels = originalCameraResolution.getPixels();
      if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
        continue;
      }
      if (originalCameraResolution.isPortrait()) {
        cameraResolution = originalCameraResolution.rotate();
      } else {
        cameraResolution = originalCameraResolution;
      }
      float aspectRatio = cameraResolution.getRatio();
      float newDiff = Math.abs(aspectRatio - screenAspectRatio);
      Log.i(TAG, "aspect Ratio:" + aspectRatio + " screenAspectRation:" + screenAspectRatio);
      Log.i(TAG, cameraResolution + " has diff " + newDiff);
      if (newDiff < diff) {
        bestSize = originalCameraResolution;
        diff = newDiff;
      }
    }

    if (bestSize == null) {
      Camera.Size defaultSize = parameters.getPreviewSize();
      bestSize = new Resolution(defaultSize.width, defaultSize.height);
      Log.i(TAG, "No suitable preview sizes, using default: " + bestSize);
    }

    Log.i(TAG, "Found best approximate preview size: " + bestSize);
    return bestSize;
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

}
