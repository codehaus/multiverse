package org.multiverse.stms.beta;

/**
 * Created by IntelliJ IDEA.
 * User: alarmnummer
 * Date: Sep 3, 2010
 * Time: 7:38:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Onzin {
    <X extends Tlocal> X openForWrite(Tobject<X> o);

    IRafTlocal openForWrite(IRaf o);

    <E> RafTlocal<E> openForWrite(Raf<E> o);
}

class Tlocal{

}