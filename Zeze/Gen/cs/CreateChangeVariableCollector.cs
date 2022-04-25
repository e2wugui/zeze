using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class CreateChangeVariableCollector : Visitor
    {
        public string ChangeVariableCollectorName { get; private set; }
        // Variable var;

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
        }

        CreateChangeVariableCollector()
        {
        }

        public void Visit(Bean type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(BeanKey type)
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

        public void Visit(TypeBool type)
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

        public void Visit(TypeFloat type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeDouble type)
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
            string kv = TypeName.GetName(type.KeyType) + ", " + TypeName.GetName(type.ValueType);
            string factory = type.ValueType.IsNormalBean
                ? "() => new Zeze.Transaction.ChangeNoteMap2<" + kv + ">(null)"
                : "() => new Zeze.Transaction.ChangeNoteMap1<" + kv + ">(null)";
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorMap(" + factory + ")";
        }

        public void Visit(TypeDynamic type)
        {
            ChangeVariableCollectorName = "Zeze.Transaction.ChangeVariableCollectorChanged()";
        }

        public void Visit(TypeQuaternion type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector2Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3 type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector3Int type)
        {
            throw new System.NotImplementedException();
        }

        public void Visit(TypeVector4 type)
        {
            throw new System.NotImplementedException();
        }
    }
}
