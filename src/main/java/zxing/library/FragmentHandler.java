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

import java.util.Collection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.ViewfinderResultPointCallback;
import com.google.zxing.client.android.camera.CameraManager;

public class FragmentHandler extends Handler {
	public static final String TAG = "FragmentHandler"; //$NON-NLS-1$

	private final DecodeThread decodeThread;
	private State state;
	private final CameraManager cameraManager;
	private final ZXingFragment fragment;

	private static enum State {
		PREVIEW, SUCCESS, DONE
	}
	
	public FragmentHandler(ZXingFragment fragment, Collection<BarcodeFormat> decodeFormats, String characterSet, CameraManager cameraManager) {
		this.fragment = fragment;
		decodeThread = new DecodeThread(fragment, decodeFormats, characterSet, new ViewfinderResultPointCallback(fragment.getViewfinderView()));
		decodeThread.start();
		state = State.SUCCESS;

		this.cameraManager = cameraManager;
		cameraManager.startPreview();
		restartPreviewAndDecode();
	}

	
	@Override
	public void handleMessage(Message message) {
		if (message.what == R.id.restart_preview) {
			Log.d(TAG, "Restart preview message received"); //$NON-NLS-1$
			restartPreviewAndDecode();
		} else if (message.what == R.id.decode_succeeded) {
			Log.d(TAG, "Decode succeeded message received"); //$NON-NLS-1$
			state = State.SUCCESS;
			Bundle bundle = message.getData();
			Bitmap barcode = null;
			float scaleFactor = 1.0f;
			if (bundle != null) {
				byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
				if (compressedBitmap != null) {
					barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
					// Mutable copy:
					barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
				}
				scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
			}
			fragment.handleDecode((Result) message.obj, barcode, scaleFactor);
		} else if (message.what == R.id.decode_failed) {
			// We're decoding as fast as possible, so when one decode fails,
			// start another.
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
		}
	}

	public void quitSynchronously() {
		state = State.DONE;
		cameraManager.stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
		quit.sendToTarget();
		try {
			// Wait at most half a second; should be enough time, and onPause()
			// will timeout quickly
			decodeThread.join(500L);
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.decode_succeeded);
		removeMessages(R.id.decode_failed);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
			fragment.drawViewfinder();
		}
	}

}
