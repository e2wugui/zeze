using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

/// <summary>
/// 1 启动数据库时，用来判断当前代码的数据定义结构是否和当前数据库的定义结构兼容。
/// 2 TODO 通过查询类型信息，从数据转换到具体实例。合服可能需要。
///   如果是通用合并的insert，应该在二进制接口上操作（目前还没有）。
///   如果合并时需要处理冲突，此时应用是知道具体类型的。
///   所以这个功能暂时先不提供了。
///   
/// * 由于仅比较兼容，所以下面是一个简单实现，以后需要了再改吧。
/// </summary>
namespace Zeze
{
    public class Schemas : Serializable
    {
        public class Type : Serializable
        {
            public string Name { get;  set; }
            public string KeyName { get; set; } = "";
            public string ValueName { get; set; } = "";

            public Type Key { get; private set; }
            public Type Value { get; private set; }

            public static bool IsCompatible(Type a, Type b)
            {
                if (a == b) // same instance || all null
                    return true;
                if (null != a)
                    return a.IsCompatible(b);
                return false; // b.IsCompatible(a);
            }

            public virtual bool IsCompatible(Type other)
            {
                if (other == this)
                    return true;

                if (other == null)
                    return false;

                if (false == Name.Equals(other.Name))
                    return false;

                if (false == IsCompatible(Key, other.Key))
                    return false;

                if (false == IsCompatible(Value, other.Value))
                    return false;

                return true;
            }

            public virtual void Decode(ByteBuffer bb)
            {
                Name = bb.ReadString();
                KeyName = bb.ReadString();
                ValueName = bb.ReadString();
            }

            public virtual void Encode(ByteBuffer bb)
            {
                bb.WriteString(Name);
                bb.WriteString(KeyName);
                bb.WriteString(ValueName);
            }

            public virtual void Compile(Schemas s)
            {
                Key = s.Compile(KeyName, "", "");
                if (null != Key && Key is Bean bean)
                    bean.KeyRefCount++;
                Value = s.Compile(ValueName, "", "");
            }
        }

        public class Variable : Serializable
        {
            public int Id { get; set; }
            public string Name { get; set; }
            public string TypeName { get; set; }
            public string KeyName { get; set; } = "";
            public string ValueName { get; set; } = "";
            public Type Type { get; private set; }

            public void Decode(ByteBuffer bb)
            {
                Id = bb.ReadInt();
                Name = bb.ReadString();
                TypeName = bb.ReadString();
                KeyName = bb.ReadString();
                ValueName = bb.ReadString();
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Id);
                bb.WriteString(Name);
                bb.WriteString(TypeName);
                bb.WriteString(KeyName);
                bb.WriteString(ValueName);
            }

            public void Compile(Schemas s)
            {
                Type = s.Compile(TypeName, KeyName, ValueName);
            }
        }

        public class Bean : Type
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            public Dictionary<int, Variable> Variables { get; } = new Dictionary<int, Variable>();
            public bool IsBeanKey { get; set; } = false;
            public int KeyRefCount { get; set; } = 0;

            /// <summary>
            /// var可能增加，也可能删除，所以兼容仅判断var.id相同的。
            /// 并且和谁比较谁没有关系。
            /// </summary>
            /// <param name="other"></param>
            /// <returns></returns>
            public override bool IsCompatible(Type other)
            {
                if (other == null)
                    return false;

                if (other is Bean beanOther)
                {
                    foreach (var vThis in Variables.Values)
                    {
                        if (beanOther.Variables.TryGetValue(vThis.Id, out var vOther))
                        {
                            if (false == Type.IsCompatible(vThis.Type, vOther.Type))
                            {
                                logger.Error("Not Compatible. bean={0} variable={1}", Name, vThis.Name);
                                return false;
                            }
                        }
                    }
                    // 限制beankey的var只能增加，不能减少。
                    // 如果发生了Bean和BeanKey改变，应该是名字服用，忽略这个检查。
                    // 如果没有被真正当作Key，忽略这个检查。
                    if (IsBeanKey && KeyRefCount > 0
                        && beanOther.IsBeanKey && beanOther.KeyRefCount > 0)
                    {
                        if (Variables.Count < beanOther.Variables.Count)
                        {
                            logger.Error("Not Compatible. beankey={0} Variables.Count < DB.Variables.Count,Must Be Reduced", Name);
                            return false;
                        }
                        foreach (var vOther in beanOther.Variables.Values)
                        {
                            if (false == Variables.TryGetValue(vOther.Id, out var _))
                            {
                                logger.Error("Not Compatible. beankey={0} variable={1} Not Exist", Name, vOther.Name);
                                return false;
                            }
                        }
                    }
                    return true;
                }
                return false;
            }

