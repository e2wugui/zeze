using System;
using System.Collections.Generic;

namespace Zeze.Gen.Types
{
	public abstract class TypeCollection : Type
	{
		public Type ValueType { get; protected set; }

		public override void Depends(HashSet<Type> includes)
		{
			if (includes.Add(this))
				ValueType.Depends(includes);
		}

		protected void _compile(ModuleSpace space, string key, string value, object param)
		{
			if (key != null && key.Length > 0)
				throw new Exception(Name + " type does not need a key. " + key);

			ValueType = Type.Compile(space, value, null, null, param);
			if (ValueType is TypeBinary)
				throw new Exception(Name + " Error : value type is binary.");
			if (ValueType is TypeDynamic)
				throw new Exception(Name + " Error : value type is dynamic.");
		}

		public override bool IsImmutable => false;
		public override bool IsCollection => true;
	}
}
