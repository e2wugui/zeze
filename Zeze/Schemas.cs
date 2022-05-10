using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

/// <summary>
/// 1 启动数据库时，用来判断当前代码的数据定义结构是否和当前数据库的定义结构兼容。
///   当前包含以下兼容检测。
///   a) 对于每个 Variable.Id，Type不能修改。
///   b) 不能复用已经删除的 Variable.Id。
///      但是允许"反悔"，也就是说可以重新使用已经删除的Variable.Id时，只要Type和原来一样，就允许。
///      这是为了处理多人使用同一个数据库进行开发时的冲突（具体不解释了）。
///   c) beankey 被应用于map.Key或set.Value或table.Key以后就不能再删除变量了。
///      当作key以后，如果删除变量，beankey.Encode() 就可能不再唯一。
///      
/// 2 通过查询类型信息，从数据转换到具体实例。合服可能需要。
///   如果是通用合并的insert，应该在二进制接口上操作（目前还没有）。
///   如果合并时需要处理冲突，此时应用是知道具体类型的。
///   所以这个功能暂时先不提供了。
///   
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
            private List<Action<Bean>> Updates { get; } = new List<Action<Bean>>();
            private List<Action<Bean>> UpdateVariables { get; } = new List<Action<Bean>>();

            public void AddUpdate(Action<Bean> Update, Action<Bean> UpdateVariable)
            {
                Updates.Add(Update);
                if (null != UpdateVariable)
                    UpdateVariables.Add(UpdateVariable);
            }

            public void Update()
            {
                foreach (var update in Updates)
                {
                    update(Bean);
                }
                foreach (var update in UpdateVariables)
                {
                    update(Bean);
                }
            }
        }
        public class Context
        {
            public Schemas Current { get; set; }
            public Schemas Previous { get; set; }
            public Dictionary<Checked, CheckResult> Checked { get; } = new Dictionary<Checked, CheckResult>();
            public Dictionary<Bean, CheckResult> CopyBeanIfRemoved { get; } = new Dictionary<Bean, CheckResult>();
            public Config Config { get; set; }

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

            public CheckResult GetCopyBeanIfRemovedResult(Bean bean)
            {
                if (CopyBeanIfRemoved.TryGetValue(bean, out var exist))
                    return exist;
                return null;
            }

            public void AddCopyBeanIfRemovedResult(Bean bean, CheckResult result)
            {
                CopyBeanIfRemoved.Add(bean, result);
            }

            public void Update()
            {
                foreach (var result in Checked.Values)
                {
                    result.Update();
                }
                foreach (var result in CopyBeanIfRemoved.Values)
                {
                    result.Update();
                }
            }

            private long ReNameCount = 0;

            public string GenerateUniqueName()
            {
                ++ReNameCount;
                return "_" + ReNameCount;
            }
        }

        public class Type : Serializable
        {
            public string Name { get;  set; }
            public string KeyName { get; set; } = "";
            public string ValueName { get; set; } = "";
            public Type Key { get; private set; }
            public Type Value { get; private set; }

            public virtual bool IsCompatible(Type other, Context context, Action<Bean> Update, Action<Bean> UpdateVariable)
            {
                if (other == this)
                    return true;

                if (other == null)
                    return false;

                if (false == Name.Equals(other.Name))
                    return false;

                // Name 相同的情况下，下面的 Key Value 仅在 Collection 时有值。
                // 当 this.Key == null && other.Key != null 在 Name 相同的情况下是不可能发生的。
                if (null != Key)
                {
                    if (false == Key.IsCompatible(other.Key, context,
                        (bean) =>
                        {
                            KeyName = bean.Name;
                            Key = bean;
                        },
                        UpdateVariable))
                    {
                        return false;
                    }
                }
                else if (other.Key != null)
                {
                    throw new Exception("(this.Key == null && other.Key != null) Impossible!");
                }

                if (null != Value)
                {
                    if (false == Value.IsCompatible(other.Value, context,
                        (bean) =>
                        {
                            ValueName = bean.Name;
                            Value = bean;
                        },
                        UpdateVariable))
                    {
                        return false;
                    }
                }
                else if (other.Value != null)
                {
                    throw new Exception("(this.Value == null && other.Value != null) Impossible!");
                }

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

            public virtual void TryCopyBeanIfRemoved(Context context, Action<Bean> Update, Action<Bean> UpdateVariable)
            {
                Key?.TryCopyBeanIfRemoved(context,
                    (bean) =>
                    {
                        KeyName = bean.Name;
                        Key = bean;
                    },
                    UpdateVariable);

                Value?.TryCopyBeanIfRemoved(context,
                    (bean) =>
                    {
                        ValueName = bean.Name;
                        Value = bean;
                    },
                    UpdateVariable);
            }
        }

        public class Variable : Serializable
        {
            public int Id { get; set; }
            public string Name { get; set; }
            public string TypeName { get; set; }
            public string KeyName { get; set; } = "";
            public string ValueName { get; set; } = "";
            public Type Type { get; set; }
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
                return this.Type.IsCompatible(other.Type, context,
                    (bean) =>
                    {
                        TypeName = bean.Name;
                        Type = bean;
                    },
                    (bean) =>
                    {
                        KeyName = Type.KeyName;
                        ValueName = Type.ValueName;
                    });
            }

            public void Update()
            {
                KeyName = this.Type.KeyName;
                ValueName = this.Type.ValueName;
            }

            public void TryCopyBeanIfRemoved(Context context)
            {
                this.Type.TryCopyBeanIfRemoved(context,
                    (bean) =>
                    {
                        TypeName = bean.Name;
                        Type = bean;
                    },
                    (bean)=>
                    {
                        KeyName = Type.KeyName;
                        ValueName = Type.ValueName;
                    });
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
            public override bool IsCompatible(Type other, Context context, Action<Bean> Update, Action<Bean> UpdateVariable)
            {
                if (other == null)
                    return false;

                if (other is Bean beanOther)
                {
                    CheckResult result = context.GetCheckResult(beanOther, this);
                    if (null != result)
                    {
                        result.AddUpdate(Update, UpdateVariable);
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
                                if (context.Config.AllowSchemasReuseVariableIdWithSameType
                                    && vThis.IsCompatible(vOther, context))
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
                                Type = vOther.Type,
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
                        Bean newBean = ShadowCopy(context);
                        context.Current.AddBean(newBean);
                        result.Bean = newBean;
                        result.AddUpdate(Update, UpdateVariable);
                        foreach (var vDelete in Deleteds)
                        {
                            vDelete.TryCopyBeanIfRemoved(context);
                            newBean.Variables.Add(vDelete.Id, vDelete);
                        }
                    }
                    return true;
                }
                return false;
            }

            public override void TryCopyBeanIfRemoved(Context context, Action<Bean> Update, Action<Bean> UpdateVariable)
            {
                CheckResult result = context.GetCopyBeanIfRemovedResult(this);
                if (null != result)
                {
                    result.AddUpdate(Update, UpdateVariable);
                    return;
                }
                result = new CheckResult() { Bean = this };
                context.AddCopyBeanIfRemovedResult(this, result);

                if (Name.StartsWith("_"))
                {
                    // bean 是内部创建的，可能是原来删除的，也可能是合并改名引起的。
                    if (context.Current.Beans.TryGetValue(RealName, out var _))
                        return;

                    var newb = ShadowCopy(context);
                    newb.RealName = RealName; // 原来是新建的Bean，要使用这个。
                    context.Current.AddBean(newb);
                    result.Bean = newb;
                    result.AddUpdate(Update, UpdateVariable);
                    return;
                }

                // 通过查找当前Schemas来发现RefZero。
                if (context.Current.Beans.TryGetValue(Name, out var _))
                    return;

                var newb2 = ShadowCopy(context);
                newb2.Deleted = true;
                context.Current.AddBean(newb2);
                result.Bean = newb2;
                result.AddUpdate(Update, UpdateVariable);

                foreach (var v in Variables.Values)
                {
                    v.TryCopyBeanIfRemoved(context);
                }
            }

            private Bean ShadowCopy(Context context)
            {
                var newBean = new Bean();
                newBean.Name = context.GenerateUniqueName();
                newBean.IsBeanKey = this.IsBeanKey;
                newBean.KeyRefCount = this.KeyRefCount;
                newBean.RealName = this.Name;
                newBean.Deleted = this.Deleted;
                foreach (var v in Variables.Values)
                {
                    newBean.Variables.Add(v.Id, v);
                }
                return newBean;
            }

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
                    && KeyType.IsCompatible(other.KeyType, context,
                    (bean) =>
                    {
                        KeyName = bean.Name;
                        KeyType = bean;
                    },
                    null)
                    && ValueType.IsCompatible(other.ValueType, context,
                    (bean) =>
                    {
                        ValueName = bean.Name;
                        ValueType = bean;
                    },
                    null);
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

        public void CheckCompatible(Schemas other, Application app)
        {
            if (null == other)
                return;

            var context = new Context()
            {
                Current = this,
                Previous = other,
                Config = app.Config,
            };
            foreach (var table in Tables.Values)
            {
                var ztable = app.GetTableSlow(table.Name);
                if (null != ztable && ztable.IsNew && app.Config.DonotCheckSchemasWhenTableIsNew)
                    continue;

                if (other.Tables.TryGetValue(table.Name, out var otherTable))
                {
                    if (false == table.IsCompatible(otherTable, context))
                        throw new Exception($"Not Compatible. table={table.Name}");
                }
            }
            context.Update();
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

        private Dictionary<string, Type> BasicTypes { get; } = new Dictionary<string, Type>();

        public Type Compile(string type, string key, string value)
        {
            if (string.IsNullOrEmpty(type))
                return null;

            if (Beans.TryGetValue(type, out var bean))
                return bean;

            // 除了Bean，其他基本类型和容器类型都动态创建。
            if (BasicTypes.TryGetValue($"{type}:{key}:{value}", out var o))
                return o;

            var n = new Type()
            {
                Name = type,
                KeyName = key,
                ValueName = value,
            };
            BasicTypes.Add($"{type}:{key}:{value}", n);
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
