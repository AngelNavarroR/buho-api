# 🦉 Buho API

**Librería JPA dinámica para Spring Boot** — Permite realizar consultas, guardar y eliminar entidades JPA usando JSON dinámico. Solo necesitás el nombre de la entidad y los filtros; Buho genera el `CriteriaQuery` automáticamente.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.angelnavarror/buho-api)](https://central.sonatype.com/artifact/io.github.angelnavarror/buho-api)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)

---

## Instalación

### Maven

```xml
<dependency>
    <groupId>io.github.angelnavarror</groupId>
    <artifactId>buho-api</artifactId>
    <version>1.0.9</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.angelnavarror:buho-api:1.0.9'
```

## Configuración

Agregá las propiedades en tu `application.properties` o `application.yml`:

```yaml
buho:
  path: /filters          # Path base de los endpoints (default: /filters)
  debug: false            # Habilitar logs detallados
```

---

## Modelo de ejemplo

Todos los ejemplos de este manual usan el siguiente modelo de entidades:

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   Persona    │       │    Ciudad    │       │   Proyecto   │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id           │  M:1  │ id           │       │ id           │
│ nombre       │──────>│ nombre       │       │ nombre       │
│ apellido     │       │ provincia    │       │ fechaInicio  │
│ cedula       │       └──────────────┘       │ estado       │
│ email        │                              └──────┬───────┘
│ fechaNac     │  1:N  ┌──────────────┐              │
│ estado       │──────>│   Telefono   │         M:N  │
│ ciudad (FK)  │       ├──────────────┤              │
└──────┬───────┘       │ id           │  ┌───────────┘
       │               │ numero       │  │
       │               │ tipo         │  │
       │               │ persona (FK) │  │
       │               └──────────────┘  │
       │                                 │
       └──── persona_proyecto (M:N) ─────┘
```

### Entidades JPA

```java
@Entity
@Table(name = "persona")
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;
    private String cedula;
    private String email;
    private Date fechaNac;
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id")
    private Ciudad ciudad;                    // ManyToOne → Ciudad

    @OneToMany(mappedBy = "persona", fetch = FetchType.LAZY)
    private List<Telefono> telefonos;         // OneToMany → Telefono

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "persona_proyecto",
        joinColumns = @JoinColumn(name = "persona_id"),
        inverseJoinColumns = @JoinColumn(name = "proyecto_id")
    )
    private List<Proyecto> proyectos;         // ManyToMany → Proyecto
}

@Entity
@Table(name = "ciudad")
public class Ciudad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String provincia;
}

@Entity
@Table(name = "telefono")
public class Telefono {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String numero;
    private String tipo;         // MOVIL, FIJO, TRABAJO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;
}

