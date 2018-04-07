package one.helfy;

public class DisableStructs {
    static final JVM jvm = new JVM();

    public static void main(String[] args) throws Exception {
        long structs = jvm.getSymbol("gHotSpotVMStructs");
        jvm.putAddress(structs, 0);

        System.out.println("VM Structs disabled. Try jstack -F or jmap -F");
        Thread.currentThread().join();
    }
}
