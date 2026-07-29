using System.Collections;
using System.Collections.Generic;
using Scriban.Runtime;
using Zeze.Gen.Types;

namespace Zeze.Gen.luaClient
{
    /// <summary>
    /// 把 Project 的业务对象图物化成 Scriban 数据（ScriptObject/ScriptArray/标量），产出镜像 Project 的顶层 Model
    /// （modules/beans/beankeys/protocols/solution）。三步、完全显式、不依赖反射，NativeAOT/trim 安全：
    ///   Pass1 BuildAll：用显式工作栈为每个对象建 ScriptObject 壳（只填标量）并注册 _map——迭代非递归，
    ///     _map 去重保证每个对象只建壳一次，并切断对象图里的循环引用（ModuleSpace.Solution→自身、Bean↔Variable↔Type 等）；
    ///   Pass2 ResolveAll：填引用字段（查 _map，无递归）；
    ///   BuildTopModel：组装顶层 Model + RefModules。
    /// 同一对象多处引用共享同一 ScriptObject（模板里的 == 比较成立）。
    /// 性能：ScriptObject 字段一律用 Add(string,object)（collection initializer {{k,v}} 或显式 so.Add），不用索引器 this[k]=v。
    /// 原因：Scriban 7.x 的索引器 set 走 TrySetValue（CanWrite→TryGetValue + AssertNotReadOnly×2 + new SourceSpan），
    /// 实测比 Add（直接 Store.Add）慢约 400x，26895 个对象 × 多字段会吃掉 ~2s。
    /// </summary>
    internal sealed class ScriptModelBuilder
    {
        readonly Project _project;
        readonly Dictionary<object, ScriptObject> _map = new();
        HashSet<ModuleSpace> _refModuleSet;
        // ResolveAll 期间复用的全量集合（避免每个 module 重建 HashSet(全量)）
        HashSet<Bean> _allBeansSet;
        HashSet<BeanKey> _allBeanKeysSet;
        HashSet<Protocol> _allProtocolsSet;

        /// <summary>镜像 Project 结构的顶层模型：modules/beans/beankeys/protocols/solution 分门别类。Make 时直接消费。</summary>
        public ScriptObject Model { get; private set; }

        /// <summary>含内容的原始 module（供 Maker 控制流：文件路径、HandleFlags、FileChunkGen 用）。</summary>
        public List<ModuleSpace> RefModules { get; private set; }

        public ScriptModelBuilder(Project project) => _project = project;

        /// <summary>Maker 控制流用原始对象，模板数据用其 ScriptObject——按原始对象回查已物化的壳。</summary>
        public ScriptObject Get(object obj) => _map.GetValueOrDefault(obj);

        /// <summary>三步：Pass1 建壳 + Pass2 填引用 + 组装顶层 Model。</summary>
        public void Build()
        {
            BuildAll();
            ResolveAll();
            BuildTopModel();
        }

        /// <summary>所有"参与生成"的 module（复刻 Maker 原 allRefModules 语义）：AllOrderDefineModules ∪ Bean/BeanKey/Protocol 的 Space。结果缓存。</summary>
        private HashSet<ModuleSpace> CollectRefModules()
        {
            if (_refModuleSet != null) return _refModuleSet;
            var set = new HashSet<ModuleSpace>();
            foreach (Module m in _project.AllOrderDefineModules) set.Add(m);
            foreach (var bk in _project.AllBeanKeys.Values) set.Add(bk.Space);
            foreach (var b in _project.AllBeans.Values) set.Add(b.Space);
            foreach (var p in _project.AllProtocols.Values) set.Add(p.Space);
            return _refModuleSet = set;
        }

