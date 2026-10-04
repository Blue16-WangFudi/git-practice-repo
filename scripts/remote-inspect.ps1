param(
    [Parameter(Mandatory = $true)] [string] $SshHost,
    [Parameter(Mandatory = $true)] [string] $User,
    [string] $IdentityFile = ""
)

$remote = @'
set -eu
echo "=== 时间 ==="
date
echo "=== CPU / Load ==="
top -b -n 1 | head -n 5 || true
echo "=== 内存 ==="
free -h || true
echo "=== 磁盘 ==="
df -h /
echo "=== Docker 容器 ==="
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' || true
echo "=== Docker 资源 ==="
docker stats --no-stream || true
'@

$sshArgs = @()
if ($IdentityFile) {
    $sshArgs += @("-i", $IdentityFile)
}
$sshArgs += @("${User}@${SshHost}", $remote)
& ssh @sshArgs
