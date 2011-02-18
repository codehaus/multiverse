package org.multiverse.api.exceptions;

import static java.lang.String.format;

/**
 * A {@link PropagationException} thrown when no {@link org.multiverse.api.Transaction} is available while it is mandatory. A typical
 * cause of this exception is that the {@link org.multiverse.api.PropagationLevel#Mandatory} is used.
 *
 * @author Peter Veentjer
 * @see org.multiverse.api.TransactionFactoryBuilder#setPropagationLevel(org.multiverse.api.PropagationLevel)
 */
public class TransactionMandatoryException extends PropagationException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a new TransactionRequiredException.
     */
    public TransactionMandatoryException() {
    }

    /**
     * Creates a new TransactionRequiredException with the provided message.
     *
     * @param message the message of the exception.
     */
    public TransactionMandatoryException(String message) {
        super(message);
    }

    /**
     * Creates a new TransactionRequiredException
     *
     * @param clazz the class of the method where the transaction was required
     * @param method the name of the method where the transaction was required.
     */
    public TransactionMandatoryException(Class clazz, String method){
        super(format("%s.%s is missing a required transaction", clazz.getName(),method));
    }

    /**
     * Creates a new TransactionRequiredException with the provided message.
     *
     * @param message the message of the exception.
     * @param cause   the cause of the exception.
     */
    public TransactionMandatoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