@Entity
@Table(name = "proyecto")
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private Date fechaInicio;
    private String estado;

    @ManyToMany(mappedBy = "proyectos", fetch = FetchType.LAZY)
    private List<Persona> personas;
}
```

---

## Endpoints

Todos los endpoints son `POST` y reciben/devuelven `application/json`.

| Endpoint | Modelo | Descripción |
|---|---|---|
| `POST /filters/findBy` | `BusquedaModel` | Busca registros sin paginación |
| `POST /filters/findBy/page` | `BusquedaModel` | Busca con paginación (header `rootSize` con total) |
| `POST /filters/findBy/count` | `BusquedaModel` | Retorna el conteo de registros |
| `POST /filters/findBy/max` | `BusquedaModel` | Retorna el valor máximo de un campo |
| `POST /filters/findBy/exists` | `BusquedaModel` | Retorna `true`/`false` si existe al menos un registro |
| `POST /filters/save/entiti` | `GuardarModel` | Guarda o actualiza una entidad (JSON string) |
| `POST /filters/save/entiti/map` | `GuardarModel` | Guarda o actualiza una entidad (Map) |
| `POST /filters/eliminar/entiti` | `GuardarModel` | Elimina una entidad |

---

## BusquedaModel

Modelo principal para todas las consultas de búsqueda.

### Campos

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `entity` | `String` | *requerido* | Nombre de la clase JPA (ej: `"Persona"`) |
| `columns` | `List<String>` | `null` | Columnas a seleccionar. Si es `null`, retorna la entidad completa. Soporta rutas con JOINs |
| `filters` | `Map<String, Object>` | `null` | Condiciones WHERE. Las keys son nombres de campo o rutas con JOINs |
| `orders` | `Map<String, String>` | `null` | Ordenamiento. Key = campo, Value = `"ASC"` o `"DESC"` |
| `functions` | `Map<String, Object>` | `null` | Funciones SQL (ej: `COUNT`, `SUM`, `CONCAT`) |
| `groupsBy` | `List<String>` | `null` | Campos para GROUP BY |
| `first` | `Integer` | `null` | Índice del primer registro (offset) |
| `pageSize` | `Integer` | `null` | Cantidad de registros por página |
| `distinct` | `Boolean` | `true` | Aplicar DISTINCT al query |
| `unicoResultado` | `Boolean` | `false` | Retornar solo el primer resultado |

### Campos de Seek Pagination (cursor)

| Campo | Tipo | Descripción |
|---|---|---|
| `seek` | `Boolean` | Activar paginación por cursor |
| `cursorField` | `String` | Campo ordenado (soporta rutas con JOIN) |
| `cursorDirection` | `String` | `"ASC"` o `"DESC"` |
| `cursorValue` | `Object` | Último valor del campo cursor |
| `cursorIdValue` | `Object` | Último ID para desempate |

### Campos de serialización

| Campo | Tipo | Descripción |
|---|---|---|
| `gson` | `Boolean` | Usar Gson para serializar |
| `unproxy` | `Boolean` | Hacer unproxy de entidades Hibernate |
| `resolverDto` | `Boolean` | Resolver mapper DTO automáticamente |
| `ignoreFieldsGson` | `List<String>` | Campos a ignorar en serialización Gson |
| `ignoreClassGson` | `List<Class>` | Clases a ignorar en serialización Gson |
| `ignoreClassGsonw` | `List<String>` | Nombres de clases a ignorar (por string) |

---

## WhereCondition

Cada filtro puede ser un valor simple o un objeto `WhereCondition` con comparador.

### Campos

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `comparador` | `String` | `"EQ"` | Operador de comparación |
| `values` | `List<Object>` | *requerido* | Valores para la comparación |
| `trim` | `Boolean` | `true` | Aplicar `TRIM()` al valor |
| `upper` | `Boolean` | `true` | Aplicar `UPPER()` al valor |

### Comparadores disponibles

| Comparador | SQL generado | Valores | Ejemplo |
|---|---|---|---|
| `EQ` | `= valor` | 1 | `{"comparador":"EQ","values":["activo"]}` |
| `NE` | `!= valor` | 1 | `{"comparador":"NE","values":["inactivo"]}` |
| `GT` | `> valor` | 1 | `{"comparador":"GT","values":[100]}` |
| `GE` | `>= valor` | 1 | `{"comparador":"GE","values":[100]}` |
| `LT` | `< valor` | 1 | `{"comparador":"LT","values":[50]}` |
| `LE` | `<= valor` | 1 | `{"comparador":"LE","values":[50]}` |
| `LIKE` | `LIKE '%valor%'` | 1 | `{"comparador":"LIKE","values":["juan"]}` |
| `STARTS_WITH` | `LIKE 'valor%'` | 1 | `{"comparador":"startsWith","values":["099"]}` |
| `ENDS_WITH` | `LIKE '%valor'` | 1 | `{"comparador":"endsWith","values":["@gmail.com"]}` |
| `IN` | `IN (v1, v2, ...)` | 1+ | `{"comparador":"IN","values":["A","P"]}` |
| `NOT_IN` | `NOT IN (v1, v2)` | 1+ | `{"comparador":"NOT_IN","values":["X","E"]}` |
| `BETWEEN` | `BETWEEN v1 AND v2` | 2 | `{"comparador":"BETWEEN","values":["2024-01-01","2024-12-31"]}` |
| `IS_NULL` | `IS NULL` | 0 | `{"comparador":"IS_NULL","values":[]}` |
| `IS_NOT_NULL` | `IS NOT NULL` | 0 | `{"comparador":"IS_NOT_NULL","values":[]}` |

### Tipos soportados en values

`String`, `Long`, `Integer`, `Short`, `Byte`, `Double`, `Float`, `Boolean`, `BigDecimal`, `BigInteger`, `Date`, `LocalDate`, `LocalDateTime`, `OffsetDateTime`, `Instant`, `Enum`.

### Formatos de fecha soportados

| Formato | Ejemplo |
|---|---|
| ISO-8601 Instant | `2024-06-15T10:30:00Z` |
| `yyyy-MM-dd'T'HH:mm:ss` | `2024-06-15T10:30:00` |
| `yyyy-MM-dd HH:mm:ss` | `2024-06-15 10:30:00` |
| `dd/MM/yyyy HH:mm:ss` | `15/06/2024 10:30:00` |
| `yyyy-MM-dd` | `2024-06-15` |
| `dd/MM/yyyy` | `15/06/2024` |
| Epoch millis | `1718441400000` |

---

## JOINs automáticos

Buho detecta automáticamente cuándo crear JOINs basándose en rutas con punto (`.`) en `filters`, `columns`, `orders`, `groupsBy` y `functions`.

### ManyToOne — `ciudad.nombre`

```
Persona ──ManyToOne──> Ciudad
```

```
"ciudad.nombre"
 ──────  ──────
    │       └── Campo final (WHERE / SELECT)
    └── JOIN: persona → ciudad
