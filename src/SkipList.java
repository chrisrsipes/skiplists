import java.util.ArrayList;
import java.util.Random;


public class SkipList<T extends Comparable<T>> {

    private Node<T> head;
    private int customHeight;
    private int maxHeight;
    private int size;

    // constructor for when default height is not provided
    public SkipList() {
        this.size = 0;
        this.customHeight = -1;
        this.maxHeight = calculateSkipListHeight(0);
        this.head = new Node<T>(this.maxHeight);
    }

    // constructor for when default height is provided
    public SkipList(int height) {
        if (height < 1) {
            height = 1;
        }

        this.size = 0;
        this.customHeight = height;
        this.maxHeight = height;
        this.head = new Node<T>(this.maxHeight);
    }

    public int size() {
        return this.size;
    }

    public int height() {
        return this.maxHeight;
    }

    public int customHeight() {
        return this.customHeight;
    }

    public Node<T> head() {
        return this.head;
    }

    public void insert(T data) {
        insert(data, generateRandomHeight());
    }

    public void insert(T data, int height) {
        Node<T> newNode;

        newNode = new Node<T>(data, height);
        ArrayList<Node<T>> dropNodes = findDropNodes(data, height);

        // set the pointers on the new nodes and those that are affected by its height
        // physical insertion
        for (int i = 0; i < dropNodes.size(); i++) {
            if (dropNodes.get(i) != null) {
                newNode.setNext(i, dropNodes.get(i).next(i));
                dropNodes.get(i).setNext(i, newNode);
            }
        }

        this.size++;
        attemptHeightChange(OperationType.INSERT);
    }



    public void delete(T data) {
        Node <T> deletedNode;
        boolean performedDeletion = false;

        ArrayList<Node<T>> dropNodes = findDropNodes(data, this.height());

        // need to find out how many levels we actually dropped during the traversal
        int nonNullCount = countNonNull(dropNodes);
        nonNullCount = nonNullCount < 1 ? 1 : nonNullCount;
        deletedNode = dropNodes.get(nonNullCount - 1).next(0);

        // traverses along the level to find the node to delete
        while (deletedNode != null && deletedNode.value() != null && deletedNode.value().compareTo(data) != 0) {
            deletedNode = deletedNode.next(0);
        }

        // if we found the node we're looking for, delete it and update the pointers
        if (deletedNode != null && deletedNode.value().compareTo(data) == 0) {
            for (int i = 0; i < deletedNode.height(); i++) {
                // is either head node or valid node
                if (dropNodes.get(i) != null && (i == 0 || dropNodes.get(i).value() != null)) {
                    dropNodes.get(i).setNext(i, deletedNode.next(i));
                    performedDeletion = true;
                }
            }
        }

        // if we performed a deletion, update the size and potentiall the node heights
        if (performedDeletion) {
            this.size--;
            attemptHeightChange(OperationType.DELETE);
        }

    }

    // checks to see if the skiplist contains T data
    public boolean contains(T data) {
        int searchLevel = this.height() - 1;
        Node<T> curr, next;

        curr = this.head();
        next = curr.next(searchLevel);

        while (searchLevel >= 0) {
            while (next != null && next.value().compareTo(data) < 0) {
                curr = next;
                next = next.next(searchLevel);
            }

            searchLevel--;
            next = curr.next(searchLevel);
        }

        return curr.next(0).value() != null && curr.next(0).value().compareTo(data) == 0;
    }

    public static double difficultyRating() {
        return 2;
    }

    public static double hoursSpent() {
        return 3;
    }

    // will attempt a height change based on the operation
    // and the direction in which the height changed.
    // (insert, increase) -> increase height
    // (delete, decrease) -> decrease height
    private void attemptHeightChange(OperationType op) {
        int newHeight = this.calculateSkipListHeight(this.size());
        Node<T> curr, next;

        if (op.equals(OperationType.INSERT) && newHeight > this.height()) {
            // we should increase the height of the skiplist, and with 50% change increase the height of
            // each of the nodes with maximum height
            curr = this.head();
            curr.grow();
            curr = curr.next(this.height());

            while (curr != null) {
                curr.maybeGrow();
                curr = curr.next(this.height());
            }

            this.maxHeight = newHeight;
        }
        else if (op.equals(OperationType.DELETE) && newHeight< this.height()) {
            // need to trim the height of each node with maximum height
            curr = this.head();

            while (curr != null) {
                curr.trim(newHeight);
                curr = curr.next(newHeight - 1);
            }

            this.maxHeight = newHeight;
        }


    }

