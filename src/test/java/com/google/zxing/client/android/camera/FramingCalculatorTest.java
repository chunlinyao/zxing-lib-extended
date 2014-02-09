package com.google.zxing.client.android.camera;

import android.graphics.Rect;

import com.google.zxing.client.android.RobolectricGradleTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(emulateSdk = 18)
public class FramingCalculatorTest {

  @Test
  public void test_framing_rectangle_landscape_mode() throws IOException {
    FramingCalculator framingCalculator = new FramingCalculator(new Resolution(1280, 800), new Resolution(2592, 1944), 90);
    Rect framingRect = framingCalculator.getFramingRect();
    assertThat(framingRect, is(not(nullValue())));
    assertThat(framingRect.top, is(200));
    assertThat(framingRect.left, is(340));
    assertThat(framingRect.right, is(940));
    assertThat(framingRect.bottom, is(600));
  }

  @Test
  public void test_framing_rectangle_portrait_mode() throws IOException {
    FramingCalculator framingCalculator = new FramingCalculator(new Resolution(1280, 800), new Resolution(2592, 1944), 0);
    Rect framingRect = framingCalculator.getFramingRect();
    assertThat(framingRect, is(not(nullValue())));
    assertThat(framingRect.top, is(440));
    assertThat(framingRect.left, is(100));
    assertThat(framingRect.right, is(700));
    assertThat(framingRect.bottom, is(840));
  }


  @Test
  public void test_framing_rectangle_preview_portrait_mode() throws IOException {
    FramingCalculator framingCalculator = new FramingCalculator(new Resolution(1280, 800), new Resolution(480, 368), 0);
    Rect framingRect = framingCalculator.getFramingRectInPreview();
    assertThat(framingRect, is(not(nullValue())));
//    assertThat(framingRect.left, is(37));
    assertThat(framingRect.left, is(46));
    assertThat(framingRect.top, is(165));
//    assertThat(framingRect.top, is(202));
//    assertThat(framingRect.right, is(262));
    assertThat(framingRect.right, is(322));
//    assertThat(framingRect.bottom, is(386));
    assertThat(framingRect.bottom, is(315));
  }

}
