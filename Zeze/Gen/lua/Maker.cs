using System;
using System.Collections.Generic;
using System.IO;
using Zeze.Util;

namespace Zeze.Gen.lua
{
    public class Maker
    {
        public Project Project { get; }

        public Maker(Project project)
        {
            Project = project;
        }

        string GetBeanTypeId(Types.Type type)
        {
            if (null == type)
                return "0";
            if (false == type.IsNormalBean)
                throw new Exception("is not a normal bean");
            return ((Types.Bean)type).TypeId.ToString();
        }

        public void Make()
        {
            string genDir = Project.GenDir; // LuaGen
            string srcDir = Project.GenDir; // LuaSrc
            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            HashSet<ModuleSpace> allRefModules = new HashSet<ModuleSpace>();
            foreach (Module mod in Project.AllOrderDefineModules)
                allRefModules.Add(mod);

            FileSystem.CreateDirectory(genDir);

            string metaFileName = Path.Combine(genDir, "ZezeMeta.lua");
            using StreamWriter swMeta = Program.OpenStreamWriter(metaFileName);
            if (swMeta != null)
            {
                swMeta.WriteLine("-- auto-generated");
                swMeta.WriteLine("local meta = {}");
                swMeta.WriteLine("meta.beans = {}");
                foreach (Types.BeanKey beanKey in Project.AllBeanKeys.Values)
                {
                    allRefModules.Add(beanKey.Space);
                    BeanFormatter.MakeMeta(beanKey.Space.PathPinyin("_", beanKey.NamePinyin),
                        beanKey.TypeId, beanKey.Variables, swMeta);
                }
                foreach (Types.Bean bean in Project.AllBeans.Values)
                {
                    allRefModules.Add(bean.Space);
                    BeanFormatter.MakeMeta(bean.Space.PathPinyin("_", bean.NamePinyin),
                        bean.TypeId, bean.Variables, swMeta);
                }
                swMeta.WriteLine();
                swMeta.WriteLine("meta.protocols = {}");
                foreach (Protocol protocol in Project.AllProtocols.Values)
                {
                    allRefModules.Add(protocol.Space);
                    if (protocol is Rpc rpc)
                    {
                        swMeta.WriteLine($"meta.protocols[{protocol.TypeId}] = {{ {GetBeanTypeId(rpc.ArgumentType)}, {GetBeanTypeId(rpc.ResultType)} }}");
                        continue;
                    }
                    swMeta.WriteLine($"meta.protocols[{protocol.TypeId}] = {{ {GetBeanTypeId(protocol.ArgumentType)} }}");
                }
                swMeta.WriteLine();
                swMeta.WriteLine("return meta");
                swMeta.Close();
            }
            /*
            foreach (Service ma in Project.Services.Values)
            {
                new ServiceFormatter(ma, genDir, srcDir).Make();
            }
            */
            SortedDictionary<int, List<ModuleSpace>> sortDepth = new SortedDictionary<int, List<ModuleSpace>>();
            ModuleSpace depth0 = null;
            foreach (ModuleSpace mod in allRefModules)
            {
                int depth = mod.PathDepth();
                if (false == sortDepth.TryGetValue(depth, out var mods))
                    sortDepth.Add(depth, mods = new List<ModuleSpace>());
                mods.Add(mod);

                if (depth == 0)
                {
                    depth0 = mod; // 记住 solution 最后生成。
                    continue;
                }
                new ModuleFormatter(Project, mod, genDir, srcDir).Make();
            }
            {
                ModuleSpace solution = Project.Solution;

                using StreamWriter sw = Program.OpenStreamWriter(Path.Combine(genDir, solution.Name + ".lua"));
                if (sw != null)
                {
                    if (depth0 != null) // 引用了solution内定义的bean，先调用ModuleFormatter生成
                        new ModuleFormatter(Project, solution, genDir, srcDir).MakeGen(sw);
                    else
                    {
                        sw.WriteLine("-- auto-generated");
                        sw.WriteLine();
                        sw.WriteLine("local " + solution.Name + " = {}");
                        //sw.WriteLine("" + module.Name + ".ModuleId = " + module.Id);
                        sw.WriteLine();
                    }
                    sw.WriteLine();
                    foreach (var es in sortDepth)
                    {
                        if (es.Key == 0)
                            continue; // solution 已经生成了。

                        foreach (var e in es.Value)
                            sw.WriteLine($"{e.Path(".", null)} = require '{e.Path(".", null)}'");
                    }
                    sw.WriteLine();
                    sw.WriteLine($"return {solution.Name}");
                }
            }

            string dispatcherFileName = Path.Combine(srcDir, "Zeze.lua");
            if (false == File.Exists(dispatcherFileName))
            {
                using StreamWriter swDispatcher = Program.OpenStreamWriter(dispatcherFileName);
                if (swDispatcher != null)
                {
                    swDispatcher.WriteLine();
                    swDispatcher.WriteLine("local Zeze = {}");
                    swDispatcher.WriteLine("Zeze.ProtocolHandles = {}");
                    swDispatcher.WriteLine("Zeze.RpcContext = {}");
                    swDispatcher.WriteLine("Zeze.RpcSidSeed = 1");
                    swDispatcher.WriteLine();
                    swDispatcher.WriteLine("function ZezeDispatchRequest(p)");
                    swDispatcher.WriteLine("    local handle = Zeze.ProtocolHandles[p.TypeId]");
                    swDispatcher.WriteLine("    if (nil == handle) then");
                    swDispatcher.WriteLine("        return 0");
                    swDispatcher.WriteLine("    end");
                    swDispatcher.WriteLine("    handle(p)");
                    swDispatcher.WriteLine("    return 1-- 1 if found.not result of handle");
                    swDispatcher.WriteLine("end");
                    swDispatcher.WriteLine();
                    swDispatcher.WriteLine("function ZezeDispatchProtocol(p)");
                    swDispatcher.WriteLine("    if (p.IsRpc) then");
                    swDispatcher.WriteLine("        if (p.IsRequest) then");
                    swDispatcher.WriteLine("            return ZezeDispatchRequest(p)");
                    swDispatcher.WriteLine("        end");
                    swDispatcher.WriteLine("        local ctx = Zeze.RpcContext.remove(p.Sid)");
                    swDispatcher.WriteLine("        if (nil == ctx) then");
                    swDispatcher.WriteLine("            return 1 -- success");
                    swDispatcher.WriteLine("        end");
                    swDispatcher.WriteLine("        ctx.IsRequest = false");
                    swDispatcher.WriteLine("        if (p.IsTimeout ~= true) then");
                    swDispatcher.WriteLine("            ctx.Result = p.Result");
                    swDispatcher.WriteLine("            ctx.ResultCode = p.ResultCode");
                    swDispatcher.WriteLine("            ctx.SessionId = p.SessionId");
                    swDispatcher.WriteLine("            ctx.Service = p.Service");
                    swDispatcher.WriteLine("        end");
                    swDispatcher.WriteLine("        ctx.HandleResult(ctx)");
                    swDispatcher.WriteLine("        return 1 -- 1 if found.not result of handle");
                    swDispatcher.WriteLine("    end");
                    swDispatcher.WriteLine("    return ZezeDispatchRequest(p)");
                    swDispatcher.WriteLine("end");
                    swDispatcher.WriteLine();
                    swDispatcher.WriteLine("function ZezeSocketClose(service, sessionId)");
                    swDispatcher.WriteLine("    print('ZezeSocketClose')");
                    swDispatcher.WriteLine("end");
                    swDispatcher.WriteLine();
                    swDispatcher.WriteLine("function ZezeSendRpc(service, session, r, functionHandleResult)");
                    swDispatcher.WriteLine("    r.IsRequest = true");
                    swDispatcher.WriteLine("    r.Service = service");
                    swDispatcher.WriteLine("    r.SessionId = session");
                    swDispatcher.WriteLine("    r.HandleResult = functionHandleResult");
                    swDispatcher.WriteLine("    r.Sid = Zeze.RpcSidSeed");
                    swDispatcher.WriteLine("    Zeze.RpcSidSeed = Zeze.RpcSidSeed + 1");
                    swDispatcher.WriteLine("    Zeze.RpcContext[r.Sid] = r");
                    swDispatcher.WriteLine("    ZezeSendProtocol(service, session, r)");
                    swDispatcher.WriteLine("end");
                    swDispatcher.WriteLine();
                    swDispatcher.WriteLine("function ZezeSendRpcResult(r)");
                    swDispatcher.WriteLine("    r.IsRequest = false");
                    swDispatcher.WriteLine("    -- r.Sid same as request");
                    swDispatcher.WriteLine("    ZezeSendProtocol(r.Service, r.SessionId, r)");
                    swDispatcher.WriteLine("end");
                    swDispatcher.WriteLine();
                    swDispatcher.WriteLine("function ZezeHandshakeDone(service, sessionId)");
                    swDispatcher.WriteLine("    Zeze.CurrentService = service");
                    swDispatcher.WriteLine("    Zeze.CurrentSessionId = sessionId");
                    swDispatcher.WriteLine("    -- connection ready. write you code here.");
                    swDispatcher.WriteLine("end");
                    swDispatcher.WriteLine();
                    swDispatcher.WriteLine("return Zeze");
                }
            }
        }
    }
}
