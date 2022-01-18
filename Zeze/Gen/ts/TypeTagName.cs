using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class TypeTagName : Types.Visitor
    {
        public string Name { get; private set; }
        public static string GetName(Types.Type type)
        {
            TypeTagName v = new TypeTagName();
            type.Accept(v);
            return v.Name;
        }

        public void Visit(Bean type)
        {
            Name = "Zeze.ByteBuffer.BEAN";
        }

        public void Visit(BeanKey type)
        {
            Name = "Zeze.ByteBuffer.BEAN";
        }

        public void Visit(TypeByte type)
        {
            Name = "Zeze.ByteBuffer.BYTE";
        }

        public void Visit(TypeShort type)
        {
            Name = "Zeze.ByteBuffer.SHORT";
        }

        public void Visit(TypeInt type)
        {
            Name = "Zeze.ByteBuffer.INT";
        }

        public void Visit(TypeLong type)
        {
            Name = "Zeze.ByteBuffer.LONG";
        }

        public void Visit(TypeBool type)
        {
            Name = "Zeze.ByteBuffer.BOOL";
        }

        public void Visit(TypeBinary type)
        {
            Name = "Zeze.ByteBuffer.BYTES";
        }

        public void Visit(TypeString type)
        {
            Name = "Zeze.ByteBuffer.STRING";
        }

        public void Visit(TypeFloat type)
        {
            Name = "Zeze.ByteBuffer.FLOAT";
        }

        public void Visit(TypeDouble type)
        {
            Name = "Zeze.ByteBuffer.DOUBLE";
        }

        public void Visit(TypeList type)
        {
            Name = "Zeze.ByteBuffer.LIST";
        }

        public void Visit(TypeSet type)
        {
            Name = "Zeze.ByteBuffer.SET";
        }

        public void Visit(TypeMap type)
        {
            Name = "Zeze.ByteBuffer.MAP";
        }

        public void Visit(TypeDynamic type)
        {
            Name = "Zeze.ByteBuffer.DYNAMIC";
        }
    }
}
