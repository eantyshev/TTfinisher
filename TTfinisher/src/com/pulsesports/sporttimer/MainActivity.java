package com.pulsesports.sporttimer;

import android.app.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.security.*;
import java.util.*;
import java.io.*;
import org.apache.commons.net.ftp.*;
import java.net.*;
import java.text.*;

public class MainActivity extends Activity implements Runnable
{

	public void run()
	{
		TextView operationStatus = (TextView)findViewById(R.id.editTextStatus);
		operationStatus.setText("Current status: " + currentStatus);
		Handler handler = new Handler();
		handler.postDelayed(this, 1000);
	}

	String ftpServer;
	String ftpFolder;
	String ftpLogin;
	String ftpPassword;
	String pointNumber;
	String defaultRootDirectory;
	String currentBackupFile;
	String currentStatus = "none";


	public boolean addNumber(String number)
	{
		EditText textResults = (EditText)findViewById(R.id.editTextResults);
		String timeStamp = new SimpleDateFormat("yy/MM/dd_HH:mm:ss.S").format(Calendar.getInstance().getTime());
		String backupFileName = timeStamp + ".txt";

		textResults.setText(
			number + 
			"#" + timeStamp +
			"#nextLap#" + "\n" +
			textResults.getText().toString());

		writeFile(defaultRootDirectory,
				  "results.txt", textResults.getText().toString());
		writeFile(defaultRootDirectory + "/timerBackup", backupFileName, textResults.getText().toString());
		if (openFTPConfig(defaultRootDirectory))
		{
			currentBackupFile = defaultRootDirectory + "/timerBackup/" + backupFileName;
			currentStatus = "uploading " + currentBackupFile;
			Thread upl = new Thread(uploader);
			upl.start();
		}
		return true;
	}

	public Runnable uploader = new Runnable() {
		@Override
		public void run()
		{
			String uploadFileName = (pointNumber.equals("0")) ?"results.txt": "results_" + pointNumber + ".txt";
			String backupFileName = currentBackupFile;
			FTPClient ftpClient = new FTPClient();
			try
			{

				/*			if (android.os.Build.VERSION.SDK_INT > 9) {
				 StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
				 StrictMode.setThreadPolicy(policy);
				 }*/
				ftpClient.connect(InetAddress.getByName(ftpServer));
				ftpClient.login(ftpLogin, ftpPassword);
				if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
				{
					ftpClient.disconnect();
					throw new Exception("login failed:" + ftpClient.getReplyCode());
				}
				ftpClient.enterLocalPassiveMode();
				ftpClient.changeWorkingDirectory(ftpFolder);
				ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
				ftpClient.storeFile(uploadFileName, new FileInputStream(new File(backupFileName)));
				ftpClient.logout();
				ftpClient.disconnect();
				currentStatus = "upload sucseeded " +
					ftpLogin + "@" + ftpServer + ftpFolder + uploadFileName;
			}
			catch (Exception e)
			{
				currentStatus = ftpLogin + "@" + ftpServer + ftpFolder + uploadFileName + " failed with output:" + e.getStackTrace()[0].toString()
					+ "\n" + e.getMessage();

				/*		operationStatus.setText("Operation status: ");
				 for(int i =0; i<e.getStackTrace().length&&i<10;i++) {
				 operationStatus.setText(operationStatus.getText()+"\n"+e.getStackTrace()[i].toString());
				 }*/
			}
		}
	};

	private boolean openFTPConfig(String defaultRootDirectory)
	{
		try
		{
			File configFile = new File(defaultRootDirectory + "/ftpConfig.txt");
			if (!configFile.exists())
			{
				currentStatus = "no ftp config-no upload " + defaultRootDirectory + "/ftpConfig.txt";
				return false;
			}
			BufferedReader br = new BufferedReader(new FileReader(configFile.getAbsolutePath()));
			ftpServer = br.readLine();
			ftpFolder = br.readLine();
			ftpLogin = br.readLine();
			ftpPassword = br.readLine();
			pointNumber = br.readLine();
			br.close();

		}
		catch (Exception e)
		{
			currentStatus = e.getMessage();
			return false;
		}
		return true;
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
		/*Handler handler = new Handler();
		handler.postDelayed(this, 1000);
		*/
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		defaultRootDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + 
			"/DCIM/TTStarter";

		EditText editNextNumber = (EditText) findViewById(R.id.editTextNextNumber);
		editNextNumber.setOnEditorActionListener(
				new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
					{
						boolean handled = false;
						String enteredNum = "xxx";
						if (actionId == EditorInfo.IME_ACTION_GO) 
						{
							enteredNum = ((EditText)v).getText().toString();
							addNumber(enteredNum);
							((EditText)v).setText("");
							handled = true;
						}
						return handled;
					}
				});

    }
	@Override
	public boolean onCreateOptionsMenu(Menu main_menu)
	{
		getMenuInflater().inflate(R.menu.main_menu, main_menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menuReset:
				((EditText)findViewById(R.id.editTextResults)).setText("");
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	public void writeFile(String filePath, String fileName, String value)
	{
		try
		{
			File locationDirs = new File(filePath);
			if (!locationDirs.exists())
			{
				locationDirs.mkdirs();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(filePath + "/" + fileName));
			bw.write(value);
			bw.close();
		}
		catch (Exception e)
		{
			currentStatus = e.getMessage();
		}
	}
}
