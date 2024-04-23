using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Reset : Visitor
    {
        readonly StreamWriter sw;
        readonly Variable var;
        readonly string prefix;
        readonly bool isData;

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool isData, bool withUnknown)
        {
            if (isData || bean.Variables.Count > 0)
            {
                sw.WriteLine(prefix + "@Override");
                sw.WriteLine(prefix + "public void reset() {");
                foreach (Variable var in bean.Variables)
                    var.VariableType.Accept(new Reset(var, sw, prefix + "    ", isData));
                if (withUnknown)
                    sw.WriteLine(prefix + "    _unknown_ = null;");
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
        }

        public Reset(Variable var, StreamWriter sw, string prefix, bool isData)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
            this.isData = isData;
        }

        void ResetValue(string defInit)
        {
            string value = var.Initial;
            if (value.Length == 0)
                value = defInit;
            if (isData)
                sw.WriteLine(prefix + var.NamePrivate + " = " + value + ";");
            else
                sw.WriteLine(prefix + var.Setter(value) + ";");
        }

        public void Visit(TypeBool type)
        {
            ResetValue("false");
        }

        public void Visit(TypeByte type)
        {
            ResetValue("(byte)0");
        }

        public void Visit(TypeShort type)
        {
            ResetValue("(short)0");
        }

        public void Visit(TypeInt type)
        {
            ResetValue("0");
        }

        public void Visit(TypeLong type)
        {
            ResetValue("0");
        }

        public void Visit(TypeFloat type)
        {
            ResetValue("0");
        }

        public void Visit(TypeDouble type)
        {
            ResetValue("0");
        }

        public void Visit(TypeBinary type)
        {
            ResetValue("Zeze.Net.Binary.Empty");
        }

        public void Visit(TypeString type)
        {
            ResetValue("\"\"");
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".clear();");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".reset();");
        }

        public void Visit(BeanKey type)
        {
            ResetValue("new " + type.FullName + "()");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.NamePrivate + ".reset();");
        }

        public void Visit(TypeVector2 type)
        {
            ResetValue("Zeze.Serialize.Vector2.ZERO");
        }

        public void Visit(TypeVector2Int type)
        {
            ResetValue("Zeze.Serialize.Vector2Int.ZERO");
        }

        public void Visit(TypeVector3 type)
        {
            ResetValue("Zeze.Serialize.Vector3.ZERO");
        }

        public void Visit(TypeVector3Int type)
        {
            ResetValue("Zeze.Serialize.Vector3Int.ZERO");
        }

        public void Visit(TypeVector4 type)
        {
            ResetValue("Zeze.Serialize.Vector4.ZERO");
        }

        public void Visit(TypeQuaternion type)
        {
            ResetValue("Zeze.Serialize.Quaternion.ZERO");
        }

        public void Visit(TypeDecimal type)
        {
            string value = var.Initial;
            if (value.Length == 0)
                value = "java.match.BigDecimal.ZERO";
            if (isData)
                sw.WriteLine(prefix + var.NamePrivate + " = " + value + ";");
            else
                sw.WriteLine(prefix + var.Setter(value) + ";");
        }
    }
}
