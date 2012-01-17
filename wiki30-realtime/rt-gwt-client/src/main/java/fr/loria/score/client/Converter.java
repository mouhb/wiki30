package fr.loria.score.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import fr.loria.score.jupiter.tree.Tree;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * Utility class to convert back and forth from a GWT DOM to a custom tree like object model.
 * Adapting XWiki's DepthFirstPreOrderIterator to create the custom tree like model
 *
 * @author Bogdan.Flueras@inria.fr
 */
public class Converter {
    public static final Logger log = Logger.getLogger(Converter.class.getName());

    /**
     * Converts a native DOM element into a serializable, hierarchical object upon which OT functions work
     * @param nativeElement the native DOM element
     * @return a custom tree like object
     */
    public static Tree fromNativeToCustom(Element nativeElement) {
        log.finest("Native DOM element: " + nativeElement.toString());
        Tree root = new Tree();
        DepthFirstPreOrderIterator dfs = new DepthFirstPreOrderIterator(nativeElement, root);
        while (dfs.hasNext()) {
            dfs.next();
        }
        log.finest("Returning tree: " + root.toString());
        return root;
    }

    /**
     * Converts a custom hierarchical model into a native DOM node
     * @param tree the custom tree model
     * @return a native DOM node
     */
    public static Node fromCustomToNative(Tree tree) {
        log.finest("Custom tree element: " + tree);
        DepthFirstPreOrderIterator1 dfs1 = new DepthFirstPreOrderIterator1(tree);
        while(dfs1.hasNext()) {
            dfs1.next();
        }
	    Node result = dfs1.getNode();
        toString(result);
        return result;
    }

    public static void toString(Node node) {
        if (node != null) {
            log.info("Node:" + node.getNodeName() + ", " + node.getNodeValue() + ", type: " + node.getNodeType());
            for(int i = 0; i < node.getChildCount(); i++) {
                toString(node.getChild(i));
            }
        }
    }


    static class DepthFirstPreOrderIterator {
        /**
         * The current position of the iterator.
         */
        private Node currentNode;

        /**
         * The node where the iteration has started (the root of the subtree which we're iterating).
         */
        private Node startNode;

        /**
         * The node which is to be filled in from the currentNode
         */
        private Tree currentTree;

        /**
         * Creates an iterator for the subtree rooted in startNode.
         *
         * @param startNode root of the subtree to iterate through.
         * @param startTree root of the custom tree to create
         */
        DepthFirstPreOrderIterator(Node startNode, Tree startTree) {
            this.startNode = startNode;
            this.currentNode = startNode;

            this.currentTree = startTree;
            copyAttributes(currentNode, startTree);
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return this.currentNode != null;
        }

        /**
         * {@inheritDoc}
         */
        public Node next() {
            // return the currentNode
            Node nodeToReturn = this.currentNode;
            log.finest("Current node:" + currentNode);

            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            // compute the next node
            // try to go down
            if (currentNode.getFirstChild() != null) {
                this.currentNode = currentNode.getFirstChild();

                Tree child = new Tree();
                child.setParent(currentTree);
                currentTree.addChild(child);
                copyAttributes(currentNode, child);

                currentTree = child;
            } else {
                // try to go right: from this node or any of its ancestors, until we haven't reached the startNode
                Node ancestor = currentNode;
                while (ancestor != startNode) {
                    if (ancestor.getNextSibling() != null) {
                        this.currentNode = ancestor.getNextSibling();

                        Tree parent = currentTree.getParent();

                        Tree child = new Tree();

                        child.setParent(parent);
                        parent.addChild(child);
                        copyAttributes(currentNode, child);

                        currentTree = child;

                        break;
                    }
                    ancestor = ancestor.getParentNode();
                    currentTree = currentTree.getParent();
                }
                // if we got back to the root searching up, then we have no more options
                if (ancestor == startNode) {
                    this.currentNode = null;
                }
            }
            return nodeToReturn;
        }
    }


