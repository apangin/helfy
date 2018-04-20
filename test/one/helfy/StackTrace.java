package one.helfy;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class StackTrace {
    static final JVM jvm = new JVM();
    static final int oopSize = jvm.intConstant("oopSize");

    static class JavaThread {
        static final long _anchor = jvm.type("JavaThread").offset("_anchor");
        static final long _last_Java_sp = _anchor + jvm.type("JavaFrameAnchor").offset("_last_Java_sp");
        static final long _last_Java_pc = _anchor + jvm.type("JavaFrameAnchor").offset("_last_Java_pc");
        static final long _last_Java_fp = _anchor + jvm.type("JavaFrameAnchor").offset("_last_Java_fp");

        static Frame topFrame(long thread) {
            long lastJavaSP = jvm.getAddress(thread + _last_Java_sp);
            long lastJavaFP = jvm.getAddress(thread + _last_Java_fp);
            long lastJavaPC = jvm.getAddress(thread + _last_Java_pc);
            return new Frame(lastJavaSP, lastJavaFP, lastJavaPC);
        }
    }

    static class Frame {
        static final long _name = jvm.type("CodeBlob").offset("_name");
        static final long _frame_size = jvm.type("CodeBlob").offset("_frame_size");
        static final long _method = jvm.type("nmethod").offset("_method");

        static final int slot_interp_bcp = -7;
        static final int slot_interp_locals = -6;
        static final int slot_interp_method = -3;
        static final int slot_interp_sender_sp = -1;
        static final int slot_link = 0;
        static final int slot_return_addr = 1;
        static final int slot_sender_sp = 2;

        long sp;
        long unextendedSP;
        long fp;
        long pc;

        Frame(long sp, long fp, long pc) {
            this(sp, sp, fp, pc);
        }

        Frame(long sp, long unextendedSP, long fp, long pc) {
            this.sp = sp;
            this.unextendedSP = unextendedSP;
            this.fp = fp;
            this.pc = pc;
        }

        long at(int slot) {
            return jvm.getAddress(fp + slot * oopSize);
        }

        long local(int index) {
            return jvm.getAddress(at(slot_interp_locals) - index * oopSize);
        }

        int bci() {
            long bcp = at(slot_interp_bcp);
            if (0 <= bcp && bcp <= 0xffff) {
                return (int) bcp;
            }
            return Method.bcpToBci(at(slot_interp_method), bcp);
        }

        long method() {
            if (Interpreter.contains(pc)) {
                return at(slot_interp_method);
            }

            long cb = CodeCache.findBlob(pc);
            if (cb != 0) {
                String name = jvm.getStringRef(cb + _name);
                if (name.endsWith("nmethod")) {
                    return jvm.getAddress(cb + _method);
                }
            }

            return 0;
        }

        Frame sender() {
            if (Interpreter.contains(pc)) {
                return new Frame(fp + slot_sender_sp * oopSize, at(slot_interp_sender_sp), at(slot_link), at(slot_return_addr));
            }

            long cb = CodeCache.findBlob(pc);
            if (cb != 0) {
                long senderSP = unextendedSP + jvm.getInt(cb + _frame_size);
                if (senderSP != sp) {
                    long senderPC = jvm.getAddress(senderSP - slot_return_addr * oopSize);
                    long savedFP = jvm.getAddress(senderSP - slot_sender_sp * oopSize);
                    return new Frame(senderSP, savedFP, senderPC);
                }
            }

            return null;
        }

        String valueAsText(long value) {
            if (value >= 0x100000000L) {
                return "0x" + Long.toHexString(value);
            } else if (value >= ' ' && value <= '~') {
                return value + " '" + (char) value + "'";
            } else {
                return Long.toString(value);
            }
        }

        void dumpLocals(PrintStream out, long method) {
            int maxLocals = Method.maxLocals(method);
            for (int i = 0; i < maxLocals; i++) {
                out.println("\t  [" + i + "] " + valueAsText(local(i)));
            }
        }

        public void dump(PrintStream out) {
            long method = method();
            if (method == 0) {
                return;
            }

            if (Interpreter.contains(pc)) {
                out.println("\tat " + Method.name(method) + " @ " + bci());
                dumpLocals(out, method);
                return;
            }

            long cb = CodeCache.findBlob(pc);
            if (cb != 0) {
                out.println("[compiled] " + Method.name(method));
            }
        }
    }

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
        static final long _pool_holder = jvm.type("ConstantPool").offset("_pool_holder");

        static long holder(long cpool) {
            return jvm.getAddress(cpool + _pool_holder);
        }

        static long at(long cpool, int index) {
            return jvm.getAddress(cpool + _header_size + index * oopSize);
        }
    }

    static class Klass {
        static final long _name = jvm.type("Klass").offset("_name");

        static String name(long klass) {
            long symbol = jvm.getAddress(klass + _name);
            return Symbol.asString(symbol);
        }
    }

    static class Method {
        static final long _constMethod = jvm.type("Method").offset("_constMethod");
        static final long _constMethod_size = jvm.type("ConstMethod").size;
        static final long _constants = jvm.type("ConstMethod").offset("_constants");
        static final long _name_index = jvm.type("ConstMethod").offset("_name_index");
        static final long _max_locals = jvm.type("ConstMethod").offset("_max_locals");

        static String name(long method) {
            long constMethod = jvm.getAddress(method + _constMethod);
            long cpool = jvm.getAddress(constMethod + _constants);
            int index = jvm.getShort(constMethod + _name_index) & 0xffff;

            String klassName = Klass.name(ConstantPool.holder(cpool));
            String methodName = Symbol.asString(ConstantPool.at(cpool, index));
            return klassName + '.' + methodName;
        }

        static int maxLocals(long method) {
            long constMethod = jvm.getAddress(method + _constMethod);
            return jvm.getShort(constMethod + _max_locals) & 0xffff;
        }

        static int bcpToBci(long method, long bcp) {
            long constMethod = jvm.getAddress(method + _constMethod);
            return (int) (bcp - constMethod - _constMethod_size);
        }
    }

    static class Interpreter {
        static final long code = jvm.getAddress(jvm.type("AbstractInterpreter").global("_code"));
        static final long _stub_buffer = code + jvm.type("StubQueue").offset("_stub_buffer");
        static final long _buffer_limit = code + jvm.type("StubQueue").offset("_buffer_limit");

        static boolean contains(long pc) {
            long offset = pc - jvm.getAddress(_stub_buffer);
            return 0 <= offset && offset < jvm.getInt(_buffer_limit);
        }
    }

    static class CodeCache {
        static final long codeHeap = jvm.getAddress(jvm.type("CodeCache").global("_heap"));
        static final long _memory = codeHeap + jvm.type("CodeHeap").offset("_memory");
        static final long _segmap = codeHeap + jvm.type("CodeHeap").offset("_segmap");
        static final long _log2_segment_size = codeHeap + jvm.type("CodeHeap").offset("_log2_segment_size");
        static final long _low = jvm.type("VirtualSpace").offset("_low");
        static final long _high = jvm.type("VirtualSpace").offset("_high");
        static final long _heap_block_size = jvm.type("HeapBlock").size;
        static final long _name = jvm.type("CodeBlob").offset("_name");

        static boolean contains(long pc) {
            return jvm.getAddress(_memory + _low) <= pc && pc < jvm.getAddress(_memory + _high);
        }

        static long findBlob(long pc) {
            if (!contains(pc)) {
                return 0;
            }

            long codeHeapStart = jvm.getAddress(_memory + _low);
            int log2SegmentSize = jvm.getInt(_log2_segment_size);

            long i = (pc - codeHeapStart) >>> log2SegmentSize;
            long b = jvm.getAddress(_segmap + _low);

            int v = jvm.getByte(b + i) & 0xff;
            if (v == 0xff) {
                return 0;
            }

            while (v > 0) {
                i -= v;
                v = jvm.getByte(b + i) & 0xff;
            }

            long heapBlock = codeHeapStart + (i << log2SegmentSize);
            return heapBlock + _heap_block_size;
        }
    }

    private static void dumpThread(Thread thread) {
        long vmThread = VMThread.of(thread);
        for (Frame f = JavaThread.topFrame(vmThread); f != null; f = f.sender()) {
            f.dump(System.err);
        }
    }

    public static void dumpCurrentThread() {
        new Thread("StackTrace getter") {
            final Thread caller = Thread.currentThread();

            @Override
            public synchronized void run() {
                dumpThread(caller);
            }

            synchronized void startAndWait() {
                try {
                    start();
                    join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.startAndWait();
    }

    public static void main(String[] args) {
        dumpCurrentThread();
    }
}
