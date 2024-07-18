package ball_tree;

import java.io.Serializable;

/**
 * TestFringe class holds details of fringe node to be processed and stored in a priority queue.
 *
 * @author Jonathan Fieldsend
 * @version 1.0
 */
class TestFringe implements Comparable<TestFringe>, Serializable
{
    BallTreeNode node;
    double ancestorExpansion;
    double nodeVolume;
    
    /**
     * Constructor for the TestFringe instance 
     * 
     * @param ancestorExpansion ancestor expansion to store for this fringe item
     * @param nodeVolume volume to store for this fringe item
     * @param node node to store for this fringe item
     */
    TestFringe(double ancestorExpansion, double nodeVolume, BallTreeNode node) {
        this.ancestorExpansion = ancestorExpansion;
        this.nodeVolume = nodeVolume;
        this.node = node;
    }
    
    @Override
    public int compareTo(TestFringe o) {
        if (this.ancestorExpansion < o.ancestorExpansion) // this expansion is smaller, so smaller
            return -1;
        if (this.ancestorExpansion > o.ancestorExpansion) // this expansion is bigger, so bigger
            return 1;
        if (this.node == o.node) // same node, so same
            return 0;
        if (this.nodeVolume < o.nodeVolume) // same exampansion value, but smaller node volume, so smaller
            return -1;
        if (this.nodeVolume == o.nodeVolume)  // same exampansion value, same node volume, so same 
            return 0;
        return 1;    // same exampansion value, but larger node volume, so bigger
    }
}
