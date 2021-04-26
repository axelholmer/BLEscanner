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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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


    private boolean mIsScanning = false;

    BluetoothManager bluetoothManager = null;
    BluetoothAdapter bluetoothAdapter = null;
    BluetoothLeScanner bluetoothLeScanner = null;
    ToggleButton toggle = null;
    File filesDir = null;
    File logFile = null;

    ScanCallback leScanCallback = null;

    int counter = 0;

    int counterScanIntervals = 0;
    FileOutputStream outputStream = null;

    @Override
    protected  void onDestroy() {
        super.onDestroy();
        stopScan();
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
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
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

                if(counterScanIntervals == 0){
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

                startScan();
                Log.i("scanning", "startScan");
            } else {
                Log.i("scanning", "stopScan");

                stopScan();
            }
        });


    }



    public void startScan(){

        counter = 0;

        /*

        String serviceUuidString = "97f74301-d40f-42f9-aabd-c0234ac90fd9"; //uuid i wanna scan
        String serviceUUIDString2 = "97F74301-D40F-42F9-AABD-C0234AC90FD9";
        UUID toUiid = UUID.fromString(serviceUUIDString2);

            ScanFilter.Builder mBuilder = new ScanFilter.Builder();
            ByteBuffer mManufacturerData = ByteBuffer.allocate(23);
            ByteBuffer mManufacturerDataMask = ByteBuffer.allocate(24);
            byte[] uuid = getIdAsByte(toUiid);
            mManufacturerData.put(0, (byte)0xBE);
            mManufacturerData.put(1, (byte)0xAC);
            for (int i=2; i<=17; i++) {
                mManufacturerData.put(i, uuid[i-2]);
            }
            for (int i=0; i<=17; i++) {
                mManufacturerDataMask.put((byte)0x01);
            }
            mBuilder.setManufacturerData(224, mManufacturerData.array(), mManufacturerDataMask.array());
        ScanFilter  mScanFilter = mBuilder.build();

*/

        //ScanFilter filter = new ScanFilter.Builder().setServiceUuid(parcelUuid, parcelUuidMask).build();
       // ScanFilter scanFilterMac = new ScanFilter.Builder().setDeviceAddress("F4:60:E2:C7:92:73").build();;
        //scanFilters.add(mScanFilter);
        leScanCallback = getLeScanCallback();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        List<ScanFilter> scanFilters = new ArrayList<>();

        if ((bluetoothLeScanner != null) && (!mIsScanning)) {
            Log.i("scanning", "Starting to Scan for BLEADV");
            // mBTAdapter.startLeScan(this);
            bluetoothLeScanner.startScan( scanFilters, scanSettings, leScanCallback );
            mIsScanning = true;

            toggle.setEnabled(false);
            new CountDownTimer(5000, 1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    counterScanIntervals++;
                    Log.i("scanning", "timerStop");
                    toggle.setChecked(false);
                    toggle.setEnabled(true);
                }
            }.start();


        }

    }


    private ScanCallback getLeScanCallback(){

        ScanCallback leScanCallback =  new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {


                // if(result.getDevice().getAddress().equals("F4:60:E2:C7:92:73") || result.getDevice().getAddress().equals("80:7B:3E:28:53:18")){
                     if(mIsScanning) {

                         String UUIDToCompare =   "97F74301D40F42F9AABDC0234AC90FD9";
                        String advHex =  bytesToHex(result.getScanRecord().getBytes());



                      //  Log.i("scanning", "Test: " +  advHex);

                         if(advHex.contains(UUIDToCompare)){
                             Log.i("scanning", "Test: " + result.getRssi());

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
                       }
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

    public void stopScan()  {
        Log.i("scanning","stop:: Stopping scanning for BLEADV");


        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(leScanCallback);
            bluetoothLeScanner.flushPendingScanResults(leScanCallback);
        }
        mIsScanning = false;

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


        if(counterScanIntervals<3){
        Log.i("scanning", "starting ScanInterval: "+  counterScanIntervals);
            Handler handler = new Handler();
            handler.postDelayed(() -> toggle.setChecked(true), 2000);



        }else {
            Log.i("scanning", "Done with scanning");
            counterScanIntervals = 0;
        }

        // mTimerHandler.removeCallbacks(mTimerRunnable);

       // vibrator.vibrate(4000);



    //    logger.stop();
    }


}