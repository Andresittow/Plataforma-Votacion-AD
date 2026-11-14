# Kit de arranque: Plataforma de Votación de Alta Disponibilidad

Archivos de este kit (súbanlos a la raíz del repo, en `develop`, antes de empezar):

| Archivo | Para qué |
|---|---|
| `AGENTS.md` | Reglas de código que Codex y Antigravity leen automáticamente |
| `CONTRACTS.md` | Contratos compartidos (topics, JSON, exchanges, clases comunes). **Fuente de verdad** |
| `docker-compose.dev.yml` | Kafka + Zookeeper + RabbitMQ para probar en local (sin límites; el compose final lo hace el Rol D) |
| `PROMPT_PERSONA_A.md` | Prompts de la Persona A (ingesta) — Antigravity |
| `PROMPT_PERSONA_B.md` | Prompts de la Persona B (procesamiento y distribución) — Codex |

## Reparto

| | Persona A (Antigravity) | Persona B (Codex) |
|---|---|---|
| Rama | `feature/ingesta` | `feature/procesamiento` |
| Módulos | parent pom, `common-domain`, `voter-registration-service`, `voting-api` | `vote-processor`, `regional-rollup-service`, `global-dashboard-worker`, `regional-dashboard-worker` |
| Patrones | Value Objects, DIP, dominio rico | Strategy, agregado rico |

## Línea de tiempo sugerida para hoy

| Bloque | A | B |
|---|---|---|
| 0:00-0:30 | **A0** esqueleto Maven → push a `develop` | Lee contratos, **B1** (solo dominio `GlobalTally` + tests) |
| 0:30-1:30 | **A1** `common-domain` + tests (push a `develop`) | B1 sigue; hace `git pull` de `develop` cuando A1 esté |
| 1:30-3:00 | **A2** voter-registration | **B2** regional + Strategy |
| 3:00-4:30 | **A3** voting-api | **B3** dashboards |
| 4:30-5:30 | **A4** Dockerfiles + prueba E2E | **B4** Dockerfiles + prueba E2E |
| Final | Integración juntos con `docker-compose.dev.yml` | |

## Reglas de convivencia en git

- Solo `common-domain` y el `pom.xml` padre son de A. Si B necesita un cambio ahí, se lo pide a A (no lo edita).
- Cada quien hace commits pequeños y `git pull --rebase origin develop` antes de hacer push.
- Merge a `develop` cuando el módulo compile y sus tests pasen (`mvn -pl <modulo> -am test`).

## Flujo de ramas (git)

- `main`: solo la versión final de entrega.
- `develop`: rama de integración. Aquí se sube el esqueleto (A0), `common-domain` (A1) y se hace merge de cada feature terminada.
- `feature/ingesta` (Persona A) y `feature/procesamiento` (Persona B) nacen de `develop`:

```bash
git checkout develop && git pull origin develop
git checkout -b feature/ingesta        # o feature/procesamiento
# ... trabajar, commits pequeños ...
git pull --rebase origin develop       # antes de integrar
git checkout develop && git merge --no-ff feature/ingesta && git push origin develop
```
- Al terminar todo y con la integración probada: merge de `develop` a `main`.