```

**SQL:** `INNER JOIN ciudad c ON persona.ciudad_id = c.id`

### OneToMany — `telefonos.numero`

```
Persona ──OneToMany──> Telefono
```

```
"telefonos.numero"
 ─────────  ──────
     │         └── Campo final
     └── JOIN: persona → telefonos
```

**SQL:** `INNER JOIN telefono t ON t.persona_id = persona.id`

### ManyToMany — `proyectos.nombre`

```
Persona ──ManyToMany──> Proyecto  (via persona_proyecto)
```

```
"proyectos.nombre"
 ─────────  ──────
     │         └── Campo final
     └── JOIN: persona → proyectos (tabla intermedia automática)
```

**SQL:** `INNER JOIN persona_proyecto pp ON pp.persona_id = persona.id INNER JOIN proyecto pr ON pp.proyecto_id = pr.id`

### JOINs encadenados

Se pueden encadenar múltiples niveles de relaciones:

```
"proyectos.personas.ciudad.provincia"
 ─────────  ────────  ──────  ─────────
     │          │        │        └── Campo final
     │          │        └── JOIN 3: persona → ciudad (ManyToOne)
     │          └── JOIN 2: proyecto → personas (ManyToMany inverso)
     └── JOIN 1: root → proyectos (ManyToMany)
```

---

## Columns (SELECT específico)

Cuando se especifica `columns`, la respuesta cambia de una lista de entidades a una **lista de `Map<String, Object>`** donde las keys son los paths de las columnas.

### Sin columns — entidad completa

```json
{
  "entity": "Persona",
  "filters": { "estado": "A" },
  "first": 0,
  "pageSize": 10
}
```

**SQL:** `SELECT * FROM persona WHERE estado = 'A'`

**Response:**
```json
[
  {
    "id": 1,
    "nombre": "Carlos",
    "apellido": "Mendoza",
    "cedula": "0912345678",
    "email": "carlos@email.com",
    "fechaNac": "1990-03-15",
    "estado": "A",
    "ciudad": { "id": 1, "nombre": "Guayaquil", "provincia": "Guayas" },
    "telefonos": [...],
    "proyectos": [...]
  }
]
```

### Con columns — solo campos seleccionados

```json
{
  "entity": "Persona",
  "columns": ["id", "nombre", "apellido", "ciudad.nombre", "ciudad.provincia"],
  "filters": { "estado": "A" },
  "first": 0,
  "pageSize": 10
}
```

**SQL:**
```sql
SELECT p.id, p.nombre, p.apellido, c.nombre, c.provincia
FROM persona p
INNER JOIN ciudad c ON p.ciudad_id = c.id
WHERE p.estado = 'A'
```

**Response:**
```json
[
  {
    "id": 1,
    "nombre": "Carlos",
    "apellido": "Mendoza",
    "ciudad.nombre": "Guayaquil",
    "ciudad.provincia": "Guayas"
  }
]
```

> Los JOINs se crean automáticamente al detectar rutas con punto (`.`) en las columnas.

---

## Ejemplos JSON

### 1. Búsqueda simple por igualdad

```json
POST /filters/findBy

