package it.unisa.petra.batch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommitManager {

    public void checkoutToCommit(String hash, File workingDirectory){
        String command = "git checkout " + hash;
        this.execShell(command,workingDirectory);
    }

    public void resetCommit(File workingDirectory, String app){
        String command1 = "git clean -fd";
        String command2 = "git reset --hard";
        this.execShell(command1,workingDirectory.getAbsoluteFile());
        this.execShell(command2,workingDirectory.getAbsoluteFile());
        if (app.equals("AlarmClock")){
            String command3 = "git checkout develop";
            this.execShell(command3,workingDirectory.getAbsoluteFile());
        } else{
            String command3 = "git checkout master";
            this.execShell(command3,workingDirectory.getAbsoluteFile());
        }
    }

    public String executeCommand(String command, File workingDirectory) {
        StringBuilder output = new StringBuilder();

        try {
            List<String> listCommands = new ArrayList<>();
//            listCommands.add(workingDirectory);
            String[] arrayExplodedCommands = command.split(" ");
            listCommands.addAll(Arrays.asList(arrayExplodedCommands));
            ProcessBuilder pb = new ProcessBuilder(listCommands);
            pb.redirectErrorStream(true);
            pb.directory(workingDirectory);

            java.lang.Process commandProcess = pb.start();

            commandProcess.waitFor();

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        return output.toString();
    }

    public void execShell(String command, File workingDirectory) {
        List<String> listCommands = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(command,null,workingDirectory);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
