# Free Scratch 🎨

App Android para **trazar imágenes sobre la cámara en vivo**: elige una
imagen (de tu teléfono o una plantilla vectorial de Pixabay), projétala
sobre lo que ve la cámara y dibújala a mano alzada sobre papel.

Procesamiento **100% local**: sin cuentas, sin servidores propios, la
cámara y tus imágenes nunca salen del dispositivo (salvo la búsqueda en
Pixabay, que usa su API pública).

## Características

- **Cámara en vivo** con imagen superpuesta: opacidad, tamaño,
  inclinación, giro y superficies (plano, vaso, rodillo, pelota).
- **Marco de selección** con arrastre de esquinas y detección de papel.
- **Congelar vista**: la imagen queda anclada al papel aunque muevas el
  teléfono.
- **Flash / linterna**, cámara frontal y trasera, deshacer/rehacer,
  presets de tamaño (0.5x / 1.0x / 1.5x).
- **Templates de Pixabay**: busca vectores y dibujos por palabra y
  úsalos como referencia (paginación con carga automática).
- **English / Español**: selector de idioma con banderitas junto al
  logo (inglés por defecto, la elección se recuerda).
- Selector de imagen desde la galería del teléfono.

## Construcción

Requisitos: Android Studio (o JDK 17+), Android SDK 34.

```bash
# 1. Prepara tus claves (una sola vez)
cp apikeys.properties.example apikeys.properties
#    y edita apikeys.properties con tus claves reales

# 2. Compila
gradlew assembleDebug          # Windows
./gradlew assembleDebug        # macOS / Linux
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Claves de API

| Servicio | Dónde va | Ejemplo de uso |
|---|---|---|
| **Pixabay** | `apikeys.properties` → `Pixabay = …` | Búsqueda de vectores en la app |

- `apikeys.properties` **está en `.gitignore`**: el repo solo incluye la
  plantilla `apikeys.properties.example` con valores en blanco.
- Al compilar, Gradle inyecta la clave en `BuildConfig.PIXABAY_API_KEY`.
- Si el archivo o la clave faltan, la app sigue funcionando pero la
  búsqueda en Pixabay avisa que falta configurar la clave.

## Estructura

```
app/src/main/java/com/pintafacil/app/
├── ui/            # Pantallas (inicio, trazador, controles, dock)
├── overlay/       # Renderizado de la imagen superpuesta
├── detect/        # Detección de papel (Otsu + componentes conexos)
└── util/          # Galería, Pixabay API
```

## Licencia

MIT — ver [LICENSE](LICENSE).
