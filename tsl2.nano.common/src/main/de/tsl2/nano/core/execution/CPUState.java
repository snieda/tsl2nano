package de.tsl2.nano.core.execution;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

import de.tsl2.nano.core.util.ByteUtil;

/**
 * provides some simple CPU measurements
 * 
 * @author Tom
 * @version $Revision$
 */
public class CPUState {
    com.sun.management.OperatingSystemMXBean jmxOS =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    RuntimeMXBean jmxRuntime = ManagementFactory.getRuntimeMXBean();
    ThreadMXBean jmxThread = ManagementFactory.getThreadMXBean();
    long upTime;
    long processCpuTime;
    long elapsedCpu;
    long elapsedTime;
    double cpuUsage, cpuUsageStep;

    public CPUState now() {
        upTime = jmxRuntime.getUptime();
        processCpuTime = jmxOS.getProcessCpuTime();
        elapsedCpu = jmxOS.getProcessCpuTime() - processCpuTime;
        elapsedTime = jmxRuntime.getUptime() - upTime;
        cpuUsage = elapsedTime > 0 ? elapsedCpu / elapsedTime : 0;
        cpuUsageStep = elapsedTime > 0 ? elapsedCpu / elapsedTime * 10000F * jmxOS.getAvailableProcessors() : 0;
        return this;
    }

    public String printInfo() {
        return "|" + System.getProperty("user.dir") + jmxRuntime.getInputArguments()
            + "\n|SYS:" + jmxOS.getArch() + "|x" + jmxOS.getAvailableProcessors() + "|" + jmxOS.getName() + "|"
            + jmxOS.getVersion() + "|CPU:" + cpuUsage + "%|MEM:"
            + ByteUtil.amount(jmxOS.getCommittedVirtualMemorySize()) + "|TOTAL:"
            + ByteUtil.amount(jmxOS.getTotalPhysicalMemorySize()) + "|THREADS:" + jmxThread.getThreadCount()
            + "|MAX-THREADS:" + jmxThread.getPeakThreadCount()
            + "\n-----------------------------------------------------------------------------------------";
    }
}
