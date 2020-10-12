using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class CreateChangeVariableCollector : Types.Visitor
    {
        public string ChangeVariableCollectorName { get; private set; }
        private Types.Variable var;

        public static void Make(System.IO.StreamWriter sw, string prefix, Types.Bean bean)
        {
            sw.WriteLine(prefix + "public override Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    switch (variableId)");
            sw.WriteLine(prefix + "    {");
            foreach (var v in bean.Variables)
            {
                CreateChangeVariableCollector vistor = new CreateChangeVariableCollector(v);
                v.VariableType.Accept(vistor);
                sw.WriteLine(prefix + "    case " + v.Id + ": return new " + vistor .ChangeVariableCollectorName + "();");
            }
            sw.WriteLine("            }");
            sw.WriteLine("            return null;");
            sw.WriteLine("        }");
            sw.WriteLine();
        }

        CreateChangeVariableCollector(Types.Variable var)
        {
            this.var = var;
        }

        void Visitor.Visit(Bean type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(BeanKey type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeByte type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeShort type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeInt type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeLong type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeBool type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeBinary type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeString type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeFloat type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeDouble type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeList type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }

        void Visitor.Visit(TypeSet type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorSet";
        }

        void Visitor.Visit(TypeMap type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorMap";
        }

        void Visitor.Visit(TypeDynamic type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged";
        }
    }
}
