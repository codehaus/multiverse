package org.multiverse.instrumentation.asm;

import org.objectweb.asm.tree.LabelNode;

import java.util.HashMap;

/**
 * A HashMap tailored to be used with the
 * {@link org.objectweb.asm.tree.AbstractInsnNode#clone(java.util.Map)}
 * <p/>
 * It automatically creates replacement LabelNodes when a getClassMetadata is called. It appears to be the
 * preferred way for cloning instructions.
 *
 * @author Peter Veentjer
 */
public final class CloneMap extends HashMap<LabelNode, LabelNode> {

    @Override
    public LabelNode get(Object key) {
        return get((LabelNode) key);
    }

    public LabelNode get(LabelNode old) {
        LabelNode found = super.get(old);
        if (found == null) {
            found = new LabelNode();
            put(old, found);
        }
        return found;
    }
}