package org.multiverse.durability.account;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import static org.multiverse.durability.StorageUtils.closeQuietly;

/**
 * @author Peter Veentjer
 */
public class SerializeUtils {

    public static Map<String,String> deserializeMap(byte[] bytes){
        ObjectInputStream in = null;
        try{
            in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (Map<String, String>) in.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally{
            closeQuietly(in);
        }
    }

    public static byte[] serialize(Map<String,String> map){
        ObjectOutputStream out = null;
        try{
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            out = new ObjectOutputStream(byteArrayOutputStream);
            out.writeObject(map);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally{
            closeQuietly(out);
        }
    }

    private SerializeUtils(){}
}
