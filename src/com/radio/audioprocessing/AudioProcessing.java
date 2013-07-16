package com.radio.audioprocessing;

import com.radio.covertradio.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class AudioProcessing extends Activity implements OnClickListener {

	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	private RealDoubleFFT transformer;
	int blockSize = 256;

	
	boolean started = false;

	RecordAudio recordTask;

	ImageView imageView;
	Bitmap bitmap;
	Canvas canvas;
	Paint paint;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/*super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		startStopButton = (Button) this.findViewById(R.id.StartStopButton);
		startStopButton.setOnClickListener(this);*/

		transformer = new RealDoubleFFT(blockSize);

		imageView = (ImageView) this.findViewById(R.id.ImageView01);
		bitmap = Bitmap.createBitmap((int) 256, (int) 100,
				Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setColor(Color.GREEN);
		imageView.setImageBitmap(bitmap);
	}

	private class RecordAudio extends AsyncTask<Void, double[], Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				int bufferSize = AudioRecord.getMinBufferSize(frequency,
						channelConfiguration, audioEncoding);

				AudioRecord audioRecord = new AudioRecord(
						0x8, frequency,
						channelConfiguration, audioEncoding, bufferSize);
				
				short[] buffer = new short[blockSize];
				double[] toTransform = new double[blockSize];
				File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() 
	                     +"/CovertRadio/reverseme.pcm");
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
				

				audioRecord.startRecording();

				while (started) {
					int bufferReadResult = audioRecord.read(buffer, 0,
							blockSize);

					for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
						toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
						dos.writeShort(buffer[i]);
					}

					transformer.ft(toTransform);
					publishProgress(toTransform);
				}

				audioRecord.stop();
				 dos.flush();
	             dos.close();
			} catch (Throwable t) {
				Log.e("AudioRecord", "Recording Failed");
			}

			return null;
		}

		protected void onProgressUpdate(double[]... toTransform) {
			canvas.drawColor(Color.BLACK);

			for (int i = 0; i < toTransform[0].length; i++) {
				int x = i;
				int downy = (int) (100 - (toTransform[0][i] * 10));
				int upy = 100;

				canvas.drawLine(x, downy, x, upy, paint);
			}
			imageView.invalidate();
		}
	}

	public void onClick(View v) {
		if (started) {
			started = false;
			
			recordTask.cancel(true);
		} else {
			started = true;
			
			recordTask = new RecordAudio();
			recordTask.execute();
		}
	}
}