package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class MetadataRepository_SetterTest {

    private MetadataRepository repo;

    @Before
    public void setUp() {
        repo = new MetadataRepository();
    }

    @Test
    public void whenPrimitiveSetter() {
        ClassMetadata classMetadata = repo.loadClassMetadata(SetterObject.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("setValue", "(I)V");

        assertEquals(MethodType.setter, methodMetadata.getMethodType());
    }

    @Test
    public void whenObjectSetter() {
        ClassMetadata classMetadata = repo.loadClassMetadata(SetterObject.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("setObjectValue", "(Ljava/lang/Object;)V");

        assertEquals(MethodType.setter, methodMetadata.getMethodType());
    }

    @Test
    public void whenSetterWithUnboxing() {
        ClassMetadata classMetadata = repo.loadClassMetadata(SetterObject.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("setWithAutoUnboxing", "(Ljava/lang/Integer;)V");

        assertEquals(MethodType.unknown, methodMetadata.getMethodType());
    }

    @Test
    public void whenStrangeName_thenStillSetter() {
        ClassMetadata classMetadata = repo.loadClassMetadata(SetterObject.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("strangeName", "(I)V");

        assertEquals(MethodType.setter, methodMetadata.getMethodType());
    }

    @Test
    public void whenSetterReturnsValue() {
        ClassMetadata classMetadata = repo.loadClassMetadata(SetterObject.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("setValueAndReturn", "(I)I");

        assertEquals(MethodType.unknown, methodMetadata.getMethodType());
    }

    @Test
    public void whenSetterDoesMore() {
        ClassMetadata classMetadata = repo.loadClassMetadata(SetterObject.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("setValueAndDoMore", "(I)V");

        assertEquals(MethodType.unknown, methodMetadata.getMethodType());
    }

    @Test
    public void whenSetterThrowsException() {
        ClassMetadata classMetadata = repo.loadClassMetadata(SetterObject.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("setValueAndThrowException", "(I)V");

        assertEquals(MethodType.unknown, methodMetadata.getMethodType());
    }

    @Test
    public void whenAbstractSetter() {
        ClassMetadata classMetadata = repo.loadClassMetadata(SetterObject.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("abstractSet", "(I)V");

        assertEquals(MethodType.unknown, methodMetadata.getMethodType());
    }

    abstract class SetterObject {
        private int value;
        private Object objectValue;

        abstract void abstractSet(int value);

        void setObjectValue(Object o) {
            this.objectValue = o;
        }

        void setValue(int newValue) {
            this.value = newValue;
        }

        void setWithAutoUnboxing(Integer newValue) {
            this.value = newValue;
        }

        void setValueAndThrowException(int newValue) {
            this.value = newValue;
            throw new RuntimeException();
        }

        void setValueAndDoMore(int newValue) {
            newValue++;
            this.value = newValue;
        }

        void strangeName(int newValue) {
            this.value = newValue;
        }

        int setValueAndReturn(int newValue) {
            this.value = newValue;
            return value;
        }
    }
}
