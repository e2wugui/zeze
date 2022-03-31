using System;
using System.IO;

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
            CopyFileOrDirectory(src, dst,
                (srcFile, dstFileName) =>
                {
                    srcFile.CopyTo(dstFileName, overwrite);
                });
        }

        public static void CopyFileOrDirectory(string src, string dst, Action<FileInfo, string> copyAction)
        {
            if (IsDirectory(src))
            {
                // 拷贝目录到目标目录下。
                if (false == IsDirectory(dst))
                    throw new Exception("Dest directory does not exist");
                dst = Path.Combine(dst, Path.GetFileName(src));
                CreateDirectory(dst);
                CopyDirectory(src, dst, true, copyAction);
            }
            else
            {

                // 拷贝文件到目标文件（Rename）或者目标目录下。
                if (Directory.Exists(dst))
                    dst = Path.Combine(dst, Path.GetFileName(src));
                copyAction(new FileInfo(src), dst);
            }
        }

        public static void CopyDirectory(string sourceDirName, string destDirName, bool copySubDirs,
            Action<FileInfo, string> copyAction)
        {
            DirectoryInfo dir = new DirectoryInfo(sourceDirName);
            if (!dir.Exists)
            {
                throw new DirectoryNotFoundException(
                    $"Source directory does not exist or could not be found: {sourceDirName}");
            }

            CreateDirectory(destDirName);
            foreach (FileInfo file in dir.GetFiles())
            {
                copyAction(file, Path.Combine(destDirName, file.Name));
            }

            if (copySubDirs)
            {
                foreach (DirectoryInfo subdir in dir.GetDirectories())
                {
                    CopyDirectory(subdir.FullName, Path.Combine(destDirName, subdir.Name), copySubDirs, copyAction);
                }
            }
        }

        public static void CopyDirectory(string sourceDirName, string destDirName, bool copySubDirs, bool overwrite)
        {
            CopyDirectory(sourceDirName, destDirName, copySubDirs,
                (srcFile, dstFileName) =>
                {
                    srcFile.CopyTo(dstFileName, overwrite);
                });
        }

        public static void CreateDirectory(string path)
        {
            Directory.CreateDirectory(path);
            for (int i = 1; !Directory.Exists(path); i++)
            {
                System.Threading.Thread.Sleep(100);
                if (i % 10 == 0)
                    Directory.CreateDirectory(path);
            }
        }

        public static void DeleteDirectory(string path)
        {
            int times = 0;
            while (Directory.Exists(path))
            {
                Directory.Delete(path, true);
                if (times > 10)
                    break;
                if (times > 0)
                    System.Threading.Thread.Sleep(100);
                ++times;
            }
        }
    }
}
