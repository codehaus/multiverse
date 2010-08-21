package org.multiverse.stms.beta;

public interface Onzin {
    <X extends Tlocal> X openForWrite(Tobject<X> o);

    IRafTlocal openForWrite(IRaf o);

    <E> RafTlocal<E> openForWrite(Raf<E> o);
}


class Raf<E> extends Tobject<RafTlocal<E>>{

}

class IRaf extends Tobject<IRafTlocal>{

}

class IRafTlocal extends Tlocal{

}

class RafTlocal<E> extends Tlocal {
    E value;

}

class Tobject<X extends Tlocal>{

}

class Tlocal{

}