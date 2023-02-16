using System;
using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
	class BoxingName : TypeName
	{
		public static string GetBoxingName(Types.Type type)
		{
			BoxingName visitor = new();
			type.Accept(visitor);
			return visitor.name;
		}

		public override void Visit(TypeBool type)
        {
			name = "Boolean";
        }

		public override void Visit(TypeByte type)
		{
			name = "Byte";
		}

		public override void Visit(TypeShort type)
		{
			name = "Short";
		}

		public override void Visit(TypeInt type)
		{
			name = "Integer";
		}

		public override void Visit(TypeLong type)
		{
			name = "Long";
		}

		public override void Visit(TypeFloat type)
		{
			name = "Float";
		}

        public override void Visit(TypeDouble type)
        {
            name = "Double";
        }

        public override void Visit(TypeList type)
		{
			throw new NotImplementedException();
		}

		public override void Visit(TypeSet type)
		{
			throw new NotImplementedException();
		}

		public override void Visit(TypeMap type)
		{
			throw new NotImplementedException();
		}
    }
}
