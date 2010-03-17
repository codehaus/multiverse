package org.multiverse.javaagent;

import java.util.logging.Logger;

/**
 * Since it is not possible to disrupt the instrumentation process executed by the JavaAgent,
 * if problems are encountered, some kind of warning mechanism needs to be created. That is
 * the task of this JavaAgentProblemMonitor.
 * <p/>
 * What is does is it launches a thread that prints warning messages every 10 second to the Log.servere when the
 * first problem is signalled. Following problems are ignored.
 * <p/>
 * todo: it should store all the exceptions (till some maximum).
 *
 * @author Peter Veentjer
 */
public final class JavaAgentProblemMonitor {

    private final static Logger logger = Logger.getLogger(JavaAgentProblemMonitor.class.getName());

    public final static JavaAgentProblemMonitor INSTANCE = new JavaAgentProblemMonitor();

    private volatile boolean problemFound;

    private JavaAgentProblemMonitor() {
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
            super("JavaAgentProblemMonitor-LoggingDaemon");
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
                    int delayMs = 10 * 1000;
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignore) {
                    //ignore
                }
            }
        }
    }
}
