package ball_tree;

import java.util.HashMap;
import org.apache.commons.math3.special.Gamma;
import java.io.Serializable;

/**
 * Defines the Ball objects managed by the tree.
 *
 * @author Jonathan Fieldsend
 * @version 1.0
 */
public class Ball implements Serializable
{
    double[] centre;
    double radius;
    double volume;
    private static final long serialVersionUID = 42L;
    // use fixed array for all sphere volume calcs, the gamma function claculation just depends on 
    // the dimension of the problem so just compute once when first needed and store. Note, due to precision limitations
    // dimensions above 452 lead to unit sphere volumes of 0.0, so we limit this.
    private static final double[] logGammaTerm = new double[453];
    private static final double[] piTerm = new double[453];
    static { // set up static members, essentially caching regularly computed constants
        System.out.println("static set up");
        for (int i = 1; i<452; i++){
            logGammaTerm[i] = Gamma.logGamma(1 + i/2.0);
            piTerm[i] = (i/2.0)*Math.log(Math.PI);
        }
    };
    
    /**
     * Create a new ball with the corresponding centre and radius.
     * 
     * @param ball centre
     * @param ball radius
     */
    Ball(double[] centre, double radius) {
        this.centre = centre;
        this.radius = radius;
        this.volume = calculateHypervolume();
    }
    
    /**
     * Creates a ball whose state copies the argument ball.
     * 
     * @param a ball whose state to copy in constructing this ball 
     */
    Ball(Ball a) {
        this.centre = new double[a.centre.length];
        for (int i = 0; i < centre.length; i++)
            this.centre[i] = a.centre[i];
        this.radius = a.radius;
        //this.radiusSquared = a.radiusSquared;
        this.volume = a.volume;
    }
    
    /** 
     * Returns the distance from the centre of the query Ball to the surface of the hypersphere of this ball
     * which is the closest possible for any point contained in this Ball to the centre of the query.
     * 
     * @param query ball to compare to centre of
     */
    double nearestDistanceToCentre(Ball query){
        return Math.sqrt(this.squaredDistanceToCentre(query)) - this.radius; // first term holds the Euclidean distance bewteen the two centres
    }
    
    /*
     * Calculate and return the (hyper)volume of this ball
     */
    private double calculateHypervolume() {
        // use rule of logs on calulation, as components prone to get very large as the number of dimenions increases
        return Math.exp(Ball.piTerm[centre.length] + this.centre.length*Math.log(this.radius) - Ball.logGammaTerm[centre.length]);
    }
    
    /**
     * Returns true if this Ball encloses the argument Ball, otherwise returns false.
     * 
     * @param ball argument to check if enclosed
     * @returns true if this encloses the argument, otherwise false 
     */
    boolean encloses(Ball ball) {
        // if the difference in radius is larger than the difference in the centre locations
        // then the argumnet ball is enclosed (includes equality)
        double radiusDiff = this.radius - ball.radius;
        if (radiusDiff < 0.0)
            return false; // cannot contain a larger sphere
        return Math.pow(radiusDiff,2) > Ball.squaredDist(this.centre,ball.centre);    // squaring first term as usually faster than sqrt of second term
    }
    
    /**
     * Returns the square distance between the centre of this ball and the centre of the argument ball.
     * 
     * @param a ball to compare to
     * @return squared distances bewteen centres
     */
    double squaredDistanceToCentre(Ball a) {
        return Ball.squaredDist(this.centre,a.centre);
    }
    
    /**
     * Returns the squared distance between the two arguments.
     * 
     * @param a array to compare
     * @param b array to compare
     * @returns squared distance between a and b
     */
    static double squaredDist(double[] a, double[] b) {
        double dist = 0.0;
        for (int i =0; i<a.length; i++)
            dist += Math.pow(a[i]-b[i],2);
        return dist;
    }
    
    /** 
     * Returns true if the argument lies within this ball.
     * 
     * @param point location to check if inside this ball
     * @returns true if point is within the ball, flase otherwise
     */
    boolean contains(double [] point) {
        return Ball.squaredDist(this.centre, point) <= Math.pow(this.radius,2);
    }
    
    /**
     * Returns a new ball instance whose centre and radius is set such that is is the smallest volume ball which encloses both arguments.
     * 
     * @param a ball to be enclosed
     * @param b ball to be enclosed
     * @returns minimal enclosing ball
     */
    static Ball boundingBall(Ball a, Ball b) {
        if (a.encloses(b))
            return new Ball(a);
        if (b.encloses(a))
            return new Ball(b);
        double[] centre = new double[a.centre.length];
        double[] centreDiff = new double[a.centre.length];
        double radiusDiff = a.radius - b.radius;
        double centreDistance = 0.0;
        for (int i = 0; i < centre.length; i++){
            centreDiff[i] = a.centre[i] - b.centre[i];
            centreDistance += Math.pow(centreDiff[i],2);
        }
        centreDistance = Math.sqrt(centreDistance);
        for (int i = 0; i < centre.length; i++)
            centre[i] = ((a.centre[i] + b.centre[i]) + (centreDiff[i]/centreDistance) * radiusDiff)/2.0;
            
        return new Ball(centre, (centreDistance + a.radius + b.radius)/2.0);    
    }
}