            public override void Decode(ByteBuffer bb)
            {
                Name = bb.ReadString();
                IsBeanKey = bb.ReadBool();
                for (int count = bb.ReadInt(); count > 0; --count)
                {
                    var v = new Variable();
                    v.Decode(bb);
                    Variables.Add(v.Id, v);
                }
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteString(Name);
                bb.WriteBool(IsBeanKey);
                bb.WriteInt(Variables.Count);
                foreach (var v in Variables.Values)
                {
                    v.Encode(bb);
                }
            }

            public override void Compile(Schemas s)
            {
                foreach (var v in Variables.Values)
                {
                    v.Compile(s);
                }
            }

            public void AddVariable(Variable var)
            {
                Variables.Add(var.Id, var);
            }
        }

        public class Table : Serializable
        {
            public string Name { get; set; } // FullName, sample: demo_Module1_Table1
            public string KeyName { get; set; }
            public string ValueName { get; set; }
            public Type KeyType { get; private set; }
            public Bean ValueType { get; private set; } // Must Be Bean

            public void Decode(ByteBuffer bb)
            {
                Name = bb.ReadString();
                KeyName = bb.ReadString();
                ValueName = bb.ReadString();
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteString(Name);
                bb.WriteString(KeyName);
                bb.WriteString(ValueName);
            }

            public bool IsCompatible(Table other)
            {
                return Name.Equals(other.Name)
                    && KeyType.IsCompatible(other.KeyType)
                    && ValueType.IsCompatible(other.ValueType);
            }

            public void Compile(Schemas s)
            {
                KeyType = s.Compile(KeyName, "", "");
                if (KeyType is Bean bean)
                    bean.KeyRefCount++;
                ValueType = (Bean)s.Compile(ValueName, "", "");
            }
        }

        public Dictionary<string, Table> Tables { get; } = new Dictionary<string, Table>();
        public Dictionary<string, Bean> Beans { get; } = new Dictionary<string, Bean>();

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public bool IsCompatible(Schemas other)
        {
            if (null == other)
                return true;

            foreach (var table in Tables.Values)
            {
                if (other.Tables.TryGetValue(table.Name, out var otherTable))
                {
                    if (false == table.IsCompatible(otherTable))
                    {
                        logger.Error("Not Compatible. table={0}", table.Name);
                        return false;
                    }
                }
            }
            return true;
        }

        public void Decode(ByteBuffer bb)
        {
            for (int count = bb.ReadInt(); count > 0; --count)
            {
                var table = new Table();
                table.Decode(bb);
                Tables.Add(table.Name, table);
            }
            for (int count = bb.ReadInt(); count > 0; --count)
            {
                var bean = new Bean();
                bean.Decode(bb);
                Beans.Add(bean.Name, bean);
            }
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteInt(Tables.Count);
            foreach (var table in Tables.Values)
            {
                table.Encode(bb);
            }
            bb.WriteInt(Beans.Count);
            foreach (var bean in Beans.Values)
            {
                bean.Encode(bb);
            }
        }

        public void Compile()
        {
            foreach (var table in Tables.Values)
            {
                table.Compile(this);
            }
            foreach (var bean in Beans.Values)
            {
                bean.Compile(this);
            }
        }

        private Dictionary<string, Type> OtherTypes { get; } = new Dictionary<string, Type>();

        public Type Compile(string type, string key, string value)
        {
            if (string.IsNullOrEmpty(type))
                return null;

            if (Beans.TryGetValue(type, out var bean))
                return bean;

            // 除了Bean，其他基本类型和容器类型都动态创建。
            if (OtherTypes.TryGetValue($"{type}:{key}:{value}", out var o))
                return o;

            var n = new Type()
            {
                Name = type,
                KeyName = key,
                ValueName = value,
            };
            OtherTypes.Add($"{type}:{key}:{value}", n);
            n.Compile(this); // 容器需要编译。这里的时机不是太好。
            return n;
        }

        public void AddBean(Bean bean)
        {
            Beans.Add(bean.Name, bean);
        }

        public void AddTable(Table table)
        {
            Tables.Add(table.Name, table);
        }
    }
}
