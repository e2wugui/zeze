namespace Zeze.Gen.Types
{
    public interface Visitor
    {
        public void Visit(TypeBool type);    // 序列化类型: 0
        public void Visit(TypeByte type);    // 序列化类型: 0
        public void Visit(TypeShort type);   // 序列化类型: 0
        public void Visit(TypeInt type);     // 序列化类型: 0
        public void Visit(TypeLong type);    // 序列化类型: 0
        public void Visit(TypeFloat type);   // 序列化类型: 1
        public void Visit(TypeDouble type);  // 序列化类型: 2
        public void Visit(TypeBinary type);  // 序列化类型: 3
        public void Visit(TypeString type);  // 序列化类型: 3
        public void Visit(TypeList type);    // 序列化类型: 4
        public void Visit(TypeSet type);     // 序列化类型: 4
        public void Visit(TypeMap type);     // 序列化类型: 5
        public void Visit(Bean type);        // 序列化类型: 6
        public void Visit(BeanKey type);     // 序列化类型: 6
        public void Visit(TypeDynamic type); // 序列化类型: 7

        public void Visit(TypeVector2 type);    // 序列化类型: 8
        public void Visit(TypeVector2Int type); // 序列化类型: 9
        public void Visit(TypeVector3 type);    // 序列化类型: 10
        public void Visit(TypeVector3Int type); // 序列化类型: 11
        public void Visit(TypeVector4 type);    // 序列化类型: 12
        public void Visit(TypeQuaternion type); // 序列化类型: 12

        public void Visit(TypeDecimal type); // 序列化类型: 3 使用String系列化
    }
}
