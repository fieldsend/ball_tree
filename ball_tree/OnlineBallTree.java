package ball_tree;

import java.util.PriorityQueue;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * OnlineBallTree is a generic implementation of the first online balltree algorithm 
 * set out in pages 11-14 of:
 * 
 * <p>Omohundro, Stephen M. 
 * Five balltree construction algorithms. 
 * Berkeley: International Computer Science Institute, 1989.</p> 
 *
 * It maintains a ball tree with the following properties:
 * 
 * <ul>
 * <li> Each interior node has two children. </li>
 * <li> Each interior node has a n-dimensional centre and a radius. </li>
 * <li> Each interior node's radius, is set such that all subsequant nodes in the subtree are contained in the ball defined by 
 * the parent. </li>
 * <li> Each leaf has a centre which denotes the location of the item stored in the leaf and a further generic type object 
 * associated with that location (does not accept null values) and has a radius of 0 (the ball is a point). </li>
 * <li> New locations are added to minimise the growth in total volume of the balls maintained in the tree </li>
 * <li> A BallTree storing M items will have M leaves. </li>
 * <li> The Euclidean distance is used</i>
 * </ul>
 * 
 * 
 * Due to precision issues, the implimentation is limited to double array locations with no more than 452 dimensions and will 
 * raise an exception if an attempt is made to construct it to hold location vectors will more elements than this (above this 
 * the volume of the unit n-ball is 0.0). Note that in practice you may find that using a number of dimensions a few smaller 
 * than this is still problematic, depending on the domain range of the locations and queries used.
 *
 * @author Jonathan Fieldsend
 * @version 1.0
 */
public class OnlineBallTree<T> implements Serializable
{
    private BallTreeNode root = null;
    /**
     * Number of dimensions 
     */
    public final int DIM;
    private int numItems = 0;
    private static final long serialVersionUID = 42L;
    /**
     * Creates an OnlineBallTree to store items associated with dim-dimensional locations.
     * 
     * @param dim number of dimensions the double arrays passed in for locations for that this OnlineBallTree will expect
     * @throws IllegalNumberOfDimensionsException if the number of dimensions is fewer than 1 or above 452
     */
    public OnlineBallTree(int dim) throws IllegalNumberOfDimensionsException {
        if (dim < 1)
            throw new IllegalNumberOfDimensionsException("The OnlineBallTree should be constructed for a minimum of 1-dimensional inputs");   
        if (dim > 452)
            throw new IllegalNumberOfDimensionsException("The OnlineBallTree should be constructed for a maximum of 452-dimensional inputs, as precision limitations means the unit ball is calculated as having a volume of 0.0 above this number.");    
        this.DIM = dim;
    }

    /**
     * Returns the height (level) of this tree. Note that leaves do not contribute to the height in this calculation.
     * 
     * @returns the number of levels of this tree
     */
    public int height() {
        if (root == null)
            return -1;
        if (root.isLeaf())
            return 0;
        return Math.max(this.root.leftChild.height(1), this.root.rightChild.height(1));    
    }

    /**
     * Returns the number of items stored in this tree (equivalent to the number of leaves in this BallTree implementation).
     * 
     * @returns the number of items in the tree
     */
    public int size() {
        return numItems;
    }
    
    /**
     * Inserts the location and the associated generic type item into the tree. Retursn true if inserted, returnd false if not inserted (when the
     * location already existing in the tree)
     * 
     * @param location array holding the location of the item to add
     * @param cargo object to be stored associated with the location
     * @returns true when location and cargo inserted, false if not inserted due to the corresponding location already being used to store an item in the tree
     * @throws IllegalNumberOfDimensionsException if the number of elements of the argument does not match that of locations stored in this tree
     */
    public boolean insert(double[] location, T cargo) throws IllegalNumberOfDimensionsException, NullPointerException {
        if (cargo == null) 
            throw new NullPointerException("null values for the cargo item are not permitted");
        if (location.length != DIM)
            throw new IllegalNumberOfDimensionsException("This OnlineBallTree is for " + DIM + " dimensions, but the centre argument has " + location.length);
        BallTreeLeaf<T> newLeaf = new BallTreeLeaf<>( new Ball(location, 0.0) ,cargo);
        if (this.root == null){
            this.root = newLeaf;
            //System.out.println("Set Root: " + newLeaf);
        } else {
            BallTreeNode sibling = this.bestSibling(newLeaf);
            BallTreeNode newParent = new BallTreeNode( Ball.boundingBall(newLeaf.ball, sibling.ball) );
            if (newParent.ball.volume == 0.0) { // if volume of parent ball is 0.0, then it is because both children have the same location 
                return false;
            }
            // put parent in tree
            newParent.parent = sibling.parent;
            if (newParent.parent == null)
                root = newParent;
            else if (sibling.parent.leftChild == sibling)
                sibling.parent.leftChild = newParent;
            else 
                sibling.parent.rightChild = newParent;
            newParent.leftChild = sibling;
            newParent.rightChild = newLeaf;

            newLeaf.parent = newParent;
            sibling.parent = newParent;
            // update the volumes covered by parents up to the root due to the addition
            this.repairParents(newParent); 
        }
        numItems++;
        return true;
    }

