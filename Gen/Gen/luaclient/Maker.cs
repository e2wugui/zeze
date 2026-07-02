using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using Scriban;
using Scriban.Parsing;
using Scriban.Runtime;
using Zeze.Gen.Types;
using Zeze.Util;

namespace Zeze.Gen.luaClient
{
    public class Maker
    {
        Project Project { get; }

        public Maker(Project project)
        {
            Project = project;
        }

        static string GetTemplate(string fileName)
        {
            using var stream = Assembly.GetEntryAssembly()?.GetManifestResourceStream($"Gen.templates.{fileName}");
            if (stream == null) return "";
            TextReader tr = new StreamReader(stream);

            return tr.ReadToEnd();
        }
        public string FirstLetterToUpper(string str)
        {
            if (str == null)
                return null;

            if (str.Length > 1)
                return Program.Upper1(str);

            return str.ToUpper();
        }


        private string Render(Template template, Dictionary<string, object> model)
        {
            var scriptObject = new ScriptObject();
            if (model != null)
            {
                // 把整棵 model 树一次性物化成纯 ScriptObject/ScriptArray，模板渲染时只做字典查找，
                // 完全不经过 .NET 反射——这样 NativeAOT/trim 不会破坏任何成员访问
                // （之前反射访问 protocol.space、KeyValuePair.value 等都被 trim 破坏成 null）。
                foreach (var kv in model)
                    scriptObject[kv.Key] = ToScript(kv.Value, new HashSet<object>());
            }
            TemplateContext context = template.LexerOptions.Lang == ScriptLang.Liquid ? new LiquidTemplateContext() : new TemplateContext();
            context.LoopLimit = 0;
            context.RecursiveLimit = 0;
            context.PushGlobal(scriptObject);
            return template.Render(context);
        }

        /// <summary>
        /// 把业务对象转成纯 Scriban 数据（ScriptObject/ScriptArray/标量），只暴露 luaclient 模板实际用到的属性。
        /// 完全显式、不依赖反射：NativeAOT/trim 安全。path 记录当前递归路径以切断对象图循环
        /// （ModuleSpace.Solution 指回自身、Protocol↔Space、Bean↔Variable 等），物化完即移除以允许跨路径共享。
        /// </summary>
        private static object ToScript(object obj, HashSet<object> path)
        {
            switch (obj)
            {
                case null: return null;
                case Bean b: return BeanToScript(b, path);
                case BeanKey bk: return BeanKeyToScript(bk, path);
                case Variable v: return VariableToScript(v, path);
                case TypeMap tm: return TypeToScript(tm, path);
                case TypeList tl: return TypeToScript(tl, path);
                case TypeSet ts: return TypeToScript(ts, path);
                case TypeDynamic td: return TypeToScript(td, path);
                case Types.Type t: return TypeToScript(t, path);
                case Rpc rpc: return ProtocolToScript(rpc, path);
                case Protocol p: return ProtocolToScript(p, path);
                case Module m: return ModuleSpaceToScript(m, path);
                case Solution s: return ModuleSpaceToScript(s, path);
                case ModuleSpace ms: return ModuleSpaceToScript(ms, path);
                case Types.Enum e: return EnumToScript(e);
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
                case System.Collections.IDictionary dict:
                {
                    var arr = new ScriptArray();
                    foreach (System.Collections.DictionaryEntry entry in dict)
                        arr.Add(new ScriptObject { ["key"] = ToScript(entry.Key, path), ["value"] = ToScript(entry.Value, path) });
                    return arr;
                }
                case System.Collections.IEnumerable seq:
                {
                    var arr = new ScriptArray();
                    foreach (var item in seq) arr.Add(ToScript(item, path));
                    return arr;
                }
                default:
                    return obj.ToString();
            }
        }

        private static ScriptObject ModuleSpaceToScript(ModuleSpace ms, HashSet<object> path)
        {
            if (!path.Add(ms)) return null;
            var so = new ScriptObject
            {
                ["name"] = ms.Name,
                ["id"] = ms.Id,
                ["full_name"] = ms.Path(),
                ["enums"] = ToScript(ms.Enums, path),
                ["beans"] = ToScript(ms.Beans.Values, path),
                ["beankeys"] = ToScript(ms.BeanKeys.Values, path),
                ["protocols"] = ToScript(ms.Protocols.Values, path),
                ["solution"] = ToScript(ms.Solution, path),
            };
            path.Remove(ms);
            return so;
        }

        private static ScriptObject BeanToScript(Bean b, HashSet<object> path)
        {
            if (!path.Add(b)) return null;
            var so = new ScriptObject
            {
                ["name"] = b.Name,
                ["full_name"] = b.FullName,
                ["type_id"] = b.TypeId,
                ["variables"] = ToScript(b.Variables, path),
                ["enums"] = ToScript(b.Enums, path),
            };
            path.Remove(b);
            return so;
        }

