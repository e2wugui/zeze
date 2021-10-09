package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public abstract class TypeCollection extends Type {
	private Type ValueType;
	public final Type getValueType() {
		return ValueType;
	}
	protected final void setValueType(Type value) {
		ValueType = value;
	}

	@Override
	public void Depends(HashSet<Type> includes) {
		if (includes.add(this)) {
			getValueType().Depends(includes);
		}
	}

	protected final void _compile(ModuleSpace space, String key, String value) {
		if (key != null && key.length() > 0) {
			throw new RuntimeException(Name + " type does not need a key. " + key);
		}

		setValueType(Type.Compile(space, value, null, null));
		if (getValueType() instanceof TypeBinary) {
			throw new RuntimeException(Name + " Error : value type is binary.");
		}
		if (getValueType() instanceof TypeDynamic) {
			throw new RuntimeException(Name + " Error : value type is dynamic.");
		}
	}

	@Override
	public boolean isImmutable() {
		return false;
	}
	@Override
	public boolean isCollection() {
		return true;
	}
}