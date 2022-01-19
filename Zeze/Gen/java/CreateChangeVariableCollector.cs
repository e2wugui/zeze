using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class CreateChangeVariableCollector : Visitor
    {
        public string ChangeVariableCollectorName { get; private set; }
        // Variable var;

        public static void Make(StreamWriter sw, string prefix, Bean bean)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {");
            sw.WriteLine(prefix + "    switch (variableId) {");
            sw.WriteLine(prefix + "        case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();");
            foreach (var v in bean.Variables)
            {
                CreateChangeVariableCollector vistor = new();
                v.VariableType.Accept(vistor);
                sw.WriteLine(prefix + "        case " + v.Id + ": return new " + vistor.ChangeVariableCollectorName + ";");
            }
            sw.WriteLine(prefix + "        default: return null;");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
        }

        CreateChangeVariableCollector()
        {
        }

        public void Visit(TypeBool type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeByte type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeShort type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeInt type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeLong type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeFloat type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeDouble type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeBinary type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeString type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeList type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeSet type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorSet()";
        }

        public void Visit(TypeMap type)
        {
            string kv = BoxingName.GetBoxingName(type.KeyType) + ", " + BoxingName.GetBoxingName(type.ValueType);
            string factory = type.ValueType.IsNormalBean
                ? "() -> new Zeze.Transaction.ChangeNoteMap2<" + kv + ">(null)"
                : "() -> new Zeze.Transaction.ChangeNoteMap1<" + kv + ">(null)";
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorMap(" + factory + ")";
        }

        public void Visit(Bean type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(BeanKey type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeDynamic type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }
    }
}
