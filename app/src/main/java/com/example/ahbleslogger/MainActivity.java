package com.example.ahbleslogger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.service.scanner.ScanFilterUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/*
*   Author: Axel Holmer
* TODO
*
*
*
*
*   testa med tva telefoner med cwa vad för tak man kan ha tex.
*   Kolla uuid
*
*
*
*
*
* */

public class MainActivity extends AppCompatActivity {

    private boolean mIsScanningWindow = false;
    BluetoothManager bluetoothManager = null;
    BluetoothAdapter bluetoothAdapter = null;
    BluetoothLeScanner bluetoothLeScanner = null;
    List<ScanFilter> scanFilters = null;
    ToggleButton toggle = null;
    File logFile = null;
    CountDownTimer timerScanPeriod = null;
    CountDownTimer timerScanWindow = null;
    ScanCallback leScanCallback = null;
    int scanPeriodCounter = 0;
    int packageCounter = 0;
    int scanWindowCounter = 0;
    FileOutputStream outputStream = null;
    MediaPlayer mediaPlayer = null;
    Vibrator vibrator = null;

    Handler delayBetweenScanwindowHandler = null;
    Handler delayBetweenscanperiodHandler = null;
    TextView mScanlogTextView = null;
    EditText mFolderEditText = null;
    EditText mScanwindowCounterEditText = null;
    EditText mScanwindowDurationEditText = null;
    EditText mScanperiodDurationEditText = null;
    EditText mRssiLimitEditText = null;
    Button mStartScanButton;
    Button mStoptScanButton;

    Boolean isStoppedByUsed = false;


    int rssiLimit = 0;
    String logString = "";

    ScanSettings scanSettings = null;

