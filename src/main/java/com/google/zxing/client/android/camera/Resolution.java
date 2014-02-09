package com.google.zxing.client.android.camera;


import android.graphics.Paint;

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

  public float getRatio() {
    return (float) getX() / (float) getY();
  }

  public int getPixels() {
    return getX() * getY();
  }

  public boolean isPortrait() {
    return getY() > getX();
  }

  @Override
  public String toString() {
    return String.format("Resolution (%d,%d)", x, y);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof Resolution)) {
      return false;
    }
    Resolution that = (Resolution) o;
    return this.x == that.x && this.y == that.y;
  }
}
