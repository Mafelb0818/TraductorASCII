$ErrorActionPreference='Stop'
$PSNativeCommandUseErrorActionPreference=$true
# Requiere PowerShell 7 y un JDK de Java 17 o posterior.
if(!$env:ANDROID_HOME -or !$env:JAVA_HOME) {
    throw 'Define ANDROID_HOME con la ruta del SDK y JAVA_HOME con la ruta del JDK.'
}
$sdk=$env:ANDROID_HOME
$jdk=Join-Path $env:JAVA_HOME 'bin'
$bt="$sdk\build-tools\36.0.0"
$android="$sdk\platforms\android-37.0\android.jar"
$src="$PSScriptRoot\app\src\main"
$obj="$PSScriptRoot\build\sdk-apk"
New-Item -ItemType Directory -Force "$obj\classes","$obj\dex" | Out-Null
& "$bt\aapt2.exe" compile --dir "$src\res" -o "$obj\compiled.zip"
(Get-Content "$src\AndroidManifest.xml" -Raw).Replace('<manifest ', '<manifest package="edu.taller.ascii" ') | Set-Content -Encoding utf8 "$obj\AndroidManifest.xml"
& "$bt\aapt2.exe" link --manifest "$obj\AndroidManifest.xml" --rename-manifest-package edu.taller.ascii --min-sdk-version 23 --target-sdk-version 35 --version-code 1 --version-name 1.0 -I $android -o "$obj\resources.apk" "$obj\compiled.zip"
$java=Get-ChildItem "$src\java" -Recurse -Filter *.java | Select-Object -Expand FullName
& "$jdk\javac.exe" --release 17 -encoding UTF-8 -cp $android -d "$obj\classes" @java
& "$jdk\jar.exe" cf "$obj\classes.jar" -C "$obj\classes" .
& "$jdk\java.exe" -cp "$bt\lib\d8.jar" com.android.tools.r8.D8 --min-api 23 --lib $android --output "$obj\dex" "$obj\classes.jar"
& "$jdk\jar.exe" uf "$obj\resources.apk" -C "$obj\dex" classes.dex
& "$bt\zipalign.exe" -f 4 "$obj\resources.apk" "$obj\aligned.apk"
if(!(Test-Path "$obj\debug.jks")){
    & "$jdk\keytool.exe" -genkeypair -keystore "$obj\debug.jks" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US'
}
& "$jdk\java.exe" -jar "$bt\lib\apksigner.jar" sign --ks "$obj\debug.jks" --ks-pass pass:android --key-pass pass:android --out "$obj\TraductorASCII.apk" "$obj\aligned.apk"
& "$jdk\java.exe" -jar "$bt\lib\apksigner.jar" verify --verbose "$obj\TraductorASCII.apk"
& "$bt\aapt2.exe" dump badging "$obj\TraductorASCII.apk" | Select-Object -First 9


