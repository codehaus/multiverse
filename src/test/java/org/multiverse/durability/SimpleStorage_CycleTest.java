package org.multiverse.durability;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.durability.account.SerializeUtils;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.*;

public class SimpleStorage_CycleTest {
    private SimpleStorage storage;

    @Before
    public void setUp() {
        storage = new SimpleStorage(new BetaStm());
        storage.clear();
        storage.register(Node.class, new NodeSerializer());
    }

    @Test
    public void testNoCycle() {
        Node node = new Node();
        NodeState nodeState = new NodeState(node);

        UnitOfWrite unitOfWrite = storage.startUnitOfWrite();
        unitOfWrite.addRoot(node);
        unitOfWrite.addChange(nodeState);
        unitOfWrite.commit();

        //NodeState state = (NodeState) storage.loadState(node.getStorageId());
        //assertNotNull(state);
        //assertSame(node, state.getOwner());
        //assertNull(state.ref);
    }

    @Test
    public void testSelfCycle() {
        Node node = new Node();

        NodeState nodeState = new NodeState(node);
        nodeState.ref = node;

        UnitOfWrite unitOfWrite = storage.startUnitOfWrite();
        unitOfWrite.addRoot(node);
        unitOfWrite.addChange(nodeState);
        unitOfWrite.commit();

        //NodeState found = (NodeState) storage.loadState(node.getStorageId());
        //assertNotNull(found);
        //assertSame(node, found.getOwner());
        //assertSame(node, found.ref);
    }

    @Test
    public void testIndirectCycle(){
        Node root = new Node();

        Node child = new Node();
        NodeState childState = new NodeState(child);
        childState.ref = root;

        NodeState rootState = new NodeState(root);
        rootState.ref = child;

        UnitOfWrite unitOfWrite = storage.startUnitOfWrite();
        unitOfWrite.addChange(rootState);
        unitOfWrite.addChange(childState);
        unitOfWrite.addRoot(root);
        unitOfWrite.commit();

        //NodeState foundRootState = (NodeState)storage.loadState(root.getStorageId());
        //assertNotNull(foundRootState);
       // assertSame(root, foundRootState.getOwner());
      //  assertSame(child, rootState.ref);

      //  NodeState foundChildState = (NodeState)storage.loadState(child.getStorageId());
      //  assertNotNull(foundChildState);
      //  assertSame(child, foundChildState.getOwner());
      //  assertSame(root, foundChildState.ref);
    }

    class NodeSerializer implements DurableObjectSerializer<Node, NodeState> {

        @Override
        public byte[] serialize(NodeState state) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("ref", state.ref == null ? "null" : state.ref.getStorageId());
            return SerializeUtils.serialize(map);
        }

        @Override
        public void populate(NodeState state, byte[] content, DurableObjectLoader loader) {
            Map<String, String> map = SerializeUtils.deserializeMap(content);

            String refId = map.get("ref");

            if (!"null".equals(refId)) {
                state.ref = (Node) loader.load(refId);
            }            
        }

        @Override
        public NodeState deserializeState(Node owner, byte[] content, DurableObjectLoader loader) {
            Map<String, String> map = SerializeUtils.deserializeMap(content);
            NodeState state = new NodeState(owner);
            String refId = map.get("ref");

            if (!"null".equals(refId)) {
                state.ref = (Node) loader.load(refId);
            }

            return state;
        }

        @Override
        public Node deserializeObject(String id, byte[] content, BetaTransaction transaction) {
            Node node = new Node();
            node.setStorageId(id);
            return node;
        }

    }

    class Node implements DurableObject {

        public String storageId = UUID.randomUUID().toString();

        @Override
        public String getStorageId() {
            return storageId;
        }

        @Override
        public void setStorageId(String id) {
            this.storageId = id;
        }

        @Override
        public void markAsDurable() {
            throw new TodoException();
        }

        @Override
        public boolean isDurable() {
            throw new TodoException();
        }
    }

    class NodeState implements DurableState {
        private Node owner;
        public Node ref;

        private NodeState(Node owner) {
            this.owner = owner;
        }

        @Override
        public Iterator<DurableObject> getReferences() {
            List<DurableObject> result = new LinkedList<DurableObject>();

            if (ref != null) {
                result.add(ref);
            }
            return result.iterator();
        }

        @Override
        public Node getOwner() {
            return owner;
        }
    }
}
