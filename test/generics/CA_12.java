package generics;

import java.util.List;

public class CA_12<T> extends AA_G<Comparable<? extends T>> implements IA_G<Comparable<? extends List<Integer>>>, IB_NG, IB_G<T> {

	public void defMethod(){}
	
	public void radiatedMethod() {
		int i = 1;
		int j = i++; //mutGenLimit 1
	}

}
