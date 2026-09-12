#Requires -Version 5.1
# Drives a real dedicated server via RCON and captures raw /gtsnlib command output.
param(
    [int]$Port = 25575,
    [string]$Password = "gtsnlib-rcon",
    [string]$OutFile = "logs/acceptance/04-server-commands.txt",
    [string]$StdOut = "logs/acceptance/04-server-stdout.log",
    [string]$StdErr = "logs/acceptance/04-server-stderr.log"
)

$ErrorActionPreference = "Stop"
$repo = (Get-Location).Path

function New-Packet([int]$id, [int]$type, [string]$body) {
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $size = 4 + 4 + $bodyBytes.Length + 2
    $ms = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter($ms)
    $bw.Write([int]$size)
    $bw.Write([int]$id)
    $bw.Write([int]$type)
    $bw.Write($bodyBytes)
    $bw.Write([byte]0)
    $bw.Write([byte]0)
    $bw.Flush()
    return $ms.ToArray()
}

function Read-Packet($stream) {
    $lenBytes = New-Object byte[] 4
    $read = 0
    while ($read -lt 4) {
        $r = $stream.Read($lenBytes, $read, 4 - $read)
        if ($r -le 0) { throw "connection closed while reading length" }
        $read += $r
    }
    $size = [System.BitConverter]::ToInt32($lenBytes, 0)
    if ($size -lt 10 -or $size -gt 65536) { throw "bad packet size $size" }
    $payload = New-Object byte[] $size
    $read = 0
    while ($read -lt $size) {
        $r = $stream.Read($payload, $read, $size - $read)
        if ($r -le 0) { throw "connection closed while reading payload" }
        $read += $r
    }
    $id = [System.BitConverter]::ToInt32($payload, 0)
    $type = [System.BitConverter]::ToInt32($payload, 4)
    $bodyLen = $size - 10
    $body = [System.Text.Encoding]::UTF8.GetString($payload, 8, $bodyLen)
    return [pscustomobject]@{ Id = $id; Type = $type; Body = $body }
}

function Send-Command($client, $stream, [string]$cmd, [string]$capture) {
    $bytes = New-Packet 7 2 $cmd
    $stream.Write($bytes, 0, $bytes.Length)
    $stream.Flush()
    $listener = [System.Diagnostics.Stopwatch]::StartNew()
    $sb = New-Object System.Text.StringBuilder
    while ($true) {
        if ($client.Available -le 0) {
            if ($listener.ElapsedMilliseconds -gt 1500) { break }
            Start-Sleep -Milliseconds 50
            continue
        }
        $pkt = Read-Packet $stream
        if ($pkt.Body.Length -eq 0) { break }
        [void]$sb.Append($pkt.Body)
        $listener.Restart()
    }
    $out = $sb.ToString()
    Add-Content -LiteralPath $capture -Value ("### /" + $cmd)
    if ([string]::IsNullOrEmpty($out)) {
        Add-Content -LiteralPath $capture -Value "(no output)"
    } else {
        Add-Content -LiteralPath $capture -Value $out
    }
    Add-Content -LiteralPath $capture -Value ""
    Write-Host ("[rcon] /" + $cmd)
    Write-Host $out
}

$stdoutFull = Join-Path $repo $StdOut
$stderrFull = Join-Path $repo $StdErr
$outFull = Join-Path $repo $OutFile
foreach ($f in @($stdoutFull, $stderrFull, $outFull)) {
    if (Test-Path -LiteralPath $f) { Remove-Item -LiteralPath $f -Force }
}
Set-Content -LiteralPath $outFull -Value ("# RCON /gtsnlib command sweep - " + (Get-Date -Format o))

$proc = Start-Process -FilePath (Join-Path $repo "gradlew.bat") `
    -ArgumentList "runServer", "--console=plain" `
    -WorkingDirectory $repo `
    -RedirectStandardOutput $stdoutFull `
    -RedirectStandardError $stderrFull `
    -NoNewWindow -PassThru
Write-Host "[rcon] started gradle runServer pid=$($proc.Id)"

$client = $null
$deadline = (Get-Date).AddSeconds(240)
try {
    while ((Get-Date) -lt $deadline) {
        if ($proc.HasExited) { throw "server process exited early (exit=$($proc.ExitCode)); see $StdErr" }
        try {
            $client = New-Object System.Net.Sockets.TcpClient
            $client.Connect("127.0.0.1", $Port)
            if ($client.Connected) { break }
        } catch {
            if ($client) { $client.Dispose(); $client = $null }
            Start-Sleep -Milliseconds 750
        }
    }
    if (-not $client -or -not $client.Connected) { throw "RCON port $Port did not open within timeout" }
    Write-Host "[rcon] connected to 127.0.0.1:$Port"
    $stream = $client.GetStream()

    # Authenticate.
    $auth = New-Packet 7 3 $Password
    $stream.Write($auth, 0, $auth.Length); $stream.Flush()
    $authResp = Read-Packet $stream
    if ($authResp.Id -eq -1) { throw "RCON auth failed" }
    Write-Host "[rcon] authenticated (id=$($authResp.Id))"

    Start-Sleep -Seconds 2

    $commands = @(
        "gtsnlib",
        "gtsnlib gt",
        "gtsnlib gt material gtsnlib:star_alloy",
        "gtsnlib gt material iron",
        "gtsnlib gt fluid gtsnlib:star_alloy_plasma",
        "gtsnlib gt fluid gtsnlib:stellar_air",
        "gtsnlib reg",
        "gtsnlib mek",
        "gtsnlib mek chemical gtsnlib:test_chemical",
        "gtsnlib ui"
    )
    foreach ($c in $commands) {
        Send-Command $client $stream $c $outFull
        Start-Sleep -Milliseconds 300
    }

    Send-Command $client $stream "stop" $outFull | Out-Null
    Start-Sleep -Seconds 2
    Write-Host "[rcon] stop sent; waiting for server to exit"
    $proc.WaitForExit(120000) | Out-Null
    Write-Host "[rcon] server exited=$($proc.HasExited) code=$($proc.ExitCode)"
} finally {
    if ($client) { $client.Dispose() }
    if (-not $proc.HasExited) {
        Write-Host "[rcon] force-killing gradle process tree"
        taskkill /PID $proc.Id /T /F 2>&1 | Out-Null
    }
}
