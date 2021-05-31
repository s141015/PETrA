package it.unisa.petra;

import it.unisa.petra.batch.ConfigManager;
import it.unisa.petra.core.exceptions.NoDeviceFoundException;
import it.unisa.petra.core.powerprofile.PowerProfile;
import it.unisa.petra.core.powerprofile.PowerProfileParser;
import it.unisa.petra.core.traceview.TraceLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import it.unisa.petra.core.Process;


public class MainTraceAnalyzer {
    public static void main(String[] args){

        try {
            Process process = new Process();
            String configFileLocation = new File(".").getAbsoluteFile().getParent() +
                    "/src/main/resources/config.properties";
            ConfigManager configManager = new ConfigManager(configFileLocation);
            File appDataFolder = new File(configManager.getOutputLocation());

            appDataFolder.delete();
            appDataFolder.mkdirs();

            String powerProfilePath = configManager.getPowerProfileFile();
            String outputLocation = configManager.getOutputLocation();

            if (powerProfilePath.isEmpty()) {
                process.extractPowerProfile(configManager.getOutputLocation());
                powerProfilePath = configManager.getOutputLocation() + "/power_profile.xml";
            }

            File[] outputDirectories = new File(outputLocation).listFiles(File::isDirectory);
            String pathToApp = "";
            String filter = "fr.neamar.kiss";
            assert outputDirectories != null;
            for (File outputDirectory : outputDirectories) {
                try{
                    String commitHash = outputDirectory.getName();
                    System.out.println("Executing apk: " + commitHash); //will be commit hash
                    pathToApp = outputDirectory.getAbsolutePath();

                    String[] appFolder = pathToApp.split("/");
                    String folderName = appFolder[appFolder.length-1];

                    String batteryStatsFilename = outputDirectory + File.separator + "batterystats";
                    String systraceFilename = outputDirectory + File.separator + "systrace";
                    String traceviewFilename = outputDirectory + File.separator + "tracedump";

                    System.out.println("aggregating results.");

                    System.out.println("parsing power profile.");
                    PowerProfile powerProfile = PowerProfileParser.parseFile(powerProfilePath);

                    List<TraceLine> traceLinesWiConsumptions = process.parseAndAggregateResults(traceviewFilename, batteryStatsFilename,
                            systraceFilename, powerProfile, filter);

                    PrintWriter resultsWriter = new PrintWriter(outputDirectory + File.separator + "result.csv", "UTF-8");
                    resultsWriter.println("signature, joule, seconds");

                    for (TraceLine traceLine : traceLinesWiConsumptions) {
                        resultsWriter.println(traceLine.getSignature() + "," + traceLine.getConsumption() + "," + traceLine.getTimeLength());
                    }

                    resultsWriter.flush();

                    System.out.println("Complete.");



                } catch( IOException | NullPointerException ex){
                    System.out.println(ex.getMessage());
                }
            }


        } catch ( NoDeviceFoundException | IOException ex) {
            System.out.println(ex.getMessage());
        }

    }
}



