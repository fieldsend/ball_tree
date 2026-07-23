package ball_tree;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.ArrayList;

/**
 * The test class OfflineBallTreeTest.
 *
 * @author  (your name)
 * @version (a version number or a date)
 */
public class OfflineBallTreeTest
{
    private static Random randomNumberGenerator = new Random(0);

    /**
     * Default constructor for test class OnlineBallTreeTest
     */
    public OfflineBallTreeTest() 
    {

    }

    @Test
    public void insertTest() throws IllegalNumberOfDimensionsException{
        int maxGens = 10067;
        int dim = 10;
        int k = 11;
        double rUsed = 0.7;
        double[][] history = new double[maxGens][dim];
        Integer[] cargo = new Integer[maxGens];

        
        for (int i=0; i < maxGens; i++) {    
            double[] decisionVector = new double[dim];
            for (int j=0; j<decisionVector.length; j++)
                decisionVector[j] = randomNumberGenerator.nextDouble();
            System.out.println("Inserting: " + decisionVector[0] + ", " + decisionVector[1]);
            history[i] = decisionVector;
            cargo[i] = i;
        }
        OfflineBallTree<Integer> ballTree = new OfflineBallTree<>(history,cargo);
        assertTrue(ballTree.size() == maxGens);
        for (int i=0; i < maxGens; i++) {   
            Integer j = ballTree.nearestNeighbourQuery(history[i]);
            assertEquals(i,j); // should return itself
            
            ArrayList<Integer> knn = ballTree.kNearestNeighbourQuery(history[i],k);
            assertTrue(knn.contains(i)); // knn must always contain the 1-nn
            ArrayList<Integer> knn1 = getKClosest(history,history[i],k);
            assertTrue(knn.containsAll(knn1)); //  both lists should have the same contents
            
            ArrayList<Integer> rnn = ballTree.getAllNeighboursInRadius(history[i], rUsed);
            ArrayList<Integer> rnn1 = getRClosest(history,history[i],rUsed);
            System.out.println("relative list lengths: " +rnn.size() + " versus " + rnn1.size());
            //assertTrue(ballTree.recursiveContentsCheck());
            assertTrue(rnn.containsAll(rnn1)); // check that r closest from exhaustive search matches r closest from ball tree
            assertTrue(rnn1.containsAll(rnn));
        }
        
    
    }

    @Test
    public void coherentTreeTest() throws IllegalNumberOfDimensionsException {
        int maxGens = 1000000;
        int dim = 2;
        double rUsed = Math.sqrt(2)/100;
        double[][] history = new double[maxGens][dim];
        Integer[] cargo = new Integer[maxGens];

        for (int i=0; i < maxGens; i++) {    
            double[] decisionVector = new double[dim];
            for (int j=0; j<decisionVector.length; j++)
                decisionVector[j] = randomNumberGenerator.nextDouble();
            if (i % 1000 == 0)
                System.out.println("Inserting: " + i + " : "  + decisionVector[0] + ", " + decisionVector[1]);
            history[i] = decisionVector;
            cargo[i] = i;
        }
        OfflineBallTree<Integer> ballTree = new OfflineBallTree<>(history,cargo);
        assertTrue(ballTree.size() == maxGens);
        
        // now check all are neighbours
        for (int i=0; i < maxGens; i++) { 
            
            ArrayList<Integer> list = ballTree.getAllNeighboursInRadius(history[i], rUsed);    
            if (i % 1000 == 0) 
                System.out.println("Checking: " + i + " : list size " + list.size());
            for (Integer index : list) {
                ArrayList<Integer> neighbourList = ballTree.getAllNeighboursInRadius(history[index], rUsed);
                assertTrue(neighbourList.contains(i));
            }
        }
    }
    
    private Integer getClosest(double[][] history, double[] decisionVector){
        double dist = Ball.squaredDist(decisionVector, history[0]);
        Integer result = 0;
        for (int i=1; i< history.length; i++) {
            if (Ball.squaredDist(decisionVector, history[i]) < dist) {
                dist = Ball.squaredDist(decisionVector, history[i]);
                result = i;
            }
        }
        return result;
    }

    private ArrayList<Integer> getRClosest(double[][] history, double[] decisionVector, double radius) {
        double[] distances = new double[history.length];
        ArrayList<Integer> result = new ArrayList<>();
        for (int i=0; i< history.length; i++) {
            distances[i] = Math.sqrt(Ball.squaredDist(decisionVector, history[i]));  
        }

        for (int i=0; i< history.length; i++) {
            if (distances[i] <= radius ) {
                result.add(i);
            }
        }
        return result;
    }

    private ArrayList<Integer> getKClosest(double[][] history, double[] decisionVector,int k) {
        double[] distances = new double[history.length];
        boolean[] used = new boolean[history.length];
        ArrayList<Integer> result = new ArrayList<>(k);
        if (k >= history.length) { // return history if k or fewer elements in it
            for (int i=0; i < history.length; i++)
                result.add(i);
            return result;    
        }

        for (int i=0; i< history.length; i++) {
            distances[i] = Ball.squaredDist(decisionVector, history[i]);  
            used[i] = false;
        }

        for (int j=0; j<k; j++){
            double dist = Double.MAX_VALUE;
            int trackIndex = -1;
            for (int i=0; i< history.length; i++) {
                if (used[i] == false) {
                    if (distances[i] < dist ) {
                        dist = distances[i];
                        trackIndex = i;
                    }
                }
            }
            used[trackIndex] = true;
            result.add(trackIndex);
        }
        return result;
    }

    /*
     
    <T> boolean validContentsCheck(OnlineBallTree<T> tree) {
        ArrayList<Ball> leafContents = new ArrayList<>();
        this.<T>fillListWithChildren(leafContents,tree);
        for (Ball b : leafContents) {
            if (this.ball.contains(b.centre) == false ) {
                // problem -- ball does not contain its leaf
                System.out.println("ball centre " + this.ball.centre[0] + ", " + this.ball.centre[1]);
                System.out.println("ball radius " + this.ball.radius);
                System.out.println("leaf centre " + b.centre[0] + ", " + b.centre[1]);
                System.out.println("leaf radius " + b.radius);
                System.out.println("Ball encloses? " + this.ball.encloses(b));
                System.out.println("Distance to ball surface" +(this.ball.nearestDistanceToCentre(b) - 2*Math.ulp(this.ball.nearestDistanceToCentre(b))));
                return false;
            }
        }
        return true;
    }

    private <T> void fillListWithChildren(ArrayList<Ball> leafContents,OnlineBallTree<T> tree) {
        if (isLeaf()) {
            leafContents.add(((BallTreeLeaf<T>) this).ball);
        } else {
            System.out.println(rightChild.validContentsCheck(tree)); // put assert in
            System.out.println(leftChild.validContentsCheck(tree)); //put assert in)
            rightChild.<T>fillListWithChildren(leafContents,tree);
            leftChild.<T>fillListWithChildren(leafContents,tree);
        }
    }
    */
    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    @BeforeEach
    public void setUp()
    {
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    @AfterEach
    public void tearDown()
    {
    }
}
