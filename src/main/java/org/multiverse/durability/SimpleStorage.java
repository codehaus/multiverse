package org.multiverse.durability;

import org.multiverse.api.exceptions.TodoException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * A very basic implementation of the {@link Storage} interface. The internals are very ugly, and purely
 * meant as a mechanism to figure out how the interaction between the stm and the storage should happen.
 *
 * @author Peter Veentjer.
 */
public class SimpleStorage implements Storage {

    private final File dir;
    private final File rootsFile;
    private final ConcurrentMap<Class, DurableObjectSerializer> map = new ConcurrentHashMap<Class, DurableObjectSerializer>();
    private final ObjectIdentityMap objectIdentityMap = new SimpleObjectIdentityMap();
    private final HashSet<String> rootIds = new HashSet<String>();
    private final BetaStm betaStm;

    public SimpleStorage(BetaStm betaStm) {
        this(betaStm, new File(System.getProperty("java.io.tmpdir"), "storage"));
    }

    public SimpleStorage(BetaStm betaStm, File dir) {
        if (betaStm == null || dir == null) {
            throw new NullPointerException();
        }

        this.betaStm = betaStm;
        this.dir = dir;
        dir.mkdirs();
        rootsFile = new File(dir, "roots");
        if (!rootsFile.exists()) {
            try {
                rootsFile.createNewFile();
            } catch (IOException ex) {
                throw new StorageException(ex);
            }
        } else {
            rootIds.addAll(StorageUtils.loadLines(rootsFile));
        }
    }

    @Override
    public UnitOfWrite startUnitOfWrite() {
        return new UnitOfWriteImpl();
    }

    public void clearEntities() {
        objectIdentityMap.clear();
    }

    public void register(Class clazz, DurableObjectSerializer serializer) {
        if (clazz == null || serializer == null) {
            throw new NullPointerException();
        }

        map.put(clazz, serializer);
    }

    @Override
    public void clear() {
        rootIds.clear();
        for (File file : dir.listFiles()) {
            file.delete();
        }
    }

    private boolean exists(String id) {
        return new File(dir, id).exists();
    }

    private Set<DurableState> loadRoots(Map<String, DurableState> overrides) {
        Set<DurableState> result = new HashSet<DurableState>();

        for (String rootId : rootIds) {
            System.out.println("rootId: " + rootId);

            DurableState rootState;

            if (overrides.containsKey(rootId)) {
                rootState = overrides.get(rootId);
            } else {
                rootState = loadState(rootId);
            }

            result.add(rootState);
        }
        return result;
    }

    //mega inefficient implementation

    private void loadGraph(DurableState root, Set<DurableState> result, Map<String, DurableState> overrides) {
        DurableObject owner = root.getOwner();
        String ownerStorageId = owner.getStorageId();

        if (overrides.containsKey(ownerStorageId)) {
            root = overrides.get(ownerStorageId);
        }

        if (result.contains(root)) {
            return;
        }

        result.add(root);

        for (Iterator<DurableObject> it = root.getReferences(); it.hasNext();) {
            DurableObject dependency = it.next();
            String storageId = dependency.getStorageId();

            DurableState state;
            if (overrides.containsKey(storageId)) {
                state = overrides.get(storageId);
            } else {
                state = loadState(storageId);
            }

            loadGraph(state, result, overrides);
        }
    }

    private Set<DurableState> loadGraph(UnitOfWriteImpl unitOfWork) {
        Map<String, DurableState> overrides = new HashMap<String, DurableState>();

        for (DurableState change : unitOfWork.changes.values()) {
            overrides.put(change.getOwner().getStorageId(), change);
        }

        System.out.println("overrides:" + overrides);

        Set<DurableState> result = new HashSet<DurableState>();
        for (DurableState rootState : loadRoots(overrides)) {
            loadGraph(rootState, result, overrides);
        }

        return result;
    }

    private Set<DurableState> filterDurables(UnitOfWriteImpl unitOfWork) {
        Set<DurableState> result = new HashSet<DurableState>();

        Set<DurableState> graph = loadGraph(unitOfWork);

        for (DurableState change : unitOfWork.changes.values()) {
            if (graph.contains(change)) {
                result.add(change);
            }
        }

        return result;
    }

