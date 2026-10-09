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
- Desde la versión 2 (T8.2): `kind` (DIGITAL / FISICO; los libros anteriores son físicos), `format` (EPUB / PDF / TXT / CBZ, opcional), `filePath`, `coverPath`, `language`, `lastOpenedAt` (todos opcionales)
- Desde la versión 3 (T9.1): `isFavorite` (Boolean).

**Collection** (versión 3, T9.1) — colecciones creadas por el usuario
- `id` (PK), `name`, `createdAt`; relación muchos a muchos con Book en `book_collections` (`bookId`, `collectionId`, `addedAt`; borrado en cascada por ambos lados).
- Las colecciones por defecto (Todos, Mis favoritos, Leyendo ahora, Terminados, Quiero leer) no se guardan: se calculan de `isFavorite` y `status`.

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

### Fuera del MVP (se implementa en las fases 8 a 18, ver sección 9)
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
3. Haz un **commit por tarea o fix** siguiendo Conventional Commits en inglés, sin el ID de la tarea (por ejemplo `feat(library): add book list screen`). Ver sección 12.
4. **Nunca** escribas claves de API en el código, en el repositorio ni en los logs.
5. Trabaja las fases en orden (MVP en la sección 8, después la sección 9) y no agregues funciones que no estén en el spec ni en el diseño. Si algo es ambiguo, **pregunta** antes de asumir.
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
- [x] **T4.2 Guardado seguro de la clave.** Con Android Keystore. *Hecho cuando:* la clave se guarda y se lee, y no aparece en logs.
- [x] **T4.3 Pantalla de configuración de IA.** Campo para la clave, botón "Probar clave", botón "Borrar clave" y una guía corta de cómo obtenerla. *Hecho cuando:* se puede guardar y borrar la clave.
- [x] **T4.4 Primer proveedor real.** Implementar un proveedor con capa gratuita (elegir uno; ver "Pendientes"). *Hecho cuando:* "Probar clave" devuelve una respuesta real.
- [x] **T4.5 Manejo de errores.** Mensajes claros para clave inválida, cuota agotada, límite de peticiones y sin internet. *Hecho cuando:* cada caso muestra un mensaje distinto.

### Fase 5 — Cámara y OCR
- [x] **T5.1 Permiso de cámara.** Solicitud en tiempo de ejecución con explicación. *Hecho cuando:* se maneja aceptar y rechazar.
- [x] **T5.2 Vista de cámara.** Vista previa con CameraX y botón de captura. *Hecho cuando:* se toma una foto.
- [x] **T5.3 Elegir imagen de galería.** Alternativa a la cámara. *Hecho cuando:* se puede seleccionar una imagen.
- [x] **T5.4 OCR.** Envoltorio de ML Kit que recibe una imagen y devuelve texto. *Hecho cuando:* extrae texto de una página de prueba.
- [x] **T5.5 Pantalla de texto extraído.** Texto editable antes de enviarlo, con límite de longitud. *Hecho cuando:* el usuario puede corregir el texto.

### Fase 6 — Explicador Feynman
- [x] **T6.1 Prompt del explicador.** Guardado en `ai/` (ver Anexo A). *Hecho cuando:* está definido y probado con 3 textos.
- [x] **T6.2 Caso de uso `ExplainText`.** Conecta el proveedor con el prompt. *Hecho cuando:* devuelve una explicación para un texto de prueba.
- [x] **T6.3 Pantalla de explicación.** Muestra el texto original y la explicación, con estado de carga. *Hecho cuando:* se ve la respuesta de la IA.
- [x] **T6.4 Guardar como nota.** Botón que crea una `Note` de tipo EXPLICACION con el texto fuente. *Hecho cuando:* la nota aparece en el libro.
- [x] **T6.5 Flujo completo.** Desde el detalle del libro: cámara → OCR → editar → explicar → guardar. *Hecho cuando:* todo el flujo funciona de extremo a extremo.

