package ball_tree;

import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
/**
 * OfflineBallTree is a generic implementation of the offline balltree algorithm 
 * set out in pages 8-9 of:
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
 * <li> A BallTree storing M items will have M leaves. </li>
 * <li> The Euclidean distance is used</i>
 * </ul>
 * 
 * 
 * Due to precision issues, the implimentation is limited to double array locations with no more than 452 dimensions and will 
 * raise an exception if an attempt is made to construct it to hold location vectors with more elements than this (above this 
 * the volume of the unit n-ball is 0.0). Note that in practice you may find that using a number of dimensions a few smaller 
 * than this is still problematic, depending on the domain range of the locations and queries used.
 *
 * @author Jonathan Fieldsend
 * @version 1.0
 */
public class OfflineBallTree<T> implements Serializable
{
    private BallTreeNode<T> root = null;
    /**
     * Number of dimensions 
     */
    public final int DIM;
    private final int NUM_ITEMS;
    private static final long serialVersionUID = 43L;
    
    /**
     * Creates an OnlineBallTree to store items associated with dim-dimensional locations.
     * 
     * @param dim number of dimensions the double arrays passed in for locations for that this OnlineBallTree will expect
     * @throws IllegalNumberOfDimensionsException if the number of dimensions is fewer than 1 or above 452
     */
    public OfflineBallTree(double[][] location, T[] cargo) throws IllegalNumberOfDimensionsException {
        if (location.length != cargo.length)
            throw new RuntimeException("array of locations must have the same number of elements as the array of cargo items for ball tree construction");
        this.DIM = location[0].length;
        this.NUM_ITEMS = cargo.length;
        this.buildTree(location,cargo);
    }

    private void buildTree(double[][] location, T cargo[]) {
        ArrayList<BallTreeLeaf<T>> ballArray = new ArrayList<>(cargo.length);
        for (int i = 0; i < location.length; i++)
            ballArray.add(new BallTreeLeaf<T>(new Ball(location[i], 0.0), cargo[i]));
        this.root = this.buildForRange(ballArray,0,location.length-1);    
    }
    
    /*
     * lowerIndex inclusive,upperIndex exclusive
     */
    private BallTreeNode<T> buildForRange(ArrayList<BallTreeLeaf<T>> ballArray, int lowerIndex, int upperIndex) {
        //System.out.println("buildforrange lb " + lowerIndex + ", ub " + upperIndex);
        if (lowerIndex == upperIndex)
            return ballArray.get(lowerIndex); // return the already build leaf
        else {
            int coordinateIndex = this.getMostSpreadDimension(ballArray,lowerIndex,upperIndex);
            int median = (lowerIndex + upperIndex)/2; // get middle of range to be partitioned
            //System.out.println("median " + median);
            this.selectOnCoordinate(ballArray, lowerIndex, upperIndex, median, coordinateIndex); // partion left and right parts 
            BallTreeNode<T> node = new BallTreeNode<>();
            node.leftChild = this.buildForRange(ballArray,lowerIndex,median);
            node.leftChild.parent = node;
            node.rightChild = this.buildForRange(ballArray,median+1,upperIndex);
            node.rightChild.parent = node;
            node.ball = Ball.boundingBall(node.leftChild.ball,node.rightChild.ball); // set node to contain both children
            return node;
        }
    }
    
    private int getMostSpreadDimension(ArrayList<BallTreeLeaf<T>> ballArray, int lowerBound, int upperBound) {
        int dim = 0;
        double[] max = new double[this.DIM];
        double[] min = new double[this.DIM];
        for (int i = 0; i < this.DIM; i++) {
            max[i] =  ballArray.get(lowerBound).ball.centre[i];
            min[i] =  ballArray.get(lowerBound).ball.centre[i];
        }
        //get ranges
        for (int j=lowerBound+1; j <= upperBound; j++) {
            for (int i = 0; i< this.DIM; i++) {
                Ball ball = ballArray.get(j).ball;
                if (max[i] < ball.centre[i]) {
                    max[i] = ball.centre[i];
                } else if (min[i] > ball.centre[i]) {
                    min[i] = ball.centre[i];
                }
            }
        }
        // identofy most spread
        for (int i = 1; i< this.DIM; i++) 
            if ((max[i]-min[i]) > (max[dim]-min[dim]))
                dim = i;
        return dim;
    }
    
