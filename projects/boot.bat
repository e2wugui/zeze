@echo off
setlocal

if "%~1"=="" (
    echo 错误：缺少必需参数SolutionName！
    echo 用法：%0 SolutionName
    pause
    exit /b 1
)

if exist "%~1" (
    echo 文件或文件夹存在：%~1
    pause
    exit /b 1
)

set "ProjectsDir=%~dp0"
set "BootDir=%ProjectsDir%zezeboot\"
set "GenDir=%ProjectsDir%..\publish\"

xcopy %BootDir% %~1 /E /I /H /Y
xcopy %GenDir% %~1\gen /E /I /H /Y

call "%~1\tool\change_solution_name.bat" "%~1"
