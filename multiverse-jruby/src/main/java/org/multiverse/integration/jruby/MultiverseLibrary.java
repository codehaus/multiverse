package org.multiverse.integration.jruby;


import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.exceptions.RaiseException;

import org.multiverse.api.GlobalStmInstance;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.ControlFlowError;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.templates.TransactionTemplate;

public class MultiverseLibrary implements Library{
    public void load(Ruby runtime, boolean wrap) throws IOException {
        runtime.getKernel().defineAnnotatedMethods(MultiverseAtomic.class);
    }

    public static class MultiverseAtomic {
        final static TransactionFactory txFactory = GlobalStmInstance.getGlobalStmInstance().getTransactionFactoryBuilder()
																					        .setReadonly(false)
																					        .setFamilyName("STM Operation")
																					        .setReadTrackingEnabled(true)
																					        .setSpeculativeConfigurationEnabled(true)
																					        .setExplicitRetryAllowed(true)
																					        .build();
        @JRubyMethod
        public static IRubyObject atomic(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby ruby = context.getRuntime();
        		return (IRubyObject) new TransactionTemplate<Object>(txFactory) {
        			@Override
        			public Object execute(Transaction tx) {
        				try{
        					return block.call(ruby.getCurrentContext());
        				}catch(RaiseException e){
        					rethrowKnownExceptions(ruby, e);
        				}
						return null;
        			}
        		}.execute();
        }
        private static void rethrowKnownExceptions(Ruby ruby, Exception ex){
        	Throwable cause = ex.getCause();
        	if(cause instanceof Retry){
    			throw Retry.create();
        	}
        	if(cause instanceof SpeculativeConfigurationFailure){
        		throw SpeculativeConfigurationFailure.create();
        	}
        	if(cause instanceof ControlFlowError){
        		throw new ControlFlowError(ex);
        	}
        	if(cause instanceof OldVersionNotFoundReadConflict){
        		throw new OldVersionNotFoundReadConflict();
        	}

        	throw ruby.newRuntimeError(ex.getLocalizedMessage());
        }
    }
}
