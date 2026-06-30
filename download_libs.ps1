# PowerShell script to download required dependencies for the project

$libDir = Join-Path $PSScriptRoot "lib"
if (-not (Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir | Out-Null
    Write-Host "Created lib directory at: $libDir"
}

# 1. Download MySQL Connector/J
$mysqlJar = "mysql-connector-j-8.3.0.jar"
$mysqlUrl = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"
$mysqlPath = Join-Path $libDir $mysqlJar

if (-not (Test-Path $mysqlPath)) {
    Write-Host "Downloading MySQL Connector/J from: $mysqlUrl"
    try {
        Invoke-WebRequest -Uri $mysqlUrl -OutFile $mysqlPath -UseBasicParsing
        Write-Host "Successfully downloaded $mysqlJar"
    } catch {
        Write-Error "Failed to download MySQL Connector/J: $_"
    }
} else {
    Write-Host "$mysqlJar already exists."
}

# 2. Download FlatLaf Swing Look and Feel
$flatlafJar = "flatlaf-3.4.1.jar"
$flatlafUrl = "https://repo1.maven.org/maven2/com/formdev/flatlaf/3.4.1/flatlaf-3.4.1.jar"
$flatlafPath = Join-Path $libDir $flatlafJar

if (-not (Test-Path $flatlafPath)) {
    Write-Host "Downloading FlatLaf from: $flatlafUrl"
    try {
        Invoke-WebRequest -Uri $flatlafUrl -OutFile $flatlafPath -UseBasicParsing
        Write-Host "Successfully downloaded $flatlafJar"
    } catch {
        Write-Error "Failed to download FlatLaf: $_"
    }
} else {
    Write-Host "$flatlafJar already exists."
}

Write-Host "Dependency setup complete!"
