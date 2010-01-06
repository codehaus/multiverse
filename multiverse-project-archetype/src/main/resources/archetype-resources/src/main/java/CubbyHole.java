#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.datastructures.refs.Ref;

/**
 * A CubbyHole that stores an object.
 *
 * @author Andrew Phillips
 * @see Ref
 */
@AtomicObject
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

    @AtomicMethod(readonly = true)
    public E getOrAwait() {
        if (contents == null) {
            retry();
        }

        return contents;
    }

    @AtomicMethod(readonly = true)
    public boolean isEmpty() {
        return contents == null;
    }

    @AtomicMethod(readonly = true)
    public E get() {
        return contents;
    }

    public void set(E newContents) {
        this.contents = newContents;
    }

    public void clear() {
        set(null);
    }

    @AtomicMethod(readonly = true)
    public String toString() {
        if (contents == null) {
            return "CubbyHole(contents=null)";
        } else {
            return format("CubbyHole(contents=%s)", contents);
        }
    }
}