        // ===================== Pass 1：为每个业务对象建 ScriptObject 壳（只填标量属性）并注册 =====================
        // 迭代式（显式工作栈），非递归：对象先注册 _map，再把子对象压栈；_map 去重保证每个对象只建壳一次，
        // 并切断对象图里的循环引用（ModuleSpace.Solution 指回自身、Bean↔Variable↔Type、TypeDynamic.RealBeans→Bean 等）。
        // 大 solution 下也不会栈溢出/CPU 卡死。
        private void BuildAll()
        {
            var pending = new Stack<object>();
            Enqueue(pending, CollectRefModules());
            Enqueue(pending, _project.AllBeans.Values);
            Enqueue(pending, _project.AllBeanKeys.Values);
            Enqueue(pending, _project.AllProtocols.Values);
            pending.Push(_project.Solution);

            while (pending.TryPop(out var obj))
            {
                if (obj == null || _map.ContainsKey(obj)) continue; // 已建壳则跳过（去重 + 切断循环）
                switch (obj)
                {
                    case Bean b:
                        // is_bean：Bean 作 variable_type 时（变量类型直接是 bean），模板据此走 build_index/build_newindex 分支。
                        _map[b] = new ScriptObject { {"name", b.Name}, {"full_name", b.FullName}, {"type_id", b.TypeId}, {"is_bean", true} };
                        Enqueue(pending, b.Variables);
                        Enqueue(pending, b.Enums);
                        break;
                    case BeanKey k:
                        // 同 Bean：BeanKey.IsBean 恒 true（Kind=beankey），作 variable_type 时需要 is_bean。
                        _map[k] = new ScriptObject { {"name", k.Name}, {"full_name", k.FullName}, {"type_id", k.TypeId}, {"is_bean", true} };
                        Enqueue(pending, k.Variables);
                        Enqueue(pending, k.Enums);
                        break;
                    case Variable v:
                        _map[v] = new ScriptObject { {"name", v.Name}, {"id", v.Id}, {"type", v.Type}, {"initial", v.Initial} };
                        pending.Push(v.VariableType);
                        break;
                    case Rpc r:
                        _map[r] = new ScriptObject { {"name", r.Name}, {"full_name", r.FullName}, {"id", r.Id}, {"type_id", r.TypeId}, {"result", true} };
                        pending.Push(r.ArgumentType);
                        pending.Push(r.ResultType);
                        pending.Push(r.Space);
                        Enqueue(pending, r.Enums);
                        break;
                    case Protocol p:
                        _map[p] = new ScriptObject { {"name", p.Name}, {"full_name", p.FullName}, {"id", p.Id}, {"type_id", p.TypeId}, {"result", false} };
                        pending.Push(p.ArgumentType);
                        pending.Push(p.Space);
                        Enqueue(pending, p.Enums);
                        break;
                    case Types.Type t:
                    {
                        // full_name=Name：模板 variable_type.full_name ?? variable_type 取到 full_name(=Name)，
                        // 等价旧反射版（Type 无 full_name 属性→null，fallback 到 Type.ToString()=Name）。Bean 类型的 variable_type 走上面的 Bean case。
                        var so = new ScriptObject { {"name", t.Name}, {"is_bean", t.IsBean}, {"is_collection", t.IsCollection}, {"full_name", t.Name} };
                        _map[t] = so;
                        if (t is TypeMap m) { pending.Push(m.KeyType); pending.Push(m.ValueType); }
                        else if (t is TypeSortedMap sm) { pending.Push(sm.KeyType); pending.Push(sm.ValueType); }
                        else if (t is TypeDynamic d) { Enqueue(pending, d.RealBeans.Values); }
                        else if (t is TypeCollection c) { pending.Push(c.ValueType); }
                        break;
                    }
                    case ModuleSpace ms:
                    {
                        // 仅 Module 暴露 full_name（= Module.FullName=Path()）；Solution 不暴露——镜像旧反射版：
                        // ModuleSpace 基类没有 full_name 属性，反射为 null（决定 LuaRoot 的 module_names 是否含 solution、
                        // LuaMeta 的 package.loaded[''] 等）。
                        var so = new ScriptObject { {"name", ms.Name}, {"id", ms.Id} };
                        if (ms is Module) so.Add("full_name", ms.Path());
                        _map[ms] = so;
                        pending.Push(ms.Solution); // 循环引用（Solution.Solution==Solution）由 _map 去重切断
                        Enqueue(pending, ms.Beans.Values);
                        Enqueue(pending, ms.BeanKeys.Values);
                        Enqueue(pending, ms.Protocols.Values);
                        Enqueue(pending, ms.Enums); // module 级 enum 也建壳（模板 module.enums 访问 enum.name/value，AOT 下反射会失败）
                        break;
                    }
                    case Types.Enum e:
                        _map[e] = new ScriptObject { {"name", e.Name}, {"value", e.Value} };
                        break;
                }
            }
        }

        static void Enqueue(Stack<object> stack, IEnumerable seq)
        {
            if (seq == null) return;
            foreach (var o in seq) stack.Push(o);
        }

        // ===================== Pass 2：填充引用字段（查 _map，O(1)，无递归） =====================
        // 用 so.Add：Pass1 只填了标量，引用字段 key 与标量不重叠，Add 安全且比索引器 set 快 ~400x。
        private void ResolveAll()
        {
            // 预建全量集合一次，后续 124 个 module 复用，避免每次 IntersectScript 重建 HashSet(全量)
            _allBeansSet = new HashSet<Bean>(_project.AllBeans.Values);
            _allBeanKeysSet = new HashSet<BeanKey>(_project.AllBeanKeys.Values);
            _allProtocolsSet = new HashSet<Protocol>(_project.AllProtocols.Values);
            foreach (var key in _map.Keys)
                ResolveRef(key);
        }

