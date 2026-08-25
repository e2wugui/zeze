notnull-agent
=============

模拟 IDEA "Add runtime assertions for notnull-annotated methods" 的 javaagent，
用于在命令行/CI 回归 @NotNull 运行时断言（对项目包内方法：@NotNull 参数入口检查抛
IllegalArgumentException，@NotNull 返回值检查抛 IllegalStateException，文案与 IDEA 一致）。

使用（ZezeJavaTest 目录下，先启动 test/service & global.bat 和 service & global.another.bat）：

    java -javaagent:../test/notnull-agent/notnull-agent.jar -ea -Dlogname=ZezeJavaTest ^
        -cp .;lib/*;build/classes/java/main;../test/notnull-agent/notnull-agent.jar ^
        org.junit.runner.JUnitCore <测试类清单，见 test-classes.txt>

重新构建（依赖 ASM，未随仓库提交，从 maven 下载到 lib/ 后用 JDK21 编译）：

    lib/asm-9.7.1.jar      https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar
    lib/asm-tree-9.7.1.jar https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.7.1/asm-tree-9.7.1.jar

    javac -cp lib/asm-9.7.1.jar;lib/asm-tree-9.7.1.jar -d classes src/notnull/NotNullAgent.java
    :: 把 asm/asm-tree 的 class 解压进 classes/（排除 module-info.class）打成 fat jar：
    jar cfm notnull-agent.jar src/MANIFEST.MF -C classes .
