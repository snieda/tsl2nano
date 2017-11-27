package de.tsl2.nano.core.execution;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.Policy;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.BitUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * provides some simple os execution utitilies.
 * 
 * @author Tom
 * @version $Revision$
 */
public class SystemUtil {

    protected static final Log LOG = LogFactory.getLog(SystemUtil.class);

    /**
     * executes given command in directory command[0].getParentFile().
     * 
     * @param command
     * @return
     */
    public static final Process execute(String... command) {
        File dir = new File(command[0]);
        dir = dir.isFile() ? dir.getParentFile() : new File(System.getProperty("user.dir"));
        return execute(dir, command);
    }

    /**
     * execute system call - waiting for process to end - and logging its console output.<br>
     * example: <code>ScriptUtil.execute("cmd", "/C", "echo", "hello");</code>
     * <p>
     * if you have admin permission problems (IOException, 740 error) use "cmd.exe" and "/C" to start your executable.
     * on unix use "sh" as shell prefix.
     * 
     * @param directory where to start the command from
     * @param command command with arguments
     */
    public static final Process execute(File directory, String... command) {
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(directory);
        Process process = null;
        try {
            LOG.info("starting process with command: " + StringUtil.toString(command, 500)
                + "\n\tdir: "
                + processBuilder.directory()
                + (LOG.isDebugEnabled() ? "\n\tenv: "
                    + processBuilder.environment() : ""));

            provideJdkAsJavaHome(processBuilder.environment());
            try {//on dalvik, inheritIO is not present - no problem 
                processBuilder.inheritIO();
            } catch (NoSuchMethodError err) {
                LOG.warn(err.toString());
            }
            process = processBuilder.start();
            //IMPROVE: could we use redirection? we need output to standard + log file
            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                LOG.info(scanner.nextLine());
            }
            scanner.close();
            int result = process.waitFor();
            LOG.info("\n-------------------------------------------------------------------\n"
                + "process '" + StringUtil.toString(command, -1) + "' finished with errorlevel: " + result);
            if (result != 0) {
                scanner = new Scanner(process.getErrorStream());
                StringBuilder buf = new StringBuilder();
                while (scanner.hasNextLine()) {
                    buf.append(scanner.nextLine() + "\n");
                }
                LOG.error(buf.toString());
                scanner.close();
            }
            LOG.info("\n-------------------------------------------------------------------");
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        return process;
    }

    /**
     * tries to set the system property 'java.home' as system environment variable 'JAVA_HOME'. if java.home is a path
     * to a JRE, we try to extract the path to JDK.
     * 
     * @param env writable os system environment map
     */
    private static void provideJdkAsJavaHome(Map<String, String> env) {
        if (!env.containsKey("java_home")) {
            String javaHome = System.getProperty("java.home");
            if (javaHome != null) {
                //use jdk instead of jre
                if (javaHome.contains("java/jre"))
                    javaHome = StringUtil.substring(javaHome, null, "/jre");
                LOG.info("JAVA_HOME wasn't set. setting it to: " + javaHome);
                env.put("JAVA_HOME", javaHome);
            }
        }
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

    /**
     * runs the given commands as root
     * 
     * @param cmds shell commands
     */
    public void runAsRoot(String... cmds) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String c : cmds) {
                os.writeBytes(c + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            ManagedException.forward(e);
        }
    }
    public static String createInfo(String buildInfo) {
        String info =
            "  build : ${build.info}\n"
                + "  args  : ${sun.java.command}\n"
                + "  dir   : ${user.dir}\n"
                + "  time  : ${nano.tstamp}\n"
                + "  user  : ${user.name}, home: ${user.home}\n"
                + "  lang  : ${user.country}_${user.language}, sun.jnu.encoding: ${sun.jnu.encoding}\n"
                + "  encode: ${file.encoding}\n"
                + "  loader: ${main.context.classloader}\n"
                + "  secure: ${security}\n"
                + "  java  : ${java.vm.name}, ${java.runtime.version}\n"
                + "  javhom: ${java.home}\n"
                + "  complr: ${java.compiler}\n"
                + "  memory: ${memory}\n"
                + "  discs : ${disc}\n"
                + "  io.tmp: ${java.io.tmpdir}\n"
                + "  os    : ${os.name}, ${os.version} ${sun.os.patch.level} ${os.arch}\n"
                + "  system: ${sun.cpu.isalist} ${sun.arch.data.model} x${processors}\n"
                + "  net-ip: ${inetadress.myip} (host-name: ${inetadress.hostname})\n";
        Properties p = new Properties();
        p.putAll(System.getProperties());
        if (System.getProperty("java.compiler") == null)
            p.put("java.compiler", "unspecified");
        p.put("nano.tstamp", new Date());
        p.put("main.context.classloader", Thread.currentThread().getContextClassLoader());

        InetAddress myAddress = NetUtil.getMyAddress();
        p.put("inetadress.myip", myAddress.getHostAddress());
        p.put("inetadress.hostname", myAddress.getHostName());

        String free = BitUtil.amount(Runtime.getRuntime().freeMemory());
        String total = BitUtil.amount(Runtime.getRuntime().totalMemory());
        p.put("memory", "free " + free + " of total " + total);

        String security;
        if (System.getSecurityManager() != null) {
            security = System.getSecurityManager().toString() + "(policy: " + Policy.getPolicy() + ")";
        } else {
            security = "<null>";
        }
        p.put("security", security);

        p.put("processors", Runtime.getRuntime().availableProcessors());

        File[] roots = File.listRoots();
        StringBuilder f = new StringBuilder();
        for (int i = 0; i < roots.length; i++) {
            f.append(roots[i].getName() + "(" + roots[i] + " " + BitUtil.amount(roots[i].getFreeSpace()) + "/"
                + BitUtil.amount(roots[i].getTotalSpace()) + ")");
        }
        p.put("disc", f.toString());
        p.put("build.info", buildInfo);
        return StringUtil.insertProperties(info, p);
    }
}