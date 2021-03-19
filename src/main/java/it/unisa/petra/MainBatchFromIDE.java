package it.unisa.petra;

import java.io.File;

import it.unisa.petra.batch.Terminal;

public class MainBatchFromIDE{
    public static void main(String[] args) {
        String path = new File(".").getAbsoluteFile().getParent();
        System.out.println(path);
        Terminal.run(path+"/src/main/resources/config.properties");
    }
}
