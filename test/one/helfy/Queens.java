package one.helfy;

public class Queens {
    private final boolean[] vert = new boolean[9];
    private final boolean[] diag1 = new boolean[128];
    private final boolean[] diag2 = new boolean[128];

    private void place(int h) {
        if (h > 'h') {
            StackTrace.dumpCurrentThread();
            System.exit(0);
        }

        for (int v = 1; v <= 8; v++) {
            if (!vert[v] && !diag1[h + v] && !diag2[h - v]) {
                vert[v] = diag1[h + v] = diag2[h - v] = true;
                place(h + 1);
                vert[v] = diag1[h + v] = diag2[h - v] = false;
            }
        }
    }

    public static void main(String[] args) {
        new Queens().place('a');
    }
}
