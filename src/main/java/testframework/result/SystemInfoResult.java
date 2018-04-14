package testframework.result;

import com.sun.management.OperatingSystemMXBean;
import testframework.TestPhase;

import javax.management.*;
import java.lang.management.ManagementFactory;

public class SystemInfoResult implements IResult {
    private final String testGroupName;
    private final TestPhase phase;

    /**
     * generated internally when the object is created.
     */
    private final long timestamp;

    private final int processorCount;
    private final double usedMemoryMB;
    private final double maxMemoryMB;
    private final double jvmCPUPercent;
    private final double sysCPUPercent;

    public SystemInfoResult(String testGroupName, TestPhase phase) {
        this.testGroupName = testGroupName;
        this.phase = phase;

        this.timestamp = System.currentTimeMillis();

        this.processorCount = Runtime.getRuntime().availableProcessors();
        this.usedMemoryMB = MemoryUtils.usedMemory();
        this.maxMemoryMB = MemoryUtils.maxMemory();
        this.jvmCPUPercent = CPUUtils.usedJVMCPU();
        this.sysCPUPercent = CPUUtils.usedSystemCPU();
    }

    @Override
    public TestPhase getTestPhase() {
        return phase;
    }

    @Override
    public String getTestGroupName() {
        return testGroupName;
    }

    @Override
    public String CSVLine(boolean writeHeader) {
        String line = "";
        if(writeHeader){
            line += String.format("timestamp,time,pCount,usedMemMB,maxMemMB,jvmCPU,sysCPU%n");
        }
        String time = timeStampReadable(timestamp);
        return line + String.format("%d,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                timestamp, time, processorCount, usedMemoryMB, maxMemoryMB, jvmCPUPercent, sysCPUPercent);
    }

    public static class CPUUtils {
        private static OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        private static ObjectName name = null;
        private static AttributeList list;
        static {
            MBeanServer mbs    = ManagementFactory.getPlatformMBeanServer();
            try {
                name = ObjectName.getInstance("java.lang:type=OperatingSystem");
                list = mbs.getAttributes(name, new String[]{ "ProcessCpuLoad" });
            } catch (InstanceNotFoundException | ReflectionException | MalformedObjectNameException e) {
                e.printStackTrace();
            }
        }

        private static double usedJVMCPU2(){
            if(name == null || list == null){
                throw new IllegalArgumentException("ObjName or AttrList is null");
            }
            if (list.isEmpty())     return Double.NaN;

            Attribute att = (Attribute)list.get(0);
            Double value  = (Double)att.getValue();

            // usually takes a couple of seconds before we get real values
            if (value == -1.0)      return Double.NaN;
            // returns a percentage value with 1 decimal point precision
            return ((int)(value * 1000) / 10.0);
        }

        /**
         * @return percent cpu used by jvm
         */
        private static double usedJVMCPU(){
            return osBean.getProcessCpuTime()/10000000000.0;  //hack??;
        }

        /**
         * @return percent system cpu used
         */
        private static double usedSystemCPU(){
            return osBean.getSystemCpuLoad();
        }
    }

    public static class MemoryUtils {
        /**
         * Returns used memory in MB
         */
        private static double usedMemory() {
            Runtime runtime = Runtime.getRuntime();
            return usedMemory(runtime);
        }

        /**
         * Returns max memory available MB
         */
        private static double maxMemory() {
            Runtime runtime = Runtime.getRuntime();
            return maxMemory(runtime);
        }

        static double usedMemory(Runtime runtime) {
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double usedMemory = (double)(totalMemory - freeMemory) / (double)(1024 * 1024);
            return usedMemory;
        }

        static double maxMemory(Runtime runtime) {
            long maxMemory = runtime.maxMemory();
            double memory = (double)maxMemory / (double)(1024 * 1024);
            return memory;
        }

        public static void freeMemory() {
            System.gc();
            System.runFinalization();
        }
    }
}
