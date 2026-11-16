@echo off
:: 禁用Windows 11右键菜单"显示更多选项"
:: 运行此脚本后需要重启资源管理器或注销重新登录

echo 正在禁用"显示更多选项"...
reg.exe add "HKCU\Software\Classes\CLSID\{86ca1aa0-34aa-4e8b-a509-50c905bae2a2}\InprocServer32" /f /ve

echo.
echo 操作完成！
echo 请重启资源管理器或注销重新登录以使更改生效。
echo.
echo 如需恢复，请运行:
echo reg.exe delete "HKCU\Software\Classes\CLSID\{86ca1aa0-34aa-4e8b-a509-50c905bae2a2}" /f
echo.
pause