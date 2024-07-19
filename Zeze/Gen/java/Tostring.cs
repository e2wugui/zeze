﻿using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Tostring : Visitor
    {
        public const int INDENT_SIZE = 4;

        readonly StreamWriter sw;
        readonly Variable var;
        readonly string varname;
        readonly string getter;
        readonly string prefix;
        readonly char sep;
        readonly bool isData;

        string Getter => var != null ? isData ? var.NamePrivate : var.Getter : getter;
        string NamePrivate => var != null ? var.NamePrivate : getter;

        public static void Make(Bean bean, StreamWriter sw, string prefix, bool isData)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public String toString() {");
            sw.WriteLine(prefix + "    var _s_ = new StringBuilder();");
            sw.WriteLine(prefix + "    buildString(_s_, 0);");
            sw.WriteLine(prefix + "    return _s_.append(System.lineSeparator()).toString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void buildString(StringBuilder _s_, int _l_) {");
            sw.WriteLine($"{prefix}    _s_.append(Zeze.Util.Str.indent(_l_)).append(\"{bean.FullName}: {{\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "    _l_ += " + INDENT_SIZE + ';');
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var, var.Name, null, prefix + "    ", sep, isData));
            }
            sw.WriteLine(prefix + "    _l_ -= " + INDENT_SIZE + ';');
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
            sw.WriteLine(prefix + "    _s_.append(System.lineSeparator());");
            sw.WriteLine(prefix + "    return _s_.toString();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void buildString(StringBuilder _s_, int _l_) {");
            sw.WriteLine($"{prefix}    _s_.append(Zeze.Util.Str.indent(_l_)).append(\"{bean.FullName}: {{\").append(System.lineSeparator());");
            sw.WriteLine(prefix + "    _l_ += " + INDENT_SIZE + ';');
            for (int i = 0; i < bean.Variables.Count; ++i)
            {
                var var = bean.Variables[i];
                char sep = i == bean.Variables.Count - 1 ? '\0' : ',';
                var.VariableType.Accept(new Tostring(sw, var, var.Name, null, prefix + "    ", sep, true));
            }
            sw.WriteLine(prefix + "    _l_ -= " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + "    _s_.append(Zeze.Util.Str.indent(_l_)).append('}');");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Tostring(StreamWriter sw, Variable var, string varname, string getter, string prefix, char sep, bool isData)
        {
            this.sw = sw;
            this.var = var;
            this.varname = varname;
            this.getter = getter;
            this.prefix = prefix;
            this.sep = sep;
            this.isData = isData;
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + $"_s_.append(Zeze.Util.Str.indent(_l_)).append(\"{varname}=\").append(System.lineSeparator());");
            sw.WriteLine(prefix + NamePrivate + ".buildString(_s_, _l_ + " + INDENT_SIZE + ");");
            sw.Write(prefix + "_s_");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine(prefix + $"_s_.append(Zeze.Util.Str.indent(_l_)).append(\"{varname}=\").append(System.lineSeparator());");
            sw.WriteLine(prefix + Getter + ".buildString(_s_, _l_ + " + INDENT_SIZE + ");");
            sw.Write(prefix + "_s_");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        void formatSimple()
        {
            sw.Write(prefix + $"_s_.append(Zeze.Util.Str.indent(_l_)).append(\"{varname}=\").append({Getter})");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
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
            sw.WriteLine(prefix + $"_s_.append(Zeze.Util.Str.indent(_l_)).append(\"{varname}=[\");");
            sw.WriteLine(prefix + $"if (!{NamePrivate}.isEmpty()) {{");
            sw.WriteLine(prefix + "    _s_.append(System.lineSeparator());");
            sw.WriteLine(prefix + "    _l_ += " + INDENT_SIZE + ';');
            if (!isData || string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine(prefix + $"    for (var _v_ : {NamePrivate}) {{");
            else
            {
                sw.WriteLine(prefix + $"    for (int _i_ = 0, _n_ = {NamePrivate}.size(); _i_ < _n_; _i_++) {{");
                sw.WriteLine(prefix + $"        var _v_ = {NamePrivate}.get(_i_);");
            }
            type.ValueType.Accept(new Tostring(sw, null, "Item", "_v_", prefix + "        ", ',', isData));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _l_ -= " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + "    _s_.append(Zeze.Util.Str.indent(_l_));");
            sw.WriteLine(prefix + "}");
            sw.Write(prefix + "_s_.append(']')");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + $"_s_.append(Zeze.Util.Str.indent(_l_)).append(\"{varname}={{\");");
            sw.WriteLine(prefix + $"if (!{NamePrivate}.isEmpty()) {{");
            sw.WriteLine(prefix + "    _s_.append(System.lineSeparator());");
            sw.WriteLine(prefix + "    _l_ += " + INDENT_SIZE + ';');
            if (!isData || string.IsNullOrEmpty(type.Variable.JavaType))
                sw.WriteLine(prefix + $"    for (var _v_ : {NamePrivate}) {{");
            else
            {
                sw.WriteLine(prefix + $"    for (var _i_ = {NamePrivate}.iterator(); _i_.moveToNext(); ) {{");
                sw.WriteLine(prefix + "        var _v_ = _i_.value();");
            }
            type.ValueType.Accept(new Tostring(sw, null, "Item", "_v_", prefix + "        ", ',', isData));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _l_ -= " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + "    _s_.append(Zeze.Util.Str.indent(_l_));");
            sw.WriteLine(prefix + "}");
            sw.Write(prefix + "_s_.append('}')");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + $"_s_.append(Zeze.Util.Str.indent(_l_)).append(\"{varname}={{\");");
            sw.WriteLine(prefix + $"if (!{NamePrivate}.isEmpty()) {{");
            sw.WriteLine(prefix + "    _s_.append(System.lineSeparator());");
            sw.WriteLine(prefix + "    _l_ += " + INDENT_SIZE + ';');
            if (!isData || string.IsNullOrEmpty(type.Variable.JavaType))
            {
                sw.WriteLine(prefix + $"    for (var _e_ : {NamePrivate}.entrySet()) {{");
                // sw.WriteLine(prefix + "        _s_.append(Zeze.Util.Str.indent(_l_)).append('(').append(System.lineSeparator());");
                type.KeyType.Accept(new Tostring(sw, null, "Key", "_e_.getKey()", prefix + "        ", ',', isData));
                type.ValueType.Accept(new Tostring(sw, null, "Value", "_e_.getValue()", prefix + "        ", ',', isData));
                // sw.WriteLine(prefix + "        _s_.append(Zeze.Util.Str.indent(_l_)).append(')').append(System.lineSeparator());");
            }
            else
            {
                sw.WriteLine(prefix + $"    for (var _i_ = {NamePrivate}.iterator(); _i_.moveToNext(); ) {{");
                // sw.WriteLine(prefix + "        _s_.append(Zeze.Util.Str.indent(_l_)).append('(').append(System.lineSeparator());");
                type.KeyType.Accept(new Tostring(sw, null, "Key", "_i_.key()", prefix + "        ", ',', isData));
                type.ValueType.Accept(new Tostring(sw, null, "Value", "_i_.value()", prefix + "        ", ',', isData));
                // sw.WriteLine(prefix + "        _s_.append(Zeze.Util.Str.indent(_l_)).append(')').append(System.lineSeparator());");
            }
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _l_ -= " + INDENT_SIZE + ';');
            sw.WriteLine(prefix + "    _s_.append(Zeze.Util.Str.indent(_l_));");
            sw.WriteLine(prefix + "}");
            sw.Write(prefix + "_s_.append('}')");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}_s_.append(Zeze.Util.Str.indent(_l_)).append(\"{varname}=\").append(System.lineSeparator());");
            sw.WriteLine($"{prefix}{NamePrivate}.get{(isData ? "Data" : "Bean")}().buildString(_s_, _l_ + {INDENT_SIZE});");
            sw.Write(prefix + "_s_");
            if (sep != 0)
                sw.Write($".append('{sep}')");
            sw.WriteLine(".append(System.lineSeparator());");
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
    }
}