### Fase 7 — Cierre del MVP
- [x] **T7.1 Pantalla de privacidad.** Explica que los fragmentos de texto se envían al proveedor de IA elegido por el usuario. *Hecho cuando:* es accesible desde configuración.
- [x] **T7.2 Pulido de UX.** Aplicar el diseño visual del usuario (paquete "Reader: Interfaz móvil con IA": `04-system-design.md` y láminas) a las pantallas del MVP, con estados de carga, vacíos y errores en todas. Las vistas post-MVP del diseño (lector, fichas, quiz, progreso, voz, barra inferior) se harán con sus funciones. Se divide en:
  - [x] **T7.2a Tema.** Colores claro/oscuro, tipografía (Plus Jakarta Sans, Fraunces) y formas según los tokens del diseño. *Hecho cuando:* toda la app usa los tokens en modo claro y oscuro.
  - [x] **T7.2b Iconos.** Reemplazar los iconos por Phosphor (trazo redondeado) como vectores. *Hecho cuando:* no quedan iconos de otra familia.
  - [x] **T7.2c Biblioteca con estantes.** Pared de madera, estantes, portadas generadas con progreso, botón flotante y estados vacío/cargando. *Hecho cuando:* los libros se ven de pie en estantes.
  - [x] **T7.2d Detalle, libro y notas.** Detalle con portada y progreso, agregar libro, lista y editor de notas con el nuevo estilo. *Hecho cuando:* las pantallas siguen el diseño.
  - [x] **T7.2e Captura y texto reconocido.** Cámara con marco guía y botón de disparo, pantalla de texto reconocido y error de OCR. *Hecho cuando:* siguen el diseño.
  - [x] **T7.2f Respuesta estructurada de la IA.** La IA devuelve los bloques del explicador (idea central, explicación sencilla, analogía, términos clave) en formato estructurado. *Hecho cuando:* se prueba con textos reales y se evalúa su comportamiento.
  - [x] **T7.2g Explicador por bloques.** Bloques del diseño, etiqueta "Generado con IA", texto original colapsable, estados de carga y sin conexión. *Hecho cuando:* la explicación se ve por bloques.
  - [x] **T7.2h Ajustes, privacidad y revisión de estados.** *Hecho cuando:* no hay pantallas sin manejo de estado.
- [x] **T7.3 Ícono y nombre de la app.** *Hecho cuando:* se ven en el lanzador.
- [x] **T7.4 Build de release.** Firmar y generar el APK. *Hecho cuando:* el APK se instala y funciona en un teléfono real.
- [x] **T7.5 README.** Cómo compilar, cómo obtener una clave de API y cómo instalar el APK. *Hecho cuando:* otra persona puede seguirlo.

---

## 9. Después del MVP: todo lo que lleva la app

Fuentes: backlog original B1–B10 y el paquete de diseño del usuario ("Reader: Interfaz móvil con IA", `04-system-design.md` y láminas). El cierre de versión (T7.3 a T7.5) se hace cuando todo esto esté listo.

**Reglas técnicas para todas las fases:**
- Cada cambio de esquema de Room lleva su migración y su prueba (`MigrationTestHelper`); nunca migración destructiva.
- Toda operación de IA nueva pasa por `AiProvider` con salida estructurada (JSON con esquema), como el explicador.
- Se reutilizan los componentes de `ui/components` y los tokens del tema; cada pantalla tiene estados vacío, cargando y error, en claro y oscuro.
- Todo sigue local y sin cuentas hasta la fase 18.

### Fase 8 — Bases para crecer
- [x] **T8.1 Migraciones.** Configurar migraciones de Room y `MigrationTestHelper`. *Hecho cuando:* una migración de prueba 1→2 pasa su test sin perder datos.
- [x] **T8.2 Libro ampliado.** Agregar a `Book`: `kind` (DIGITAL/FISICO), `format`, `filePath`, `coverPath`, `language`, `lastOpenedAt`. *Hecho cuando:* los libros existentes migran y se ven igual.
- [x] **T8.3 Barra inferior.** Cuatro destinos (Biblioteca, Repasar, Progreso, Más) con pill activa, cambio de pestaña con fundido y "Atrás" según el diseño (3 §6). Repasar y Progreso empiezan con su estado vacío. *Hecho cuando:* se navega entre pestañas y "Atrás" vuelve a Biblioteca.
- [x] **T8.4 Pantalla Más.** Entradas a Todas las notas, Respaldo, Ajustes, Privacidad y Acerca de. *Hecho cuando:* cada entrada abre su pantalla o su estado vacío.
- [x] **T8.5 Onboarding.** Tres pasos saltables; se muestra solo la primera vez (DataStore). *Hecho cuando:* aparece en la primera apertura y no vuelve a aparecer.

### Fase 9 — Biblioteca completa
- [x] **T9.1 Colecciones.** Entidades `Collection` + `BookCollectionCrossRef`. Colecciones por defecto: Todos, Mis favoritos, Leyendo ahora, Terminados y Quiero leer (estas tres últimas derivadas del estado). *Hecho cuando:* se crean y se listan.
- [x] **T9.2 Selector de colección.** Título de la barra con ▾; cambiar de colección filtra los estantes, y una colección vacía muestra "Nada aquí todavía". *Hecho cuando:* se cambia de colección.
- [x] **T9.3 Añadir a colección.** Desde el detalle del libro. *Hecho cuando:* el libro aparece en la colección.
- [x] **T9.4 Búsqueda.** Por título y autor. *Hecho cuando:* filtra mientras se escribe.
- [x] **T9.5 Ordenar y filtrar.** Hoja inferior: ordenar por último leído, título, autor, fecha o progreso; filtrar por estado, formato y digital/físico. *Hecho cuando:* los estantes respetan la selección y esta persiste.
- [x] **T9.6 Vistas.** Estantes, Cuadrícula y Lista; libros por estante de 2 a 4. *Hecho cuando:* la vista elegida persiste.
- [x] **T9.7 Menú de portada.** Mantener pulsado: Abrir, Detalle, Colección, Marcar como leído, Eliminar. Insignias "Nuevo" y "Físico". *Hecho cuando:* cada opción funciona.
- [x] **T9.8 Selección múltiple.** Marcar varios libros (check y atenuado) para mover a colección o eliminar. *Hecho cuando:* se aplica a todos los marcados.
- [x] **T9.9 Movimiento.** Los libros aparecen con una leve subida escalonada; se respeta "reducir animaciones". *Hecho cuando:* se ve la animación y se desactiva con el ajuste del sistema.