    /**
     * Removes (and returns) the item stored at the corresponding location. Note, the tree construction does not allow duplicate locations -- so 
     * there is no issue of a potential one to many mapping from location to items.
     * 
     * @param location location to be removed and corresponding item retured. If the location does not exist in this tree, then null is returned.
     * @returns the item stored at this location.
     */
    public T remove(double[] location) throws IllegalNumberOfDimensionsException {
        BallTreeLeaf<T> storedLocation = this.nearestLeafQuery(location);
        if (storedLocation == null)
            return null; // empty tree
        if (Ball.squaredDist(storedLocation.ball.centre, location) > 0.0 )
            return null; // location does not exist in tree 
        T cargo = storedLocation.cargo; // get cargo associated with location to return 
        if (storedLocation.parent == null){
            // the root is a leaf, so removing will empty the tree
            root = null;
            numItems--;
            return cargo;
        }
        // at this point the location is valid, and the tree has more than one element, so the partent is valid, need to remove and rearrange the 
        // structure
        BallTreeNode newParent = storedLocation.parent; // the parent will eventually get slotted out into a leaf holding the sibling
        BallTreeNode sibling;
        if (newParent.leftChild == storedLocation)
            sibling = newParent.rightChild;
        else
            sibling = newParent.leftChild;
        sibling.parent = newParent.parent; // set the sibling's new parent to be the parent of the previous parent -- could be null
        if (newParent.parent == null) {
            root = sibling;
        } else if (newParent.parent.leftChild == newParent){
            newParent.parent.leftChild = sibling;
        } else {
            newParent.parent.rightChild = sibling;
        }
        this.shrinkBallsRecursively(newParent); // the parent no longer covers the removed location, so need to shrink the balls
        numItems--;
        return cargo;
    }
    
    /*
     * Goes up the tree adusting the ball of the parent to enclose the children
     */
    private void shrinkBallsRecursively(BallTreeNode parent){
        parent.ball = Ball.boundingBall(parent.leftChild.ball,parent.rightChild.ball);
        if (parent.parent != null)
            this.shrinkBallsRecursively(parent.parent);
        return;    
    }
    
    /*
     * Goes up the tree and ensures that the parents contain the updated node
     */
    private void repairParents(BallTreeNode node){
        if (node.parent == null)
            return; //reached root, nothing else to process
        if (node.parent.ball.encloses(node.ball))
            return; // parent contains the ball, which means all parents further up must also do, so can return
        else {
            // update centre and radius of parent to cover the new child node
            Ball tempBall = Ball.boundingBall(node.parent.ball, node.ball);
            node.parent.ball = tempBall; // replace ball with expanded version
            this.repairParents(node.parent); // now update its parent recursively. Maybe make a node method?
        }
    }
    
    /**
     * Returns the item whose location is closest to the query.
     * 
     * @param location query point
     * @returns the item stored at the location closest to the query point
     * @throws IllegalNumberOfDimensionsException if the number of elements of the argument does not match that of locations stored in this tree
     */
    public T nearestNeighbourQuery(double[] location) throws IllegalNumberOfDimensionsException {
        BallTreeLeaf<T> queryResult = this.nearestLeafQuery(location);
        return queryResult == null ? null : queryResult.cargo;
    }
    
