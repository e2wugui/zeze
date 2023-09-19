package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.BoolList.BKey;
import Zeze.Builtin.Collections.BoolList.BValue;

public class BoolList {
	public static class Module extends AbstractBoolList {
		private final ConcurrentHashMap<String, BoolList> boolLists = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;

		public Module(Zeze.Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(zeze);
		}

		public BoolList open(String name) {
			if (name.contains("@"))
				throw new IllegalArgumentException("name contains '@', that is reserved.");
			if (name.isEmpty())
				throw new IllegalArgumentException("name is empty.");
			return boolLists.computeIfAbsent(name, key -> new BoolList(this, key));
		}
	}

	private final Module module;
	private final String name;

	private BoolList(Module module, String name) {
		this.module = module;
		this.name = name;
	}

	public Module getModule() {
		return module;
	}

	public String getName() {
		return name;
	}

	public boolean get(int index) {
		if (index < 0)
			throw new IllegalArgumentException("index < 0");

		var key = new BKey(name, index / 512);
		var value = module._tBoolList.get(key);
		if (null == value)
			return false;
		return get(value, index);
	}

	public void set(int index) {
		if (index < 0)
			throw new IllegalArgumentException("index < 0");

		var key = new BKey(name, index / 512);
		var value = module._tBoolList.getOrAdd(key);
		set(value, index);
	}

	private boolean get(BValue value, int index) {
		var i = index % 512;
		var varId = i / 64;
		var varIndex = i % 64;
		switch (varId) {
		case 0:
			return (value.getItem0() & (1L << varIndex)) != 0;
		case 1:
			return (value.getItem1() & (1L << varIndex)) != 0;
		case 2:
			return (value.getItem2() & (1L << varIndex)) != 0;
		case 3:
			return (value.getItem3() & (1L << varIndex)) != 0;
		case 4:
			return (value.getItem4() & (1L << varIndex)) != 0;
		case 5:
			return (value.getItem5() & (1L << varIndex)) != 0;
		case 6:
			return (value.getItem6() & (1L << varIndex)) != 0;
		case 7:
			return (value.getItem7() & (1L << varIndex)) != 0;
		}
		return false;
	}

	private void set(BValue value, int index) {
		var i = index % 512;
		var varId = i / 64;
		var varIndex = i % 64;
		switch (varId) {
		case 0:
			value.setItem0(value.getItem0() | (1L << varIndex));
			break;
		case 1:
			value.setItem1(value.getItem1() | (1L << varIndex));
			break;
		case 2:
			value.setItem2(value.getItem2() | (1L << varIndex));
			break;
		case 3:
			value.setItem3(value.getItem3() | (1L << varIndex));
			break;
		case 4:
			value.setItem4(value.getItem4() | (1L << varIndex));
			break;
		case 5:
			value.setItem5(value.getItem5() | (1L << varIndex));
			break;
		case 6:
			value.setItem6(value.getItem6() | (1L << varIndex));
			break;
		case 7:
			value.setItem7(value.getItem7() | (1L << varIndex));
			break;
		}
	}
}
