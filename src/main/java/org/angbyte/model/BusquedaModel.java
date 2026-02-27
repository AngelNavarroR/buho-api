package org.angbyte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.angbyte.utils.Utilities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a dynamic search model to encapsulate query parameters such as entity, filters,
 * ordering, pagination, grouping, and cursor-based pagination metadata. This class is typically
 * used to construct and manage criteria for executing dynamic, parameterized database queries.
 * <p>
 * It includes details for:
 * - Entity name to query.
 * - Filtering conditions based on key-value pairs.
 * - Sorting and ordering preferences.
 * - Pagination metadata (e.g., first record, page size).
 * - Distinct result requirement.
 * - Cursor-based pagination details (e.g., field, direction, and values).
 * - JSON serialization/deserialization handling fields and classes to be ignored.
 * - Functions and grouping fields for aggregation queries.
 * <p>
 * Additionally, this class provides support for DTO resolution, Gson functionality, and proxy-related settings.
 */
public class BusquedaModel implements Serializable {

    private String entity;
    private Map<String, String> orders;
    private Map<String, Object> filters;
    private Map<String, Object> functions;
    private List<String> groupsBy;

    /**
     * Lista de columnas específicas a seleccionar en el query.
     * Soporta rutas con joins, ej: ["id", "nombre", "propietario.predio.claveCat"]
     * Si es null o vacío, se selecciona la entidad completa (SELECT *).
     */
    private List<String> columns;

    private Integer first;
    private Integer pageSize;
    private Boolean distinct = true;
    private Boolean unicoResultado = false;
    private Boolean unproxy = false;
    private Boolean gson = false;
    private Boolean resolverDto = false;

    private Boolean seek;            // Activar paginación por cursor
    private String cursorField;      // Campo ordenado (ruta con join si aplica)
    private String cursorDirection;  // ASC o DESC
    private Object cursorValue;      // Valor del cursor para el campo ordenado
    private Object cursorIdValue;    // Valor del cursor para el ID (desempate)

    private List<String> ignoreFieldsGson;
    private List<Class> ignoreClassGson;
    private List<String> ignoreClassGsonw;

    public BusquedaModel() {
    }

    public BusquedaModel(String entity) {
        this.entity = entity;
    }

