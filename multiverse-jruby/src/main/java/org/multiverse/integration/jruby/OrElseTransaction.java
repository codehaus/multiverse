package org.multiverse.integration.jruby;

import org.jruby.Ruby;
import org.jruby.runtime.Block;
import org.multiverse.api.Transaction;
import org.multiverse.templates.OrElseTemplate;

public class OrElseTransaction {
    private Ruby ruby;
    private Block orBlock;

    public OrElseTransaction(final Ruby ruby, final Block orBlock) {
        this.ruby = ruby;
        this.orBlock = orBlock;
    }
    public void orElse(final Block elseBlock){
        new OrElseTemplate(){

            @Override
            public Object either(Transaction tx) {
                orBlock.call(ruby.getCurrentContext());
                return null;
            }

            @Override
            public Object orelse(Transaction tx) {
                elseBlock.call(ruby.getCurrentContext());
                return null;
            }
        }.execute();
    }
}