    private void selectOnCoordinate(ArrayList<BallTreeLeaf<T>> ballArray, int lowerIndex, int upperIndex, int k, int partitionIndex) {
        int lb = lowerIndex;
        int ub = upperIndex;
        while (lb < ub) {
            int pivotIndex = ThreadLocalRandom.current().nextInt(lb, ub+1); // includes returning ub
            Collections.swap(ballArray, pivotIndex, lb);// swap elements at pivotIndex and lb
            int m = lb;
            for (int i = lb+1; i<=ub; i++) {
                if (ballArray.get(i).ball.centre[partitionIndex] < ballArray.get(lb).ball.centre[partitionIndex]) {
                    m++;
                    Collections.swap(ballArray, m, i);// swap elements at m and i
                }
            }
            Collections.swap(ballArray, m, lb);// swap elements at lb and m
            // all values from lb to m are now smaller than the item in element m on coordinate "partitionIndex"
            if (m <= k) // processed up to a value lower than k, 
                lb = m+1;
            if (m >= k) // processed p to a value higher than
                ub = m-1;
        }
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
        return this.NUM_ITEMS;
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
    
    /*
     * Method does various legality checking and the leaf whose location is closest to the query.
     */
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
     * Returns the radius of the (minimum) ball containing the k items whose locations are closest to the query. 
     * If this.{@link size}() < k, then it will only return the radius for this.{@link size}() items. 
     * Will return 0.0 if this tree is empty.
     * 
     * @param location query point
     * @param k the number of nearest neighbours to obtain
     * @returns the radius of the ball containing the items stored at the locations closest to the query point
     * @throws IllegalNumberOfDimensionsException if the number of elements of the argument does not match that of locations stored in this tree
     */
    public double kNearestNeighbourBallRadiusQuery(double[] location, int k) throws IllegalNumberOfDimensionsException {
        // use priority queue. In java the head of the queue is the value with the least value
        // so want to order by negative of the distance, so head of the queue is worst value in queue.
        // additionally if the queue exceeds k, we simply poll the head to remove it.
        
        // set up initial query Ball so that the centre is at the query location and the radius is such that the ball completely encloses
        // the root ball of the tree
        if (root == null)
            return 0.0;
        Ball query = new Ball(location, Math.sqrt(Ball.squaredDist(location,this.root.ball.centre)) + this.root.ball.radius);
        PriorityQueue<QueuedLeaf<T>> kNNQueue = new PriorityQueue<>();
        this.kNearestNeighbourSearch(query, root, kNNQueue, k);
        QueuedLeaf<T> mostDistant = kNNQueue.poll();
        
        return mostDistant.distance;    
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
        
        // set up initial query Ball so that the centre is at the query location and the radius is such that the ball completely encloses
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
    
    /**
     * Returns the items whose locations are within the distance queryRadius to the query.
     * 
     * @param location query point
     * @param queryRadius the ball radius around the location whose points to return
     * @returns the items stored at the locations within the ball
     */
    public ArrayList<T> getAllNeighboursInRadius(double[] location, double queryRadius) {
        if (root == null)
            return null;
        Ball query = new Ball(location, queryRadius);
        ArrayList<T> result = new ArrayList<>();
        this.radiusNeighbourSearch(query, root, result);
        return result;    
    }
    
    /*
     * Recursive method for k-nearest neighbour search
     */
    private void kNearestNeighbourSearch(Ball query, BallTreeNode<T> processingNode, PriorityQueue<QueuedLeaf<T>> kNNQueue, int k) {
        if (processingNode instanceof BallTreeLeaf<?>) {//(processingNode.isLeaf()) {
            double distance = Math.sqrt(query.squaredDistanceToCentre(processingNode.ball));
            if (kNNQueue.size() < k-1) { // fewer than k neighbours in queue so far, so query radius should not be reduced
                kNNQueue.offer(new QueuedLeaf<T>((BallTreeLeaf<T>)processingNode, distance));
            } else if (kNNQueue.size() == k-1){ // special casee where queue first reaches k elements and need to update query radius
                kNNQueue.offer(new QueuedLeaf<T>((BallTreeLeaf<T>)processingNode, distance));
                query.radius = kNNQueue.peek().distance;
            } else { // now prority queue has k members, so only add if the new point has a distance value smaller than the head of the queue
                if (distance <= query.radius){
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
     * Recursive method for getting neighbours within radius
     */
    private void radiusNeighbourSearch(Ball query, BallTreeNode<T> processingNode, ArrayList<T> listOfItemsWithinRadius) {
        if (processingNode.isLeaf()) {
            double distance = Math.sqrt(query.squaredDistanceToCentre(processingNode.ball));
            if (distance <= query.radius){ // location is within query ball, so store cargo to return
                 listOfItemsWithinRadius.add(((BallTreeLeaf<T>)processingNode).cargo);
                 return; 
            }
            
        } else { // if at interior node, check if we need to go down
            double distLeft = processingNode.leftChild.ball.nearestDistanceToCentre(query);
            double distRight = processingNode.rightChild.ball.nearestDistanceToCentre(query);

            if (distLeft <= query.radius)
                radiusNeighbourSearch(query, processingNode.leftChild, listOfItemsWithinRadius);
            if (distRight <= query.radius)
                radiusNeighbourSearch(query, processingNode.rightChild, listOfItemsWithinRadius);
        }
        return;
    }
    
    /*
     * Recursive method for nearest neighbour search
     */
    private BallTreeNode<T> nearestNeighbourSearch(Ball query, BallTreeNode<T> processingNode, BallTreeNode<T> nearestNeighbour) {
        if (processingNode.isLeaf()) {
            double distance = Math.sqrt(query.squaredDistanceToCentre(processingNode.ball));
            if (distance <= query.radius){
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
}