package com.radio.covertradio;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.androino.ttt.FSKDecoder;

import ca.uol.aig.fftpack.RealDoubleFFT;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.Equalizer;
import android.media.audiofx.NoiseSuppressor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class RadioActivity extends Activity implements View.OnClickListener{

	private String TAG = "Radio!!!";
	private Toast toast;
	private boolean deviceSupported;
	
	private FMPlayerServiceWrapper mFmRadioServiceWrapper; // Enable Radio control 
//	private IFMRadioNotification mRadioNotification = new GalaxyRadioNotification();
	private AudioManager aManager = null;
	private RecAndPlay recAndPlay = null;
	//private AudioProcessing aProcessing = null;
	
	private static final int RADIO_AUDIO_STREAM = 0xa;
	private static final String FM_RADIO_SERVICE_NAME = "FMPlayer";
	private static final String DEF_FREQ = "100200";
	
	int frequency = 44100;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;// AudioFormat.CHANNEL_IN_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	private RealDoubleFFT transformer;
	int blockSize = 512;
	String fileName;

	
	boolean started = false;
	boolean isPlaying = false;
	boolean isRecording = false;
	PlayAudio playTask;
	DataOutputStream dos = null;


	RecordAudio recordTask;
//
	//Equalizer equalizer_;

	ImageView imageView;
	Bitmap bitmap;
	Canvas canvas;
	Paint paint;
	
//	private Handler mClientHandler;
//	private FSKDecoder mDecoder;

	private void log(String msg) {
		Log.i(TAG, msg);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		deviceSupported = isRadioSupported();
		if (deviceSupported) { // device is not supported so avoid exceptions
			mFmRadioServiceWrapper = new FMPlayerServiceWrapper(getSystemService(FM_RADIO_SERVICE_NAME));
	//		mFmRadioServiceWrapper.setListener(mRadioNotification);
			aManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			recAndPlay = new RecAndPlay("radioRec");
		//	aProcessing = new AudioProcessing();
		}

		((Button)findViewById(R.id.button1)).setOnClickListener(this);
		((Button)findViewById(R.id.button2)).setOnClickListener(this);
		((Button)findViewById(R.id.button3)).setOnClickListener(this);
		((Button)findViewById(R.id.button4)).setOnClickListener(this);
		((Button)findViewById(R.id.StartStopButton)).setOnClickListener(this);
		((Button)findViewById(R.id.button6)).setOnClickListener(this);
		((Button)findViewById(R.id.Button7)).setOnClickListener(this);

		setFileName(((EditText)findViewById(R.id.editText2)).getText().toString()); //set file nume to text view
		transformer = new RealDoubleFFT(blockSize);

		imageView = (ImageView) this.findViewById(R.id.ImageView01);
		bitmap = Bitmap.createBitmap((int) 512, (int) 200,
				Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setColor(Color.RED);
		imageView.setImageBitmap(bitmap);
		
//		this.mDecoder = new FSKDecoder(this.mClientHandler);
		//this.mDecoder.start();
	}
	
	public void setFileName(String name) {
		fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
		fileName += "/CovertRadio/"+name+".pcm";	
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
/*	@Override
	public void onStop() {
		if (deviceSupported) {
			mFmRadioServiceWrapper.off();
			recAndPlay.onPause();
		}
	}*/

	@Override
	public void onClick(View v) {
		toast = Toast.makeText(getApplicationContext(), "Pressing", Toast.LENGTH_SHORT);
		if (false) {//(!deviceSupported) { //do not enable pushing buttons if device not supported
			toast.setText("Device is not supported");
			log("Device is not supported");	
		} else { // device is supported
			switch(v.getId()) {
			case R.id.button1:
				toast.setText("Start Radio");
				log("Start button pushed");	
				String temp = ((EditText)findViewById(R.id.editText1)).getText().toString(); //get the frequency text entered
				int currentFreq = Integer.parseInt(temp); //parse to integer
				if (currentFreq < 88000 || currentFreq > 108000) {
					toast.setText("Invalid freq");
					currentFreq = Integer.parseInt(DEF_FREQ);
					((EditText)findViewById(R.id.editText1)).setText(DEF_FREQ);
				}					
				turnRadioOn();
				mFmRadioServiceWrapper.tune(currentFreq);
				mFmRadioServiceWrapper.setSpeakerOn(true);				
				aManager.setStreamVolume (RADIO_AUDIO_STREAM, aManager.getStreamVolume(RADIO_AUDIO_STREAM), 0x0);
				break;
			case R.id.button2:
				toast.setText("Stop Radio");
				log("Stop button pushed");
				turnRadioOff();					
				break;
			case R.id.button3:
				//recAndPlay.setFileName(((EditText)findViewById(R.id.editText2)).getText().toString()); //get the frequency text entered
				if (!started) {
					started = true;
					recordTask = new RecordAudio();
					recordTask.execute();
				}
				setFileName(((EditText)findViewById(R.id.editText2)).getText().toString()); //set file name to text view
				File file = new File(fileName);
				try {
				if (!isRecording) {
					dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
					isRecording = true;

				}else {
					isRecording = false;
					dos.flush();
					dos.close();
				}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				toast.setText("Record");
				log("Record button pushed");				
			  /*	if (recAndPlay.recordButtonClick())
			  		toast.setText("Stop recording");
		    	else 
		    		toast.setText("Start recording");*/
				break;
			case R.id.button4:
				//recAndPlay.setFileName(((EditText)findViewById(R.id.editText2)).getText().toString()); //get the frequency text entered
				setFileName(((EditText)findViewById(R.id.editText2)).getText().toString()); //set file name to text view
				toast.setText("Play");
				log("Play button pushed");
			//	recAndPlay.analyze();
			/*	if (recAndPlay.playButtonClick())
			  		toast.setText("Stop playing");
		    	else 
		    		toast.setText("Start playing");*/
				//PlayAudioFileViaAudioTrack(fileName);
				
				if (!isRecording) {
					if (started) {
						started = false;				
						recordTask.cancel(true);
					}
					play();
				}
				break;
			case R.id.StartStopButton:
				if (started) {
					started = false;				
					recordTask.cancel(true);
				} else {
					started = true;
					recordTask = new RecordAudio();
					recordTask.execute();
				}
				break;
			case R.id.button6:
				currentFreq = 100200;
				((EditText)findViewById(R.id.editText1)).setText("100200");
				break;
			case R.id.Button7:
				currentFreq = 99800;
				((EditText)findViewById(R.id.editText1)).setText("99800");
				break;
			default:
				break;
			}
		}
		toast.show();
	}
	
	public boolean isRadioSupported() {
    	try {
			Class.forName("com.samsung.media.fmradio.FMEventListener");
			log("Radio class found! device is supported");
			return true;
		} catch (ClassNotFoundException e) {
			log("Radio class NOT found! device is NOT supported");
			return false;
		}
	}
	
	public boolean turnRadioOn() {
		if (!mFmRadioServiceWrapper.isOn()) {
			try {
				mFmRadioServiceWrapper.on();
				log("Turning Radio On");
				return true;
			} catch (FMPlayerException e) {
				toast = Toast.makeText(getApplicationContext(), "Unable to start Radio", Toast.LENGTH_SHORT);
				switch (e.getCode()) {
				case FMPlayerException.AIRPLANE_MODE:
					toast.setText("Airplane Mode is on");
					log("Airplane Mode is on");
					break;
				case FMPlayerException.HEAD_SET_IS_NOT_PLUGGED:
					toast.setText("No earphones connected");
					log("No earphones connected");
					break;
				}
				toast.show();
				return false;
			}
		}
		toast = Toast.makeText(getApplicationContext(), "Radio already On", Toast.LENGTH_SHORT);
		toast.show();
		log("Radio already On");
		return false;
	}
	
	public void turnRadioOff() {
		if (mFmRadioServiceWrapper.isOn()) {
			mFmRadioServiceWrapper.off();
		}
	}
	
	private class RecordAudio extends AsyncTask<Void, double[], Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				int bufferSize = AudioRecord.getMinBufferSize(frequency,
						channelConfiguration, audioEncoding);

				AudioRecord audioRecord = new AudioRecord(0x8, frequency,channelConfiguration, audioEncoding, bufferSize);
				
				// log("noise suppresor is "+ NoiseSuppressor.create(audioRecord.getAudioSessionId()).toString());
				// log("noise suppresor is "+ AcousticEchoCanceler.create(audioRecord.getAudioSessionId()).toString());
				 //log("noise suppresor is "+ AutomaticGainControl.create(audioRecord.getAudioSessionId()).toString());
				 
				short[] buffer = new short[blockSize];
				//byte[] buffer = new byte[blockSize];
				double[] toTransform = new double[blockSize];
				
				

				audioRecord.startRecording();

				while (started) {
					int bufferReadResult = audioRecord.read(buffer, 0,blockSize);

					for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
						toTransform[i] = (double) buffer[i] / 32768.0; // signed 16bit
						if (isRecording)
							dos.writeShort(buffer[i]);
						//	dos.writeByte(buffer[i]);
					}

					transformer.ft(toTransform);
					publishProgress(toTransform);
				}

				audioRecord.stop();
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
				
				//canvas.drawLine(2*x, downy, 2*x, upy, paint);
				//canvas.drawLine(2*x+1, downy, 2*x+1, upy, paint);
				canvas.drawLine(x, downy, x, upy, paint);
			}
						
			imageView.invalidate();
		}
		

	}
