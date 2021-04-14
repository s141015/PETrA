package it.unisa.petra.core;

import it.unisa.petra.core.batterystats.BatteryStatsParser;
import it.unisa.petra.core.batterystats.EnergyInfo;
import it.unisa.petra.core.exceptions.ADBNotFoundException;
import it.unisa.petra.core.exceptions.AppNameCannotBeExtractedException;
import it.unisa.petra.core.exceptions.NoDeviceFoundException;
import it.unisa.petra.core.powerprofile.PowerProfile;
import it.unisa.petra.core.powerprofile.PowerProfileParser;
import it.unisa.petra.core.systrace.CpuFrequency;
import it.unisa.petra.core.systrace.SysTrace;
import it.unisa.petra.core.systrace.SysTraceParser;
import it.unisa.petra.core.traceview.TraceLine;
import it.unisa.petra.core.traceview.TraceViewParser;
import it.unisa.petra.core.traceview.TraceviewStructure;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;
import java.util.stream.*;

public class Process {

    public void installApp(String apkLocation) throws NoDeviceFoundException, ADBNotFoundException {

        this.checkADBExists();

        this.executeCommand("adb shell dumpsys battery set ac 0", null);
        this.executeCommand("adb shell dumpsys battery set usb 0", null);

        System.out.println("Installing app.");
        this.executeCommand("adb install -r " + apkLocation, null);
    }

    public void uninstallApp(String appName) throws NoDeviceFoundException, ADBNotFoundException {

        this.checkADBExists();

        System.out.println("Uninstalling app.");
        this.executeCommand("adb shell pm uninstall " + appName, null);
    }


    public void playRun(String appName, String powerProfileFile, String outputLocation, String filter,
                        String commitHash, String repoPath, String profilingScript)
            throws InterruptedException, IOException, NoDeviceFoundException, ADBNotFoundException {

        String sdkFolderPath = System.getenv("ANDROID_HOME");
        this.checkADBExists();

//        String platformToolsFolder = sdkFolderPath + File.separator + "platform-tools";
//        String toolsFolder = sdkFolderPath + File.separator + "tools";

//        Random random = new Random();
//        int seed = random.nextInt();

//        if (scriptLocationPath.isEmpty()) {
//            System.out.println("seed: " + seed);
//        }
        String[] appFolder = repoPath.split("/");
        String folderName = appFolder[appFolder.length-1];
        String outputDirectory = outputLocation + File.separator + folderName + File.separator +
                commitHash + File.separator;
        File commit_folder = new File(outputDirectory);

        commit_folder.mkdirs();

        String batteryStatsFilename = outputDirectory + "batterystats";
        String systraceFilename = outputDirectory + "systrace";
        String traceviewFilename = outputDirectory + "tracedump";

        //Generate trace and tracedump
        this.runProcess(traceviewFilename, systraceFilename, batteryStatsFilename, repoPath, outputDirectory, profilingScript);
//        this.resetApp(appName);
//        this.startApp(appName);

//        Date time1 = new Date();
//        SysTraceRunner sysTraceRunner = this.startProfiling(appName, timeCapturing, systraceFilename, platformToolsFolder);
//        Thread systraceThread = new Thread(sysTraceRunner);
//        systraceThread.start();

//        this.executeActions(appName, scriptLocationPath, toolsFolder, interactions, timeBetweenInteractions, seed);

//        Date time2 = new Date();
//        long timespent = time2.getTime() - time1.getTime();
//
//        timeCapturing = (int) ((timespent + 10000) / 1000);

//        this.extractInfo(appName, batteryStatsFilename, outputDirectory, platformToolsFolder, traceviewFilename);

//        systraceThread.join();

        System.out.println("aggregating results.");

        System.out.println("parsing power profile.");
        PowerProfile powerProfile = PowerProfileParser.parseFile(powerProfileFile);

        List<TraceLine> traceLinesWiConsumptions = parseAndAggregateResults(traceviewFilename, batteryStatsFilename,
                systraceFilename, powerProfile, filter);

        PrintWriter resultsWriter = new PrintWriter(outputDirectory + "result.csv", "UTF-8");
        resultsWriter.println("signature, joule, seconds");

        for (TraceLine traceLine : traceLinesWiConsumptions) {
            resultsWriter.println(traceLine.getSignature() + "," + traceLine.getConsumption() + "," + traceLine.getTimeLength());
        }

        resultsWriter.flush();
        //this.stopApp(appName, run);

        this.executeCommand("adb shell dumpsys battery reset", null);

        System.out.println("Complete.");
//        return new ProcessOutput(,seed);
    }

