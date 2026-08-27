$env:JAVA_HOME=$env:JAVA_PATH
$env:PATH="$env:PATH;$env:JAVA_HOME/bin:$env:JAVA_HOME/include;$env:JAVA_HOME/include/win32"
$env:PATH="$env:PATH;$env:OpenCLPath"

# 定位 Visual Studio 并初始化 MSVC 编译环境。
# 修复 GitHub Actions windows-latest runner 上 cmake 报 "could not find any instance of Visual Studio" 的问题。
$vswhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
$vsPath = ""
if (Test-Path $vswhere) {
    $vsPath = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
}
if (-not $vsPath) {
    # 兜底：常见 VS 安装路径
    $candidates = @(
        "C:\Program Files\Microsoft Visual Studio\2022\Enterprise",
        "C:\Program Files\Microsoft Visual Studio\2022\Professional",
        "C:\Program Files\Microsoft Visual Studio\2022\Community",
        "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools",
        "C:\Program Files\Microsoft Visual Studio\2022\BuildTools"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { $vsPath = $c; break }
    }
}
if (-not $vsPath) {
    throw "Visual Studio 2022 (VC tools) not found. Please install Visual Studio 2022 with C++ workload."
}
Write-Host "Using Visual Studio at: $vsPath"
$vcvarsall = Join-Path $vsPath "VC\Auxiliary\Build\vcvarsall.bat"
if (-not (Test-Path $vcvarsall)) {
    throw "vcvarsall.bat not found at $vcvarsall"
}
function Invoke-VsEnv([string]$arch) {
    # 在子进程中执行 vcvarsall 并捕获环境变量，然后应用到当前进程
    $tmp = [System.IO.Path]::GetTempFileName()
    cmd /c "`"$vcvarsall`" $arch >nul 2>&1 && set" | ForEach-Object {
        if ($_ -match '^([^=]+)=(.*)$') {
            Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
        }
    }
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
}

Remove-Item .\out\ -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path .\out\ -Force

$env:OpenCL_LIBRARY=$env:OpenCL86
Remove-Item .\work_dir\ -Recurse -Force -ErrorAction SilentlyContinue
Invoke-VsEnv x86
cmake . -B .\work_dir\ -G "Visual Studio 17 2022" -A Win32
cmake --build .\work_dir\ --config Release
Move-Item -Path .\work_dir\Release\ScreenInMC-CPP-Bridge.dll -Destination .\out\screen-in-mc-windows-i386.dll

$env:OpenCL_LIBRARY=$env:OpenCL64
Remove-Item .\work_dir\ -Recurse -Force -ErrorAction SilentlyContinue
Invoke-VsEnv amd64
cmake . -B .\work_dir\ -G "Visual Studio 17 2022" -A x64
cmake --build .\work_dir\ --config Release
Move-Item -Path .\work_dir\Release\ScreenInMC-CPP-Bridge.dll -Destination .\out\screen-in-mc-windows-amd64.dll

Remove-Item .\work_dir\ -Recurse -Force -ErrorAction SilentlyContinue