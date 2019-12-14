package net.pregi.networking.speedtest;

public class TransferMeasure {
    private long time0;
    public long getTime0() {
        return time0;
    }

    private long time1;
    public long getTime1() {
        return time1;
    }

    private long nanoseconds;
    public long getNanoseconds() {
        return nanoseconds;
    }
    public long getMicroseconds() {
        return nanoseconds/1000;
    }
    public long getMilliseconds() {
        return nanoseconds/1000000;
    }

    private long size;
    public long getByteCount() {
        return size;
    }

    public TransferMeasure(long nanoseconds0, long nanoseconds1, long size) {
        this.time0 = nanoseconds0;
        this.time1 = nanoseconds1;
        this.nanoseconds = nanoseconds1-nanoseconds0;
        this.size = size;
    }
}
