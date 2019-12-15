package net.pregi.android.netmesh.speedtest.process;

public class SummarizedThrowable extends Throwable {
    @Override
    public synchronized Throwable fillInStackTrace() {
        // Do nothing.
        // The entire point of this class is to avoid having to save this at all.
        return this;
    }

    private Class<?> originalExceptionClass;
    public Class<?> getOriginalExceptionClass() {
        return originalExceptionClass;
    }

    public SummarizedThrowable(Throwable e) {
        super(e.getMessage(), e.getCause() != null ? new SummarizedThrowable(e.getCause()) : null);

        originalExceptionClass = e.getClass();
    }
}
