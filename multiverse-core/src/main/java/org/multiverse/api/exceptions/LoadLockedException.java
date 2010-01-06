package org.multiverse.api.exceptions;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * A {@link LoadException} that indicates that a load failed because the item was locked.
 *
 * @author Peter Veentjer.
 */
public class LoadLockedException extends LoadException implements RecoverableThrowable {

    private static final long serialVersionUID = 0;

    public final static LoadLockedException INSTANCE = new LoadLockedException();

    public final static boolean reuse = parseBoolean(
            getProperty(LoadLockedException.class.getName() + ".reuse", "true"));

    public LoadLockedException() {
    }

    public LoadLockedException(String message) {
        super(message);
    }

    public LoadLockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadLockedException(Throwable cause) {
        super(cause);
    }
}
