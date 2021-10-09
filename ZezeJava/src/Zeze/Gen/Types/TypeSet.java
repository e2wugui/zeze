package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class TypeSet extends TypeCollection {
	@Override
	public void Accept(Visitor visitor) {
		visitor.Visit(this);
	}

	@Override
	public Type Compile(ModuleSpace space, String key, String value) {
		return new TypeSet(space, key, value);
	}

	private TypeSet(ModuleSpace space, String key, String value) {
		_compile(space, key, value);
		if (!getValueType().isKeyable()) {
			throw new RuntimeException("set value need a keyable type.");
		}
	}

	public TypeSet(TreeMap<String, Type> types) {
		types.put(getName(), this);
	}

	@Override
	public String getName() {
		return "set";
	}
	@Override
	public boolean isNeedNegativeCheck() {
		return getValueType().isNeedNegativeCheck();
	}

}