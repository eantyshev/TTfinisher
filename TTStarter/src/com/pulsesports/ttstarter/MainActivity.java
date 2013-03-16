package com.pulsesports.ttstarter;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.*;
import android.content.*;
import android.media.*;
import android.hardware.*;
import android.hardware.Camera.Size;
import android.view.ViewGroup.LayoutParams;
import android.content.res.Configuration;

import java.io.*;
import android.content.pm.*;
import java.util.*;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback, Camera.PreviewCallback, Camera.AutoFocusCallback, Runnable
{
Double lastStart = 0.0;
int currentStartType;
String picturesPath;
MenuItem menuItemReset;
MenuItem menuItemTT;
MenuItem menuItemGandicap;
MenuItem menuItemRestore;
static final int sameIntervalsType = 1;
static final int fromFileIntervals = 2;
boolean startedTimer = false;
Double timeInterval = 1.0;
Long firstNumber = new Long(1);
Double startDelay = 1.0;
DecimalFormat waitTimeFormatter;
DecimalFormat startNumberFormatter;
Camera cam;
SurfaceView camPreview;
SurfaceHolder camSurfaceHolder;
boolean applicationActive = false;
Long nextNumber = new Long(1);
MediaPlayer beepPlayer;
float sizeIncreaser = new Float(5.0);
LinkedList<Competitor> startList;

	private class Competitor{
		SimpleDateFormat timeParser ;
		public String number;
		public Double startDelay;
		public String name;
		public Competitor() {
			timeParser = new SimpleDateFormat("dd HH:mm:ss");
		}
		public Competitor(String competitorInfo) {
			this();
			number = competitorInfo.split("#")[0];
			name = competitorInfo.split("#")[1];
			try
			{
				String timeStr = competitorInfo.split("#")[9];
				String millitmStr = timeStr.split("\\.")[1];
				Date myDate = timeParser.parse(timeStr.split("\\.")[0]);
				startDelay = (myDate.getHours()*60+myDate.getMinutes())*60+myDate.getSeconds() + Double.parseDouble(millitmStr)/Math.pow(10,millitmStr.length());
			}
			catch (ParseException e)
			{}
		}
	};
	private Handler handler = new Handler();
		public void run() {
			if(startedTimer) {
				Double elaspedSeconds = (SystemClock.elapsedRealtime() - 
					((Chronometer)findViewById(R.id.chronometerCurrentTime)).getBase())/1000.0;
				Double timeToStart = 0.0;
				switch (currentStartType){
					case sameIntervalsType:
						nextNumber = new Double(Math.max(0.0,(elaspedSeconds-startDelay)/timeInterval + 1.0) + firstNumber).longValue();
						timeToStart = (startDelay) + (nextNumber-firstNumber)*timeInterval - (elaspedSeconds-0.5);
						if(lastStart<elaspedSeconds-0.3) {
							((TextView)findViewById(R.id.textViewTimeTillNextStart)).
								setText(waitTimeFormatter.format(
											(nextNumber>firstNumber)?timeToStart%timeInterval:timeToStart
										));
									}
						break;
					case fromFileIntervals:
						Competitor nearestCompetitor = new Competitor("0#ALL STARTED########0 99:99:99.999##");
						for(Competitor compet:startList) {
							if(nearestCompetitor.startDelay > compet.startDelay &&
							compet.startDelay > elaspedSeconds - startDelay) {
								nearestCompetitor = compet;	
							}
						}
						nextNumber = new Double(Double.parseDouble(nearestCompetitor.number)).longValue();
						timeToStart = nearestCompetitor.startDelay + startDelay - (elaspedSeconds-0.5);
						if(lastStart<elaspedSeconds-0.3) {
							((TextView)findViewById(R.id.textViewNextNumberTitle)).setText(nearestCompetitor.name);
							((TextView)findViewById(R.id.textViewTimeTillNextStart)).
								setText(waitTimeFormatter.format(timeToStart));
						}
						break;
					default:
						break;
				}

				if((!((TextView)findViewById(R.id.textViewNextNumber)).
				   getText().toString().equalsIgnoreCase(startNumberFormatter.format(nextNumber)))&&
				   (!((TextView)findViewById(R.id.textViewTimeTillNextStart)).
				   getText().toString().equalsIgnoreCase("GO!")))
				   {//here should be beep signal on start and photo from frontal camera
					((TextView)findViewById(R.id.textViewTimeTillNextStart)).
						setText("GO!");
					beepPlayer.start();
					if(cam!=null) {
						cam.autoFocus(this);
					}
					lastStart = elaspedSeconds;
		

				}
//				if(lastStart<elaspedSeconds-0.5) {
				((TextView)findViewById(R.id.textViewNextNumber)).setText(startNumberFormatter.format(nextNumber));
//				}
				if(applicationActive){
					handler.postDelayed(this,50);
				}
			}
		}
	public void startTimer(){
		handler.postDelayed(this,500);
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
			picturesPath = 
				Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/TTStarter";
		}else{
			picturesPath="DCIM/TTStarter";
		}
		currentStartType = sameIntervalsType;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		((Button)findViewById(R.id.buttonStart)).setOnClickListener(
		new Button.OnClickListener() {

	public void onClick(View v) {
		((Chronometer)findViewById(R.id.chronometerCurrentTime)).setBase(SystemClock.elapsedRealtime());
		((Chronometer)findViewById(R.id.chronometerCurrentTime)).start();
		startDelay = Double.parseDouble(((EditText)findViewById(R.id.editTextWaitBeforeFirst)).getText().toString());
		switch (currentStartType) {
			case sameIntervalsType:
				firstNumber = Long.parseLong(((EditText)findViewById(R.id.editTextFirstNumber)).getText().toString());
				timeInterval = Double.parseDouble(((EditText)findViewById(R.id.editTextTimeInterval)).getText().toString());
				((TextView)findViewById(R.id.textViewNextNumberTitle)).setText("Next Number");
				break;
			case fromFileIntervals:
			startList = new LinkedList<Competitor>();
				try
				{
					BufferedReader br = 
						new BufferedReader(
						new FileReader(
							new File(
								((EditText)findViewById(R.id.editTextStartProtocolFilePath)).getText().toString()
							)));
					String nextLine;
					while((nextLine=br.readLine()) != null) {
						startList.addLast(new Competitor(nextLine));
					}
					br.close();
				}
				catch (Exception e)
				{
					((EditText)findViewById(R.id.editTextFirstNumber)).setText(e.getMessage());
				}
				break;
			default:
				break;
				
		}
		startedTimer = true;
		startTimer();
		switchStartTypes();
	}
	
	});
	
	
		beepPlayer = MediaPlayer.create(getApplicationContext(),R.raw.start_signal);
		waitTimeFormatter = new DecimalFormat("#0");
		startNumberFormatter = new DecimalFormat("#0");
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
		getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
		camPreview = (SurfaceView)findViewById(R.id.surfaceViewCameraPreview);
		camSurfaceHolder = camPreview.getHolder();
		camSurfaceHolder.addCallback(this);
		camSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		 ((EditText)findViewById(R.id.editTextStartProtocolFilePath)).
			 setText(picturesPath + "/start.txt");
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu main_menu) {
		getMenuInflater().inflate(R.menu.main_menu,main_menu);
		menuItemReset = main_menu.findItem(R.id.menuReset);
		menuItemRestore = main_menu.findItem(R.id.menuRestore);
		menuItemTT = main_menu.findItem(R.id.menuTimeTrialStart);
		menuItemGandicap = main_menu.findItem(R.id.menuGandicapsStart);
		switchStartTypes();
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menuReset:
				if(confirmOperation()){
					((Chronometer)findViewById(R.id.chronometerCurrentTime)).stop();
					((Chronometer)findViewById(R.id.chronometerCurrentTime)).setText("00:00");
					startedTimer = false;
					switchStartTypes();
				}
				break;
			case R.id.menuRestore:
				((Chronometer)findViewById(R.id.chronometerCurrentTime)).start();
				startedTimer = true;
				switchStartTypes();
				startTimer();
				break;
			case R.id.menuTimeTrialStart:
				currentStartType = sameIntervalsType;
				switchStartTypes();
				break;
			case R.id.menuGandicapsStart:
				currentStartType = fromFileIntervals;
				switchStartTypes();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	private void switchStartTypes() {
			enableDisableControls();
			return;	
	}
	private void enableDisableControls() {
		boolean showAll = !startedTimer;
		boolean showTT = (currentStartType == sameIntervalsType)&&!startedTimer;
		boolean showGandicaps = (currentStartType == fromFileIntervals)&&!startedTimer;
		
		addRemoveView(R.id.buttonStart,showAll);
		addRemoveView(R.id.editTextFirstNumber,showTT);
		addRemoveView(R.id.editTextTimeInterval,showTT);
		addRemoveView(R.id.editTextWaitBeforeFirst,showAll);
		addRemoveView(R.id.textViewFirstNumber,showTT);
		addRemoveView(R.id.textViewStartDelay,showAll);
		addRemoveView(R.id.textViewTimeInterval,showTT);
		addRemoveView(R.id.textViewStartProtocolFilePath,showGandicaps);
		addRemoveView(R.id.editTextStartProtocolFilePath,showGandicaps);
		addRemoveView(R.id.buttonSelectFile,showGandicaps);
		addRemoveMenu(menuItemGandicap,showTT);
		addRemoveMenu(menuItemTT,showGandicaps);
		addRemoveMenu(menuItemRestore,showAll);
		addRemoveMenu(menuItemReset,startedTimer);
	}
	private void addRemoveView(int viewId,boolean enableControls) {
		findViewById(viewId).setVisibility(enableControls?View.VISIBLE:View.GONE);
	}
	private void addRemoveMenu(MenuItem menuItem,boolean enableMenu) {
//		menuItem.getItemId();
		
		menuItem.setVisible(enableMenu);
//		return;
	}

	
	public boolean confirmOperation() {
		return true;
	}


    @Override
    protected void onResume()
    {
//		switchStartTypes();
		applicationActive = true;
		handler.postDelayed(this,50);
        super.onResume();
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
		getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
        cam = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
		}
    }

    @Override
    protected void onPause()
    {
		applicationActive =false;
        super.onPause();
        if (cam != null)
        {
            cam.setPreviewCallback(null);
            cam.stopPreview();
            cam.release();
            cam = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
		getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
        try
        {
            cam.setPreviewDisplay(holder);
            cam.setPreviewCallback(this);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        Size previewSize = cam.getParameters().getPreviewSize();
        float aspect = (float) previewSize.width / previewSize.height;
        int previewSurfaceWidth = camPreview.getWidth();
        int previewSurfaceHeight = camPreview.getHeight();
        LayoutParams lp = camPreview.getLayoutParams();
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
        {
	//		camPreview.setRotation(90);
            cam.setDisplayOrientation(90);
            lp.height = previewSurfaceHeight;
            lp.width = (int) (previewSurfaceHeight / aspect);
        }
        else
        {
	//		camPreview.setRotation(90);
            cam.setDisplayOrientation(0);
            lp.width = previewSurfaceWidth;
            lp.height = (int) (previewSurfaceWidth / aspect);
        }
        camPreview.setLayoutParams(lp);
	
        cam.startPreview();
		}
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }
    @Override
    public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
    {

        try
         {
			//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            File saveDir = new File(picturesPath);
            if (!saveDir.exists())
            {
                saveDir.mkdirs();
            }
            FileOutputStream os = new FileOutputStream(picturesPath + String.format("/PulseSports_%s_%d.jpg",
			startNumberFormatter.format(nextNumber-1), System.currentTimeMillis()));
            os.write(paramArrayOfByte);
            os.close();
        }
        catch (Exception e)
        {
        }
        paramCamera.startPreview();
    }

    @Override
    public void onAutoFocus(boolean paramBoolean, Camera paramCamera)
    {
        if (paramBoolean)
        {
			if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
			{
				//		camPreview.setRotation(90);
				cam.setDisplayOrientation(90);
				}
//			paramCamera.setDisplayOrientation(0);
            paramCamera.takePicture(null, null, null, this);
        }
    }

    @Override
    public void onPreviewFrame(byte[] paramArrayOfByte, Camera paramCamera)
    {
    }
}


