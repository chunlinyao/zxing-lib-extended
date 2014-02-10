package com.google.zxing.client.android.camera;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class YUITransformerTest {


  @Test
  public void rotate_image(){
    byte[] original=new byte[]{
        1,2,3,4,5,6,
        7,8,9,10,11,12,
        13,14,15,16,17,18,
        19,20,21,22,23,24,
        25,26,27,28,29,30,
        31,32,33,34,35,36};
    byte[] expected=new byte[]{
        19,13,7,1,
        20,14,8,2,
        21,15,9,3,
        22,16,10,4,
        23,17,11,5,
        24,18,12,6,
        28,25,29,26,30,27,
        34,31,35,32,36,33};

    assertThat(YUITransformer.rotateYUV420Degree90(original,6,4),is(expected));

  }

}
