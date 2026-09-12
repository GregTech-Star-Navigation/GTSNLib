# GTSNLib #23 字体放大对比证据生成脚本（Windows PowerShell / System.Drawing）
#
# 从自动测试的两张截图里，截取「同一段中英混排样例文本」区域并放大 >=4x，
# 生成可直接肉眼判别的字形对比证据：
#   font-zoom-theme-4x.png    —— 主题字体（gtsnlib:sarasa_ui_sc，平滑现代无衬线）
#   font-zoom-vanilla-4x.png  —— 原版字体（minecraft:default，块状像素字）
#   font-zoom-compare-4x.png  —— 两者上下堆叠 + 标注，单图自证
#
# 用法（仓库根目录）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File docs/acceptance/zoom-font-proof.ps1
#
# 采集命令：$env:GTSNLIB_UI_AUTOTEST="1"; .\gradlew.bat runClient

param(
    [string]$ThemeShot = "docs/acceptance/screenshots/gtsnlib-ui-font-theme.png",
    [string]$VanillaShot = "docs/acceptance/screenshots/gtsnlib-ui-font-vanilla.png",
    [string]$OutDir = "docs/acceptance/screenshots",
    [int]$X = 636,
    [int]$Y = 56,
    [int]$W = 400,
    [int]$H = 28,
    [int]$Scale = 4
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Crop-Scale([string]$inPath, [string]$outPath, [int]$x, [int]$y, [int]$w, [int]$h, [int]$scale) {
    $src = [System.Drawing.Image]::FromFile($inPath)
    try {
        $rect = New-Object System.Drawing.Rectangle($x, $y, $w, $h)
        $dst = New-Object System.Drawing.Bitmap(($w * $scale), ($h * $scale))
        $g = [System.Drawing.Graphics]::FromImage($dst)
        try {
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $g.DrawImage($src, (New-Object System.Drawing.Rectangle(0, 0, ($w * $scale), ($h * $scale))), $rect, [System.Drawing.GraphicsUnit]::Pixel)
        } finally { $g.Dispose() }
        $dst.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $dst.Dispose()
    } finally { $src.Dispose() }
}

$themeOut = Join-Path $OutDir "font-zoom-theme-4x.png"
$vanillaOut = Join-Path $OutDir "font-zoom-vanilla-4x.png"
$compareOut = Join-Path $OutDir "font-zoom-compare-4x.png"

Crop-Scale $ThemeShot $themeOut $X $Y $W $H $Scale
Crop-Scale $VanillaShot $vanillaOut $X $Y $W $H $Scale

# 堆叠成单张对照图（含标注），避免只看单图时误判。
$theme = [System.Drawing.Bitmap]::new((Resolve-Path $themeOut).Path)
$vanilla = [System.Drawing.Bitmap]::new((Resolve-Path $vanillaOut).Path)
$labelH = 28
$canvas = [System.Drawing.Bitmap]::new($theme.Width, (($theme.Height + $labelH) * 2))
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.Clear([System.Drawing.Color]::FromArgb(24, 28, 34))
$brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$font = [System.Drawing.Font]::new("Consolas", 14)
$g.DrawString("THEME FONT  gtsnlib:sarasa_ui_sc", $font, $brush, 4, 4)
$g.DrawImage($theme, 0, $labelH)
$g.DrawString("VANILLA FONT  minecraft:default", $font, $brush, 4, $labelH + $theme.Height + 4)
$g.DrawImage($vanilla, 0, $labelH + $theme.Height + $labelH)
$g.Dispose()
$brush.Dispose()
$font.Dispose()
$canvas.Save($compareOut, [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()
$theme.Dispose()
$vanilla.Dispose()

Write-Output "wrote $themeOut"
Write-Output "wrote $vanillaOut"
Write-Output "wrote $compareOut"
