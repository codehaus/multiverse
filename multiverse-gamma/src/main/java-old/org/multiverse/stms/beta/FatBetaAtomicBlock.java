package org.multiverse.stms.beta;

import org.multiverse.api.PropagationLevel;
import org.multiverse.api.closures.*;

import java.util.logging.Logger;

/**
 * An AbstractBetaAtomicBlock made for the BetaStm.
 * <p/>
 * This code is generated.
 *
 * @author Peter Veentjer
 */
public final class FatBetaAtomicBlock extends AbstractBetaAtomicBlock {
    private static final Logger logger = Logger.getLogger(FatBetaAtomicBlock.class.getName());


    private final PropagationLevel propagationLevel;

    public FatBetaAtomicBlock(final BetaTransactionFactory transactionFactory) {
        super(transactionFactory);
        this.propagationLevel = transactionConfiguration.propagationLevel;
    }

    @Override
    public BetaTransactionFactory getTransactionFactory() {
        return transactionFactory;
    }

    @Override
    public <E> E execute(AtomicClosure<E> closure) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <E> E executeChecked(AtomicClosure<E> closure) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int execute(AtomicIntClosure closure) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int executeChecked(AtomicIntClosure closure) throws Exception {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long execute(AtomicLongClosure closure) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long executeChecked(AtomicLongClosure closure) throws Exception {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double execute(AtomicDoubleClosure closure) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double executeChecked(AtomicDoubleClosure closure) throws Exception {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean execute(AtomicBooleanClosure closure) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean executeChecked(AtomicBooleanClosure closure) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(AtomicVoidClosure closure) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void executeChecked(AtomicVoidClosure closure) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
