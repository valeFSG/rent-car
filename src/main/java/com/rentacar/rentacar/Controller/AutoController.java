package com.rentacar.rentacar.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.rentacar.rentacar.Model.Auto;
import com.rentacar.rentacar.Service.AutoService;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
/**
 * CAPA DE CONTROLADOR (Entry Point)
 * Esta clase expone los Endpoints para que un cliente (Postman, Frontend) 
 * pueda comunicarse con nuestra aplicación. 
 * Gestiona las Solicitudes (Requests) y entrega las Respuestas (Responses).
 */
@RestController // Define que esta clase es un controlador REST (maneja datos JSON).
@RequestMapping("/api/autos") // Ruta base para todos los endpoints de este controlador.
public class AutoController {

    @Autowired // Inyección de dependencias para conectar con la capa de Servicio.
    private AutoService service;
    
    private static final Logger log = LoggerFactory.getLogger(AutoController.class);
    @GetMapping("/holaMundo")
    public String HolaMundo (){
        return "hola Mundo";
    }

    @GetMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerLog(
            @RequestParam(value = "level", defaultValue = "info") String level,
            @RequestParam(value = "message", defaultValue = "Transacción procesada correctamente") String message,
            @RequestParam(value = "path", defaultValue = "/api/autos") String fakePath) {

        // Ya no usamos UUID alternativos. Le pedimos a OpenTelemetry el Span actual (el contexto HTTP)
        // y extraemos el verdadero TraceID que viajará hacia Grafana Tempo.
       String manualTraceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        MDC.put("traceId", manualTraceId);
        MDC.put("http_path", fakePath);

        Map<String, Object> response = new HashMap<>();
        response.clear();

        switch (level.toLowerCase()) {
            case "info":
                log.info("MDC_FIELDS - {} - Duration: {}ms", message, (int) (Math.random() * 200 + 50));
                response.put("status", "success");
                break;
                
            case "warn":
                log.warn("MDC_FIELDS - Alerta de rendimiento: {}", message);
                response.put("status", "warning");
                break;
                
            case "error":
                // Forzamos una excepción para que el logback capture el stacktrace en el JSON
                try {
                    throw new RuntimeException("Error interno del servidor: " + message);
                } catch (Exception e) {
                    log.error("MDC_FIELDS - Error crítico detectado en el controlador", e);
                }
                response.put("status", "error");
                break;
                
            default:
                log.info("Nivel no reconocido, cayendo en INFO por defecto: {}", message);
                response.put("status", "default");
        }

        response.put("traceId", manualTraceId);
        response.put("simulated_level", level.toUpperCase());
        
        // Limpiamos el MDC para que no contamine otros hilos
        MDC.clear(); 

        return ResponseEntity.ok(response);
    }
    /**
     * MÉTODO GET: Obtener todos los vehículos.
     * URL: GET localhost:8080/api/autos
     * @return 200 OK con la lista completa.
     */
    @GetMapping
    public ResponseEntity<List<Auto>> getAll() {

        long startTime = System.currentTimeMillis();
        String manualTraceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        MDC.put("traceId", manualTraceId);

        // 1. INICIALIZAR EL TRACER DE OPENTELEMETRY
        Tracer tracer = GlobalOpenTelemetry.getTracer("rentacar-controller", "1.0.0");
            
        // 2. CREAR EL SPAN RAÍZ DE LA PETICIÓN HTTP
        Span rootSpan = tracer.spanBuilder("HTTP GET /api/autos").startSpan();
        rootSpan.setAttribute("http.method", "GET");
        rootSpan.setAttribute("custom.trace_id", manualTraceId);

        // El controlador pide la lista al servicio
        List<Auto> listaAutos = service.listarTodos();
      
        // Calculamos cuánto demoró la operación en milisegundos
        long duration = System.currentTimeMillis() - startTime;

        // Inyectamos metadatos útiles en el MDC para Grafana Loki
        MDC.put("action", "LISTAR_FLOTA");
        MDC.put("total_autos", String.valueOf(listaAutos.size()));
        MDC.put("duration_ms", String.valueOf(duration));
        MDC.put("http_path", "/api/autos");

        // Emitimos el log estructurado
        if (listaAutos.isEmpty()) {
            log.warn("FLOTA_EVENT - Se consultó la flota pero no existen vehículos registrados en la base de datos.");
        } else {
            log.info("FLOTA_EVENT - Consulta de flota exitosa. Registros enviados al cliente.");
        }

        // ¡Súper importante! Limpiamos el MDC para liberar el hilo actual de Tomcat
        MDC.clear();

        return ResponseEntity.ok(listaAutos);
    }
    /**
     * MÉTODO GET: Filtrar solo vehículos disponibles.
     * URL: GET localhost:8080/api/autos/disponibles
     * @return 200 OK con la lista filtrada.
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Auto>> getDisponibles() {
       String manualTraceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        MDC.put("traceId", manualTraceId);
        MDC.put("action", "LISTAR_FLOTA_DISPONIBLES");
        return ResponseEntity.ok(service.listarDisponibles());
    }

    /**
     * MÉTODO POST: Registrar un nuevo vehículo.
     * URL: POST localhost:8080/api/autos
     * @param auto Objeto JSON enviado en el cuerpo de la petición.
     * @return 201 Created si tuvo éxito | 400 Bad Request si la patente ya existe.
     */
    @PostMapping
    public ResponseEntity<String> create(@RequestBody Auto auto) {
        if (service.guardar(auto)) {
            // El código 201 es el estándar para creación exitosa.
            return new ResponseEntity<>("Auto registrado con éxito", HttpStatus.CREATED);
        }
        // Si la lógica de negocio falla (patente duplicada), devolvemos un 400.
        return new ResponseEntity<>("Error: La patente ya existe", HttpStatus.BAD_REQUEST);
    }

    /**
     * MÉTODO PUT: Arrendar un vehículo (Actualizar estado).
     * URL: PUT localhost:8080/api/autos/arrendar/{id}
     * @param id ID del vehículo enviado en la URL.
     * @return 200 OK, 400 Bad Request o 404 Not Found según el caso.
     */
    @PutMapping("/arrendar/{id}")
    public ResponseEntity<String> rent(@PathVariable Long id) {
        int resultado = service.arrendarVehiculo(id);

        // Evaluamos la respuesta de la lógica de negocio para elegir el código HTTP.
        return switch (resultado) {
            case 2 -> ResponseEntity.ok("Vehículo arrendado correctamente"); // Éxito (200)
            case 1 -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El vehículo ya está arrendado"); // Error lógico (400)
            default -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vehículo no encontrado"); // Error de existencia (404)
        };
    }

    /**
     * MÉTODO DELETE: Eliminar un registro.
     * URL: DELETE localhost:8080/api/autos/{id}
     * @return 204 No Content si se borró | 404 Not Found si no existía.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.eliminar(id)) {
            // 204 No Content es la respuesta ideal para una eliminación exitosa.
            return ResponseEntity.noContent().build();
        }
        // Si no se encontró nada que borrar, devolvemos 404.
        return ResponseEntity.notFound().build();
    }
}