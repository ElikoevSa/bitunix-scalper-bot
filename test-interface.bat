@echo off
echo Testing Bitunix Scalper Bot Interface...
echo.

echo 1. Testing static test page...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/test.html' -UseBasicParsing; Write-Host 'Test Page Status:' $response.StatusCode; Write-Host 'Content Length:' $response.Content.Length; Write-Host 'SUCCESS: Test page is working!' } catch { Write-Host 'ERROR: Test page failed:' $_.Exception.Message }"
echo.

echo 2. Testing API endpoints...
echo.
echo Testing /api/test/status:
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/test/status' -UseBasicParsing; Write-Host 'Status:' $response.StatusCode; Write-Host 'Response:' $response.Content } catch { Write-Host 'Error:' $_.Exception.Message }"
echo.

echo Testing /api/rate-limiter/status:
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/rate-limiter/status' -UseBasicParsing; Write-Host 'Status:' $response.StatusCode; Write-Host 'Response:' $response.Content } catch { Write-Host 'Error:' $_.Exception.Message }"
echo.

echo 3. Testing main dashboard (with timeout)...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080' -UseBasicParsing -TimeoutSec 15; Write-Host 'Dashboard Status:' $response.StatusCode; Write-Host 'Content Length:' $response.Content.Length; Write-Host 'SUCCESS: Dashboard loaded!' } catch { Write-Host 'Dashboard timeout or error:' $_.Exception.Message }"
echo.

echo 4. Opening browsers...
echo.
echo Opening test page in browser...
start http://localhost:8080/test.html

timeout 2

echo Opening main dashboard in browser...
start http://localhost:8080

echo.
echo Test completed!
echo.
echo If the dashboard loads slowly, it's due to API rate limiting.
echo The test page should load instantly.
echo.
pause
