
using System;
using System.Collections.Generic;

namespace Zeze.Gen.Types
{
	public abstract class Type
	{
		public abstract Type Compile(ModuleSpace space, string key, string value);
		public abstract void Depends(HashSet<Type> includes);
		public abstract void Accept(Visitor visitor);

		public abstract string Name { get; }

		public abstract bool IsImmutable { get; } // most for deep copy.
		public string Kind { get; protected set; } = "";
		public bool IsBean => Kind.Equals("bean") || Kind.Equals("beankey");
		public virtual bool IsKeyable => IsImmutable; // most for table.key, cbean.
		public bool IsRocks => Kind.Equals("rocks");
		public bool IsNormalBean => Kind.Equals("bean"); // 普通的bean，不是beankey
		public bool IsNormalBeanOrRocks => IsRocks || IsNormalBean;

		public virtual bool IsCollection => false;
		public abstract bool IsNeedNegativeCheck { get; }
		public override string ToString()
		{
			return Name;
		}

		/////////////////////////////////////////////////////////////////////////////
		public static SortedDictionary<string, Type> Types { get; private set; } = new SortedDictionary<string, Type>();

		public static void Add(global::Zeze.Gen.ModuleSpace space, Type type)
		{
            string fullName = space.Path(".", type.Name);

			if (Types.ContainsKey(fullName))
				throw new Exception("duplicate type: " + fullName);

			Types.Add(fullName, type);
		}

		public static Type Compile(global::Zeze.Gen.ModuleSpace space, string name)
		{
			return Compile(space, name, null, null);
		}

		public static Type Compile(ModuleSpace space, string name, string key, string value)
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
			new TypeBool(Types);
			new TypeByte(Types);
			new TypeShort(Types);
			new TypeInt(Types);
			new TypeLong(Types);

			new TypeFloat(Types);
			new TypeDouble(Types);

			new TypeBinary(Types);
			new TypeString(Types);

			new TypeMap(Types);
			new TypeList(Types);
			new TypeSet(Types);

			new TypeDynamic(Types);
		}
	}
}
