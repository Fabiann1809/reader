# Asistente de Lectura — Documento de proyecto y tareas

> Documento pensado para ser entregado a Claude Code. Contiene el contexto, las decisiones tomadas, el alcance y una lista de tareas cortas y verificables.

---

## 1. Resumen del proyecto

App móvil **solo Android** que funciona como **organizador y asistente de lectura**. El usuario administra sus libros, lleva el seguimiento de su lectura y usa IA para entender y retener lo que lee.

Flujo central del MVP:

> Fotografío una página → la app extrae el texto → la IA me lo explica (método Feynman) → guardo la nota.

### Funciones de la visión completa
- Biblioteca de libros y seguimiento de lectura.
- Captura rápida de páginas físicas con la cámara, o selección directa de texto desde el libro dentro de la app (libros importados).
- Notas de voz en movimiento.
- Repaso rápido mediante fichas.
- **Explicador multimodal:** al fotografiar o seleccionar un párrafo confuso, la IA lo desglosa con el método Feynman (términos sencillos y analogías cotidianas).
- **Evaluación de comprensión:** la IA analiza la interpretación escrita del usuario y genera quizzes tipo test del capítulo leído, **solo si el usuario lo desea**.

---

## 2. Decisiones tomadas

| Tema | Decisión |
|---|---|
| Plataforma | Solo Android |
| Lenguaje / framework | **Kotlin nativo** con Android Studio y **Jetpack Compose** (se descartaron Expo/React Native y Flutter) |
| Almacenamiento | **Local en el dispositivo** (Room/SQLite y almacenamiento interno). Sin cuentas ni sincronización |
| Libros | El usuario **importa sus propios archivos** (EPUB/PDF). La app no descarga ni distribuye libros. (Importación queda fuera del MVP inicial) |
| Conexión | No será offline: la IA requiere internet |
| IA en el MVP | **El usuario pone su propia clave de API (BYOK).** Sin backend |
| IA a futuro | Modo con cupo gratuito administrado por el autor, con función intermediaria y Play Integrity (post-MVP) |
| OCR | Google ML Kit Text Recognition, en el dispositivo (gratis) |
| Costo | Todo lo posible gratis. APK distribuido directamente (sin Play Store) |
| Prioridad | Tener algo **funcional cuanto antes** |

---

## 3. Stack técnico

| Pieza | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose + Navigation Compose |
| Arquitectura | MVVM (ViewModel + StateFlow), repositorios |
| Base de datos | Room |
| Cámara | CameraX |
| OCR | ML Kit Text Recognition |
| Red (IA) | Retrofit u OkHttp + kotlinx.serialization |
| Clave de API | Android Keystore (DataStore cifrado o Tink). Nunca en texto plano ni en logs |
| Voz (post-MVP) | SpeechRecognizer de Android |
| EPUB/PDF (post-MVP) | Readium Kotlin Toolkit y PdfRenderer |
| Build | Android Studio, APK generado en local |

**minSdk:** API 26 o superior. **Nombre de la app:** `Reader` (provisional). **Paquete:** `io.github.fabiann1809.reader`.

---

## 4. Arquitectura y capa de IA

Estructura de paquetes sugerida:

```
app/
  data/        (Room: entidades, DAOs, base de datos, repositorios)
  ai/          (interfaz AiProvider, implementaciones, prompts)
  ocr/         (envoltorio de ML Kit)
  ui/          (pantallas Compose, ViewModels, navegación, tema)
  util/
```

**Capa de abstracción de IA (requisito):** toda llamada a un modelo pasa por una interfaz para poder cambiar de proveedor sin tocar la UI.

```kotlin
interface AiProvider {
    suspend fun explain(text: String): Result<String>
}
```

Más adelante se añadirán operaciones como `generateQuiz(...)`. La app debe manejar con mensajes claros: clave inválida, cuota agotada, límite de peticiones y falta de internet.

---

## 5. Modelo de datos (MVP)

**Book**
- `id` (PK), `title`, `author`, `currentPage` (Int), `totalPages` (Int?, opcional), `status` (POR_LEER / LEYENDO / TERMINADO), `createdAt`

