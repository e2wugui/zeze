package Zeze.Util;

import Zeze.*;
import java.io.*;
import java.nio.file.*;

public class FileSystem {
	public static boolean IsDirectory(String path) {
		FileAttributes attr = File.GetAttributes(path);
		return (attr.getValue() & FileAttributes.Directory.getValue()) == FileAttributes.Directory.getValue();
	}

	public static boolean IsFile(String path) {
		return !IsDirectory(path);
	}

	public static void CopyFileOrDirectory(String src, String dst, boolean overwrite) {
		CopyFileOrDirectory(src, dst, (srcFile, dstFileName) -> {
					srcFile.CopyTo(dstFileName, overwrite);
		});
	}

	public static void CopyFileOrDirectory(String src, String dst, tangible.Action2Param<File, String> copyAction) {
		if (IsDirectory(src)) {
			// 拷贝目录到目标目录下。
			if (false == IsDirectory(dst)) {
				throw new RuntimeException("Dest directory does not exist");
			}
			dst = Paths.get(dst).resolve((new File(src)).getName()).toString();
			(new File(dst)).mkdirs();
			CopyDirectory(src, dst, true, copyAction);
		}
		else {

			// 拷贝文件到目标文件（Rename）或者目标目录下。
			if ((new File(dst)).isDirectory()) {
				dst = Paths.get(dst).resolve((new File(src)).getName()).toString();
			}
			copyAction.invoke(new File(src), dst);
		}
	}

	public static void CopyDirectory(String sourceDirName, String destDirName, boolean copySubDirs, tangible.Action2Param<File, String> copyAction) {
		File dir = new File(sourceDirName);
		if (!dir.exists()) {
			throw new DirectoryNotFoundException(String.format("Source directory does not exist or could not be found: %1$s", sourceDirName));
		}

		(new File(destDirName)).mkdirs();
		for (File file : dir.GetFiles()) {
			copyAction.invoke(file, Paths.get(destDirName).resolve(file.getName()).toString());
		}

		if (copySubDirs) {
			for (File subdir : dir.GetDirectories()) {
				CopyDirectory(subdir.getPath(), Paths.get(destDirName).resolve(subdir.getName()).toString(), copySubDirs, copyAction);
			}
		}
	}

	public static void CopyDirectory(String sourceDirName, String destDirName, boolean copySubDirs, boolean overwrite) {
		CopyDirectory(sourceDirName, destDirName, copySubDirs, (srcFile, dstFileName) -> {
					srcFile.CopyTo(dstFileName, overwrite);
		});
	}
}