using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class FileSystem
    {
        public static bool IsDirectory(string path)
        {
            FileAttributes attr = File.GetAttributes(path);
            return (attr & FileAttributes.Directory) == FileAttributes.Directory;
        }

        public static bool IsFile(string path)
        {
            return !IsDirectory(path);
        }

        public static void CopyFileOrDirectory(string src, string dst, bool overwrite)
        {
            if (IsDirectory(src))
            {
                // 拷贝目录到目标目录下。
                if (false == IsDirectory(dst))
                    throw new Exception("Dest directory does not exist");
                dst = Path.Combine(dst, Path.GetFileName(src));
                Directory.CreateDirectory(dst);
                CopyDirectory(src, dst, true, overwrite);
            }
            else
            {

                // 拷贝文件到目标文件（Rename）或者目标目录下。
                if (Directory.Exists(dst))
                    dst = Path.Combine(dst, Path.GetFileName(src));
                File.Copy(src, dst, overwrite);
            }
        }

        public static void CopyDirectory(string sourceDirName, string destDirName, bool copySubDirs, bool overwrite)
        {
            DirectoryInfo dir = new DirectoryInfo(sourceDirName);
            if (!dir.Exists)
            {
                throw new DirectoryNotFoundException(
                    $"Source directory does not exist or could not be found: {sourceDirName}");
            }

            Directory.CreateDirectory(destDirName);
            foreach (FileInfo file in dir.GetFiles())
            {
                file.CopyTo(Path.Combine(destDirName, file.Name), overwrite);
            }

            if (copySubDirs)
            {
                foreach (DirectoryInfo subdir in dir.GetDirectories())
                {
                    CopyDirectory(subdir.FullName, Path.Combine(destDirName, subdir.Name), copySubDirs, overwrite);
                }
            }
        }
    }
}
