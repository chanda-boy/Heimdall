# Heimdall

Sistema de Alerta Temprana frente a Amenazas Hidrometeorológicas (Reescritura de Sarada).

> 📌 **Memoria compartida y estado del proyecto:** Consulta [`STATE.md`](file:///home/danielcoronel/Projects/Heimdall/STATE.md) para el estado actual de los bounded contexts, arquitectura, decisiones y próximos pasos.

---

### Estándares del Proyecto
- **Estándar de codificación:** CamelCase, Google Java Style Guide.
- **Estándar de arquitectura:** Clean Architecture & DDD Estricto (capa `.domain` agnóstica de frameworks, verificada con ArchUnit).
- **Estándar de testing:** Pruebas unitarias, de integración y de arquitectura con JUnit 5 y ArchUnit (`./gradlew check`).

---

### Ejecución Rápida Local (Perfil Dev con H2)
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Documentación Adicional
- [Documento de Estado y Seguimiento (`STATE.md`)](file:///home/danielcoronel/Projects/Heimdall/STATE.md)
- [Inventario del Dominio Sarada](file:///home/danielcoronel/Projects/Heimdall/docs/domain/01_inventario_sarada.md)
- [Modelo del Dominio Heimdall](file:///home/danielcoronel/Projects/Heimdall/docs/domain/03_modelo_dominio.md)
- [Modelo Relacional y ERD](file:///home/danielcoronel/Projects/Heimdall/docs/domain/04_modelo_relacional.md)
- [Registro de Decisiones Arquitectónicas (ADR)](file:///home/danielcoronel/Projects/Heimdall/docs/domain/05_decisiones.md)
- [Plan Técnico Fase 2](file:///home/danielcoronel/Projects/Heimdall/docs/domain/07_plan_tecnico_fase_2.md)
