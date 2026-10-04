param(
    [Parameter(Mandatory = $true)] [string] $SshHost,
    [Parameter(Mandatory = $true)] [string] $User,
    [int] $LocalPort = 8088,
    [int] $RemotePort = 8088,
    [string] $IdentityFile = ""
)

$sshArgs = @("-N", "-L", "${LocalPort}:127.0.0.1:${RemotePort}")
if ($IdentityFile) {
    $sshArgs += @("-i", $IdentityFile)
}
$sshArgs += "${User}@${SshHost}"
Write-Host "建立 SSH Tunnel：本机 $LocalPort -> 服务器 127.0.0.1:$RemotePort"
& ssh @sshArgs
