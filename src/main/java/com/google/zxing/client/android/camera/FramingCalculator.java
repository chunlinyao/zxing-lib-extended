package com.google.zxing.client.android.camera;

import android.graphics.Rect;
import android.util.Log;


class FramingCalculator {

  private static final int MIN_FRAME_WIDTH = 240;
  private static final int MIN_FRAME_HEIGHT = 240;
  private static final int MAX_FRAME_WIDTH = 600;
  private static final int MAX_FRAME_HEIGHT = 400;

  private static final String TAG = FramingCalculator.class.getSimpleName();

  private final Resolution screenResolution;
  private final Resolution cameraResolution;

  FramingCalculator(Resolution screenResolution, Resolution cameraResolution, int rotation) {
    if (screenResolution != null) {
      if (rotation % 180 == 0) {
        this.screenResolution = screenResolution.rotate();
      } else {
        this.screenResolution = screenResolution;
      }
    } else {
      this.screenResolution = null;
    }
    if (cameraResolution != null) {
      if (rotation % 180 == 0) {
        this.cameraResolution= cameraResolution.rotate();
      } else {
        this.cameraResolution = cameraResolution;
      }
    } else {
      this.cameraResolution= null;
    }

  }

  /**
   * Calculates the framing rect which the UI should draw to show the user where to place the
   * barcode. This target helps with alignment as well as forces the user to hold the device
   * far enough away to ensure the image will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public Rect getFramingRect() {
    if (screenResolution == null) {
      // Called early, before init even finished
      return null;
    }
    return calculateFrameRect(screenResolution);
  }

  public static Rect calculateFrameRect(Resolution resolution) {

    int width = resolution.getX() * 3 / 4;
    if (width < MIN_FRAME_WIDTH) {
      width = MIN_FRAME_WIDTH;
    } else if (width > MAX_FRAME_WIDTH) {
      width = MAX_FRAME_WIDTH;
    }
    int height = resolution.getY() * 3 / 4;
    if (height < MIN_FRAME_HEIGHT) {
      height = MIN_FRAME_HEIGHT;
    } else if (height > MAX_FRAME_HEIGHT) {
      height = MAX_FRAME_HEIGHT;
    }
    int leftOffset = (resolution.getX() - width) / 2;
    int topOffset = (resolution.getY() - height) / 2;
    Rect framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    Log.d(TAG, "Calculated framing rect: " + framingRect);
    return framingRect;
  }

  /**
   * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
   * not UI / screen.
   */
  public Rect getFramingRectInPreview() {
    Rect framingRect = getFramingRect();
    if (framingRect == null) {
      return null;
    }
    Rect rect = new Rect(framingRect);
    if (cameraResolution == null || screenResolution == null) {
      // Called early, before init even finished
      return null;
    }
    rect.left = rect.left * cameraResolution.getX() / screenResolution.getX();
    rect.right = rect.right * cameraResolution.getX() / screenResolution.getX();
    rect.top = rect.top * cameraResolution.getY() / screenResolution.getY();
    rect.bottom = rect.bottom * cameraResolution.getY() / screenResolution.getY();

    return rect;
  }

}
