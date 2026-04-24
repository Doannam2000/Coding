$body = @{
    parts = @(@{type="text"; text="hello"})
    model = @{providerID="opencode"; modelID="big-pickle"}
} | ConvertTo-Json -Depth 10

$result = Invoke-RestMethod -Uri "http://127.0.0.1:4096/session/ses_264f02f52ffe5JqGdmlnovR9Oc/message" -Method POST -Body $body -ContentType "application/json"

$result | ConvertTo-Json -Depth 10