### Fase 10 — Importar libros (B1)
- [x] **T10.1 Importar archivo.** Selector del sistema (SAF; incluye Drive y Dropbox si están instalados), copia a almacenamiento interno y lectura de metadatos (título y autor) con Readium para EPUB y PDF. *Hecho cuando:* un EPUB y un PDF aparecen en el estante con sus datos.
- [x] **T10.2 Portadas reales.** Portada del EPUB y primera página del PDF (PdfRenderer) guardadas en el almacenamiento interno; `BookCover` usa la imagen si existe. *Hecho cuando:* se ven las portadas.
- [x] **T10.3 Estados de importación.** "Importando 3 de 7…", error con Reintentar o Descartar (lámina 1f). *Hecho cuando:* se ven al importar varios archivos y uno dañado.
- [ ] **T10.4 TXT y CBZ.** Formatos simples que Readium o el propio código soportan. *Hecho cuando:* se abren.
- [x] **T10.5 Compartir hacia Reader.** Intent filter para recibir archivos desde otras apps. *Hecho cuando:* "Compartir → Reader" importa el libro.
- [x] **T10.6 Carpeta vigilada.** Carpeta elegida con permiso persistente, revisada con WorkManager. *Hecho cuando:* un archivo nuevo en la carpeta aparece en la biblioteca.
- [ ] **T10.7 Libro físico con portada e ISBN.** Foto de portada y escaneo de ISBN (ML Kit Barcode + Open Library, gratis), con edición manual. *Hecho cuando:* el ISBN autocompleta título y autor.

### Fase 11 — Lector (B2)
- [x] **T11.1 Prueba técnica de Readium.** Navegador de EPUB dentro de Compose, embebido como Fragment en Compose (los navegadores de Readium están hechos con Fragments). *Hecho cuando:* un EPUB se pagina en pantalla completa.
- [x] **T11.2 Abrir y continuar.** Guardar la posición (`Locator`) y `lastOpenedAt`; "Continuar leyendo" en el botón flotante y en el detalle. *Hecho cuando:* al reabrir, el libro vuelve a la misma página.
- [x] **T11.3 PDF.** Visualización de PDF con PdfRenderer o el adaptador de Readium, con posición guardada. *Hecho cuando:* un PDF se lee y recuerda la página.
- [x] **T11.4 Overlay de controles.** Toque al centro: barra superior (atrás, capítulo, marcador, menú) e inferior (barra de progreso con capítulo, Índice, Aa, Voz, IA, Grabar). *Hecho cuando:* aparece y desaparece en 180 ms.
- [x] **T11.5 Zonas de toque y gestos.** Izquierda, centro y derecha; deslizar; pellizcar para el tamaño; borde izquierdo para el brillo. El tamaño y el brillo valen mientras se lee (se pierden al cerrar el libro; "Aa" los guardará). Sin doble toque: el cambio día/noche está en "Aa" (T11.7). *Hecho cuando:* cada gesto funciona.
- [x] **T11.6 Índice y marcadores.** Entidad `Bookmark`. *Hecho cuando:* se salta a un capítulo y a un marcador.
- [x] **T11.7 Ajustes "Aa".** Temas Día, Sepia, Gris papel, Noche, AMOLED y Personalizado (fondo y texto elegidos de una paleta); fuentes (Literata, Merriweather, Source Serif, Lora, Atkinson, OpenDyslexic, Inter; todas OFL); tamaño, interlineado, márgenes y alineación; independientes del tema de la app; el cambio rápido Día/Noche vive aquí. *Hecho cuando:* los cambios se ven al instante y persisten.
- [x] **T11.8 Efecto de página.** Deslizar, desplazamiento continuo o ninguno, en EPUB (sin "desvanecer": Readium no lo trae y habría que modificar su navegador). *Hecho cuando:* se elige y se aplica.
- [x] **T11.9 Indicadores discretos.** Hora, batería y página/total; mantener la pantalla encendida. *Hecho cuando:* son activables.
- [x] **T11.10 Selección de texto.** Barra contextual: Explicar, Resaltar, Nota, Voz, Ficha y Más (copiar, buscar, compartir). *Hecho cuando:* aparece al seleccionar en un EPUB.
- [x] **T11.11 Explicador como hoja inferior.** Reutiliza `ExplainText` y los bloques de `ui/explanation`; al cerrar vuelve al punto de lectura. *Hecho cuando:* se explica una selección sin salir del lector.
- [x] **T11.12 Resaltados.** Entidad `Highlight` en 4 colores (decoraciones de Readium), visibles en el detalle. *Hecho cuando:* persisten al reabrir.
- [x] **T11.13 Notas ancladas.** `Note` guarda la posición (locator) y se puede saltar desde la nota al texto. *Hecho cuando:* tocar la nota abre el lector en ese punto.
- [x] **T11.14 Explicar en PDF.** El visor de PDF no permite seleccionar texto: el botón "IA" permite marcar un párrafo con el dedo, se recorta esa zona y se lee con el `TextRecognizer` existente. *Hecho cuando:* se explica un párrafo de un PDF.
- [ ] **T11.15 Lectura en voz alta.** TextToSpeech de Android o TTS de Readium: reproducir/pausar, velocidad, voz, temporizador y resaltado de la frase. *Hecho cuando:* lee el capítulo resaltando. **Pospuesta** (decisión del usuario, 2026-10-02): se retoma más adelante.
- [ ] **T11.16 Portada → lector.** Transición de elemento compartido. *Hecho cuando:* la portada se expande a la página. **Al final del proyecto** (decisión del usuario, 2026-10-02): es la de más riesgo técnico.