/*	
	private class PlayAudio extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			isPlaying = true;

			int bufferSize = AudioTrack.getMinBufferSize(frequency,
					channelConfiguration, audioEncoding);
			short[] audiodata = new short[bufferSize / 4];

			try {
				DataInputStream dis = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								fileName)));

				AudioTrack audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC, frequency,
						channelConfiguration, audioEncoding, bufferSize,
						AudioTrack.MODE_STREAM);

				audioTrack.play();

				while (isPlaying && dis.available() > 0) {
					int i = 0;
					while (dis.available() > 0 && i < audiodata.length) {
						audiodata[i] = dis.readShort();
						i++;
					}
					audioTrack.write(audiodata, 0, audiodata.length);
				}

				dis.close();

			} catch (Throwable t) {
				Log.e("AudioTrack", "Playback Failed");
			}

			return null;
		}
	} */
	private class PlayAudio extends AsyncTask<Void, double[], Void> {
		@Override
		protected Void doInBackground(Void... params) {
			isPlaying = true;

			int bufferSize = AudioTrack.getMinBufferSize(frequency,
					channelConfiguration, audioEncoding);


			try {
				DataInputStream dis = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								fileName)));

				AudioTrack audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC, frequency,
						channelConfiguration, audioEncoding, bufferSize,
						AudioTrack.MODE_STREAM);
				aManager.setSpeakerphoneOn(true);
				/*
		        equalizer_ = new Equalizer(0, audioTrack.getAudioSessionId());
		        equalizer_.setEnabled(true);
		        short bands = equalizer_.getNumberOfBands();
		        Log.d("EqualizerSample", "NumberOfBands: " + bands);

		        short minEQLevel = equalizer_.getBandLevelRange()[0];
		        short maxEQLevel = equalizer_.getBandLevelRange()[1];
		        Log.d("EqualizerSample", "minEQLevel: " + String.valueOf(minEQLevel));            
		        Log.d("EqualizerSample", "maxEQLevel: " + String.valueOf(maxEQLevel));            

		        for (short i = 0; i < bands/2; i++) {
		                Log.d("EqualizerSample", i + String.valueOf(equalizer_.getCenterFreq(i) / 1000) + "Hz");
		               // equalizer_.setBandLevel(i, (short)((minEQLevel + maxEQLevel) / 2));
		                equalizer_.setBandLevel(i, (short)(minEQLevel));
		        }*/
				
				short[] buffer = new short[blockSize];
			//	byte[]  buffer = new byte[blockSize];
				double[] toTransform = new double[blockSize];

				audioTrack.play();

				while (isPlaying && dis.available() > 0) {
					int i = 0;
					while (dis.available() > 0 && i < buffer.length) {
						
						buffer[i] = dis.readShort();
						//buffer[i] = dis.readByte();
						toTransform[i] = (double) buffer[i] / 32768.0; // signed 16bit
						i++;
					}
					//mDecoder.addSound(buffer, buffer.length);	
					//mDecoder.decodeFSK(buffer);
					

					
					audioTrack.write(buffer, 0, buffer.length);
					transformer.ft(toTransform);
					publishProgress(toTransform);
					//calculate(frequency, buffer);	

				}
				dis.close();

			} catch (Throwable t) {
				Log.e("AudioTrack", "Playback Failed");
			}

			return null;
		}
		
		
		
		public int calculate(int sampleRate, short[] audioData) {

		    int numSamples = audioData.length;
		    int numCrossing = 0;
		    int max = 0;
		    int sum = 0;
		    for (int p = 0; p < numSamples-1; p++)
		    {
		        if ((audioData[p] > 0 && audioData[p + 1] <= 0) || 
		            (audioData[p] < 0 && audioData[p + 1] >= 0))
		        {
		            numCrossing++;
		        }
		        if (Math.abs(audioData[p]) > max)
		        	max = Math.abs(audioData[p]);
		        sum +=Math.abs(audioData[p]);
		    }

		    float numSecondsRecorded = (float)numSamples/(float)sampleRate;
		    float numCycles = numCrossing/2;
		    float frequency = numCycles/numSecondsRecorded;

			log("The freq: " + frequency + " Max: "+ max+ " avg is: "+ sum/numSamples);	
		    return (int)frequency;
		}
		
		protected void onProgressUpdate(double[]... toTransform) {
			canvas.drawColor(Color.BLACK);
			int count = 0;
			double sum = 0;
			
			for (int i = 0; i < toTransform[0].length; i++) {
				int x = i;
				int downy = 100;
			//	if (Math.abs(toTransform[0][i]) > 5) {
					downy = (int) (100 - (toTransform[0][i] * 10));
			//		count ++;
			//		sum+=i;
			//	}
				int upy = 100;
				
				//canvas.drawLine(2*x, downy, 2*x, upy, paint);
				//canvas.drawLine(2*x+1, downy, 2*x+1, upy, paint);
				canvas.drawLine(x, downy, x, upy, paint);
			}
			log("the avg of freq is: " + sum/count+ " count: " + count);
		//	log("The freq: " + calculate(frequency, toTransform[0]));	
			
			imageView.invalidate();
		}
	}
	
	public void play() {
		playTask = new PlayAudio();
		playTask.execute();

	}
	public void stopPlaying() {
		isPlaying = false;
	}
	
	private void PlayAudioFileViaAudioTrack(String filePath) {

		if (filePath==null)
			return;
		int intSize = android.media.AudioTrack.getMinBufferSize(frequency, channelConfiguration,audioEncoding); 

		AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, AudioFormat.CHANNEL_OUT_MONO,audioEncoding, intSize, AudioTrack.MODE_STREAM); 

		int count = blockSize;
		//Reading the file..
		File file = new File(filePath);

		byte[] byteData = new byte[(int)count];
		FileInputStream in = null;
		try {
			in = new FileInputStream( file );

			int bytesread = 0, ret = 0;
			int size = (int) file.length();
			at.play();
			while (bytesread < size) { 
				try {
					ret = in.read( byteData,0, count);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				} 
				if (ret != -1) {  // Write the byte array to the track 
					at.write(byteData,0, ret); 
					bytesread += ret; 
				} else
					break; 
			} 
			in.close(); 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		at.stop(); 
		at.release(); 
	}

}

