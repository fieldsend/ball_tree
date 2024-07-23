package ball_tree;

import java.io.Serializable;

/**
 * BallTreeNode holds details for each node in the BallTree (ball, children and parents.
 *
 * @author Jonathan Fieldsend
 * @version 1.0
 */
public class BallTreeNode implements Serializable
{
    BallTreeNode rightChild = null;
    BallTreeNode leftChild = null;
    BallTreeNode parent = null;
    Ball ball;

    private static final long serialVersionUID = 42L;
    /**
     * Creates an instance of the BallTreeNode containing the ball argument, but which is not yet attached to a tree (no parent or children)
     */
    BallTreeNode(Ball ball) {
        this.ball = ball;
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