### Fase 12 — Explicador y captura ampliados
- [ ] **T12.1 Más simple y otro ejemplo.** Operaciones nuevas en `AiProvider` con el contexto de la explicación anterior. *Hecho cuando:* cada botón devuelve una versión nueva. **Pospuesta** (decisión del usuario, 2026-10-02): se decide tras usar el explicador un tiempo.
- [ ] **T12.2 Nivel e idioma.** Simple, Intermedio o Técnico, e idioma de las explicaciones, en Ajustes; el prompt lo usa. *Hecho cuando:* cambia el tono. **Descartada** (decisión del usuario, 2026-10-02): el prompt del sistema ya fija el tono y no aporta lo suficiente.
- [ ] **T12.3 Guardar para después.** Sin conexión: cola `PendingExplanation` y WorkManager con restricción de red, más una notificación cuando la explicación está lista. *Hecho cuando:* la explicación llega sola al reconectar. **Pospuesta** (decisión del usuario, 2026-10-02): por ahora basta el mensaje de error de conexión que ya existe.
- [x] **T12.4 OCR dudoso.** Usar la confianza de ML Kit para mostrar el aviso "No estoy seguro de haber leído bien" y marcar las líneas dudosas. *Hecho cuando:* una foto borrosa muestra el aviso.
- [x] **T12.5 Recorte y párrafo.** Esquinas editables y "Seleccionar párrafo" (bloques de ML Kit). *Hecho cuando:* se explica solo el párrafo elegido.
- [ ] **T12.6 Asociar captura.** Selector de libro (o "sin libro", lo que requiere migrar `Note.bookId` a nullable) y número de página. *Hecho cuando:* la nota queda en el libro y página elegidos. **Pospuesta** (decisión del usuario, 2026-10-02): hoy la captura solo se abre desde un libro, así que la nota ya queda en él. Al retomarla no habrá "sin libro": en una app de lectura toda nota va asociada a un libro.
- [ ] **T12.7 Citas.** Tipo de nota CITA ("Guardar como cita"). *Hecho cuando:* se guarda y se distingue en la lista. **Descartada por ahora** (decisión del usuario, 2026-10-02): una frase para guardar se anota con "Crear ficha".

### Fase 13 — Notas de voz (B3)
- [x] **T13.1 Permiso y grabación.** Pantalla explicativa del micrófono y grabación con MediaRecorder en almacenamiento interno. *Hecho cuando:* se graba y se reproduce.
- [x] **T13.2 Transcripción.** Archivo de audio enviado a Gemini con `AiProvider.transcribe` (ver decisión 2). *Hecho cuando:* la nota muestra el texto editable.
- [x] **T13.3 Interfaz de grabación.** Hoja con onda, temporizador y etiquetas Idea, Duda, Cita y Tarea; se asocia al libro y la posición; snackbar con Deshacer. *Hecho cuando:* coincide con la lámina 1i.
- [ ] **T13.4 Grabar sin mirar.** Notificación persistente "Grabar/Detener" con un servicio en primer plano de tipo micrófono. *Hecho cuando:* graba con la pantalla apagada. **Pospuesta** (decisión del usuario, 2026-10-02).
- [x] **T13.5 Nota de voz en listas.** Reproductor y transcripción en el detalle y en Todas las notas. *Hecho cuando:* se reproduce desde la lista.

