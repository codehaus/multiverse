package org.multiverse.instrumentation.metadata;

import org.objectweb.asm.Type;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Peter Veentjer
 */
public class CompactFamilyNameStrategy implements FamilyNameStrategy {

    @Override
    public String create(String className, String methodName, String desc) {
        StringBuffer sb = new StringBuffer();
        sb.append(compact(className));

        sb.append('.');
        sb.append(methodName);
        sb.append('(');
        Type[] argTypes = Type.getArgumentTypes(desc);
        for (int k = 0; k < argTypes.length; k++) {
            String arg = argTypes[k].getClassName().replace(".", "/");
            sb.append(compact(arg));

            if (k < argTypes.length - 1) {
                sb.append(',');
            }
        }
        sb.append(')');

        return sb.toString();
    }

    public boolean isJavaLangPackage(String[] packages) {
        return packages.length == 2 && packages[0].equals("java") && packages[1].equals("lang");
    }

    public String compact(String classname) {
        StringBuffer sb = new StringBuffer();
        String[] packages = getPackages(classname);
        if (!isJavaLangPackage(packages)) {
            for (String pack : packages) {
                sb.append(pack.substring(0, 1)).append(".");
            }
        }
        sb.append(getBasicClassName(classname));
        return sb.toString();
    }

    public String[] getPackages(String classname) {
        classname = trim(classname);

        int indexOf = classname.lastIndexOf("/");
        if (indexOf == -1) {
            return new String[]{};
        }

        String packages = classname.substring(0, indexOf);

        List<String> result = new LinkedList<String>();
        StringTokenizer tokenizer = new StringTokenizer(packages, "/");
        while (tokenizer.hasMoreElements()) {
            String s = tokenizer.nextToken();
            result.add(s);
        }

        return result.toArray(new String[]{});
    }

    private String trim(String classname) {
        if (classname.endsWith(";")) {
            classname = classname.substring(0, classname.length() - 1);
        }

        if (classname.endsWith("L")) {
            classname = classname.substring(1, classname.length() - 1);
        }

        return classname;
    }

    public String getBasicClassName(String classname) {
        int indexOf = classname.lastIndexOf("/");
        if (indexOf == -1) {
            return classname;
        }

        return classname.substring(indexOf + 1);
    }
}
