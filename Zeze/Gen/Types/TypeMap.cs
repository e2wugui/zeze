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

		public override Type Compile(ModuleSpace space, String key, String value)
		{
			return new TypeMap(space, key, value);
		}

		public override String Name => "map";

		public override void Depends(HashSet<Type> includes)
		{
			if (includes.Add(this))
			{
				KeyType.Depends(includes);
				ValueType.Depends(includes);
			}
		}

		private TypeMap(global::Zeze.Gen.ModuleSpace space, String key, String value)
		{
			if (key.Length == 0)
				throw new Exception("map type need a key");
			if (value.Length == 0)
				throw new Exception("map type need a value");

			KeyType = Type.Compile(space, key, null, null);
			if (!KeyType.IsKeyable)
				throw new Exception("map key need a keyable type");
			ValueType = Type.Compile(space, value, null, null);
		}

		internal TypeMap(SortedDictionary<String, Type> types)
		{
			types.Add(Name, this);
		}

        public override bool IsImmutable => false;
		public override bool IsCollection => true;
		public override bool IsNeedNegativeCheck => ValueType.IsNeedNegativeCheck;

	}
}
