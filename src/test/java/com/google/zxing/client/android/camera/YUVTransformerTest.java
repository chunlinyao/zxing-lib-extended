package com.google.zxing.client.android.camera;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class YUVTransformerTest {


  @Test
  public void rotate_image() {
    byte[] original = new byte[]{
        1, 2, 3, 4, 5, 6,
        7, 8, 9, 10, 11, 12,
        13, 14, 15, 16, 17, 18,
        19, 20, 21, 22, 23, 24,
        25, 26, 27, 28, 29, 30,
        31, 32, 33, 34, 35, 36};
    byte[] expected = new byte[]{
        19, 13, 7, 1,
        20, 14, 8, 2,
        21, 15, 9, 3,
        22, 16, 10, 4,
        23, 17, 11, 5,
        24, 18, 12, 6,
        31, 32, 25, 26, 33, 34,
        27, 28, 35, 36, 29, 30};

    assertThat(YUVTransformer.rotate90degrees(original, 6, 4), is(expected));
  }


  @Test
  public void mirror_image() {
    byte[] original = new byte[]{
        1, 2, 3, 4, 5, 6,
        7, 8, 9, 10, 11, 12,
        13, 14, 15, 16, 17, 18,
        19, 20, 21, 22, 23, 24,
        25, 26, 27, 28, 29, 30,
        31, 32, 33, 34, 35, 36};
    byte[] expected = new byte[]{
        6, 5, 4, 3, 2, 1,
        12, 11, 10, 9, 8, 7,
        18, 17, 16, 15, 14, 13,
        24, 23, 22, 21, 20, 19,
        29, 30, 27, 28, 25, 26,
        35, 36, 33, 34, 31, 32};

    assertThat(YUVTransformer.mirror(original, 6, 4), is(expected));
  }

  @Test
  public void mirror_and_rotate_image() {
    byte[] original = new byte[]{
        1, 2, 3, 4, 5, 6,
        7, 8, 9, 10, 11, 12,
        13, 14, 15, 16, 17, 18,
        19, 20, 21, 22, 23, 24,
        25, 26, 27, 28, 29, 30,
        31, 32, 33, 34, 35, 36};
    byte[] expected = new byte[]{
        1, 7, 13, 19,
        2, 8, 14, 20,
        3, 9, 15, 21,
        4, 10, 16, 22,
        5, 11, 17, 23,
        6, 12, 18, 24,
        25, 26, 31, 32,
        27, 28, 33, 34,
        29, 30, 35, 36};

    assertThat(YUVTransformer.mirror(YUVTransformer.rotate90degrees(original, 6, 4), 4, 6), is(expected));
  }

}
