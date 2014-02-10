package com.google.zxing.client.android.camera;

public class YUVTransformer {

  private static final String TAG = YUVTransformer.class.getSimpleName();

  private YUVTransformer() {
  }

  public static byte[] rotate90degrees(byte[] data, int imageWidth, int imageHeight) {
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

  public static byte[] mirror(byte[] data, int imageWidth, int imageHeight) {
    byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
    for (int y = 0; y < imageHeight; y++) {
      for (int x = 0; x < imageWidth; x++) {
        int newIndex = x + y * imageWidth;
        int oldIndex = ((imageWidth - 1) - (x % imageWidth)) + y * imageWidth;
        System.out.println("Moving old index " + oldIndex + " to new index" + newIndex);
        yuv[newIndex] = data[oldIndex];
      }
    }


    int offset = imageWidth * imageHeight;
    int uvHeight = imageHeight / 2;
    int uvWidth = imageWidth / 2;
    for (int y = 0; y < uvHeight; y++) {
      for (int x = 0; x < uvWidth; x++) {
        int partialNewIndex = x + y * uvWidth;
        int partialOldIndex = ((uvWidth - 1) - (x % uvWidth)) + y * uvWidth;
        System.out.println("Moving old index " + partialOldIndex + " to new index " + partialNewIndex);
        yuv[offset + partialNewIndex * 2] = data[offset + partialOldIndex * 2];
        yuv[offset + partialNewIndex * 2 + 1] = data[offset + partialOldIndex * 2 + 1];
      }
    }
    return yuv;
  }


}
