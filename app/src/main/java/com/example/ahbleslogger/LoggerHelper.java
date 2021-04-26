package com.example.ahbleslogger;


import android.content.Context;
import android.os.SystemClock;

import java.io.File;

public final class LoggerHelper {


    public static File defineLogFilename(Context context, String path, String baseFilename, String extension, boolean toAppend){

        // Get the absolute path to the directory on the primary shared/external storage device
        // where the application can place persistent files.
        File absoluteDir = new File(context.getExternalFilesDir(null), path);
        if (!absoluteDir.exists()){
            absoluteDir.mkdirs();
        }

        String nanoTimeString = Long.toString(SystemClock.elapsedRealtimeNanos());
        File[] files = absoluteDir.listFiles();
        if (files.length == 0) { // In case there are no files in the dir
            // The first logging file is indexed as 0
            baseFilename = baseFilename + "__" + nanoTimeString + "__0" + "." + extension;
        } else {
            int lastIndex = -1;
            for (File file : files){
                if (file.isFile()){
                    String[] items = file.getName().split("\\.")[0].split("__");
                    // There must be two "__" tokens in the file name: sessionName__nanoTime__index
                    // items[0]=sessionName, items[1]=nanoTime. items[2]=index
                    int index = Integer.parseInt(items[2]);
                    // The index of the last file is obtained
                    if ((index > lastIndex) && (items[0].equals(baseFilename))){
                        lastIndex = index;
                        if (toAppend){
                            nanoTimeString = items[1];
                        }
                    }
                }
            }
            if (toAppend){
                baseFilename = baseFilename + "__" + nanoTimeString + "__" + lastIndex + "." + extension;
            } else {
                baseFilename = baseFilename + "__" + nanoTimeString + "__" + (lastIndex+1) + "." + extension;
            }
        }

        return new File(absoluteDir.getAbsolutePath() + File.separator + baseFilename);
    }}