**Note**
- `id` (PK), `bookId` (FK → Book, borrado en cascada), `page` (Int?), `sourceText` (texto original capturado, opcional), `content` (nota o explicación), `type` (MANUAL / EXPLICACION), `createdAt`

Tablas posteriores (post-MVP): `Flashcard`, `Quiz`, `QuizQuestion`, `VoiceNote`.

---

## 6. Alcance

### Dentro del MVP
1. Biblioteca simple (agregar libros manualmente, estado y página actual).
2. Notas por libro (manuales y generadas por IA).
3. Configuración de la clave de API con botón de prueba.
4. Captura con cámara (o galería) + OCR.
5. Explicador Feynman sobre el texto extraído (editable antes de enviar).
6. Guardar la explicación como nota.
7. Generar el APK de prueba.

### Fuera del MVP (no empezar hasta terminar el MVP)
- Importar EPUB/PDF y seleccionar texto dentro de la app.
- Notas de voz.
- Fichas de repaso y repetición espaciada.
- Quizzes y evaluación de comprensión.
- Respaldo (exportar/importar).
- Modo con cupo gratuito y backend intermediario.
- Cuentas, sincronización, social, versión web o iOS.

---

## 7. Reglas de trabajo para Claude Code

1. **Una tarea a la vez.** No adelantes tareas de otras fases.
2. Al terminar cada tarea, verifica que el proyecto **compila** y cumple su criterio de "Hecho cuando".
3. Haz un **commit por tarea o fix** siguiendo Conventional Commits en inglés con el ID de la tarea (por ejemplo `feat(library): add book list screen (T2.1)`). Ver sección 12.
4. **Nunca** escribas claves de API en el código, en el repositorio ni en los logs.
5. No agregues funciones fuera del alcance del MVP. Si algo es ambiguo, **pregunta** antes de asumir.
6. Mantén el código simple y legible: es un proyecto de aprendizaje además de un producto.
7. Explica brevemente qué hiciste y por qué al cerrar cada tarea.

---

## 8. Tareas del MVP

Cada tarea debe poder completarse en una sesión corta.

### Fase 0 — Preparación
- [x] **T0.1 Crear el proyecto.** Proyecto "Empty Activity" con Compose, Kotlin, minSdk 26. *Hecho cuando:* la app vacía corre en emulador o teléfono.
- [x] **T0.2 Dependencias.** Configurar catálogo de versiones y agregar Compose, Navigation, Room, CameraX, ML Kit, OkHttp/Retrofit, kotlinx.serialization, DataStore y Coroutines. *Hecho cuando:* sincroniza y compila.
- [x] **T0.3 Estructura de paquetes.** Crear los paquetes de la sección 4. *Hecho cuando:* existen y el proyecto compila.
- [x] **T0.4 Tema y navegación base.** Tema Material 3 y `NavHost` con una pantalla de inicio vacía. *Hecho cuando:* se puede navegar entre dos pantallas de prueba.

### Fase 1 — Datos locales
- [x] **T1.1 Entidad Book.** Según la sección 5. *Hecho cuando:* compila con Room.
- [x] **T1.2 Entidad Note.** Con clave foránea a Book y borrado en cascada. *Hecho cuando:* compila.
- [x] **T1.3 DAOs.** Operaciones para crear, leer, actualizar y borrar libros y notas; notas por libro como `Flow`. *Hecho cuando:* compila.
- [x] **T1.4 Base de datos y repositorios.** `AppDatabase` y repositorios `BookRepository` y `NoteRepository`. *Hecho cuando:* se pueden insertar y leer datos desde una prueba.
- [x] **T1.5 Pruebas de los DAOs.** Pruebas instrumentadas básicas. *Hecho cuando:* pasan.

### Fase 2 — Biblioteca
- [x] **T2.1 Lista de libros.** Pantalla con los libros guardados y estado vacío. *Hecho cuando:* muestra libros de la base de datos.
- [x] **T2.2 Agregar libro.** Formulario con título, autor y total de páginas opcional. *Hecho cuando:* el libro aparece en la lista.
- [x] **T2.3 Detalle del libro.** Muestra datos, progreso y estado. *Hecho cuando:* se abre desde la lista.
- [x] **T2.4 Actualizar progreso.** Cambiar página actual y estado. *Hecho cuando:* los cambios persisten al reiniciar la app.
- [x] **T2.5 Eliminar libro.** Con confirmación. *Hecho cuando:* también se borran sus notas.