### Fase 14 — Fichas y repaso (B4, B5)
- [x] **T14.1 Entidad Flashcard.** Frente, reverso, fuente (libro y página), próxima revisión, facilidad e intervalo. *Hecho cuando:* compila con su migración.
- [x] **T14.2 Crear ficha.** Manual, y desde nota, explicación, cita o selección; la IA propone frente y reverso (`AiProvider.makeFlashcard`). *Hecho cuando:* se crea desde cada origen.
- [x] **T14.3 Pestaña Repasar.** Hoy, Por libro y Por etiqueta; punto de aviso cuando hay pendientes; estado vacío "Sin fichas por repasar". *Hecho cuando:* muestra las fichas pendientes.
- [x] **T14.4 Sesión de repaso.** Volteo 3D y 4 botones con el intervalo debajo; algoritmo SM-2 simplificado con pruebas unitarias. *Hecho cuando:* las fechas de repaso se recalculan bien.
- [x] **T14.5 Resumen de sesión.** Repasadas, aciertos y próxima revisión, con "Ponme a prueba" opcional. *Hecho cuando:* aparece al terminar. "Ponme a prueba" se agrega con el quiz (T15.3), para no mostrar un botón que aún no hace nada.
- [ ] **T14.6 Recordatorio diario.** Notificación de repaso con WorkManager y permiso de notificaciones. *Hecho cuando:* llega a la hora elegida. **Pospuesta** (decisión del usuario, 2026-10-02): por ahora no se implementa el recordatorio.

### Fase 15 — Comprensión y quiz (B6, B7)
- [x] **T15.1 Ahora tú.** El usuario escribe su interpretación y `AiProvider.analyzeInterpretation` devuelve qué está bien, incompleto o confuso, con referencia al texto. *Hecho cuando:* coincide con la lámina 1i. La referencia al texto es una cita del párrafo explicado (no hay número de página que enlazar); "Ponme a prueba" llega con el quiz (T15.3).
- [x] **T15.2 Generar quiz.** `AiProvider.generateQuiz` con 3, 5 o 10 preguntas de 4 opciones, en JSON con esquema; el contexto es el capítulo (lector) o las notas y capturas (libro físico). *Hecho cuando:* devuelve un quiz válido.
- [x] **T15.3 Pantalla de quiz.** Opción elegida, correcta o incorrecta, explicación, enlace a la página y salir con diálogo. *Hecho cuando:* coincide con la lámina 1h. Se abre con "Ponme a prueba" desde el resumen de repaso (sobre las fichas repasadas), desde "Ahora tú" (3 preguntas sobre el párrafo) y desde el menú del lector (el capítulo, 3, 5 o 10 preguntas; solo EPUB), según decidió el usuario (2026-10-02). Sin enlace "Ver en la página": las preguntas no traen página.
- [x] **T15.4 Resultado.** Puntaje, temas fuertes y débiles, y "Crear fichas de lo que falló". *Hecho cuando:* crea las fichas. Los temas salen de un campo "tema" que la IA pone a cada pregunta; las fichas van al libro del quiz (en el repaso, el libro con más fichas de la sesión).
- [x] **T15.5 Sugerencia al terminar un capítulo.** Discreta y descartable, nunca modal. *Hecho cuando:* aparece una vez por capítulo. Cuenta como terminado pasar al capítulo siguiente del índice (solo EPUB); la tarjeta ofrece «Repasar» y «Ponme a prueba» (3 preguntas) y se oculta sola.

### Fase 16 — Progreso y metas (B10)
- [x] **T16.1 Sesiones de lectura.** Entidad `ReadingSession`, registrada sola desde el lector; en libros físicos, al actualizar el progreso. *Hecho cuando:* se registran las sesiones.
- [x] **T16.2 Pestaña Progreso.** Anillo de meta diaria, racha, gráfica semanal y libros terminados del año. *Hecho cuando:* coincide con la lámina 1i.
- [x] **T16.3 Meta y recordatorio.** Meta diaria en minutos o páginas, con recordatorio de lectura. *Hecho cuando:* el anillo usa la meta. Solo la meta (se cambia tocando el anillo); el recordatorio de lectura queda **pospuesto** junto con T14.6, para hacer ambos con WorkManager más adelante (decisión del usuario, 2026-10-03).
- [x] **T16.4 Detalle con pestañas.** Resumen, Notas y resaltados, Fichas y Sesiones; páginas restantes y tiempo estimado para terminar. *Hecho cuando:* cada pestaña muestra sus datos.
- [x] **T16.5 Racha animada.** La llama se anima una vez al cumplir la meta. *Hecho cuando:* se anima solo al cumplir.
- [x] **T16.6 Progreso de libros digitales.** Calcular porcentaje y página/total desde la posición guardada (`Locator`) y mostrarlos en el detalle, la lista y la portada, en lugar de "Página 0". *Hecho cuando:* un EPUB a medio leer muestra su progreso real. La página es la posición de Readium (unas mil letras) y el total, las posiciones del libro; se guardan al leer, así que un libro abierto antes de este cambio se actualiza la próxima vez que se lee.

