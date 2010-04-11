#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.transactional.TransactionalReference;

/**
 * A CubbyHole that stores an object.
 *
 * @author Andrew Phillips
 * @see TransactionalReference
 */
@TransactionalObject
public final class CubbyHole<E> {
    private E contents;

    /**
     * Creates a CubbyHole with a {@code null} content.
     */
    public CubbyHole() {
        contents = null;
    }

    /**
     * Creates a new CubbyHole with the provided content. This object is allowed to
     * be {@code null}.
     *
     * @param contents the content to store in this CubbyHole.
     */
    public CubbyHole(E contents) {
        this.contents = contents;
    }

    @TransactionalMethod(readonly = true)
    public E getOrAwait() {
        if (contents == null) {
            retry();
        }

        return contents;
    }

    @TransactionalMethod(readonly = true)
    public boolean isEmpty() {
        return contents == null;
    }

    @TransactionalMethod(readonly = true)
    public E get() {
        return contents;
    }

    public E set(E newContents) {
        E currentContents = contents;        
        this.contents = newContents;
        return currentContents;
    }

    public void clear() {
        set(null);
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        if (contents == null) {
            return "CubbyHole(contents=null)";
        } else {
            return format("CubbyHole(contents=%s)", contents);
        }
    }
}