using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
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

        void Visitor.Visit(Bean type)
        {
            Name = "ByteBuffer.BEAN";
        }

        void Visitor.Visit(BeanKey type)
        {
            Name = "ByteBuffer.BEAN";
        }

        void Visitor.Visit(TypeByte type)
        {
            Name = "ByteBuffer.BYTE";
        }

        void Visitor.Visit(TypeShort type)
        {
            Name = "ByteBuffer.SHORT";
        }

        void Visitor.Visit(TypeInt type)
        {
            Name = "ByteBuffer.INT";
        }

        void Visitor.Visit(TypeLong type)
        {
            Name = "ByteBuffer.LONG";
        }

        void Visitor.Visit(TypeBool type)
        {
            Name = "ByteBuffer.BOOL";
        }

        void Visitor.Visit(TypeBinary type)
        {
            Name = "ByteBuffer.BYTES";
        }

        void Visitor.Visit(TypeString type)
        {
            Name = "ByteBuffer.STRING";
        }

        void Visitor.Visit(TypeFloat type)
        {
            Name = "ByteBuffer.FLOAT";
        }

        void Visitor.Visit(TypeDouble type)
        {
            Name = "ByteBuffer.DOUBLE";
        }

        void Visitor.Visit(TypeList type)
        {
            Name = "ByteBuffer.LIST";
        }

        void Visitor.Visit(TypeSet type)
        {
            Name = "ByteBuffer.SET";
        }

        void Visitor.Visit(TypeMap type)
        {
            Name = "ByteBuffer.MAP";
        }

        void Visitor.Visit(TypeDynamic type)
        {
            Name = "ByteBuffer.DYNAMIC";
        }
    }
}
