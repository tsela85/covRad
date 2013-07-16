package com.radio.covertradio;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;

import java.io.File;
import java.io.IOException;


public class RecAndPlay extends Activity
{
	private static final String TAG = "RecAndPlay";
	private static final int RADIO_AUDIO_SOURCE = 0x8;

	private static String mFileName = null;

	private MediaRecorder mRecorder = null;
	private MediaPlayer   mPlayer = null;
	private boolean mStartRecording = true;  
	private boolean mStartPlaying = true;
	private MediaPlayer mMediaPlayer;
	private Visualizer mVisualizer;

	private void log(String msg) {
		Log.i(TAG, msg);
	}

	private void onRecord(boolean start) {
		if (start) {
			startRecording();
		} else {
			stopRecording();
		}
	}

	private void onPlay(boolean start) {
		if (start) {
			startPlaying();
		} else {
			stopPlaying();
		}
	}

	private void startPlaying() {
		mPlayer = new MediaPlayer();
		try {
			/* 	mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
    			public void onCompletion(MediaPlayer mediaPlayer) {
    				stopPlaying();
    				mStartPlaying = false;
    			}
    		});*/
			mPlayer.setDataSource(mFileName);
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			log("prepare() failed");
		}
	}

	private void stopPlaying() {
		mPlayer.release();
		mPlayer = null;
	}

	private void startRecording() {		
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(RADIO_AUDIO_SOURCE);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		mRecorder.setOutputFile(mFileName);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			log("prepare() failed");
		}

