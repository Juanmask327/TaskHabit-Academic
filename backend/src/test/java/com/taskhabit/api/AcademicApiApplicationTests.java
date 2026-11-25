package com.taskhabit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;

/**
 * Pruebas de integración: levantan TODO el backend (controladores, servicios, JPA, Flyway)
 * contra H2 en memoria y hacen peticiones HTTP simuladas con MockMvc.
 * jwt() simula un token de Firebase ya validado (sin llamar a Google).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AcademicApiApplicationTests {

    @Autowired
    MockMvc mvc;

    /** Simula el token del usuario de Firebase con ese uid y correo. */
    private static RequestPostProcessor token(String uid, String correo) {
        return jwt().jwt(j -> j.subject(uid).claim("email", correo));
    }

    /** Arma una petición con cuerpo JSON y contexto /api. */
    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String cuerpo) {
        return req.contextPath("/api").contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/tareas").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilSeCreaEnPrimeraPeticionYSeActualiza() throws Exception {
        var ana = token("uid-ana", "ana@uni.edu");

        // Primera llamada: crea la fila con rol ESTUDIANTE (como hace RegisterActivity).
        mvc.perform(get("/api/usuarios/me/").contextPath("/api").with(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firebase_uid").value("uid-ana"))
                .andExpect(jsonPath("$.nombre").value("ana"))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));

        mvc.perform(json(patch("/api/usuarios/me/"), "{\"nombre\":\"Ana Pérez\",\"rol\":\"DOCENTE\"}").with(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.rol").value("DOCENTE"));

        // Un docente puede listar usuarios; un estudiante no.
        mvc.perform(get("/api/usuarios").contextPath("/api").with(ana))
                .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").contextPath("/api").with(token("uid-beto", "beto@uni.edu")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crudCompletoDeTareasYAislamientoEntreUsuarios() throws Exception {
        var carla = token("uid-carla", "carla@uni.edu");

        // CREATE
        String respuesta = mvc.perform(json(post("/api/tareas"),
                        "{\"titulo\":\"Taller\",\"tipo\":\"TAREA\",\"prioridad\":\"ALTA\"}").with(carla))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.completada").value(false))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        // READ
        mvc.perform(get("/api/tareas/" + id).contextPath("/api").with(carla))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Taller"));

        // UPDATE (PUT) y PATCH completada
        mvc.perform(json(put("/api/tareas/" + id),
                        "{\"titulo\":\"Taller 2\",\"tipo\":\"TAREA\",\"prioridad\":\"MEDIA\"}").with(carla))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Taller 2"));
        mvc.perform(json(patch("/api/tareas/" + id + "/completada"), "{\"completada\":true}").with(carla))
                .andExpect(jsonPath("$.completada").value(true));

        // Filtro de la lista
        mvc.perform(get("/api/tareas?completada=true").contextPath("/api").with(carla))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/tareas?tipo=HABITO").contextPath("/api").with(carla))
                .andExpect(jsonPath("$", hasSize(0)));

        // Otro usuario NO puede ver ni borrar la tarea de Carla -> 404
        var intruso = token("uid-intruso", "x@uni.edu");
        mvc.perform(get("/api/tareas/" + id).contextPath("/api").with(intruso))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/tareas/" + id).contextPath("/api").with(intruso))
                .andExpect(status().isNotFound());

        // DELETE
        mvc.perform(delete("/api/tareas/" + id).contextPath("/api").with(carla))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tareas/" + id).contextPath("/api").with(carla))
                .andExpect(status().isNotFound());
    }

    @Test
    void validacionDevuelve400ConDetallePorCampo() throws Exception {
        mvc.perform(json(post("/api/tareas"), "{\"titulo\":\"\",\"tipo\":\"TAREA\"}")
                        .with(token("uid-dani", "dani@uni.edu")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.titulo").exists())
                .andExpect(jsonPath("$.detalles.prioridad").exists());
    }

    @Test
    void reporteCalculaTotalesDesdeLasTareas() throws Exception {
        var eva = token("uid-eva", "eva@uni.edu");
        mvc.perform(json(post("/api/tareas"),
                "{\"titulo\":\"A\",\"tipo\":\"TAREA\",\"prioridad\":\"BAJA\",\"completada\":true}").with(eva));
        mvc.perform(json(post("/api/tareas"),
                "{\"titulo\":\"B\",\"tipo\":\"TAREA\",\"prioridad\":\"BAJA\"}").with(eva));
        mvc.perform(json(post("/api/tareas"),
                "{\"titulo\":\"Leer\",\"tipo\":\"HABITO\",\"prioridad\":\"MEDIA\",\"completada\":true}").with(eva));

        String hoy = LocalDate.now().toString();
        String respuesta = mvc.perform(json(post("/api/reportes"),
                        "{\"titulo\":\"Hoy\",\"fecha_inicio\":\"" + hoy + "\",\"fecha_fin\":\"" + hoy + "\"}").with(eva))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total_tareas").value(2))
                .andExpect(jsonPath("$.tareas_completadas").value(1))
                .andExpect(jsonPath("$.total_habitos").value(1))
                .andExpect(jsonPath("$.habitos_completados").value(1))
                .andExpect(jsonPath("$.porcentaje_cumplimiento").value(66.67))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        // Periodo al revés -> 400
        mvc.perform(json(put("/api/reportes/" + id),
                        "{\"titulo\":\"X\",\"fecha_inicio\":\"2026-10-05\",\"fecha_fin\":\"2026-10-01\"}").with(eva))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/reportes").contextPath("/api").with(eva))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(delete("/api/reportes/" + id).contextPath("/api").with(eva))
                .andExpect(status().isNoContent());
    }
}