    static class DepthFirstPreOrderIterator1 {
        /**
         * The current position of the iterator.
         */
        private Node currentNode;

        /**
         * The node where the iteration has started (the root of the subtree which we're iterating).
         */
        private Tree startTree;

        /**
         * The node which is to be filled in from the currentNode
         */
        private Tree currentTree;

        /**
         * Creates an iterator for the subtree rooted in startTree.
         *
         * @param startTree root of the custom tree to create
         */
        DepthFirstPreOrderIterator1(Tree startTree) {
            this.startTree = startTree;
            this.currentTree = startTree;

            this.currentNode = createNativeNode(startTree);
        }

	public Node getNode() {
		return this.currentNode;
	}
        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return this.currentTree != null;
        }

        /**
         * {@inheritDoc}
         */
        public Tree next() {
            log.finest("Current tree:" + currentTree);

            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            // try to go down
            if (currentTree.getChild(0) != null) {
                currentTree = currentTree.getChild(0);

                Node child = createNativeNode(currentTree);
                currentNode.appendChild(child);
                currentNode = child;
            } else {
                // try to go right: from this node or any of its ancestors, until we haven't reached the startTree
                Tree ancestor = currentTree;
                while (ancestor != startTree) {
                    Tree nextSibling = ancestor.getNextSibling();
                    if (nextSibling != null) {
                        currentTree = nextSibling;

                        Node parent = currentNode.getParentElement();
                        Node child = createNativeNode(currentTree);
                        parent.appendChild(child);
                        currentNode = child;
                        break;
                    }
                    ancestor = ancestor.getParent();
                    currentNode = currentNode.getParentNode();
                }
                // if we got back to the root searching up, then we have no more options
                if (ancestor == startTree) {
                    this.currentTree = null;
                }
            }
            return currentTree;
        }
    }

    private static void copyAttributes(Node currentNode, Tree treeNode) {
        Map<String, String> attributes = new HashMap<String, String>();

        putIfValueNotNull(attributes, Tree.NODE_NAME, currentNode.getNodeName());
        putIfValueNotNull(attributes, Tree.NODE_VALUE, currentNode.getNodeValue());

        int type = currentNode.getNodeType();
        putIfValueNotNull(attributes, Tree.NODE_TYPE, String.valueOf(type));

        if (type == Node.ELEMENT_NODE) {
            Element element = (Element) currentNode;
//            putIfValueNotNull(attributes, "className", element.getClassName());
//            putIfValueNotNull(attributes, "id", element.getId());
//            putIfValueNotNull(attributes, "style", element.getStyle().toString());
            putIfValueNotNull(attributes, "tagName", element.getTagName().trim().toLowerCase());
//            putIfValueNotNull(attributes, "title", element.getTitle());
        } else if (type == Node.TEXT_NODE) {
            Text textElement = (Text) currentNode;
            treeNode.setValue(textElement.getData());
        }
        treeNode.setAttributes(attributes);
    }

    private static Node createNativeNode(Tree tree) {
        log.fine("Copying attributes from tree: " + tree);
        Node node = null;

        int nodeType = Integer.valueOf(tree.getAttribute(Tree.NODE_TYPE));
        if (nodeType == Node.ELEMENT_NODE) {
            Element element = Document.get().createElement(tree.getNodeName().trim().toLowerCase());
//            Map<String, String> attrs = tree.getAttributes();
//            for (Map.Entry<String, String> entry : attrs.entrySet()) {
//                element.setAttribute(entry.getKey(), entry.getValue());
//            }
            element.setNodeValue(tree.getValue());

            node = element;
        } else if (nodeType == Node.TEXT_NODE) {
            Text text = Document.get().createTextNode(tree.getValue());
            node = text;
        }
        log.fine("Node created is: " + node.toString());
        return node;
    }

    private static <K, V> void putIfValueNotNull(Map<K, V> map, K key, V value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}