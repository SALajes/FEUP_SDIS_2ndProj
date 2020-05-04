package project.store;

public class Pair<U, V> {

    public U first;   	// first field of a Pair
    public V second;  	// second field of a Pair

    // Constructs a new Pair with specified values
    public Pair(U first, V second)
    {
        this.first = first;
        this.second = second;
    }
}
