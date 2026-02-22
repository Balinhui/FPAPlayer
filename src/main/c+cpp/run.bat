@REM 使用MSVC编译，如果使用gcc，clang编译请勿运行此脚本
@echo off
chcp 65001
@REM 这里需要使用自己VS的x64 Native Tools Commend Prompt路径或者在这命令行中运行脚本
call "D:\User\Apps\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"

echo ======================================================================
echo 编译C++ ~
echo 编译器: cl.exe
echo ======================================================================

@REM 这些是源文件
set IN_FILE_CHOOSER=FileChooser.c
set IN_MESSAGER=Messager.cpp
set IN_WINDOWS_TASKBAR=WindowsTaskbar.cpp

@REM 这是JDK的头文件
set JNI_PATH=%JAVA_HOME%\include

@REM 这些是输出文件
set OUT_FILE_CHOOSER=file_chooser.dll
set OUT_MESSAGER=message.dll
set OUT_WINDOWS_TASKBAR=windows_taskbar.dll

@REM 输出目录
set DEST_DIR=..\..\..\

@REM cl.exe的编译选项
set OPTIONS=/LD /nologo /EHsc



@REM 检查文件是否存在

echo -------------------------检测所需文件是否存在-------------------------
if not exist "%DEST_DIR%" (
    echo 不是哥们，你输出目录呢？自己创建去吧！
    goto :end
)
echo 输出目录：存在

if not exist "%JNI_PATH%" (
    echo Java虚拟机的头文件都不见了, 那还编个啥?
    goto :end
)
echo Java虚拟机的头文件: 存在

if not exist "%IN_FILE_CHOOSER%" (
    echo 源文件 %IN_FILE_CHOOSER% 都没有，鬼！
    goto :end
)
echo 源文件 %IN_FILE_CHOOSER%：存在

if not exist "%IN_MESSAGER%" (
    echo 源文件 %IN_MESSAGER% 都没有，鬼！
    goto :end
)
echo 源文件 %IN_MESSAGER%：存在

if not exist "%IN_WINDOWS_TASKBAR%" (
    echo 源文件 %IN_WINDOWS_TASKBAR% 都没有，鬼！
    goto :end
)
echo 源文件 %IN_WINDOWS_TASKBAR%：存在

echo -------------------------------检测完成-------------------------------



@REM 编译

echo -------------------------------开始编译-------------------------------

echo 正在编译 %IN_FILE_CHOOSER% ...
cl.exe %OPTIONS% /Fe%OUT_FILE_CHOOSER% %IN_FILE_CHOOSER% /I%JNI_PATH% /I%JNI_PATH%\win32
if errorlevel 1 (
    echo 编译失败，这写的啥？都别编了
    goto :end
) else (
    echo 编译成功...
)

echo 正在编译 %IN_MESSAGER% ...
cl.exe %OPTIONS% /Fe%OUT_MESSAGER% %IN_MESSAGER% /I%JNI_PATH% /I%JNI_PATH%\win32
if errorlevel 1 (
    echo 编译失败，这写的啥？都别编了
    goto :end
) else (
    echo 编译成功...
)

echo 正在编译 %IN_WINDOWS_TASKBAR% ...
cl.exe %OPTIONS% /Fe%OUT_WINDOWS_TASKBAR% %IN_WINDOWS_TASKBAR% /I%JNI_PATH% /I%JNI_PATH%\win32
if errorlevel 1 (
    echo 编译失败，这写的啥？都别编了
    goto :end
) else (
    echo 编译成功...
)

echo -------------------------------编译完成-------------------------------



@REM 移动

echo -------------------------------移动文件-------------------------------

echo 正在移动 %OUT_FILE_CHOOSER% 到 %DEST_DIR%
move /y %OUT_FILE_CHOOSER% "%DEST_DIR%\" >nul
if errorlevel 1 (
    echo 移动文件失败！
    goto :error
) else (
    echo 移动文件成功！
)

echo 正在移动 %OUT_MESSAGER% 到 %DEST_DIR%
move /y %OUT_MESSAGER% "%DEST_DIR%\" >nul
if errorlevel 1 (
    echo 移动文件失败！
    goto :error
) else (
    echo 移动文件成功！
)

echo 正在移动 %OUT_WINDOWS_TASKBAR% 到 %DEST_DIR%
move /y %OUT_WINDOWS_TASKBAR% "%DEST_DIR%\" >nul
if errorlevel 1 (
    echo 移动文件失败！
    goto :error
) else (
    echo 移动文件成功！
)

echo -------------------------------移动完成-------------------------------



@REM 清理

echo -------------------------------清理文件-------------------------------

echo 正在清理 (.obj) 文件...
for %%f in (*.obj) do (
    del /q "%%f" 2>nul
    if exist "%%f" (
        echo "无法删除: %%f, 请自己清理"
    ) else (
        echo "已删除: %%f"
    )
)

echo 正在清理 (.lib) 文件...
for %%f in (*.lib) do (
    del /q "%%f" 2>nul
    if exist "%%f" (
        echo "无法删除: %%f, 请自己清理"
    ) else (
        echo "已删除: %%f"
    )
)

echo 正在清理 (.exp) 文件...
for %%f in (*.exp) do (
    del /q "%%f" 2>nul
    if exist "%%f" (
        echo "无法删除: %%f, 请自己清理"
    ) else (
        echo "已删除: %%f"
    )
)

echo -------------------------------清理完成-------------------------------

:end
pause
