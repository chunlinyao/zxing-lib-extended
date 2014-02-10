package com.google.zxing.client.android.camera;

public class YUITransformer {

  private YUITransformer(){}

  public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
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

  public static byte[] mirrorYUV420(byte[] data, int imageWidth, int imageHeight) {
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


}
