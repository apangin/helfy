package one.helfy;

public class ObjectAddress {
    private static final long arrayBase = Unsafe.instance.arrayBaseOffset(Object[].class);
    private static final long arrayScale = Unsafe.instance.arrayIndexScale(Object[].class);
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
            return Unsafe.instance.getLong(array, arrayBase);
        } else {
            long narrowOop = Unsafe.instance.getInt(array, arrayBase) & 0xffffffffL;
            return narrowOopBase + (narrowOop << narrowOopShift);
        }
    }

    public static void main(String[] args) {
        Object o = new Object();
        long oopAddress = oopAddress(o);
        System.out.println(Long.toHexString(oopAddress));
    }
}
