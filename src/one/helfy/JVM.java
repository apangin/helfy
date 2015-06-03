package one.helfy;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

public class JVM {
    private final Map<String, Type> types = new LinkedHashMap<>();
    private final Map<String, Number> constants = new LinkedHashMap<>();

    public JVM() {
        readVmTypes(readVmStructs());
        readVmIntConstants();
        readVmLongConstants();
    }

    private Map<String, Set<Field>> readVmStructs() {
        long entry            = getSymbol("gHotSpotVMStructs");
        long typeNameOffset   = getSymbol("gHotSpotVMStructEntryTypeNameOffset");
        long fieldNameOffset  = getSymbol("gHotSpotVMStructEntryFieldNameOffset");
        long typeStringOffset = getSymbol("gHotSpotVMStructEntryTypeStringOffset");
        long isStaticOffset   = getSymbol("gHotSpotVMStructEntryIsStaticOffset");
        long offsetOffset     = getSymbol("gHotSpotVMStructEntryOffsetOffset");
        long addressOffset    = getSymbol("gHotSpotVMStructEntryAddressOffset");
        long arrayStride      = getSymbol("gHotSpotVMStructEntryArrayStride");

        Map<String, Set<Field>> structs = new HashMap<>();

        for (;; entry += arrayStride) {
            String typeName = getStringRef(entry + typeNameOffset);
            String fieldName = getStringRef(entry + fieldNameOffset);
            if (fieldName == null) break;

            String typeString = getStringRef(entry + typeStringOffset);
            boolean isStatic = getInt(entry + isStaticOffset) != 0;
            long offset = getLong(entry + (isStatic ? addressOffset : offsetOffset));

            Set<Field> fields = structs.get(typeName);
            if (fields == null) structs.put(typeName, fields = new TreeSet<>());
            fields.add(new Field(fieldName, typeString, offset, isStatic));
        }

        return structs;
    }

    private void readVmTypes(Map<String, Set<Field>> structs) {
        long entry                = getSymbol("gHotSpotVMTypes");
        long typeNameOffset       = getSymbol("gHotSpotVMTypeEntryTypeNameOffset");
        long superclassNameOffset = getSymbol("gHotSpotVMTypeEntrySuperclassNameOffset");
        long isOopTypeOffset      = getSymbol("gHotSpotVMTypeEntryIsOopTypeOffset");
        long isIntegerTypeOffset  = getSymbol("gHotSpotVMTypeEntryIsIntegerTypeOffset");
        long isUnsignedOffset     = getSymbol("gHotSpotVMTypeEntryIsUnsignedOffset");
        long sizeOffset           = getSymbol("gHotSpotVMTypeEntrySizeOffset");
        long arrayStride          = getSymbol("gHotSpotVMTypeEntryArrayStride");

        for (;; entry += arrayStride) {
            String typeName = getStringRef(entry + typeNameOffset);
            if (typeName == null) break;

            String superclassName = getStringRef(entry + superclassNameOffset);
            boolean isOop = getInt(entry + isOopTypeOffset) != 0;
            boolean isInt = getInt(entry + isIntegerTypeOffset) != 0;
            boolean isUnsigned = getInt(entry + isUnsignedOffset) != 0;
            int size = getInt(entry + sizeOffset);

            Set<Field> fields = structs.get(typeName);
            types.put(typeName, new Type(typeName, superclassName, size, isOop, isInt, isUnsigned, fields));
        }
    }

    private void readVmIntConstants() {
        long entry       = getSymbol("gHotSpotVMIntConstants");
        long nameOffset  = getSymbol("gHotSpotVMIntConstantEntryNameOffset");
        long valueOffset = getSymbol("gHotSpotVMIntConstantEntryValueOffset");
        long arrayStride = getSymbol("gHotSpotVMIntConstantEntryArrayStride");

        for (;; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;

            int value = getInt(entry + valueOffset);
            constants.put(name, value);
        }
    }

    private void readVmLongConstants() {
        long entry       = getSymbol("gHotSpotVMLongConstants");
        long nameOffset  = getSymbol("gHotSpotVMLongConstantEntryNameOffset");
        long valueOffset = getSymbol("gHotSpotVMLongConstantEntryValueOffset");
        long arrayStride = getSymbol("gHotSpotVMLongConstantEntryArrayStride");

        for (;; entry += arrayStride) {
            String name = getStringRef(entry + nameOffset);
            if (name == null) break;

            long value = getLong(entry + valueOffset);
            constants.put(name, value);
        }
    }

    public int getInt(long addr) {
        return Unsafe.instance.getInt(addr);
    }

    public long getLong(long addr) {
        return Unsafe.instance.getLong(addr);
    }

    public long getAddress(long addr) {
        return Unsafe.instance.getAddress(addr);
    }

    public String getString(long addr) {
        if (addr == 0) {
            return null;
        }

        char[] chars = new char[40];
        int offset = 0;
        for (byte b; (b = Unsafe.instance.getByte(addr + offset)) != 0; ) {
            if (offset >= chars.length) chars = Arrays.copyOf(chars, offset * 2);
            chars[offset++] = (char) b;
        }
        return new String(chars, 0, offset);
    }

    public String getStringRef(long addr) {
        return getString(getAddress(addr));
    }

    public long getSymbol(String name) {
        long address = Symbols.lookup(name);
        if (address == 0) {
            throw new NoSuchElementException("No such symbol: " + name);
        }
        return getLong(address);
    }

    public Type type(String name) {
        Type type = types.get(name);
        if (type == null) {
            throw new NoSuchElementException("No such type: " + name);
        }
        return type;
    }

    public long constant(String name) {
        Number constant = constants.get(name);
        if (constant == null) {
            throw new NoSuchElementException("No such constant: " + name);
        }
        return constant.longValue();
    }

    public void dump(PrintStream out) {
        out.println("Constants:");
        for (Map.Entry<String, Number> entry : constants.entrySet()) {
            String type = entry.getValue() instanceof Long ? "long" : "int";
            out.println("const " + type + ' ' + entry.getKey() + " = " + entry.getValue());
        }
        out.println();

        out.println("Types:");
        for (Type type : types.values()) {
            out.println(type);
        }
    }
}
