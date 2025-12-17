using Zeze.Util;

namespace Zeze.Gen.ts
{
    public class App
    {
        Project project;
        string genDir;

        public App(Project project, string genDir)
        {
            this.project = project;
            this.genDir = genDir;
        }

        private const string ChunkNamePropertyGen = "PROPERTY GEN";
        private const string ChunkNamePropertyInitGen = "PROPERTY INIT GEN";
        private const string ChunkNameImportGen = "IMPORT GEN";
        private const string ChunkNameStartGen = "START MODULE GEN";
        private const string ChunkNameStopGen = "STOP MODULE GEN";
        private void GenChunkByName(System.IO.StreamWriter writer, Zeze.Util.FileChunkGen.Chunk chunk)
        {
            switch (chunk.Name)
            {
                case ChunkNamePropertyGen:
                    PropertyGen(writer);
                    break;
                case ChunkNameImportGen:
                    ImportGen(writer);
                    break;
                case ChunkNameStartGen:
                    StartGen(writer);
                    break;
                case ChunkNameStopGen:
                    StopGen(writer);
                    break;
                case ChunkNamePropertyInitGen:
                    PropertyInitInConstructorGen(writer);
                    break;
            }
        }

        private void PropertyGen(System.IO.StreamWriter sw)
        {
            foreach (Module m in project.AllOrderDefineModules)
            {
                sw.WriteLine("    public module" + Program.Upper1(m.Name) + ": Module" + Program.Upper1(m.Name) + ";");
            }
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("    public " + m.Name + ": Zeze.Service;");
            }
        }

        private void PropertyInitInConstructorGen(System.IO.StreamWriter sw)
        {
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("        this." + m.Name + " = new Zeze.Service(\"" + m.Name + "\");");
            }
            foreach (Module m in project.AllOrderDefineModules)
            {
                sw.WriteLine("        this.module" + Program.Upper1(m.Name) + " = new Module" + Program.Upper1(m.Name) + "(this);");
            }
        }

        private void ImportGen(System.IO.StreamWriter sw)
        {
            sw.WriteLine("import { Zeze } from '../Zeze/zeze';");
            foreach (Module m in project.AllOrderDefineModules)
            {
                var clsName = "Module" + Program.Upper1(m.Name);
                var path = m.Path("/", clsName);
                if (m.Solution.Name == project.Solution.Name)
                    path = "./" + path[(path.IndexOf('/') + 1)..];
                else
                    path = "../" + path;
                sw.WriteLine("import " + clsName  + " from '" + path + "';");
            }
        }

        private void StartGen(System.IO.StreamWriter sw)
        {
            foreach (var m in project.ModuleStartOrder)
            {
                sw.WriteLine("        this.module" + Program.Upper1(m.Name) + ".Start(this);");
            }
            foreach (Module m in project.AllOrderDefineModules)
            {
                if (project.ModuleStartOrder.Contains(m))
                    continue;
                sw.WriteLine("        this.module" + Program.Upper1(m.Name) + ".Start(this);");
            }
        }

        private void StopGen(System.IO.StreamWriter sw)
        {
            foreach (Module m in project.AllOrderDefineModules)
            {
                sw.WriteLine("        this.module" + Program.Upper1(m.Name) + ".Stop(this);");
            }
        }

        public void Make()
        {
            FileChunkGen fcg = new FileChunkGen();
            string fullDir = project.Solution.GetFullPath(genDir);
            string fullFileName = System.IO.Path.Combine(fullDir, "App.ts");
            if (fcg.LoadFile(fullFileName))
            {
                fcg.SaveFile(fullFileName, GenChunkByName);
                return;
            }
            // new file
            FileSystem.CreateDirectory(fullDir);
            using System.IO.StreamWriter sw = Program.OpenStreamWriter(fullFileName);
            if (sw == null)
                return;

            sw.WriteLine("/* eslint-disable import/no-cycle, lines-between-class-members, prettier/prettier */");
            sw.WriteLine(fcg.ChunkStartTag + " " + ChunkNameImportGen);
            ImportGen(sw);
            sw.WriteLine(fcg.ChunkEndTag + " " + ChunkNameImportGen);
            sw.WriteLine();
            sw.WriteLine("export default class App {");
            sw.WriteLine("    " + fcg.ChunkStartTag + " " + ChunkNamePropertyGen);
            PropertyGen(sw);
            sw.WriteLine("    " + fcg.ChunkEndTag + " " + ChunkNamePropertyGen);
            sw.WriteLine();
            sw.WriteLine("    public constructor() {");
            sw.WriteLine("        " + fcg.ChunkStartTag + " " + ChunkNamePropertyInitGen);
            PropertyInitInConstructorGen(sw);
            sw.WriteLine("        " + fcg.ChunkEndTag + " " + ChunkNamePropertyInitGen);
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public Start(): void {");
            sw.WriteLine("        " + fcg.ChunkStartTag + " " + ChunkNameStartGen);
            StartGen(sw);
            sw.WriteLine("        " + fcg.ChunkEndTag + " " + ChunkNameStartGen);
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public Stop(): void {");
            sw.WriteLine("        " + fcg.ChunkStartTag + " " + ChunkNameStopGen);
            StopGen(sw);
            sw.WriteLine("        " + fcg.ChunkEndTag + " " + ChunkNameStopGen);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}