{
  "entity": "Persona",
  "filters": {
    "estado": "A"
  }
}
```

### 2. Búsqueda con paginación

```json
POST /filters/findBy/page

{
  "entity": "Persona",
  "filters": {
    "estado": "A"
  },
  "orders": { "apellido": "ASC", "nombre": "ASC" },
  "first": 0,
  "pageSize": 20,
  "distinct": true
}
```

> El header de respuesta incluye `rootSize` con el total de registros.

### 3. startsWith — buscar por cédula

```json
{
  "entity": "Persona",
  "filters": {
    "cedula": {
      "comparador": "startsWith",
      "trim": true,
      "upper": false,
      "values": ["0912"]
    }
  },
  "orders": { "apellido": "ASC" },
  "first": 0,
  "pageSize": 50
}
```

**SQL:** `WHERE TRIM(cedula) LIKE '0912%'`

### 4. BETWEEN — rango de fechas de nacimiento

```json
{
  "entity": "Persona",
  "filters": {
    "fechaNac": {
      "comparador": "BETWEEN",
      "values": ["1990-01-01", "1999-12-31"]
    }
  },
  "orders": { "fechaNac": "DESC" }
}
```

### 5. IN — filtrar por múltiples estados

```json
{
  "entity": "Persona",
  "filters": {
    "estado": {
      "comparador": "IN",
      "values": ["A", "P", "S"]
    }
  }
}
```

### 6. IS_NULL / IS_NOT_NULL

```json
{
  "entity": "Persona",
  "filters": {
    "email": {
      "comparador": "IS_NOT_NULL",
      "values": []
    },
    "fechaNac": {
      "comparador": "IS_NULL",
      "values": []
    }
  }
}
```

### 7. ManyToOne — filtrar por ciudad

```json
{
  "entity": "Persona",
  "filters": {
    "ciudad.nombre": {
      "comparador": "EQ",
      "values": ["Guayaquil"]
    }
  },
  "orders": { "apellido": "ASC" },
  "first": 0,
  "pageSize": 20
}
```

**SQL:**
```sql
SELECT p.* FROM persona p
INNER JOIN ciudad c ON p.ciudad_id = c.id
WHERE UPPER(TRIM(c.nombre)) = 'GUAYAQUIL'
ORDER BY p.apellido ASC
```

### 8. OneToMany — buscar personas por tipo de teléfono

```json
{
  "entity": "Persona",
  "filters": {
    "telefonos.tipo": {
      "comparador": "EQ",
      "values": ["MOVIL"]
    }
  },
  "distinct": true,
  "first": 0,
  "pageSize": 20
}
```

**SQL:**
```sql
SELECT DISTINCT p.* FROM persona p
INNER JOIN telefono t ON t.persona_id = p.id
WHERE UPPER(TRIM(t.tipo)) = 'MOVIL'
```

### 9. OneToMany + Columns — buscar por número de teléfono

```json
{
  "entity": "Persona",
  "columns": ["id", "nombre", "apellido", "telefonos.numero", "telefonos.tipo"],
  "filters": {
    "telefonos.numero": {
      "comparador": "startsWith",
      "values": ["0991"]
    }
  }
}
```

**SQL:**
```sql
SELECT p.id, p.nombre, p.apellido, t.numero, t.tipo
FROM persona p
INNER JOIN telefono t ON t.persona_id = p.id
WHERE TRIM(t.numero) LIKE '0991%'
```

**Response:**
```json
[
  {
    "id": 1,
    "nombre": "Carlos",
    "apellido": "Mendoza",
    "telefonos.numero": "0991234567",
    "telefonos.tipo": "MOVIL"
  }
]
```

### 10. ManyToMany — buscar personas por proyecto

```json
{
  "entity": "Persona",
  "filters": {
    "proyectos.nombre": {
      "comparador": "LIKE",
      "values": ["Portal Ciudadano"]
    }
  },
  "distinct": true,
  "orders": { "apellido": "ASC" },
  "first": 0,
  "pageSize": 20
}
```

**SQL:**
```sql
SELECT DISTINCT p.* FROM persona p
INNER JOIN persona_proyecto pp ON pp.persona_id = p.id
INNER JOIN proyecto pr ON pp.proyecto_id = pr.id
WHERE UPPER(TRIM(pr.nombre)) LIKE '%PORTAL CIUDADANO%'
ORDER BY p.apellido ASC
```

### 11. ManyToMany + Columns — datos de persona y proyecto

```json
{
  "entity": "Persona",
  "columns": [
    "id",
    "nombre",
    "apellido",
    "email",
    "proyectos.nombre",
    "proyectos.estado"
  ],
  "filters": {
    "proyectos.estado": {
      "comparador": "EQ",
      "values": ["ACTIVO"]
    }
  },
  "orders": { "apellido": "ASC" }
}
```

**SQL:**
```sql
SELECT p.id, p.nombre, p.apellido, p.email, pr.nombre, pr.estado
FROM persona p
INNER JOIN persona_proyecto pp ON pp.persona_id = p.id
INNER JOIN proyecto pr ON pp.proyecto_id = pr.id
WHERE UPPER(TRIM(pr.estado)) = 'ACTIVO'
ORDER BY p.apellido ASC
```

**Response:**
```json
[
  {
    "id": 1,
    "nombre": "Carlos",
    "apellido": "Mendoza",
    "email": "carlos@email.com",
    "proyectos.nombre": "Portal Ciudadano",
    "proyectos.estado": "ACTIVO"
  },
  {
    "id": 2,
    "nombre": "María",
    "apellido": "García",
    "email": "maria@email.com",
    "proyectos.nombre": "App Móvil",
    "proyectos.estado": "ACTIVO"
  }
]
```

### 12. Combinación — ManyToOne + OneToMany + ManyToMany

```json
{
  "entity": "Persona",
  "columns": [
    "nombre",
    "apellido",
    "ciudad.nombre",
    "telefonos.numero",
    "proyectos.nombre"
  ],
  "filters": {
    "ciudad.provincia": {
      "comparador": "EQ",
      "values": ["Guayas"]
    },
    "telefonos.tipo": {
      "comparador": "EQ",
      "values": ["MOVIL"]
    },
    "proyectos.estado": {
      "comparador": "EQ",
      "values": ["ACTIVO"]
    }
  },
  "distinct": true,
  "orders": { "apellido": "ASC" }
}
```

**SQL:**
```sql
SELECT DISTINCT p.nombre, p.apellido, c.nombre, t.numero, pr.nombre
FROM persona p
INNER JOIN ciudad c ON p.ciudad_id = c.id
INNER JOIN telefono t ON t.persona_id = p.id
INNER JOIN persona_proyecto pp ON pp.persona_id = p.id
INNER JOIN proyecto pr ON pp.proyecto_id = pr.id
WHERE UPPER(TRIM(c.provincia)) = 'GUAYAS'
  AND UPPER(TRIM(t.tipo)) = 'MOVIL'
  AND UPPER(TRIM(pr.estado)) = 'ACTIVO'
