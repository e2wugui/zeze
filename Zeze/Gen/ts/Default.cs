using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.ts
{
    public class Default : Types.Visitor
    {
        private Types.Variable variable;
        public string Value { get; private set; }

        public static string GetDefault(Types.Variable var)
        {
            Default def = new Default(var);
            var.VariableType.Accept(def);
            return def.Value;
        }

        private Default(Types.Variable var)
        {
            this.variable = var;
        }

        private void SetDefaultValue(string def)
        {
            Value = (variable.Initial.Length > 0) ? variable.Initial : def;
        }

        void Visitor.Visit(Bean type)
        {
            Value = "null";
        }

        void Visitor.Visit(BeanKey type)
        {
            Value = "null";
        }

        void Visitor.Visit(TypeByte type)
        {
            SetDefaultValue("0");
        }

        void Visitor.Visit(TypeShort type)
        {
            SetDefaultValue("0");
        }

        void Visitor.Visit(TypeInt type)
        {
            Value = "null";
        }

        void Visitor.Visit(TypeLong type)
        {
            SetDefaultValue("0n");
        }

        void Visitor.Visit(TypeBool type)
        {
            SetDefaultValue("false");
        }

        void Visitor.Visit(TypeBinary type)
        {
            Value = "null";
        }

        void Visitor.Visit(TypeString type)
        {
            SetDefaultValue("\"\"");
        }

        void Visitor.Visit(TypeFloat type)
        {
            SetDefaultValue("0");
        }

        void Visitor.Visit(TypeDouble type)
        {
            SetDefaultValue("0");
        }

        void Visitor.Visit(TypeList type)
        {
            Value = "null";
        }

        void Visitor.Visit(TypeSet type)
        {
            Value = "null";
        }

        void Visitor.Visit(TypeMap type)
        {
            Value = "null";
        }

        void Visitor.Visit(TypeDynamic type)
        {
            Value = "null";
        }
    }
}
