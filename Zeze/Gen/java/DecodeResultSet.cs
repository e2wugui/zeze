using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class DecodeResultSet : Visitor
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {");
            var hasParentName = new bool[1];
            foreach (Variable v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new DecodeResultSet(v, v.Id, "rs", sw, $"{prefix}    ", hasParentName, false));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {");
            var hasParentName = new bool[1];
            foreach (Variable v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new DecodeResultSet(v, v.Id, "rs", sw, $"{prefix}    ", hasParentName, true));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        readonly string columnName;
        readonly string tmpvarname;
        readonly Variable var;
        readonly int id;
        readonly string bb;
        readonly StreamWriter sw;
        readonly string prefix;
        readonly bool[] hasParentsName;
        readonly bool isData;

        public DecodeResultSet(string columnName, string tmpvarname, int id, string bb, StreamWriter sw, string prefix, bool[] hasParentsName, bool isData)
        {
            this.columnName = columnName;
            this.tmpvarname = tmpvarname;
            this.var = null;
            this.id = id;
            this.bb = bb;
            this.sw = sw;
            this.prefix = prefix;
            this.hasParentsName = hasParentsName;
            this.isData = isData;
        }

        public DecodeResultSet(Variable var, int id, string bb, StreamWriter sw, string prefix, bool[] hasParentsName, bool isData)
        {
            this.columnName = null;
            this.tmpvarname = null;
            this.var = var;
            this.id = id;
            this.bb = bb;
            this.sw = sw;
            this.prefix = prefix;
            this.hasParentsName = hasParentsName;
            this.isData = isData;
        }

        string ColumnName => null != this.columnName ? columnName : var.Name;
        string Getter => var != null ? isData ? var.NamePrivate : var.Getter : tmpvarname;
        string NamePrivate => var != null ? var.NamePrivate : tmpvarname;
        string ParaneName => columnName != null ? "" : "_parents_name_ + ";

        string AssignText(string value)
        {
            if (var != null)
                return var.Bean.IsNormalBean ? var.Setter(value) : $"{var.NamePrivate} = {value}";
            return $"{tmpvarname} = {value}";
        }

        void ensureParentsName()
        {
            if (!hasParentsName[0] && null == columnName)
            {
                hasParentsName[0] = true;
                sw.WriteLine($"{prefix}var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);");
            }
        }

        public void Visit(TypeBool type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getBoolean({ParaneName}\"{ColumnName}\")")};");
        }

        public void Visit(TypeByte type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getByte({ParaneName}\"{ColumnName}\")")};");
        }

        public void Visit(TypeShort type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getShort({ParaneName}\"{ColumnName}\")")};");
        }

        public void Visit(TypeInt type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getInt({ParaneName}\"{ColumnName}\")")};");
        }

        public void Visit(TypeLong type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getLong({ParaneName}\"{ColumnName}\")")};");
        }

        public void Visit(TypeFloat type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getFloat({ParaneName}\"{ColumnName}\")")};");
        }

        public void Visit(TypeDouble type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getDouble({ParaneName}\"{ColumnName}\")")};");
        }

        public void Visit(TypeBinary type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"new Zeze.Net.Binary({bb}.getBytes({ParaneName}\"{ColumnName}\"))")};");
        }

        public void Visit(TypeString type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{AssignText($"{bb}.getString({ParaneName}\"{ColumnName}\")")};");
            sw.WriteLine($"{prefix}if ({Getter} == null)");
            sw.WriteLine($"{prefix}    {AssignText("\"\"")};");
        }

        public void Visit(TypeList type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.decodeJsonList({NamePrivate}, {BoxingName.GetBoxingName(type.ValueType)}.class, {bb}.getString({ParaneName}\"{ColumnName}\"));");
        }

        public void Visit(TypeSet type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.decodeJsonSet({NamePrivate}, {BoxingName.GetBoxingName(type.ValueType)}.class, {bb}.getString({ParaneName}\"{ColumnName}\"));");
        }

        public void Visit(TypeMap type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.decodeJsonMap(this, \"{var.Name}\", {NamePrivate}, {bb}.getString({ParaneName}\"{ColumnName}\"));");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{NamePrivate}.decodeResultSet(parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{Getter}.decodeResultSet(parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeDynamic type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.decodeJsonDynamic({NamePrivate}, {bb}.getString({ParaneName}\"{ColumnName}\"));");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeQuaternion(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector2(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector2Int(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector3(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector3Int(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector4(parents, {bb})")};");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeDecimal type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}{{");
            sw.WriteLine($"{prefix}    var tmp = {bb}.getString({ParaneName}\"{ColumnName}\");");
            sw.WriteLine($"{prefix}    if (tmp == null)");
            sw.WriteLine($"{prefix}        {AssignText("java.math.BigDecimal.ZERO")};");
            sw.WriteLine($"{prefix}    else");
            sw.WriteLine($"{prefix}        {AssignText($"new java.math.BigDecimal(tmp, java.math.MathContext.DECIMAL128)")};");
            sw.WriteLine($"{prefix}}}");
        }
    }
}
