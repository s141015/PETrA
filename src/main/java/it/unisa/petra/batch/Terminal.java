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

        //Must add connection to postgres database, checkout commit and previous commit

        try {
            Process process = new Process();
//            int trials = 0;
//            BufferedWriter seedsWriter = null;

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

            String setupScript = configManager.getSetupScript();
            String profilingScript = configManager.getProfilingScript();
            //File[] apps = new File(configManager.getAppDirectory()).listFiles(File::isDirectory);
            File[] apks = new File(configManager.getApkDirectory()).listFiles(File::isDirectory);
            String pathToApp = "";
            String build = "false";
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

                    //System.out.println("Found Apk: " + file.get(0));
                    String appName = process.extractAppName(debugApk);
                    process.installApp(debugApk);
                    process.installApp(testApk);
//                    String setupCommand = "python3 " + setupScript + " --repo " + pathToApp + " --build " + build;
//                    System.out.println(setupCommand);

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

//                String refactoringCommitsFile = configManager.getRefactoringCommits() + File.separator + app.getName() + ".txt";
//                System.out.println("Refactoring Commits: " + refactoringCommitsFile);
//
//                refactoringCommitsReader = new BufferedReader(new FileReader(refactoringCommitsFile));
//                String line = refactoringCommitsReader.readLine();
//
//                while(line != null){
//                    if (line.equals(introducedAndroidTest)){
//                        startReading = true;
//                    }
//                    if (!startReading){
//                        line = refactoringCommitsReader.readLine();
//                        continue;
//                    }
//                    String[] refactoringCommit_parent = line.split(",");
//
//                    for (String commitHash : refactoringCommit_parent) { //[0] is ref commit, [1] is parent of ref commit
//                        System.out.println("Commit: " + commitHash);
//                        commitManager.resetCommit(app.getAbsoluteFile(),app.getName());
//                        commitManager.checkoutToCommit(commitHash,app.getAbsoluteFile());
//
//                        try {
//
//
//                            String setupCommand = "python3 " + setupScript + " --repo " + pathToApp + " --build " + build;
//                            System.out.println(setupCommand);
//                          /*
//                            FIRST PART OF PYTHON SCRIPT
//                            MODIFY BUILD FILE
//                            GENERATE APK
//                            INSTALL APK
//                          */
//                            process.executeCommand(setupCommand, null);

//                Find apk for the appname --> must contain specific string and ends with .apk
                    //file = process.search(pathToApp, "build/outputs/apk/fdroid/debug");
  //                          }


                //



//                    line = refactoringCommitsReader.readLine();

//                refactoringCommitsReader.close();


