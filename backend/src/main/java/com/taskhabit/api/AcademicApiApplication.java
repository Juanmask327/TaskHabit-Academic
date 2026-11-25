package com.taskhabit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend.
 *
 * @SpringBootApplication activa tres cosas a la vez:
 *  - @Configuration: esta clase puede declarar configuración.
 *  - @EnableAutoConfiguration: Spring Boot configura solo lo que detecta en el classpath
 *    (ve el driver de PostgreSQL + JPA -> crea el DataSource y el EntityManager, etc.).
 *  - @ComponentScan: busca @RestController, @Service, @Repository... en este paquete
 *    (com.taskhabit.api) y sus subpaquetes, y los registra como "beans".
 */
@SpringBootApplication
public class AcademicApiApplication {

    public static void main(String[] args) {
        // Arranca el contenedor de Spring y el Tomcat embebido (puerto 8000, ver application.properties).
        SpringApplication.run(AcademicApiApplication.class, args);
    }
}