        private static ScriptObject BeanKeyToScript(BeanKey k, HashSet<object> path)
        {
            if (!path.Add(k)) return null;
            var so = new ScriptObject
            {
                ["name"] = k.Name,
                ["full_name"] = k.FullName,
                ["type_id"] = k.TypeId,
                ["variables"] = ToScript(k.Variables, path),
                ["enums"] = ToScript(k.Enums, path),
            };
            path.Remove(k);
            return so;
        }

        private static ScriptObject VariableToScript(Variable v, HashSet<object> path)
        {
            if (!path.Add(v)) return null;
            var so = new ScriptObject
            {
                ["name"] = v.Name,
                ["id"] = v.Id,
                ["type"] = v.Type,
                ["variable_type"] = ToScript(v.VariableType, path),
                ["initial"] = v.Initial,
            };
            path.Remove(v);
            return so;
        }

        private static ScriptObject TypeToScript(Types.Type t, HashSet<object> path)
        {
            if (!path.Add(t)) return null;
            var so = new ScriptObject
            {
                ["name"] = t.Name,
                ["is_bean"] = t.IsBean,
                ["is_collection"] = t.IsCollection,
            };
            switch (t)
            {
                case TypeMap m:
                    so["key_type"] = ToScript(m.KeyType, path);
                    so["value_type"] = ToScript(m.ValueType, path);
                    break;
                case TypeDynamic d:
                    so["real_beans"] = ToScript(d.RealBeans, path);
                    break;
                case TypeCollection c:
                    so["value_type"] = ToScript(c.ValueType, path);
                    break;
            }
            path.Remove(t);
            return so;
        }

        private static ScriptObject ProtocolToScript(Protocol p, HashSet<object> path)
        {
            if (!path.Add(p)) return null;
            var so = new ScriptObject
            {
                ["name"] = p.Name,
                ["full_name"] = p.FullName,
                ["id"] = p.Id,
                ["type_id"] = p.TypeId,
                ["argument_type"] = ToScript(p.ArgumentType, path),
                ["space"] = ToScript(p.Space, path),
                ["enums"] = ToScript(p.Enums, path),
                ["result"] = false,
                ["result_type"] = null,
            };
            if (p is Rpc r)
            {
                so["result"] = true;
                so["result_type"] = ToScript(r.ResultType, path);
            }
            path.Remove(p);
            return so;
        }

        private static ScriptObject EnumToScript(Types.Enum e) => new ScriptObject
        {
            ["name"] = e.Name,
            ["value"] = e.Value,
        };

        public void Make()
        {
            HashSet<ModuleSpace> allRefModules = new HashSet<ModuleSpace>();
            foreach (Module mod in Project.AllOrderDefineModules)
                allRefModules.Add(mod);

            foreach (BeanKey beanKey in Project.AllBeanKeys.Values)
            {
                allRefModules.Add(beanKey.Space);
            }

            foreach (Bean bean in Project.AllBeans.Values)
            {
                allRefModules.Add(bean.Space);
            }

            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                allRefModules.Add(protocol.Space);
            }

            List<ModuleSpace> allRefModulesList = new List<ModuleSpace>();
            foreach (ModuleSpace m in allRefModules)
            {
                var beans = Project.AllBeans.Values.Intersect(m.Beans.Values);
                var beanKeys = Project.AllBeanKeys.Values.Intersect(m.BeanKeys.Values);
                var protocols = Project.AllProtocols.Values.Intersect(m.Protocols.Values);
                if (!beans.Any() && !beanKeys.Any() && !protocols.Any())
                {
                    continue;
                }

                allRefModulesList.Add(m);
            }

            string rootNameSpace = "";
            var schemaNamespace = rootNameSpace.Length > 0 ? $"{rootNameSpace}.msg.__msgmeta__" : "msg.__msgmeta__";
            var messageNamespace = rootNameSpace.Length > 0 ? $"{rootNameSpace}.msg" : "msg";
            string genDir = Project.GenDir; // "msg"
            string metaDir = Path.Combine(genDir, "__msgmeta__");
            string srcDir = Project.SrcDir; // "module"
            Program.AddGenDir(genDir);
            {
                string luaMetaTemplateString = GetTemplate("LuaMeta.scriban-txt");
                Template template = Template.Parse(luaMetaTemplateString);
                string luaMeta = Render(template, new Dictionary<string, object>
                {
                    ["modules"] = allRefModulesList,
                    ["beans"] = Project.AllBeans.Values,
                    ["beankeys"] = Project.AllBeanKeys.Values,
                    ["protocols"] = Project.AllProtocols.Values,
                    ["message_namespace"] = messageNamespace,
                    ["schema_namespace"] = schemaNamespace
                });

                string metaFileName = Path.Combine(genDir, "ZezeMeta.lua");
                using StreamWriter swMeta = Program.OpenStreamWriter(metaFileName);
                if (swMeta != null)
                {
                    swMeta.Write(luaMeta);
                    swMeta.Close();
                }
            }


