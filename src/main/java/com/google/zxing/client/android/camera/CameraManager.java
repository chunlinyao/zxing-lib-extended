/*
 * Copyright (C) 2008 ZXing authors
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

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.google.zxing.PlanarYUVLuminanceSource;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

  private static final String TAG = CameraManager.class.getSimpleName();

  private final CameraConfigurationManager configManager;

  private Camera camera;
  private Integer cameraIndex;

  private Integer cameraRotationInDegrees;

  private AutoFocusManager autoFocusManager;
  private Rect framingRect;
  private Rect framingRectInPreview;
  private boolean initialized;
  private boolean previewing;
  private int requestedFramingRectWidth;
  private int requestedFramingRectHeight;
  /**
   * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
   * clear the handler so it will only receive one message.
   */
  private final PreviewCallback previewCallback;

  public CameraManager(CameraConfigurationManager configManager) {
    this.configManager = configManager;
    previewCallback = new PreviewCallback(configManager);
  }

  /**
   * Opens the camera driver and initializes the hardware parameters.
   *
   * @param holder The surface object which the camera will draw preview frames into.
   * @throws IOException Indicates the camera driver failed to open.
   */
  public synchronized void openDriver(TextureView holder) throws IOException {
    Camera theCamera = camera;
    if (theCamera == null) {
      theCamera = open();
      if (theCamera == null) {
        throw new IOException();
      }
      camera = theCamera;
      updateCameraOrientation(holder);
    }
    theCamera.setPreviewTexture(holder.getSurfaceTexture());

    calculateRotationInDegrees(cameraRotationInDegrees);

    if (!initialized) {
      initialized = true;
      configManager.initFromCameraParameters(theCamera);
      if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
        setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
        requestedFramingRectWidth = 0;
        requestedFramingRectHeight = 0;
      }
    }

    Camera.Parameters parameters = theCamera.getParameters();
    String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
    try {
      configManager.setDesiredCameraParameters(theCamera, false);
    } catch (RuntimeException re) {
      // Driver failed
      Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
      Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
      // Reset:
      if (parametersFlattened != null) {
        parameters = theCamera.getParameters();
        parameters.unflatten(parametersFlattened);
        try {
          theCamera.setParameters(parameters);
          configManager.setDesiredCameraParameters(theCamera, true);
        } catch (RuntimeException re2) {
          // Well, darn. Give up
          Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
        }
      }
    }

  }

  /**
   * Opens a rear-facing camera with {@link Camera#open(int)}, if one exists, or opens camera 0.
   */
  private Camera open() {

    int numCameras = Camera.getNumberOfCameras();
    if (numCameras == 0) {
      Log.w(TAG, "No cameras!");
      return null;
    }

    int index = 0;
    while (index < numCameras) {
      Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
      Camera.getCameraInfo(index, cameraInfo);
      if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
        break;
      }
      index++;
    }

    if (index < numCameras) {
      Log.i(TAG, "Opening camera #" + index);
      cameraIndex = index;
    } else {
      Log.i(TAG, "No camera facing back; returning camera #0");
      cameraIndex = 0;
    }

    return Camera.open(cameraIndex);
  }


  public synchronized boolean isOpen() {
    return camera != null;
  }

  /**
   * Closes the camera driver if still in use.
   */
  public synchronized void closeDriver() {
    if (camera != null) {
      camera.release();
      camera = null;
      // Make sure to clear these each time we close the camera, so that any scanning rect
      // requested by intent is forgotten.
      framingRect = null;
      framingRectInPreview = null;
    }
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   */
  public synchronized void startPreview() {
    Camera theCamera = camera;
    if (theCamera != null && !previewing) {
      theCamera.startPreview();
      previewing = true;
      autoFocusManager = new AutoFocusManager(camera);
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   */
  public synchronized void stopPreview() {
    if (autoFocusManager != null) {
      autoFocusManager.stop();
      autoFocusManager = null;
    }
    if (camera != null && previewing) {
      camera.stopPreview();
      previewCallback.setHandler(null, 0);
      previewing = false;
    }
  }

  public synchronized void setTorch(boolean newSetting) {
    if (camera != null) {
      if (autoFocusManager != null) {
        autoFocusManager.stop();
      }
      configManager.setTorch(camera, newSetting);
      if (autoFocusManager != null) {
        autoFocusManager.start();
      }
    }
  }

  /**
   * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
   * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
   * respectively.
   *
   * @param handler The handler to send the message to.
   * @param message The what field of the message to be sent.
   */
  public synchronized void requestPreviewFrame(Handler handler, int message) {
    Camera theCamera = camera;
    if (theCamera != null && previewing) {
      previewCallback.setHandler(handler, message);
      theCamera.setOneShotPreviewCallback(previewCallback);
    }
  }

  /**
   * Calculates the framing rect which the UI should draw to show the user where to place the
   * barcode. This target helps with alignment as well as forces the user to hold the device
   * far enough away to ensure the image will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public synchronized Rect getFramingRect() {
    if (framingRect == null && camera != null) {
      framingRect = new FramingCalculator(
          configManager.getScreenResolution(),
          configManager.getCameraResolution(),
          cameraRotationInDegrees).getFramingRect();
    }
    return framingRect;
  }

  /**
   * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
   * not UI / screen.
   */
  public synchronized Rect getFramingRectInPreview() {
    if (framingRectInPreview == null && camera != null) {
      framingRectInPreview = new FramingCalculator(
          configManager.getScreenResolution(),
          configManager.getCameraResolution(),
          cameraRotationInDegrees).getFramingRectInPreview();
    }
    return framingRectInPreview;
  }

  /**
   * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
   * them automatically based on screen resolution.
   *
   * @param width  The width in pixels to scan.
   * @param height The height in pixels to scan.
   */
  public synchronized void setManualFramingRect(int width, int height) {
    if (initialized) {
      Resolution screenResolution = configManager.getScreenResolution();
      if (width > screenResolution.getX()) {
        width = screenResolution.getX();
      }
      if (height > screenResolution.getY()) {
        height = screenResolution.getY();
      }
      int leftOffset = (screenResolution.getX() - width) / 2;
      int topOffset = (screenResolution.getY() - height) / 2;
      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
      Log.d(TAG, "Calculated manual framing rect: " + framingRect);
      framingRectInPreview = null;
    } else {
      requestedFramingRectWidth = width;
      requestedFramingRectHeight = height;
    }
  }

  /**
   * A factory method to build the appropriate LuminanceSource object based on the format
   * of the preview buffers, as described by Camera.Parameters.
   *
   * @param data   A preview frame.
   * @param width  The width of the image.
   * @param height The height of the image.
   * @return A PlanarYUVLuminanceSource instance.
   */
  public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
    Rect rect = getFramingRectInPreview();
    if (rect == null) {
      return null;
    }
    // Go ahead and assume it's YUV rather than die.
    if (this.cameraRotationInDegrees % 180 == 0) {
      Log.i(TAG, "Rotate that YUV image");
      byte[] rotateData = mirrorYUV420(rotateYUV420Degree90(data, width, height), height, width);

      Rect rotateRect = new FramingCalculator(
          configManager.getScreenResolution(),
          configManager.getCameraResolution(),
          cameraRotationInDegrees).getFramingRectInPreview();
      return new PlanarYUVLuminanceSource(rotateData, height, width, rotateRect.left, rotateRect.top, rotateRect.width(), rotateRect.height(), false);
    } else {
      return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
    }
  }


  private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
    byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
    // Rotate the Y luma
    int i = 0;
    for (int x = 0; x < imageWidth; x++) {
      for (int y = imageHeight - 1; y >= 0; y--) {
        yuv[i] = data[y * imageWidth + x];
        i++;
      }
    }
    // Rotate the U and V color components
    i = imageWidth * imageHeight * 3 / 2 - 1;
    for (int x = imageWidth - 1; x > 0; x = x - 2) {
      for (int y = 0; y < imageHeight / 2; y++) {
        yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
        i--;
        yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
        i--;
      }
    }
    return yuv;
  }

  private byte[] mirrorYUV420(byte[] data, int imageWidth, int imageHeight) {
    byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
    // Mirror the Y luma
    int i = 0;
    for (int x = imageWidth - 1; x >= 0; x--) {
      for (int y = imageHeight - 1; y >= 0; y--) {
        yuv[i] = data[y * imageWidth + x];
        i++;
      }
    }
    // Rotate the U and V color components
    i = imageWidth * imageHeight * 3 / 2 - 1;
    for (int x = 0; x < imageWidth - 1; x = x + 2) {
      for (int y = (imageHeight - 1) / 2; y >= 0; y--) {
        yuv[i] = 0xf;
        i--;
        yuv[i] = 0xf;
        i--;
      }
    }
    return yuv;
  }


  public void setCameraDisplayOrientation(int rotation) {
    cameraRotationInDegrees = calculateRotationInDegrees(rotation);
  }

  private int calculateRotationInDegrees(int rotation) {
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
    return degrees;
  }

  private void updateCameraOrientation(TextureView textureView) {
    android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
    android.hardware.Camera.getCameraInfo(cameraIndex, info);
    int result;
    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      Log.i(TAG, "Front facing camera so trying to compensate the mirroring");
      result = (info.orientation + cameraRotationInDegrees) % 360;
      result = (360 - result) % 360;  // compensate the mirror

      Matrix matrix = new Matrix();
      matrix.setScale(-1, 1);
      matrix.postTranslate(textureView.getWidth(), 0);
      textureView.setTransform(matrix);

    } else {  // back-facing
      result = (info.orientation - cameraRotationInDegrees + 360) % 360;
    }
    Log.i(TAG, "Settings application to orientation:" + result);
    camera.setDisplayOrientation(result);
  }
}
