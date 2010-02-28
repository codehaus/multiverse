package org.multiverse.stms.alpha.instrumentation.asm;

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
public class CloneMap extends HashMap<LabelNode, LabelNode> {

//    private Map<LabelNode, List<StackTraceElement[]>> traceMap =
//            new HashMap<LabelNode, List<StackTraceElement[]>>();

//    public void printTraces(LabelNode labelNode) {
//        List<StackTraceElement[]> list = traceMap.getClassMetadata(labelNode);
//        for(StackTraceElement[] elements : list){
//            for(StackTraceElement element: elements){
//                System.out.println(element);
//            }
//            System.out.println("-------------------------------");
//        }
//    }

    @Override
    public LabelNode get(Object key) {
        return get((LabelNode) key);
    }

    public LabelNode get(LabelNode old) {
//        List<StackTraceElement[]> list = traceMap.getClassMetadata(old);
//        if (list == null) {
//            list = new LinkedList();
//            traceMap.put(old, list);
//        }
//
//        list.add(new Exception().getStackTrace());

        LabelNode found = super.get(old);
        if (found == null) {
            found = new LabelNode();
            put(old, found);
        }
        return found;
    }
}