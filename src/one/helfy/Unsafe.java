package one.helfy;

import java.lang.reflect.Field;

public class Unsafe {
    public static final sun.misc.Unsafe instance;

    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            instance = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new JVMException("Unable to get Unsafe", e);
        }
    }
}
