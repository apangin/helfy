package one.helfy;

import java.util.NoSuchElementException;
import java.util.Set;

public class Type {
    private static final Field[] NO_FIELDS = new Field[0];

    public final String name;
    public final String superName;
    public final int size;
    public final boolean isOop;
    public final boolean isInt;
    public final boolean isUnsigned;
    public final Field[] fields;

    Type(String name, String superName, int size, boolean isOop, boolean isInt, boolean isUnsigned, Set<Field> fields) {
        this.name = name;
        this.superName = superName;
        this.size = size;
        this.isOop = isOop;
        this.isInt = isInt;
        this.isUnsigned = isUnsigned;
        this.fields = fields == null ? NO_FIELDS : fields.toArray(new Field[0]);
    }

    public Field field(String name) {
        for (Field field : fields) {
            if (field.name.equals(name)) {
                return field;
            }
        }
        throw new NoSuchElementException("No such field: " + name);
    }

    public long global(String name) {
        Field field = field(name);
        if (field.isStatic) {
            return field.offset;
        }
        throw new IllegalArgumentException("Static field expected");
    }

    public long offset(String name) {
        Field field = field(name);
        if (!field.isStatic) {
            return field.offset;
        }
        throw new IllegalArgumentException("Instance field expected");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (superName != null) sb.append(" extends ").append(superName);
        sb.append(" @ ").append(size).append('\n');
        for (Field field : fields) {
            sb.append("  ").append(field).append('\n');
        }
        return sb.toString();
    }
}
