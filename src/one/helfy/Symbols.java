package one.helfy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Symbols {
    private static Method findNative;
    private static ClassLoader classLoader;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            String jre = System.getProperty("java.home");
            if (!loadLibrary(jre + "/bin/server/jvm.dll") && !loadLibrary(jre + "/bin/client/jvm.dll")) {
                throw new JVMException("Cannot find jvm.dll. Unsupported JVM?");
            }
            classLoader = Symbols.class.getClassLoader();
        }

        try {
            findNative = ClassLoader.class.getDeclaredMethod("findNative", ClassLoader.class, String.class);
            findNative.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new JVMException("Method ClassLoader.findNative not found");
        }
    }

    private static boolean loadLibrary(String dll) {
        try {
            System.load(dll);
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static long lookup(String name) {
        try {
            return (Long) findNative.invoke(null, classLoader, name);
        } catch (InvocationTargetException e) {
            throw new JVMException(e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new JVMException(e);
        }
    }
}
