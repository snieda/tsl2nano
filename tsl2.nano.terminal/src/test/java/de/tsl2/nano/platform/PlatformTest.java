package de.tsl2.nano.platform;

import java.util.ArrayList;

import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.platform.PlatformManagement;

public class PlatformTest {

    @Test
    public void testPlatformBeans() throws Exception {
        ENV.setProperty("beandef.autoinit", false);
        PlatformManagement.printMBeans(System.out, null);
        PlatformManagement.logNotifications(null);

        //do a outofmemoryerror
        ArrayList<byte[]> array = new ArrayList<byte[]>();
        try {
            for (int i = 0; i < 1000000; i++) {
                array.add(new byte[1024 * i]);
            }
        } catch (OutOfMemoryError e) {
            System.out.println(e.toString());
        }
        System.gc();
        System.out.println("end: " + array.size());
    }
}
