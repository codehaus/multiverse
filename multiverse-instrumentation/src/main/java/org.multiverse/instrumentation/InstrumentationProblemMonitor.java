package org.multiverse.instrumentation;

import java.util.logging.Logger;

/**
 * Since it is not possible to disrupt the instrumentation process if problems are encountered, some kind of warning
 * mechanism needs to be created. That is the task of this InstrumentationProblemMonitor.
 * <p/>
 * What is does is it launches a thread that prints warning messages every 10 second to the Log.servere when the
 * first problem is signalled. Following problems are ignored.
 *
 * @author Peter Veentjer
 */
public final class InstrumentationProblemMonitor {

    private final static Logger logger = Logger.getLogger(InstrumentationProblemMonitor.class.getName());

    public final static InstrumentationProblemMonitor INSTANCE = new InstrumentationProblemMonitor();

    private volatile boolean problemFound;

    private InstrumentationProblemMonitor() {
    }

    public boolean isProblemFound() {
        return problemFound;
    }

    public void signalProblem() {
        if (problemFound) {
            return;
        }

        synchronized (this) {
            if (problemFound) {
                return;
            }

            problemFound = true;
            new LoggingDaemon().start();
        }
    }

    static class LoggingDaemon extends Thread {
        LoggingDaemon() {
            super("LoggingDaemon");
            setDaemon(true);
        }

        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                logger.severe("STM integrity compromised, instrumentation problems encountered. " +
                        "Partial instrumented classes could give unexpected results. Check the logging " +
                        "for the instrumentation exception(s).");
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException ignore) {
                    //ignore
                }
            }
        }
    }
}
