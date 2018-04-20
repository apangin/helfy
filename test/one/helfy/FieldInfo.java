package one.helfy;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

public class FieldInfo {
    static final JVM jvm = new JVM();
    static final int oopSize = jvm.intConstant("oopSize");

    static class Symbol {
        static final long _length = jvm.type("Symbol").offset("_length");
        static final long _body = jvm.type("Symbol").offset("_body");

        static String asString(long symbol) {
            int length = jvm.getShort(symbol + _length) & 0xffff;

            byte[] data = new byte[length];
            for (int i = 0; i < data.length; i++) {
                data[i] = jvm.getByte(symbol + _body + i);
            }

            return new String(data, StandardCharsets.UTF_8);
        }
    }

    static class ConstantPool {
        static final long _header_size = jvm.type("ConstantPool").size;

        static long at(long cpool, int index) {
            return jvm.getAddress(cpool + _header_size + index * oopSize);
        }
    }

    static class InstanceKlass {
        static final long _klass_offset = jvm.getInt(jvm.type("java_lang_Class").global("_klass_offset"));
        static final long _constants = jvm.type("InstanceKlass").offset("_constants");
        static final long _fields = jvm.type("InstanceKlass").offset("_fields");
        static final long _java_fields_count = jvm.type("InstanceKlass").offset("_java_fields_count");
        static final long _fields_data = jvm.type("Array<u2>").offset("_data");

        static long fromJavaClass(Class cls) {
            return JVM.unsafe.getLong(cls, _klass_offset);
        }

        static void printFields(Class cls) {
            long klass = fromJavaClass(cls);
            long cpool = jvm.getAddress(klass + _constants);
            long fields = jvm.getAddress(klass + _fields);

            int fieldCount = jvm.getShort(klass + _java_fields_count) & 0xffff;

            int accessFlagsOffset = jvm.intConstant("FieldInfo::access_flags_offset");
            int nameIndexOffset = jvm.intConstant("FieldInfo::name_index_offset");
            int lowPackedOffset = jvm.intConstant("FieldInfo::low_packed_offset");
            int fieldSlots = jvm.intConstant("FieldInfo::field_slots");
            int tagSize = jvm.intConstant("FIELDINFO_TAG_SIZE");

            for (int i = 0; i < fieldCount; i++) {
                long f = fields + _fields_data + i * fieldSlots * 2;

                short accessFlags = jvm.getShort(f + accessFlagsOffset * 2);
                if (Modifier.isStatic(accessFlags)) {
                    continue;
                }

                int nameIndex = jvm.getShort(f + nameIndexOffset * 2) & 0xffff;
                String name = Symbol.asString(ConstantPool.at(cpool, nameIndex));
                int offset = (jvm.getShort(f + lowPackedOffset * 2) & 0xffff) >>> tagSize;

                System.out.printf("  %2d  %s\n", offset, name);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("--- Fields visible through Reflection ---");

        for (java.lang.reflect.Field f : Throwable.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            System.out.println(f.getName());
        }

        System.out.println("--- Fields visible through VM Structs ---");

        InstanceKlass.printFields(Throwable.class);
    }
}
