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

        public void Visit(Bean type)
        {
            Value = "null";
        }

        public void Visit(BeanKey type)
        {
            Value = "null";
        }

        public void Visit(TypeByte type)
        {
            SetDefaultValue("0");
        }

        public void Visit(TypeShort type)
        {
            SetDefaultValue("0");
        }

        public void Visit(TypeInt type)
        {
            Value = "null";
        }

        public void Visit(TypeLong type)
        {
            SetDefaultValue("0n");
        }

        public void Visit(TypeBool type)
        {
            SetDefaultValue("false");
        }

        public void Visit(TypeBinary type)
        {
            Value = "null";
        }

        public void Visit(TypeString type)
        {
            SetDefaultValue("\"\"");
        }

        public void Visit(TypeFloat type)
        {
            SetDefaultValue("0");
        }

        public void Visit(TypeDouble type)
        {
            SetDefaultValue("0");
        }

        public void Visit(TypeList type)
        {
            Value = "null";
        }

        public void Visit(TypeSet type)
        {
            Value = "null";
        }

        public void Visit(TypeMap type)
        {
            Value = "null";
        }

        public void Visit(TypeDynamic type)
        {
            Value = "null";
        }
    }
}