    private void persist(DurableObject entity, DurableState state) {
        if (entity == null || state == null) {
            throw new NullPointerException();
        }

        DurableObjectSerializer serializer = map.get(entity.getClass());
        if (serializer == null) {
            throw new StorageException("Can't find serializer for class: " + entity.getClass());
        }

        objectIdentityMap.putIfAbsent(entity);

        File file = new File(dir, entity.getStorageId());

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(entity.getClass().getName().getBytes());
            out.write('\n');
            out.write(serializer.serialize(state));
            out.close();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    //todo: method needs to go.

    private DurableState loadState(String id) {
        if (id == null) {
            throw new NullPointerException();
        }

        File file = new File(dir, id);
        if (!file.exists()) {
            throw new StorageException(format("DurableObject with %s doesn't exist", id));
        }

        byte[] buffer;
        Class clazz;
        try {
            FileInputStream in = new FileInputStream(file);

            StringBuffer nameSb = new StringBuffer();
            int b;
            while ((b = in.read()) != '\n') {
                nameSb.append(new Character((char) b));
            }

            clazz = Class.forName(nameSb.toString());

            byte[] content = new byte[100000];
            int k = 0;
            while ((b = in.read()) != -1) {
                content[k] = (byte) b;
                k++;
            }
            buffer = Arrays.copyOf(content, k);
            in.close();
        } catch (IOException e) {
            throw new StorageException(e);
        } catch (ClassNotFoundException e) {
            throw new StorageException(e);
        }

        DurableObjectSerializer serializer = map.get(clazz);
        if (serializer == null) {
            throw new StorageException("Can't find serializer for class: " + clazz);
        }

        DurableObject entity = loadDurableObject(id);
        return serializer.deserializeState(entity, buffer, new DurableObjectLoaderImpl());
    }

    @Override
    public DurableObject loadDurableObject(String id) {
        if (id == null) {
            throw new NullPointerException();
        }

        DurableObject object = objectIdentityMap.get(id);

        //if it already is there, we are done.
        if (object != null) {
            return object;
        }

        //the object is not there.. so we need to load it        .
        BetaTransaction tx = betaStm.startDefaultTransaction();
        object = loadDurableObject(id, tx);
        DurableState state = tx.openForConstruction((BetaTransactionalObject) object, getThreadLocalBetaObjectPool());
        populate(state);
        tx.commit();
        return object;
    }

    private void populate(DurableState state) {
        String id = state.getOwner().getStorageId();
        File file = new File(dir, id);

        if (!file.exists()) {
            throw new StorageException(format("DurableObject with %s doesn't exist", id));
        }

        byte[] buffer;
        try {
            FileInputStream in = new FileInputStream(file);

            StringBuffer nameSb = new StringBuffer();
            int b;
            while ((b = in.read()) != '\n') {
                nameSb.append(new Character((char) b));
            }

            byte[] content = new byte[100000];
            int k = 0;
            while ((b = in.read()) != -1) {
                content[k] = (byte) b;
                k++;
            }
            buffer = Arrays.copyOf(content, k);
            in.close();
        } catch (IOException e) {
            throw new StorageException(e);
        }

        DurableObjectSerializer serializer = map.get(state.getOwner().getClass());
        serializer.populate(state, buffer, new DurableObjectLoaderImpl());
    }

    private DurableObject loadDurableObject(String id, BetaTransaction tx) {
        if (id == null) {
            throw new NullPointerException();
        }

        //it is not loaded before.
        File file = new File(dir, id);
        if (!file.exists()) {
            throw new StorageException(format("DurableObject with %s doesn't exist", id));
        }

        byte[] buffer;
        Class clazz;
        try {
            FileInputStream in = new FileInputStream(file);

            StringBuffer nameSb = new StringBuffer();
            int b;
            while ((b = in.read()) != '\n') {
                nameSb.append(new Character((char) b));
            }

            clazz = Class.forName(nameSb.toString());

            byte[] content = new byte[100000];
            int k = 0;
            while ((b = in.read()) != -1) {
                content[k] = (byte) b;
                k++;
            }
            buffer = Arrays.copyOf(content, k);
            in.close();
        } catch (IOException e) {
            throw new StorageException(e);
        } catch (ClassNotFoundException e) {
            throw new StorageException(e);
        }

        DurableObjectSerializer serializer = map.get(clazz);
        if (serializer == null) {
            throw new StorageException("Can't find serializer for class: " + clazz);
        }

        DurableObject entity = serializer.deserializeObject(id, buffer, tx);
        DurableObject found = objectIdentityMap.putIfAbsent(entity);
        return found == null ? entity : found;
    }

    class DurableObjectLoaderImpl implements DurableObjectLoader {

        @Override
        public DurableObject load(String id) {
            return loadDurableObject(id);
        }
    }

    class UnitOfWriteImpl implements UnitOfWrite {

        private Map<DurableObject, DurableState> changes = new HashMap<DurableObject, DurableState>();
        private Set<DurableObject> roots = new HashSet<DurableObject>();
        private UnitOfWorkState state = UnitOfWorkState.Active;

        @Override
        public DurableObject getOrCreate(String id, DurableObjectFactory factory) {
            if (id == null || factory == null) {
                throw new NullPointerException();
            }

            DurableObject entity = objectIdentityMap.get(id);

            if (entity != null) {
                return entity;
            }

            if (exists(id)) {
                return loadDurableObject(id);
            }

            //it is not in memory and it isn't isn't on disk. So lets create it.
            entity = factory.create();
            objectIdentityMap.putIfAbsent(entity);
            entity.setStorageId(id);
            //return entity;
            throw new TodoException();
        }

        @Override
        public void addRoot(DurableObject root) {
            switch (state) {
                case Active:
                    if (root == null) {
                        throw new NullPointerException();
                    }
                    roots.add(root);
                    break;
                case Aborted:
                    throw new IllegalStateException();
                case Committed:
                    throw new IllegalStateException();
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public UnitOfWorkState getState() {
            return state;
        }

        @Override
        public void addChange(DurableState change) {
            switch (state) {
                case Active:
                    if (change == null) {
                        throw new NullPointerException();
                    }

                    DurableState found = changes.get(change.getOwner());
                    if (found != null && found != change) {
                        throw new IllegalArgumentException();
                    }

                    changes.put(change.getOwner(), change);
                    break;
                case Aborted:
                    throw new IllegalStateException();
                case Committed:
                    throw new IllegalStateException();
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public void commit() {
            switch (state) {
                case Active:
                    for (DurableObject root : roots) {
                        rootIds.add(root.getStorageId());
                        StorageUtils.saveLines(rootsFile, new LinkedList<String>(rootIds));
                    }

                    for (DurableState change : filterDurables(this)) {
                        persist(change.getOwner(), change);
                    }
                    state = UnitOfWorkState.Committed;
                    break;
                case Aborted:
                    throw new IllegalStateException();
                case Committed:
                    throw new IllegalStateException();
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
