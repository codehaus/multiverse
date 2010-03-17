package org.multiverse.javaagent;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;

/**
 * Since it is not possible to disrupt the instrumentation process executed by the JavaAgent,
 * if problems are encountered, some kind of warning mechanism needs to be created. That is
 * the task of this JavaAgentProblemMonitor.
 * <p/>
 * What is does is it launches a thread that prints warning messages every X second to the Log.
 * severe when the first problem is signalled.
 * <p/>
 * Class is threadsafe to call.
 * <p/>
 *
 * @author Peter Veentjer
 */
public final class JavaAgentProblemMonitor {

    private final static Logger logger = Logger.getLogger(JavaAgentProblemMonitor.class.getName());

    public final static JavaAgentProblemMonitor INSTANCE = new JavaAgentProblemMonitor();

    private final int maxProblemListSize;
    private final int delayMs;
    private final boolean startLoggingDeamon;

    private volatile boolean problemFound;
    private volatile List<String> problemClasses;

    private JavaAgentProblemMonitor() {
        this.maxProblemListSize = parseInt(getProperty("org.multiverse.javaagent.problemmonitor.maxProblems", "10"));
        this.startLoggingDeamon = parseBoolean(getProperty("org.multiverse.javaagent.problemmonitor.startLoggingDeamon", "true"));
        this.delayMs = parseInt(getProperty("org.multiverse.javaagent.problemmonitor.delayMs", "10000"));
    }

    public boolean isProblemFound() {
        return problemFound;
    }

    /**
     * Signals that a problem has happened while instrumenting some class.
     *
     * @param classname the class where the problem happened.
     * @throws NullPointerException if classname is null.
     */
    public void signalProblem(String classname) {
        if (classname == null) {
            throw new NullPointerException();
        }

        if (problemFound) {
            return;
        }

        synchronized (this) {
            if (problemFound) {
                return;
            }

            if (problemClasses.size() < maxProblemListSize && !problemClasses.contains(classname)) {
                List<String> newProblemClasses = new LinkedList<String>(problemClasses);
                newProblemClasses.add(classname);
                problemClasses = newProblemClasses;
            }

            problemFound = true;
            if (startLoggingDeamon) {
                new LoggingDaemon().start();
            }
        }
    }

    class LoggingDaemon extends Thread {

        LoggingDaemon() {
            super("JavaAgentProblemMonitor-LoggingDaemon");
            setDaemon(true);
        }

        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                StringBuffer sb = new StringBuffer("STM integrity compromised, instrumentation problems encountered. " +
                        "Partial instrumented classes could give unexpected results. Check the logging " +
                        "for the instrumentation exception(s).\n");

                sb.append(format("List of STM problem classes: (%s max)\n", maxProblemListSize));


                List<String> c = problemClasses;
                for (String classname : c) {
                    sb.append("    ");
                    sb.append(classname);
                    sb.append("\n");
                }

                logger.severe(sb.toString());

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignore) {
                    //ignore
                }
            }
        }
    }
}
