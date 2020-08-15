using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Encode : Types.Visitor
    {
        private string varname;
        private int id;
        private string bufname;
        private System.IO.StreamWriter sw;
        private string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override void Encode(ByteBuffer _os_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    _os_.WriteInt(" + bean.Variables.Count + "); // Variables.Count");

            foreach (Types.Variable v in bean.Variables)
            {
                v.VariableType.Accept(new Encode(v.NamePrivate, v.Id, "_os_", sw, prefix + "    "));
            }

            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }

        public Encode(string varname, int id, string bufname, System.IO.StreamWriter sw, string prefix)
        {
            this.varname = varname;
            this.id = id;
            this.bufname = bufname;
            this.sw = sw;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.BEAN | " + id + " << Helper.TAG_SHIFT);");
            }

            sw.WriteLine(prefix + bufname + ".WriteByteBuffer(Helper.Encode(" + varname + "));");
        }

        public void Visit(BeanKey type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.BEAN | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteByteBuffer(Helper.Encode(" + varname + "));");
        }

        public void Visit(TypeByte type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.BYTE | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteByte(" + varname + ");");
        }

        public void Visit(TypeDouble type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.DOUBLE | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteDouble(" + varname + ");");
        }

        public void Visit(TypeInt type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.INT | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteInt(" + varname + ");");
        }

        public void Visit(TypeLong type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.LONG | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteLong(" + varname + ");");
        }

        public void Visit(TypeBool type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.BOOL | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteBool(" + varname + ");");
        }

        public void Visit(TypeBinary type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.BYTES | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteBytes(" + varname + ");");
        }

        public void Visit(TypeString type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + bufname + ".WriteInt(Helper.STRING | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + bufname + ".WriteString(" + varname + ");");
        }

        private void writeCollection(TypeCollection type)
        {
            Types.Type vt = type.ValueType;
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    ByteBuffer _temp_ = ByteBuffer.Allocate();");
            sw.WriteLine(prefix + "    _temp_.WriteInt(" + varname + ".Count);");
            sw.WriteLine(prefix + "    foreach (var _v_ in " + varname + ")");
            sw.WriteLine(prefix + "    {");
            vt.Accept(new Encode("_v_", -1, "_temp_", sw, prefix + "        "));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _os_.WriteByteBuffer(_temp_);"); // 如果嵌套更深的化，这里就不能直接用 _os_了，现在先这样。
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeList type)
        {
            if (id < 0)
                throw new Exception("invalie Variable.Id");
            sw.WriteLine(prefix + bufname + ".WriteInt(Helper.LIST | " + id + " << Helper.TAG_SHIFT);");
            writeCollection(type);
        }

        public void Visit(TypeSet type)
        {
            if (id < 0)
                throw new Exception("invalie Variable.Id");
            sw.WriteLine(prefix + bufname + ".WriteInt(Helper.SET | " + id + " << Helper.TAG_SHIFT);");
            writeCollection(type);
        }

        public void Visit(TypeMap type)
        {
            Types.Type keytype = type.KeyType;
            Types.Type valuetype = type.ValueType;

            if (id < 0)
                throw new Exception("invalie Variable.Id");
            sw.WriteLine(prefix + bufname + ".WriteInt(Helper.MAP | " + id + " << Helper.TAG_SHIFT);");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    ByteBuffer _temp_ = ByteBuffer.Allocate();");
            sw.WriteLine(prefix + "    _temp_.WriteInt(" + varname + ".Count);");
            sw.WriteLine(prefix + "    foreach (var _e_ in " + varname + ")");
            sw.WriteLine(prefix + "    {");
            keytype.Accept(new Encode("_e_.Key", -1, "_temp_", sw, prefix + "        "));
            valuetype.Accept(new Encode("_e_.Value", -1, "_temp_", sw, prefix + "        "));
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    _os_.WriteByteBuffer(_temp_);"); // 如果嵌套更深的化，这里就不能直接用 _os_了，现在先这样。
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeFloat type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "_os_.WriteInt(Helper.FLOAT | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + "_os_.WriteFloat(" + varname + ");");
        }

        public void Visit(TypeShort type)
        {
            if (id >= 0)
            {
                sw.WriteLine(prefix + "_os_.WriteInt(Helper.SHORT | " + id + " << Helper.TAG_SHIFT);");
            }
            sw.WriteLine(prefix + "_os_.WriteShort(" + varname + ");");
        }
    }
}
