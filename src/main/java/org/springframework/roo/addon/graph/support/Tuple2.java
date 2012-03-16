/**
 * 
 */
package org.springframework.roo.addon.graph.support;

public final class Tuple2<T1,T2> {
	public final T1 _1;
	public final T2 _2;

	private Tuple2(T1 _1,T2 _2) {
		this._1=_1;
		this._2=_2;
	}
	public static <T1,T2>  Tuple2<T1,T2> _(T1 _1, T2 _2) {
		return new Tuple2<T1,T2>(_1,_2);
	}

    @Override
    public String toString() {
        return String.format("(%s,%s)",_1,_2);
    }
}