    public void extractPowerProfile(String outputLocation) throws NoDeviceFoundException {
        System.out.println("Extracting power profile.");
        String jarDirectory = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath();

        this.executeCommand("adb pull /system/framework/framework-res.apk", null);

        this.executeCommand("jar xf " + jarDirectory + "/PETrA.jar apktool_2.2.2.jar", null);
        this.executeCommand("java -jar apktool_2.2.2.jar if framework-res.apk", null);
        this.executeCommand("java -jar apktool_2.2.2.jar d framework-res.apk", null);
        this.executeCommand("mv " + jarDirectory + "/framework-res/res/xml/power_profile.xml " + outputLocation, null);
        this.executeCommand("rm -rf " + jarDirectory + "/apktool_2.2.2.jar", null);
        this.executeCommand("rm -rf " + jarDirectory + "/framework-res.apk", null);
        this.executeCommand("rm -rf " + jarDirectory + "/framework-res", null);
    }

    private void resetApp(String appName) throws NoDeviceFoundException {
        System.out.println("Resetting app and batteristats.");
        this.executeCommand("adb shell pm clear " + appName, null);
        this.executeCommand("adb shell dumpsys batterystats --reset", null);
    }

//    private SysTraceRunner startProfiling(String appName, int timeCapturing, String systraceFilename,
//                                          String platformToolsFolder) throws NoDeviceFoundException {
//        System.out.println("Run " + run + ": starting profiling.");
//        this.executeCommand("adb shell am profile start " + appName + " ./data/local/tmp/log.trace", null);
//
//        System.out.println("Run " + run + ": capturing system traces.");
//        return new SysTraceRunner(timeCapturing, systraceFilename, platformToolsFolder);
//    }

    private void executeActions(String appName, int run, String scriptLocationPath, String toolsFolder, int interactions,
                                int timeBetweenInteractions, int seed) throws NoDeviceFoundException {
        if (scriptLocationPath.isEmpty()) {
            System.out.println("Run " + run + ": executing random actions.");
            this.executeCommand("adb shell monkey -p " + appName + " -s " + seed + " --throttle " + timeBetweenInteractions + " --ignore-crashes --ignore-timeouts --ignore-security-exceptions " + interactions, null);
        } else {
            System.out.println("Run " + run + ": running monkeyrunner script.");
            String jarDirectory = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath();
            this.executeCommand("jar xf " + jarDirectory + "/PETrA.jar monkey_playback.py", null);
            this.executeCommand(toolsFolder + "/bin/monkeyrunner " + jarDirectory + "/monkey_playback.py " + scriptLocationPath, null);
            this.executeCommand("rm -rf " + jarDirectory + "/monkey_playback.py", null);
        }
    }

    private void extractInfo(String appName, int run, String batteryStatsFilename, String runDataFolderName, String platformToolsFolder, String traceviewFilename) throws NoDeviceFoundException {
        System.out.println("Run " + run + ": stop profiling.");
        this.executeCommand("adb shell am profile stop " + appName, null);

        System.out.println("Run " + run + ": saving battery stats.");
        this.executeCommand("adb shell dumpsys batterystats", new File(batteryStatsFilename));

        System.out.println("Run " + run + ": saving traceviews.");
        this.executeCommand("adb pull ./data/local/tmp/log.trace " + runDataFolderName, null);
        this.executeCommand(platformToolsFolder + "/dmtracedump -o " + runDataFolderName + "log.trace", new File(traceviewFilename));

    }

    List<TraceLine> parseAndAggregateResults(String traceviewFilename, String batteryStatsFilename, String systraceFilename,
                                             PowerProfile powerProfile, String filter) throws IOException {
        List<TraceLine> traceLinesWConsumption = new ArrayList<>();

        System.out.println("Elaborating traceview info.");
        TraceviewStructure traceviewStructure = TraceViewParser.parseFile(traceviewFilename, filter);
        List<TraceLine> traceLines = traceviewStructure.getTraceLines();
        int traceviewLength = traceviewStructure.getEndTime();
        int traceviewStart = traceviewStructure.getStartTime();

        System.out.println("Elaborating battery stats info.");
        List<EnergyInfo> energyInfoArray = BatteryStatsParser.parseFile(batteryStatsFilename, traceviewStart);

        System.out.println("Elaborating systrace stats info.");
        SysTrace cpuInfo = SysTraceParser.parseFile(systraceFilename, traceviewStart, traceviewLength);

        System.out.println("Aggregating results.");
        energyInfoArray = this.mergeEnergyInfo(energyInfoArray, cpuInfo, cpuInfo.getNumberOfCpu());
        for (TraceLine traceLine : traceLines) {
            traceLinesWConsumption.add(this.calculateConsumption(traceLine, energyInfoArray, powerProfile));
        }

        return traceLinesWConsumption;
    }

