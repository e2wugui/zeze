
using System;
using System.Collections.Generic;

namespace Zeze.Gen.Types
{
	public abstract class Type
	{
		public abstract Type Compile(ModuleSpace space, string key, string value, Variable var);
		public abstract void Depends(HashSet<Type> includes, string parent);
		public abstract void DependsIncludesNoRecursive(HashSet<Type> includes);

        public abstract void Accept(Visitor visitor);

		public abstract string Name { get; }

		public abstract bool IsImmutable { get; } // most for deep copy.
		public virtual bool IsJavaPrimitive => true;
		public string Kind { get; protected set; } = "";
		public bool IsBean => Kind.Equals("bean") || Kind.Equals("beankey");
		public virtual bool IsKeyable => IsImmutable; // most for table.key, cbean.
		public bool IsRocks => Kind.Equals("rocks");
		public bool IsNormalBean => Kind.Equals("bean") || Kind.Equals("dynamic"); // 普通的bean，不是beankey
		public bool IsNormalBeanOrRocks => IsRocks || IsNormalBean;

		public virtual bool IsCollection => false;
		public abstract bool IsNeedNegativeCheck { get; }
		public override string ToString()
		{
			return Name;
		}
		public Variable Variable { get; protected set; }

		/////////////////////////////////////////////////////////////////////////////
		public static SortedDictionary<string, Type> Types { get; } = new SortedDictionary<string, Type>();

		public virtual void DetectCircle(HashSet<Type> circle)
        {
        }

		public static void Add(global::Zeze.Gen.ModuleSpace space, Type type)
		{
            string fullName = space.Path(".", type.Name);

			if (Types.ContainsKey(fullName))
				throw new Exception("duplicate type: " + fullName);

			Types.Add(fullName, type);
		}

		public static Type Compile(string name)
		{
			if (Types.TryGetValue(name, out var type))
				return type;
			throw new Exception("simple type compile not found=" + name);
		}

		public static Type Compile(global::Zeze.Gen.ModuleSpace space, string name)
		{
			return Compile(space, name, null, null, null);
		}

		public static Type Compile(ModuleSpace space, string name, string key, string value, Variable var)
		{
			Type type;

			if (Types.TryGetValue(name, out type))
			{
				return type.Compile(space, key, value, var);
			}

			if (false == Program.IsFullName(name))
			{
				name = space.Path(".", name);
				if (Types.TryGetValue(name, out type))
				{
					return type.Compile(space, key, value, var);
				}
			}

			throw new Exception("type NOT FOUND! '" + name + "'" + key + "." + value);
		}

		static Type()
		{
			// 内置类型在此显式注册（key 取自各类型的 Name），不依赖构造函数副作用。
			static void Reg(Type t) => Types.Add(t.Name, t);

			Reg(new TypeBool());
			Reg(new TypeByte());
			Reg(new TypeShort());
			Reg(new TypeInt());
			Reg(new TypeLong());

			Reg(new TypeFloat());
			Reg(new TypeDouble());

			Reg(new TypeBinary());
			Reg(new TypeString());

			Reg(new TypeMap());
			Reg(new TypeList());
			Reg(new TypeArray());
			Reg(new TypeSet());

			Reg(new TypeDynamic());

			Reg(new TypeQuaternion());
			Reg(new TypeVector2());
			Reg(new TypeVector2Int());
			Reg(new TypeVector3());
			Reg(new TypeVector3Int());
			Reg(new TypeVector4());

			Reg(new TypeDecimal());

			Reg(new TypeGTable());
		}
	}
}
