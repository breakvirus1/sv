$scriptsDir = $PSScriptRoot

$scripts = @(
    @{ Name = "all"; Label = "Rebuild ALL services (rebuild.ps1)" }
    @{ Name = "rebuild-api-gateway"; Label = "API Gateway" }
    @{ Name = "rebuild-calculator-service"; Label = "Calculator Service" }
    @{ Name = "rebuild-client-service"; Label = "Client Service" }
    @{ Name = "rebuild-comment-service"; Label = "Comment Service" }
    @{ Name = "rebuild-discovery-server"; Label = "Discovery Server" }
    @{ Name = "rebuild-employee-service"; Label = "Employee Service" }
    @{ Name = "rebuild-file-service"; Label = "File Service" }
    @{ Name = "rebuild-frontend"; Label = "Frontend" }
    @{ Name = "rebuild-generate-data-service"; Label = "Generate Data Service" }
    @{ Name = "rebuild-material-service"; Label = "Material Service" }
    @{ Name = "rebuild-order-service"; Label = "Order Service" }
    @{ Name = "rebuild-statistic-service"; Label = "Statistic Service" }
)

function Show-Menu {
    Clear-Host
    Write-Host "=============================="
    Write-Host "   Rebuild Service Menu"
    Write-Host "=============================="
    Write-Host ""

    for ($i = 0; $i -lt $scripts.Length; $i++) {
        Write-Host (" {0,2}) {1}" -f ($i + 1), $scripts[$i].Label)
    }

    Write-Host ""
    Write-Host (" {0,2}) {1}" -f 0, "Exit")
    Write-Host ""

    $choice = Read-Host "Choose service to rebuild"

    if ($choice -eq "0") {
        Write-Host "Exiting."
        exit 0
    }

    if ($choice -match '^\d+$' -and [int]$choice -ge 1 -and [int]$choice -le $scripts.Length) {
        $index = [int]$choice - 1
        $scriptName = $scripts[$index].Name

        if ($scriptName -eq "all") {
            Write-Host ""
            Write-Host "=============================="
            Write-Host " Running: $($scripts[$index].Label)"
            Write-Host "=============================="
            Write-Host ""
            & "$scriptsDir\rebuild.ps1"
        }
        else {
            $scriptPath = Join-Path $scriptsDir ($scriptName + ".ps1")

            if (-not (Test-Path $scriptPath)) {
                Write-Host "Script not found: $scriptPath"
                exit 1
            }

            Write-Host ""
            Write-Host "=============================="
            Write-Host " Running: $($scripts[$index].Label)"
            Write-Host "=============================="
            Write-Host ""
            & $scriptPath
        }
    }
    else {
        Write-Host "Invalid choice: $choice"
        exit 1
    }
}

Show-Menu
