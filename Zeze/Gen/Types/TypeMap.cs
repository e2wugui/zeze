using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

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

		public override void Depends(HashSet<Type> includes)
		{
			if (includes.Add(this))
			{
				KeyType.Depends(includes);
				ValueType.Depends(includes);
			}
		}

		private TypeMap(global::Zeze.Gen.ModuleSpace space, string key, string value, Variable var)
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

			if (ValueType.IsNormalBeanOrRocks)
				(ValueType as Bean).MapKeyTypes.Add(KeyType);

			//if (ValueType is TypeBinary)
			//	throw new Exception(Name + " Error : value type is binary.");
		}

		internal TypeMap(SortedDictionary<string, Type> types)
		{
			types.Add(Name, this);
		}

        public override bool IsImmutable => false;
		public override bool IsCollection => true;
		public override bool IsNeedNegativeCheck => ValueType.IsNeedNegativeCheck;

	}
}
