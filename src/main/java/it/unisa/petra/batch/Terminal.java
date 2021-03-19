package it.unisa.petra.batch;

import it.unisa.petra.core.Process;
import it.unisa.petra.core.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dardin88
 */
public class Terminal {

    public static void run(String configFileLocation) {

        //Must add connection to postgres database, checkout commit and previous commit

        try {
            Process process = new Process();
//            int trials = 0;
//            BufferedWriter seedsWriter = null;

            ConfigManager configManager = new ConfigManager(configFileLocation);

            File appDataFolder = new File(configManager.getOutputLocation());

            appDataFolder.delete();
            appDataFolder.mkdirs();



//            if (configManager.getScriptLocationPath().isEmpty()) {
//                File seedsFile = new File(configManager.getOutputLocation() + File.separator + "seeds");
//                seedsWriter = new BufferedWriter(new FileWriter(seedsFile, true));
//            }


//            File apkFile = new File(configManager.getApkLocationPath());
//            if (apkFile.exists()) {
//                process.installApp(configManager.getApkLocationPath());
//            } else {
//                throw new ApkNotFoundException();
//            }

            String powerProfilePath = configManager.getPowerProfileFile();

            if (powerProfilePath.isEmpty()) {
                process.extractPowerProfile(configManager.getOutputLocation());
                powerProfilePath = configManager.getOutputLocation() + "/power_profile.xml";
            }

//            int timeCapturing = (configManager.getInteractions() * configManager.getTimeBetweenInteractions()) / 1000;
//
//            if (timeCapturing <= 0) {
//                timeCapturing = 100;
//            }
//
//            if (!configManager.getScriptLocationPath().isEmpty()) {
//                timeCapturing = Integer.parseInt(configManager.getScriptTime());
//            }
            String build;
            for (int run = 1; run <= 1; run++) {
                System.out.println("Run: " + run);
                try {
                    if(run == 0){
                        build = "true";
                    }
                    else{
                        build = "false";
                    }
////                    if (trials == configManager.getTrials()) {
////                        throw new NumberOfTrialsExceededException();
////                    }

                    String test_app = "/Users/posl/Desktop/temp-petra-test/testing_apps/andOTP";
                    String python_script = "/Users/posl/PycharmProjects/petra_python_part/app_setup1.py";
                    String setupCommand = "python3 " + python_script + " --repo " + test_app + " --build " + build;
                    System.out.println(setupCommand);
                  /*
                    FIRST PART OF PYTHON SCRIPT
                    MODIFY BUILD FILE
                    GENERATE APK
                    INSTALL APK
                  */
                    process.executeCommand(setupCommand,null);

                    //Find apk for the appname --> must contain specific string and ends with .apk
                    //List<String> file = process.search(test_app,"build/outputs/apk/debug");
                    List<String> file = process.search(test_app,"build/outputs/apk/fdroid/debug");
                    System.out.println("Found File: " + file.get(0));
                    String appName = process.extractAppName(file.get(0));

                    //Add loop to go through all apps and commits from postgres data
                    process.playRun(appName,
                            powerProfilePath, configManager.getOutputLocation(), appName,
                            "test_commit_hash", test_app, run);

//
                } catch (InterruptedException | IOException ex) {
                    System.out.println(ex.getMessage());
                }
           }
//            process.uninstallApp(appName);
        } catch (AppNameCannotBeExtractedException | NoDeviceFoundException | IOException | ADBNotFoundException ex) {
            Logger.getLogger(Terminal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
