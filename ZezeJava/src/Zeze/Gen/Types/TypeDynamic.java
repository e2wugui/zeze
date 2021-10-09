package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class TypeDynamic extends Type {
	@Override
	public String getName() {
		return "dynamic";
	}

	@Override
	public boolean isImmutable() {
		return false;
	}
	@Override
	public boolean isBean() {
		return false;
	}
	@Override
	public boolean isNeedNegativeCheck() {
		for (var v : getRealBeans().values()) {
			if (v.IsNeedNegativeCheck) {
				return true;
			}
		}
		return false;
	}

	private TreeMap<Long, Bean> RealBeans = new TreeMap<Long, Bean> ();
	public final TreeMap<Long, Bean> getRealBeans() {
		return RealBeans;
	}
	private int SpecialCount;
	public final int getSpecialCount() {
		return SpecialCount;
	}

	@Override
	public void Accept(Visitor visitor) {
		visitor.Visit(this);
	}

	@Override
	public Type Compile(ModuleSpace space, String key, String value) {
		if (key != null && key.length() > 0) {
			throw new RuntimeException(getName() + " type does not need a key. " + key);
		}
		return new TypeDynamic(space, value);
	}

	// value=BeanName[:SpecialTypeId],BeanName2[:SpecialTypeId2]
	// 如果指定特别的TypeId，必须全部都指定。虽然部分指定也可以处理，感觉这样不大好。
	private TypeDynamic(ModuleSpace space, String value) {
		for (var beanWithSpecialTypeId : value.split("[,]", -1)) {
			if (beanWithSpecialTypeId.Length == 0) { // empty
				continue;
			}
			var beanWithSpecialTypeIdArray = beanWithSpecialTypeId.Split("[:]", -1);
			if (beanWithSpecialTypeIdArray.Length == 0) {
				continue;
			}
			Type type = Type.Compile(space, beanWithSpecialTypeIdArray[0], null, null);
			if (false == type.isNormalBean()) {
				throw new RuntimeException("dynamic only support normal bean");
			}
			Bean bean = type instanceof Bean ? (Bean)type : null;
			long specialTypeId = bean.getTypeId(); // default
			if (beanWithSpecialTypeIdArray.Length > 1) {
				SpecialCount = getSpecialCount() + 1;
				specialTypeId = Long.parseLong(beanWithSpecialTypeIdArray[1]);
				if (specialTypeId <= 0) {
					throw new RuntimeException("SpecialTypeId <= 0 is reserved");
				}
			}
			getRealBeans().put(specialTypeId, bean);
		}

		if (getSpecialCount() == 0) { // 没有配置特别的TypeId，全部使用Bean本身的TypeId。
			return;
		}

		if (getRealBeans().isEmpty()) { // 动态类型没有配置任何具体的Bean。允许。
			return;
		}

		if (getSpecialCount() != getRealBeans().size()) {
			throw new RuntimeException("dynamic setup special TypeId，But Not All.");
		}
	}

	@Override
	public void Depends(HashSet<Type> includes) {
		if (includes.add(this)) {
			for (var bean : getRealBeans().values()) {
				bean.Depends(includes);
			}
		}
	}

	public TypeDynamic(TreeMap<String, Type> types) {
		types.put(getName(), this);
	}
}