using System;
using System.Collections.Generic;

namespace Zeze.Gen.Types
{
	public class TypeMap : Type
	{
		public Type KeyType { get; private set; }
		public Type ValueType { get; private set; }

		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override Type Compile(ModuleSpace space, string key, string value, Variable var)
		{
			return new TypeMap(space, key, value, var);
		}

		public override string Name => "map";

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
        
		private TypeMap(ModuleSpace space, string key, string value, Variable var)
		{
			Variable = var;
			if (key.Length == 0)
				throw new Exception("map type need a key");
			if (value.Length == 0)
				throw new Exception("map type need a value");

			KeyType = Type.Compile(space, key, null, null, var);
			if (!KeyType.IsKeyable)
				throw new Exception("map key need a keyable type");
			ValueType = Type.Compile(space, value, null, null, var);

			if (ValueType is Bean b)
				b.MapKeyTypes.Add(KeyType);
			//else if (ValueType is TypeDynamic d)
			//	d.MapKeyTypes.Add(KeyType);

			//if (ValueType is TypeBinary)
			//	throw new Exception(Name + " Error : value type is binary.");
		}

		internal TypeMap(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}

        public override bool IsImmutable => false;
		public override bool IsCollection => true;
		public override bool IsJavaPrimitive => false;

		private bool? isNeedNegativeCheckCache = null;
		public override bool IsNeedNegativeCheck
		{
			get
			{
				if (isNeedNegativeCheckCache != null)
					return isNeedNegativeCheckCache.Value;
				isNeedNegativeCheckCache = false;
				if (ValueType.IsNeedNegativeCheck)
					return true;
				isNeedNegativeCheckCache = null;
                return false;
            }
        }
	}
}
