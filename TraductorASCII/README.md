# Traductor ASCII · Taller 3

Aplicación nativa para **Android 6.0 (API 23) o posterior**, escrita en Java. Funciona sin conexión. Abre en el menú con accesos a Texto a ASCII, ASCII a Texto y Ajustes.

## Instalar el APK en un celular

1. Descarga `TraductorASCII.apk` en el celular o cópialo por USB a la carpeta Descargas.
2. En el celular, abre **Archivos → Descargas** y toca el APK.
3. Si Android lo solicita, permite que esa aplicación instale desde esa fuente, vuelve al instalador y toca **Instalar**.
4. Toca **Abrir** o busca **Traductor ASCII** entre tus aplicaciones.
5. En **Texto a ASCII**, escribe `HOLA` y toca **Traducir**. Debe aparecer `72 79 76 65`. En **ASCII a Texto**, escribe esos códigos y toca **Decodificar**. Debe aparecer `HOLA`.

El APK está firmado con una clave de prueba para el taller. Si instalas una compilación local con otra firma sobre este APK, Android exigirá desinstalar la anterior primero; eso borra sus ajustes.

## Abrir y ejecutar desde Android Studio

1. Extrae el ZIP. En Android Studio selecciona **File → Open** y abre la carpeta `TraductorASCII`, donde está `settings.gradle`.
2. En **Tools → SDK Manager**, instala la plataforma **Android API 37** y **Android SDK Build-Tools 36.0.0** si faltan. Usa una versión de Android Studio compatible con **Android Gradle Plugin 9.4.1**. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, selecciona el JDK incluido con Android Studio (JBR).
3. Espera la sincronización de Gradle. La primera vez necesita conexión para descargar Gradle 9.6.0 y los componentes que falten.
4. Para usar un **emulador**, ve a **Tools → Device Manager**, crea un dispositivo virtual (por ejemplo, Pixel 6 con Android 13), descarga su imagen si hace falta e inícialo. Para usar un **celular**, activa las **Opciones de desarrollador** pulsando varias veces **Número de compilación**, habilita **Depuración USB**, conecta el cable y acepta la autorización en el teléfono.
5. Selecciona el dispositivo y la configuración **app** en la barra superior. Pulsa **Run ▶**. Android Studio compila, instala y abre la app.
6. Para generar solo el APK desde PowerShell, abre una terminal en la raíz del proyecto y ejecuta `.\gradlew.bat assembleDebug`. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`. En Linux o macOS usa `sh ./gradlew assembleDebug`.

Si Gradle no puede iniciar, el proyecto incluye una alternativa para Windows. En PowerShell 7, define `ANDROID_HOME` y `JAVA_HOME` con las rutas del SDK y el JDK, y ejecuta `.\Compilar-con-SDK.ps1`. El APK queda en `build/sdk-apk/TraductorASCII.apk`.

Referencias oficiales: [Compilar y ejecutar en Android Studio](https://developer.android.com/studio/run) · [Probar en un dispositivo físico](https://developer.android.com/studio/run/device) · [Compilar desde la terminal](https://developer.android.com/build/building-cmdline).

## Qué hace cada pantalla

- **Menú principal:** abre las tres herramientas. La flecha superior y el botón Atrás de Android regresan al menú.
- **Texto a ASCII:** genera un número por carácter. **Elegir carácter** inserta uno de los 95 caracteres imprimibles. **Arte ASCII** muestra una figura con el primer carácter visible de la entrada y el borde elegido. Escribe `A` y selecciona `#` para probarlo.
- **ASCII a Texto:** acepta espacios, comas y punto y coma; también acepta tabulaciones y saltos de línea entre códigos. Cada código debe estar entre 0 y 127.
- **Ajustes:** guarda tema claro u oscuro, separador de salida, tamaño de letra e idioma (español o inglés). **Acerca de** explica el alcance de la aplicación.

En los traductores, **Copiar** envía el resultado al portapapeles, **Limpiar** vacía la entrada y el resultado, y **CSV** abre el selector de archivos de Android. Elige una carpeta y pulsa **Guardar**. El archivo tiene las columnas `modo,entrada,resultado`.

## Descripción técnica

