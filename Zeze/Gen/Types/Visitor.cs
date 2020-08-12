

namespace Zeze.Gen.Types
{
	public interface Visitor
	{
		public void Visit(Bean type);
		public void Visit(BeanKey type);
		public void Visit(TypeByte type);
		public void Visit(TypeDouble type);
		public void Visit(TypeInt type);
		public void Visit(TypeLong type);
		public void Visit(TypeBool type);
		public void Visit(TypeBinary type);
		public void Visit(TypeString type);
		public void Visit(TypeList type);
		public void Visit(TypeSet type);
		public void Visit(TypeMap type);
		public void Visit(TypeFloat type);
		public void Visit(TypeShort type);
	}
}
