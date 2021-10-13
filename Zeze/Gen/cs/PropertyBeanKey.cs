using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class PropertyBeanKey : Visitor
    {
        System.IO.StreamWriter sw;
        Types.Variable var;
        string prefix;

        public static void Make(Types.BeanKey bean, System.IO.StreamWriter sw, string prefix)
        {
            foreach (Types.Variable var in bean.Variables)
            {
                var.VariableType.Accept(new PropertyBeanKey(sw, var, prefix));
            }
        }

        public PropertyBeanKey(System.IO.StreamWriter sw, Types.Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
            throw new NotImplementedException();
        }

        private void WriteProperty(Types.Type type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.Getter + "{");
            sw.WriteLine(prefix + "    return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(BeanKey type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeByte type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDouble type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeInt type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeLong type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeBool type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeBinary type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeString type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeList type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeSet type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeMap type)
        {
            throw new NotImplementedException();
        }

        public void Visit(TypeFloat type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeShort type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDynamic type)
        {
            throw new NotImplementedException();
        }
    }
}
