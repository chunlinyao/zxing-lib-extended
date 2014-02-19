package com.google.zxing.client.android.camera;

public class YUVImage {

  private final byte[] data;
  private final int width;
  private final int height;


  public YUVImage(byte[] data, int width, int height) {
    this.data = data;
    this.width = width;
    this.height = height;
  }

  public byte[] getData() {
    return data;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public YUVImage rotateClockwise() {
    byte[] yuv = new byte[width * height * 3 / 2];
    // Rotate the Y luma
    int i = 0;
    for (int x = 0; x < width; x++) {
      for (int y = height - 1; y >= 0; y--) {
        yuv[i] = data[y * width + x];
        i++;
      }
    }
    // Rotate the U and V color components
    i = width * height * 3 / 2 - 1;
    for (int x = width - 1; x > 0; x = x - 2) {
      for (int y = 0; y < height / 2; y++) {
        yuv[i] = data[(width * height) + (y * width) + x];
        i--;
        yuv[i] = data[(width * height) + (y * width) + (x - 1)];
        i--;
      }
    }
    return new YUVImage(yuv, height, width);
  }

  public YUVImage rotateCounterClockwise() {
    byte[] yuv = new byte[width * height * 3 / 2];
    // Rotate the Y luma
    int i = 0;
    for (int x = width - 1; x >= 0; x--) {
      for (int y = 0; y < height; y++) {
        yuv[i] = data[y * width + x];
        i++;
      }
    }
    // Rotate the U and V color components
    i = width * height * 3 / 2 - 1;
    for (int x = 0; x < width; x = x + 2) {
      for (int y = (height / 2); y > 0; y--) {
        yuv[i] = data[(width * height) + ((y-1) * width) + (x + 1)];
        i--;
        yuv[i] = data[(width * height) + ((y-1) * width) + x];
        i--;
      }
    }
    return new YUVImage(yuv, height, width);
  }


  public YUVImage mirror() {
    byte[] yuv = new byte[width * height * 3 / 2];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int newIndex = x + y * width;
        int oldIndex = ((width - 1) - (x % width)) + y * width;
        System.out.println("Moving old index " + oldIndex + " to new index" + newIndex);
        yuv[newIndex] = data[oldIndex];
      }
    }


    int offset = width * height;
    int uvHeight = height / 2;
    int uvWidth = width / 2;
    for (int y = 0; y < uvHeight; y++) {
      for (int x = 0; x < uvWidth; x++) {
        int partialNewIndex = x + y * uvWidth;
        int partialOldIndex = ((uvWidth - 1) - (x % uvWidth)) + y * uvWidth;
        System.out.println("Moving old index " + partialOldIndex + " to new index " + partialNewIndex);
        yuv[offset + partialNewIndex * 2] = data[offset + partialOldIndex * 2];
        yuv[offset + partialNewIndex * 2 + 1] = data[offset + partialOldIndex * 2 + 1];
      }
    }
    return new YUVImage(yuv, width, height);
  }

}
