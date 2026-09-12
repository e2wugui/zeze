package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.Util.PersistentAtomicLong;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-18：预算判断用加法形式 current+count>allocatedEnd，current 逼近 Long.MAX 时
 * 溢出为负跳过 allocate 分支，CAS 落地返回负数 id。纪元回绕窗口由水位文件预置到
 * MAX-1 确定性构造（构造器自然加载，无需反射）；改减法形式后经 reset 哨兵正常换纪元。
 */
@Fast
public class TestPersistentAtomicLongWrap {

	@Test
	public void testWrapPointNeverNegative() throws Exception {
		// 预置水位 MAX-1=9223372036854775806：currentId=allocatedEnd=MAX-1
		var name = "UnitTest.FND4_18.WrapTest";
		var fileName = name + ".zeze.pal";
		var file = Path.of(fileName);
		Files.deleteIfExists(file);
		try {
			Files.writeString(file, "9223372036854775806", StandardCharsets.UTF_8);
			var pal = PersistentAtomicLong.getOrAdd(name);

			// 旧实现：current+count = (MAX-1)+2 溢出为负 → 负数 > allocatedEnd 不成立 →
			// 跳过 allocate → CAS 到 (MAX+1)=MIN+1 返回负数 id
			var id = pal.next(2);
			Assertions.assertTrue(id >= 0, "回绕点不得发放负数id，got " + id);

			// 换纪元后继续发号正常且非负
			var id2 = pal.next();
			Assertions.assertTrue(id2 >= 0, "换纪元后发号必须非负，got " + id2);
			Assertions.assertTrue(id2 > 0 && id2 < 10_000, "新纪元应从小值重新开始，got " + id2);
		} finally {
			closePalHandle(name, fileName); // 静态缓存持有打开句柄，Windows下不关无法删除
			Files.deleteIfExists(file);
		}
	}

	@SuppressWarnings("unchecked")
	private static void closePalHandle(String name, String fileName) throws Exception {
		var allocFilesField = PersistentAtomicLong.class.getDeclaredField("allocFiles");
		allocFilesField.setAccessible(true);
		var allocFiles = (java.util.concurrent.ConcurrentHashMap<String, PersistentAtomicLong.FileWithLock>)
				allocFilesField.get(null);
		var fs = allocFiles.remove(fileName);
		if (fs != null)
			fs.close();
		var palsField = PersistentAtomicLong.class.getDeclaredField("pals");
		palsField.setAccessible(true);
		((java.util.concurrent.ConcurrentHashMap<String, PersistentAtomicLong>)palsField.get(null)).remove(name);
	}
}
