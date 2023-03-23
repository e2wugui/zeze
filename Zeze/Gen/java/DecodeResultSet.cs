using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class DecodeResultSet : Visitor
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {");
            sw.WriteLine(prefix + "    var _parents_name_ = parentsToName(parents);");
            foreach (Variable v in bean.Variables)
            {
                v.VariableType.Accept(new DecodeResultSet(v, v.Id, "rs", sw, $"{prefix}    "));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {");
            sw.WriteLine(prefix + "    var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);");
            foreach (Variable v in bean.Variables)
            {
                v.VariableType.Accept(new DecodeResultSet(v, v.Id, "rs", sw, $"{prefix}    "));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        Variable var;
        int id;
        string bb;
        StreamWriter sw;
        string prefix;

        public DecodeResultSet(Variable var, int id, string bb, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.id = id;
            this.bb = bb;
            this.sw = sw;
            this.prefix = prefix;
        }

        string AssignText(string value)
        {
            return var.Bean.IsNormalBean ? var.Setter(value) : $"{var.NamePrivate} = {value}";
        }

        public void Visit(TypeBool type)
        {
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getBoolean(_parents_name_ + \"{var.Name}\")")};");
        }

        public void Visit(TypeByte type)
        {
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getByte(_parents_name_ + \"{var.Name}\")")};");
        }

        public void Visit(TypeShort type)
        {
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getShort(_parents_name_ + \"{var.Name}\")")};");
        }

        public void Visit(TypeInt type)
        {
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getInt(_parents_name_ + \"{var.Name}\")")};");
        }

        public void Visit(TypeLong type)
        {
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getLong(_parents_name_ + \"{var.Name}\")")};");
        }

        public void Visit(TypeFloat type)
        {
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getFloat(_parents_name_ + \"{var.Name}\")")};");
        }

        public void Visit(TypeDouble type)
        {
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getDouble(_parents_name_ + \"{var.Name}\")")};");
        }

        public void Visit(TypeBinary type)
        {
            sw.WriteLine($"{prefix}{AssignText($"new Zeze.Net.Binary({bb}.getBytes(_parents_name_ + \"{var.Name}\"))")};");
        }

        public void Visit(TypeString type)
        {
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getString(_parents_name_ + \"{var.Name}\")")};");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine($"{prefix}// todo decodeJsonList");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine($"{prefix}// todo decodeJsonSet");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine($"{prefix}// todo decodeJsonMap");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{var.Getter}.decodeResultSet(parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{var.Getter}.decodeResultSet(parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}// todo decodeJsonDynamic");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeQuaternion(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector2(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector2Int(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector3(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector3Int(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector4(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

    }
}
