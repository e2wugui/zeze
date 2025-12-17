using System.Collections.Generic;
using System.IO;

namespace Zeze.Gen.cs
{
    public class ModuleFormatter
    {
        readonly Project project;
        internal readonly Module module;
        readonly string genDir;
        readonly string srcDir;

        public ModuleFormatter(Project project, Module module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakeInterface();
            MakePartialImplement();
            MakePartialImplementInGen();
        }

        private string GetCollectionLogTemplateName(Types.Type type)
        {
            if (type is Types.TypeList tlist)
            {
                string value = rrcs.TypeName.GetName(tlist.ValueType);
                return "Zeze.Raft.RocksRaft.LogList" + (tlist.ValueType.IsNormalBeanOrRocks ? "2<" : "1<") + value + ">";
            }
            else if (type is Types.TypeSet tset)
            {
                string value = rrcs.TypeName.GetName(tset.ValueType);
                return "Zeze.Raft.RocksRaft.LogSet1<" + value + ">";
            }
            else if (type is Types.TypeMap tmap)
            {
                string key = rrcs.TypeName.GetName(tmap.KeyType);
                string value = rrcs.TypeName.GetName(tmap.ValueType);
                var version = tmap.ValueType.IsNormalBeanOrRocks ? "2<" : "1<";
                return $"Zeze.Raft.RocksRaft.LogMap{version}{key}, {value}>";
            }
            throw new System.Exception();
        }

