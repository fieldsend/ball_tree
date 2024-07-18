package ball_tree;


/**
 * IllegalNumberOfDimensionsException is for when the dimensions of arguments do not match requirements of the {@link OnlineBallTree}.
 *
 * @author Jonathan Fieldsend
 * @version 1.0
 */
public class IllegalNumberOfDimensionsException extends Exception
{
    
    /**
     * No-argumnet constructor for objects of class IllegalNumberOfDimensionsException
     */
    public IllegalNumberOfDimensionsException()
    {
    }
    
    /**
     * Constructor for objects of class IllegalNumberOfDimensionsException containing a message
     * 
     * @param message message about the cause of the exception
     */
    public IllegalNumberOfDimensionsException(String message)
    {
        super(message);
    }
}