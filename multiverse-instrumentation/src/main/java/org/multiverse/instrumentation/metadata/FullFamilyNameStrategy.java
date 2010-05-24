package org.multiverse.instrumentation.metadata;

import org.objectweb.asm.Type;

/**
 * @author Peter Veentjer
 */
public class FullFamilyNameStrategy implements FamilyNameStrategy {

    @Override
    public String create(String className, String methodName, String desc) {
        StringBuffer sb = new StringBuffer();
        sb.append(className.replace("/", "."));
        sb.append('.');
        sb.append(methodName);
        sb.append('(');
        Type[] argTypes = Type.getArgumentTypes(desc);
        for (int k = 0; k < argTypes.length; k++) {
            sb.append(argTypes[k].getClassName());
            if (k < argTypes.length - 1) {
                sb.append(',');
            }
        }
        sb.append(')');

        return sb.toString();
    }
}
