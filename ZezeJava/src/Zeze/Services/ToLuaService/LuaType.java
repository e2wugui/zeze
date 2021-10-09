package Zeze.Services.ToLuaService;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import java.util.*;

public enum LuaType {
	None(-1),
	Nil(0),
	Boolean(1),
	LightUserData(2),
	Number(3),
	String(4),
	Table(5),
	Function(6),
	UserData(7),
	Thread(8);

	public static final int SIZE = java.lang.Integer.SIZE;

	private int intValue;
	private static java.util.HashMap<Integer, LuaType> mappings;
	private static java.util.HashMap<Integer, LuaType> getMappings() {
		if (mappings == null) {
			synchronized (LuaType.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, LuaType>();
				}
			}
		}
		return mappings;
	}

	private LuaType(int value) {
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static LuaType forValue(int value) {
		return getMappings().get(value);
	}
}