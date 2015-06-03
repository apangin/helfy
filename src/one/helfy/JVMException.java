package one.helfy;

public class JVMException extends RuntimeException {

    public JVMException() {
    }

    public JVMException(String message) {
        super(message);
    }

    public JVMException(String message, Throwable cause) {
        super(message, cause);
    }

    public JVMException(Throwable cause) {
        super(cause);
    }
}
