# build-installer.ps1
# Requires Java 17+ and WiX Toolset installed locally to generate an .exe on Windows.

Write-Host "Building SmartPlayer fat JAR using Maven..."
mvn clean package -f pom.xml

if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed. Aborting installer creation."
    exit $LASTEXITCODE
}

Write-Host "Cleaning up old installer directory..."
if (Test-Path "installer") {
    Remove-Item -Recurse -Force "installer"
}

Write-Host "Packaging SmartPlayerSetup.exe using jpackage..."

# The version needs to be extracted from pom.xml, but we hardcode 1.0.0 for this script
$APP_VERSION = "1.0.0"

jpackage `
  --type exe `
  --name SmartPlayer `
  --app-version $APP_VERSION `
  --vendor "Suraj Meher" `
  --description "A modern JavaFX + VLCJ desktop video player" `
  --input target/ `
  --main-jar smartplayer-1.0.0-shaded.jar `
  --main-class com.smartplayer.Launcher `
  --icon src/main/resources/icons/app.ico `
  --win-shortcut `
  --win-menu `
  --win-menu-group "SmartPlayer" `
  --win-dir-chooser `
  --win-per-user-install `
  --dest installer

if ($LASTEXITCODE -eq 0) {
    Write-Host "Installer built successfully in the 'installer' directory!" -ForegroundColor Green
} else {
    Write-Error "jpackage failed. Ensure WiX Toolset v3 is installed and in your PATH."
}
