package de.tsl2.nano.execution;

import java.io.File;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.util.StringUtil;

public class SystemUtil {

    protected static final Log LOG = LogFactory.getLog(ScriptUtil.class);

    /**
     * execute system call - waiting for process to end - and logging its console output.<br>
     * example: <code>ScriptUtil.execute("cmd", "/C", "echo", "hello");</code>
     * <p>
     * if you have admin permission problems (IOException, 740 error) use "cmd.exe" and "/C" to start your executable.
     * 
     * @param command command with arguments
     */
    public static final Process execute(String... command) {
        final File scripFile = new File(command[0]);
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(scripFile.getParentFile());
        Process process = null;
        try {
            LOG.info("starting process with command: " + StringUtil.toString(command, 500)
                + "\ndir: "
                + processBuilder.directory()
                + "\nenv: "
                + processBuilder.environment());
            process = processBuilder.start();
            final Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                LOG.info(scanner.nextLine());
            }
            scanner.close();
            LOG.info("errorlevel: " + process.waitFor());
        } catch (final Exception e) {
            ForwardedException.forward(e);
        }
        return process;
    }

    /**
     * works only on windows! the given filename will be started with a registered windows application, handling files
     * of that type.<br>
     * example: <code>ScriptUtil.executeRegisteredWindowsPrg("c:/mydir/myfile.pdf");</code>
     * 
     * @param fileName file name with registered extension
     */
    public static final Process executeRegisteredWindowsPrg(String fileName) {
        return execute("rundll32", "url.dll,FileProtocolHandler", fileName);
    }

    public SystemUtil() {
        super();
    }

}