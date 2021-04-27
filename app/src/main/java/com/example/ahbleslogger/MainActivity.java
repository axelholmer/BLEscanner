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
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ToggleButton;

import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.service.scanner.ScanFilterUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


/*
* Author: Axel Holmer
* TODO
* 1.Fixa inputfeld för foldersnamn,
* 2. gör sa att man kann växla mellan timerscann och togglescann,
*  3. gör sa att man kann sätta timerscan tid
* 4. gör sa att man kan sätta pa intevaler och hur manga.
* 5. gör input fled för uuid.
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
    int counter = 0;
    int scanWindowCounter = 0;
    FileOutputStream outputStream = null;


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


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},0);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      //  String path = sessionName + File.separator + mSensorName + "_" + sessionName;

        bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanFilters = new ArrayList<>();



        BeaconParser beaconParser = new BeaconParser().setBeaconLayout("s:0-1=fd6f,p:-:-59,i:2-17,d:18-21");
        ArrayList<BeaconParser> beaconParsersArray = new ArrayList<>();
        beaconParsersArray.add(beaconParser);

      scanFilters =  new ScanFilterUtils().createScanFiltersForBeaconParsers(beaconParsersArray);

        //ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(covidUUI)).build();
       // scanFilters.add(filter);

   //  bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();



        toggle = findViewById(R.id.toggleButton);
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
                /*
            File logFile = null;
                logFile = LoggerHelper.defineLogFilename(this, "", "test", "txt", true);
                //Log.i(TAG, "Creating file: "+logFile.getAbsolutePath());
                //   mOutputStream = new BufferedOutputStream(new FileOutputStream(logFile, mAppend));
*/
                String folderPath = "FolderTest";
                if(scanWindowCounter == 0){
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
        });

    }



    public void startScanningPeriod(){

        Log.i("scanning", "Starting scanperiod: " + scanPeriodCounter);

        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if ((bluetoothLeScanner != null) ) {
            leScanCallback = getLeScanCallback();
            bluetoothLeScanner.startScan( scanFilters, scanSettings, leScanCallback );
        }

        if(timerScanPeriod != null){
            timerScanPeriod.cancel();
        }

        timerScanPeriod = new CountDownTimer(4096, 1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    if (bluetoothLeScanner != null) {
                        bluetoothLeScanner.stopScan(leScanCallback);
                        bluetoothLeScanner.flushPendingScanResults(leScanCallback);

                        if(mIsScanningWindow){
                            scanPeriodCounter++;
                            startScanningPeriod();
                        }

                    }
                }
            };
        timerScanPeriod.start();
        }

    public void startScanWindow(){
        mIsScanningWindow = true;
        scanPeriodCounter = 0;
        startScanningPeriod();
        counter = 0;

           //Starting BLEScann

            toggle.setEnabled(false);

            if(timerScanWindow != null){
                timerScanWindow.cancel();
            }

        timerScanWindow = new CountDownTimer(24000, 1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    scanWindowCounter++;
                    Log.i("scanning", "timerStop");
                    toggle.setChecked(false);
                    toggle.setEnabled(true);
                }
            }.start();


    }

    private ScanCallback getLeScanCallback(){

        ScanCallback leScanCallback =  new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {

                // if(result.getDevice().getAddress().equals("F4:60:E2:C7:92:73") || result.getDevice().getAddress().equals("80:7B:3E:28:53:18")){
                     if(mIsScanningWindow) {
                         String UUIDToCompare =   "97F74301D40F42F9AABDC0234AC90FD9";
                         String UUIDToCompare2 = "2F234454CF6D4A0FADF2F4911BA9FFA6";

                        String advHex =  bytesToHex(result.getScanRecord().getBytes());
                      //  Log.i("scanning", "Test: " +  advHex);
                      //   if(advHex.contains(UUIDToCompare2)){
                             Log.i("scanning", "ScanWindowCounter: " + scanWindowCounter + " ScanPeriodCounter: " + scanPeriodCounter + " RSSI: " + result.getRssi() + " PacketCounter: " + counter);

                             long currentMillis = (new Date()).getTime();
                             String log =  String.format("%s", currentMillis) + ";" + result.getRssi();
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
                             counter++;
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
           outputStream.flush();

       } catch (IOException e) {
           e.printStackTrace();
       }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(scanWindowCounter <3){
        Log.i("scanning", "starting ScanInterval: "+ scanWindowCounter);
            Handler handler = new Handler();
            handler.postDelayed(() -> toggle.setChecked(true), 2000);

        }else {
            Log.i("scanning", "Done with scanning");
            scanWindowCounter = 0;
        }
        // mTimerHandler.removeCallbacks(mTimerRunnable);
       // vibrator.vibrate(4000);

    //    logger.stop();
    }


}