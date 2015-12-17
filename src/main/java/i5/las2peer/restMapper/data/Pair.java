package i5.las2peer.restMapper.data;

/**
 * Just a generic type to store a value pair
 * 
 * @author Alexander
 *
 * @param <T>
 */
public class Pair<T> {
	private T one;
	private T two;

	public T getOne() {
		return one;
	}

	public T getTwo() {
		return two;
	}

	public Pair(T one, T two) {
		this.one = one;
		this.two = two;
	}

}