    private BallTreeLeaf<T> nearestLeafQuery(double[] location) throws IllegalNumberOfDimensionsException{
        if (location.length != this.DIM)
            throw new IllegalNumberOfDimensionsException("This OnlineBallTree is for " + this.DIM + " dimensions, but the query argument has " + location.length);
        
        // set up initial query Ball so that the centre is at the query location and the radius is such that the ball cmpletely encloses
        // the root ball of the tree
        if (root == null)
            return null;
        Ball query = new Ball(location, Math.sqrt(Ball.squaredDist(location,this.root.ball.centre)) + this.root.ball.radius);
        return (BallTreeLeaf<T>) this.nearestNeighbourSearch(query, root, null);
    }
    
    /**
     * Returns the k items whose locations are closest to the query. If this.{@link size}() < k, then it will only return this.{@link size}() items in the ArrayList.
     * 
     * @param location query point
     * @param k the number of nearest neighbours to obtain
     * @returns the items stored at the locations closest to the query point
     * @throws IllegalNumberOfDimensionsException if the number of elements of the argument does not match that of locations stored in this tree
     */
    public ArrayList<T> kNearestNeighbourQuery(double[] location, int k) throws IllegalNumberOfDimensionsException {
        if (location.length != this.DIM)
            throw new IllegalNumberOfDimensionsException("This OnlineBallTree is for " + this.DIM + " dimensions, but the query argument has " + location.length);
        
        // use priority queue. In java the head of the queue is the value with the least value
        // so want to order by negative of the distance, so head of the queue is worst value in queue.
        // additionally if the queue exceeds k, we simply poll the head to remove it.
        
        // set up initial query Ball so that the centre is at the query location and the radius is such that the ball cmpletely encloses
        // the root ball of the tree
        if (root == null)
            return null;
        Ball query = new Ball(location, Math.sqrt(Ball.squaredDist(location,this.root.ball.centre)) + this.root.ball.radius);
        PriorityQueue<QueuedLeaf<T>> kNNQueue = new PriorityQueue<>();
        this.kNearestNeighbourSearch(query, root, kNNQueue, k);
        
        ArrayList<T> result = new ArrayList<>(kNNQueue.size());
        for (QueuedLeaf<T> q : kNNQueue)
            result.add(q.leaf.cargo);
        return result;    
    }
    
    /*
     * Recursive method for k-nearest neighbour search
     */
    private void kNearestNeighbourSearch(Ball query, BallTreeNode processingNode, PriorityQueue<QueuedLeaf<T>> kNNQueue, int k) {
        if (processingNode.isLeaf()) {
            double distance = Math.sqrt(query.squaredDistanceToCentre(processingNode.ball));
            //System.out.println("dist: " + distance + ", query r: " + query.radius);
            if (kNNQueue.size() < k-1) { // fewer than k neighbours in queue so far, so query radius should not be reduced
                //System.out.println("returning leaf, queue not full");
                kNNQueue.offer(new QueuedLeaf<T>((BallTreeLeaf<T>)processingNode, distance));
            } else if (kNNQueue.size() == k-1){ // special casee where queue firs reaches k elements and need to update query radius
                //System.out.println("returning leaf, queue reaches capacity");
                kNNQueue.offer(new QueuedLeaf<T>((BallTreeLeaf<T>)processingNode, distance));
                query.radius = kNNQueue.peek().distance;
            } else { // now prority queue has k members, so only add if the new point has a distance value smaller than the head of the queue
                if (distance <= query.radius){
                    //System.out.println("returning leaf, as better than worst queue member");
                    query.radius = distance; // update best distance
                    kNNQueue.poll(); // remove head
                    kNNQueue.offer(new QueuedLeaf<T>((BallTreeLeaf<T>)processingNode, distance));
                    query.radius = kNNQueue.peek().distance; // update radius
                    return; 
                }
            }
        } else { // if at interior node
            double distLeft = processingNode.leftChild.ball.nearestDistanceToCentre(query);
            double distRight = processingNode.rightChild.ball.nearestDistanceToCentre(query);

            if ((distLeft > query.radius) && (distRight > query.radius)) {
                return; // current estimate of nearest neighbour unchanged
            }
            if (distLeft  < distRight) { // search nearer child first
                kNearestNeighbourSearch(query, processingNode.leftChild, kNNQueue, k);
                if (distRight < query.radius) { // check if worth searching
                    kNearestNeighbourSearch(query, processingNode.rightChild, kNNQueue, k);
                }
            } else {
                kNearestNeighbourSearch(query, processingNode.rightChild, kNNQueue, k);
                if (distLeft < query.radius) { // check if worth searching
                    kNearestNeighbourSearch(query, processingNode.leftChild, kNNQueue, k);
                }
            }
        }
        return;
    }
    
