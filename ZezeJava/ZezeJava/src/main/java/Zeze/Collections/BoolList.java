package Zeze.Collections;

import java.util.ArrayList;
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

	private static final int RECORD_BOOLS_SHIFT = 9;
	private static final int RECORD_BOOLS_COUNT = 1 << 9; // 512
	private static final int RECORD_BOOLS_MASK = RECORD_BOOLS_COUNT - 1;
	private static final int LONG_BITS_SHIFT = 6;
	private static final int LONG_BITS_COUNT = 1 << LONG_BITS_SHIFT; // 64
	private static final int LONG_BITS_MASK = LONG_BITS_COUNT - 1;

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

		var key = new BKey(name, index >>> RECORD_BOOLS_SHIFT); // /RECORD_BOOLS_COUNT
		var value = module._tBoolList.get(key);
		return value != null && get(value, index);
	}

	public void set(int index) {
		if (index < 0)
			throw new IllegalArgumentException("index < 0");

		var key = new BKey(name, index >>> RECORD_BOOLS_SHIFT); // /RECORD_BOOLS_COUNT
		var value = module._tBoolList.getOrAdd(key);
		set(value, index);
	}

	public void clear(int index) {
		if (index < 0)
			throw new IllegalArgumentException("index < 0");

		var key = new BKey(name, index >>> RECORD_BOOLS_SHIFT); // /RECORD_BOOLS_COUNT
		var value = module._tBoolList.getOrAdd(key);
		clear(value, index);
	}

	private static boolean get(BValue value, int index) {
		var i = index & RECORD_BOOLS_MASK; // %RECORD_BOOLS_COUNT
		var varBit = 1L << (i & LONG_BITS_MASK);
		switch (i >>> LONG_BITS_SHIFT) { // /LONG_BITS_COUNT
		case 0:
			return (value.getItem0() & varBit) != 0;
		case 1:
			return (value.getItem1() & varBit) != 0;
		case 2:
			return (value.getItem2() & varBit) != 0;
		case 3:
			return (value.getItem3() & varBit) != 0;
		case 4:
			return (value.getItem4() & varBit) != 0;
		case 5:
			return (value.getItem5() & varBit) != 0;
		case 6:
			return (value.getItem6() & varBit) != 0;
		case 7:
			return (value.getItem7() & varBit) != 0;
		}
		return false;
	}

	private static void set(BValue value, int index) {
		var i = index & RECORD_BOOLS_MASK; // %RECORD_BOOLS_COUNT
		var varBit = 1L << (i & LONG_BITS_MASK);
		switch (i >>> LONG_BITS_SHIFT) { // /LONG_BITS_COUNT
		case 0:
			value.setItem0(value.getItem0() | varBit);
			break;
		case 1:
			value.setItem1(value.getItem1() | varBit);
			break;
		case 2:
			value.setItem2(value.getItem2() | varBit);
			break;
		case 3:
			value.setItem3(value.getItem3() | varBit);
			break;
		case 4:
			value.setItem4(value.getItem4() | varBit);
			break;
		case 5:
			value.setItem5(value.getItem5() | varBit);
			break;
		case 6:
			value.setItem6(value.getItem6() | varBit);
			break;
		case 7:
			value.setItem7(value.getItem7() | varBit);
			break;
		}
	}

	private static void clear(BValue value, int index) {
		var i = index & RECORD_BOOLS_MASK; // %RECORD_BOOLS_COUNT
		var varMask = ~(1L << (i & LONG_BITS_MASK));
		switch (i >>> LONG_BITS_SHIFT) { // /LONG_BITS_COUNT
		case 0:
			value.setItem0(value.getItem0() & varMask);
			break;
		case 1:
			value.setItem1(value.getItem1() & varMask);
			break;
		case 2:
			value.setItem2(value.getItem2() & varMask);
			break;
		case 3:
			value.setItem3(value.getItem3() & varMask);
			break;
		case 4:
			value.setItem4(value.getItem4() & varMask);
			break;
		case 5:
			value.setItem5(value.getItem5() & varMask);
			break;
		case 6:
			value.setItem6(value.getItem6() & varMask);
			break;
		case 7:
			value.setItem7(value.getItem7() & varMask);
			break;
		}
	}

	/**
	 * clearAll，使用walkDatabaseKey并且删除所有记录的方式清除当前name的所有Bools。
	 * 事务外调用.
	 */
	public void clearAll() {
		var table = module._tBoolList;
		var curKey = new BKey(name, 0);
		var curKeyFinal = curKey;
		module.zeze.newProcedure(() -> {
			table.remove(curKeyFinal);
			return 0;
		}, "remove first").call();

		final int BATCH_COUNT = 20;
		var batch = new ArrayList<BKey>(BATCH_COUNT);
		do {
			curKey = table.walkKey(curKey, BATCH_COUNT, key -> name.equals(key.getName()) && batch.add(key));
			if (!batch.isEmpty()) {
				module.zeze.newProcedure(() -> {
					for (var key : batch)
						table.remove(key);
					return 0;
				}, "remove some").call();
				batch.clear();
			}
		}
		while (curKey != null && name.equals(curKey.getName()));
	}
}