### Fase 17 — Ajustes, respaldo y accesibilidad (B8)
- [x] **T17.1 Ajustes completos.** Apariencia (tema claro, oscuro o del sistema; vista por defecto; libros por estante), lectura por defecto, voz, IA (activar o desactivar funciones) y notificaciones. *Hecho cuando:* cada ajuste persiste y se aplica. Hecho solo con lo que ya existe (decisión del usuario, 2026-10-03): apariencia, lectura por defecto (la hoja «Aa») e IA (transcripción automática y sugerencia al terminar un capítulo; explicar y los quiz ya son a pedido). Voz y notificaciones se agregan cuando se retomen T11.15, T14.6 y el recordatorio de T16.3.
- [x] **T17.2 Exportar respaldo.** ZIP con los datos en JSON, archivos de libros y audios, guardado con SAF. Nunca incluye la clave de API. *Hecho cuando:* se genera el archivo.
- [x] **T17.3 Importar respaldo.** Con confirmación y validación de versión. *Hecho cuando:* restaura en una instalación limpia. Reemplaza toda la biblioteca tras confirmar (decisión del usuario, 2026-10-03); verificado con una prueba que restaura en una base y una carpeta vacías, sin desinstalar la app para no borrar la clave.
- [x] **T17.4 Todas las notas.** Lista global con búsqueda y filtro por tipo. *Hecho cuando:* encuentra una nota por texto.
- [ ] **T17.6 Widget y accesos directos.** Widget con Glance (Continuar o Grabar nota) y accesos directos del ícono (Continuar, Captura, Nota de voz). *Hecho cuando:* abren el destino correcto. **Pospuesta** (decisión del usuario, 2026-10-02).

### Fase 18 — Nube (requiere decisiones; ver riesgos)
> **Al final del proyecto** (decisión del usuario, 2026-10-02): nada de esta fase se hace hasta terminar las demás.
- [ ] **T18.1 Sincronización con Google Drive.** Carpeta de datos de la app (appDataFolder) con inicio de sesión de Google, estados (Sincronizado, Pendiente, Error con Reintentar) y resolución de conflictos. Cambia la decisión "sin cuentas" del spec. *Hecho cuando:* dos dispositivos ven los mismos datos.
- [ ] **T18.2 Dropbox.** Mismo contrato que T18.1. *Hecho cuando:* sincroniza.

### Fase 19 — Rediseño v2
> Decisión del usuario (2026-10-08): por ahora solo el rediseño, antes que las tareas pospuestas. Fuente: paquete "Reader Rediseño" (`Reader Rediseño.dc.html` = prototipo interactivo con 23 pantallas, y `uploads/reader-redesign-brief.md`). Solo cambia el aspecto: las funciones y los datos no cambian ni se agrega nada que no esté en el prototipo. Cada tarea cubre claro y oscuro, estados vacío, cargando y error, y "reducir animaciones".

**Base visual**
- [x] **T19.1 Colores.** Paleta clara y oscura del prototipo (fondo, superficies, tinta, acento terracota, morado de IA, estados ok/aviso/error, pared y estante, barra inferior). *Hecho cuando:* los tokens del tema coinciden con el prototipo en ambos modos.
- [x] **T19.2 Tipografía.** Manrope (interfaz), DM Serif Display (títulos) y Literata (lectura) en lugar de Plus Jakarta Sans y Fraunces. *Hecho cuando:* ninguna pantalla usa las fuentes anteriores.
- [x] **T19.3 Formas y movimiento.** Radios, sombras y animaciones del prototipo (entrada escalonada, pop, fundidos), todas atenuadas con "reducir animaciones". *Hecho cuando:* existen como tokens reutilizables.
- [x] **T19.4 Componentes base.** Botones, chips, campos de texto, tarjetas, hojas inferiores y bloques de estado (vacío, cargando, error) con el nuevo estilo. *Hecho cuando:* `ui/components` usa solo los tokens nuevos.
- [x] **T19.5a Barra inferior.** Barra flotante redondeada con pill que se desliza a la pestaña activa y punto de pendientes. *Hecho cuando:* coincide con el prototipo.
- [x] **T19.5b Pantalla Más.** Título, lista en tarjeta con iconos circulares y subtítulos, tarjeta de clave de Gemini y conteo de notas (decisión del usuario 2026-10-08: sin interruptor de modo oscuro). *Hecho cuando:* coincide con el prototipo.
- [x] **T19.6 Onboarding.** Los 3 pasos saltables con las ilustraciones del prototipo. *Hecho cuando:* se ve igual solo la primera vez.