ORDER BY p.apellido ASC
```

### 13. Conteo con JOIN ManyToMany

```json
POST /filters/findBy/count

{
  "entity": "Persona",
  "filters": {
    "proyectos.nombre": {
      "comparador": "EQ",
      "values": ["Portal Ciudadano"]
    }
  },
  "distinct": true
}
```

**Response:** `15`

### 14. Verificar existencia

```json
POST /filters/findBy/exists

{
  "entity": "Persona",
  "filters": {
    "cedula": "0912345678"
  }
}
```

**Response:** `true`

### 15. Functions — contar personas por ciudad

```json
{
  "entity": "Persona",
  "functions": {
    "ciudad.nombre": "ciudad.nombre",
    "count": "id"
  },
  "groupsBy": ["ciudad.nombre"],
  "filters": {
    "estado": "A"
  }
}
```

**Response:**
```json
[
  { "ciudad.nombre": "Guayaquil", "count": 45 },
  { "ciudad.nombre": "Quito", "count": 32 },
  { "ciudad.nombre": "Cuenca", "count": 18 }
]
```

### 16. Functions — contar personas por proyecto (ManyToMany)

```json
{
  "entity": "Persona",
  "functions": {
    "proyectos.nombre": "proyectos.nombre",
    "count": "id"
  },
  "groupsBy": ["proyectos.nombre"],
  "filters": {
    "estado": "A"
  }
}
```

**Response:**
```json
[
  { "proyectos.nombre": "Portal Ciudadano", "count": 12 },
  { "proyectos.nombre": "App Móvil", "count": 8 },
  { "proyectos.nombre": "ERP Municipal", "count": 15 }
]
```

### 17. OR — mismo campo, múltiples condiciones

Buscar personas cuyo nombre empiece con "Car" **o** con "Mar":

```json
{
  "entity": "Persona",
  "filters": {
    "nombre": {
      "comparador": "OR",
      "values": [
        { "comparador": "startsWith", "values": ["Car"] },
        { "comparador": "startsWith", "values": ["Mar"] }
      ]
    }
  },
  "orders": { "apellido": "ASC" }
}
```

**SQL:**
```sql
SELECT * FROM persona p
WHERE (UPPER(TRIM(p.nombre)) LIKE 'CAR%' OR UPPER(TRIM(p.nombre)) LIKE 'MAR%')
ORDER BY p.apellido ASC
```

### 18. OR — diferentes campos

Buscar personas donde el nombre sea "Carlos" **o** el apellido sea "García":

```json
{
  "entity": "Persona",
  "filters": {
    "nombre": {
      "comparador": "OR",
      "values": [
        { "nombre": { "comparador": "EQ", "values": ["Carlos"] } },
        { "apellido": { "comparador": "EQ", "values": ["García"] } }
      ]
    }
  }
}
```

**SQL:**
```sql
SELECT * FROM persona p
WHERE (UPPER(TRIM(p.nombre)) = 'CARLOS' OR UPPER(TRIM(p.apellido)) = 'GARCÍA')
```

### 19. OR — diferentes campos con JOINs

Buscar personas que vivan en Guayaquil **o** que estén en el proyecto "Portal Ciudadano":

```json
{
  "entity": "Persona",
  "filters": {
    "ciudad.nombre": {
      "comparador": "OR",
      "values": [
        { "ciudad.nombre": { "comparador": "EQ", "values": ["Guayaquil"] } },
        { "proyectos.nombre": { "comparador": "EQ", "values": ["Portal Ciudadano"] } }
      ]
    }
  },
  "distinct": true
}
```

**SQL:**
```sql
SELECT DISTINCT p.* FROM persona p
INNER JOIN ciudad c ON p.ciudad_id = c.id
INNER JOIN persona_proyecto pp ON pp.persona_id = p.id
INNER JOIN proyecto pr ON pp.proyecto_id = pr.id
WHERE (UPPER(TRIM(c.nombre)) = 'GUAYAQUIL' OR UPPER(TRIM(pr.nombre)) = 'PORTAL CIUDADANO')
```

### 20. AND — mismo campo, múltiples condiciones

Buscar personas cuyo nombre contenga "ar" **y** termine con "os":

```json
{
  "entity": "Persona",
  "filters": {
    "nombre": {
      "comparador": "AND",
      "values": [
        { "comparador": "LIKE", "values": ["ar"] },
        { "comparador": "endsWith", "values": ["os"] }
      ]
    }
  }
}
```

**SQL:**
```sql
SELECT * FROM persona p
WHERE (UPPER(TRIM(p.nombre)) LIKE '%AR%' AND UPPER(TRIM(p.nombre)) LIKE '%OS')
```

### 21. AND + OR combinados

Buscar personas activas que sean de Guayaquil **o** Quito, y cuyo teléfono sea móvil:

```json
{
  "entity": "Persona",
  "filters": {
    "estado": "A",
    "ciudad.nombre": {
      "comparador": "OR",
      "values": [
        { "comparador": "EQ", "values": ["Guayaquil"] },
        { "comparador": "EQ", "values": ["Quito"] }
      ]
    },
    "telefonos.tipo": {
      "comparador": "EQ",
      "values": ["MOVIL"]
    }
  },
  "distinct": true,
  "orders": { "apellido": "ASC" }
}
```

**SQL:**
```sql
SELECT DISTINCT p.* FROM persona p
INNER JOIN ciudad c ON p.ciudad_id = c.id
INNER JOIN telefono t ON t.persona_id = p.id
WHERE UPPER(TRIM(p.estado)) = 'A'
  AND (UPPER(TRIM(c.nombre)) = 'GUAYAQUIL' OR UPPER(TRIM(c.nombre)) = 'QUITO')
  AND UPPER(TRIM(t.tipo)) = 'MOVIL'
