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
public class OfflineBoxTreeTest
{
    private static Random randomNumberGenerator = new Random(0);

    /**
     * Default constructor for test class OnlineBallTreeTest
     */
    public OfflineBoxTreeTest() 
    {

    }

    @Test
    public void insertTest() throws IllegalNumberOfDimensionsException{
        int maxGens = 100067;
        int dim = 10;
        int k = 11;
        double rUsed = 0.25;
        double[][] history = new double[maxGens][dim];
        int[] cargo = new int[maxGens];

        
        for (int i=0; i < maxGens; i++) {    
            double[] decisionVector = new double[dim];
            for (int j=0; j<decisionVector.length; j++)
                decisionVector[j] = randomNumberGenerator.nextDouble();
            //System.out.println("Inserting: " + decisionVector[0] + ", " + decisionVector[1]);
            history[i] = decisionVector;
            cargo[i] = i;
        }
        OfflineBoxTree boxTree = new OfflineBoxTree(history,cargo);
        assertTrue(boxTree.size() == maxGens);
        System.out.println("SC");
        assertTrue(boxTree.sanityCheck());
        for (int i=0; i < maxGens; i++) {   
            Integer j = boxTree.nearestNeighbourQuery(history[i]);
            assertEquals(i,j); // should return itself
        }
        int timesWrong=0;
        for (int i=0; i < maxGens; i++) {   
            
            //ArrayList<Integer> knn = boxTree.kNearestNeighbourQuery(history[i],k);
            //assertTrue(knn.contains(i)); // knn must always contain the 1-nn
            //ArrayList<Integer> knn1 = getKClosest(history,history[i],k);
            //assertTrue(knn.containsAll(knn1)); //  both lists should have the same contents
            //System.out.println("SEARCH query: " + i);
            ArrayList<Integer> rnn = boxTree.getAllNeighboursInBound(history[i], rUsed);
            ArrayList<Integer> rnn1 = getRClosest(history,history[i],rUsed);
            System.out.println("SEARCH query was : " + i + " num returned " + rnn.size());
            /*for (int kk = 0; kk<history[i].length; kk++){
                    System.out.print(history[i][kk] + ",");
                }
                System.out.println("\ntree returned, bound is : " + rUsed);    
            for (Integer jj : rnn){
                for (int kk = 0; kk<history[jj].length; kk++){
                    System.out.print(history[jj][kk] + ",");
                }
                System.out.println();
            }
            System.out.println("returned, bound is : " + rUsed);    
            for (Integer jj : rnn1){
                for (int kk = 0; kk<history[jj].length; kk++){
                    System.out.print(history[jj][kk] + ",");
                }
                System.out.println();
            }*/
            //System.out.println("relative list lengths: " +rnn.size() + " versus " + rnn1.size());
            //assertTrue(ballTree.recursiveContentsCheck());
            //if (rnn.size() != rnn1.size()) {
            //    timesWrong++;
            //} else {
                assertTrue(rnn.containsAll(rnn1),"num of box tree closest in bound: " + rnn.size() + " check num: " + rnn1.size()); // check that r closest from exhaustive search matches r closest from ball tree
                assertTrue(rnn1.containsAll(rnn));
            //}
        }
        //System.out.println("number of times arrays of different lengths "+timesWrong);
    
    }

    @Test
    public void coherentTreeTest() throws IllegalNumberOfDimensionsException {
        int maxGens = 1000000;
        int dim = 2;
        double rUsed = Math.sqrt(2)/100;
        double[][] history = new double[maxGens][dim];
        int[] cargo = new int[maxGens];

        for (int i=0; i < maxGens; i++) {    
            double[] decisionVector = new double[dim];
            for (int j=0; j<decisionVector.length; j++)
                decisionVector[j] = randomNumberGenerator.nextDouble();
            if (i % 1000 == 0)
                System.out.println("Inserting: " + i + " : "  + decisionVector[0] + ", " + decisionVector[1]);
            history[i] = decisionVector;
            cargo[i] = i;
        }
        OfflineBoxTree boxTree = new OfflineBoxTree(history,cargo);
        assertTrue(boxTree.size() == maxGens);
        
        // now check all are neighbours
        for (int i=0; i < maxGens; i++) { 
            
            ArrayList<Integer> list = boxTree.getAllNeighboursInBound(history[i], rUsed);    
            if (i % 1000 == 0) 
                System.out.println("Checking: " + i + " : list size " + list.size());
            for (Integer index : list) {
                ArrayList<Integer> neighbourList = boxTree.getAllNeighboursInBound(history[index], rUsed);
                assertTrue(neighbourList.contains(i));
            }
        }
    }
    /*
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
*/
    private ArrayList<Integer> getRClosest(double[][] history, double[] decisionVector, double bound) {
        double[] distances = new double[history.length];
        ArrayList<Integer> result = new ArrayList<>();
        for (int i=0; i< history.length; i++) {
            double d = 0.0;
            for (int j=0; j<decisionVector.length; j++)
                d = Math.max(d,Math.abs(decisionVector[j]-history[i][j]));
            distances[i] = d;  
        }

        for (int i=0; i< history.length; i++) {
            if (distances[i] <= bound ) {
                result.add(i);
            }
        }
        return result;
    }
/*
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