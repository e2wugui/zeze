package Zeze.Gen.lua;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Maker {
	private Project Project;
	public final Project getProject() {
		return Project;
	}

	public Maker(Project project) {
		Project = project;
	}

	private String GetBeanTypeId(Types.Type type) {
		if (null == type) {
			return "0";
		}
		if (false == type.isNormalBean()) {
			throw new RuntimeException("is not a normal bean");
		}
		return String.valueOf(((Types.Bean)type).getTypeId());
	}

	public final void Make() {
		String projectBasedir = getProject().getGendir();
		String projectDir = Paths.get(projectBasedir).resolve(getProject().getName()).toString();
		String genDir = Paths.get(projectDir).resolve("LuaGen").toString();
		String srcDir = Paths.get(projectDir).resolve("LuaSrc").toString();

		if ((new File(genDir)).isDirectory()) {
			Directory.Delete(genDir, true);
		}

		HashSet<ModuleSpace> allRefModules = new HashSet<ModuleSpace>();
		for (Module mod : getProject().getAllModules().values()) {
			allRefModules.add(mod);
		}

		(new File(genDir)).mkdirs();

		String metaFileName = Paths.get(genDir).resolve("ZezeMeta.lua").toString();
		try (OutputStreamWriter swMeta = new OutputStreamWriter(metaFileName, java.nio.charset.StandardCharsets.UTF_8)) {
			swMeta.write("-- auto-generated" + System.lineSeparator());
			swMeta.write("local meta = {}" + System.lineSeparator());
			swMeta.write("meta.beans = {}" + System.lineSeparator());
			for (Types.BeanKey beanKey : getProject().getAllBeanKeys().values()) {
				allRefModules.add(beanKey.getSpace());
				BeanFormatter.MakeMeta(beanKey.getSpace().PathPinyin("_", beanKey.getNamePinyin()), beanKey.getTypeId(), beanKey.getVariables(), swMeta);
			}
			for (Types.Bean bean : getProject().getAllBeans().values()) {
				allRefModules.add(bean.getSpace());
				BeanFormatter.MakeMeta(bean.getSpace().PathPinyin("_", bean.getNamePinyin()), bean.getTypeId(), bean.getVariables(), swMeta);
			}
			swMeta.WriteLine();
			swMeta.write("meta.protocols = {}" + System.lineSeparator());
			for (Protocol protocol : getProject().getAllProtocols().values()) {
				allRefModules.add(protocol.getSpace());
				boolean tempVar = protocol instanceof Rpc;
				Rpc rpc = tempVar ? (Rpc)protocol : null;
				if (tempVar) {
					swMeta.write(String.format("meta.protocols[%1$s] = { %2$s, %3$s }", protocol.getTypeId(), GetBeanTypeId(rpc.getArgumentType()), GetBeanTypeId(rpc.getResultType())) + System.lineSeparator());
					continue;
				}
				swMeta.write(String.format("meta.protocols[%1$s] = { %2$s }", protocol.getTypeId(), GetBeanTypeId(protocol.getArgumentType())) + System.lineSeparator());
			}
			swMeta.WriteLine();
			swMeta.write("return meta" + System.lineSeparator());
			swMeta.close();
			/*
			foreach (Service ma in Project.Services.Values)
			{
			    new ServiceFormatter(ma, genDir, srcDir).Make();
			}
			*/
			TreeMap<Integer, ArrayList<ModuleSpace>> sortDepth = new TreeMap<Integer, ArrayList<ModuleSpace>>();
			ModuleSpace depth0 = null;
			for (ModuleSpace mod : allRefModules) {
				int depth = mod.PathDepth();
				TValue mods;
				if (false == (sortDepth.containsKey(depth) && (mods = sortDepth.get(depth)) == mods)) {
					sortDepth.put(depth, mods = new ArrayList<ModuleSpace>());
				}
				mods.Add(mod);
    
				if (depth == 0) {
					depth0 = mod; // 记住 solution 最后生成。
					continue;
				}
				(new ModuleFormatter(getProject(), mod, genDir, srcDir)).Make();
			} {
				ModuleSpace solution = getProject().getSolution();
    
				try (OutputStreamWriter sw = new OutputStreamWriter(Paths.get(genDir).resolve(solution.getName() + ".lua").toString(), java.nio.charset.StandardCharsets.UTF_8)) {
					if (null != depth0) { // 引用了solution内定义的bean，先调用ModuleFormatter生成
						(new ModuleFormatter(getProject(), solution, genDir, srcDir)).MakeGen(sw);
					}
					else {
						sw.write("-- auto-generated" + System.lineSeparator());
						sw.WriteLine();
						sw.write("local " + solution.getName() + " = {}" + System.lineSeparator());
						//sw.WriteLine("" + module.Name + ".ModuleId = " + module.Id);
						sw.WriteLine();
					}
					sw.write("" + System.lineSeparator());
					for (var es : sortDepth.entrySet()) {
						if (es.getKey() == 0) {
							continue; // solution 已经生成了。
						}
        
						for (var e : es.getValue()) {
							sw.write(String.format("%1$s = require '%2$s'", e.Path(".", null), e.Path(".", null)) + System.lineSeparator());
						}
					}
					sw.write("" + System.lineSeparator());
					sw.write(String.valueOf(String.format("return %1$s", solution.getName())) + System.lineSeparator());
				}
			}
    
			String dispatcherFileName = Paths.get(srcDir).resolve("Zeze.lua").toString();
			if (false == (new File(dispatcherFileName)).isFile()) {
				try (OutputStreamWriter swDispatcher = new OutputStreamWriter(dispatcherFileName, java.nio.charset.StandardCharsets.UTF_8)) {
        
					swDispatcher.write("" + System.lineSeparator());
					swDispatcher.write("local Zeze = { }" + System.lineSeparator());
					swDispatcher.write("Zeze.ProtocolHandles = { }" + System.lineSeparator());
					swDispatcher.write("Zeze.RpcContext = { }" + System.lineSeparator());
					swDispatcher.write("Zeze.RpcSidSeed = 1" + System.lineSeparator());
					swDispatcher.write("" + System.lineSeparator());
					swDispatcher.write("function ZezeDispatchRequest(p)" + System.lineSeparator());
					swDispatcher.write("    local handle = Zeze.ProtocolHandles[p.TypeId]" + System.lineSeparator());
					swDispatcher.write("    if (nil == handle) then" + System.lineSeparator());
					swDispatcher.write("        return 0" + System.lineSeparator());
					swDispatcher.write("    end" + System.lineSeparator());
					swDispatcher.write("    handle(p)" + System.lineSeparator());
					swDispatcher.write("    return 1-- 1 if found.not result of handle" + System.lineSeparator());
					swDispatcher.write("end" + System.lineSeparator());
					swDispatcher.write("" + System.lineSeparator());
					swDispatcher.write("function ZezeDispatchProtocol(p)" + System.lineSeparator());
					swDispatcher.write("    if (p.IsRpc) then" + System.lineSeparator());
					swDispatcher.write("        if (p.IsRequest) then" + System.lineSeparator());
					swDispatcher.write("            return ZezeDispatchRequest(p)" + System.lineSeparator());
					swDispatcher.write("        end" + System.lineSeparator());
					swDispatcher.write("        local ctx = Zeze.RpcContext.remove(p.Sid)" + System.lineSeparator());
					swDispatcher.write("        if (nil == ctx) then" + System.lineSeparator());
					swDispatcher.write("            return 1 -- success" + System.lineSeparator());
					swDispatcher.write("        end" + System.lineSeparator());
					swDispatcher.write("        ctx.IsRequest = false" + System.lineSeparator());
					swDispatcher.write("        if (p.IsTimeout ~= true) then" + System.lineSeparator());
					swDispatcher.write("            ctx.Result = p.Result" + System.lineSeparator());
					swDispatcher.write("            ctx.ResultCode = p.ResultCode" + System.lineSeparator());
					swDispatcher.write("            ctx.SessionId = p.SessionId" + System.lineSeparator());
					swDispatcher.write("            ctx.Service = p.Service" + System.lineSeparator());
					swDispatcher.write("        end" + System.lineSeparator());
					swDispatcher.write("        ctx.HandleResult(ctx)" + System.lineSeparator());
					swDispatcher.write("        return 1 -- 1 if found.not result of handle" + System.lineSeparator());
					swDispatcher.write("    end" + System.lineSeparator());
					swDispatcher.write("    return ZezeDispatchRequest(p)" + System.lineSeparator());
					swDispatcher.write("end" + System.lineSeparator());
					swDispatcher.write("" + System.lineSeparator());
					swDispatcher.write("function ZezeSocketClose(service, sessionId)" + System.lineSeparator());
					swDispatcher.write("    print('ZezeSocketClose')" + System.lineSeparator());
					swDispatcher.write("end" + System.lineSeparator());
					swDispatcher.write("" + System.lineSeparator());
					swDispatcher.write("function ZezeSendRpc(service, session, r, functionHandleResult)" + System.lineSeparator());
					swDispatcher.write("    r.IsRequest = true" + System.lineSeparator());
					swDispatcher.write("    r.Service = service" + System.lineSeparator());
					swDispatcher.write("    r.SessionId = session" + System.lineSeparator());
					swDispatcher.write("    r.HandleResult = functionHandleResult" + System.lineSeparator());
					swDispatcher.write("    r.Sid = Zeze.RpcSidSeed" + System.lineSeparator());
					swDispatcher.write("    Zeze.RpcSidSeed = Zeze.RpcSidSeed + 1" + System.lineSeparator());
					swDispatcher.write("    Zeze.RpcContext[r.Sid] = r" + System.lineSeparator());
					swDispatcher.write("    ZezeSendProtocol(service, session, r)" + System.lineSeparator());
					swDispatcher.write("end" + System.lineSeparator());
					swDispatcher.write("" + System.lineSeparator());
					swDispatcher.write("function ZezeSendRpcResult(r)" + System.lineSeparator());
					swDispatcher.write("    r.IsRequest = false" + System.lineSeparator());
					swDispatcher.write("    -- r.Sid same as request" + System.lineSeparator());
					swDispatcher.write("    ZezeSendProtocol(r.Service, r.SessionId, r)" + System.lineSeparator());
					swDispatcher.write("end" + System.lineSeparator());
					swDispatcher.write("" + System.lineSeparator());
					swDispatcher.write("function ZezeHandshakeDone(service, sessionId)" + System.lineSeparator());
					swDispatcher.write("    Zeze.CurrentService = service" + System.lineSeparator());
					swDispatcher.write("    Zeze.CurrentSessionId = sessionId" + System.lineSeparator());
					swDispatcher.write("    -- connection ready. write you code here." + System.lineSeparator());
					swDispatcher.write("end" + System.lineSeparator());
					swDispatcher.write("" + System.lineSeparator());
					swDispatcher.write("return Zeze" + System.lineSeparator());
				}
			}
		}
	}
}
