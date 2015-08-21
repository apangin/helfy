package one.helfy;

import static one.helfy.JVM.unsafe;

public class ObjectAddress {
    private static final long arrayBase = unsafe.arrayBaseOffset(Object[].class);
    private static final long arrayScale = unsafe.arrayIndexScale(Object[].class);
    private static final long narrowOopBase;
    private static final int narrowOopShift;

    static {
        JVM jvm = new JVM();
        Type universe = jvm.type("Universe");
        narrowOopBase = jvm.getAddress(universe.global("_narrow_oop._base"));
        narrowOopShift = jvm.getInt(universe.global("_narrow_oop._shift"));
    }

    public static long oopAddress(Object o) {
        Object[] array = new Object[] {o};
        if (arrayScale == 8) {
            return unsafe.getLong(array, arrayBase);
        } else {
            long narrowOop = unsafe.getInt(array, arrayBase) & 0xffffffffL;
            return narrowOopBase + (narrowOop << narrowOopShift);
        }
    }

    public static void main(String[] args) {
        Object o = new Object();
        long oopAddress = oopAddress(o);
        System.out.println(Long.toHexString(oopAddress));
    }
}
