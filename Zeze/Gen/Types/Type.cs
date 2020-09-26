
using System;
using System.Collections.Generic;

namespace Zeze.Gen.Types
{
	public abstract class Type
	{
		public abstract Type Compile(ModuleSpace space, String key, String value);
		public abstract void Depends(HashSet<Type> includes);
		public abstract void Accept(Visitor visitor);

		public abstract string Name { get; }

		public abstract bool IsImmutable { get; } // most for deep copy.
		public virtual bool IsBean => false;
		public virtual bool IsKeyable => IsImmutable; // most for table.key, cbean.
		public virtual bool IsNormalBean => IsBean && !IsKeyable; // 普通的bean，不是beankey
		public virtual bool IsCollection => false;
		public abstract bool IsNeedNegativeCheck { get; }
		public override String ToString()
		{
			return Name;
		}

		/////////////////////////////////////////////////////////////////////////////
		public static SortedDictionary<String, Type> Types { get; private set; } = new SortedDictionary<String, Type>();

		public static void Add(global::Zeze.Gen.ModuleSpace space, Type type)
		{
			String fullName = space.Path(".", type.Name);

			if (Types.ContainsKey(fullName))
				throw new Exception("duplicate type: " + fullName);

			Types.Add(fullName, type);
		}

		public static Type Compile(global::Zeze.Gen.ModuleSpace space, String name)
		{
			return Compile(space, name, null, null);
		}

		public static Type Compile(ModuleSpace space, String name, String key, String value)
		{
			Type type = null;

			if (Types.TryGetValue(name, out type))
			{
				return type.Compile(space, key, value);
			}

			if (false == Program.IsFullName(name))
			{
				name = space.Path(".", name);
				if (Types.TryGetValue(name, out type))
				{
					return type.Compile(space, key, value);
				}
			}

			throw new Exception("type NOT FOUND! '" + name + "'" + key + "." + value);
		}

		static Type()
		{
			new TypeDouble(Types);
			new TypeBinary(Types);
			new TypeBool(Types);
			new TypeByte(Types);
			new TypeInt(Types);
			new TypeLong(Types);
			new TypeMap(Types);
			new TypeList(Types);
			new TypeSet(Types);
			new TypeString(Types);
			new TypeFloat(Types);
			new TypeShort(Types);
		}
	}
}
