@echo off
echo Testing Bitunix Scalper Bot Web Interface...
echo.

echo 1. Testing API endpoints...
echo.

echo Testing /api/test/status:
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/test/status' -UseBasicParsing; Write-Host 'Status:' $response.StatusCode; Write-Host 'Response:' $response.Content } catch { Write-Host 'Error:' $_.Exception.Message }"
echo.

echo Testing /api/rate-limiter/status:
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/rate-limiter/status' -UseBasicParsing; Write-Host 'Status:' $response.StatusCode; Write-Host 'Response:' $response.Content } catch { Write-Host 'Error:' $_.Exception.Message }"
echo.

echo 2. Testing main dashboard...
echo.

echo Testing main page:
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080' -UseBasicParsing -TimeoutSec 30; Write-Host 'Status:' $response.StatusCode; Write-Host 'Content-Type:' $response.Headers['Content-Type']; Write-Host 'Content Length:' $response.Content.Length; if($response.Content -match 'Bitunix|Scalper|Trading') { Write-Host 'Dashboard content looks good!' } else { Write-Host 'Dashboard content may have issues' } } catch { Write-Host 'Error accessing dashboard:' $_.Exception.Message }"
echo.

echo 3. Opening browser...
echo.

echo Opening http://localhost:8080 in default browser...
start http://localhost:8080

echo.
echo Test completed!
pause
