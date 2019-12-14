package net.pregi.android.speedtester.speedtest.process;

public class SummarizedException extends Exception {
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

    public SummarizedException(Exception e) {
        super(e.getMessage(), e.getCause() != null ? new Throwable(e.getCause()) : null);

        originalExceptionClass = e.getClass();
    }
}
