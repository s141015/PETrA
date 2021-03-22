package org.shadowice.flocke.andotp;

import android.os.Bundle;
import android.util.Log;

import androidx.test.internal.runner.tracker.UsageTrackerRegistry;
import androidx.test.runner.AndroidJUnitRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MyRunner extends AndroidJUnitRunner {

    @Override
    public void finish(int resultCode, Bundle results) {
        Log.d("PETRA", "STOP_PROFILING_NOW");
        String[] commands3 = {"sleep", "5"};
        adbComandExe(commands3);
        super.finish(resultCode, results);
    }
    private String adbComandExe(String[] command) {

        StringBuilder suilder = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        InputStream iStream = null;
        InputStreamReader isReader = null;

        try{

            Process proc = processBuilder.start();
            iStream = proc.getInputStream();
            isReader = new InputStreamReader(iStream);
            BufferedReader bufferedReader = new BufferedReader(isReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                suilder.append(line);
                suilder.append("\n");
            }
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try{
                if(iStream != null){
                    iStream.close();
                }
                if(isReader != null){
                    isReader.close();
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        return suilder.toString();
    }
    private void adbComandExe2(String[] command) {

        ProcessBuilder processBuilder = new ProcessBuilder(command);


        try{

            Process proc = processBuilder.start();

        } catch(Exception e){
            e.printStackTrace();
        } finally {

        }
    }
}