| Componente | Responsabilidad |
|---|---|
| `MainActivity.java` | Navegación y vistas nativas, estado de cada pantalla, validación visible, portapapeles y selector de archivos. |
| `Ascii.java` | Codificación, decodificación, representación de controles, figura de texto y generación de CSV. No depende de Android. |
| `SharedPreferences` | Guarda solo las preferencias. Las entradas y los resultados se conservan al navegar y durante la recreación de la actividad. |
| `tests/AsciiTest.java` | Pruebas de conversión de los 128 valores, controles, entradas inválidas, figura y comillas en CSV. |

ASCII estándar consta de 128 valores. La app conserva espacios, mayúsculas y minúsculas; informa si encuentra tildes, ñ o emojis. Los controles invisibles se representan con símbolos cuando hace falta; al copiar se mantiene el texto decodificado original. El CSV se guarda en UTF-8, escapa las comillas y antepone una comilla simple a celdas que una hoja de cálculo podría interpretar como fórmulas. La app no declara permisos de red ni de almacenamiento: el usuario escoge dónde guardar el archivo.

### Ejecutar pruebas de lógica en PowerShell

```powershell
New-Item -ItemType Directory -Force build/test-classes | Out-Null
javac -encoding UTF-8 -d build/test-classes app/src/main/java/edu/taller/ascii/Ascii.java tests/AsciiTest.java
java -cp build/test-classes edu.taller.ascii.AsciiTest
```

## Lista de validación y evidencias

| Módulo | Acción | Resultado esperado |
|---|---|---|
| Menú | Abrir la app y tocar cada tarjeta | Cada tarjeta abre su pantalla; Atrás retorna al menú. |
| Texto a ASCII | Traducir `HOLA` | `72 79 76 65`. |
| Selector y figura | Elegir `A`, traducir y generar figura con `#` | Código `65` y figura con `A` en el centro. |
| ASCII a Texto | Decodificar `72, 79; 76 65` | `HOLA`. |
| Validación | Probar un campo vacío, `niño`, `72,,79` y `128` | Mensaje específico en cada caso; no queda un resultado viejo. |
| Copia y limpieza | Copiar, pegar en otra aplicación y limpiar | El pegado coincide con el resultado; se vacían entrada y resultado. |
| Exportación | Guardar el CSV y abrirlo en Archivos | Un encabezado y una fila con el modo, la entrada y el resultado. |
| Ajustes | Activar oscuro, coma y A+; cerrar y volver a abrir | Se conservan las preferencias; `AB` se traduce como `65, 66`. |

Para documentar los módulos, toma una captura del menú, una de cada traductor con el ejemplo resuelto y una de Ajustes. Captura también la figura, un mensaje de error y el CSV abierto si quieres demostrar todos los criterios. En el emulador puedes usar el botón de cámara; en muchos teléfonos, el atajo es **encendido + bajar volumen**.

## Guion de video (aprox. 3 minutos)

| Tiempo | Qué mostrar y explicar |
|---|---|
| 0:00–0:20 | Abre la app. Presenta el objetivo: convertir entre texto y códigos ASCII decimales. Muestra las tres tarjetas. |
| 0:20–1:00 | Traduce `HOLA`; explica que cada carácter produce un número. Usa el selector para insertar `A` y muestra la figura. |
| 1:00–1:35 | Decodifica `72 79 76 65`, copia y pega el resultado. Limpia y prueba `128` para mostrar la validación. |
| 1:35–2:10 | Guarda un CSV y ábrelo para mostrar sus tres columnas. |
| 2:10–2:40 | Cambia tema y tamaño. Selecciona coma y traduce `AB` para mostrar que el ajuste tiene efecto. |
| 2:40–3:00 | Cierra explicando la separación entre la interfaz y la lógica Java, y el límite de 0 a 127. |

Usa el grabador de pantalla del celular o **Record Screen** del emulador. Incluye tu explicación y revisa que la grabación dure menos de cinco minutos.


### Resultado de la verificación

Se verificó la firma del APK y se completaron 274 comprobaciones automáticas: codificación y decodificación de los 128 valores, manejo de controles, entradas incorrectas, figura ASCII y formato CSV. También se verificó la compilación con el script del SDK. El emulador de este entorno no completó el arranque; las pruebas de interacción en Android, las capturas y la grabación quedan pendientes.
