
using System;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
	class BoxingName : TypeName
	{
		public static string GetName(Types.Type type)
		{
			BoxingName visitor = new BoxingName();
			type.Accept(visitor);
			return visitor.name;
		}

		public override void Visit(Types.TypeBool type)
        {
			name = "Boolean";
        }

		public override void Visit(Types.TypeByte type)
		{
			name = "Byte";
		}

		public override void Visit(Types.TypeShort type)
		{
			name = "Short";
		}

		public override void Visit(Types.TypeInt type)
		{
			name = "Integer";
		}

		public override void Visit(Types.TypeLong type)
		{
			name = "Long";
		}

		public override void Visit(Types.TypeFloat type)
		{
			name = "Float";
		}

		public override void Visit(Types.TypeList type)
		{
			throw new NotImplementedException();
		}

		public override void Visit(Types.TypeSet type)
		{
			throw new NotImplementedException();
		}

		public override void Visit(Types.TypeMap type)
		{
			throw new NotImplementedException();
		}

        public override void Visit(TypeDouble type)
        {
			name = "Double";
        }
    }
}
