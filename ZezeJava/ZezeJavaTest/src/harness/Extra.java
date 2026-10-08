package harness;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 不常用功能族（zoker/history/dbh2/mq/rocketmq/log4jquery）的附加车道标记：
 * 移出默认 test 快速车道与 integrationTest 全量车道，由 gradle extraTest 任务
 * （includeTags "extra"）按需执行，不进 test.bat 默认链。族边界=包边界+Services根
 * Log4jQuery 散类；同族测试改环境隔离约定时须整族评审（见 AGENTS.md 车道章节）。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("extra")
public @interface Extra {
}
