using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class EncodeSQLStatement : Visitor
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {");
            var hasParentName = new bool[1];
            foreach (Variable v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new EncodeSQLStatement(null, v, null, v.Id, "st", sw, prefix + "    ", hasParentName, false));
            }
            sw.WriteLine(prefix + "}");
            // sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {");
            var hasParentsName = new bool[1];
            foreach (Variable v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new EncodeSQLStatement(null, v, null, v.Id, "st", sw, prefix + "    ", hasParentsName, true));
            }
            sw.WriteLine(prefix + "}");
            // sw.WriteLine();
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
            sw.WriteLine($"{prefix}st.appendBoolean({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeByte type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendByte({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeShort type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendShort({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeInt type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendInt({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeLong type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendLong({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeFloat type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendFloat({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeDouble type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendDouble({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeBinary type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendBinary({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeString type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString({ParaneName}\"{ColumnName}\", {Getter});");
        }

        public void Visit(TypeDecimal type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString({ParaneName}\"{ColumnName}\", {Getter}.toString());");
        }

        public void Visit(TypeList type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString({ParaneName}\"{ColumnName}\", Zeze.Serialize.Helper.encodeJson({NamePrivate}));");
        }

        public void Visit(TypeSet type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString({ParaneName}\"{ColumnName}\", Zeze.Serialize.Helper.encodeJson({NamePrivate}));");
        }

        public void Visit(TypeMap type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString({ParaneName}\"{ColumnName}\", Zeze.Serialize.Helper.encodeJson({NamePrivate}));");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{NamePrivate}.encodeSQLStatement(parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}{Getter}.encodeSQLStatement(parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeDynamic type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString({ParaneName}\"{ColumnName}\", Zeze.Serialize.Helper.encodeJson({NamePrivate}));");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeQuaternion({Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector2({Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector2Int({Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector3({Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector3Int({Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{ColumnName}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector4({Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        readonly string columnName;
        readonly Variable var;
        readonly string varname;
        readonly int id;
        readonly string bb;
        readonly StreamWriter sw;
        readonly string prefix;
        readonly bool[] hasParentsName;
        readonly bool isData;

        string Getter => var != null ? isData ? var.NamePrivate : var.Getter : varname;
        string NamePrivate => var != null ? var.NamePrivate : varname;
        string ColumnName => null != columnName ? columnName : var.Name;
        string ParaneName => columnName != null ? "" : "_parents_name_ + ";

        public EncodeSQLStatement(string columnName, Variable var, string varname, int id, string bb, StreamWriter sw, string prefix, bool[] hasParentsName, bool isData)
        {
            this.columnName = columnName;
            this.var = var;
            this.varname = varname;
            this.id = id;
            this.bb = bb;
            this.sw = sw;
            this.prefix = prefix;
            this.hasParentsName = hasParentsName;
            this.isData = isData;
        }
    }
}
