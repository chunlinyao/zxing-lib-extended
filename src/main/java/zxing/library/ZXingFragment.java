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

package zxing.library;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.google.zxing.Result;
import com.google.zxing.client.android.FinishListener;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraConfigurationManager;
import com.google.zxing.client.android.camera.CameraManager;

import java.io.IOException;

/**
 * A fragment that provides all of the UI/processing required to handle barcode
 * decoding
 *
 * @author kennydude
 */
public class ZXingFragment extends Fragment {

  private static final String TAG = ZXingFragment.class.getSimpleName();
  boolean hasSurface;
  private CameraManager cameraManager;
  private ViewfinderView viewfinderView;
  private FragmentHandler handler;
  private Result savedResultToShow;
  private TextureView.SurfaceTextureListener callback;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_capture, container, false);
  }

  @Override
  public void onStart() {
    super.onStart();

    // This is a good forced option
    Window window = getActivity().getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    hasSurface = false;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onResume() {
    super.onResume();

    // Setup camera view
    Context context = getActivity().getApplication();
    cameraManager = new CameraManager(new CameraConfigurationManager(context));
    cameraManager.setManualFramingRect(getView().getWidth(), getView().getHeight());
    cameraManager.setCameraDisplayOrientation(getActivity().getWindowManager().getDefaultDisplay().getRotation());

    viewfinderView = (ViewfinderView) getView().findViewById(R.id.viewfinder_view);
    viewfinderView.setCameraManager(cameraManager);

    final TextureView textureView = (TextureView) getView().findViewById(R.id.preview_view);
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still
      // exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(textureView);
    } else {
      // Install the callback and wait for surfaceCreated() to init the
      // camera.

      callback=new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
          if (surface == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!"); //$NON-NLS-1$
          }
          if (!hasSurface) {
            hasSurface = true;
            initCamera(textureView);
          }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
          hasSurface = false;
          return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
      };

      textureView.setSurfaceTextureListener(callback);
    }
  }

  @Override
  public void onPause() {
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    // inactivityTimer.onPause();
    // ambientLightManager.stop();
    cameraManager.closeDriver();
    if (!hasSurface) {
      TextureView view = (TextureView) getView().findViewById(R.id.preview_view);
      view.setSurfaceTextureListener(null);
    }
    super.onPause();
  }

  private void initCamera(TextureView textureView) {
    if (textureView == null) {
      throw new IllegalStateException("No TextureView provided"); //$NON-NLS-1$
    }
    if (cameraManager.isOpen()) {
      Log.w(TAG, "initCamera() while already open -- late textureView callback?"); //$NON-NLS-1$
      return;
    }
    try {
      cameraManager.openDriver(textureView);
      // Creating the handler starts the preview, which can also throw a
      // RuntimeException.
      if (handler == null) {
        // TODO: Replace with getArguments()... for custom setups
        handler = new FragmentHandler(this, null, null, cameraManager);
      }
      decodeOrStoreSavedBitmap(null, null);
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      displayFrameworkBugMessageAndExit();
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(TAG, "Unexpected error initializing camera", e); //$NON-NLS-1$
      displayFrameworkBugMessageAndExit();
    }
  }

  private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
    // Bitmap isn't used yet -- will be used soon
    if (handler == null) {
      savedResultToShow = result;
    } else {
      if (result != null) {
        savedResultToShow = result;
      }
      if (savedResultToShow != null) {
        Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
        handler.sendMessage(message);
      }
      savedResultToShow = null;
    }
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(getString(R.string.app_name));
    builder.setMessage(getString(R.string.msg_camera_framework_bug));
    builder.setPositiveButton(R.string.button_ok, new FinishListener(getActivity()));
    builder.setOnCancelListener(new FinishListener(getActivity()));
    builder.show();
  }


  public ViewfinderView getViewfinderView() {
    return viewfinderView;
  }

  public void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }

  public CameraManager getCameraManager() {
    return cameraManager;
  }

  public Handler getHandler() {
    return handler;
  }

  DecodeCallback dc = null;

  public void setDecodeCallback(DecodeCallback callback) {
    this.dc = callback;
  }

  public void handleDecode(Result obj, Bitmap barcode, float scaleFactor) {
    if (this.dc != null) {
      this.dc.handleBarcode(obj, barcode, scaleFactor);
    }
  }

  public void restartScanning() {
    handler.sendEmptyMessage(R.id.restart_preview);
  }

  public void restartScanningIn(int millis) {
    handler.sendEmptyMessageDelayed(R.id.restart_preview, millis);
  }

}
