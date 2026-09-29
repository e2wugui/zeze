package Zeze.Services;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * FND29 zoker-02 守卫：递归分发的目录列举失败（listFiles()==null：IO 错误/权限拒绝/
 * 目录被并发删除）必须使整个 distribute 失败，不得静默跳过。
 * 修复前：null→return，该子树整棵缺失于 uploaded 与集合清单，服务端清单校验只验
 * "清单内齐全"，被裁剪的集合照常成版切 current——回执成功而现役版本缺文件。
 * 修复：对齐同方法读失败路径（FileInputStream 抛错→closeFile 压制+重抛）抛 IOException。
 * 注入形态：File 桩 listFiles()==null（真实 IO 故障无法跨平台确定性注入：Windows ACL/
 * 路径长度/竞态皆机器相关），反射直调私有递归重载（套件既有 setAccessible 惯例）；
 * 失败先于一切 RPC（null 即抛，openFile 不会发生），直构 ZokerAgent（不起网络）即可测。
 * 修复前红点：静默正常返回（uploaded 空）。
 */
@Fast
public class TestDistributeListFilesFailPropagates {

	/** 桩目录：列举恒失败（listFiles()==null），isDirectory 维持目录形态。 */
	private static final class UnlistableDir extends File {
		private static final long serialVersionUID = 1L;

		UnlistableDir(File dir) {
			super(dir.getPath());
		}

		@Override
		public boolean isDirectory() {
			return true;
		}

		@Override
		public File[] listFiles() {
			return null;
		}
	}

	/** 核心红点：子目录列举失败必须上抛 IOException（distribute 整体失败、清单不提交）。 */
	@Test
	public void testListFilesNullFailsDistribute(@TempDir Path tempDir) throws Exception {
		var agent = new ZokerAgent(new Config()); // 直构（不起网络）：失败先于任何 RPC
		var method = ZokerAgent.class.getDeclaredMethod("distributeService",
				String.class, Path.class, File.class, List.class);
		method.setAccessible(true);
		var uploaded = new ArrayList<String>();
		try {
			method.invoke(agent, "zk1", tempDir, new UnlistableDir(tempDir.toFile()), uploaded);
			fail("listFiles()==null 必须使 distribute 失败（修复前静默 return，子树整棵缺失于清单而回执成功）");
		} catch (InvocationTargetException e) {
			assertTrue(e.getCause() instanceof IOException,
					"列举失败须对齐读失败传播（IOException 上抛），实际: " + e.getCause());
		}
		assertTrue(uploaded.isEmpty(), "失败路径不得留下部分上传记账");
	}
}
