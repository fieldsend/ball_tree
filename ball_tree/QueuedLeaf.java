package ball_tree;

import java.io.Serializable;

/**
 * QueuedLeaf wraps up a leaf item in the priority queue for k nearest neighbour storage (and queries).
 * as the prioority queue has the worst value at the head with the least value, the comparitor
 * is in the negative distance
 *
 * @author Jonathan Fieldsend
 * @version 1.0
 */
public class QueuedLeaf<T> implements Comparable<QueuedLeaf<T>>, Serializable
{
    BallTreeLeaf<T> leaf;
    double distance;
    
    private static final long serialVersionUID = 42L;
    /**
     * Creates a QueuedLeaf containing the leaf argument and its corresponding distance to the query
     * 
     * @param leaf item to be queued
     * @param distance of the leaf item to the query
     */
    QueuedLeaf(BallTreeLeaf<T> leaf, double distance) {
        this.distance = distance;//toQueue.ball.squaredDistanceToCentre(query);
        this.leaf = leaf;
    }
    
    /**
     * Compares this QueuedLeaf to the argument. If this QueuedLeaf has a smaller distance tio the query then it is categorised as being
     * bigger than the argument, and if this QueuedLeaf has a larger distance to the query then it is categorised as being smaller than 
     * the argument, as the prority queue puts smaller values to its head
     */
    @Override
    public int compareTo(QueuedLeaf<T> o) {
        if (this.distance < o.distance) // this distance is smaller, so better, so place away from the head of the queue
            return 1;
        if (this.distance > o.distance) // this distance is bigger, so worse, so place toward the head of the queue
            return -1;
        return 0;    // same distance, so equivalent
    }


}