		mRecorder.start();
	}

	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
	}
	
	public void setFileName(String name) {
		mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
		mFileName += "/CovertRadio/"+name+".3gp";	
	}


	public boolean recordButtonClick() {
		onRecord(mStartRecording);
		return mStartRecording = !mStartRecording;
	}



	public boolean playButtonClick() {
		onPlay(mStartPlaying);
		return mStartPlaying = !mStartPlaying;
	}


	public RecAndPlay(String name) {
		mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
		mFileName += "/CovertRadio";
		  File direct = new File(mFileName);
		   if(!direct.exists())		    
		        direct.mkdir(); 
		mFileName += "/"+name+".3gp";
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

	}

	@Override
	public void onPause() {
		super.onPause();
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}

		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}
	private static int findMaxIndex(double[] array) {
		double max = array[0];
		int maxIndex = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] > max) {
				max = array[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	public void analyze() {
		try {
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource(mFileName);

			mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				public void onCompletion(MediaPlayer mediaPlayer) {
					mVisualizer.setEnabled(false);
				}
			});

			int Id = mMediaPlayer.getAudioSessionId();

			mVisualizer = new Visualizer(Id);
			mVisualizer.setEnabled(false);
			mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
			mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
				private double matchedFreq;
				private int matched;
				private boolean startRecording;
				private String result;
				public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
					samplingRate = samplingRate / 1000;
					double[] magnitude = new double[bytes.length / 2];

					for (int i = 2, j = 0; i < bytes.length; i += 2, j++) {
						magnitude[j] = Math.pow((double) (bytes[i] * bytes[i] + bytes[i + 1] * bytes[i + 1]), 0.5);
					}
					double max = findMaxIndex(magnitude);  				
					double frequency = max * samplingRate / bytes.length;
					String output = "the frequency: " + max * samplingRate / bytes.length + " Hz";
					log(output);
					//if (frequency == matchedFreq) {
						matchedFreq = frequency;
						matched++;
						if ((matchedFreq == 86.1328125 || matchedFreq == 43.06640625 || matchedFreq == 172.265625 || matchedFreq == 215.33203125) && startRecording == false) {
							startRecording = true;
							log("started recording");
						}
						if (startRecording) {
							if (matchedFreq == 43.06640625) {
								log("frequency: " + matchedFreq);
								/*} else if (matchedFreq == 86.1328125) {
							startRecording = true;
							log("started recording");*/
							} else if (matchedFreq == 129.19921875) {
								startRecording = false;
								Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG);
								toast.show();
								log("stopped recording");
								result = "";
							} else if (matchedFreq == 172.265625) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 215.33203125) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 258.3984375) {
								log(" ");
								result += " ";
							} else if (matchedFreq == 301.46484375) {
								log("!");
								result += "!";
							} else if (matchedFreq == 344.53125) {
								log("\"");
								result += "\"";
							} else if (matchedFreq == 387.59765625) {
								log("#");
								result += "#";
							} else if (matchedFreq == 430.6640625) {
								log("$");
								result += "$";
							} else if (matchedFreq == 473.73046875) {
								log("%");
								result += "%";
							} else if (matchedFreq == 516.796875) {
								log("&");
								result += "&";
							} else if (matchedFreq == 559.86328125) {
								log("'");
								result += "'";
							} else if (matchedFreq == 602.9296875) {
								log("(");
								result += "(";
							} else if (matchedFreq == 645.99609375) {
								log(")");
								result += ")";
							} else if (matchedFreq == 689.0625) {
								log("*");
								result += "*";
							} else if (matchedFreq == 732.12890625) {
								log("+");
								result += "+";
							} else if (matchedFreq == 775.1953125) {
								log(",");
								result += ",";
							} else if (matchedFreq == 818.26171875) {
								log("-");
								result += "-";
							} else if (matchedFreq == 861.328125) {
								log(".");
								result += ".";
							} else if (matchedFreq == 904.39453125) {
								log("/");
								result += "/";
							} else if (matchedFreq == 947.4609375) {
								log("0");
								result += "0";
							} else if (matchedFreq == 990.52734375) {
								log("1");
								result += "1";
							} else if (matchedFreq == 1033.59375) {
								log("2");
								result += "2";
							} else if (matchedFreq == 1076.66015625) {
								log("3");
								result += "3";
							} else if (matchedFreq == 1119.7265625) {
								log("4");
								result += "4";
							} else if (matchedFreq == 1162.79296875) {
								log("5");
								result += "5";
							} else if (matchedFreq == 1205.859375) {
								log("6");
								result += "6";
							} else if (matchedFreq == 1248.92578125) {
								log("7");
								result += "7";
							} else if (matchedFreq == 1291.9921875) {
								log("8");
								result += "8";
							} else if (matchedFreq == 1335.05859375) {
								log("9");
								result += "9";
							} else if (matchedFreq == 1378.125) {
								log(":");
								result += ":";
							} else if (matchedFreq == 1421.19140625) {
								log(";");
								result += ";";
							} else if (matchedFreq == 1464.2578125) {
								log("<");
								result += "<";
							} else if (matchedFreq == 1507.32421875) {
								log("=");
								result += "=";
							} else if (matchedFreq == 1550.390625) {
								log(">");
								result += ">";
							} else if (matchedFreq == 1593.45703125) {
								log("?");
								result += "?";
							} else if (matchedFreq == 1636.5234375) {
								log("@");
								result += "@";
							} else if (matchedFreq == 1679.58984375) {
								log("A");
								result += "A";
							} else if (matchedFreq == 1722.65625) {
								log("B");
								result += "B";
							} else if (matchedFreq == 1765.72265625) {
								log("C");
								result += "C";
							} else if (matchedFreq == 1808.7890625) {
								log("D");
								result += "D";
							} else if (matchedFreq == 1851.85546875) {
								log("E");
								result += "E";
							} else if (matchedFreq == 1894.921875) {
								log("F");
								result += "F";
							} else if (matchedFreq == 1937.98828125) {
								log("G");
								result += "G";
							} else if (matchedFreq == 1981.0546875) {
								log("H");
								result += "H";
							} else if (matchedFreq == 2024.12109375) {
								log("I");
								result += "I";
							} else if (matchedFreq == 2067.1875) {
								log("J");
								result += "J";
							} else if (matchedFreq == 2110.25390625) {
								log("K");
								result += "K";
							} else if (matchedFreq == 2153.3203125) {
								log("L");
								result += "L";
							} else if (matchedFreq == 2196.38671875) {
								log("M");
								result += "M";
							} else if (matchedFreq == 2239.453125) {
								log("N");
								result += "N";
							} else if (matchedFreq == 2282.51953125) {
								log("O");
								result += "O";
							} else if (matchedFreq == 2325.5859375) {
								log("P");
								result += "P";
							} else if (matchedFreq == 2368.65234375) {
								log("Q");
								result += "Q";
							} else if (matchedFreq == 2411.71875) {
								log("R");
								result += "R";
							} else if (matchedFreq == 2454.78515625) {
								log("S");
								result += "S";
							} else if (matchedFreq == 2497.8515625) {
								log("T");
								result += "T";
							} else if (matchedFreq == 2540.91796875) {
								log("U");
								result += "U";
							} else if (matchedFreq == 2583.984375) {
								log("V");
								result += "V";
							} else if (matchedFreq == 2627.05078125) {
								log("W");
								result += "W";
							} else if (matchedFreq == 2670.1171875) {
								log("X");
								result += "X";
							} else if (matchedFreq == 2713.18359375) {
								log("Y");
								result += "Y";
							} else if (matchedFreq == 2756.25) {
								log("Z");
								result += "Z";
							} else if (matchedFreq == 2799.31640625) {
								log("[");
								result += "[";
							} else if (matchedFreq == 2842.3828125) {
								log("\\");
								result += "\\";
							} else if (matchedFreq == 2885.44921875) {
								log("]");
								result += "]";
							} else if (matchedFreq == 2928.515625) {
								log("^");
								result += "^";
							} else if (matchedFreq == 2971.58203125) {
								log("_");
								result += "_";
							} else if (matchedFreq == 3014.6484375) {
								log("`");
								result += "`";
							} else if (matchedFreq == 3057.71484375) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3100.78125) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3186.9140625) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3229.98046875) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3273.046875) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3445.3125) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3488.37890625) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3617.578125) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3660.64453125) {
								log("frequency: " + matchedFreq);
							} else if (matchedFreq == 3746.77734375) {
								log("frequency: " + matchedFreq);
							}
							matched = 0;
						}
				}
				public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {}
			}, 2000 /*Visualizer.getMaxCaptureRate() / 2*/, true, true);
			mVisualizer.setEnabled(true);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
		} catch (Exception e) {
			log("error: " + e);
		}	
	}
}