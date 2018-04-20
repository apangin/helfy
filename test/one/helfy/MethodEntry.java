package one.helfy;

public class MethodEntry {
    static final JVM jvm = new JVM();
    static final int oopSize = jvm.intConstant("oopSize");

    static class InstanceKlass {
        static final long _klass_offset = jvm.getInt(jvm.type("java_lang_Class").global("_klass_offset"));
        static final long _methods = jvm.type("InstanceKlass").offset("_methods");
        static final long _methods_data = jvm.type("Array<Method*>").offset("_data");

        static long fromJavaClass(Class cls) {
            return JVM.unsafe.getLong(cls, _klass_offset);
        }

        static long methodAt(long klass, int slot) {
            long methods = jvm.getAddress(klass + _methods);
            int length = jvm.getInt(methods);
            if (slot < 0 || slot >= length) {
                throw new IndexOutOfBoundsException("Invalid method slot: " + slot);
            }
            return jvm.getAddress(methods + _methods_data + slot * oopSize);
        }
    }

    static class Method {
        static final long _i2i_entry = jvm.type("Method").offset("_i2i_entry");
        static final long _from_compiled_entry = jvm.type("Method").offset("_from_compiled_entry");
        static final long _from_interpreted_entry = jvm.type("Method").offset("_from_interpreted_entry");
        static final long _native_entry = jvm.type("Method").size;
        static final long _code = jvm.type("Method").offset("_code");
        static final long _verified_entry_point = jvm.type("nmethod").offset("_verified_entry_point");

        static long fromJavaMethod(Class<?> cls, String name, Class<?>... parameterTypes) throws ReflectiveOperationException {
            return fromJavaMethod(cls.getDeclaredMethod(name, parameterTypes));
        }

        static long fromJavaMethod(java.lang.reflect.Method m) throws ReflectiveOperationException {
            long klass = InstanceKlass.fromJavaClass(m.getDeclaringClass());
            java.lang.reflect.Field f = m.getClass().getDeclaredField("slot");
            f.setAccessible(true);
            int slot = f.getInt(m);
            return InstanceKlass.methodAt(klass, slot);
        }

        static void optimizeEntry(long method, String asm) {
            long code = jvm.getAddress(method + _code);
            long entry = jvm.getAddress(code + _verified_entry_point);

            jvm.putAddress(method + _i2i_entry, entry);
            jvm.putAddress(method + _from_compiled_entry, entry);
            jvm.putAddress(method + _from_interpreted_entry, entry);

            int length = asm.length();
            for (int i = 0; i < length; i += 2) {
                byte b = (byte) Integer.parseInt(asm.substring(i, i + 2), 16);
                jvm.putByte(entry++, b);
            }
        }
    }

    private static long benchmark() {
        long sum = 0;
        for (int i = 0; i < 1000000; i++) {
            sum += rdtsc();
        }
        return sum;
    }

    private static void runBenchmark() {
        long startTime = System.nanoTime();
        long sum = benchmark();
        long endTime = System.nanoTime();
        System.out.println((endTime - startTime) / 1e6 + "  " + sum);
    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary("MethodEntry");

        // Force compilation
        benchmark();

        long method = Method.fromJavaMethod(MethodEntry.class, "rdtsc");
        Method.optimizeEntry(method, "0f3148c1e220480bc2c3cccc");

        for (int i = 0; i < 1000; i++) {
            runBenchmark();
            System.gc();
        }
    }

    private static native long rdtsc();
}
