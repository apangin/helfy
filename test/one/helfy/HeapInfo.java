package one.helfy;

public class HeapInfo {

    public static void main(String[] args) {
        JVM jvm = new JVM();

        // Parallel GC
        Type parallelScavengeHeap = jvm.type("ParallelScavengeHeap");
        long youngGen = jvm.getAddress(parallelScavengeHeap.global("_young_gen"));
        long oldGen = jvm.getAddress(parallelScavengeHeap.global("_old_gen"));
        if (youngGen != 0 && oldGen != 0) {
            System.out.println("ParallelScavengeHeap detected");
            Type psVirtualSpace = jvm.type("PSVirtualSpace");

            long vs = jvm.getAddress(youngGen + jvm.type("PSYoungGen").offset("_virtual_space"));
            long start = jvm.getAddress(vs + psVirtualSpace.offset("_reserved_low_addr"));
            long end = jvm.getAddress(vs + psVirtualSpace.offset("_reserved_high_addr"));
            System.out.println("PSYoungGen: 0x" + Long.toHexString(start) + " - 0x" + Long.toHexString(end));

            vs = jvm.getAddress(oldGen + jvm.type("PSOldGen").offset("_virtual_space"));
            start = jvm.getAddress(vs + psVirtualSpace.offset("_reserved_low_addr"));
            end = jvm.getAddress(vs + psVirtualSpace.offset("_reserved_high_addr"));
            System.out.println("PSOldGen: 0x" + Long.toHexString(start) + " - 0x" + Long.toHexString(end));
        }

        // CMS GC
        Type genCollectedHeap = jvm.type("GenCollectedHeap");
        long gch = jvm.getAddress(genCollectedHeap.global("_gch"));
        if (gch != 0) {
            System.out.println("GenCollectedHeap detected");
            Type generation = jvm.type("Generation");
            Type virtualSpace = jvm.type("VirtualSpace");
            int ptrSize = jvm.type("Generation*").size;

            int nGens = jvm.getInt(gch + genCollectedHeap.offset("_n_gens"));
            for (int i = 0; i < nGens; i++) {
                long gen = jvm.getAddress(gch + genCollectedHeap.offset("_gens") + i * ptrSize);
                long vs = gen + generation.offset("_virtual_space");
                long start = jvm.getAddress(vs + virtualSpace.offset("_low_boundary"));
                long end = jvm.getAddress(vs + virtualSpace.offset("_high_boundary"));
                System.out.println("Generation " + i + ": 0x" + Long.toHexString(start) + " - 0x" + Long.toHexString(end));
            }
        }
    }
}
