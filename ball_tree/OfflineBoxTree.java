package ball_tree;

import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.util.Precision;

/**
 * OfflineBoxTree is a implementation of the offline balltree algorithm 
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
 * <li> Each interior node has a n-dimensional centre and a distance (step) from the centre to the box edge for each 
 * dimension (so cube length is 2 * step for all dimensions). </li>
 * <li> Each interior node's steop, is set such that all subsequant nodes in the subtree are contained in the box defined by 
 * the parent. </li>
 * <li> Each leaf has a centre which denotes the location of the item stored in the leaf and a further Integer object 
 * associated with that location (does not accept null values) and has a radius of 0 (the box is a point). </li>
 * <li> A BoxTree storing M items will have M leaves. </li>
 * <li> The Chebyshev Distance is used</i>
 * </ul>
 * 
 * 
 * @author Jonathan Fieldsend
 * @version 1.0
 */
public class OfflineBoxTree implements Serializable
{
    private BoxTreeNode root = null;
    /**
     * Number of dimensions 
     */
    public final int DIM;
    private final int NUM_ITEMS;
    private static final long serialVersionUID = 44L;

    
    static class BoxQuery {
        double[] centre;
        double step;
        BoxQuery(double[] centre, double step) {
            this.centre = centre;
            this.step = step;
        }
    }
    
    static class Box {
        double[] upper;
        double[] lower;
        static final double EPS = Precision.EPSILON*10.0;
        //double volume;
        private static final long serialVersionUID = 43L;
        
        Box(double[] upper, double[] lower) {
            this.upper = upper;
            this.lower = lower;
            //this.volume = (step > 0.0) ? calculateHypervolume() : 0.0;
        }
        
        //creates point box
        Box(double[] point) {
            this.upper = point;
            this.lower = point;
        }
        
        /**
         * Creates a box whose state copies the argument ball.
         * 
         * @param a box whose state to copy in constructing this ball 
         */
        Box(Box a) {
            //shallow copy
            this.upper = a.upper;
            this.lower = a.upper;
        }

        double getCentre(int i) {
            return (this.upper[i] + this.lower[i])/2.0;
        }
        
        @Override
        public String toString() {
            String s ="\nUpper: ";
            for (int i=0; i<this.upper.length; i++)
                s += upper[i] + ",";
            s += "\nLower: ";
            for (int i=0; i<this.lower.length; i++)
                s += lower[i] + ",";
            s += "\n";
            return s;
        }
        
        /** 
         * Returns the distance from the centre of the query Box to the surface of the hypercube of this box
         * which is the closest possible for any point contained in this Box to the centre of the query. Will return 
         * a negative value if the query centre lies within this Box.
         * 
         * @param query ball to compare to centre of
         */
        double nearestDistanceToCentre(BoxQuery query){
            //return nearestDistanceToCentre(query.centre); // closest distance from box to query
            double d = 0.0;
            for (int i = 0; i< query.centre.length; i++) {
                if (query.centre[i] < this.lower[i]) d = Math.max(d, this.lower[i]-query.centre[i]);
                else if (query.centre[i] > this.upper[i]) d = Math.max(d, query.centre[i]-this.upper[i]);
            }
            return d;
        }

        double nearestDistanceToCentre(double[] point){
            double[] closestPoint = clamp(point); // get closest point on surface of box to the query
            return Box.chebyshevDistance(closestPoint,point); // closest distance from box to query
        }
        
        double[] clamp(double[] query) {
            double[] closestPoint = new double[query.length];
            for (int i = 0; i< query.length; i++) {
                if (query[i] < this.lower[i] )
                    closestPoint[i] = this.lower[i];
                else if (query[i] > this.upper[i] )
                    closestPoint[i] = this.upper[i];
                else
                    closestPoint[i] = query[i];
            }
            return closestPoint;
        }

        static double chebyshevDistance(double[] closestPoint, double[] query) {
            double maxDist = Math.abs(closestPoint[0]-query[0]);
            for (int i=1; i<query.length; i++) {
                double d =  Math.abs(closestPoint[i]-query[i]);
                if (d > maxDist) maxDist = d;
            }
            return maxDist;
        }

        /**
         * Returns true if this Box encloses the argument Box, otherwise returns false.
         * 
         * @param box argument to check if enclosed
         * @returns true if this encloses the argument, otherwise false 
         */
        boolean encloses(Box box) {
            for (int i = 0; i<this.upper.length; i++){
                if (box.upper[i] > this.upper[i]){
                    return false;
                }
                if (box.lower[i] < this.lower[i])  { 
                    return false;
                }
            }
            return true;
        }

