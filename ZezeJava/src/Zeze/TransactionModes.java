package Zeze;

public enum TransactionModes {
	ExecuteInTheCallerTransaction,
	ExecuteInNestedCall,
	ExecuteInAnotherThread;

	public static final int SIZE = java.lang.Integer.SIZE;

	public int getValue() {
		return this.ordinal();
	}

	public static TransactionModes forValue(int value) {
		return values()[value];
	}
}