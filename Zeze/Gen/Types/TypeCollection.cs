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

		public override bool IsImmutable => false;
		public override bool IsCollection => true;
	}
}
