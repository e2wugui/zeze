using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class CreateChangeVariableCollector : Visitor
    {
        public string ChangeVariableCollectorName { get; private set; }
        //private Variable var;

        public static void Make(StreamWriter sw, string prefix, Bean bean)
        {
            sw.WriteLine(prefix + "public override Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    return variableId switch");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        0 => new Zeze.Transaction.ChangeVariableCollectorChanged(),");
            foreach (var v in bean.Variables)
            {
                CreateChangeVariableCollector vistor = new();
                v.VariableType.Accept(vistor);
                sw.WriteLine(prefix + "        " + v.Id + " => new " + vistor .ChangeVariableCollectorName + ",");
            }
            sw.WriteLine(prefix + "        _ => null,");
            sw.WriteLine(prefix + "    };");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        CreateChangeVariableCollector()
        {
        }

        void Visitor.Visit(Bean type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(BeanKey type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeByte type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeShort type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeInt type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeLong type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeBool type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeBinary type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeString type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeFloat type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeDouble type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeList type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        void Visitor.Visit(TypeSet type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorSet()";
        }

        void Visitor.Visit(TypeMap type)
        {
            string kv = TypeName.GetName(type.KeyType) + ", " + TypeName.GetName(type.ValueType);
            string factory = type.ValueType.IsNormalBean
                ? "() => new Zeze.Transaction.ChangeNoteMap2<" + kv + ">(null)"
                : "() => new Zeze.Transaction.ChangeNoteMap1<" + kv + ">(null)";
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorMap(" + factory + ")";
        }

        void Visitor.Visit(TypeDynamic type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }
    }
}
