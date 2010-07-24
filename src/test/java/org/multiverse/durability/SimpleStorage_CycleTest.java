package org.multiverse.durability;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.durability.account.SerializeUtils;

import java.util.*;

import static org.junit.Assert.*;

public class SimpleStorage_CycleTest {
    private SimpleStorage storage;

    @Before
    public void setUp() {
        storage = new SimpleStorage();
        storage.clear();
        storage.register(Node.class, new NodeSerializer());
    }

    @Test
    public void testNoCycle() {
        Node node = new Node();
        NodeState nodeState = new NodeState(node);

        UnitOfWork unitOfWork = storage.startUnitOfWork();
        unitOfWork.addRoot(node);
        unitOfWork.addChange(nodeState);
        unitOfWork.commit();

        NodeState state = (NodeState) storage.loadState(node.getStorageId());
        assertNotNull(state);
        assertSame(node, state.getOwner());
        assertNull(state.ref);
    }

    @Test
    public void testSelfCycle() {
        Node node = new Node();

        NodeState nodeState = new NodeState(node);
        nodeState.ref = node;

        UnitOfWork unitOfWork = storage.startUnitOfWork();
        unitOfWork.addRoot(node);
        unitOfWork.addChange(nodeState);
        unitOfWork.commit();

        NodeState found = (NodeState) storage.loadState(node.getStorageId());
        assertNotNull(found);
        assertSame(node, found.getOwner());
        assertSame(node, found.ref);
    }

    @Test
    public void testIndirectCycle(){
        Node root = new Node();

        Node child = new Node();
        NodeState childState = new NodeState(child);
        childState.ref = root;

        NodeState rootState = new NodeState(root);
        rootState.ref = child;

        UnitOfWork unitOfWork = storage.startUnitOfWork();
        unitOfWork.addChange(rootState);
        unitOfWork.addChange(childState);
        unitOfWork.addRoot(root);
        unitOfWork.commit();

        NodeState foundRootState = (NodeState)storage.loadState(root.getStorageId());
        assertNotNull(foundRootState);
        assertSame(root, foundRootState.getOwner());
        assertSame(child, rootState.ref);

        NodeState foundChildState = (NodeState)storage.loadState(child.getStorageId());
        assertNotNull(foundChildState);
        assertSame(child, foundChildState.getOwner());
        assertSame(root, foundChildState.ref);
    }

    class NodeSerializer implements DurableObjectSerializer<Node, NodeState> {

        @Override
        public byte[] serialize(NodeState state) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("ref", state.ref == null ? "null" : state.ref.getStorageId());
            return SerializeUtils.serialize(map);
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
        public Node deserializeObject(String id, byte[] content) {
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
