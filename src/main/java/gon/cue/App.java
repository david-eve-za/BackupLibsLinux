package gon.cue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;


public class App {

    private static Logger log = Logger.getLogger(App.class.getName());

    static {
        initializeLog4j();
    }

    public static void main(String[] args) {

        Map<String, String> toBackup = new HashMap<String, String>();

        log.info("Start App");

        findDependency(toBackup, "/projects/sdk/bindings/java/.libs/libmegajava.so");

        String[] spl = "/projects/sdk/bindings/java/.libs/libmegajava.so".split("/");
        if (!toBackup.containsKey(spl[spl.length - 1])) {
            toBackup.put(spl[spl.length - 1], "/projects/sdk/bindings/java/.libs/libmegajava.so");
        }

        toBackup.forEach((k, v) -> {
            try {
                log.info("Copy file to: " + new File(System.getProperty("user.dir") + "/libs/" + k).getAbsolutePath());
                Files.copy(new File(v).toPath(), new File(System.getProperty("user.dir") + "/libs/" + k).toPath(),
                           StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });

        log.info("Total Dependencies: " + toBackup.size());
    }

    private static void findDependency(Map<String, String> toBackup, String library) {

        log.info("Processing: " + library);

        List<String> depend = runWithPrivileges("ldd", "-d", "-r", library);

        log.info("Dependencies: " + depend);

        for (String line : depend) {
            int firstSlash = line.indexOf("/");
            int firstParentesis = line.indexOf("(");

            if (firstSlash >= 0) {
                line = line.substring(firstSlash, firstParentesis - 1);

                String[] spl = line.split("/");

                if (!toBackup.containsKey(spl[spl.length - 1])) {
                    toBackup.put(spl[spl.length - 1], line);

                    findDependency(toBackup, line);
                }

            }
        }
    }

    public static List<String> runWithPrivileges(String... params) {

        List<String> _lines = new ArrayList<String>();

        InputStreamReader input;
        OutputStreamWriter output;
        BufferedReader buffRd;

        try {

            Process pb = new ProcessBuilder(params).start();

            output = new OutputStreamWriter(pb.getOutputStream());
            input = new InputStreamReader(pb.getInputStream());
            buffRd = new BufferedReader(input);

            String line;
            while ((line = buffRd.readLine()) != null) {
                _lines.add(line);
                if (line.contains("[sudo] password")) {
                    System.out.println("Please enter admin password");
                    char password[] = System.console().readPassword();
                    output.write(password);
                    output.write('\n');
                    output.flush();
                    // erase password data, to avoid security issues.
                    Arrays.fill(password, '\0');
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return _lines;
    }

    private static void initializeLog4j() {
        ConsoleAppender console = new ConsoleAppender(); // create appender
        // configure the appender
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.ALL);
        console.activateOptions();
        // add appender to any Logger (here is root)
        Logger.getRootLogger().addAppender(console);

        FileAppender fa = new FileAppender();
        fa.setName("FileLogger");
        fa.setFile("INFO.log");
        fa.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        fa.setThreshold(Level.ALL);
        fa.setAppend(true);
        fa.activateOptions();

        // add appender to any Logger (here is root)
        Logger.getRootLogger().addAppender(fa);
        // repeat with all other desired appenders
    }
}
