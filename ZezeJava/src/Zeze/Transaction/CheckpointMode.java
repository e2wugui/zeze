package Zeze.Transaction;

import Zeze.*;

public enum CheckpointMode {
	Period(0),
	Immediately(1),
	Table(2);

	public static final int SIZE = java.lang.Integer.SIZE;

	private int intValue;
	private static java.util.HashMap<Integer, CheckpointMode> mappings;
	private static java.util.HashMap<Integer, CheckpointMode> getMappings() {
		if (mappings == null) {
			synchronized (CheckpointMode.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, CheckpointMode>();
				}
			}
		}
		return mappings;
	}

	private CheckpointMode(int value) {
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static CheckpointMode forValue(int value) {
		return getMappings().get(value);
	}
}