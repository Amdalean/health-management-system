@echo off
chcp 65001 >nul
setlocal

:: ============================================================
::  HMS-FloatWindow Android 插件 — Gradle JAR 构建脚本
::  输出: android/HMS-FloatWindow.jar
::  add by Qwen 2026-06-03
:: ============================================================

set "PLUGIN_DIR=%~dp0"
set "ANDROID_DIR=%PLUGIN_DIR%android"
set "GRADLEW=%ANDROID_DIR%\gradlew.bat"

:: ---------- 步骤 1: 检查 Java ----------
echo [1/3] 检查 Java 环境...

if defined JAVA_HOME (
    echo   使用 JAVA_HOME: %JAVA_HOME%
) else if exist "E:\tool\jdks\jdk22\bin\java.exe" (
    set "JAVA_HOME=E:\tool\jdks\jdk22"
    echo   使用 JDK: E:\tool\jdks\jdk22
) else (
    where java >nul 2>&1
    if errorlevel 1 (
        echo   错误: 找不到 Java，请设置 JAVA_HOME
        pause
        exit /b 1
    )
    echo   使用 PATH 中的 Java
)

:: ---------- 步骤 2: Gradle 构建 JAR ----------
echo [2/3] Gradle 构建 JAR...

cd /d "%ANDROID_DIR%"

if exist "build" rmdir /s /q "build"

call "%GRADLEW%" jarRelease --no-daemon
if errorlevel 1 (
    echo.
    echo ============================================================
    echo  构建失败！常见解决方案：
    echo  1. 确认 local.properties 中 sdk.dir 路径正确
    echo  2. 确认 Android SDK 已安装 android-35 平台和 build-tools
    echo  3. 确认 android/src/ 目录下有源码文件
    echo ============================================================
    pause
    exit /b 1
)

:: ---------- 步骤 3: 验证 ----------
echo [3/3] 验证 JAR...

if exist "HMS-FloatWindow.jar" (
    for %%A in ("HMS-FloatWindow.jar") do echo   JAR 大小: %%~zA 字节
    echo.
    echo ============================================================
    echo   构建完成！JAR 文件: android\HMS-FloatWindow.jar
    echo.
    echo   配置说明（已自动设置）：
    echo   - package.json: integrateType = "jar"
    echo   - AndroidManifest.xml: 权限和组件声明
    echo   - lib/uniapp-stubs.jar: 编译期依赖，不进入最终包
    echo.
    echo   在 HBuilder X 中重新制作自定义基座即可
    echo ============================================================
) else (
    echo   错误: 找不到生成的 JAR 文件
    pause
    exit /b 1
)

endlocal
pause
