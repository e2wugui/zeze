using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

/// <summary>
/// 1 启动数据库时，用来判断当前代码的数据定义结构是否和当前数据库的定义结构兼容。
///   当前包含以下兼容检测。
///   a) 对于每个 Variable.Id，Type不能修改。
///   b) 不能复用已经删除的 Variable.Id。
///   c) beankey 被应用于map.Key或set.Value或table.Key以后就不能再删除变量了。
///      当作key以后，如果删除变量，beankey.Encode() 就可能不再唯一。
///      
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
        public class Checked
        {
            public Bean Previous { get; set; }
            public Bean Current { get; set; }

            public override int GetHashCode()
            {
                const int _prime_ = 31;
                int _h_ = 0;
                _h_ = _h_ * _prime_ + Previous.Name.GetHashCode();
                _h_ = _h_ * _prime_ + Current.Name.GetHashCode();
                return _h_;
            }

            public override bool Equals(object obj)
            {
                if (this == obj)
                    return true;
                return (obj is Checked other) && Previous == other.Previous && Current == other.Current;
            }
        }

        public class CheckResult
        {
            public Bean Bean { get; set; }
        }
        public class Context
        {
            public Schemas Current { get; set; }
            public Schemas Previous { get; set; }
            public Dictionary<Checked, CheckResult> Checked { get; } = new Dictionary<Checked, CheckResult>();
            public Dictionary<Bean, CheckResult> CreateBeanIfRefZero { get; } = new Dictionary<Bean, CheckResult>();

            public CheckResult GetCheckResult(Bean previous, Bean current)
            {
                if (Checked.TryGetValue(
                    new Schemas.Checked() { Previous = previous, Current = current },
                    out var exist))
                    return exist;
                return null;
            }

            public void AddCheckResult(Bean previous, Bean current, CheckResult result)
            {
                Checked.Add(new Schemas.Checked() { Previous = previous, Current = current }, result);
            }

            public CheckResult GetCreateBeanIfRefZero(Bean bean)
            {
                if (CreateBeanIfRefZero.TryGetValue(bean, out var exist))
                    return exist;
                return null;
            }

            public void AddCreateBeanIfRefZero(Bean bean, CheckResult result)
            {
                CreateBeanIfRefZero.Add(bean, result);
            }
        }

        public class Type : Serializable
        {
            public string Name { get;  set; }
            public string KeyName { get; set; } = "";
            public string ValueName { get; set; } = "";
            public Type Key { get; private set; }
            public Type Value { get; private set; }

            public static bool IsCompatible(Type a, Type b, Context context, Action<Bean> Update)
            {
                if (a == b) // same instance || all null
                    return true;
                if (null != a)
                    return a.IsCompatible(b, context, Update);
                return false; // b.IsCompatible(a);
            }

            public virtual bool IsCompatible(Type other, Context context, Action<Bean> Update)
            {
                if (other == this)
                    return true;

                if (other == null)
                    return false;

                if (false == Name.Equals(other.Name))
                    return false;

                if (false == IsCompatible(Key, other.Key, context, (bean) =>
                {
                    KeyName = bean.Name;
                    Key= bean;
                }))
                    return false;

                if (false == IsCompatible(Value, other.Value, context, (bean) =>
                {
                    ValueName = bean.Name;
                    Value = bean;
                }))
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
                if (null != Key && Key is Bean key)
                {
                    key.KeyRefCount++;
                }

                Value = s.Compile(ValueName, "", "");
                if (null != Value)
                {
                    if (Name.Equals("set") && Value is Bean value)
                        value.KeyRefCount++;
                }
            }

            public virtual void TryCreateBeanIfRefZeroWhenDelete(Context context, Action<Bean> Update)
            {
                Key?.TryCreateBeanIfRefZeroWhenDelete(context, (bean) =>
                {
                    KeyName = bean.Name;
                    Key = bean;
                });
                Value?.TryCreateBeanIfRefZeroWhenDelete(context, (bean) =>
                {
                    ValueName = bean.Name;
                    Value = bean;
                });
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
            public bool Deleted { get; set; } = false;

            public void Decode(ByteBuffer bb)
            {
                Id = bb.ReadInt();
                Name = bb.ReadString();
                TypeName = bb.ReadString();
                KeyName = bb.ReadString();
                ValueName = bb.ReadString();
                Deleted = bb.ReadBool();
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Id);
                bb.WriteString(Name);
                bb.WriteString(TypeName);
                bb.WriteString(KeyName);
                bb.WriteString(ValueName);
                bb.WriteBool(Deleted);
            }

            public void Compile(Schemas s)
            {
                Type = s.Compile(TypeName, KeyName, ValueName);
            }

            public bool IsCompatible(Variable other, Context context)
            {
                bool result = this.Type.IsCompatible(other.Type, context, (bean) =>
                {
                    TypeName = bean.Name;
                    Type = bean;
                });
                // collection 时在Type内部变量可能发生改变，把修改复制过来。
                KeyName = this.Type.KeyName;
                ValueName = this.Type.ValueName;
                return result;
            }

            public void TryCreateBeanIfRefZeroWhenDelete(Context context)
            {
                this.Type.TryCreateBeanIfRefZeroWhenDelete(context, (bean) =>
                {
                    TypeName = bean.Name;
                    Type = bean;
                });
                // collection 时在Type内部传递ref，把修改复制到变量里。
                KeyName = this.Type.KeyName;
                ValueName = this.Type.ValueName;
            }
        }

        public class Bean : Type
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            public Dictionary<int, Variable> Variables { get; } = new Dictionary<int, Variable>();
            public bool IsBeanKey { get; set; } = false;
            public int KeyRefCount { get; set; } = 0;
            // 这个变量当前是不需要的，作为额外的属性记录下来，以后可能要用。
            public bool Deleted { get; private set; } = false;
            // 这里记录在当前版本Schemas中Bean的实际名字，只有生成的bean包含这个。
            public string RealName { get; private set; } = "";

            /// <summary>
            /// var可能增加，也可能删除，所以兼容仅判断var.id相同的。
            /// 并且和谁比较谁没有关系。
            /// </summary>
            /// <param name="other"></param>
            /// <returns></returns>
            public override bool IsCompatible(Type other, Context context, Action<Bean> Update)
            {
                if (other == null)
                    return false;

                if (other is Bean beanOther)
                {
                    CheckResult result = context.GetCheckResult(beanOther, this);
                    if (null != result)
                    {
                        Update(result.Bean);
                        return true;
                    }
                    result = new CheckResult() { Bean = this }; // result在后面可能被更新。
                    context.AddCheckResult(beanOther, this, result);

                    List<Variable> Deleteds = new List<Variable>();
                    foreach (var vOther in beanOther.Variables.Values)
                    {
                        if (Variables.TryGetValue(vOther.Id, out var vThis))
                        {
                            if (vThis.Deleted)
                            {
                                // bean 可能被多个地方使用，前面比较的时候，创建或者复制了被删除的变量。
                                // 所以可能存在已经被删除var，这个时候忽略比较就行了。
                                continue;
                            }
                            if (vOther.Deleted)
                            {
                                if (vThis.IsCompatible(vOther, context))
                                {
                                    // 反悔
                                    continue;
                                }
                                // 重用了已经被删除的var。此时vOther.Type也是null。
                                logger.Error("Not Compatible. bean={0} variable={1} Can Not Reuse Deleted Variable.Id", Name, vThis.Name);
                                return false;
                            }
                            if (false == vThis.IsCompatible(vOther, context))
                            {
                                logger.Error("Not Compatible. bean={0} variable={1}", Name, vOther.Name);
                                return false;
                            }
                        }
                        else
                        {
                            // 新删除或以前删除的都创建一个新的。
                            Deleteds.Add(new Variable()
                            {
                                Id = vOther.Id,
                                Name = vOther.Name,
                                TypeName = vOther.TypeName,
                                KeyName = vOther.KeyName,
                                ValueName = vOther.ValueName,
                                Deleted = true,
                            });
                        }
                    }
                    // 限制beankey的var只能增加，不能减少。
                    // 如果发生了Bean和BeanKey改变，忽略这个检查。
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
                            if (vOther.Deleted)
                            {
                                // 当作Key前允许删除变量，所以可能存在已经被删除的变量。
                                continue;
                            }
                            if (false == Variables.TryGetValue(vOther.Id, out var _))
                            {
                                // 被当作Key以后就不能再删除变量了。
                                logger.Error("Not Compatible. beankey={0} variable={1} Not Exist", Name, vOther.Name);
                                return false;
                            }
                        }
                    }

                    if (Deleteds.Count > 0)
                    {
                        // 把本次（包括以前）删除的变量复制过来。
                        /*
                        if (beanOther.Name.Equals(this.Name))
                        {
                            // Name 相同，正常情况下删除 bean.var。不新建Bean。
                            foreach (var vDelete in Deleteds)
                            {
                                Variables.Add(vDelete.Id, vDelete);
                            }
                            return true;
                            // 总是新建Bean，否则的话，不知道会不会有没有考虑到的情况。
                        }
                        */
                        Bean newBean = Copy(); // CopyExcludeDeletedVariable
                        while (true)
                        {
                            if (context.Current.Beans.TryAdd(newBean.Name, newBean))
                            {
                                Update(newBean);
                                result.Bean = newBean; // 保存下来，后续重复的比较需要更新到最新的Bean。
                                break;
                            }
                            newBean.Name = Schemas.GenerateName();
                        }
                        foreach (var vDelete in Deleteds)
                        {
                            vDelete.TryCreateBeanIfRefZeroWhenDelete(context);
                            newBean.Variables.Add(vDelete.Id, vDelete);
                        }
                    }
                    return true;
                }
                return false;
            }

            public override void TryCreateBeanIfRefZeroWhenDelete(Context context, Action<Bean> Update)
            {
                CheckResult result = context.GetCreateBeanIfRefZero(this);
                if (null != result)
                {
                    Update(result.Bean);
                    return;
                }
                result = new CheckResult() { Bean = this };
                context.AddCreateBeanIfRefZero(this, result);

                if (Name.StartsWith("_"))
                {
                    // bean 是内部创建的，可能是原来删除的，也可能是合并改名引起的。
                    if (context.Current.Beans.TryGetValue(RealName, out var _))
                        return;

                    if (context.Current.Beans.TryAdd(Name, this))
                        return; // 直接引用旧的实例。

                    var newb = Copy(); // 原来创建的名字重复了，新建一个，可能性不大。
                    while (true)
                    {
                        if (context.Current.Beans.TryAdd(newb.Name, newb))
                        {
                            Update(newb);
                            result.Bean = newb;
                            return;
                        }
                        newb.Name = Schemas.GenerateName();
                    }
                }

                // 通过查找当前Schemas来发现RefZero。
                if (context.Current.Beans.TryGetValue(Name, out var _))
                    return;
                var newb2 = Copy();
                newb2.Deleted = true;
                while (true)
                {
                    if (context.Current.Beans.TryAdd(newb2.Name, newb2))
                    {
                        Update(newb2);
                        result.Bean = newb2;
                        break;
                    }
                    newb2.Name = Schemas.GenerateName();
                }

                foreach (var v in Variables.Values)
                {
                    v.TryCreateBeanIfRefZeroWhenDelete(context);
                }
            }

            private Bean Copy()
            {
                var newBean = new Bean();
                newBean.Name = Schemas.GenerateName();
                newBean.IsBeanKey = this.IsBeanKey;
                newBean.KeyRefCount = this.KeyRefCount;
                newBean.RealName = this.Name;
                newBean.Deleted = this.Deleted;
                foreach (var v in Variables.Values)
                {
                    newBean.Variables.Add(v.Id, v); // 这些变量不会被改变，先不拷贝了。需要再拷贝。
                }
                return newBean;
            }

            /*
            private Bean CopyExcludeDeletedVariable()
            {
                var newBean = new Bean();
                newBean.Name = Schemas.GenerateName();
                newBean.IsBeanKey = this.IsBeanKey;
                newBean.KeyRefCount = this.KeyRefCount;
                newBean.RealName = this.Name;
                // Deleted 仅用来保存不被引用的Bean，这种情况下不可能发生Copy需求。

                foreach (var v in Variables.Values)
                {
                    if (v.Deleted)
                        continue;
                    newBean.Variables.Add(v.Id, v); // 这些变量不会被改变，先不拷贝了。需要再拷贝。
                }
                return newBean;
            }
            */

            public override void Decode(ByteBuffer bb)
            {
                Name = bb.ReadString();
                IsBeanKey = bb.ReadBool();
                Deleted = bb.ReadBool();
                RealName = bb.ReadString();
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
                bb.WriteBool(Deleted);
                bb.WriteString(RealName);
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
            public Type ValueType { get; private set; }

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

            public bool IsCompatible(Table other, Context context)
            {
                return Name.Equals(other.Name)
                    && KeyType.IsCompatible(other.KeyType, context, (bean) =>
                    {
                        KeyName = bean.Name;
                        KeyType = bean;
                    })
                    && ValueType.IsCompatible(other.ValueType, context, (bean) =>
                    {
                        ValueName = bean.Name;
                        ValueType = bean;
                    });
            }

            public void Compile(Schemas s)
            {
                KeyType = s.Compile(KeyName, "", "");
                if (KeyType is Bean bean)
                    bean.KeyRefCount++;
                ValueType = s.Compile(ValueName, "", "");
            }
        }

        public Dictionary<string, Table> Tables { get; } = new Dictionary<string, Table>();
        public Dictionary<string, Bean> Beans { get; } = new Dictionary<string, Bean>();

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public bool IsCompatible(Schemas other)
        {
            if (null == other)
                return true;

            var context = new Context()
            {
                Current = this,
                Previous = other,
            };
            foreach (var table in Tables.Values)
            {
                if (other.Tables.TryGetValue(table.Name, out var otherTable))
                {
                    if (false == table.IsCompatible(otherTable, context))
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

        public static string GenerateName()
        {
            return "_" + DateTime.Now.Ticks;
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
