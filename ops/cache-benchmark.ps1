param(
    [string] $BaseUrl = "http://127.0.0.1:8088",
    [string] $ServerId = "local-compose",
    [int] $MissRuns = 3,
    [int] $HitRuns = 20
)

$endpoint = "$($BaseUrl.TrimEnd('/'))/api/v1/metrics/servers/$ServerId"
$redisKey = "sentinel:server:$ServerId"

function Clear-ServerCache {
    docker-compose exec -T redis redis-cli DEL $redisKey | Out-Null
}

function Measure-Request {
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-RestMethod -Uri $endpoint | Out-Null
    $stopwatch.Stop()
    return [double]$stopwatch.Elapsed.TotalMilliseconds
}

$missTimes = @()
for ($index = 0; $index -lt $MissRuns; $index++) {
    Clear-ServerCache
    $missTimes += Measure-Request
}

$hitTimes = @()
for ($index = 0; $index -lt $HitRuns; $index++) {
    $hitTimes += Measure-Request
}

$cache = Invoke-RestMethod -Uri "$($BaseUrl.TrimEnd('/'))/api/v1/metrics/cache"

[ordered]@{
    serverId = $ServerId
    endpoint = $endpoint
    missRuns = $MissRuns
    hitRuns = $HitRuns
    missAverageMs = [math]::Round(($missTimes | Measure-Object -Average).Average, 2)
    hitAverageMs = [math]::Round(($hitTimes | Measure-Object -Average).Average, 2)
    hitMinMs = [math]::Round(($hitTimes | Measure-Object -Minimum).Minimum, 2)
    hitMaxMs = [math]::Round(($hitTimes | Measure-Object -Maximum).Maximum, 2)
    cacheHits = $cache.hits
    cacheMisses = $cache.misses
    cacheHitRate = $cache.hitRate
} | ConvertTo-Json