    public static Builder builder(String entity) {
        return new Builder(entity);
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Map<String, String> getOrders() {
        return orders;
    }

    public void setOrders(Map<String, String> orders) {
        this.orders = orders;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        if (filters != null) {
            filters.replaceAll((key, value) -> {
                if (value instanceof Map) {
                    WhereCondition cond = null;
                    try {
                        cond = (WhereCondition) Utilities.toObjectFromJson(Utilities.toJson(value), WhereCondition.class);
                        if (cond != null && (cond.comparador != null || cond.values != null)) {
                            value = cond;
                            return cond;
                        } else {
                            return value;
                        }
                    } catch (Exception c) {
                        System.out.println("Error al extraer js desde Map key " + key + " vallue " + value + "-nValue " + value + " is map " + (value instanceof Map) + " cond " + cond);
                        return value;
                    }
                } else if (value.toString().startsWith("{") && value.toString().endsWith("}")) {
                    try {
                        WhereCondition cond = (WhereCondition) Utilities.toObjectFromJson(value, WhereCondition.class);
                        if (cond != null) {
                            value = cond;
                            return cond;
                        }
                    } catch (Exception c) {
                        System.out.println("Error al extraer js key " + key + " vallue " + value);
                    }
                }
                return value;
            });
        }
        this.filters = filters;
    }

    public Integer getFirst() {
        return first;
    }

    public void setFirst(Integer first) {
        this.first = first;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Boolean getDistinct() {
        if (this.distinct == null) {
            this.distinct = true;
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    public Map<String, Object> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, Object> functions) {
        this.functions = functions;
    }

    public List<String> getGroupsBy() {
        return groupsBy;
    }

    public void setGroupsBy(List<String> groupsBy) {
        this.groupsBy = groupsBy;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public Boolean getUnproxy() {
        return unproxy;
    }

    public void setUnproxy(Boolean unproxy) {
        this.unproxy = unproxy;
    }

    public Boolean getUnicoResultado() {
        if (unicoResultado == null) {
            return unicoResultado;
        }
        return unicoResultado;
    }

    public void setUnicoResultado(Boolean unicoResultado) {
        this.unicoResultado = unicoResultado;
    }

    public Boolean getGson() {
        return gson;
    }

    public void setGson(Boolean gson) {
        this.gson = gson;
    }

    public List<String> getIgnoreFieldsGson() {
        return ignoreFieldsGson;
    }

    public void setIgnoreFieldsGson(List<String> ignoreFieldsGson) {
        this.ignoreFieldsGson = ignoreFieldsGson;
    }

    public List<Class> getIgnoreClassGson() {
        return ignoreClassGson;
    }

    public void setIgnoreClassGson(List<Class> ignoreClassGson) {
        this.ignoreClassGson = ignoreClassGson;
    }

    public List<String> getIgnoreClassGsonw() {
        return ignoreClassGsonw;
    }

    public void setIgnoreClassGsonw(List<String> ignoreClassGsonw) {
        this.ignoreClassGsonw = ignoreClassGsonw;
    }

    public Boolean getResolverDto() {
        return resolverDto;
    }

    public void setResolverDto(Boolean resolverDto) {
        this.resolverDto = resolverDto;
    }

    /**
     * Activar paginación por cursor
     *
     * @return
     */
    public Boolean getSeek() {
        return seek;
    }

    /**
     * Activar paginación por cursor
     */
    public void setSeek(Boolean seek) {
        this.seek = seek;
    }

    /**
     * Campo ordenado (ruta con join si aplica)
     *
     * @return
     */
    public String getCursorField() {
        return cursorField;
    }

    /**
     * Campo ordenado (ruta con join si aplica)
     *
     * @param cursorField
     */
    public void setCursorField(String cursorField) {
        this.cursorField = cursorField;
    }

    /**
     * Direccion del curso: ASC o DESC
     *
     * @return
     */
    public String getCursorDirection() {
        return cursorDirection;
    }

    /**
     * Direccion del curso: ASC o DESC
     *
     * @param cursorDirection
     */
    public void setCursorDirection(String cursorDirection) {
        this.cursorDirection = cursorDirection;
    }

    /**
     * Valor del cursor para el campo ordenado
     *
     * @return
     */
    public Object getCursorValue() {
        return cursorValue;
    }

    /**
     * Valor del cursor para el campo ordenado
     *
     * @param cursorValue
     */
    public void setCursorValue(Object cursorValue) {
        this.cursorValue = cursorValue;
    }

    /**
     * Valor del cursor para el ID (desempate)
     *
     * @return
     */
    public Object getCursorIdValue() {
        return cursorIdValue;
    }

    /**
     * Valor del cursor para el ID (desempate)
     *
     * @param cursorIdValue
     */
    public void setCursorIdValue(Object cursorIdValue) {
        this.cursorIdValue = cursorIdValue;
    }

    @Override
    public String toString() {
        return "BusquedaDinamica{" + "entity='" + entity + '\'' + ", orders=" + orders + ", filters=" + filters + ", functions=" + functions + ", groupsBy=" + groupsBy + ", first=" + first + ", pageSize=" + pageSize + ", distinct=" + distinct + '}';
    }

    public static class WhereCondition {
        private static final Logger LOG = Logger.getLogger(WhereCondition.class.getName());
        private static volatile DateTimeSettings DATE_TIME_SETTINGS = DateTimeSettings.defaults();
        private String comparador = "EQ";
        private Boolean trim = true;
        private Boolean upper = true;
        private List<Object> values;

        public WhereCondition() {
        }

        public WhereCondition(List<Object> values) {
            this.values = values;
        }

        public WhereCondition(String comparador, List<Object> values) {
            this.comparador = comparador;
            this.values = values;
        }

        public static DateTimeSettings getDateTimeSettings() {
            return DATE_TIME_SETTINGS;
        }

        public static void setDateTimeSettings(DateTimeSettings settings) {
            if (settings == null) return;
            DATE_TIME_SETTINGS = settings;
        }

        public static WhereCondition eq(Object value) {
            WhereCondition wc = new WhereCondition();
            wc.setComparador(ComparatorOp.EQ.name());
            wc.setValues(List.of(value));
            return wc;
        }

        public static WhereCondition in(List<?> vals) {
            WhereCondition wc = new WhereCondition();
            wc.setComparador(ComparatorOp.IN.name());
            wc.setValues(new ArrayList<>(vals));
            return wc;
        }

        public static WhereCondition between(Object start, Object end) {
            WhereCondition wc = new WhereCondition();
            wc.setComparador(ComparatorOp.BETWEEN.name());
            wc.setValues(List.of(start, end));
            return wc;
        }

        public static WhereCondition in(List<?> vals, boolean upper, boolean trim) {
            WhereCondition wc = new WhereCondition();
            wc.setUpper(upper);
            wc.setTrim(trim);
            wc.setComparador(ComparatorOp.IN.name());
            wc.setValues(new ArrayList<>(vals));
            return wc;
        }

        public static WhereCondition gte(Object start) {
            WhereCondition wc = new WhereCondition();
            wc.setComparador(ComparatorOp.GE.name());
            wc.setValues(List.of(start));
            return wc;
        }

        public static WhereCondition isNotNull() {
            WhereCondition wc = new WhereCondition();
            wc.setComparador(ComparatorOp.IS_NOT_NULL.name());
            return wc;
        }

        public static WhereCondition isNull() {
            WhereCondition wc = new WhereCondition();
            wc.setComparador(ComparatorOp.IS_NULL.name());
            return wc;
        }

        public static WhereCondition like(Object value) {
            WhereCondition wc = new WhereCondition();
            wc.setComparador(ComparatorOp.LIKE.name());
            wc.setValues(List.of(value));
            return wc;
        }


        public String getComparador() {
            return comparador;
        }

        public void setComparador(String comparador) {
            this.comparador = comparador;
        }

        public ComparatorOp getComparatorOp() {
            return ComparatorOp.from(this.comparador);
        }

        public Boolean getTrim() {
            if (trim == null) {
                trim = true;
            }
            return trim;
        }

        public void setTrim(Boolean trim) {
            this.trim = trim;
        }

        public Boolean getUpper() {
            if (upper == null) {
                upper = true;
            }
            return upper;
        }

        public void setUpper(Boolean upper) {
            this.upper = upper;
        }

        public List<Object> getValues() {
            return values;
        }

        public void setValues(List<Object> values) {
            this.values = values;
        }

        public void validate() {
            ComparatorOp op = getComparatorOp();
            int size = values == null ? 0 : values.size();
            switch (op) {
                case BETWEEN -> {
                    if (size != 2) throw new IllegalArgumentException("BETWEEN requiere exactamente 2 valores");
                }
                case IN, NOT_IN -> {
                    if (size < 1) throw new IllegalArgumentException(op + " requiere al menos 1 valor");
                }
                case IS_NULL, IS_NOT_NULL -> { /* sin valores */ }
                default -> {
                    if (size < 1) throw new IllegalArgumentException(op + " requiere al menos 1 valor");
                }
            }
        }

        private String normalize(String s) {
            if (s == null) return null;
            String r = getTrim() ? s.trim() : s;
            if (getUpper()) r = r.toUpperCase(Locale.ROOT);
            return r;
        }

        private String escapeLike(String s, char escape) {
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '%' || c == '_' || c == escape) sb.append(escape);
                sb.append(c);
            }
            return sb.toString();
        }

        private String likePattern(String raw, String prefix, String suffix) {
            String base = normalize(raw);
            if (base == null) return null;
            return prefix + escapeLike(base, DATE_TIME_SETTINGS.likeEscape) + suffix;
        }

        private Date tryParseDate(String s, int indexInValues) {
            // 0) epoch millis
            try {
                long epoch = Long.parseLong(s);
                return new Date(epoch);
            } catch (NumberFormatException ignore) {
            }

            // 1) Instant ISO-8601
            try {
                Instant iso = Instant.parse(s);
                return Date.from(iso);
            } catch (Exception ignore) {
            }

            // 2) LocalDateTime con parsers configurados
            for (DateTimeFormatter f : DATE_TIME_SETTINGS.localDateTimeParsers) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(s, f);
                    ZonedDateTime zdt = ldt.atZone(DATE_TIME_SETTINGS.zoneId);
                    return Date.from(zdt.toInstant());
                } catch (Exception ignore) {
                }
            }

            // 3) LocalDate con parsers configurados
            for (DateTimeFormatter f : DATE_TIME_SETTINGS.localDateParsers) {
                try {
                    LocalDate ld = LocalDate.parse(s, f);
                    // Si es BETWEEN y es el segundo valor y está habilitado: fin de día
                    boolean endOfDay = getComparatorOp() == ComparatorOp.BETWEEN
                                       && indexInValues == 1
                                       && DATE_TIME_SETTINGS.endOfDayOnBetweenUpperBound;
                    ZonedDateTime zdt = endOfDay
                            ? ld.atTime(LocalTime.MAX).atZone(DATE_TIME_SETTINGS.zoneId)
                            : ld.atStartOfDay(DATE_TIME_SETTINGS.zoneId);
                    return Date.from(zdt.toInstant());
                } catch (Exception ignore) {
                }
            }

            // 4) Legado por compatibilidad
            try {
                java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return formato.parse(s);
            } catch (Exception ignore) {
            }

            return null;
        }

        @JsonIgnore
        public List<Object> getValuesCast(Class<?> fieldType) {
            if (values == null) return new ArrayList<>();

            List<Object> list = new ArrayList<>(values.size());
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (value == null) continue;
                String s = value.toString();
                if ("null".equalsIgnoreCase(s)) continue;

                try {
                    if (fieldType.equals(String.class)) {
                        list.add(normalize(s));
                        continue;
                    }
                    if (fieldType.equals(BigDecimal.class)) {
                        list.add(new BigDecimal(s));
                        continue;
                    }
                    if (fieldType.equals(BigInteger.class)) {
                        list.add(new BigInteger(s));
                        continue;
                    }
                    if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                        list.add(Long.valueOf(s));
                        continue;
                    }
                    if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                        list.add(Integer.valueOf(s));
                        continue;
                    }
                    if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
                        list.add(Short.valueOf(s));
                        continue;
                    }
                    if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
                        list.add(Byte.valueOf(s));
                        continue;
                    }
                    if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                        list.add(Double.valueOf(s));
                        continue;
                    }
                    if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                        list.add(Float.valueOf(s));
                        continue;
                    }
                    if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                        list.add(Boolean.valueOf(s));
                        continue;
                    }

                    if (fieldType.equals(LocalDate.class)) {
                        for (DateTimeFormatter f : DATE_TIME_SETTINGS.localDateParsers) {
                            try {
                                list.add(LocalDate.parse(s, f));
                                break;
                            } catch (Exception ignore) {
                            }
                        }
                        if (list.size() == i) list.add(value); // fallback si no parseó
                        continue;
                    }

                    if (fieldType.equals(LocalDateTime.class)) {
                        boolean parsed = false;
                        for (DateTimeFormatter f : DATE_TIME_SETTINGS.localDateTimeParsers) {
                            try {
                                list.add(LocalDateTime.parse(s, f));
                                parsed = true;
                                break;
                            } catch (Exception ignore) {
                            }
                        }
                        if (!parsed) list.add(value);
                        continue;
                    }

                    if (fieldType.equals(OffsetDateTime.class)) {
                        list.add(OffsetDateTime.parse(s));
                        continue;
                    }

                    if (fieldType.equals(Instant.class)) {
                        try {
                            long epoch = Long.parseLong(s);
                            list.add(Instant.ofEpochMilli(epoch));
                        } catch (NumberFormatException nfe) {
                            list.add(Instant.parse(s));
                        }
                        continue;
                    }

                    if (fieldType.equals(Date.class)) {
                        Date dt = tryParseDate(s, i);
                        list.add(dt != null ? dt : value);
                        continue;
                    }

                    if (Enum.class.isAssignableFrom(fieldType)) {
                        @SuppressWarnings("unchecked")
                        Class<? extends Enum> enumType = (Class<? extends Enum>) fieldType;
                        list.add(Enum.valueOf(enumType, s.trim()));
                        continue;
                    }

                    list.add(value);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "No se pudo castear el valor '" + s + "' a " + fieldType.getName(), e);
                    list.add(value);
                }
            }
            return list;
        }

        @Override
        public String toString() {
            return "WhereCondition{" + "comparador='" + comparador + '\'' + ", values=" + values + '}';
        }

        @JsonIgnore
        public String getValueLikes() {
            try {
                if (Utilities.isNotEmpty(values)) {
                    return likePattern(values.get(0).toString(), "%", "%");
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Error al obtener valor para like (contains)", e);
            }
            return null;
        }

        @JsonIgnore
        public String getValueStart() {
            try {
                if (Utilities.isNotEmpty(values)) {
                    return likePattern(values.get(0).toString(), "", "%");
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Error al obtener valor para like (startsWith)", e);
            }
            return null;
        }

        @JsonIgnore
        public String getValueEnd() {
            try {
                if (Utilities.isNotEmpty(values)) {
                    return likePattern(values.get(0).toString(), "%", "");
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Error al obtener valor para like (endsWith)", e);
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WhereCondition that)) return false;
            return Objects.equals(comparador, that.comparador)
                   && Objects.equals(getTrim(), that.getTrim())
                   && Objects.equals(getUpper(), that.getUpper())
                   && Objects.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(comparador, getTrim(), getUpper(), values);
        }

        public enum ComparatorOp {
            EQ, NE, GT, GE, LT, LE, LIKE, STARTS_WITH, ENDS_WITH, IN, NOT_IN, BETWEEN, IS_NULL, IS_NOT_NULL;

            public static ComparatorOp from(String raw) {
                if (raw == null || raw.isBlank()) return EQ;
                try {
                    return ComparatorOp.valueOf(raw.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    return EQ;
                }
            }
        }

        // Config global para fechas/horas y LIKE
        public static final class DateTimeSettings {
            private final ZoneId zoneId;
            private final List<DateTimeFormatter> localDateParsers;
            private final List<DateTimeFormatter> localDateTimeParsers;
            private final char likeEscape;
            private final boolean endOfDayOnBetweenUpperBound;

            private DateTimeSettings(ZoneId zoneId,
                                     List<DateTimeFormatter> localDateParsers,
                                     List<DateTimeFormatter> localDateTimeParsers,
                                     char likeEscape,
                                     boolean endOfDayOnBetweenUpperBound) {
                this.zoneId = zoneId;
                this.localDateParsers = localDateParsers;
                this.localDateTimeParsers = localDateTimeParsers;
                this.likeEscape = likeEscape;
                this.endOfDayOnBetweenUpperBound = endOfDayOnBetweenUpperBound;
            }

            public static Builder builder() {
                return new Builder();
            }

            public static DateTimeSettings defaults() {
                return builder()
                        .zoneId(ZoneId.systemDefault())
                        // LocalDate: ISO y algunos legados frecuentes
                        .addLocalDatePattern("yyyy-MM-dd")
                        .addLocalDatePattern("dd/MM/yyyy")
                        // LocalDateTime: ISO y legados frecuentes
                        .addLocalDateTimePattern("yyyy-MM-dd'T'HH:mm:ss")
                        .addLocalDateTimePattern("yyyy-MM-dd HH:mm:ss")
                        .addLocalDateTimePattern("dd/MM/yyyy HH:mm:ss")
                        .likeEscape('\\')
                        .endOfDayOnBetweenUpperBound(true)
                        .build();
            }

            public static final class Builder {
                private final List<DateTimeFormatter> localDateParsers = new ArrayList<>();
                private final List<DateTimeFormatter> localDateTimeParsers = new ArrayList<>();
                private ZoneId zoneId = ZoneId.systemDefault();
                private char likeEscape = '\\';
                private boolean endOfDayOnBetweenUpperBound = true;

                public Builder zoneId(ZoneId zoneId) {
                    this.zoneId = zoneId;
                    return this;
                }

                public Builder addLocalDatePattern(String pattern) {
                    localDateParsers.add(DateTimeFormatter.ofPattern(pattern));
                    return this;
                }

                public Builder addLocalDateTimePattern(String pattern) {
                    localDateTimeParsers.add(DateTimeFormatter.ofPattern(pattern));
                    return this;
                }

                public Builder likeEscape(char escape) {
                    this.likeEscape = escape;
                    return this;
                }

                public Builder endOfDayOnBetweenUpperBound(boolean enabled) {
                    this.endOfDayOnBetweenUpperBound = enabled;
                    return this;
                }

                public DateTimeSettings build() {
                    // Siempre incluir ISO por defecto
                    List<DateTimeFormatter> ld = new ArrayList<>();
                    ld.add(DateTimeFormatter.ISO_LOCAL_DATE);
                    ld.addAll(localDateParsers);

                    List<DateTimeFormatter> ldt = new ArrayList<>();
                    ldt.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    ldt.addAll(localDateTimeParsers);

                    return new DateTimeSettings(zoneId, ld, ldt, likeEscape, endOfDayOnBetweenUpperBound);
                }
            }
        }
    }


    public static final class Builder {

        private BusquedaModel b;

        /**
         * Constructs a new Builder instance for initializing a BusquedaModel
         * with the specified entity.
         *
         * @param entity the entity name or identifier used to initialize the BusquedaModel
         */
        public Builder(String entity) {
            b = new BusquedaModel(entity);
        }

        /**
         * Sets the "first" value in the underlying model and returns the Builder instance.
         *
         * @param first the value to be set as the first element in the model
         * @return the Builder instance, enabling method chaining
         */
        public Builder first(Integer first) {
            b.setFirst(first);
            return this;
        }

        /**
         * Sets the page size for the underlying model and returns the Builder instance.
         *
         * @param pageSize the size of the page to be set in the model
         * @return the Builder instance, enabling method chaining
         */
        public Builder pageSize(Integer pageSize) {
            b.setPageSize(pageSize);
            return this;
        }

        /**
         * Sets the distinct flag for the page size configuration in the underlying model.
         *
         * @param distinct a Boolean value indicating whether the results should be distinct
         * @return the Builder instance, enabling method chaining
         */
        public Builder pageSize(Boolean distinct) {
            b.setDistinct(distinct);
            return this;
        }

        /**
         * Adds a filter condition to the filter map for the specified field.
         * Initializes the filter map if it is not already created. The specified
         * field and value are stored as a key-value pair in the filter map.
         *
         * @param field the name of
         */
        public Builder where(String field, Object value) {
            if (b.getFilters() == null) {
                b.setFilters(new LinkedHashMap<>());
            }
            b.getFilters().put(field, value);
            return this;
        }

        /**
         * Adds a condition to the current set of filters for a specific field.
         * If the filters map is not initialized, it will be created.
         * The specified field and its associated condition are added to the filters map.
         *
         * @param field the name of the field to which the condition applies
         * @param value the {@code WhereCondition} to apply to the specified field
         * @return the Builder instance, enabling method chaining
         */
        public Builder whereCondition(String field, WhereCondition value) {
            if (b.getFilters() == null) {
                b.setFilters(new LinkedHashMap<>());
            }
            b.getFilters().put(field, value);
            return this;
        }

        /**
         * Adds an order criterion to the Builder instance by specifying a field and its order.
         * If no order criteria exist, a new map is initialized to store the order criteria.
         *
         * @param field the name of the field on which to apply the order
         * @param order the sorting order to be applied (e.g., ascending
         */
        public Builder order(String field, String order) {
            if (b.getOrders() == null) {
                b.setOrders(new LinkedHashMap<>());
            }
            b.getOrders().put(field, order);
            return this;
        }

        /**
         * Adds a function mapping to the Builder instance with the specified field and order.
         * If no function mappings exist, a new map is initialized.
         *
         * @param field the name of the field to be included in the functions mapping
         * @param order the order associated with the specified field (e.g., ascending, descending)
         * @return the Builder instance, enabling method chaining
         */
        public Builder functions(String field, String order) {
            if (b.getFunctions() == null) {
                b.setFunctions(new LinkedHashMap<>());
            }
            b.getFunctions().put(field, order);
            return this;
        }

        /**
         * Sets the distinct flag in the underlying model to indicate
         * whether the resultant query should only return distinct results.
         *
         * @param distinct a Boolean value indicating whether the query should retrieve distinct results
         * @return the Builder instance, enabling method chaining
         */
        public Builder distinct(Boolean distinct) {
            b.setDistinct(distinct);
            return this;
        }

        /**
         * Sets the "unicoResultado" flag in the underlying model to specify whether
         * the query should return a unique result or not.
         *
         * @param unicoResultado a Boolean value indicating
         */
        public Builder unicoResultado(Boolean unicoResultado) {
            b.setUnicoResultado(unicoResultado);
            return this;
        }

        /**
         * @param cursorDirection
         * @return
         */
        public Builder cursorDirection(String cursorDirection) {
            b.setCursorDirection(cursorDirection);
            return this;
        }

        /**
         * Sets the cursor field used for ordering or pagination.
         *
         * @param cursorField the name of the cursor field to be set
         * @return the Builder instance, enabling method chaining
         */
        public Builder cursorField(String cursorField) {
            b.setCursorField(cursorField);
            return this;
        }

        /**
         * Sets the cursor value used for pagination or ordered field traversal.
         *
         * @param cursorValue the value to be set as the cursor position
         * @return the Builder instance, enabling method chaining
         */
        public Builder cursorValue(Object cursorValue) {
            b.setCursorValue(cursorValue);
            return this;
        }

        /**
         * Sets the cursor ID value for disambiguation purposes in the {@link BusquedaModel}.
         *
         * @param cursorIdValue the value to be set as the cursor ID
         * @return the Builder instance, for method chaining
         */
        public Builder cursorIdValue(Object cursorIdValue) {
            b.setCursorIdValue(cursorIdValue);
            return this;
        }

        /**
         * Sets the columns to select in the query.
         * Supports join paths like "propietario.predio.claveCat".
         *
         * @param columns the list of column paths to select
         * @return the Builder instance, enabling method chaining
         */
        public Builder columns(String... columns) {
            b.setColumns(Arrays.asList(columns));
            return this;
        }

        /**
         * Sets the columns to select in the query.
         *
         * @param columns the list of column paths to select
         * @return the Builder instance, enabling method chaining
         */
        public Builder columns(List<String> columns) {
            b.setColumns(columns);
            return this;
        }

        /**
         * Builds and returns the configured instance of {@link BusquedaModel}.
         *
         * @return the constructed instance of {@link BusquedaModel}.
         */
        public BusquedaModel build() {
            return b;
        }
    }

}