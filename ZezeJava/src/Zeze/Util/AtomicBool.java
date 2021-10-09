package Zeze.Util;

import Zeze.*;

public final class AtomicBool {
	private int _value;


	public AtomicBool() {
		this(false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public AtomicBool(bool initialValue = false)
	public AtomicBool(boolean initialValue) {
		_value = initialValue ? 1 : 0;
	}

	public boolean CompareAndExchange(boolean expectedValue, boolean newValue) {
		int n = newValue ? 1 : 0;
		int e = expectedValue ? 1 : 0;
		tangible.RefObject<Integer> tempRef__value = new tangible.RefObject<Integer>(_value);
		int r = System.Threading.Interlocked.CompareExchange(tempRef__value, n, e);
	_value = tempRef__value.refArgValue;
		return r != 0;
	}

	public boolean Get() {
		return _value != 0;
	}

	/** 
	 ??? 对于 bool 来说，和 CompareAndExchange 差不多 ???
	 
	 @param newValue
	 @return 
	*/
	public boolean GetAndSet(boolean newValue) {
		tangible.RefObject<Integer> tempRef__value = new tangible.RefObject<Integer>(_value);
		var tempVar = System.Threading.Interlocked.Exchange(tempRef__value, newValue ? 1 : 0) != 0;
	_value = tempRef__value.refArgValue;
	return tempVar;
	}
}