    /*
     * Recursive method for nearest neighbour search
     */
    private BallTreeNode nearestNeighbourSearch(Ball query, BallTreeNode processingNode, BallTreeNode nearestNeighbour) {
        if (processingNode.isLeaf()) {
            double distance = Math.sqrt(query.squaredDistanceToCentre(processingNode.ball));
            //System.out.println("dist: " + distance + ", query r: " + query.radius);
            if (distance <= query.radius){
                //System.out.println("returning leaf");
                query.radius = distance; // update best distance
                return processingNode; // return processing node as best so far
            }
        } else { // if at interior node
            double distLeft = processingNode.leftChild.ball.nearestDistanceToCentre(query);
            double distRight = processingNode.rightChild.ball.nearestDistanceToCentre(query);

            if ((distLeft > query.radius) && (distRight > query.radius)) {
                return nearestNeighbour; // currente stimate of nearest neighbour unchanged
            }
            if (distLeft  < distRight) { // search nearer child first
                nearestNeighbour = nearestNeighbourSearch(query, processingNode.leftChild, nearestNeighbour);
                if (distRight < query.radius) { // check if worth searching
                    nearestNeighbour = nearestNeighbourSearch(query, processingNode.rightChild, nearestNeighbour);
                }
            } else {
                nearestNeighbour = nearestNeighbourSearch(query, processingNode.rightChild, nearestNeighbour);
                if (distLeft < query.radius) { // check if worth searching
                    nearestNeighbour = nearestNeighbourSearch(query, processingNode.leftChild, nearestNeighbour);
                }
            }
        }
        return nearestNeighbour;
    }

    /*
     * Returns the best sibling for the new leaf argument
     */
    private BallTreeNode bestSibling(BallTreeNode newLeaf) {
        if (root == null) {
            return null;
        }
        //System.out.println("Inside bestSibling method: root " + this.root + ", new leaf: " + newLeaf);
        // initialise priority queue to store the fringe nodes as empty
        // The head of the queue is the least element of the ancestor expansion
        PriorityQueue<TestFringe> fringeNodePriorityQueue = new PriorityQueue<>();
        BallTreeNode result = this.root; // initially set result to root
        Ball testBall = Ball.boundingBall(this.root.ball, newLeaf.ball);
        TestFringe tf;
        double bestCost = testBall.volume;
        if (result.isLeaf() == false) {
            // create test fringe, set test fringe aexp = 0 as no ancestsors, set nd vol of test fringe to ballCost, set nd of test fringe to result
            tf = new TestFringe(0.0, bestCost, result);
            // insert tf to start of the priority queue frng
            fringeNodePriorityQueue.offer(tf);
        }
        while (fringeNodePriorityQueue.size() > 0){// priority queue is not empty
            // tf is popped off priority queue as the best candidate
            //System.out.println("In while loop");
            tf = fringeNodePriorityQueue.poll();
            if (tf.ancestorExpansion >= bestCost)
                break; 
            else {
                double expansion = tf.ancestorExpansion + tf.nodeVolume - tf.node.ball.volume;
                // process left node
                testBall = Ball.boundingBall(tf.node.leftChild.ball, newLeaf.ball);
                double volume = testBall.volume;
                if (volume + expansion < bestCost){
                    bestCost = volume + expansion;
                    result = tf.node.leftChild;
                }
                // add left tree to priority queue
                if (tf.node.leftChild.isLeaf() == false){
                    TestFringe tf2 = new TestFringe(expansion, volume,tf.node.leftChild);
                    fringeNodePriorityQueue.offer(tf2);
                }
                // process right node
                testBall = Ball.boundingBall(tf.node.rightChild.ball, newLeaf.ball);
                volume = testBall.volume;
                if (volume + expansion < bestCost){
                    bestCost = volume + expansion;
                    result = tf.node.rightChild;
                }
                if (tf.node.rightChild.isLeaf() == false){
                    TestFringe tf2 = new TestFringe(expansion, volume, tf.node.rightChild);
                    fringeNodePriorityQueue.offer(tf2);
                }
            }
        }
        return result;
    }
}
