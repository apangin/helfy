package one.helfy;

import java.nio.charset.Charset;

import static one.helfy.JVM.unsafe;

public class GetBytecode {

    public static void main(String[] args) {
        JVM jvm = new JVM();
        Class targetClass = String.class;

        int oopSize = jvm.intConstant("oopSize");
        long klassOffset = jvm.getInt(jvm.type("java_lang_Class").global("_klass_offset"));
        long klass = oopSize == 8
                ? unsafe.getLong(targetClass, klassOffset)
                : unsafe.getInt(targetClass, klassOffset) & 0xffffffffL;

        long methodArray = jvm.getAddress(klass + jvm.type("InstanceKlass").offset("_methods"));
        int methodCount = jvm.getInt(methodArray);
        long methods = methodArray + jvm.type("Array<Method*>").offset("_data");

        long constMethodOffset = jvm.type("Method").offset("_constMethod");
        Type constMethodType = jvm.type("ConstMethod");
        Type constantPoolType = jvm.type("ConstantPool");
        long constantPoolOffset = constMethodType.offset("_constants");
        long codeSizeOffset = constMethodType.offset("_code_size");
        long nameIndexOffset = constMethodType.offset("_name_index");
        long signatureIndexOffset = constMethodType.offset("_signature_index");
        long symbolBodyOffset = jvm.type("Symbol").offset("_body");

        for (int i = 0; i < methodCount; i++) {
            long method = jvm.getAddress(methods + i * oopSize);
            long constMethod = jvm.getAddress(method + constMethodOffset);

            long constantPool = jvm.getAddress(constMethod + constantPoolOffset);
            int codeSize = jvm.getShort(constMethod + codeSizeOffset) & 0xffff;
            int nameIndex = jvm.getShort(constMethod + nameIndexOffset) & 0xffff;
            int signatureIndex = jvm.getShort(constMethod + signatureIndexOffset) & 0xffff;

            String name = getSymbol(jvm, constantPool + constantPoolType.size + nameIndex * oopSize);
            String signature = getSymbol(jvm, constantPool + constantPoolType.size + signatureIndex * oopSize);
            System.out.println("Method " + name + signature);

            long bytecodeStart = constMethod + constMethodType.size;
            for (int bci = 0; bci < codeSize; bci++) {
                System.out.printf(" %02x", jvm.getByte(bytecodeStart + bci));
            }
            System.out.println();
        }
    }

    private static String getSymbol(JVM jvm, long symbolAddress) {
        Type symbolType = jvm.type("Symbol");
        long symbol = jvm.getAddress(symbolAddress);
        long body = symbol + symbolType.offset("_body");
        int length = jvm.getShort(symbol + symbolType.offset("_length")) & 0xffff;

        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = jvm.getByte(body + i);
        }
        return new String(b, Charset.forName("UTF-8"));
    }
}