        public void RegisterRocksTables(StreamWriter sw)
        {
            var depends = new HashSet<Types.Type>();
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks)
                {
                    var key = TypeName.GetName(table.KeyType);
                    var value = TypeName.GetName(table.ValueType);
                    table.Depends(depends, null);
                    sw.WriteLine($"            rocks.RegisterTableTemplate<{key}, {value}>(\"{table.Name}\");");
                }
            }
            var tlogs = new HashSet<string>();
            foreach (var dep in depends)
            {
                if (dep.IsCollection)
                {
                    tlogs.Add(GetCollectionLogTemplateName(dep));
                    continue;
                }
                if (dep.IsNormalBeanOrRocks)
                    continue;
                tlogs.Add($"Zeze.Raft.RocksRaft.Log<{TypeName.GetName(dep)}>");
            }
            foreach (var tlog in tlogs)
            {
                sw.WriteLine($"            Zeze.Raft.RocksRaft.Rocks.RegisterLog<{tlog}>();");
            }
        }

        public void RegisterZezeTables(StreamWriter sw, string zeze = null)
        {
            var zezeVar = string.IsNullOrEmpty(zeze) ? "App.Zeze" : zeze;
            sw.WriteLine("            // register table");
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks == false)
                    sw.WriteLine($"            {zezeVar}.AddTable({zezeVar}.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
        }

        public void UnRegisterZezeTables(StreamWriter sw, string zeze = null)
        {
            var zezeVar = string.IsNullOrEmpty(zeze) ? "App.Zeze" : zeze;
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks == false)
                    sw.WriteLine($"            {zezeVar}.RemoveTable({zezeVar}.Config.GetTableConf(_{table.Name}.Name).DatabaseName, _{table.Name});");
            }
        }

        public void RegisterProtocols(StreamWriter sw, bool isFirst = true, string serviceVarName = null)
        {
            sw.WriteLine("            // register protocol factory and handles");
            if (isFirst)
                sw.WriteLine("            var _reflect = new Zeze.Util.Reflect(GetType());");
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                var serviceVar = string.IsNullOrEmpty(serviceVarName) ? $"App.{serv.Name}" : serviceVarName;
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"            {serviceVar}.AddFactoryHandle({rpc.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                        sw.WriteLine("            {");
                        sw.WriteLine($"                Factory = () => new {rpc.Space.Path(".", rpc.Name)}(),");
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine($"                Handle = Process{rpc.Name}Request,");
                            sw.WriteLine($"                TransactionLevel = _reflect.GetTransactionLevel(\"Process{rpc.Name}Request\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel}),");
                            sw.WriteLine($"                Mode = _reflect.GetDispatchMode(\"Process{rpc.Name}Request\", Zeze.Transaction.DispatchMode.Normal),");
                        }
                        else
                        {
                            sw.WriteLine($"                TransactionLevel = _reflect.GetTransactionLevel(\"Process{rpc.Name}Response\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel}),");
                            sw.WriteLine($"                Mode = _reflect.GetDispatchMode(\"Process{rpc.Name}Response\", Zeze.Transaction.DispatchMode.Normal),");
                        }
                        sw.WriteLine("            });");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"            {serviceVar}.AddFactoryHandle({p.TypeId}, new Zeze.Net.Service.ProtocolFactoryHandle()");
                        sw.WriteLine("            {");
                        sw.WriteLine($"                Factory = () => new {p.Space.Path(".", p.Name)}(),");
                        sw.WriteLine($"                Handle = Process{p.Name},");
                        sw.WriteLine($"                TransactionLevel = _reflect.GetTransactionLevel(\"Process{p.Name}p\", Zeze.Transaction.TransactionLevel.{p.TransactionLevel}),");
                        sw.WriteLine($"                Mode = _reflect.GetDispatchMode(\"Process{p.Name}\", Zeze.Transaction.DispatchMode.Normal),");
                        sw.WriteLine("            });");
                    }
                }
            }
        }

        public void UnRegisterProtocols(StreamWriter sw, string serviceVarName = null)
        {
            Service serv = module.ReferenceService;
            if (serv != null)
            {
                var serviceVar = string.IsNullOrEmpty(serviceVarName) ? $"App.{serv.Name}" : serviceVarName;
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        // rpc 可能作为客户端发送也需要factory，所以总是注册factory。
                        sw.WriteLine($"            {serviceVar}.Factorys.TryRemove({rpc.TypeId}, out var _);");
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"            {serviceVar}.Factorys.TryRemove({p.TypeId}, out var _);");
                    }
                }
            }
        }

        public void DefineZezeTables(StreamWriter sw)
        {
            foreach (Table table in module.Tables.Values)
            {
                if (project.GenTables.Contains(table.Gen) && table.IsRocks == false)
                {
                    sw.WriteLine($"        internal {table.FullName} _{table.Name} = new();");
                }
            }
        }

        public void MakePartialImplementInGen()
        {
            using StreamWriter sw = module.OpenWriter(genDir, $"Module{module.Name}Gen.cs");
            if (sw == null)
                return;
            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public partial class Module{module.Name} : AbstractModule");
            sw.WriteLine("    {");
            sw.WriteLine($"        public const int ModuleId = {module.Id};");
            sw.WriteLine();
            DefineZezeTables(sw);
            sw.WriteLine();
            sw.WriteLine($"        public global::{project.Solution.Name}.App App {{ get; }}");
            sw.WriteLine();
            sw.WriteLine($"        public Module{module.Name}(global::{project.Solution.Name}.App app)");
            sw.WriteLine("        {");
            sw.WriteLine("            App = app;");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public override void Register()");
            sw.WriteLine("        {");
            RegisterProtocols(sw);
            RegisterZezeTables(sw);
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public override void UnRegister()");
            sw.WriteLine("        {");
            UnRegisterProtocols(sw);
            UnRegisterZezeTables(sw);
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void GenEmptyProtocolHandles(StreamWriter sw, bool shortIf = true)
        {
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine($"        protected override async System.Threading.Tasks.Task<long> Process" + rpc.Name + "Request(Zeze.Net.Protocol _p)");
                            sw.WriteLine("        {");
                            sw.WriteLine($"            var p = _p as {(shortIf ? rpc.ShortNameIf(module) : rpc.FullName)};");
                            sw.WriteLine("            return Zeze.Util.ResultCode.NotImplement;");
                            sw.WriteLine("        }");
                            sw.WriteLine();
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine($"        protected override async System.Threading.Tasks.Task<long> Process" + p.Name + "(Zeze.Net.Protocol _p)");
                        sw.WriteLine("        {");
                        sw.WriteLine($"            var p = _p as {(shortIf ? p.ShortNameIf(module) : p.FullName)};");
                        sw.WriteLine("            return Zeze.Util.ResultCode.NotImplement;");
                        sw.WriteLine("        }");
                        sw.WriteLine();
                    }
                }
            }
        }

        public void MakePartialImplement()
        {
            using StreamWriter sw = module.OpenWriter(srcDir, $"Module{module.Name}.cs", false);
            if (sw == null)
                return;

            sw.WriteLine();
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public partial class Module{module.Name} : AbstractModule");
            sw.WriteLine("    {");
            sw.WriteLine("        public void Start(global::" + project.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void Stop(global::" + project.Solution.Name + ".App app)");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine();

            GenEmptyProtocolHandles(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void GenEnums(StreamWriter sw)
        {
            // declare enums
            if (module.Enums.Count > 0)
                sw.WriteLine();
            foreach (Types.Enum e in module.Enums)
                sw.WriteLine($"        public const {TypeName.GetName(Types.Type.Compile(e.Type))} {e.Name} = {e.Value};{e.Comment}");
        }

        public void GenAbstractProtocolHandles(StreamWriter sw)
        {
            if (module.ReferenceService != null)
            {
                int serviceHandleFlags = module.ReferenceService.HandleFlags;
                foreach (Protocol p in module.Protocols.Values)
                {
                    if (p is Rpc rpc)
                    {
                        if ((rpc.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags) != 0)
                        {
                            sw.WriteLine();
                            sw.WriteLine($"        protected abstract System.Threading.Tasks.Task<long>  Process" + rpc.Name + "Request(Zeze.Net.Protocol p);");
                        }
                        continue;
                    }
                    if (0 != (p.HandleFlags & serviceHandleFlags & Program.HandleCSharpFlags))
                    {
                        sw.WriteLine();
                        sw.WriteLine($"        protected abstract System.Threading.Tasks.Task<long>  Process" + p.Name + "(Zeze.Net.Protocol p);");
                    }
                }
            }
        }

        public void MakeInterface()
        {
            using StreamWriter sw = module.OpenWriter(genDir, "AbstractModule.cs");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            if (module.Comment.Length > 0)
                sw.WriteLine(module.Comment);
            sw.WriteLine("// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable");
            sw.WriteLine("// ReSharper disable once CheckNamespace");
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public abstract class AbstractModule : Zeze.IModule");
            sw.WriteLine("    {");
            sw.WriteLine($"        public override string FullName => \"{module.Path()}\";");
            sw.WriteLine($"        public override string Name => \"{module.Name}\";");
            sw.WriteLine($"        public override int Id => {module.Id};");
            GenEnums(sw);
            GenAbstractProtocolHandles(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
