package org.multiverse.durability;

/**
 * A {@link RuntimeException} that is thrown when something fails while 
 *
 * @author Peter Veentjer.
 */
public class StorageException extends RuntimeException{

    public StorageException() {
    }

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(Throwable cause) {
        super(cause);
    }
}
