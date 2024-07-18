# ball_tree
A dynamic generic ball tree implementation in Java

OnlineBallTree is a generic implementation of the first online balltree algorithm set out in pages 11-14 of:

Omohundro, Stephen M. 
Five balltree construction algorithms. 
Berkeley: International Computer Science Institute, 1989

Due to precision contraints, limited to arrays of max 452 dimensions (practically I would advise fewer than 
this anyway, as the range of the varaibles will also have an effect on when ball volumes get vanishingly small).

To instantiate use:
<code>
OnlineBallTree&lt;Type&gt; ballTree = new OnlineBallTree&lt;&gt;(dim);
</code>
Where <code>Type</code> denotes the object type (cargo) you want to associate with each element in the ball 
tree, and <code>dim</code> denotes the number of variables in the double-precision arrays which are the 
locations stored in the ball tree.

<code>
ballTree.insert(location,cargo);
</code>

Will insert double array <code>location</code> into the ball tree, along with the associated cargo object.

Various methods are provided for querying, updating, removing, getting cargo of all neighbours within the ball around a query location, etc. (see JavaDoc).

Note, requires apache commons, specifically
<code>
org.apache.commons.math3.special.Gamma;
</code>
