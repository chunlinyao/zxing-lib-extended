package com.google.zxing.client.android.camera;


public class Resolution {

  private final int x;
  private final int y;

  public Resolution(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public Resolution rotate() {
    return new Resolution(y, x);
  }
}
