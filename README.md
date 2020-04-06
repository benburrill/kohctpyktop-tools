# Kohctpyktop tools

The KohSave class represents a Kohctpyktop save.
This can be loaded using `KohSave.fromString` from a save string.

So, for example, to load from a file, you can do:

```java
String fileName = "/path/to/file.sav";
File saveString = Files.readString(Paths.get(fileName));
KohSave save = KohSave.fromString(saveString);
```

Once you have a `KohSave` object, you can use the `.getPins()` method to get an
array of `Wire` objects in the centers of each of the pads on the left and right
of the grid.  This is a good place to start when applying an algorithm to the
data.  Once you have a wire object (which you could get from `getPins`), you can
use the `.getNeighbors(wire)` method.  This will give you a `LinkedList` of all 
the neighboring wires which current can flow to.  This includes going up and 
down through vias as well as to adjacent wires.

When on a gate, `.getGateSwitches(wire)` will tell you where the top wire came 
from and what kind of gate it is.  It actually returns a linked list as well 
because it is possible for the top wire to go in two directions (For example, 
see `saves/double-crossings.sav`).

Another useful class is `CircuitMap`.  It is just a data structure that keeps
track of arbitrary data associated with a wire position.

So for example, to create a mapping from wires to boolean representing whether
the wire has been seen yet, you can do: 
```java
CircuitMap<Boolean> seen = new CircuitMap<>(save, false);
// Mark a wire as seen
seen.set(wire, true);
// Test if a wire has been seen
if (seen.get(wire)) System.out.println(wire + " was seen");
```

This is useful for algorithms that need to mark down information about wires as
they are found.