using System.Collections.Generic;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
{
    public class Tostring : Visitor
    {
        public const int INDENT_SIZE = 4;

        readonly StreamWriter sw;
        readonly string varname;
        readonly string getter;
        readonly string prefix;
        readonly char sep;
        readonly bool subItem;

        public static void genIndents(StreamWriter sw, string prefix, List<Variable> vars)
        {
            int maxLevel = vars.Count > 0 ? 1 : 0;
            foreach (var var in vars)
            {
                var varType = var.VariableType;
                if (varType is TypeCollection or TypeMap)
                {
                    maxLevel = 2;
                    break;
                }
            }
            if (maxLevel >= 1)
            {
                sw.WriteLine($"{prefix}    var _i1_ = Zeze.Util.Str.indent(_l_ + {INDENT_SIZE});");
                if (maxLevel >= 2)
                    sw.WriteLine($"{prefix}    var _i2_ = Zeze.Util.Str.indent(_l_ + {INDENT_SIZE * 2});");
            }
        }

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public String toString() {");
            sw.WriteLine(prefix + "    var _s_ = new StringBuilder();");
            sw.WriteLine(prefix + "    buildString(_s_, 0);");
            sw.WriteLine(prefix + "    return _s_.toString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void buildString(StringBuilder _s_, int _l_) {");
            genIndents(sw, prefix, bean.Variables);
            sw.WriteLine($"{prefix}    _s_.append(\"{bean.FullName}: {{\\n\");");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var.Name, var.Getter, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    _s_.append(Zeze.Util.Str.indent(_l_)).append('}');");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public String toString() {");
            sw.WriteLine(prefix + "    var _s_ = new StringBuilder();");
            sw.WriteLine(prefix + "    buildString(_s_, 0);");
            sw.WriteLine(prefix + "    return _s_.toString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void buildString(StringBuilder _s_, int _l_) {");
            genIndents(sw, prefix, bean.Variables);
            sw.WriteLine($"{prefix}    _s_.append(\"{bean.FullName}: {{\\n\");");
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var.Name, var.Getter, prefix + "    ", sep));
            }
            sw.WriteLine(prefix + "    _s_.append(Zeze.Util.Str.indent(_l_)).append('}');");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Tostring(StreamWriter sw, string varname, string getter, string prefix, char sep, bool subItem = false)
        {
            this.sw = sw;
            this.varname = varname;
            this.getter = getter;
            this.prefix = prefix;
            this.sep = sep;
            this.subItem = subItem;
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + $"_s_.append(_i{(subItem ? 2 : 1)}_).append(\"{varname}=\");");
            sw.WriteLine(prefix + getter + ".buildString(_s_, _l_ + " + INDENT_SIZE * (subItem ? 3 : 2) + ");");
            sw.WriteLine(prefix + (sep != 0 ? $"_s_.append(\"{sep}\\n\");" : "_s_.append('\\n');"));
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"_s_.append(_i{(subItem ? 2 : 1)}_).append(\"{varname}=\");");
            sw.WriteLine(prefix + getter + ".buildString(_s_, _l_ + " + INDENT_SIZE * (subItem ? 3 : 2) + ");");
            sw.WriteLine(prefix + (sep != 0 ? $"_s_.append(\"{sep}\\n\");" : "_s_.append('\\n');"));
        }

        void formatSimple()
        {
            sw.Write(prefix + $"_s_.append(_i{(subItem ? 2 : 1)}_).append(\"{varname}=\").append({getter})");
            sw.WriteLine(sep != 0 ? $".append(\"{sep}\\n\");" : ".append('\\n');");
        }

        public void Visit(TypeBool type)
        {
            formatSimple();
        }

        public void Visit(TypeByte type)
        {
            formatSimple();
        }

        public void Visit(TypeShort type)
        {
            formatSimple();
        }

        public void Visit(TypeInt type)
        {
            formatSimple();
        }

        public void Visit(TypeLong type)
        {
            formatSimple();
        }

        public void Visit(TypeFloat type)
        {
            formatSimple();
        }

        public void Visit(TypeDouble type)
        {
            formatSimple();
        }

        public void Visit(TypeBinary type)
        {
            formatSimple();
        }

        public void Visit(TypeString type)
        {
            formatSimple();
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + $"_s_.append(_i1_).append(\"{varname}=[\\n\");");
            sw.WriteLine(prefix + $"for (var _item_ : {getter}) {{");
            type.ValueType.Accept(new Tostring(sw, "Item", "_item_", prefix + "    ", ',', true));
            sw.WriteLine(prefix + "}");
            sw.Write(prefix + "_s_.append(_i1_)");
            sw.WriteLine(sep != 0 ? $".append(\"]{sep}\\n\");" : ".append(\"]\\n\");");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"_s_.append(_i1_).append(\"{varname}=[\\n\");");
            sw.WriteLine(prefix + $"for (var _item_ : {getter}) {{");
            type.ValueType.Accept(new Tostring(sw, "Item", "_item_", prefix + "    ", ',', true));
            sw.WriteLine(prefix + "}");
            sw.Write(prefix + "_s_.append(_i1_)");
            sw.WriteLine(sep != 0 ? $".append(\"]{sep}\\n\");" : ".append(\"]\\n\");");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"_s_.append(_i1_).append(\"{varname}=[\\n\");");
            sw.WriteLine(prefix + $"for (var _kv_ : {getter}.entrySet()) {{");
            // sw.WriteLine(prefix + "    _s_.append(_i2_).append(\"(\\n\");");
            type.KeyType.Accept(new Tostring(sw, "Key", "_kv_.getKey()", prefix + "    ", ',', true));
            type.ValueType.Accept(new Tostring(sw, "Value", "_kv_.getValue()", prefix + "    ", ',', true));
            // sw.WriteLine(prefix + "    _s_.append(_i2_).append(\")\\n\");");
            sw.WriteLine(prefix + "}");
            sw.Write(prefix + "_s_.append(_i1_)");
            sw.WriteLine(sep != 0 ? $".append(\"]{sep}\\n\");" : ".append(\"]\\n\");");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + $"_s_.append(_i{(subItem ? 2 : 1)}_).append(\"{varname}=\");");
            sw.WriteLine(prefix + getter + ".getBean().buildString(_s_, _l_ + " + INDENT_SIZE * (subItem ? 3 : 2) + ");");
            sw.WriteLine(prefix + (sep != 0 ? $"_s_.append(\"{sep}\\n\");" : "_s_.append('\\n');"));
        }

        public void Visit(TypeQuaternion type)
        {
            formatSimple();
        }

        public void Visit(TypeVector2 type)
        {
            formatSimple();
        }

        public void Visit(TypeVector2Int type)
        {
            formatSimple();
        }

        public void Visit(TypeVector3 type)
        {
            formatSimple();
        }

        public void Visit(TypeVector3Int type)
        {
            formatSimple();
        }

        public void Visit(TypeVector4 type)
        {
            formatSimple();
        }

        public void Visit(TypeDecimal type)
        {
            formatSimple();
        }

        public void Visit(TypeGTable type)
        {
            //throw new System.NotImplementedException();
        }
    }
}
