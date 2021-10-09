package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class TypeBinary extends Type {
	@Override
	public Type Compile(ModuleSpace space, String key, String value) {
		if (key != null && key.length() > 0) {
			throw new RuntimeException(getName() + " type does not need a key. " + key);
		}

		if (value != null && value.length() > 0) {
			throw new RuntimeException(getName() + " type does not need a value. " + value);
		}

		return this;
	}

	@Override
	public void Accept(Visitor visitor) {
		visitor.Visit(this);
	}

	@Override
	public void Depends(HashSet<Type> includes) {
		includes.add(this);
	}

	@Override
	public String getName() {
		return "binary";
	}
	@Override
	public boolean isImmutable() {
		return true;
	}
	@Override
	public boolean isKeyable() {
		return true;
	}
	@Override
	public boolean isNeedNegativeCheck() {
		return false;
	}

	public TypeBinary(TreeMap<String, Type> types) {
		types.put(getName(), this);
	}
}