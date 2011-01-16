package org.multiverse.stms.gamma;

import org.multiverse.api.references.RefFactoryBuilder;

public interface GammaRefFactoryBuilder  extends RefFactoryBuilder{

    @Override
    GammaRefFactory build();
}
