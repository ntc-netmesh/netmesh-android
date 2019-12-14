package net.pregi.lang;

/**<p>This class handles the running of one thread only if none is running.</p>
 * <p>Unlike a normal Thread, you can (actually, you should) use start() without recreating this object.
 * start() will only create a new thread if there is currently no running thread.</p>
 */
public class OneThread {
    private Runnable runnable;
    /** <p>Get the Runnable that will be executed by the next call of start(), or the Runnable
     * that is presently being executed by an active thread.</p>
     */
    public Runnable getRunnable() {
        return runnable;
    }

    private Thread activeThread;
    private boolean isDead() {
        return activeThread == null || !activeThread.isAlive();
    }

    /** <p>Starts a Thread with the runnable given to this OneThread object, if nothing is already running.</p>
     *
     * @return whether a thread was started by this call.
     */
    public boolean start() {
        return start(runnable);
    }

    private Object lock = new Object();
    /** <p>Starts a Thread with the given Runnable. This method allows you to make sure you will only
     * start one Runnable if you have a set of Runnables that you'd rather not run concurrently.</p>
     *
     * @param runnable the runnable to start.
     * @return whether a thread was started by this invocation. Providing a null also returns false.
     */
    public boolean start(Runnable runnable) {
        if (runnable != null && isDead()) {
            synchronized (lock) {
                if (isDead()) {
                    this.runnable = runnable;

                    activeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                OneThread.this.runnable.run();
                            } finally {
                                activeThread = null;
                            }
                        }
                    });
                    activeThread.start();
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    /** <p>Runs the last set Runnable. No effect if nothing was previously given.</p>
     *
     */
    public void run() {
        if (runnable != null) {
            run();
        }
    }

    public boolean isRunning() {
        return !isDead();
    }

    public OneThread() {
    }
    public OneThread(Runnable runnable) {
        this.runnable = runnable;
    }
}