ORDER BY p.apellido ASC
```

> **Nota:** Los filtros a nivel raíz del `filters` siempre se combinan con **AND**. Para agrupar condiciones con **OR**, usá `"comparador": "OR"` dentro de un `WhereCondition`.

### 22. Seek Pagination (cursor)

Más eficiente que OFFSET para datasets grandes.

**Primera página:**
```json
POST /filters/findBy/page

{
  "entity": "Persona",
  "filters": { "estado": "A" },
  "seek": true,
  "cursorField": "id",
  "cursorDirection": "ASC",
  "pageSize": 50
}
```

**Siguiente página** (usando el último `id` recibido):
```json
{
  "entity": "Persona",
  "filters": { "estado": "A" },
  "seek": true,
  "cursorField": "id",
  "cursorDirection": "ASC",
  "cursorValue": 1050,
  "cursorIdValue": 1050,
  "pageSize": 50
}
```

### 23. Guardar entidad

```json
POST /filters/save/entiti

{
  "entity": "Persona",
  "data": "{\"nombre\":\"Luis\",\"apellido\":\"Torres\",\"cedula\":\"0987654321\",\"email\":\"luis@email.com\",\"estado\":\"A\"}"
}
```

### 24. Guardar entidad con Map

```json
POST /filters/save/entiti/map

