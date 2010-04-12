package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class MetadataRepository_GetterTest {
    private MetadataRepository repo;

    @Before
    public void setUp() {
        repo = new MetadataRepository();
    }

    @Test
    public void whenSimpleGetter() {
        ClassMetadata personMetadata = repo.loadClassMetadata(Person.class);
        MethodMetadata getNameMetadata = personMetadata.getMethodMetadata("getName", "()Ljava/lang/String;");
        assertEquals(MethodType.getter, getNameMetadata.getMethodType());
    }

    @Test
    public void whenAbstractGetter() {
        ClassMetadata personMetadata = repo.loadClassMetadata(Person.class);
        MethodMetadata getNameMetadata = personMetadata.getMethodMetadata("getAbstractName", "()Ljava/lang/String;");
        assertEquals(MethodType.unknown, getNameMetadata.getMethodType());
    }

    @Test
    public void nameDoesntMatterForGetter() {
        ClassMetadata personMetadata = repo.loadClassMetadata(Person.class);
        MethodMetadata getNameMetadata = personMetadata.getMethodMetadata("emanTeg", "()Ljava/lang/String;");
        assertEquals(MethodType.getter, getNameMetadata.getMethodType());
    }

    @Test
    public void whenVoidGetter_thenNoGetter() {
        ClassMetadata personMetadata = repo.loadClassMetadata(Person.class);
        MethodMetadata getNameMetadata = personMetadata.getMethodMetadata("getNameWithoutValue", "()V");
        assertEquals(MethodType.unknown, getNameMetadata.getMethodType());
    }

    @Test
    public void whenGetterHasArgument_thenNoGetter() {
        ClassMetadata personMetadata = repo.loadClassMetadata(Person.class);
        MethodMetadata getNameMetadata = personMetadata.getMethodMetadata("getName", "(Ljava/lang/String;)Ljava/lang/String;");
        assertEquals(MethodType.unknown, getNameMetadata.getMethodType());
    }

    @Test
    public void whenGetterIsMoreComplex_thenNoGetter() {
        ClassMetadata personMetadata = repo.loadClassMetadata(Person.class);
        MethodMetadata getNameMetadata = personMetadata.getMethodMetadata("getNameTooComplex", "()Ljava/lang/String;");
        assertEquals(MethodType.unknown, getNameMetadata.getMethodType());
    }

    @Test
    public void whenGetterIsConstant_thenNoGetter() {
        ClassMetadata personMetadata = repo.loadClassMetadata(Person.class);
        MethodMetadata getNameMetadata = personMetadata.getMethodMetadata("getConstantName", "()Ljava/lang/String;");
        assertEquals(MethodType.unknown, getNameMetadata.getMethodType());
    }

    abstract class Person {
        String name;

        public String getName() {
            return name;
        }

        public String emanTeg() {
            return name;
        }

        public void setName(String newName) {
            this.name = newName;
        }

        public void getNameWithoutValue() {
        }

        public abstract String getAbstractName();

        public String getName(String fake) {
            return name;
        }

        public String getNameTooComplex() {
            System.out.println("hello");
            return name;
        }

        public String get() {
            return name.toString();
        }

        public String getConstantName() {
            return "Peter";
        }
    }
}
