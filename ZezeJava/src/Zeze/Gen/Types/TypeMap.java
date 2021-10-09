package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class TypeMap extends Type {
	private Type KeyType;
	public final Type getKeyType() {
		return KeyType;
	}
	private void setKeyType(Type value) {
		KeyType = value;
	}
	private Type ValueType;
	public final Type getValueType() {
		return ValueType;
	}
	private void setValueType(Type value) {
		ValueType = value;
	}

	@Override
	public void Accept(Visitor visitor) {
		visitor.Visit(this);
	}

	@Override
	public Type Compile(ModuleSpace space, String key, String value) {
		return new TypeMap(space, key, value);
	}

	@Override
	public String getName() {
		return "map";
	}

	@Override
	public void Depends(HashSet<Type> includes) {
		if (includes.add(this)) {
			getKeyType().Depends(includes);
			getValueType().Depends(includes);
		}
	}

	private TypeMap(Zeze.Gen.ModuleSpace space, String key, String value) {
		if (key.length() == 0) {
			throw new RuntimeException("map type need a key");
		}
		if (value.length() == 0) {
			throw new RuntimeException("map type need a value");
		}

		setKeyType(Type.Compile(space, key, null, null));
		if (!getKeyType().isKeyable()) {
			throw new RuntimeException("map key need a keyable type");
		}
		setValueType(Type.Compile(space, value, null, null));
		//if (ValueType is TypeBinary)
		//	throw new Exception(Name + " Error : value type is binary.");
		if (getValueType() instanceof TypeDynamic) {
			throw new RuntimeException(getName() + " Error : value type is dynamic.");
		}
	}

	public TypeMap(TreeMap<String, Type> types) {
		types.put(getName(), this);
	}

	@Override
	public boolean isImmutable() {
		return false;
	}
	@Override
	public boolean isCollection() {
		return true;
	}
	@Override
	public boolean isNeedNegativeCheck() {
		return getValueType().isNeedNegativeCheck();
	}

}