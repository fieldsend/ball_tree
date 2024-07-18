package ball_tree;

import java.io.Serializable;

/**
 * BallTreeLeaf holds leaf details in the tree.
 *
 * @author Jonathan Fieldsend
 * @version 1.0
 */
public class BallTreeLeaf<T> extends BallTreeNode implements Serializable
{
    T cargo;
    
    private static final long serialVersionUID = 42L;
    /**
     * Stores the ball argument, and as this is a leaf also stores the cargo item
     * 
     * @param ball ball (point) to be stored
     * @param cargo item associated with the corresponding ball (point) to be stored at the leaf
     */
    BallTreeLeaf(Ball ball, T cargo){
        super(ball);
        this.cargo = cargo;
    }
    
    /**
     * The BallTreeLeaf is a leaf, so returns true
     * 
     * @returns true if this is a leaf
     */
    @Override
    boolean isLeaf() {
        return true;
    }
}
