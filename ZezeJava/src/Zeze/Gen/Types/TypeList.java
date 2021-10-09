package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class TypeList extends TypeCollection {
	@Override
	public void Accept(Visitor visitor) {
		visitor.Visit(this);
	}

	@Override
	public Type Compile(ModuleSpace space, String key, String value) {
		return new TypeList(space, key, value);
	}

	private TypeList(Zeze.Gen.ModuleSpace space, String key, String value) {
		if (key != null && key.length() > 0) {
			throw new RuntimeException(getName() + " type does not need a key. " + key);
		}

		setValueType(Type.Compile(space, value, null, null));
		//if (ValueType is TypeBinary)
		//	throw new Exception(Name + " Error : value type is binary.");
		if (getValueType() instanceof TypeDynamic) {
			throw new RuntimeException(getName() + " Error : value type is dynamic.");
		}
	}

	public TypeList(TreeMap<String, Type> types) {
		types.put(getName(), this);
	}

	@Override
	public String getName() {
		return "list";
	}
	@Override
	public boolean isNeedNegativeCheck() {
		return getValueType().isNeedNegativeCheck();
	}

}