    private List<EnergyInfo> mergeEnergyInfo(List<EnergyInfo> energyInfoArray, SysTrace cpuInfo, int numOfCore) {

        List<Integer> cpuFrequencies = new ArrayList<>();

        List<EnergyInfo> finalEnergyInfoArray = new ArrayList<>();

        for (int i = 0; i < numOfCore; i++) {
            cpuFrequencies.add(0);
        }

        for (EnergyInfo energyInfo : energyInfoArray) {
            int fixedEnergyInfoTime = cpuInfo.getSystraceStartTime() + energyInfo.getEntrance();
            for (CpuFrequency frequency : cpuInfo.getFrequencies()) {
                if (frequency.getTime() < fixedEnergyInfoTime) {
                    EnergyInfo finalEnergyInfo = new EnergyInfo(energyInfo);

                    cpuFrequencies.set(frequency.getCore(), frequency.getValue());

                    int finalEnergyInfoTime = frequency.getTime() - cpuInfo.getSystraceStartTime();
                    finalEnergyInfo.setEntrance(finalEnergyInfoTime);
                    finalEnergyInfo.setCpuFrequencies(cpuFrequencies);
                    finalEnergyInfoArray.add(finalEnergyInfo);
                } else {
                    break;
                }
            }
        }
        return finalEnergyInfoArray;
    }

    private TraceLine calculateConsumption(TraceLine traceLine, List<EnergyInfo> energyInfoArray, PowerProfile powerProfile) {

        double joule = 0;
        double totalSeconds = 0;

        int numberOfCores = energyInfoArray.get(0).getCpuFrequencies().size();

        boolean[] previouslyIdle = new boolean[numberOfCores];

        for (EnergyInfo energyInfo : energyInfoArray) {
            if (traceLine.getEntrance() >= energyInfo.getEntrance()) {

                double ampere = 0;

                List<Integer> cpuFrequencies = energyInfo.getCpuFrequencies();

                for (int i = 0; i < numberOfCores; i++) {
                    int coreFrequency = cpuFrequencies.get(i);
                    int coreCluster = powerProfile.getClusterByCore(i);
                    ampere += powerProfile.getCpuConsumptionByFrequency(coreCluster, coreFrequency) / 1000;
                    if (coreFrequency != 0) {
                        if (previouslyIdle[i]) {
                            /*
                            Our power profile is different. From the description on the official source, this cpu.awake
                            field is now called cpu.idle and the old cpu.idle is now cpu.suspend in the power profile
                            https://source.android.com/devices/tech/power/values
                            * */
                            ampere += powerProfile.getDevices().get("cpu.idle") / 1000; // cpu.awake
                        }
                    } else {
                        previouslyIdle[i] = true;
                    }
                }

                /*
                States we can probably ignore because shared between all apps:
                running: STATE_CPU_RUNNING_FLAG
                wake_lock: STATE_WAKE_LOCK_FLAG
                usb_data: STATE2_USB_DATA_LINK_FLAG
                ble_scan: STATE2_BLUETOOTH_SCAN_FLAG

                should we take rx or tx for wifi_radio?
                Need to find value for wifi: new BitDescription(HistoryItem.STATE2_WIFI_ON_FLAG, "wifi", "W"), according to desc it means wifi is on

                for (String deviceString : energyInfo.getDevices()) {
                    if (deviceString.contains("wifi")) {
                        ampere += powerProfile.getDevices().get("wifi.controller.idle") / 1000;
                    } else if (deviceString.contains("wifi.radio")) {
                        ampere += powerProfile.getDevices().get("wifi.controller.voltage") / 1000;
                    }   else if (deviceString.contains("screen")) {
                        ampere += powerProfile.getDevices().get("screen.on") / 1000;
                    }   else if (deviceString.contains("gps")) {
                        ampere += powerProfile.getDevices().get("gps.voltage") / 1000;
                    } else if (deviceString.contains("camera")) {
                        ampere += powerProfile.getDevices().get("camera.avg") / 1000;
                    }//Old petra: wifi.scanning, phone.scanning, phone.running, bluetooth, bluetooth.running
                }

                int phoneSignalStrength = energyInfo.getPhoneSignalStrength();

                if (powerProfile.getRadioInfo().size() == phoneSignalStrength - 1) {
                    ampere += powerProfile.getRadioInfo().get(phoneSignalStrength - 1) / 1000;
                } else {
                    ampere += powerProfile.getRadioInfo().get(powerProfile.getRadioInfo().size() - 1) / 1000;
                }
            */

                double watt = ampere * energyInfo.getVoltage() / 1000;
                double nanoseconds;
                if (traceLine.getExit() < energyInfo.getExit()) {
                    nanoseconds = traceLine.getExit() - energyInfo.getEntrance();
                } else {
                    nanoseconds = energyInfo.getExit() - energyInfo.getEntrance();
                }
                double seconds = nanoseconds / 1000000000;
                totalSeconds += seconds;
                joule += watt * seconds;
            }
        }

        traceLine.setTimeLength(totalSeconds);
        traceLine.setConsumption(joule);

        return traceLine;
    }