{
  "entity": "Persona",
  "dataMap": {
    "nombre": "Luis",
    "apellido": "Torres",
    "cedula": "0987654321",
    "email": "luis@email.com",
    "estado": "A"
  }
}
```

### 25. Eliminar entidad

```json
POST /filters/eliminar/entiti

{
  "entity": "Persona",
  "data": "{\"id\": 123}"
}
```

---

## Builder (uso en Java)

### Búsqueda simple

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .where("estado", "A")
    .order("apellido", "ASC")
    .first(0)
    .pageSize(20)
    .build();
```

### Con WhereCondition

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .whereCondition("cedula",
        new WhereCondition("startsWith", Arrays.asList("0912")))
    .whereCondition("fechaNac",
        WhereCondition.between("1990-01-01", "1999-12-31"))
    .whereCondition("estado",
        WhereCondition.in(Arrays.asList("A", "P")))
    .order("apellido", "ASC")
    .first(0)
    .pageSize(50)
    .build();
```

### Con Columns

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .columns("id", "nombre", "apellido", "ciudad.nombre", "proyectos.nombre")
    .whereCondition("proyectos.estado",
        WhereCondition.eq("ACTIVO"))
    .order("apellido", "ASC")
    .first(0)
    .pageSize(20)
    .distinct(true)
    .build();
```

### JOIN ManyToOne — filtrar por ciudad

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .whereCondition("ciudad.provincia",
        WhereCondition.eq("Guayas"))
    .order("apellido", "ASC")
    .build();
```

### JOIN OneToMany — buscar por teléfono

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .columns("id", "nombre", "telefonos.numero", "telefonos.tipo")
    .whereCondition("telefonos.tipo",
        WhereCondition.eq("MOVIL"))
    .distinct(true)
    .build();
```

### JOIN ManyToMany — buscar por proyecto

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .whereCondition("proyectos.nombre",
        new WhereCondition("LIKE", Arrays.asList("Portal")))
    .order("apellido", "ASC")
    .distinct(true)
    .first(0)
    .pageSize(20)
    .build();