            {
                string luaModuleTemplateString = GetTemplate("LuaModule.scriban-txt");
                Template moduleTemplate = Template.Parse(luaModuleTemplateString);
                foreach (var module in allRefModulesList)
                {
                    var beans = Project.AllBeans.Values.Intersect(module.Beans.Values).ToList();
                    var beanKeys = Project.AllBeanKeys.Values.Intersect(module.BeanKeys.Values).ToList();
                    var protocols = Project.AllProtocols.Values.Intersect(module.Protocols.Values).ToList();
                    if (!beans.Any() && !beanKeys.Any() && !protocols.Any())
                    {
                        continue;
                    }

                    string fullFileName = module.GetFullPath(genDir) + ".lua";
                    string fullDir = Path.GetDirectoryName(fullFileName);
                    string luaModule = Render(moduleTemplate, new Dictionary<string, object>
                    {
                        ["module"] = module,
                        ["beans"] = beans,
                        ["beankeys"] = beanKeys,
                        ["protocols"] = protocols,
                        ["message_namespace"] = messageNamespace,
                        ["schema_namespace"] = schemaNamespace,
                        ["lua_util_dir"] = Project.LuaUtilDir
                    });
                    if (fullDir != null) FileSystem.CreateDirectory(fullDir);
                    using var sw = Program.OpenStreamWriter(fullFileName);
                    if (sw != null)
                    {
                        sw.Write(luaModule);
                        sw.Close();
                    }
                }
            }

            {
                string luaModuleTemplateString = GetTemplate("LuaModuleMeta.scriban-txt");
                Template moduleTemplate = Template.Parse(luaModuleTemplateString);
                foreach (var module in allRefModulesList)
                {
                    var beans = Project.AllBeans.Values.Intersect(module.Beans.Values).ToList();
                    var beanKeys = Project.AllBeanKeys.Values.Intersect(module.BeanKeys.Values).ToList();
                    var protocols = Project.AllProtocols.Values.Intersect(module.Protocols.Values).ToList();
                    if (!beans.Any() && !beanKeys.Any() && !protocols.Any())
                    {
                        continue;
                    }

                    string fullFileName = module.GetFullPath(metaDir) + "Meta.lua";
                    string fullDir = Path.GetDirectoryName(fullFileName);
                    string luaModule = Render(moduleTemplate, new Dictionary<string, object>
                    {
                        ["module"] = module,
                        ["beans"] = beans,
                        ["beankeys"] = beanKeys,
                        ["protocols"] = protocols,
                        ["message_namespace"] = messageNamespace,
                        ["schema_namespace"] = schemaNamespace
                    });
                    if (fullDir != null) FileSystem.CreateDirectory(fullDir);
                    using var sw = Program.OpenStreamWriter(fullFileName);
                    if (sw != null)
                    {
                        sw.Write(luaModule);
                        sw.Close();
                    }
                }
            }

            {
                string luaRootTemplateString = GetTemplate("LuaRoot.scriban-txt");
                Template rootTemplate = Template.Parse(luaRootTemplateString);
                string luaRoot = Render(rootTemplate, new Dictionary<string, object>
                {
                    ["modules"] = allRefModulesList,
                    ["solution"] = Project.Solution,
                    ["message_namespace"] = messageNamespace,
                    ["schema_namespace"] = schemaNamespace
                });

                using StreamWriter sw = Program.OpenStreamWriter(Path.Combine(genDir, "message.lua"));
                if (sw != null)
                {
                    sw.Write(luaRoot);
                    sw.Close();
                }
            }

            {
                var solutionNames = allRefModulesList.Select(m => m.Solution.Name).ToHashSet();
                string luaInitTemplateText = GetTemplate("message_init.lua");
                Template luaInitTemplate = Template.Parse(luaInitTemplateText);
                string luaRoot = Render(luaInitTemplate, new Dictionary<string, object>
                {
                    ["solution_names"] = solutionNames,
                    ["message_namespace"] = messageNamespace,
                    ["schema_namespace"] = schemaNamespace,
                    ["lua_util_dir"] = Project.LuaUtilDir
                });

                using StreamWriter sw = Program.OpenStreamWriter(Path.Combine(genDir, "message_init.lua"));
                if (sw != null)
                {
                    sw.Write(luaRoot);
                    sw.Close();
                }
            }