    private void startApp(String appName) throws NoDeviceFoundException {
        System.out.println("Starting app.");
        this.executeCommand("adb shell input keyevent 82", null);
        this.executeCommand("adb shell monkey -p " + appName + " 1", null);
        this.executeCommand("adb shell am broadcast -a org.thisisafactory.simiasque.SET_OVERLAY --ez enable true", null);

    }

    private void stopApp(String appName, int run) throws NoDeviceFoundException {
        System.out.println("Run " + run + ": stopping app.");
        this.executeCommand("adb shell am broadcast -a org.thisisafactory.simiasque.SET_OVERLAY --ez enable false", null);
        this.executeCommand("adb shell am force-stop " + appName, null);
        this.executeCommand("adb shell pm clear " + appName, null);
    }

    public String executeCommand(String command, File outputFile) throws NoDeviceFoundException {
        StringBuilder output = new StringBuilder();

        try {
            List<String> listCommands = new ArrayList<>();

            String[] arrayExplodedCommands = command.split(" ");
            listCommands.addAll(Arrays.asList(arrayExplodedCommands));
            ProcessBuilder pb = new ProcessBuilder(listCommands);
            pb.redirectErrorStream(true);
            if (outputFile != null) {
                pb.redirectOutput(outputFile);
            }

            java.lang.Process commandProcess = pb.start();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    output.append(line).append("\n");
                    if (line.contains("error: no devices/emulators found")) {
                        throw new NoDeviceFoundException();
                    }
                }

                commandProcess.waitFor();
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        return output.toString();
    }

    private void checkADBExists() throws ADBNotFoundException {
        String sdkFolderPath = System.getenv("ANDROID_HOME");
        String adbPath = sdkFolderPath + "/platform-tools/adb";
        File adbFile = new File(adbPath);
        if (!adbFile.exists()) {
            throw new ADBNotFoundException();
        }
    }

    public String extractAppName(String apkLocationPath) throws NoDeviceFoundException, AppNameCannotBeExtractedException {
        String sdkFolderPath = System.getenv("ANDROID_HOME");
        String aaptPath = sdkFolderPath + "/build-tools/25.0.3/aapt";
        String aaptOutput = this.executeCommand(aaptPath + " dump badging " + apkLocationPath, null);
        String appName = "";
        Pattern pattern = Pattern.compile("package: name='([^']*)' versionCode='[^']*' versionName='[^']*' platformBuildVersionName='[^']*'");

        for (String line : aaptOutput.split("\n")) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                appName = matcher.group(1);
            }
        }

        if (appName.isEmpty()) {
            throw new AppNameCannotBeExtractedException();
        }

        return appName;
    }


    public void runProcess(String traceviewFilename, String systraceFilename, String batteryStatsFilename,
                           String repoPath, String outputDirectory, String profilingScript){
//        Start profiling
//	      Monitor logcat output
//	      Launch stop profile command
//        Detect logcat stop message, stop profiling
//	      pull trace file

//        ProcessBuilder pb = new ProcessBuilder("python3",python_script,"--trace_dump",traceviewFilename);
        try{
            String command = "python3 " + profilingScript + " --output_dir " + outputDirectory + " --repo " + repoPath +
                    " --trace_dump " + traceviewFilename + " --systrace " + systraceFilename + " --batterystats " +
                    batteryStatsFilename;
            System.out.println(command);
            this.executeCommand(command,null);
           // java.lang.Process commandProcess = pb.start();
            // commandProcess.waitFor();

        } catch (NoDeviceFoundException ex) {
            System.out.println(ex.getMessage());
        }
    }

    //build/outputs/apk then keyword should be debug
    public List<String> searchApk(String path, String directoryStructureString, String keyword) throws IOException{
        Stream<Path> paths = Files.walk(Paths.get(path));
        try{
            List<String> files = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toAbsolutePath().toString().toLowerCase().contains(directoryStructureString) &&
                                    p.toAbsolutePath().toString().toLowerCase().contains(keyword) &&
                            p.getFileName().toString().endsWith(".apk"))
                    .map(p -> p.toString())
                    .collect(Collectors.toList());
            return files;
        }finally{
            if(null != paths){
                paths.close();
            }
        }
    }
}
