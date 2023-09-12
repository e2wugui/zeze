using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.HotDistribute;
using Zeze.Gen.java;
using Zeze.Gen.Types;
using Zeze.Serialize;

namespace Gen
{
    public class Program
    {
        public static void Main(string[] args)
        {
            string command = "gen";
            for (int i = 0; i < args.Length; ++i)
            {
                if (args[i].Equals("-c"))
                    command = args[++i];
            }
            switch (command)
            {
                case "gen":
                    Zeze.Gen.Program.Main(args);
                    break;

                case "genr":
                    Zeze.Util.RedirectGenMain.Main(args);
                    break;

                //case "ExportConf":
                //    ExportConf(args);
                //    break;
            }
        }
        /*
        private static void ExportConf(string[] args)
        {
            string zezeSrcDir = null;
            string copyDstDir = ".";
            for (int i = 0; i < args.Length; ++i)
            {
                if (args[i].Equals("-ZezeSrcDir"))
                    zezeSrcDir = args[++i];
                if (args[i].Equals("-CopyDstDir"))
                    copyDstDir = args[++i];
            }
            if (string.IsNullOrEmpty(zezeSrcDir))
            {
                Console.WriteLine("Usage -ZezeSrcDir path -CopyDstDir [.]");
                return;
            }

            Directory.CreateDirectory(Path.Combine(copyDstDir, "Zeze/Util/"));
            Directory.CreateDirectory(Path.Combine(copyDstDir, "Zeze/Net/"));
            Directory.CreateDirectory(Path.Combine(copyDstDir, "Zeze/Serialize/"));
            Zeze.Util.FileSystem.CopyFileOrDirectory(
                Path.Combine(zezeSrcDir, "Zeze/Util/Str.cs"),
                Path.Combine(copyDstDir, "Zeze/Util/Str.cs"),
                true);
            Zeze.Util.FileSystem.CopyFileOrDirectory(
                Path.Combine(zezeSrcDir, "Zeze/Util/ConfBean.cs"),
                Path.Combine(copyDstDir, "Zeze/Util/ConfBean.cs"),
                true);
            Zeze.Util.FileSystem.CopyFileOrDirectory(
                Path.Combine(zezeSrcDir, "Zeze/Util/FixedHash.cs"),
                Path.Combine(copyDstDir, "Zeze/Util/FixedHash.cs"),
                true);
            Zeze.Util.FileSystem.CopyFileOrDirectory(
                Path.Combine(zezeSrcDir, "Zeze/Net/Binary.cs"),
                Path.Combine(copyDstDir, "Zeze/Net/Binary.cs"),
                true);
            Zeze.Util.FileSystem.CopyFileOrDirectory(
                Path.Combine(zezeSrcDir, "Zeze/Serialize/Serializable.cs"),
                Path.Combine(copyDstDir, "Zeze/Serialize/Serializable.cs"),
                true);
            Zeze.Util.FileSystem.CopyFileOrDirectory(
                Path.Combine(zezeSrcDir, "Zeze/Serialize/SerializeHelper.cs"),
                Path.Combine(copyDstDir, "Zeze/Serialize/SerializeHelper.cs"),
                true);
            Zeze.Util.FileSystem.CopyFileOrDirectory(
                Path.Combine(zezeSrcDir, "Zeze/Serialize/ByteBuffer.cs"),
                Path.Combine(copyDstDir, "Zeze/Serialize/ByteBuffer.cs"),
                (srcFile, dstFile) =>
                {
                    var source = File.ReadAllText(srcFile.FullName, Encoding.UTF8);
                    source = source.Replace("MACRO_CONF_CS_PREDEFINE", "MACRO_CONF_CS");
                    File.WriteAllText(dstFile, source, Encoding.UTF8);
                });
        }
        */
    }
}