**Biblioteca**
- [x] **T19.7 Cabecera de biblioteca.** Título con selector de colección y conteo de libros, botones de búsqueda y filtros, y chips de colección inteligente con conteos (decisión del usuario 2026-10-08). *Hecho cuando:* coincide con el prototipo.
- [x] **T19.8 Sigues leyendo y orden.** Tarjeta "Sigues leyendo", etiqueta de orden y selector de vista (estantes, cuadrícula, lista). *Hecho cuando:* la vista elegida persiste.
- [x] **T19.9 Vista de estantes.** Pared, estantes y portadas con progreso, insignias "Nuevo" y "Físico", entrada escalonada y botón flotante. *Hecho cuando:* los libros se ven como en el prototipo.
- [x] **T19.10a Lista.** Tarjetas por libro con portada, autor y formato, barra de progreso y menú.
- [x] **T19.10b Vista Colecciones.** Reemplaza a la cuadrícula (decisión del usuario 2026-10-08): por colección, pila de portadas en abanico bajo una banda pastel, nombre y conteo; botones ＋ (añadir libros a la colección, función nueva) y ⋯ (renombrar y eliminar) solo en las colecciones propias, y "Nueva colección" arriba. Inteligentes y propias. *Hecho cuando:* coincide con el prototipo y tocar una colección la abre.
- [x] **T19.11 Búsqueda y filtros.** Campo de búsqueda con contador y hoja de ordenar y filtrar. *Hecho cuando:* coinciden con el prototipo.
- [x] **T19.12a Selector de colecciones y selección.** Hoja de colecciones con punto de color, conteo y marca; barra de selección clara con botones circulares. *Hecho cuando:* coinciden con el prototipo.
- [x] **T19.12b Color de colección.** Cada colección guarda su color (uno de los cuatro pasteles; migración de base de datos con prueba y respaldo compatible); las existentes conservan el que tenían. *Hecho cuando:* la banda y el punto usan el color guardado.
- [x] **T19.12c Pantalla Nueva colección.** Pantalla completa con vista previa en abanico, nombre, color y elección de libros (decisión del usuario 2026-10-08). *Hecho cuando:* coincide con el prototipo y crea la colección con sus libros y su color.
- [x] **T19.13 Importación.** Estados "Importando 3 de 7…" y error con Reintentar/Descartar. *Hecho cuando:* coinciden con el prototipo.

**Libro**
- [x] **T19.14 Agregar libro.** Formulario de libro físico. *Hecho cuando:* coincide con el prototipo.
- [x] **T19.15 Detalle del libro.** Portada, progreso, estado, pestañas Resumen, Notas, Fichas y Sesiones. *Hecho cuando:* coincide con el prototipo.

**Lector**
- [x] **T19.16a Lector: barras.** Barra superior (atrás, capítulo y libro, lupa, marcador, menú) e inferior con los colores del tema de lectura, progreso con posición actual y total, y acciones Índice, Aa, IA (ficha morada), Notas y Grabar; se quita el botón muerto Voz (decisión del usuario 2026-10-08). *Hecho cuando:* coinciden con el prototipo.
- [x] **T19.16b Lector: hoja de notas.** "Notas" abre una hoja con las notas y resaltados del libro (función nueva, decisión del usuario 2026-10-08). *Hecho cuando:* se listan y llevan a su pasaje.
- [x] **T19.16c Lector: barra de selección.** Barra contextual al seleccionar texto. *Hecho cuando:* coincide con el prototipo.
- [x] **T19.17a Lector: ajustes de lectura.** Hoja "Aa". *Hecho cuando:* coincide con el prototipo.
- [x] **T19.17b Lector: índice y marcadores.** *Hecho cuando:* coinciden con el prototipo.
- [x] **T19.17c Lector: buscar en el libro.** Pantalla de búsqueda con resultados que llevan al pasaje (función nueva, decisión del usuario 2026-10-08). *Hecho cuando:* encuentra texto en un EPUB y abre el lector en ese punto.

**IA y captura**
- [x] **T19.18 Explicador.** Bloques, etiqueta "Generado con IA", texto original colapsable y estados; acciones fijas abajo con "Ponme a prueba" agregado (decisión del usuario 2026-10-08). *Hecho cuando:* coincide con el prototipo.
- [x] **T19.19 Cámara y recorte.** Marco guía, disparo, galería y recorte. *Hecho cuando:* coinciden con el prototipo.
- [x] **T19.20 Texto reconocido.** Texto editable con aviso de dudas y error de OCR. *Hecho cuando:* coincide con el prototipo.
- [x] **T19.21 Ahora tú.** Entrada del usuario y respuesta con bien, incompleto y confuso. *Hecho cuando:* coincide con el prototipo.

**Repaso y progreso**
- [x] **T19.22 Repasar.** Pestaña con Hoy, Por libro y Por etiqueta, y estado vacío. *Hecho cuando:* coincide con el prototipo.
- [x] **T19.23 Sesión y resumen de fichas.** Tarjeta con volteo, botones de dificultad y resumen. *Hecho cuando:* coinciden con el prototipo.
- [x] **T19.24 Quiz.** Pregunta, corrección, salida y resultado. Incluye elegir el número de preguntas y "Repetir" (pedido del usuario). *Hecho cuando:* coinciden con el prototipo.
- [x] **T19.25 Progreso.** Anillo de meta, racha, gráfica semanal y libros del año. *Hecho cuando:* coincide con el prototipo.

