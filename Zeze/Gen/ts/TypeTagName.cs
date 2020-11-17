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

        void Visitor.Visit(Bean type)
        {
            Name = "Zeze.ByteBuffer.BEAN";
        }

        void Visitor.Visit(BeanKey type)
        {
            Name = "Zeze.ByteBuffer.BEAN";
        }

        void Visitor.Visit(TypeByte type)
        {
            Name = "Zeze.ByteBuffer.BYTE";
        }

        void Visitor.Visit(TypeShort type)
        {
            Name = "Zeze.ByteBuffer.SHORT";
        }

        void Visitor.Visit(TypeInt type)
        {
            Name = "Zeze.ByteBuffer.INT";
        }

        void Visitor.Visit(TypeLong type)
        {
            Name = "Zeze.ByteBuffer.LONG";
        }

        void Visitor.Visit(TypeBool type)
        {
            Name = "Zeze.ByteBuffer.BOOL";
        }

        void Visitor.Visit(TypeBinary type)
        {
            Name = "Zeze.ByteBuffer.BYTES";
        }

        void Visitor.Visit(TypeString type)
        {
            Name = "Zeze.ByteBuffer.STRING";
        }

        void Visitor.Visit(TypeFloat type)
        {
            Name = "Zeze.ByteBuffer.FLOAT";
        }

        void Visitor.Visit(TypeDouble type)
        {
            Name = "Zeze.ByteBuffer.DOUBLE";
        }

        void Visitor.Visit(TypeList type)
        {
            Name = "Zeze.ByteBuffer.LIST";
        }

        void Visitor.Visit(TypeSet type)
        {
            Name = "Zeze.ByteBuffer.SET";
        }

        void Visitor.Visit(TypeMap type)
        {
            Name = "Zeze.ByteBuffer.MAP";
        }

        void Visitor.Visit(TypeDynamic type)
        {
            Name = "Zeze.ByteBuffer.DYNAMIC";
        }
    }
}
