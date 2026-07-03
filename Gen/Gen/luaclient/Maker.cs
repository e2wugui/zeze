using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using Scriban;
using Scriban.Parsing;
using Scriban.Runtime;
using Zeze.Util;

namespace Zeze.Gen.luaClient
{
    public class Maker
    {
        Project Project { get; }

        // Scriban 模型构建器（两遍：建壳 + 填引用），引用语义共享，详见 ScriptModelBuilder。
        ScriptModelBuilder _modelBuilder;

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


        private string Render(Template template, ScriptObject model)
        {
            // 模板数据全部来自 ScriptModelBuilder 预构建的 ScriptObject/ScriptArray/标量，不再 ToScript 原始对象。
            TemplateContext context = template.LexerOptions.Lang == ScriptLang.Liquid ? new LiquidTemplateContext() : new TemplateContext();
            context.LoopLimit = 0;
            context.RecursiveLimit = 0;
            context.PushGlobal(model);
            return template.Render(context);
        }

        public void Make()
        {
            _modelBuilder = new ScriptModelBuilder(Project);
            _modelBuilder.Build();
            var model = _modelBuilder.Model;            // 镜像 Project 的顶层模型（模板数据来源）
            var refModules = _modelBuilder.RefModules;  // 含内容的原始 module（控制流：路径、HandleFlags、FileChunkGen）

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
                var so = new ScriptObject
                {
                    ["modules"] = model["modules"],
                    ["beans"] = model["beans"],
                    ["beankeys"] = model["beankeys"],
                    ["protocols"] = model["protocols"],
                    ["message_namespace"] = messageNamespace,
                    ["schema_namespace"] = schemaNamespace
                };
                string luaMeta = Render(template, so);

                string metaFileName = Path.Combine(genDir, "ZezeMeta.lua");
                using StreamWriter swMeta = Program.OpenStreamWriter(metaFileName);
                if (swMeta != null)
                {
                    swMeta.Write(luaMeta);
                    swMeta.Close();
                }
            }


            // LuaModule / LuaModuleMeta：按 module 逐个生成，仅模板名/目录/后缀/lua_util_dir 不同，提取公共逻辑
            void RenderPerModule(string templateName, string baseDir, string suffix, bool withLuaUtilDir)
            {
                Template template = Template.Parse(GetTemplate(templateName));
                foreach (var module in refModules)
                {
                    var mso = _modelBuilder.Get(module);
                    string fullFileName = module.GetFullPath(baseDir) + suffix;
                    string fullDir = Path.GetDirectoryName(fullFileName);
                    var so = new ScriptObject
                    {
                        ["module"] = mso,
                        ["beans"] = mso["beans"],
                        ["beankeys"] = mso["beankeys"],
                        ["protocols"] = mso["protocols"],
                        ["message_namespace"] = messageNamespace,
                        ["schema_namespace"] = schemaNamespace,
                    };
                    if (withLuaUtilDir) so["lua_util_dir"] = Project.LuaUtilDir;
                    string luaModule = Render(template, so);
                    if (fullDir != null) FileSystem.CreateDirectory(fullDir);
                    using var sw = Program.OpenStreamWriter(fullFileName);
                    if (sw != null)
                    {
                        sw.Write(luaModule);
                        sw.Close();
                    }
                }
            }

            RenderPerModule("LuaModule.scriban-txt", genDir, ".lua", withLuaUtilDir: true);
            RenderPerModule("LuaModuleMeta.scriban-txt", metaDir, "Meta.lua", withLuaUtilDir: false);

            {
                string luaRootTemplateString = GetTemplate("LuaRoot.scriban-txt");
                Template rootTemplate = Template.Parse(luaRootTemplateString);
                var so = new ScriptObject
                {
                    ["modules"] = model["modules"],
                    ["solution"] = model["solution"],
                    ["message_namespace"] = messageNamespace,
                    ["schema_namespace"] = schemaNamespace
                };
                string luaRoot = Render(rootTemplate, so);

                using StreamWriter sw = Program.OpenStreamWriter(Path.Combine(genDir, "message.lua"));
                if (sw != null)
                {
                    sw.Write(luaRoot);
                    sw.Close();
                }
            }

            {
                // 顺序保持与原实现一致：HashSet<string> 决定 solution_names 的遍历序
                var solutionNamesSet = new HashSet<string>();
                foreach (var m in refModules) solutionNamesSet.Add(m.Solution.Name);
                var solutionNames = new ScriptArray();
                foreach (var name in solutionNamesSet) solutionNames.Add(name);

                string luaInitTemplateText = GetTemplate("message_init.lua");
                Template luaInitTemplate = Template.Parse(luaInitTemplateText);
                var so = new ScriptObject
                {
                    ["solution_names"] = solutionNames,
                    ["message_namespace"] = messageNamespace,
                    ["schema_namespace"] = schemaNamespace,
                    ["lua_util_dir"] = Project.LuaUtilDir
                };
                string luaRoot = Render(luaInitTemplate, so);

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
                foreach (ModuleSpace module in refModules)
                {
                    var mso = _modelBuilder.Get(module);
                    if (((ScriptArray)mso["protocols"])!.Count == 0)
                    {
                        continue;
                    }
                    // HandleFlags 过滤（控制流，原始对象）：FileChunkGen 的 C# 拼接要用 protocol.Name/FullName
                    var moduleRef = (Module)module;
                    var protocols = Project.AllProtocols.Values.Intersect(module.Protocols.Values)
                        .Where(p => 0 != (p.HandleFlags & moduleRef.ReferenceService.HandleFlags)).ToList();

                    string fullDir = module.GetFullPath(srcDir);
                    string fullFileName = Path.Combine(fullDir, $"Module{FirstLetterToUpper(module.Name)}.lua");

                    if (!fileChunkGen.LoadFile(fullFileName))
                    {
                        var protoArr = new ScriptArray();
                        foreach (var p in protocols) protoArr.Add(_modelBuilder.Get(p));
                        var so = new ScriptObject
                        {
                            ["module"] = mso,
                            ["protocols"] = protoArr,
                            ["message_namespace"] = messageNamespace,
                            ["schema_namespace"] = schemaNamespace
                        };
                        string luaModule = Render(moduleTemplate, so);
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
                var modules = new ScriptArray();
                foreach (var m in refModules)
                {
                    var mso = _modelBuilder.Get(m);
                    if (((ScriptArray)mso["protocols"])!.Count > 0)
                        modules.Add(mso);
                }

                string luaRoot = Render(luaInitTemplate, new ScriptObject { ["modules"] = modules });

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