### Fase 3 — Notas
- [x] **T3.1 Lista de notas por libro.** En el detalle del libro. *Hecho cuando:* muestra las notas ordenadas por fecha.
- [x] **T3.2 Crear nota manual.** Con página opcional. *Hecho cuando:* se guarda y se ve en la lista.
- [x] **T3.3 Editar y borrar nota.** *Hecho cuando:* los cambios persisten.

### Fase 4 — Configuración de IA
- [x] **T4.1 Interfaz `AiProvider`.** Con `explain(text)`. *Hecho cuando:* existe y hay una implementación falsa para pruebas.
- [ ] **T4.2 Guardado seguro de la clave.** Con Android Keystore. *Hecho cuando:* la clave se guarda y se lee, y no aparece en logs.
- [ ] **T4.3 Pantalla de configuración de IA.** Campo para la clave, botón "Probar clave", botón "Borrar clave" y una guía corta de cómo obtenerla. *Hecho cuando:* se puede guardar y borrar la clave.
- [ ] **T4.4 Primer proveedor real.** Implementar un proveedor con capa gratuita (elegir uno; ver "Pendientes"). *Hecho cuando:* "Probar clave" devuelve una respuesta real.
- [ ] **T4.5 Manejo de errores.** Mensajes claros para clave inválida, cuota agotada, límite de peticiones y sin internet. *Hecho cuando:* cada caso muestra un mensaje distinto.

### Fase 5 — Cámara y OCR
- [ ] **T5.1 Permiso de cámara.** Solicitud en tiempo de ejecución con explicación. *Hecho cuando:* se maneja aceptar y rechazar.
- [ ] **T5.2 Vista de cámara.** Vista previa con CameraX y botón de captura. *Hecho cuando:* se toma una foto.
- [ ] **T5.3 Elegir imagen de galería.** Alternativa a la cámara. *Hecho cuando:* se puede seleccionar una imagen.
- [ ] **T5.4 OCR.** Envoltorio de ML Kit que recibe una imagen y devuelve texto. *Hecho cuando:* extrae texto de una página de prueba.
- [ ] **T5.5 Pantalla de texto extraído.** Texto editable antes de enviarlo, con límite de longitud. *Hecho cuando:* el usuario puede corregir el texto.

### Fase 6 — Explicador Feynman
- [ ] **T6.1 Prompt del explicador.** Guardado en `ai/` (ver Anexo A). *Hecho cuando:* está definido y probado con 3 textos.
- [ ] **T6.2 Caso de uso `ExplainText`.** Conecta el proveedor con el prompt. *Hecho cuando:* devuelve una explicación para un texto de prueba.
- [ ] **T6.3 Pantalla de explicación.** Muestra el texto original y la explicación, con estado de carga. *Hecho cuando:* se ve la respuesta de la IA.
- [ ] **T6.4 Guardar como nota.** Botón que crea una `Note` de tipo EXPLICACION con el texto fuente. *Hecho cuando:* la nota aparece en el libro.
- [ ] **T6.5 Flujo completo.** Desde el detalle del libro: cámara → OCR → editar → explicar → guardar. *Hecho cuando:* todo el flujo funciona de extremo a extremo.

### Fase 7 — Cierre del MVP
- [ ] **T7.1 Pantalla de privacidad.** Explica que los fragmentos de texto se envían al proveedor de IA elegido por el usuario. *Hecho cuando:* es accesible desde configuración.
- [ ] **T7.2 Pulido de UX.** Estados de carga, vacíos y errores en todas las pantallas. *Hecho cuando:* no hay pantallas sin manejo de estado.
- [ ] **T7.3 Ícono y nombre de la app.** *Hecho cuando:* se ven en el lanzador.
- [ ] **T7.4 Build de release.** Firmar y generar el APK. *Hecho cuando:* el APK se instala y funciona en un teléfono real.
- [ ] **T7.5 README.** Cómo compilar, cómo obtener una clave de API y cómo instalar el APK. *Hecho cuando:* otra persona puede seguirlo.

---