```

### Combinación de JOINs

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .columns("nombre", "apellido", "ciudad.nombre",
             "telefonos.numero", "proyectos.nombre")
    .whereCondition("ciudad.provincia",
        WhereCondition.eq("Guayas"))
    .whereCondition("telefonos.tipo",
        WhereCondition.eq("MOVIL"))
    .whereCondition("proyectos.estado",
        WhereCondition.eq("ACTIVO"))
    .order("apellido", "ASC")
    .distinct(true)
    .build();
```

### OR — mismo campo

```java
// nombre que empiece con "Car" O con "Mar"
BusquedaModel busq = BusquedaModel.builder("Persona")
    .whereCondition("nombre",
        new WhereCondition("OR", Arrays.asList(
            new WhereCondition("startsWith", Arrays.asList("Car")),
            new WhereCondition("startsWith", Arrays.asList("Mar"))
        )))
    .order("apellido", "ASC")
    .build();
```

### AND — mismo campo

```java
// nombre que contenga "ar" Y termine con "os"
BusquedaModel busq = BusquedaModel.builder("Persona")
    .whereCondition("nombre",
        new WhereCondition("AND", Arrays.asList(
            new WhereCondition("LIKE", Arrays.asList("ar")),
            new WhereCondition("endsWith", Arrays.asList("os"))
        )))
    .build();
```

### AND + OR combinados

```java
// Personas activas de Guayaquil O Quito, con teléfono móvil
BusquedaModel busq = BusquedaModel.builder("Persona")
    .where("estado", "A")
    .whereCondition("ciudad.nombre",
        new WhereCondition("OR", Arrays.asList(
            new WhereCondition("EQ", Arrays.asList("Guayaquil")),
            new WhereCondition("EQ", Arrays.asList("Quito"))
        )))
    .whereCondition("telefonos.tipo",
        WhereCondition.eq("MOVIL"))
    .distinct(true)
    .order("apellido", "ASC")
    .build();
```

### Seek Pagination

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .where("estado", "A")
    .cursorField("id")
    .cursorDirection("ASC")
    .cursorValue(lastId)
    .cursorIdValue(lastId)
    .pageSize(50)
    .build();
```

### Único resultado

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .where("cedula", "0912345678")
    .unicoResultado(true)
    .build();
```

### Functions con Group By

```java
BusquedaModel busq = BusquedaModel.builder("Persona")
    .functions("ciudad.nombre", "ciudad.nombre")
    .functions("count", "id")
    .where("estado", "A")
    .build();
busq.setGroupsBy(Arrays.asList("ciudad.nombre"));
```

---

## DateTimeSettings

Configuración global para el parseo de fechas en `WhereCondition`:

```java
@Configuration
public class BuhoConfig {

    @PostConstruct
    void configureDates() {
        WhereCondition.setDateTimeSettings(
            DateTimeSettings.builder()
                .zoneId(ZoneId.of("America/Guayaquil"))
                .addLocalDatePattern("yyyy-MM-dd")
                .addLocalDatePattern("dd/MM/yyyy")
                .addLocalDateTimePattern("yyyy-MM-dd'T'HH:mm:ss")
                .addLocalDateTimePattern("dd/MM/yyyy HH:mm:ss")
                .likeEscape('\\')
                .endOfDayOnBetweenUpperBound(true)
                .build()
        );
    }
}
```

> Con `endOfDayOnBetweenUpperBound(true)`, un BETWEEN con fecha `"2024-12-31"` se interpreta como `2024-12-31 23:59:59.999` para el límite superior, garantizando que se incluya todo el día.

---

## Requisitos

- **Java:** 17+
- **Spring Boot:** 3.x
- **JPA/Hibernate:** 6.x
- **Base de datos:** Cualquiera soportada por Hibernate (PostgreSQL, MySQL, Oracle, H2, etc.)

## Licencia

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Autor

**Angel Navarro** — [navarroangelr@gmail.com](mailto:navarroangelr@gmail.com)

[GitHub](https://github.com/AngelNavarroR/jpa-busqueda-api)