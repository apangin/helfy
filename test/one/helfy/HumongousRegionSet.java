package one.helfy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Counts the number of humongous regions in G1 heap.
 * See https://stackoverflow.com/questions/58856845
 */
public class HumongousRegionSet {
    private final JVM jvm = new JVM();
    private final long lengthFieldAddr = getLengthFieldAddr();

    private long getLengthFieldAddr() {
        long addr = jvm.getAddress(jvm.type("Universe").global("_collectedHeap"));
        addr += jvm.type("G1CollectedHeap").offset("_humongous_set");

        try {
            addr += jvm.type("HeapRegionSetBase").offset("_length");
        } catch (NoSuchElementException e) {
            addr += jvm.type("HeapRegionSetBase").offset("_count");
            addr += jvm.type("HeapRegionSetCount").offset("_length");
        }

        return addr;
    }

    public int getLength() {
        return jvm.getInt(lengthFieldAddr);
    }

    public static void main(String[] args) {
        HumongousRegionSet hrs = new HumongousRegionSet();
        System.out.printf("Initial number of humongous regions: %d\n", hrs.getLength());

        List<Object> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            list.add(new byte[64 * 1024 * 1024]);
            System.out.printf("Humongous regions after iteration #%d: %d\n", i, hrs.getLength());
        }

        list.clear();
        System.gc();

        System.out.printf("Humongous regions after GC: %d\n", hrs.getLength());
    }
}