    // this will provide a path of nodes to jump from, where the index represents
    // the level that node was used to skip on
    private ArrayList<Node<T>> findDropNodes(T data, int maxHeight) {
        int searchLevel = this.height() - 1;
        Node<T> curr = this.head();
        Node<T> next = curr.next(searchLevel);
        ArrayList<Node<T>> dropNodes = new ArrayList<Node<T>>(this.height());

        // head will always be first.  In the case that we cant
        // skip around at all, then we are only traversing along
        // height = 1, and therefore dropNotes[0] correctly corresponds with
        // taking the path along height = 1
        dropNodes.add(this.head());

        // make room for the rest of the dropNodes
        for (int i = 1; i < this.height(); i++) {
            dropNodes.add(null);
        }

        // while we can still drop any levels
        while (searchLevel >= 0) {
            // go the farthest along the level before having to drop
            while (next != null && next.value().compareTo(data) < 0) {
                curr = next;
                next = next.next(searchLevel);
            }

            // add the node were dropping on to the list
            if (searchLevel < maxHeight && curr != null) {
                dropNodes.set(searchLevel, curr);
            }

            // drop a level
            searchLevel--;
            next = curr.next(searchLevel);
        }

        return dropNodes;
    }

    private int countNonNull(ArrayList<Node<T>> nodes) {
        int i, count = 0;

        for (i = 0; i < nodes.size(); i++) {
            count += (nodes.get(i) != null && nodes.get(i).value() != null) ? 1 : 0;
        }

        return count;
    }


    // generates a random height to be assigned to a node based on the max height
    // the list will support
    private int generateRandomHeight() {
        int height = 1, maxHeight = this.height();
        Random random = new Random();

        while (height <= maxHeight && random.nextBoolean()) {
            height++;
        }

        return height;
    }

    // calculates the maximum height the skip list will support
    private int calculateSkipListHeight(int size) {
        int calculatedHeight;

        if (size == 0) {
            calculatedHeight =  1;
        }
        else if (size == 1) {
            calculatedHeight =  1;
        }
        else {
            calculatedHeight = (int)Math.ceil(Math.log(this.size) / Math.log(2));
        }

        return calculatedHeight;
    }


}

class Node<T> {
    int height;
    T data;
    private ArrayList<Node<T>> next;

    public Node(int height) {
        // initialize instance variables
        this.data = null;
        this.height = height;
        this.next = new ArrayList<Node<T>>(height);

        for (int i = 0; i < height; i++) {
            this.next.add(null);
        }
    }

    public Node(T data, int height) {
        this(height);

        this.data = data;
    }

    public T value() {
        return this.data;
    }

    public int height() {
        return height;
    }

    public Node<T> next(int level) {
        if (level >= 0 && level < this.height) {
            return this.next.get(level);
        }
        else {
            return null;
        }
    }


    // Set the next reference at the given level within this node to node.
    public void setNext(int level, Node<T> node) {
        if (level >= 0 && level < this.height) {
            this.next.set(level, node);
        }
    }

    // Grow this node by exactly one level.
    // (I.e., add a null reference to the top of its tower of next references).
    // This is useful for forcing the skip list’s head node to grow when
    // inserting into the skip list causes the list’s maximum height to increase.
    public void grow() {
        this.next.add(this.height, null);
        this.height++;
    }

    // Grow this node by exactly one level with a
    // probability of 50%. (I.e., add a null
    // reference to the top of its tower of next references).
    // This is useful for when inserting into the skip list causes the
    // list’s maximum height to increase.
    public void maybeGrow() {
        Random random = new Random();

        if (random.nextBoolean()) {
            this.grow();
        }

    }

    // Remove references from the top of this
    // node’s tower of next references until this node’s height
    // has been reduced to the value given in the height parameter.
    // This is useful for when deleting from the skip list causes the
    // list’s maximum height to decrease.
    public void trim(int height) {
        if (height < this.height && height >= 0) {
            for (int i = this.height - 1; i >= height; i--) {
                this.next.remove(i);
            }

            this.height = height;
        }
    }
}

enum OperationType {
    INSERT, DELETE
}
