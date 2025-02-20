# Log

本章介绍Zeze日志的配置选项，定制方法。

## Task 日志
所有经Task启动的任务在执行失败的时候会记录日志。记录日志使用Task.ILogAction接口。
这个接口实现保存在全局变量Task.logAction中。
* 设置变量Task.logAction为null将禁止日志记录。
* 可以通过自己实现ILogAction并设置到变量Task.logAction中，就可以定制自己的日志。
* 变量初始值和接口定义如下：
```
public static ILogAction logAction = Task::DefaultLogAction;

@FunctionalInterface
public interface ILogAction {
    void run(@Nullable Throwable ex, long result, @Nullable Protocol<?> p, @NotNull String actionName);
}
```

## Procedure 日志
所有的存储过程执行失败的日志都会自动记录。记录日志使用Procedure.ILogAction接口。
这个接口实现保存在全局变量Procedure.logAction中。
* 设置变量Procedure.logAction为null将禁止日志记录。
* 可以通过自己实现ILogAction并设置到变量Procedure.logAction中，就可以定制自己的日志。
* 变量初始值和接口定义如下：
```
public static @Nullable ILogAction logAction = Procedure::defaultLogAction;

@FunctionalInterface
public interface ILogAction {
    void run(@Nullable Throwable ex, long result, @NotNull Procedure p, @NotNull String message);
}
```

## Protocol 日志
所有经由Net收发的协议根据选项输出到日志文件中。可以通过jvm参数进行配置。
*  -DprotocolLog=info 设置协议日志的级别。

## log4j2.xml 默认配置和选项
Zeze发布的时候，自带一个默认的log4j2.xml配置文件。可以通过jvm参数进行配置。
* -Dlogname=gs 这是日志文件的名字。
* -Dloglevel=INFO 设置日志级别。
* -Dlogconsole=Null 不输出日志到stdout, 一般用于正式部署服务器时。
* -Dlog4j.configurationFile=path/log4j2.xml 使用自定义的log4j配置。