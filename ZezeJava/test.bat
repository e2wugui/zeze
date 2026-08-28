@echo off

@rem :: 快速自包含测试（@Fast 标注的类，无外部依赖）
call gradlew.bat :ZezeJavaTest:test

@rem  :: 全量功能测试（自动在进程内启动 SM/GCM，不含fast和bench）
call gradlew.bat :ZezeJavaTest:integrationTest

@rem :: 吞吐基准（@Bench 标注的类）
call gradlew.bat :ZezeJavaTest:bench

pause
