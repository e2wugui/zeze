using System;
using System.Collections.Generic;
using System.Text;

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
            }
        }

        private void PropertyGen(System.IO.StreamWriter sw)
        {
            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("    public " + m.Path("_", "Module") + " = new " + m.Path("_", "Module") + "(this);");
            }
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("    public " + m.Name + ": Zeze.Service = new Zeze.Service(\" + m.Name + \");");
            }
            sw.WriteLine();
        }

        private void ImportGen(System.IO.StreamWriter sw)
        {
            sw.WriteLine("import { Zeze } from \"zeze.js\"");
            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("import { " + m.Path("_", "Module")  + " } from \"" + m.Path("/", "Module.js") + "\"");
            }
        }

        private void StartGen(System.IO.StreamWriter sw)
        {
            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("        this." + m.Path("_", "Module") + ".Start(this);");
            }
        }

        private void StopGen(System.IO.StreamWriter sw)
        {
            foreach (Module m in project.AllModules)
            {
                sw.WriteLine("        this." + m.Path("_", "Module") + ".Stop(this);");
            }
        }

        public void Make()
        {
            Zeze.Util.FileChunkGen fcg = new Util.FileChunkGen();
            string fullDir = project.Solution.GetFullPath(genDir);
            string fullFileName = System.IO.Path.Combine(fullDir, "App.ts");
            if (fcg.LoadFile(fullFileName))
            {
                fcg.SaveFile(fullFileName, GenChunkByName);
                return;
            }
            // new file
            System.IO.Directory.CreateDirectory(fullDir);
            using System.IO.StreamWriter sw = new System.IO.StreamWriter(fullFileName, false, Encoding.UTF8);
            sw.WriteLine();
            sw.WriteLine(fcg.ChunkStartTag + " " + ChunkNameImportGen);
            ImportGen(sw);
            sw.WriteLine(fcg.ChunkEndTag + " " + ChunkNameImportGen);
            sw.WriteLine();
            sw.WriteLine("export class " + project.Solution.Name + "_App {");
            sw.WriteLine("    " + fcg.ChunkStartTag + " " + ChunkNamePropertyGen);
            PropertyGen(sw);
            sw.WriteLine("    " + fcg.ChunkEndTag + " " + ChunkNamePropertyGen);
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
