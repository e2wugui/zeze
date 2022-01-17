﻿using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class TypeTagName : Visitor
    {
        public string Name { get; private set; }
 
        public static string GetName(Type type)
        {
            TypeTagName v = new TypeTagName();
            type.Accept(v);
            return v.Name;
        }

        void Visitor.Visit(TypeBool type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        void Visitor.Visit(TypeByte type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        void Visitor.Visit(TypeShort type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        void Visitor.Visit(TypeInt type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        void Visitor.Visit(TypeLong type)
        {
            Name = "ByteBuffer.INTEGER";
        }

        void Visitor.Visit(TypeFloat type)
        {
            Name = "ByteBuffer.FLOAT";
        }

        void Visitor.Visit(TypeDouble type)
        {
            Name = "ByteBuffer.DOUBLE";
        }

        void Visitor.Visit(TypeBinary type)
        {
            Name = "ByteBuffer.BYTES";
        }

        void Visitor.Visit(TypeString type)
        {
            Name = "ByteBuffer.BYTES";
        }

        void Visitor.Visit(TypeList type)
        {
            Name = "ByteBuffer.LIST";
        }

        void Visitor.Visit(TypeSet type)
        {
            Name = "ByteBuffer.LIST";
        }

        void Visitor.Visit(TypeMap type)
        {
            Name = "ByteBuffer.MAP";
        }

        void Visitor.Visit(Bean type)
        {
            Name = "ByteBuffer.BEAN";
        }

        void Visitor.Visit(BeanKey type)
        {
            Name = "ByteBuffer.BEAN";
        }

        void Visitor.Visit(TypeDynamic type)
        {
            Name = "ByteBuffer.DYNAMIC";
        }
    }
}