        /*
         * Returns the square distance between the centre of this box and the centre of the argument box.
         * 
         * @param a box to compare to
         * @return chebyshev distance between centres
         */
        /*double chebyshevDistanceToCentre(Box a) {
            return Box.chebyshevDistance(this.centre,a.centre);
        }*/

        /** 
         * Returns true if the argument lies within this box.
         * 
         * @param point location to check if inside this box
         * @returns true if point is within the ball, flase otherwise
         */
        boolean contains(double [] point) {
            for (int i = 0; i<this.upper.length; i++){
                if (this.upper[i] < point[i]){
                    return false;
                }
                if (this.lower[i] > point[i])  { 
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns a new box instance whose step is set such that is is the smallest volume box which encloses both arguments.
         * 
         * 
         * @param a box to be enclosed
         * @param b box to be enclosed
         * @returns minimal enclosing box
         */
        static Box boundingBox(Box a, Box b) {
            if (a.encloses(b))
                return new Box(a);
            if (b.encloses(a))
                return new Box(b);
            double[] upperLimit = new double[a.upper.length];
            double[] lowerLimit = new double[a.upper.length];
            for (int i = 0; i < upperLimit.length; i++){
                upperLimit[i] = Math.max(a.upper[i],b.upper[i]);
                lowerLimit[i] = Math.min(a.lower[i],b.lower[i]);
            }
            return new Box(upperLimit, lowerLimit);
        }
    }

    static class BoxTreeNode {
        BoxTreeNode rightChild = null;
        BoxTreeNode leftChild = null;
        BoxTreeNode parent = null;
        Box box;

        private static final long serialVersionUID = 42L;
        /**
         * Create an instance of the BoxTreeNode with no state set 
         */
        BoxTreeNode() {}

        /**
         * Creates an instance of the BoxTreeNode containing the box argument, but which is not yet attached to a tree (no parent or children)
         */
        BoxTreeNode(Box box) {
            this.box = box;
        }

        
        boolean sanityCheck() {
            if (this.isLeaf()) {
                return true;
            }
            if (!this.box.encloses(this.rightChild.box)) {
                System.out.println("right child not contained");
                System.out.println("This Box");
                System.out.println(this.box);
                System.out.println("Child Box");
                System.out.println(this.rightChild.box);
                return false;
            }
            if (!box.encloses(this.leftChild.box)) {
                System.out.println("left child not contained");
                System.out.println(this.box);
                System.out.println(this.leftChild.box);
                return false;    
            }
            if (!rightChild.sanityCheck())
                return false;
            if (!leftChild.sanityCheck())
                return false;
            return true;
        }
        /**
         * Interior nodes are not leaves, so return false
         * 
         * @returns whether this node is a leaf
         */
        boolean isLeaf() {
            return false;
        }

        /**
         * Returns the height of this subtree plus 1
         * 
         * @returns hieght
         */ 
        int height(int i) {
            if (this.isLeaf())
                return i;
            else
                return Math.max(this.leftChild.height(i+1), this.rightChild.height(i+1));
        }
    }

    static class BoxTreeLeaf extends BoxTreeNode {
        Integer cargo;

        private static final long serialVersionUID = 42L;
        /**
         * Stores the ball argument, and as this is a leaf also stores the cargo item
         * 
         * @param ball ball (point) to be stored
         * @param cargo item associated with the corresponding ball (point) to be stored at the leaf
         */
        BoxTreeLeaf(Box box, Integer cargo){
            super(box);
            this.cargo = cargo;
        }

        /**
         * The BoxTreeLeaf is a leaf, so returns true
         * 
         * @returns true if this is a leaf
         */
        @Override
        boolean isLeaf() {
            return true;
        }
    }

    /**
     * Creates an OnlineBallTree to store items associated with dim-dimensional locations.
     * 
     * @param dim number of dimensions the double arrays passed in for locations for that this OnlineBallTree will expect
     * @throws IllegalNumberOfDimensionsException if the number of dimensions is fewer than 1 or above 452
     */
    public OfflineBoxTree(double[][] location, int[] cargo) throws IllegalNumberOfDimensionsException {
        if (location.length != cargo.length)
            throw new RuntimeException("array of locations must have the same number of elements as the array of cargo items for ball tree construction");
        this.DIM = location[0].length;
        this.NUM_ITEMS = cargo.length;
        this.buildTree(location,cargo);
    }

    private void buildTree(double[][] location, int cargo[]) {
        ArrayList<BoxTreeLeaf> boxArray = new ArrayList<>(cargo.length);
        for (int i = 0; i < location.length; i++)
            boxArray.add(new BoxTreeLeaf(new Box(location[i]), cargo[i]));
        this.root = this.buildForRange(boxArray,0,location.length-1);    
    }

    /*
     * lowerIndex inclusive,upperIndex exclusive
     */
    private BoxTreeNode buildForRange(ArrayList<BoxTreeLeaf> boxArray, int lowerIndex, int upperIndex) {
        //System.out.println("buildforrange lb " + lowerIndex + ", ub " + upperIndex);
        if (lowerIndex == upperIndex) {
            //System.out.println(boxArray.get(lowerIndex).box.upper);
            return boxArray.get(lowerIndex); // return the already build leaf
        } else {
            int coordinateIndex = this.getMostSpreadDimension(boxArray,lowerIndex,upperIndex);
            int median = (lowerIndex + upperIndex)/2; // get middle of range to be partitioned
            //System.out.println("median " + median);
            this.selectOnCoordinate(boxArray, lowerIndex, upperIndex, median, coordinateIndex); // partion left and right parts 
            BoxTreeNode node = new BoxTreeNode();
            node.leftChild = this.buildForRange(boxArray,lowerIndex,median);
            node.leftChild.parent = node;
            node.rightChild = this.buildForRange(boxArray,median+1,upperIndex);
            node.rightChild.parent = node;
            node.box = Box.boundingBox(node.leftChild.box,node.rightChild.box); // set node to contain both children
            return node;
        }
    }

    private int getMostSpreadDimension(ArrayList<BoxTreeLeaf> boxArray, int lowerBound, int upperBound) {
        int dim = 0;
        double[] max = new double[this.DIM];
        double[] min = new double[this.DIM];
        for (int i = 0; i < this.DIM; i++) {
            max[i] =  boxArray.get(lowerBound).box.getCentre(i);
            min[i] =  boxArray.get(lowerBound).box.getCentre(i);
        }
        //get ranges
        for (int j=lowerBound+1; j <= upperBound; j++) {
            for (int i = 0; i< this.DIM; i++) {
                Box box = boxArray.get(j).box;
                if (max[i] < box.getCentre(i)) {
                    max[i] = box.getCentre(i);
                } else if (min[i] > box.getCentre(i)) {
                    min[i] = box.getCentre(i);
                }
            }
        }
        // identify most spread
        for (int i = 1; i< this.DIM; i++) 
            if ((max[i]-min[i]) > (max[dim]-min[dim]))
                dim = i;
        return dim;
    }

    private void selectOnCoordinate(ArrayList<BoxTreeLeaf> boxArray, int lowerIndex, int upperIndex, int k, int partitionIndex) {
        int lb = lowerIndex;
        int ub = upperIndex;
        while (lb < ub) {
            int pivotIndex = ThreadLocalRandom.current().nextInt(lb, ub+1); // includes returning ub
            Collections.swap(boxArray, pivotIndex, lb);// swap elements at pivotIndex and lb
            int m = lb;
            for (int i = lb+1; i<=ub; i++) {
                if (boxArray.get(i).box.getCentre(partitionIndex) < boxArray.get(lb).box.getCentre(partitionIndex)) {
                    m++;
                    Collections.swap(boxArray, m, i);// swap elements at m and i
                }
            }
            Collections.swap(boxArray, m, lb);// swap elements at lb and m
            // all values from lb to m are now smaller than the item in element m on coordinate "partitionIndex"
            if (m <= k) // processed up to a value lower than k, 
                lb = m+1;
            if (m >= k) // processed p to a value higher than
                ub = m-1;
        }
    }

    /**
     * 
     */
    public boolean sanityCheck() {
        if (root == null)
            return true;
        if (root.isLeaf()) {
            return true;
        }
        if (root.leftChild.sanityCheck() == false)
            return false;
        if (root.rightChild.sanityCheck() == false)
            return false;
        return true;
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
    public Integer nearestNeighbourQuery(double[] location) throws IllegalNumberOfDimensionsException {
        BoxTreeLeaf queryResult = this.nearestLeafQuery(location);
        return queryResult == null ? null : queryResult.cargo;
    }

    /*
     * Method does various legality checking and the leaf whose location is closest to the query.
     */
    private BoxTreeLeaf nearestLeafQuery(double[] location) throws IllegalNumberOfDimensionsException{
        if (location.length != this.DIM)
            throw new IllegalNumberOfDimensionsException("This OnlineBoxTree is for " + this.DIM + " dimensions, but the query argument has " + location.length);

        // set up initial query Ball so that the centre is at the query location and the radius is such that the box completely encloses
        // the root box of the tree
        if (root == null)
            return null;
        // start with a box that will contain the tree and the query entirely    
        BoxQuery query = new BoxQuery(location, Double.MAX_VALUE);
        return (BoxTreeLeaf) this.nearestNeighbourSearch(query, root, null);
    }

    /**
     * Returns the items whose locations are within the distance queryRadius to the query.
     * 
     * @param location query point
     * @param queryRadius the ball radius around the location whose points to return
     * @returns the items stored at the locations within the ball
     */
    public ArrayList<Integer> getAllNeighboursInBound(double[] location, double queryBound) {
        if (root == null)
            return null;
        //System.out.println("QUERY BOUND ARGUMENT "+location[0]);
        BoxQuery query = new BoxQuery(location, queryBound);
        //System.out.println(query);
        ArrayList<Integer> result = new ArrayList<>();
        this.boundedNeighbourSearch(query, root, result);
        //System.out.println("list length "+result.size());
        return result;    
    }

    /*
     * Recursive method for getting neighbours within bound
     */
    private void boundedNeighbourSearch(BoxQuery query, BoxTreeNode processingNode, ArrayList<Integer> listOfItemsWithinBounds) {
        if (processingNode.isLeaf()) {
            listOfItemsWithinBounds.add(((BoxTreeLeaf)processingNode).cargo);
            //System.out.println("+++++ adding " + ((BoxTreeLeaf)processingNode).cargo.intValue());
        } else { // if at interior node, check if we need to go down
            double distLeft = processingNode.leftChild.box.nearestDistanceToCentre(query); //distance from closest point of child box to the query
            double distRight = processingNode.rightChild.box.nearestDistanceToCentre(query);
            /*if (distLeft-Box.EPS > query.step) {
                System.out.println("query: ");
                System.out.println(query);
                System.out.println("left: " + distLeft);
                System.out.println(processingNode.leftChild.box);
            }
            if (distRight-Box.EPS > query.step) {
                System.out.println("query: ");
                System.out.println(query);
                System.out.println("right: " + distRight);
                System.out.println(processingNode.rightChild.box);
            }*/
            //System.out.println("left: " + distLeft);
            //System.out.println("right: " + distRight);
            //System.out.println("step " + query.step);
            if (distLeft <= query.step) //use EPS?
                this.boundedNeighbourSearch(query, processingNode.leftChild, listOfItemsWithinBounds);
            if (distRight <= query.step)
                this.boundedNeighbourSearch(query, processingNode.rightChild, listOfItemsWithinBounds);
            
            //System.out.println(".... DONE ");
        }
    }

    /*
     * Recursive method for nearest neighbour search
     */
    private BoxTreeNode nearestNeighbourSearch(BoxQuery query, BoxTreeNode processingNode, BoxTreeNode nearestNeighbour) {
        if (processingNode.isLeaf()) {
            double distance = Box.chebyshevDistance(query.centre, processingNode.box.upper); // in a leaf the upper and lower are the same, and reference the data item location
            //System.out.println("leaf dist:"+ distance);
            if (distance <= query.step){
                query.step = distance; // update best distance
                return processingNode; // return processing node as best so far
            }
        } else { // if at interior node
            double distLeft = processingNode.leftChild.box.nearestDistanceToCentre(query);
            double distRight = processingNode.rightChild.box.nearestDistanceToCentre(query);
            //System.out.println("left: " + distLeft);
            //System.out.println("right: " + distRight);
            //System.out.println("step " + processingNode.box.step);
            if ((distLeft > query.step) && (distRight > query.step)) {
                return nearestNeighbour; // current estimate of nearest neighbour unchanged
            }
            if (distLeft  < distRight) { // search nearer child first
                nearestNeighbour = nearestNeighbourSearch(query, processingNode.leftChild, nearestNeighbour);
                if (distRight < query.step) { // check if worth searching
                    nearestNeighbour = nearestNeighbourSearch(query, processingNode.rightChild, nearestNeighbour);
                }
            } else {
                nearestNeighbour = nearestNeighbourSearch(query, processingNode.rightChild, nearestNeighbour);
                if (distLeft < query.step) { // check if worth searching
                    nearestNeighbour = nearestNeighbourSearch(query, processingNode.leftChild, nearestNeighbour);
                }
            }
        }
        return nearestNeighbour;
    }
}