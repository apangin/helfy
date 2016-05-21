package one.helfy;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import com.sun.management.HotSpotDiagnosticMXBean;

public class ChangeVmFlag {

    public static void main(String[] args) throws IOException {
        MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean mxbean = 
                ManagementFactory.newPlatformMXBeanProxy(
                        mbserver, 
                        "com.sun.management:type=HotSpotDiagnostic", 
                        HotSpotDiagnosticMXBean.class);
        
        System.out.println("[BEFORE] UnlockDiagnosticVMOptions: " + mxbean.getVMOption("UnlockDiagnosticVMOptions"));
        
        JVM jvm = new JVM();

        Type flagType = jvm.type("Flag");
        int flagSize = flagType.size;
        
        Field flagsField = flagType.field("flags");
        long flagsFieldAddress = jvm.getAddress(flagsField.offset);
        
        Field numFlagsField = flagType.field("numFlags");
        int numFlagsValue = jvm.getInt(numFlagsField.offset);
        
        Field _nameField = flagType.field("_name");
        Field _addrField = flagType.field("_addr");
        
        // iterate until `numFlagsValue - 1` because last flag contains null values
        for (int i = 0; i < numFlagsValue - 1; i++) {
            long flagAddress = flagsFieldAddress + (i * flagSize);
            long flagValueAddress = jvm.getAddress(flagAddress +  _addrField.offset);
            long flagNameAddress = jvm.getAddress(flagAddress + _nameField.offset);
            String flagName = jvm.getString(flagNameAddress);
            
            if ("UnlockDiagnosticVMOptions".equals(flagName)) {
                if (jvm.getByte(flagValueAddress) == 0) {
                    jvm.putByte(flagValueAddress, (byte) 1);
                    System.out.println(flagName + " has been enabled");
                } else {
                    System.out.println(flagName + " is already enabled");
                }
            }
        }
        
        System.out.println("[AFTER] UnlockDiagnosticVMOptions: " + mxbean.getVMOption("UnlockDiagnosticVMOptions"));
    }

}
