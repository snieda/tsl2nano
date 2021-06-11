package de.tsl2.nano.core.execution;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.tsl2.nano.core.util.ConcurrentUtil;

/**
 * provides a simple thread info list
 * 
 * @author Tom
 * @version $Revision$
 */
public class ThreadState {
    ThreadMXBean jmxThread = ManagementFactory.getThreadMXBean();
    long[] threadIds;
    long cpuUsage, time;
    //stores thread-id-->cpuTime
    HashMap<Long, Long> lastTimes = new HashMap<Long, Long>();
    String header;
    List<String> display;
    String info;
    CPUState cpu;
    int lines;
    boolean stop = false;
    
    public ThreadState() {
        this(22);
    }

    public ThreadState(int lines) {
        jmxThread.setThreadCpuTimeEnabled(true);
        cpu = new CPUState().now();
        this.lines = lines;
    }

    public ThreadState now() {
        threadIds = jmxThread.getAllThreadIds();
        time = System.nanoTime();

        header = cpu.now().printInfo();
        display = new ArrayList<String>(threadIds.length);
        for (int i = 0; i < threadIds.length; i++) {
            long cpuTime = jmxThread.getThreadCpuTime(threadIds[i]);
            // Calculate coarse CPU usage:
            Long lastThreadTime = lastTimes.get(threadIds[i]);
            if (lastThreadTime == null)
                lastThreadTime = cpuTime;
            double load = (cpuTime - lastThreadTime / (double) (System.nanoTime() - time));
            info = jmxThread.getThreadInfo(threadIds[i]).toString();
            display.add((float) load + "% " + info.substring(0, info.length() - 1));
            lastTimes.put(threadIds[i], cpuTime);
        }
        return this;
    }

    protected void print(String header, List<String> display) {
        Collections.sort(display);
        System.out.println(header);
        for (String line : display) {
            System.out.print(line);
        }
        int printed = 3 + display.size();
        for (int i = 0; i < lines - printed; i++) {
            System.out.println("");
        }
    }

    public void top(final long sleep) {
        ConcurrentUtil.startDaemon("cpu-times", new Runnable() {
            @Override
            public void run() {
                while (!stop) {
                    now().print(header, display);
                    ConcurrentUtil.sleep(sleep);
                }
            }
        });
    }
    public void stop() {
    	stop = true;
    }
}
