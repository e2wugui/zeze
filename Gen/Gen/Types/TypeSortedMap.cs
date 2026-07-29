using System;
using System.Collections.Generic;

namespace Zeze.Gen.Types
{
	public class TypeSortedMap : Type
	{
		public Type KeyType { get; }
		public Type ValueType { get; }

		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override Type Compile(ModuleSpace space, string key, string value, Variable var)
		{
			return new TypeSortedMap(space, key, value, var);
		}

		public override string Name => "sortedmap";

		public override void Depends(HashSet<Type> includes, string parent)
		{
			if (includes.Add(this))
			{
				KeyType.Depends(includes, parent);
				ValueType.Depends(includes, parent);
			}
		}

		public override void DependsIncludesNoRecursive(HashSet<Type> includes)
		{
			if (includes.Add(this))
			{
				KeyType.DependsIncludesNoRecursive(includes);
				ValueType.DependsIncludesNoRecursive(includes);
			}
		}

		private TypeSortedMap(ModuleSpace space, string key, string value, Variable var)
		{
			Variable = var;
			if (key.Length == 0)
				throw new Exception("sortedmap type need a key");
			if (value.Length == 0)
				throw new Exception("sortedmap type need a value");

			KeyType = Type.Compile(space, key, null, null, var);
			if (!KeyType.IsKeyable)
				throw new Exception("sortedmap key need a keyable type");
			ValueType = Type.Compile(space, value, null, null, var);

			if (ValueType is Bean b)
				b.MapKeyTypes.Add(KeyType);
		}

		internal TypeSortedMap()
		{
		}

		public override bool IsImmutable => false;
		public override bool IsCollection => true;
		public override bool IsJavaPrimitive => false;
		public override bool IsNeedNegativeCheck => ValueType.IsNeedNegativeCheck;
	}
}
