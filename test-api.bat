@echo off
echo Testing API connections...
echo.

echo Testing Bitunix API...
curl -s "https://api.bitunix.com/api/v1/ticker/24hr" | head -c 200
echo.
echo.

echo Testing Binance API...
curl -s "https://api.binance.com/api/v3/ticker/24hr" | head -c 200
echo.
echo.

echo Testing CoinGecko API...
curl -s "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=5&page=1" | head -c 200
echo.
echo.

echo API test completed.
pause
