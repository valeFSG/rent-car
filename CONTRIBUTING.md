# Guía de Contribución - Rent-A-Car Express v1.0

¡Gracias por sumarte al desarrollo de este microservicio! Para mantener el código limpio, escalable y profesional, seguimos las siguientes normas:

---

## 🌿 Flujo de Trabajo (Git Flow)

1. **No subir cambios directamente a `main`**: Esta rama es solo para código estable y probado.
2. **Crear una rama de trabajo**: Usa nombres descriptivos con los siguientes prefijos:
   - `feature/` para nuevas funcionalidades (ej: `feature/filtro-marcas`).
   - `fix/` para corregir errores (ej: `fix/error-borrado-id`).
   - `docs/` para documentación (ej: `docs/mejorar-comentarios`).
   - `refactor/` para mejoras de código sin cambiar funcionalidad.

---

## 🏗️ Estándares de Código (Clean Code)

Para asegurar la calidad del microservicio, respete la arquitectura de **3 capas**:
* **Controller**: Solo recibe peticiones y entrega respuestas. No debe tener lógica de `if` complejos o cálculos.
* **Service**: Aquí reside toda la **Lógica de Negocio**. Es el único lugar donde se validan reglas.
* **Repository**: Solo gestiona el acceso a los datos (la lista `flota`).

### Reglas de Oro:
* **Lombok**: Use `@Data`, `@AllArgsConstructor` y `@NoArgsConstructor` en los modelos.
* **Validaciones**: Use `@NotBlank` y `@Min` para asegurar que los datos que llegan por POST sean válidos.
* **Idiomas**: Mantenga los nombres de variables y métodos en español (o inglés, pero sea consistente).

---

## 📝 Formato de Commits

Usamos mensajes de commit claros que expliquen el **qué** y el **por qué**:
* ✅ `feat: agregar validación de patente duplicada en Service`
* ✅ `fix: corregir error 500 al eliminar ID inexistente`
* ❌ `update: cambios varios` (Evite mensajes genéricos).

---

## 🧪 Pruebas antes de enviar

Antes de solicitar la integración de su código (Pull Request), asegúrese de:
1. Que el proyecto **compile** correctamente (`mvn clean compile`).
2. Probar los endpoints en **Postman**.
3. Verificar que el archivo `README.md` esté actualizado si agregó una ruta nueva.

---

**¡A programar con lógica!** 🚀