## 9. Después del MVP (backlog, no iniciar aún)

- **B1** Importar EPUB/PDF (privado por usuario, almacenamiento interno).
- **B2** Lector con selección de texto y explicación directa (Readium para EPUB, PdfRenderer para PDF).
- **B3** Notas de voz con transcripción.
- **B4** Fichas de repaso generadas desde notas.
- **B5** Repetición espaciada básica.
- **B6** Evaluación de comprensión: el usuario escribe su interpretación y la IA da retroalimentación.
- **B7** Quizzes tipo test por capítulo (solo si el usuario lo pide).
- **B8** Respaldo: exportar e importar los datos a un archivo.
- **B9** Modo con cupo gratuito: función intermediaria serverless, Play Integrity, tope diario global y límite por dispositivo.
- **B10** Metas de lectura y estadísticas.

---

## 10. Riesgos técnicos (en orden)

1. Lector de EPUB/PDF con selección de texto (por eso queda aislado en B1 y B2).
2. Calidad del OCR con fotos reales (poca luz, páginas curvas): probar temprano, en T5.4.
3. Costo, límites y latencia de la IA.

---

## 11. Pendientes por decidir

- Nombre definitivo de la app (provisional: `Reader`).
- Primer proveedor de IA (criterio: facilidad de obtener una clave gratuita y calidad de las explicaciones). Se recomienda probar 20 párrafos reales antes de fijarlo.
- Idioma de las explicaciones (por defecto, el idioma del texto o español).

---

## 12. Convenciones del repositorio

- **Repositorio:** público en GitHub, `Fabiann1809/reader`, rama principal `main`.
- **Autoría:** todos los commits van solo a nombre de `Fabiann1809 <leiderfabian538@gmail.com>`. Sin `Co-Authored-By` ni firmas de herramientas de IA.
- **Commits:** uno por tarea o fix, con [Conventional Commits](https://www.conventionalcommits.org/) en inglés: `type(scope): imperative summary (Txx)`.
  - Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`, `style`.
  - Scopes: `data`, `library`, `notes`, `ai`, `ocr`, `camera`, `ui`, `build`.
  - Resumen de 72 caracteres como máximo, en imperativo y sin punto final.
  - La casilla `[x]` de la tarea se marca en este documento en el mismo commit.
- **Idioma:** código, identificadores, comentarios, commits y documentación técnica en **inglés**. El texto que ve el usuario va en **español**, siempre en `res/values/strings.xml` (nunca escrito directamente en Compose).
- **Arquitectura:** MVVM. Los composables no acceden a repositorios; los ViewModels exponen `StateFlow<UiState>`; toda llamada a IA pasa por `AiProvider`. Inyección de dependencias manual con un `AppContainer`.
- **Dependencias:** todas las versiones en `gradle/libs.versions.toml`.
- **Seguridad:** ninguna clave en el código, el repositorio, los logs ni los mensajes de excepción. Keystore de release y `local.properties` fuera del repo.
- **Definición de hecho:** compila (`./gradlew assembleDebug`), los tests relevantes pasan, se cumple el "Hecho cuando" y se explica brevemente qué se hizo y por qué.
- Las reglas operativas completas están en `CLAUDE.md`.

---

## Anexo A — Borrador del prompt del explicador Feynman

```
Eres un tutor que explica textos difíciles con el método Feynman.
Recibirás un párrafo de un libro. Responde en el mismo idioma del párrafo, así:

1. Idea central: una sola frase con lo esencial.
2. Explicación sencilla: como si hablaras con alguien sin conocimientos previos, sin jerga.
3. Analogía cotidiana: una comparación con algo de la vida diaria.
4. Términos clave: define en una línea cada término difícil del texto.

Reglas: no inventes información que no esté en el texto; si el fragmento es ambiguo o
está incompleto, dilo. Sé breve.
```

## Anexo B — Notas sobre derechos de autor y privacidad

- Los libros pertenecen al usuario y permanecen en su dispositivo. La app no los comparte ni los sube a ningún servidor propio.
- A la IA se envían únicamente los fragmentos de texto necesarios (un párrafo o una página), nunca el libro completo.
- La clave de API es del usuario, se guarda cifrada y se puede borrar en cualquier momento.
