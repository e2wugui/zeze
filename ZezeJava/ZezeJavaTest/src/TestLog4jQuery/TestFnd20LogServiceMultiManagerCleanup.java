package TestLog4jQuery;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static harness.DirCleanup.deleteBestEffort;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Services.LogService;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * 案外#3回归：LogService构造循环建多个Log4jFileManager，中途失败不回收前面已成功者的
 * detector线程与索引定时器（GD-C07只修了单manager构造内回收）——嵌入宿主进程泄漏，
 * standalone main随进程退出无害。修复后循环提取为buildLogManagers：失败时stop+clear已入表者。
 * 观察：构造失败后在valid目录创建active文件——泄漏的watch线程会处理CREATE并创建索引文件；
 * 已回收的线程不产生磁盘副作用（TestFnd19ManagerConstructFailCleanup同款观察形态）。
 */
@Fast
public class TestFnd20LogServiceMultiManagerCleanup {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testMidwayFailureStopsPriorManagers() throws Exception {
		var dirValid = Files.createTempDirectory("fnd20-multi-valid");
		var dirPoison = Files.createTempDirectory("fnd20-multi-poison");
		// ConcurrentHashMap无插入序：运行时探测迭代序，把毒化conf排在末位，
		// 保证至少一个manager先成功后失败（毒化形态：rotate名.index是目录→LogIndex构造必抛）。
		var confs = parseConf("""
				<LogServiceConf>
					<LogConf LogActive="a.log"/>
					<LogConf LogActive="b.log"/>
				</LogServiceConf>
				""");
		var order = new ArrayList<LogServiceConf.LogConf>(confs.getLogConfs().values());
		var validConf = order.get(0);
		var poisonConf = order.get(1);
		validConf.logDir = dirValid.toString();
		poisonConf.logDir = dirPoison.toString();
		Files.write(dirPoison.resolve(poisonConf.logActive.replace(".log", ".2026-01-01.log")),
				new byte[0]);
		Files.createDirectory(dirPoison.resolve(
				poisonConf.logActive.replace(".log", ".2026-01-01.log") + ".index"));

		var logManagers = new ConcurrentHashMap<String, Log4jFileManager>();
		// 修复前：循环内构造，中途失败直接抛出，先成功者滞留map（detector线程与定时器泄漏）。
		//（InvocationTargetException：method.invoke包装构造异常；修复前方法不存在，查找即抛
		// NoSuchMethodException使本用例红。）
		assertThrows(InvocationTargetException.class, () -> invokeBuildLogManagers(confs, logManagers));
		assertTrue(logManagers.isEmpty(), "中途失败应stop并清空已成功的manager（修复前先成功者滞留）");

		// detector已死的观察：在valid目录创建active文件，泄漏的watch线程会处理CREATE并创建索引。
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		AtomicFileWriter.replace(dirValid.resolve(validConf.logActive),
				(fmt.format(LocalDateTime.now()) + " hello\n").getBytes());
		var activeIndex = dirValid.resolve(validConf.logActive + ".index");
		var deadline = System.currentTimeMillis() + 1_000;
		while (Files.notExists(activeIndex) && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		assertTrue(Files.notExists(activeIndex),
				"构造失败后先成功manager的detector线程应已join，不得再处理文件创建事件");

		deleteBestEffort(dirValid);
		deleteBestEffort(dirPoison);
	}

	private static LogServiceConf parseConf(String xml) throws Exception {
		var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		var conf = new LogServiceConf();
		conf.parse(doc.getDocumentElement());
		return conf;
	}

	private static void invokeBuildLogManagers(LogServiceConf confs,
											   ConcurrentHashMap<String, Log4jFileManager> logManagers) throws Exception {
		Method method = LogService.class.getDeclaredMethod("buildLogManagers",
				LogServiceConf.class, ConcurrentHashMap.class);
		method.setAccessible(true);
		method.invoke(null, confs, logManagers);
	}
}
