package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Test;
import org.multiverse.instrumentation.asm.AsmUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class AsmUtilsTest {

    @Test
    public void test() {
        ClassNode node = AsmUtils.loadAsClassNode(Bar.class);

        for (MethodNode methodNode : (List<MethodNode>) node.methods) {
            if (methodNode.name.equals("<init>")) {
                int result = AsmUtils.firstIndexAfterSuper(methodNode,
                        Type.getInternalName(Bar.class.getSuperclass()));
                System.out.printf("method: %s.%s  = %s\n", methodNode.name, methodNode.desc, result);
            }
        }
    }


    public static class Foo {

        public Foo() {
        }

        public Foo(int i) {
            this();
        }
    }

    public static class Bar extends Foo {

        public Bar() {
        }

        public Bar(int i) {
            this();
        }
    }
}
