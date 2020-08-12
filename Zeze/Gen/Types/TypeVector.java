package rpcgen.types;

import java.util.*;
import rpcgen.Namespace;

public class TypeVector extends TypeCollection {
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public boolean isComparable() {
		return false;
	}

	public Type compile(Namespace space, String key, String value) {
		return new TypeVector(space, key, value);
	}

	private TypeVector(Namespace space, String key, String value) {
		_compile(space, key, value);
	}

	TypeVector(Map<String, Type> types) {
		types.put(getName(), this);
	}

	@Override
	public String getName() {
		return "vector";
	}

}
