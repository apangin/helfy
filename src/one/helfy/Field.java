package one.helfy;

public class Field implements Comparable<Field> {
    public final String name;
    public final String typeName;
    public final long offset;
    public final boolean isStatic;

    Field(String name, String typeName, long offset, boolean isStatic) {
        this.name = name;
        this.typeName = typeName;
        this.offset = offset;
        this.isStatic = isStatic;
    }

    @Override
    public int compareTo(Field o) {
        if (isStatic != o.isStatic) {
            return isStatic ? -1 : 1;
        }
        return Long.compare(offset, o.offset);
    }

    @Override
    public String toString() {
        if (isStatic) {
            return "static " + typeName + ' ' + name + " @ 0x" + Long.toHexString(offset);
        } else {
            return typeName + ' ' + name + " @ " + offset;
        }
    }
}