            {
                string luaModuleTemplateString = GetTemplate("LuaModuleHandle.scriban-txt");
                Template moduleTemplate = Template.Parse(luaModuleTemplateString);
                FileChunkGen fileChunkGen = new FileChunkGen("--- [[ AUTO GENERATE START ]] ---",
                    "--- [[ AUTO GENERATE END ]] ---");
                foreach (ModuleSpace module in allRefModulesList)
                {
                    if (!Project.AllProtocols.Values.Intersect(module.Protocols.Values).Any())
                    {
                        continue;
                    }
                    var protocols = Project.AllProtocols.Values.Intersect(module.Protocols.Values)
                        .Where(p => 0 != (p.HandleFlags & ((Module)module).ReferenceService.HandleFlags)).ToList();

                    string fullDir = module.GetFullPath(srcDir);
                    string fullFileName = Path.Combine(fullDir, $"Module{FirstLetterToUpper(module.Name)}.lua");

                    if (!fileChunkGen.LoadFile(fullFileName))
                    {
                        string luaModule = Render(moduleTemplate, new Dictionary<string, object>
                        {
                            ["module"] = module,
                            ["protocols"] = protocols,
                            ["message_namespace"] = messageNamespace,
                            ["schema_namespace"] = schemaNamespace
                        });
                        FileSystem.CreateDirectory(fullDir);
                        using var sw = Program.OpenStreamWriter(fullFileName);
                        if (sw != null)
                        {
                            sw.Write(luaModule);
                            sw.Close();
                        }
                        continue;
                    }

                    if (fileChunkGen.Chunks.Count < 3)
                    {
                        continue;
                    }

                    var handlerChunk = fileChunkGen.Chunks[2];
                    var generatedHandlers = new HashSet<string>();


                    foreach (var line in handlerChunk.Lines)
                    {
                        if (line.StartsWith($"function {module.Name}.OnMsg_"))
                        {
                            var protoName = line.Substring($"function {module.Name}.OnMsg_".Length).Split("(")[0];
                            generatedHandlers.Add(protoName.Trim());
                        }
                    }

                    fileChunkGen.SaveFile(fullFileName, (writer, _) =>
                        {
                            writer.WriteLine($"function {module.Name}.RegisterHandlers()");
                            foreach (var protocol in protocols)
                            {
                                writer.WriteLine(
                                    $"    {messageNamespace}.{protocol.FullName}.Handle = {module.Name}.OnMsg_{protocol.Name}");
                            }

                            writer.WriteLine("end");
                        }, null, (writer, _) =>
                        {
                            foreach (var protocol in protocols)
                            {
                                if (generatedHandlers.Contains(protocol.Name))
                                    continue;
                                writer.WriteLine();
                                writer.WriteLine($"---@param p {messageNamespace}.{protocol.FullName}");
                                writer.WriteLine($"function {module.Name}.OnMsg_{protocol.Name}(p)");
                                writer.WriteLine("end");
                            }
                        }
                    );
                }
            }

            {
                string luaInitTemplateText = GetTemplate("ModuleRoot.scriban-txt");
                Template luaInitTemplate = Template.Parse(luaInitTemplateText);
                var modules = allRefModulesList.Where(module => Project.AllProtocols.Values.Intersect(module.Protocols.Values).Any())
                    .ToList();

                string luaRoot = Render(luaInitTemplate, new Dictionary<string, object>
                {
                    ["modules"] = modules,
                });

                var fullFileName = Path.Combine(srcDir, "module.lua");

                FileChunkGen fileChunkGen = new FileChunkGen("--- [[ AUTO GENERATE START ]] ---",
                    "--- [[ AUTO GENERATE END ]] ---");
                if (!fileChunkGen.LoadFile(fullFileName))
                {
                    using StreamWriter sw = Program.OpenStreamWriter(Path.Combine(srcDir, "module.lua"));
                    if (sw != null)
                    {
                        sw.WriteLine("local module = {}");
                        sw.WriteLine("");
                        sw.WriteLine(fileChunkGen.ChunkStartTag);
                        sw.Write(luaRoot);
                        sw.WriteLine(fileChunkGen.ChunkEndTag);
                        sw.WriteLine("");
                        sw.WriteLine("function module.Init()");
                        sw.WriteLine("    module.InternalInit()");
                        sw.WriteLine("end");
                        sw.WriteLine("");
                        sw.WriteLine("return module");
                    }
                }
                else
                {
                    fileChunkGen.SaveFile(fullFileName, (writer, _) => writer.Write(luaRoot));
                }
            }
        }
    }
}
