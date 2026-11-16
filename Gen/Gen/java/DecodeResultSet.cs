using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class DecodeResultSet : Visitor
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {");
            var hasParentName = new bool[1];
            foreach (Variable v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new DecodeResultSet(v, v.Id, "_r_", sw, $"{prefix}    ", hasParentName, false));
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            var hasInitializedReader = bean.NeedsInitializedSqlReader && bean.Variables.Count > 0;
            const string sqlParameters = "java.util.ArrayList<String> _p_, java.sql.ResultSet _r_";
            const string throwsSql = "throws java.sql.SQLException";
            sw.WriteLine($"{prefix}public static {bean.Name} readFromResultSet({sqlParameters}) {throwsSql} {{");
            sw.WriteLine($"{prefix}    return new {bean.Name}(_p_, _r_);");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            if (hasInitializedReader)
            {
                var parameters = ParamName.GetParamList(bean.Variables);
                var arguments = string.Join(", ", bean.Variables.ConvertAll(v => v.NamePrivate + "_"));
                sw.WriteLine($"{prefix}public static {bean.Name} readFromResultSet({sqlParameters}, {parameters}) {throwsSql} {{");
                sw.WriteLine($"{prefix}    return new {bean.Name}(_p_, _r_, {arguments});");
                sw.WriteLine($"{prefix}}}");
                sw.WriteLine();
                sw.WriteLine($"{prefix}private {bean.Name}({sqlParameters}, {parameters}) {throwsSql} {{");
                sw.WriteLine($"{prefix}    this({arguments});");
                sw.WriteLine($"{prefix}    readSqlFields(_p_, _r_);");
                sw.WriteLine($"{prefix}}}");
                sw.WriteLine();
            }
            sw.WriteLine($"{prefix}private {bean.Name}({sqlParameters}) {throwsSql} {{");
            if (Decode.NeedsKeyDefaults(bean))
                sw.WriteLine($"{prefix}    this();");
            if (hasInitializedReader)
            {
                sw.WriteLine($"{prefix}    readSqlFields(_p_, _r_);");
                sw.WriteLine($"{prefix}}}");
                sw.WriteLine();
                sw.WriteLine($"{prefix}private void readSqlFields({sqlParameters}) {throwsSql} {{");
            }
            var hasParentName = new bool[1];
            foreach (Variable v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new DecodeResultSet(v, v.Id, "_r_", sw, $"{prefix}    ", hasParentName, true));
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
        string ParaneName => columnName != null ? "" : "_pn_ + ";

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
                sw.WriteLine($"{prefix}var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);");
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

        public void Visit(TypeSortedMap type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.decodeJsonMap(this, \"{var.Name}\", {NamePrivate}, {bb}.getString({ParaneName}\"{ColumnName}\"));");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}_p_.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{NamePrivate}.decodeResultSet(_p_, {bb});");
            sw.WriteLine($"{prefix}_p_.removeLast();");
        }

        public void Visit(BeanKey type)
        {
            var initial = type is ExternalBeanKey || string.IsNullOrEmpty(var?.Initial) ? "" : ", " + var.Initial;
            var value = $"{TypeName.GetName(type)}.readFromResultSet(_p_, {bb}{initial})";
            sw.WriteLine($"{prefix}_p_.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}try {{");
            sw.WriteLine($"{prefix}    {AssignText(value)};");
            sw.WriteLine($"{prefix}}} finally {{");
            sw.WriteLine($"{prefix}    _p_.removeLast();");
            sw.WriteLine($"{prefix}}}");
        }

        public void Visit(TypeDynamic type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.decodeJsonDynamic({NamePrivate}, {bb}.getString({ParaneName}\"{ColumnName}\"));");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}_p_.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeQuaternion(_p_, {bb})")};");
            sw.WriteLine($"{prefix}_p_.removeLast();");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}_p_.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector2(_p_, {bb})")};");
            sw.WriteLine($"{prefix}_p_.removeLast();");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}_p_.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector2Int(_p_, {bb})")};");
            sw.WriteLine($"{prefix}_p_.removeLast();");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}_p_.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector3(_p_, {bb})")};");
            sw.WriteLine($"{prefix}_p_.removeLast();");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}_p_.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector3Int(_p_, {bb})")};");
            sw.WriteLine($"{prefix}_p_.removeLast();");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}_p_.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{AssignText($"Zeze.Serialize.Helper.decodeVector4(_p_, {bb})")};");
            sw.WriteLine($"{prefix}_p_.removeLast();");
        }

        public void Visit(TypeDecimal type)
        {
            ensureParentsName();
            // 不带 MathContext：与 LogDecimal.decode 一致保持 encode/decode 双射（FND3-06）。
            sw.WriteLine($"{prefix}{{");
            sw.WriteLine($"{prefix}    var tmp = {bb}.getString({ParaneName}\"{ColumnName}\");");
            sw.WriteLine($"{prefix}    if (tmp == null)");
            sw.WriteLine($"{prefix}        {AssignText("java.math.BigDecimal.ZERO")};");
            sw.WriteLine($"{prefix}    else");
            sw.WriteLine($"{prefix}        {AssignText($"new java.math.BigDecimal(tmp)")};");
            sw.WriteLine($"{prefix}}}");
        }

        public void Visit(TypeGTable type)
        {
            ensureParentsName();
            var name12 = type.ValueType.IsNormalBean ? "2" : "1";
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.decodeJsonGTable{name12}(this, \"{var.Name}\", {NamePrivate}, {bb}.getString({ParaneName}\"{ColumnName}\"));");
        }
    }
}
