package it.unisa.petra.batch;

import it.unisa.petra.core.Process;
import it.unisa.petra.core.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dardin88
 */
public class Terminal {

    public static void run(String configFileLocation) {

        try {
            Process process = new Process();

            ConfigManager configManager = new ConfigManager(configFileLocation);
            CommitManager commitManager = new CommitManager();
            File appDataFolder = new File(configManager.getOutputLocation());

            appDataFolder.delete();
            appDataFolder.mkdirs();

            String powerProfilePath = configManager.getPowerProfileFile();

            if (powerProfilePath.isEmpty()) {
                process.extractPowerProfile(configManager.getOutputLocation());
                powerProfilePath = configManager.getOutputLocation() + "/power_profile.xml";
            }

            String profilingScript = configManager.getProfilingScript();
            File[] apks = new File(configManager.getApkDirectory()).listFiles(File::isDirectory);
            String pathToApp = "";
            assert apks != null;
            for (File apk_folder : apks) {
                try{
                    String commitHash = apk_folder.getName();
                    System.out.println("Executing apk: " + commitHash); //will be commit hash
                    pathToApp = apk_folder.getAbsolutePath();

                    String debugApk,testApk;
                    try{
                        debugApk  = process.searchApk(pathToApp, "build/outputs/apk","debug.apk").get(0);
                    } catch(IndexOutOfBoundsException ex){
                        System.out.println("Couldn't find debug apk");
                        throw new ApkNotFoundException();
                    }
                    try{
                        testApk = process.searchApk(pathToApp, "build/outputs/apk","androidtest.apk").get(0);
                    } catch(IndexOutOfBoundsException ex){
                        System.out.println("Couldn't find test apk");
                        throw new ApkNotFoundException();
                    }

                    String appName = process.extractAppName(debugApk);
                    process.installApp(debugApk);
                    process.installApp(testApk);

                    process.playRun(appName,
                            powerProfilePath, configManager.getOutputLocation(), appName,
                            commitHash, pathToApp, profilingScript);

                    process.uninstallApp(appName);

                } catch(InterruptedException | IOException | NullPointerException | ApkNotFoundException ex){
                    System.out.println(ex.getMessage());
                }
                break;
            }
        }catch (AppNameCannotBeExtractedException | NoDeviceFoundException | IOException | ADBNotFoundException ex) {
            Logger.getLogger(Terminal.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getMessage());
        }
    }
}