    @Override
    protected  void onDestroy() {
        super.onDestroy();
        timerScanPeriod.cancel();
        timerScanWindow.cancel();
        stopScanWindow();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        Log.i("scanning", "init");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},0);


        rssiLimit = -105;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      //  String path = sessionName + File.separator + mSensorName + "_" + sessionName;

        delayBetweenScanwindowHandler = new Handler();
        delayBetweenscanperiodHandler = new Handler();

        bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);

       // bluetoothAdapter = bluetoothManager.getAdapter();


        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();

        scanFilters = new ArrayList<>();

        Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        mediaPlayer = new MediaPlayer();

        vibrator = ((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE));

        //uiElements
        mScanwindowDurationEditText = findViewById(R.id.scanwindowDurationEditText);
        mScanwindowCounterEditText = findViewById(R.id.scanwindowCounterEditText);
        mFolderEditText = findViewById(R.id.FolderEditText);
        mScanlogTextView = findViewById(R.id.scanlogTextView);
        mScanperiodDurationEditText = findViewById(R.id.scanperiodDurationEditText);
        mRssiLimitEditText = findViewById(R.id.rssiLimitEditText);
       // toggle = findViewById(R.id.toggleButton);
        mStartScanButton = findViewById(R.id.startScanButton);
       // mStoptScanButton = findViewById(R.id.stopScanButton);


        mStartScanButton.setOnClickListener(view -> start());

     //  mStoptScanButton.setOnClickListener(view -> stop());


        try {
            mediaPlayer.setDataSource(this, defaultRingtoneUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> mp.release());

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Filter for GAEN Beacons
        BeaconParser beaconParser = new BeaconParser().setBeaconLayout("s:0-1=fd6f,p:-:-59,i:2-17,d:18-21");
        ArrayList<BeaconParser> beaconParsersArray = new ArrayList<>();
        beaconParsersArray.add(beaconParser);

        scanFilters =  new ScanFilterUtils().createScanFiltersForBeaconParsers(beaconParsersArray);

        //ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(covidUUI)).build();
       // scanFilters.add(filter);
   //  bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

/*
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                long currentMillis = (new Date()).getTime();
           //   String path = sessionName + File.separator + mSensorName + "_" + sessionName;
                //Checking the availability state of the External Storage.
                String state = Environment.getExternalStorageState();
                if (!Environment.MEDIA_MOUNTED.equals(state)) {
                    //If it isn't mounted - we can't write into it.
                    return;
                }

                String folderPath = String.valueOf(mFolderEditText.getText());
                if(scanWindowCounter == 0){
                    mediaPlayer.stop();
                    File absoluteDir = new File(getExternalFilesDir(null), folderPath);
                    if (!absoluteDir.exists()){
                        absoluteDir.mkdirs();
                    }
                }
                String txtPath = folderPath + File.separator + "Session_" + currentMillis +".txt" ;
                logFile = new File(getExternalFilesDir(null), txtPath);

                try {
                    logFile.createNewFile();
                    outputStream = new FileOutputStream(logFile, true);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                //    logger = new CustomLogger(context, path, sessionName, mSensorName, "txt", false, mNanosOffset, logFileMaxSize);
                Log.i("scanning", "Starting Scanwindow: " + scanWindowCounter);
                startScanWindow();

            } else {
                Log.i("scanning", "Stoppin Scanwindow: " + scanWindowCounter);

                stopScanWindow();
            }
        }); */

    }


    public void setInputsDisabled(Boolean isEnabled){
        isStoppedByUsed = isEnabled;
        mScanwindowDurationEditText.setEnabled(isEnabled);
        mScanwindowCounterEditText.setEnabled(isEnabled);
        mFolderEditText.setEnabled(isEnabled);
        mScanperiodDurationEditText.setEnabled(isEnabled);
        mStartScanButton.setEnabled(isEnabled);
    }


    public void start(){
        setInputsDisabled(false);



        timerScanPeriod = new CountDownTimer(Integer.parseInt(String.valueOf(mScanperiodDurationEditText.getText())), 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                if (bluetoothLeScanner != null) {
                    bluetoothLeScanner.stopScan(leScanCallback);
                    bluetoothLeScanner.flushPendingScanResults(leScanCallback);

                    if(mIsScanningWindow){

                        //Handler here
                        /*
                        delayBetweenscanperiodHandler.postDelayed(() -> {
                            scanPeriodCounter++;
                            startScanningPeriod();}, 2000);
                            */

                        scanPeriodCounter++;
                        startScanningPeriod();
                    }

                }
            }
        };

        timerScanWindow = new CountDownTimer(Integer.parseInt(String.valueOf(mScanwindowDurationEditText.getText())), 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                scanWindowCounter++;
                Log.i("scanning", "timerStop");

                setScanning(false);
                //toggle.setChecked(false);
               // toggle.setEnabled(true);
            }
        };

        setScanning(true);
    }

    public void stop(){

        isStoppedByUsed = true;

        setInputsDisabled(true);

        setScanning(false);
    }

    public void setScanning(Boolean startScan){
        if (startScan) {
            long currentMillis = (new Date()).getTime();
            //   String path = sessionName + File.separator + mSensorName + "_" + sessionName;
            //Checking the availability state of the External Storage.
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) {
                //If it isn't mounted - we can't write into it.
                return;
            }
                /*
            File logFile = null;
                logFile = LoggerHelper.defineLogFilename(this, "", "test", "txt", true);
                //Log.i(TAG, "Creating file: "+logFile.getAbsolutePath());
                //   mOutputStream = new BufferedOutputStream(new FileOutputStream(logFile, mAppend));
*/
            rssiLimit = Integer.valueOf(String.valueOf(mRssiLimitEditText.getText()));

            //String folderPath = "FolderTest";
            String folderPath = String.valueOf(mFolderEditText.getText());
            if(scanWindowCounter == 0){
                mediaPlayer.stop();
                File absoluteDir = new File(getExternalFilesDir(null), folderPath);
                if (!absoluteDir.exists()){
                    absoluteDir.mkdirs();
                }
            }
            String txtPath = folderPath + File.separator + "Session_" + currentMillis +".txt" ;
            logFile = new File(getExternalFilesDir(null), txtPath);

            try {
                logFile.createNewFile();
                outputStream = new FileOutputStream(logFile, true);

                 /*
                    //second argument of FileOutputStream constructor indicates whether
                    //to append or create new file if one exists
                    outputStream = new FileOutputStream(logFile, true);

                    outputStream.write(textToWrite.getBytes());
                    outputStream.flush();
                    outputStream.close();
                  */
            } catch (Exception e) {
                e.printStackTrace();
            }

            //    logger = new CustomLogger(context, path, sessionName, mSensorName, "txt", false, mNanosOffset, logFileMaxSize);
            Log.i("scanning", "Starting Scanwindow: " + scanWindowCounter);
            startScanWindow();

        } else {
            Log.i("scanning", "Stoppin Scanwindow: " + scanWindowCounter);

            stopScanWindow();
        }
    }

    public void startScanningPeriod(){

        Log.i("scanning", "Starting scanperiod: " + scanPeriodCounter);

        bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if ((bluetoothLeScanner != null) ) {
            leScanCallback = getLeScanCallback();
            bluetoothLeScanner.startScan( scanFilters, scanSettings, leScanCallback );
        }

        timerScanPeriod.start();
        }

    public void startScanWindow(){
        mIsScanningWindow = true;
        scanPeriodCounter = 0;
        startScanningPeriod();
        packageCounter = 0;
        logString = "";

           //Starting BLEScann


        timerScanWindow.start();
    }

    private ScanCallback getLeScanCallback(){

        ScanCallback leScanCallback =  new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {

                // if(result.getDevice().getAddress().equals("F4:60:E2:C7:92:73") || result.getDevice().getAddress().equals("80:7B:3E:28:53:18")){
                     if(mIsScanningWindow && (result.getRssi() > rssiLimit)) {
                         String UUIDToCompare =   "97F74301D40F42F9AABDC0234AC90FD9";
                         String UUIDToCompare2 = "2F234454CF6D4A0FADF2F4911BA9FFA6";

                        String advHex =  bytesToHex(result.getScanRecord().getBytes());
                      //  Log.i("scanning", "Test: " +  advHex);
                      //   if(advHex.contains(UUIDToCompare2)){
                         String logText =  "ScanWindowCounter:" + scanWindowCounter + " ScanPeriodCounter:" + scanPeriodCounter + " RSSI:" + result.getRssi() + " PacketCounter:" + packageCounter + " ID: " + result.getDevice().getAddress();
                         Log.i("scanning", logText );



                             mScanlogTextView.setText(logText);
                         // String.valueOf(mScanwindowPeriodEditText);
                             long currentMillis = (new Date()).getTime();
                             logString += String.format("%s", currentMillis) + ";" + packageCounter + ";" + scanWindowCounter + ";" + scanPeriodCounter + ";" + result.getRssi() +";" +result.getDevice().getAddress()+'\n';

                            /*
                             String log =  String.format("%s", currentMillis) + ";" + packageCounter + ";" + scanWindowCounter + ";" + scanPeriodCounter + ";" + result.getRssi();
                             try {
                                 outputStream.write(log.getBytes());
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                             try{
                                 outputStream.write(System.lineSeparator().getBytes());
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }

                             */
                             packageCounter++;
                      // }
                    }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };
        return leScanCallback;
    }

    public static String bytesToHex(byte[] bytes) {

       // Log.i("scanning", "ented");
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void stopScanWindow()  {
        mIsScanningWindow = false;


        try {
            outputStream.write(logString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }


       try {
           outputStream.flush();

       } catch (IOException e) {
           e.printStackTrace();
       }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if( (scanWindowCounter < Integer.parseInt(String.valueOf(mScanwindowCounterEditText.getText()))) && !isStoppedByUsed){
            delayBetweenScanwindowHandler.postDelayed(() ->
                    setScanning(true), 200);
                    //toggle.setChecked(true), 200);
        }else {
           // mediaPlayer.start();
             vibrator.vibrate(10000);
            Log.i("scanning", "Done with scanning");
            mScanlogTextView.setText("Done with scanning");
            scanWindowCounter = 0;

            setInputsDisabled(true);

        }
        // mTimerHandler.removeCallbacks(mTimerRunnable);


    }


}