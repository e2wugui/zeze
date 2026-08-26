package harness;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 吞吐基准：靠打印 M/s/耗时观察，不设断言，自包含与否都不进 gradle test 快速车道。
 * 由 gradle bench 任务（includeTags "bench"）执行；integrationTest 按标签排除。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("bench")
public @interface Bench {
}
