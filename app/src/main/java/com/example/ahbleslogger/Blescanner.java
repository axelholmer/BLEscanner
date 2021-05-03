package com.example.ahbleslogger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.service.scanner.ScanFilterUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Blescanner extends Thread  {

    boolean mIsScanningWindow = false;

    int packageCounter = 0;
    String logString = "";
    File absoluteDir;
    String folderPath;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner = null;
    ScanCallback leScanCallback = null;
    Context context;
    List<ScanFilter> scanFilters;
    ScanSettings scanSettings;
    int rssiLimit;
    int currentScanWindow = 0;
    int currentScanperiod = 0;
    TextView mScanlogTextView;
    int timeBetweenScanperiods;
    int numberOfScanperiods;
    int numberOfScanwindows;
    int scanperiodDuration;
    FileOutputStream outputStream = null;
    ScannerCallback scannerCallback;


    public Blescanner(Context context, File absoluteDir, String folderPath, int rssiLimit, TextView mScanlogTextView, int timeBetweenScanperiods, int numberOfScanperiods, int numberOfScanwindows, int scanperiodDuration, ScannerCallback scannerCallback) {
        this.context = context;
        this.absoluteDir = absoluteDir;
        this.folderPath = folderPath;
        this.rssiLimit = rssiLimit;
        this.mScanlogTextView = mScanlogTextView;
        this.timeBetweenScanperiods = timeBetweenScanperiods;
        this.numberOfScanperiods = numberOfScanperiods;
        this.numberOfScanwindows = numberOfScanwindows;
        this.scanperiodDuration = scanperiodDuration;
        this.scannerCallback = scannerCallback;



        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


        //Filter for GAEN Beacons
        BeaconParser beaconParser = new BeaconParser().setBeaconLayout("s:0-1=fd6f,p:-:-59,i:2-17,d:18-21");
        ArrayList<BeaconParser> beaconParsersArray = new ArrayList<>();
        beaconParsersArray.add(beaconParser);

        scanFilters = new ScanFilterUtils().createScanFiltersForBeaconParsers(beaconParsersArray);
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();


    }

    public void run(){
        System.out.println("MyThread running");





        //How many scanWindows.
        for(int counterScanwindows = 0; counterScanwindows < numberOfScanwindows; counterScanwindows++) {
            Log.i("scanning", "Starting Scanwindow: " + counterScanwindows);

            currentScanWindow = counterScanwindows;
            mIsScanningWindow = true;

            packageCounter = 0;


            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) {
                //If it isn't mounted - we can't write into it.
                return;
            }

            long currentMillis = (new Date()).getTime();
            String txtPath = folderPath + File.separator + "Session_" + currentMillis +".txt" ;
            File logFile = new File(context.getExternalFilesDir(null), txtPath);


            try {
                logFile.createNewFile();

            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                outputStream = new FileOutputStream(logFile, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int scanPeriodCounter = 0; scanPeriodCounter < numberOfScanperiods; scanPeriodCounter++) {
                Log.i("scanning", "Starting scanperiod: " + scanPeriodCounter);
                 currentScanperiod = scanPeriodCounter;
                logString = "";


                Thread scanPeriodThread = new Thread(() -> startScanningPeriod());

                scanPeriodThread.start();
                try {
                    scanPeriodThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.i("scanning", "Stopping scanperiod: " + scanPeriodCounter);
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

            Log.i("scanning", "Stoping Scanwindow: " + counterScanwindows);
        }

        //When all scanwindows done
        scannerCallback.doneScanning();

    }

    public void startScanningPeriod(){

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if ((bluetoothLeScanner != null) ) {
            leScanCallback = getLeScanCallback();

            //game loop
           // Thread bluetoothScannerThread = new Thread(() ->  bluetoothLeScanner.startScan( scanFilters, scanSettings, leScanCallback ));


            Thread bluetoothScannerThread = new Thread(() -> {

                bluetoothLeScanner.startScan( scanFilters, scanSettings, leScanCallback );

                //Scan for 4096 ms
                try {
                    Thread.sleep(scanperiodDuration);
                    //   wait(4096);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bluetoothLeScanner.stopScan(leScanCallback);
                bluetoothLeScanner.flushPendingScanResults(leScanCallback);
            });

            bluetoothScannerThread.start();

            try {
                bluetoothScannerThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            outputStream.write(logString.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }



        //wait before starting another scanperiod
        try {
            Thread.sleep(timeBetweenScanperiods);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private ScanCallback getLeScanCallback(){

        ScanCallback leScanCallback =  new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {

                // if(result.getDevice().getAddress().equals("F4:60:E2:C7:92:73") || result.getDevice().getAddress().equals("80:7B:3E:28:53:18")){
                if((result.getRssi() > rssiLimit)) {
                    String UUIDToCompare =   "97F74301D40F42F9AABDC0234AC90FD9";
                    String UUIDToCompare2 = "2F234454CF6D4A0FADF2F4911BA9FFA6";

                    String advHex =  bytesToHex(result.getScanRecord().getBytes());
                    //  Log.i("scanning", "Test: " +  advHex);
                    //   if(advHex.contains(UUIDToCompare2)){
                    String logText =  "ScanWindowCounter:" + currentScanWindow + " ScanPeriodCounter:" + currentScanperiod + " RSSI:" + result.getRssi() + " PacketCounter:" + packageCounter + " ID: " + result.getDevice().getAddress();
                    Log.i("scanning", logText );



                    mScanlogTextView.setText(logText);
                    // String.valueOf(mScanwindowPeriodEditText);
                    long currentMillis = (new Date()).getTime();
                    logString += String.format("%s", currentMillis) + ";" + packageCounter + ";" + currentScanWindow + ";" + currentScanperiod + ";" + result.getRssi() +";" +result.getDevice().getAddress()+'\n';

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

}