        private void ResolveRef(object obj)
        {
            if (!_map.TryGetValue(obj, out var so)) return;
            switch (obj)
            {
                case Bean b:
                    so.Add("variables", ToScript(b.Variables));
                    so.Add("enums", ToScript(b.Enums));
                    break;
                case BeanKey k:
                    so.Add("variables", ToScript(k.Variables));
                    so.Add("enums", ToScript(k.Enums));
                    break;
                case Variable v:
                    so.Add("variable_type", ToScript(v.VariableType));
                    break;
                case Rpc r:
                    so.Add("argument_type", ToScript(r.ArgumentType));
                    so.Add("result_type", ToScript(r.ResultType));
                    so.Add("space", ToScript(r.Space));
                    so.Add("enums", ToScript(r.Enums));
                    break;
                case Protocol p:
                    so.Add("argument_type", ToScript(p.ArgumentType));
                    so.Add("space", ToScript(p.Space));
                    so.Add("enums", ToScript(p.Enums));
                    break;
                case TypeMap m:
                    so.Add("key_type", ToScript(m.KeyType));
                    so.Add("value_type", ToScript(m.ValueType));
                    break;
                case TypeSortedMap sm:
                    so.Add("key_type", ToScript(sm.KeyType));
                    so.Add("value_type", ToScript(sm.ValueType));
                    break;
                case TypeDynamic d:
                    so.Add("real_beans", ToScript(d.RealBeans));
                    break;
                case TypeCollection c:
                    so.Add("value_type", ToScript(c.ValueType));
                    break;
                case ModuleSpace ms:
                    // 过滤为 ∩AllBeans/AllBeanKeys/AllProtocols 的子集，与 Maker 原 Intersect 语义一致
                    so.Add("beans", IntersectScript(ms.Beans.Values, _allBeansSet));
                    so.Add("beankeys", IntersectScript(ms.BeanKeys.Values, _allBeanKeysSet));
                    so.Add("protocols", IntersectScript(ms.Protocols.Values, _allProtocolsSet));
                    so.Add("enums", ToScript(ms.Enums));
                    so.Add("solution", ToScript(ms.Solution));
                    break;
            }
        }

        /// <summary>src ∩ allSet，命中且已物化的收集成 ScriptArray（保持 src 原顺序）。allSet 由 ResolveAll 预建复用。</summary>
        private ScriptArray IntersectScript<T>(IEnumerable<T> src, HashSet<T> allSet) where T : class
        {
            var arr = new ScriptArray();
            foreach (var x in src)
                if (allSet.Contains(x) && _map.TryGetValue(x, out var so))
                    arr.Add(so);
            return arr;
        }

        // ===================== 组装顶层 Model（镜像 Project）+ RefModules（原始对象，控制流用） =====================
        private void BuildTopModel()
        {
            var modulesArr = new ScriptArray();
            RefModules = new List<ModuleSpace>();
            foreach (var ms in CollectRefModules())
            {
                if (!_map.TryGetValue(ms, out var so)) continue;
                var beans = (ScriptArray)so["beans"];
                var beankeys = (ScriptArray)so["beankeys"];
                var protocols = (ScriptArray)so["protocols"];
                if (beans != null && beans.Count == 0 && beankeys.Count == 0 && protocols.Count == 0) continue;
                modulesArr.Add(so); // ScriptObject（AOT 安全：避免反射 module.full_name/name）
                RefModules.Add(ms);
            }

            // Solution 不设 full_name（镜像反射版 Solution 无此属性），LuaRoot 的 module_names = map 'full_name' 会得到 null→循环跳过，与 genold 一致。
            Model = new ScriptObject
            {
                {"modules", modulesArr},
                {"beans", ToScript(_project.AllBeans.Values)},
                {"beankeys", ToScript(_project.AllBeanKeys.Values)},
                {"protocols", ToScript(_project.AllProtocols.Values)},
                {"solution", ToScript(_project.Solution)},
            };
        }

        // ===================== 渲染期：业务对象查 _map，标量/集合就地处理，不递归物化 =====================
        public object ToScript(object obj)
        {
            if (obj == null) return null;
            if (_map.TryGetValue(obj, out var so)) return so;
            switch (obj)
            {
                case string:
                case int:
                case long:
                case bool:
                case double:
                case float:
                case byte:
                case short:
                case decimal:
                    return obj;
                case IDictionary dict:
                {
                    var arr = new ScriptArray();
                    foreach (DictionaryEntry entry in dict)
                        arr.Add(new ScriptObject { {"key", ToScript(entry.Key)}, {"value", ToScript(entry.Value)} });
                    return arr;
                }
                case IEnumerable seq:
                {
                    var arr = new ScriptArray();
                    foreach (var item in seq) arr.Add(ToScript(item));
                    return arr;
                }
                default:
                    return obj.ToString();
            }
        }
    }
}
