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
            Name = "Helper.BEAN";
        }

        void Visitor.Visit(BeanKey type)
        {
            Name = "Helper.BEAN";
        }

        void Visitor.Visit(TypeByte type)
        {
            Name = "Helper.BYTE";
        }

        void Visitor.Visit(TypeShort type)
        {
            Name = "Helper.SHORT";
        }

        void Visitor.Visit(TypeInt type)
        {
            Name = "Helper.INT";
        }

        void Visitor.Visit(TypeLong type)
        {
            Name = "Helper.LONG";
        }

        void Visitor.Visit(TypeBool type)
        {
            Name = "Helper.BOOL";
        }

        void Visitor.Visit(TypeBinary type)
        {
            Name = "Helper.BYTES";
        }

        void Visitor.Visit(TypeString type)
        {
            Name = "Helper.STRING";
        }

        void Visitor.Visit(TypeFloat type)
        {
            Name = "Helper.FLOAT";
        }

        void Visitor.Visit(TypeDouble type)
        {
            Name = "Helper.DOUBLE";
        }

        void Visitor.Visit(TypeList type)
        {
            Name = "Helper.LIST";
        }

        void Visitor.Visit(TypeSet type)
        {
            Name = "Helper.SET";
        }

        void Visitor.Visit(TypeMap type)
        {
            Name = "Helper.MAP";
        }

        void Visitor.Visit(TypeDynamic type)
        {
            Name = "Helper.DYNAMIC";
        }
    }
}