**Notas y ajustes**
- [x] **T19.26a Todas las notas y tarjeta de nota.** Notas agrupadas por libro, buscador, chips y tarjeta con icono por tipo. *Hecho cuando:* coincide con el prototipo.- [x] **T19.26b Editor de nota.** *Hecho cuando:* coincide con el sistema visual del prototipo.- [x] **T19.26c Hoja de grabación de voz.** *Hecho cuando:* coincide con el sistema visual del prototipo.
- [x] **T19.26b Editor de nota.** *Hecho cuando:* coincide con el sistema visual del prototipo.
- [x] **T19.26c Hoja de grabación de voz.** *Hecho cuando:* coincide con el sistema visual del prototipo.
- [x] **T19.27a Ajustes.** Clave de API, apariencia, lectura, funciones de IA y tarjeta de privacidad. *Hecho cuando:* coincide con el prototipo.
- [x] **T19.27b Privacidad.** *Hecho cuando:* coincide con el sistema visual del prototipo.
- [x] **T19.27c Respaldo y Acerca de.** *Hecho cuando:* coincide con el prototipo.
- [x] **T19.28 Revisión final.** Recorrer las 23 pantallas en claro y oscuro, con reducir animaciones, y corregir diferencias. Actualizar capturas del README. *Hecho cuando:* no queda ninguna pantalla con el estilo anterior.

## 10. Riesgos técnicos (en orden)

1. Lector de EPUB/PDF con selección de texto (por eso queda aislado en B1 y B2).
2. Calidad del OCR con fotos reales (poca luz, páginas curvas): probar temprano, en T5.4.
3. Costo, límites y latencia de la IA.

---

### Riesgos de las fases 8 a 18

1. **Lector:** los navegadores de Readium están basados en Fragments; integrarlos en Compose es el mayor riesgo técnico, y por eso T11.1 es una prueba técnica antes de seguir.
2. **Transcripción de voz:** `SpeechRecognizer` transcribe en vivo, no archivos, y no se puede grabar el audio al mismo tiempo de forma fiable. Por eso T13.2 propone transcribir con Gemini, lo que gasta cuota.
3. **Selección de texto en PDF:** PdfRenderer no da capa de texto; la alternativa es OCR de la zona (T11.14).
4. **Formatos:** MOBI/AZW3 (propietarios), FB2, DOCX, CBR y RAR (licencia de unrar) no tienen soporte gratuito sencillo en Readium. El plan cubre EPUB, PDF, TXT y CBZ; el resto se decide aparte.
5. **Cuota de IA:** muchas funciones nuevas usan Gemini (quiz, fichas, transcripción, análisis), y la capa gratuita tiene límites por modelo. Se puede ampliar el respaldo de modelos ante `QuotaExhausted` y cachear respuestas.
6. **Fase 18:** contradice "sin cuentas ni sincronización". Se decide antes de empezarla.
7. **Efecto "curl" de página:** Readium no lo trae; se ofrecen deslizar, desvanecer, continuo o ninguno.

## 11. Pendientes por decidir

- Nombre definitivo de la app (provisional: `Reader`).
- ~~Primer proveedor de IA~~ → decidido: **Google Gemini** (`gemini-flash-latest`, clave gratuita de Google AI Studio).
- Idioma de las explicaciones (por defecto, el idioma del texto o español).
- **Modelo Free/Pro:** límites por plan y pantalla Pro. Falta decidir el método de pago (sin Play Store no hay Play Billing).
- **Revisión de accesibilidad (opcional):** prueba con TalkBack, texto al 200 %, reducir animaciones y háptica, según la checklist del diseño (4 §9 y §13). Se hará solo si es posible al final.
- **Cupo gratuito (B9):** función intermediaria serverless con tope diario global y límite por dispositivo, como un segundo `AiProvider`. Play Integrity exige publicar en Play Store.

---

## 12. Convenciones del repositorio

- **Repositorio:** público en GitHub, `Fabiann1809/reader`, rama principal `main`.
- **Autoría:** todos los commits van solo a nombre de `Fabiann1809 <leiderfabian538@gmail.com>`. Sin `Co-Authored-By` ni firmas de herramientas de IA.
- **Commits:** uno por tarea o fix, con [Conventional Commits](https://www.conventionalcommits.org/) en inglés: `type(scope): imperative summary`.
  - Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`, `style`.
  - Scopes: `data`, `library`, `notes`, `ai`, `ocr`, `camera`, `ui`, `build`, `reader`, `voice`, `review`, `quiz`, `progress`, `backup`, `sync`.
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
