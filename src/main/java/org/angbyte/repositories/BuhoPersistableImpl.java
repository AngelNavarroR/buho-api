package org.angbyte.repositories;


import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.angbyte.config.BuhoProperties;
import org.angbyte.model.BusquedaModel;
import org.angbyte.model.EjecucionFuncion;
import org.angbyte.model.GuardarModel;
import org.angbyte.model.Respuesta;
import org.angbyte.utils.BuhoCache;
import org.angbyte.utils.Messages;
import org.angbyte.utils.Utilities;
import org.antlr.v4.runtime.misc.NotNull;
import org.hibernate.Hibernate;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository
public class BuhoPersistableImpl<T> implements BuhoPersistable {

    private static final int DEFAULT_FETCH_SIZE = 500;
    private static final int DEFAULT_QUERY_TIMEOUT_MS = 10000;
    private static final int JDBC_BATCH_SIZE = 50;


    private static final String ENTITY_PREFIX = "entitys_";
    private static final String CLASS_TRANSLATION_PREFIX = "clazz_trans_";
    private final static Logger LOGGER = Logger.getLogger(BuhoPersistableImpl.class.getName());
    private final BuhoProperties buhoProperties;
    private final BuhoCache cache;
    @PersistenceContext
    private EntityManager entityManager;
    private Map<Long, Map<String, Object>> joinMappings;
    private Map<Long, Map<String, EntityType>> entityTypes;

    public BuhoPersistableImpl(BuhoCache cache, BuhoProperties buhoProperties) {
        this.cache = cache;
        this.buhoProperties = buhoProperties;
    }

    /**
     * Initializes the repository and sets up entity model caching.
     */
    @PostConstruct
    public void init() {
        refreshEntityModelCache(null);
        joinMappings = new LinkedHashMap<>();
        entityTypes = new LinkedHashMap<>();
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @NotNull
    protected Long getIdTrans() {
        return (long) (Math.random() * 100000);
    }

    /**
     * Refreshes the cache with entity models from the EntityManager.
     * Creates a mapping of entity names to their corresponding EntityType.
     *
     * @return
     */
    private EntityType refreshEntityModelCache(String entityName) {
        try {
            if (entityManager == null) {
                LOGGER.log(Level.WARNING, "EntityManager is not initialized");
                return null;
            }
            AtomicReference<EntityType> entityTypeC = new AtomicReference<>();
            StringBuffer sb = new StringBuffer();
            AtomicReference<Integer> count = new AtomicReference<>(0);
            entityManager.getMetamodel().getEntities().forEach(entityType -> {
                String cacheKey = ENTITY_PREFIX + entityType.getName();
                if (entityType.getName().equalsIgnoreCase(entityName)) {
                    entityTypeC.set(entityType);
                }
                if (!cache.containsKey(cacheKey)) {
                    cache.add(cacheKey, entityType);
                    if (buhoProperties.isDebug()) sb.append(entityType.getName()).append(", ");
                }
                count.getAndSet(count.get() + 1);
            });

            if (buhoProperties.isDebug()) LOGGER.log(Level.INFO, "Entity Models Cache: {0} entidades agregadas a la cache {1}", new Object[]{sb.toString(), count});

            return entityTypeC.get();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error refreshing entity model cache", e);
        }
        return null;
    }

    private void removeTranssaction(Long transactionId) {
        joinMappings.remove(transactionId);
        entityTypes.remove(transactionId);
    }

    private boolean isMultiColumnQuery(BusquedaModel searchCriteria) {
        return (searchCriteria.getFunctions() != null && searchCriteria.getFunctions().size() > 1)
               || (searchCriteria.getColumns() != null && !searchCriteria.getColumns().isEmpty());
    }

    private EntityType<?> getEntityTypeFromCache(String entityName, Long idTrans) {
        // ═══════════════════════════════════════════════════════════════
        // FIX: Usar entityName como segunda clave del cache.
        //
        // ANTES: entityTypes era Map<Long, EntityType> — UNA sola entidad
        // por transacción. Si la query involucraba JOINs (ej:
        // CatEscrituraPropietario → propietario → predio → claveCat),
        // la primera entidad cacheada (CatEscrituraPropietario) se
        // retornaba para TODAS las búsquedas → getAttribute("claveCat")
        // fallaba en CatEscrituraPropietario en vez de buscarlo en CatPredio.
        //
        // AHORA: entityTypes es Map<Long, Map<String, EntityType>>,
        // permite cachear múltiples EntityTypes por transacción.
        // ═══════════════════════════════════════════════════════════════
        Map<String, EntityType> transCache = entityTypes.computeIfAbsent(idTrans, k -> new LinkedHashMap<>());
        if (transCache.containsKey(entityName)) {
            return transCache.get(entityName);
        }

        String nameEntiti = ENTITY_PREFIX + entityName;
        EntityType<?> entityType = null;
        if (!cache.containsKey(nameEntiti)) {
            entityType = this.refreshEntityModelCache(entityName);
            Utilities.logs(this, "Iniciando cache de Entities: ");
        }
        Utilities.logs(this, "Buscando cache " + nameEntiti + " Entities en cache: " + cache.size());
        if (entityType == null) {
            entityType = (EntityType<?>) cache.get(nameEntiti);
        }
        if (entityType != null) {
            transCache.put(entityName, entityType);
        }
        return entityType;
    }

    private Class<?> getDomainClass(EntityType<?> entityType, String entityName, BusquedaModel searchCriteria) {
        if (entityType == null) {
            LOGGER.log(Level.INFO, "Entity not found in cache model: {0} with parameters {1}", new Object[]{entityName, searchCriteria});
            throw new EntityNotFoundException("Entity not found: " + entityName);
        }

        Class<?> domainClass = entityType.getJavaType();
        if (domainClass == null) {
            LOGGER.log(Level.INFO, "Java type not found for entity: {0} with parameters {1}", new Object[]{entityName, searchCriteria});
            throw new EntityNotFoundException("Java type not found for entity: " + entityName);
        }
        Utilities.logs(this, "Case encontrada: " + domainClass.getSimpleName());
        return domainClass;
    }

    private void updateJoinMappings(Long transactionId, Class<?> domainClass) {
        Map<String, Object> domainMapping = joinMappings.computeIfAbsent(transactionId, k -> new LinkedHashMap<>());
        domainMapping.put(CLASS_TRANSLATION_PREFIX + transactionId, domainClass);
        Utilities.logs(this, "JoinMappings peticion: " + transactionId);
    }

    private CriteriaQuery<?> createAppropriateQuery(CriteriaBuilder builder, Class<?> domainClass, boolean isMultiColumnQuery) {
        return isMultiColumnQuery ? builder.createTupleQuery() : builder.createQuery(domainClass);
    }

    private TypedQuery<?> criteriaDistinct(BusquedaModel filtros, CriteriaQuery<?> query) {
        try {
            if (Boolean.TRUE.equals(filtros.getDistinct())) {
                query.distinct(true);
            }

            TypedQuery<?> typedQuery = getEntityManager().createQuery(query);

            Optional.ofNullable(filtros.getFirst()).ifPresent(typedQuery::setFirstResult);

            Optional.ofNullable(filtros.getPageSize()).ifPresent(typedQuery::setMaxResults);

            // Hints de alto volumen
            typedQuery.setFlushMode(FlushModeType.COMMIT);
            typedQuery.setHint("org.hibernate.readOnly", Boolean.TRUE);
            typedQuery.setHint("org.hibernate.cacheable", Boolean.FALSE);

            try {
                org.hibernate.query.Query<?> hq = typedQuery.unwrap(org.hibernate.query.Query.class);
                hq.setReadOnly(true);
                hq.setFetchSize(DEFAULT_FETCH_SIZE);
                hq.setTimeout(Math.max(1, DEFAULT_QUERY_TIMEOUT_MS / 1000));
            } catch (Exception ignore) {
                if (buhoProperties.isDebug()) LOGGER.log(Level.INFO, "Error al settear hints para la consulta: {0}", ignore.getMessage());
            }

            return typedQuery;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating TypedQuery", e);
            throw new IllegalStateException("Failed to create TypedQuery", e);
        }
    }

    private void ensureStableOrder(Long idTrans, BusquedaModel busq, Root<?> root) {
        try {
            Map<String, String> orders = busq.getOrders();
            if (orders == null || orders.isEmpty()) return;

            EntityType<?> entityType = getEntityTypeFromCache(root.getJavaType().getSimpleName(), idTrans);
            if (entityType == null) return;

            String idName = entityType.getId(entityType.getIdType().getJavaType()).getName();

            boolean containsId = orders.keySet().stream().map(k -> Utilities.getUltimaPosicion(k, "\\.")).anyMatch(k -> k.equals(idName));

            if (!containsId) {
                String dir = orders.values().stream().findFirst().orElse("ASC");
                orders.put(idName, dir);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "ensureStableOrder error", e);
        }
    }

    private void applySeekPagination(Long idTrans, BusquedaModel busq, CriteriaBuilder builder, CriteriaQuery<?> query, Root<?> root) {
        if (!Boolean.TRUE.equals(busq.getSeek())) return;
        if (busq.getCursorField() == null || busq.getCursorDirection() == null || busq.getCursorValue() == null) return;

        try {
            String key = busq.getCursorField();
            String nameField = Utilities.getUltimaPosicion(key, "\\.");
            From<?, ?> join = createJoin(idTrans, key, root);

            EntityType<?> et = getEntityTypeFromCache(join.getJavaType().getSimpleName(), idTrans);
            if (et == null) return;
            Attribute<?, ?> metaField = et.getAttribute(nameField);
            Class<?> fieldType = metaField.getJavaType();

            Object cursorVal = castToType(fieldType, busq.getCursorValue());

            EntityType<?> rootEt = getEntityTypeFromCache(root.getJavaType().getSimpleName(), idTrans);
            SingularAttribute<?, ?> idAttr = rootEt.getId(rootEt.getIdType().getJavaType());
            String idName = idAttr.getName();
            Path<?> idPath = root.get(idName);

            Predicate seekPredicate;

            Object cursorIdVal = busq.getCursorIdValue();
            if (cursorIdVal != null) {
                Object castIdVal = castToType(idAttr.getJavaType(), cursorIdVal);
                Predicate main;
                Predicate tie;
                Predicate eq;

                if ("ASC".equalsIgnoreCase(busq.getCursorDirection())) {
                    main = builder.greaterThan(join.get(nameField), (Comparable) cursorVal);
                    tie = builder.greaterThan((Expression<? extends Comparable>) idPath, (Comparable) castIdVal);
                } else {
                    main = builder.lessThan(join.get(nameField), (Comparable) cursorVal);
                    tie = builder.lessThan((Expression<? extends Comparable>) idPath, (Comparable) castIdVal);
                }
                eq = builder.equal(join.get(nameField), cursorVal);
                seekPredicate = builder.or(main, builder.and(eq, tie));
            } else {
                if ("ASC".equalsIgnoreCase(busq.getCursorDirection())) {
                    seekPredicate = builder.greaterThan(join.get(nameField), (Comparable) cursorVal);
                } else {
                    seekPredicate = builder.lessThan(join.get(nameField), (Comparable) cursorVal);
                }
            }

            Predicate existing = query.getRestriction();
            if (existing != null) {
                query.where(builder.and(existing, seekPredicate));
            } else {
                query.where(seekPredicate);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "applySeekPagination error", e);
        }
    }

    private Object castToType(Class<?> type, Object val) {
        if (val == null) return null;
        try {
            if (type == String.class) return val.toString();
            if (type == Integer.class || type == int.class) return Integer.valueOf(val.toString());
            if (type == Long.class || type == long.class) return Long.valueOf(val.toString());
            if (type == Double.class || type == double.class) return Double.valueOf(val.toString());
            if (type == Float.class || type == float.class) return Float.valueOf(val.toString());
            if (type == Short.class || type == short.class) return Short.valueOf(val.toString());
            if (type == Byte.class || type == byte.class) return Byte.valueOf(val.toString());
            if (type == Boolean.class || type == boolean.class) return Boolean.valueOf(val.toString());
            if (type == java.math.BigDecimal.class) return new java.math.BigDecimal(val.toString());
            if (type == java.util.Date.class) {
                if (val instanceof java.util.Date) return val;
                try {
//                    return DatatypeConverter.parseDateTime(val.toString()).getTime();
                    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(val.toString());
                } catch (Exception ignore) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(val.toString());
                }
            }
            return val;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "castToType fallback " + type + " val: " + val, e);
            return val;
        }
    }


    private <T> List getList(BusquedaModel busq, boolean isTuple, CriteriaQuery query) {
        TypedQuery tq = criteriaDistinct(busq, query);
        if (tq == null) {
            return null;
        }
        List tuples = tq.getResultList();
        if (isTuple) {
            List<Map<String, Object>> resultp = new ArrayList<>();
            for (Tuple single : (List<Tuple>) tuples) {
                Map<String, Object> tempMap = new LinkedHashMap<>();
                single.getElements().forEach((element) -> {
                    String alias = element.getAlias();
                    if (alias != null && !alias.isEmpty()) {
                        tempMap.put(alias, single.get(alias));
                    } else {
                        // Fallback: usar índice si no hay alias
                        int idx = tempMap.size();
                        try {
                            tempMap.put("col_" + idx, single.get(idx));
                        } catch (Exception ignore) {
                            // Elemento sin acceso por índice
                        }
                    }
                });
                resultp.add(tempMap);
            }
            if (resultp.size() == 0) {
                return null;
            }
            if (busq.getUnproxy()) {
                Hibernate.unproxy(resultp);
            }
            return (List<T>) resultp;
        } else {
            if (Boolean.TRUE.equals(busq.getResolverDto())) {
                try {
                    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
                    scanner.addIncludeFilter(new AnnotationTypeFilter(Mapper.class));

                    for (BeanDefinition bd : scanner.findCandidateComponents("org.ventanilla.interna.mappers")) {
                        Class aClass = Class.forName(bd.getBeanClassName());
                        String classMapper = busq.getEntity() + "Mapper";
                        if (aClass.getSimpleName().startsWith(classMapper)) {
                            Object instance = aClass.newInstance();
                            Method[] method = aClass.getMethods();
                            for (Method m : method) {
                                Class<?>[] types = m.getParameterTypes();
                                if ("toDto".equals(m.getName()) && types[0].equals(List.class)) {
                                    System.out.println(aClass.getSimpleName() + " Encontrado " + m + " Lista toDto: " + (tuples == null ? "0" : tuples.size()));
                                    List rsdto = (List) m.invoke(instance, tuples);
                                    return rsdto;
                                }

                            }

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                return tuples;
            } else {
                return tuples;
            }
        }
    }

    private From createJoin(Long idTrans, String key, Root from) {
        try {
            Map<String, Object> j = joinMappings.get(idTrans);
            if (j == null) {
                j = new LinkedHashMap<>();
            }
            String joinKey = key.contains(".") ? key.substring(0, key.lastIndexOf(".")) : key;

            if (j.containsKey(joinKey)) {
                return (From) j.get(joinKey);
            }

            From join = from;
            String[] pathParts = key.split("\\.");
            if (pathParts.length <= 1) {
                return from;
            }

            StringBuilder currentPath = new StringBuilder();

            for (int i = 0; i < pathParts.length - 1; i++) {
                String part = pathParts[i];

                if (currentPath.length() > 0) currentPath.append(".");
                currentPath.append(part);
                String currentPathStr = currentPath.toString();

                if (j.containsKey(currentPathStr)) {
                    join = (From) j.get(currentPathStr);
                    continue;
                }

                Class<?> attributeType = join.get(part).getJavaType();

                boolean isEntity = attributeType.isAnnotationPresent(Entity.class) &&
                                   !isBasicType(attributeType) &&
                                   !attributeType.isAnnotationPresent(Embeddable.class) &&
                                   !attributeType.getSimpleName().endsWith("PK");

                if (isEntity) {
                    join = join.join(part, JoinType.INNER);
                    LOGGER.fine("Created JOIN: " + currentPathStr + " -> " + join.getJavaType().getSimpleName());
                } else {
                    LOGGER.fine("Skip JOIN for: " + currentPathStr + " (type: " + attributeType.getSimpleName() + ")");
                }
                j.put(currentPathStr, join);
            }

            joinMappings.put(idTrans, j);
            return join;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "findProperty key " + key, e);
        }
        return null;
    }

    private boolean isBasicType(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Embeddable.class) ||
            clazz.isAnnotationPresent(Entity.class)) {
            return false;
        }

        if (clazz.isPrimitive() || isWrapperType(clazz)) {
            return true;
        }

        if (clazz.equals(String.class) || clazz.equals(Character.class)) {
            return true;
        }

        if (clazz.isEnum()) {
            return true;
        }

        if (Number.class.isAssignableFrom(clazz) ||
            clazz.equals(java.math.BigDecimal.class) ||
            clazz.equals(java.math.BigInteger.class)) {
            return true;
        }

        if (isTemporalType(clazz)) {
            return true;
        }

        if (clazz.equals(java.util.UUID.class) ||
            clazz.equals(java.util.Locale.class)) {
            return true;
        }

        return false;
    }

    private boolean isWrapperType(Class<?> clazz) {
        return clazz.equals(Boolean.class) ||
               clazz.equals(Byte.class) ||
               clazz.equals(Short.class) ||
               clazz.equals(Integer.class) ||
               clazz.equals(Long.class) ||
               clazz.equals(Float.class) ||
               clazz.equals(Double.class) ||
               clazz.equals(Character.class);
    }

    private boolean isTemporalType(Class<?> clazz) {
        return java.util.Date.class.isAssignableFrom(clazz) ||
               java.time.temporal.Temporal.class.isAssignableFrom(clazz) ||
               clazz.equals(java.time.LocalDate.class) ||
               clazz.equals(java.time.LocalDateTime.class) ||
               clazz.equals(java.time.LocalTime.class) ||
               clazz.equals(java.time.OffsetDateTime.class) ||
               clazz.equals(java.time.ZonedDateTime.class) ||
               clazz.equals(java.time.Instant.class) ||
               clazz.equals(java.time.Year.class) ||
               clazz.equals(java.time.Month.class) ||
               clazz.equals(java.time.YearMonth.class);
    }


    private void processGroupBy(Long idTrans, BusquedaModel busq, CriteriaQuery query, Root from) {
        try {
            if (busq.getGroupsBy() != null && busq.getGroupsBy().size() > 0) {
                List<Expression<Object>> groups = new ArrayList<>(busq.getFunctions().size());
                busq.getGroupsBy().forEach((key) -> {
                    if (!(key.trim() == "")) {
                        String nameField = Utilities.getUltimaPosicion(key, "\\.");
                        From join = from;
                        try {
                            join = createJoin(idTrans, key, from);
                            groups.add(join.get(nameField));
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "findProperty key " + key, e);
                        }
                    }
                });
                query.groupBy(groups);
                Utilities.logs(this, "Procesando grupos de agrupacion");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    /**
     * Procesa el mapa de parametro con los ordenamientos para realizar la consulta.
     *
     * @param idTrans
     * @param builder Permite crear las predicados para la conculta.
     * @param from    Root donde se va realizar la selección de la tabla.
     * @param orders  Mapa de parametros con los filtro para los predicados.
     * @param query   Objeto con selección de la tabla
     */
    private void processOrderBY(Long idTrans, CriteriaBuilder builder, Root from, Map<String, String> orders, CriteriaQuery query) {
        try {
            if (orders == null) {
                return;
            }
            if (orders.size() == 0) {
                return;
            }
            List<Order> ordersby = new ArrayList<>(orders.size());
            orders.entrySet().forEach((Map.Entry<String, String> entry) -> {
                String key = entry.getKey();
                if (!(key.trim() == "")) {
                    String nameField = Utilities.getUltimaPosicion(key, "\\.");
                    String func = null;
                    if (nameField.contains(":")) {
                        String[] split = nameField.split(":");
                        nameField = split[0];
                        func = split[1];
                    }
                    From join = from;
                    try {
                        join = createJoin(idTrans, key, from);
                        if ("ASC".equalsIgnoreCase(entry.getValue())) {
                            if (func == null) {
                                ordersby.add(builder.asc(join.get(nameField)));
                            } else {
                                Expression length = null;
                                if (func.toLowerCase().contains("length") && func.toLowerCase().contains("trim")) {
                                    length = builder.length(builder.trim(join.get(nameField)));
                                } else if (func.toLowerCase().contains("length")) {
                                    length = builder.length(join.get(nameField));
                                } else if (func.toLowerCase().contains("trim")) {
                                    length = builder.trim(join.get(nameField));
                                }
                                if (length == null) {
                                    ordersby.add(builder.asc(join.get(nameField)));
                                } else {
                                    ordersby.add(builder.asc(length));
                                }
                            }
                        } else {
                            if (func == null) {
                                ordersby.add(builder.desc(join.get(nameField)));
                            } else {
                                Expression length = null;
                                if (func.toLowerCase().contains("length") && func.toLowerCase().contains("trim")) {
                                    length = builder.length(builder.trim(join.get(nameField)));
                                } else if (func.toLowerCase().contains("length")) {
                                    length = builder.length(join.get(nameField));
                                } else if (func.toLowerCase().contains("trim")) {
                                    length = builder.trim(join.get(nameField));
                                }

                                if (length == null) {
                                    ordersby.add(builder.desc(join.get(nameField)));
                                } else {
                                    ordersby.add(builder.desc(length));
                                }
                            }
                        }

                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "findProperty key " + key, e);
                    }
                }
            });
            if (ordersby.size() > 0) {
                query.orderBy(ordersby);
                Utilities.logs(this, "Procesando ordenaciones " + ordersby.size());
            }
            Utilities.logs(this, "Sin ordenaciones");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    private void processFunction(Long idTrans, BusquedaModel busq, CriteriaBuilder builder, CriteriaQuery query, Root from) {
        try {
            if (busq.getFunctions() != null && busq.getFunctions().size() > 0) {
                List<Expression<Object>> multSele = new ArrayList<>(busq.getFunctions().size());
                busq.getFunctions().forEach((key, value) -> {
                    List<Object> vals = new ArrayList<>();
                    if (value instanceof Collection) {
                        vals.addAll((Collection<?>) value);
                    } else {
                        vals.add(value.toString());
                    }
                    String nameField = "";
                    From join = from;
                    if (vals.size() > 0) {
                        int count_ = 0;
                        Expression[] c = new Expression[vals.size()];
                        for (Object ob : vals) {
                            nameField = Utilities.getUltimaPosicion(ob + "", "\\.");
                            join = createJoin(idTrans, ob + "", from);
                            Path path = null;
                            try {
                                path = join.get(nameField);
                            } catch (Exception v) {
                                System.out.println(v.getMessage());
                            }
                            if (path != null) {
                                c[count_] = path;
                            } else {
                                c[count_] = builder.literal(nameField);
                            }
                            count_++;
                        }
                        Class clazz = Object.class;
                        if (key.equalsIgnoreCase(value.toString())) {
                            multSele.addAll(Arrays.<Expression<Object>>asList(c));
                        } else {
                            LOGGER.log(Level.INFO, "processFunction Key " + key + " clazz " + clazz + " c " + c);
                            multSele.add(builder.function(key, clazz, c));
                        }
                    }
                });
                query.multiselect(multSele);
                Utilities.logs(this, "Procesando columnas");

            } else if (busq.getColumns() != null && !busq.getColumns().isEmpty()) {
                // ═══════════════════════════════════════════════════════════════
                // COLUMNS: Selección de columnas específicas.
                // Soporta rutas con joins: "propietario.predio.claveCat"
                // Genera: SELECT col1, col2, col3 ... en vez de SELECT *
                // Resultado: List<Map<String, Object>> con alias como keys
                // ═══════════════════════════════════════════════════════════════
                List<Selection<?>> selections = new ArrayList<>(busq.getColumns().size());
                for (String col : busq.getColumns()) {
                    String nameField = Utilities.getUltimaPosicion(col, "\\.");
                    From join = createJoin(idTrans, col, from);
                    if (join == null) {
                        join = from;
                    }
                    try {
                        // Usar el nombre completo como alias para identificar en el resultado
                        Path path = join.get(nameField);
                        selections.add(path.alias(col));
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Column no encontrada: " + col + " en " + join.getJavaType().getSimpleName(), e);
                    }
                }
                if (!selections.isEmpty()) {
                    query.multiselect(selections);
                    Utilities.logs(this, "Procesando columns: " + busq.getColumns().size() + " columnas seleccionadas");
                } else {
                    Utilities.logs(this, "Ninguna columna válida, seleccionando entidad completa");
                    query.select(from);
                }

            } else {

                Utilities.logs(this, "Procesando toda la entidad");
                query.select(from);
            }
        } catch (Exception f) {
            LOGGER.log(Level.SEVERE, "", f);
        }
    }

    /**
     * Procesa el mapa de parametro con los filtras para realizar los predicados de la consulta.
     *
     * @param idTrans
     * @param builder Permite crear las predicados para la conculta.
     * @param from    Root donde se va realizar la selección de la tabla.
     * @param filtros Mapa de parametros con los filtro para los predicados.
     * @param query   Objeto con selección de la tabla
     * @return
     */
    private Predicate[] processWhere(Long idTrans, CriteriaBuilder builder, Root from, Map<String, Object> filtros, CriteriaQuery query) {
        try {
            List<Predicate> predicates = new ArrayList<>();
            if (filtros == null) {
                Utilities.logs(this, "Predicates agregados al query 0");
                return new Predicate[0];
            }
            if (filtros.size() == 0) {
                Utilities.logs(this, "Predicates agregados al query 0");
                return new Predicate[0];
            }
            filtros.entrySet().forEach((Map.Entry<String, Object> entry) -> {
                String key = entry.getKey();
                if (!(key.trim() == "")) {
                    BusquedaModel.WhereCondition condicion = null;
                    if (entry.getValue() instanceof BusquedaModel.WhereCondition) {
                        condicion = (BusquedaModel.WhereCondition) entry.getValue();
                    } else {
                        condicion = new BusquedaModel.WhereCondition(Arrays.asList(entry.getValue()));
                    }
                    Predicate findProperty = null;
                    From join = from;
                    String nameField = Utilities.getUltimaPosicion(key, "\\.");
                    try {
                        join = createJoin(idTrans, key, from);
                        if (join == null) {
                            LOGGER.log(Level.WARNING, "createJoin retornó null para key: " + key + ", usando root");
                            join = from;
                        }
                        findProperty = getPredicateField(idTrans, builder, nameField, join, condicion, from, null);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error al buscar NameField " + nameField + " condicion " + condicion
                                                  + " Join type " + (join != null ? join.getJavaType().getSimpleName() : "null"), e);
                    }
                    if (findProperty != null) {
                        predicates.add(findProperty);
                    }
                }
            });
            if (predicates.size() > 0) {
                Predicate[] result = new Predicate[predicates.size()];
                result = predicates.toArray(result);
                query.where(predicates.toArray(result));
                Utilities.logs(this, "Predicates agregados al query : " + predicates.size());
                return result;
            }
            Utilities.logs(this, "Predicates agregados al query 0");
        } catch (Exception e) {
            System.out.println("BusquedaDinamica " + filtros);
            LOGGER.log(Level.SEVERE, "", e);
        }
        return null;
    }

    /**
     * Procesa los predicados para la consulta.
     *
     * @param idTrans
     * @param builder   Permite crear las predicados para la conculta.
     * @param nameField Nombre del campo a filtrar.
     * @param join      Join hacia la tabla relacionada.
     * @param condicion Objecto con la condición where para la tabla.
     * @param from
     * @return Predicado sobre el join realizado
     */
    private Predicate getPredicateField(Long idTrans, CriteriaBuilder builder, String nameField, From join, BusquedaModel.WhereCondition condicion, Root from, BusquedaModel.WhereCondition condicionPrin) {
        String clazzNameJoin = join.getJavaType().getSimpleName();
        try {
            EntityType o = getEntityTypeFromCache(clazzNameJoin, idTrans);
            Attribute metaField = null;
            Expression path = null;
            if (!nameField.equalsIgnoreCase("SELECTCASE")) {
                if (o == null) {
                    System.out.println(clazzNameJoin + " No se encontro referencia a " + nameField);
                    return null;
                }
                metaField = o.getAttribute(nameField);
                path = join.get(nameField);
                if (metaField.getJavaType().equals(String.class) || metaField.getJavaType().equals(Character.class)) {
                    if (condicion.getTrim()) {
                        path = builder.trim(builder.upper(join.get(nameField)));
                    } else {
                        path = builder.upper(join.get(nameField));
                    }
                }
            }
            if (condicion == null || condicion.getComparador() == null) {
                LOGGER.log(Level.INFO, "---> EntityModel No tiene comparador " + (ENTITY_PREFIX + clazzNameJoin) + " nameField " + nameField + " condicion " + condicion);
                return null;
            }
            switch (condicion.getComparador().toUpperCase()) {
                case "NOTEQUAL":
                case "NE":
                    List<Object> l = condicion.getValuesCast(metaField.getJavaType());
                    if (l.size() > 1) {
                        return builder.notEqual(path, l);
                    } else {
                        return builder.notEqual(path, l.get(0));
                    }
                case "IN":
                    return join.get(nameField).in(condicion.getValuesCast(metaField.getJavaType()));
                case "NOTIN":
                    return join.get(nameField).in(condicion.getValuesCast(metaField.getJavaType())).not();
                case "NOTLIKE":
                case "NOTCONTAINS":
                    return getPredicatelLike(idTrans, builder, nameField, join, condicion, from, path, 1, true, condicionPrin);
                case "LIKE":
                case "CONTAINS":
                    return builder.like(builder.upper(path), condicion.getValueLikes());
                case "STARTSWITH":
                    if (metaField.getJavaType() == String.class) {
                        boolean negado = false;
                        if (condicionPrin != null && condicionPrin.getComparador() != null) {
                            if ("NOTLIKE".equalsIgnoreCase(condicionPrin.getComparador()) || "NOTCONTAINS".equalsIgnoreCase(condicionPrin.getComparador())) {
                                negado = true;
                            }
                        }
                        return getPredicatelLike(idTrans, builder, nameField, join, condicion, from, path, 1, negado, condicionPrin);
                    } else {
                        Object v = condicion.getValuesCast(metaField.getJavaType()).get(0);
                        if (metaField.getJavaType().equals(Date.class)) {
                            Date date = null;
                            if (v instanceof Date) {
                                date = (Date) v;
                            } else {
                                date = new SimpleDateFormat("yyyy-MM-dd").parse(v.toString());
                            }
                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                            date = df.parse(df.format(date));
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);
                            cal.add(Calendar.DAY_OF_MONTH, 1);
                            cal.add(Calendar.SECOND, -1);
                            return builder.between(path, date, cal.getTime());
                        } else {
                            return builder.equal(path, v);
                        }

                    }
                case "ENDSWITH":
                    boolean negado1 = false;
                    if (condicionPrin != null && condicionPrin.getComparador() != null) {
                        if ("NOTLIKE".equalsIgnoreCase(condicionPrin.getComparador()) || "NOTCONTAINS".equalsIgnoreCase(condicionPrin.getComparador())) {
                            negado1 = true;
                        }
                    }
                    return getPredicatelLike(idTrans, builder, nameField, join, condicion, from, path, 2, negado1, condicionPrin);
                case "LT":
                case "<":
                    Object value = condicion.getValuesCast(metaField.getJavaType()).get(0);
                    if (value instanceof Comparable) {
                        return builder.lessThan(path, (Comparable) value);
                    }
                case "LTE":
                case "<=":
                    Object value1 = condicion.getValuesCast(metaField.getJavaType()).get(0);
                    if (value1 instanceof Comparable) {
                        return builder.lessThanOrEqualTo(path, (Comparable) value1);
                    }
                case "GT":
                case ">":
                    Object value2 = condicion.getValuesCast(metaField.getJavaType()).get(0);
                    if (value2 instanceof Comparable) {
                        return builder.greaterThan(path, (Comparable) value2);
                    }
                case "GTE":
                case ">=":
                    Object value3 = condicion.getValuesCast(metaField.getJavaType()).get(0);
//                    System.out.println("--> " + value3 + " --> " + metaField.getJavaType());
                    if (value3 instanceof Comparable) {
                        return builder.greaterThanOrEqualTo(path, (Comparable) value3);
                    }
                case "BETWEEN":
                    List<Object> valuesCast = condicion.getValuesCast(metaField.getJavaType());
                    return builder.between(path, (Comparable) valuesCast.get(0), (Comparable) valuesCast.get(1));
                case "ISNULL":
                    return builder.isNull(path);
                case "ISNOTNULL":
                    return builder.isNotNull(path);
                case "ISEMPTY":
                    return builder.isEmpty(path);
                case "ISNOTEMPTY":
                    return builder.isNotEmpty(path);
                case "CASE":
                    List<Predicate> conds = new ArrayList<>(condicion.getValues().size());
                    List<Predicate> condElse = new ArrayList<>(condicion.getValues().size());
                    List<Object> values = condicion.getValues();
                    Object v2 = values.get(0);
                    values = values.subList(1, values.size());
                    for (Object cd : values) {
                        BusquedaModel.WhereCondition v = null;
                        if (cd instanceof Map) {
                            Map cdv = (Map) cd;
                            nameField = (String) cdv.keySet().stream().findFirst().get();
                            v = (BusquedaModel.WhereCondition) Utilities.toObjectFromJson(Utilities.toJson(cdv.values().stream().findFirst().get()), BusquedaModel.WhereCondition.class);
                        } else if (cd instanceof BusquedaModel.WhereCondition) {
                            v = (BusquedaModel.WhereCondition) cd;
                        }
                        LOGGER.log(Level.INFO, "Verificando valores " + cd + " where cond " + v);
                        Predicate field = null;
                        if (v.getComparador() == null && cd.toString().startsWith("{") && cd.toString().endsWith("{")) {
                            analizarOrPred(idTrans, builder, from, conds, cd, condicion, null);
                        } else if (v.getComparador() != null) {
                            field = this.getPredicateField(idTrans, builder, nameField, join, v, from, condicion);
                        }
                        if (field != null) {
                            if ("ELSE".equalsIgnoreCase(v.getComparador())) {
                                condElse.add(field);
                            } else {
                                conds.add(field);
                            }
                        }
                    }
                    Expression vv = null;
                    if (v2 != null) {
                        try {
                            if (v2 instanceof Map) {
                                Map cdv = (Map) v2;
                                nameField = (String) cdv.keySet().stream().findFirst().get();
                                v2 = Utilities.toObjectFromJson(Utilities.toJson(cdv.values().stream().findFirst().get()), BusquedaModel.WhereCondition.class);
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.INFO, "No es js " + e.getMessage());
                            v2 = new BusquedaModel.WhereCondition(null, null);
                        }
                        if (v2 instanceof BusquedaModel.WhereCondition) {
                            vv = this.getPredicateField(idTrans, builder, nameField, join, (BusquedaModel.WhereCondition) v2, from, condicion);
                        } else {
                            vv = builder.literal(v2);
                        }
                    }
                    Predicate[] result = new Predicate[conds.size()];
                    LOGGER.log(Level.INFO, "Generando case " + conds.size() + " then value " + v2 + " Else " + condElse.size());
                    CriteriaBuilder.Case<Object> when = builder.selectCase().when(builder.and(conds.toArray(result)), vv);
                    if (condElse != null && condElse.size() > 0) {
                        when.otherwise(condElse);
                    }
                    return builder.isTrue(when.as(Boolean.class));
                case "OR":
                    System.out.println("Ingreso al OR values " + condicion.getValues());
                    List<Predicate> ors = new ArrayList<>(condicion.getValues().size());
                    for (Object cd : condicion.getValues()) {
                        BusquedaModel.WhereCondition v = null;
                        Map val = null;
                        try {
                            if (cd instanceof BusquedaModel.WhereCondition) {
                                v = (BusquedaModel.WhereCondition) cd;
                            } else if (cd instanceof Map) {
                                val = (Map) cd;
                            } else {
                                System.out.println("Or values no es BusquedaDinamica.WhereCondition ni  Map " + cd);
                                if (!cd.toString().startsWith("{")) {
//                                    v = new BusquedaDinamica.WhereCondition("AND", Arrays.asList(cd));
                                } else {
                                    v = (BusquedaModel.WhereCondition) Utilities.toObjectFromJson(cd.toString(), BusquedaModel.WhereCondition.class);
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("No es js " + e.getMessage());
                            v = new BusquedaModel.WhereCondition(null, null);
                        }
                        if ((v == null || v.getComparador() == null) && val != null) {
                            analizarOrPred(idTrans, builder, from, ors, cd, condicion, nameField);
                        } else if (v != null && v.getComparador() != null) {
                            ors.add(this.getPredicateField(idTrans, builder, nameField, join, v, from, condicion));
                        } else {
                            BusquedaModel.WhereCondition oc = new BusquedaModel.WhereCondition(Arrays.asList(cd));
                            ors.add(this.getPredicateField(idTrans, builder, nameField, join, oc, from, condicion));
                        }
                    }
                    return builder.or(ors.toArray(new Predicate[]{}));
                case "AND":
                    if (Utilities.isNotEmpty(condicion.getValues())) {
                        List<Predicate> ands = new ArrayList<>(condicion.getValues().size());
                        for (Object cd : condicion.getValues()) {
                            BusquedaModel.WhereCondition v = null;
                            try {
                                if (!cd.toString().startsWith("{")) {
//                                v = new BusquedaDinamica.WhereCondition("AND", Arrays.asList(cd));
                                } else {
                                    v = (BusquedaModel.WhereCondition) Utilities.toObjectFromJson(cd.toString(), BusquedaModel.WhereCondition.class);
                                }
                            } catch (Exception e) {
                                System.out.println("No es js " + e.getMessage());
                                v = new BusquedaModel.WhereCondition(null, null);
                            }
                            if (v.getComparador() == null && cd.toString().startsWith("{")) {
                                analizarOrPred(idTrans, builder, from, ands, cd, condicion, null);
                            } else if (v.getComparador() != null) {
                                ands.add(this.getPredicateField(idTrans, builder, nameField, join, v, from, condicion));
                            } else {
                                BusquedaModel.WhereCondition oc = new BusquedaModel.WhereCondition(Arrays.asList(cd));
                                ands.add(this.getPredicateField(idTrans, builder, nameField, join, oc, from, condicion));
                            }
                        }
                        return builder.and(ands.toArray(new Predicate[]{}));
                    }
                default:
                    Object v = null;
                    if (metaField.getJavaType().equals(String.class) || metaField.getJavaType().equals(Character.class)) {
                        if (condicion.getValuesCast(metaField.getJavaType()).size() > 0) {
                            v = condicion.getValuesCast(metaField.getJavaType()).get(0);
                        } else {
                            v = "";
                        }
                    } else {
                        v = condicion.getValuesCast(metaField.getJavaType()).get(0);
                    }
                    if (metaField.getJavaType().equals(Date.class)) {
                        Date date = null;
                        if (v instanceof Date) {
                            date = (Date) v;
                        } else {
                            date = new SimpleDateFormat("yyyy-MM-dd").parse(v.toString());
                        }
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        date = df.parse(df.format(date));
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                        cal.add(Calendar.SECOND, -1);
                        return builder.between(path, date, cal.getTime());
                    } else {
                        if (v != null) {
                            return builder.equal(path, v);
                        }
                    }
            }
        } catch (Exception e) {
            System.out.println("----->entityModel " + (ENTITY_PREFIX + clazzNameJoin) + " nameField " + nameField + " condicion " + condicion);
            LOGGER.log(Level.SEVERE, "entityModel", e);
            return null;
        }
        return null;
    }

    private Predicate getPredicatelLike(Long idTrans, CriteriaBuilder builder, String nameField, From join, BusquedaModel.WhereCondition condicion, Root from, Expression path, int i, boolean negado, BusquedaModel.WhereCondition condicionPrin) {
        if (condicion.getValues() != null && condicion.getValues().size() > 1) {
            List<Predicate> pred = new ArrayList<>(condicion.getValues().size());
            for (Object ob : condicion.getValues()) {
                if (ob instanceof Map) {
                    pred.add(this.getPredicateField(idTrans, builder, nameField, join, (BusquedaModel.WhereCondition) Utilities.toObjectFromJson(Utilities.toJson(ob), BusquedaModel.WhereCondition.class), from, condicionPrin));
                } else {
                    if (!negado) {
                        if (i == 1) {
                            pred.add(builder.like(builder.upper(path), (ob.toString().endsWith("%") ? ob.toString().toUpperCase() : ob.toString().toUpperCase().concat("%"))));
                        } else if (i == 2) {
                            pred.add(builder.like(builder.upper(path), (ob.toString().startsWith("%") ? ob.toString().toUpperCase() : "%".concat(ob.toString().toUpperCase()))));
                        } else {
                            pred.add(builder.like(builder.upper(path), (ob.toString().contains("%") ? ob.toString().toUpperCase() : "%".concat(ob.toString().toUpperCase()).concat("%"))));
                        }
                    } else {
                        if (i == 1) {
                            pred.add(builder.notLike(builder.upper(path), (ob.toString().endsWith("%") ? ob.toString().toUpperCase() : ob.toString().toUpperCase().concat("%"))));
                        } else if (i == 2) {
                            pred.add(builder.notLike(builder.upper(path), (ob.toString().startsWith("%") ? ob.toString().toUpperCase() : "%".concat(ob.toString().toUpperCase()))));
                        } else {
                            pred.add(builder.notLike(builder.upper(path), (ob.toString().contains("%") ? ob.toString().toUpperCase() : "%".concat(ob.toString().toUpperCase()).concat("%"))));
                        }
                    }
                }
            }
            LOGGER.log(Level.INFO, "negado " + negado + " pred " + pred.size());
            if (condicionPrin != null && "OR".equalsIgnoreCase(condicionPrin.getComparador())) {
                return builder.or(pred.toArray(new Predicate[pred.size()]));
            } else {
                return builder.and(pred.toArray(new Predicate[pred.size()]));
            }
        } else {
            Object vd = condicion.getValues().get(0);
            if (vd instanceof Map) {
                return getPredicatelLike(idTrans, builder, nameField, join, (BusquedaModel.WhereCondition) Utilities.toObjectFromJson(Utilities.toJson(vd), BusquedaModel.WhereCondition.class), from, path, i, negado, condicion);
            } else {
                String v = condicion.getValues().get(0).toString();
                v = v.toUpperCase();
                if (!negado) {
                    if (i == 1) {
                        return builder.like(builder.upper(path), (v.endsWith("%") ? v : v.concat("%")));
                    } else if (i == 2) {
                        return builder.like(builder.upper(path), (v.startsWith("%") ? v : "%".concat(v)));
                    } else {
                        return builder.like(builder.upper(path), (v.contains("%") ? v : "%".concat(v).concat("%")));
                    }
                } else {
                    if (i == 1) {
                        return builder.notLike(builder.upper(path), (v.endsWith("%") ? v : v.concat("%")));
                    } else if (i == 2) {
                        return builder.notLike(builder.upper(path), (v.startsWith("%") ? v : "%".concat(v)));
                    } else {
                        return builder.notLike(builder.upper(path), (v.contains("%") ? v : "%".concat(v).concat("%")));
                    }
                }
            }
        }
    }

    private void analizarOrPred(Long idTrans, CriteriaBuilder builder, Root from, List<Predicate> ors, Object cd, org.angbyte.model.BusquedaModel.WhereCondition condicion, String nameField) {
        if (cd != null) {
            if (cd instanceof String && cd.toString().startsWith("{")) {
                try {
                    BusquedaModel.WhereCondition x = (BusquedaModel.WhereCondition) Utilities.toObjectFromJson(cd.toString(), BusquedaModel.WhereCondition.class);
                    Object o = (x.getComparador() == null) ? Utilities.toObjectFromJson(cd.toString(), HashMap.class) : x;
                    analizarOrPred(idTrans, builder, from, ors, o, condicion, null);
                } catch (Exception e) {
                    System.out.println("error al convertir " + e);
                }
            } else {
                HashMap<String, Object> fil = (cd instanceof HashMap) ? (HashMap<String, Object>) cd : (HashMap<String, Object>) Utilities.toObjectFromJson(cd.toString(), HashMap.class);
                BusquedaModel d = new BusquedaModel();
                // Aqui se envia hacer la conversion de Map a WhereCondition
                d.setFilters(fil);
                List<Predicate> orss = new ArrayList<>(fil.size());
                fil.entrySet().forEach((Map.Entry<String, Object> entry) -> {
                    try {
                        BusquedaModel.WhereCondition b = (BusquedaModel.WhereCondition) Utilities.toObjectFromJson(cd.toString(), BusquedaModel.WhereCondition.class);
                        for (Object o : b.getValues()) {
                            if (o.toString().startsWith("{")) {
                                HashMap<String, Object> cp = new HashMap<>();
                                cp.put(entry.getKey(), o);
                                analizarOrPred(idTrans, builder, from, orss, cp, condicion, null);
                            } else {
                                orss.add(this.getPredicateField(idTrans, builder, entry.getKey(), this.createJoin(idTrans, entry.getKey(), from), new BusquedaModel.WhereCondition("EQ", Arrays.asList(o)), from, condicion));

                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Value " + entry.getValue(), e);
                    }
                });
                ors.add(builder.or(orss.toArray(new Predicate[]{})));
            }
        }
    }


    private void buildQuery(Long transactionId, BusquedaModel searchCriteria, CriteriaBuilder builder, CriteriaQuery<?> query, Root<?> root) {
        processFunction(transactionId, searchCriteria, builder, query, root);
        processWhere(transactionId, builder, root, searchCriteria.getFilters(), query);
        // Aplica seek (agrega predicado al WHERE)
        applySeekPagination(transactionId, searchCriteria, builder, query, root);
        // Asegura orden estable (agrega PK a los Orders si falta)
        ensureStableOrder(transactionId, searchCriteria, root);

        processGroupBy(transactionId, searchCriteria, query, root);
        processOrderBY(transactionId, builder, root, searchCriteria.getOrders(), query);
    }

    /**
     * Retrieves the Class object for a given entity name from the cache.
     *
     * @param entityName The name of the entity class to retrieve
     * @return The Class object for the entity, or null if not found
     */
    public Class<?> getEntityClass(String entityName) {
        try {
            Long transsaction = getIdTrans();
            EntityType entityType = getEntityTypeFromCache(entityName, transsaction);

            if (entityType == null) {
                LOGGER.log(Level.INFO, "No cached entity model found for {0}", entityName);
                return null;
            }
            if (buhoProperties.isDebug()) LOGGER.log(Level.INFO, "Obtener cache para {0}", entityType.getName());
            return entityType.getJavaType();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving entity class for " + entityName, e);
            return null;
        }
    }

    @Override
    public Object saveEntity(Object entity) {
        if (entity == null) {
            return null;
        }

        return saveUpdate(entity);

    }

    @Override
    public Long max(BusquedaModel data) {
        return 0L;
    }

    /**
     * Implementación para la busqueda dinamica
     *
     * @param searchCriteria Modelo con los datos para la busqueda
     * @param <T>            Cualquier tipo de dato
     */
    public <T> Optional<List<T>> findAllDynamic(BusquedaModel searchCriteria) {
        try {
            String entityName = searchCriteria.getEntity();
            Long transactionId = getIdTrans();
            boolean isMultiColumnQuery = isMultiColumnQuery(searchCriteria);

            EntityType<?> entityType = getEntityTypeFromCache(entityName, transactionId);
            Class<?> domainClass = getDomainClass(entityType, entityName, searchCriteria);

            updateJoinMappings(transactionId, domainClass);

            CriteriaBuilder builder = this.getEntityManager().getCriteriaBuilder();
            CriteriaQuery<?> query = createAppropriateQuery(builder, domainClass, isMultiColumnQuery);
            Root<?> root = query.from(domainClass);

            buildQuery(transactionId, searchCriteria, builder, query, root);

            removeTranssaction(transactionId);

            List resultList = getList(searchCriteria, isMultiColumnQuery, query);
            if (Boolean.TRUE.equals(searchCriteria.getResolverDto())) {
                ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
                scanner.addIncludeFilter(new AnnotationTypeFilter(Mapper.class));
                for (BeanDefinition bd : scanner.findCandidateComponents("org.ventanilla.interna.mappers")) {
                    Class aClass = Class.forName(bd.getBeanClassName());
                    String classMapper = searchCriteria.getEntity() + "Mapper";
                    if (aClass.getSimpleName().startsWith(classMapper)) {
                        Object instance = aClass.newInstance();
                        Method[] method = aClass.getMethods();
                        for (Method m : method) {
                            Class<?>[] types = m.getParameterTypes();
                            if ("toDto".equals(m.getName()) && types[0].equals(List.class)) {
                                System.out.println(aClass.getSimpleName() + " Encontrado " + m + " Lista toDto: " + (resultList == null ? "0" : resultList.size()));
                                List rsdto = (List) m.invoke(instance, resultList);
                                return Optional.ofNullable(rsdto);
                            }
                        }
                    }
                }
            }
            return Optional.ofNullable(resultList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing dynamic query", e);
            return Optional.empty();
        }
    }

    @Override
    public <T> List<T> findAllDinamic(BusquedaModel filtros) {
        Optional<List<Object>> optionalObjects = findAllDynamic(filtros);
        if (optionalObjects.isPresent()) {
            return (List<T>) optionalObjects.get();
        }
        return null;
    }

    /**
     * Implementación para la busqueda dinamica
     *
     * @param searchCriteria Modelo con los datos para la busqueda
     * @param headers        Parametro para agregar en el Header de la respuesta el rootSize con el conteo de los datos encontrados en la consulta.
     * @param <T>            Cualquier tipo de dato
     * @return Listado con los registro encontrado, si el un multiselect devuelve un listado Map, caso contrario el listado de la misma entidad.
     */
    public <T> List<T> findAllDinamic(BusquedaModel searchCriteria, MultiValueMap<String, String> headers) {
        String nameClazz = searchCriteria.getEntity();
        try {
            Long idTrans = getIdTrans();
            EntityType o = getEntityTypeFromCache(nameClazz, idTrans);
            Long count = 0L;
            if (o == null) {
                LOGGER.log(Level.INFO, "No existe objeto " + nameClazz + " con parametros " + searchCriteria);
                return null;
            }

            Class domainClass = o.getJavaType();
            Map<String, Object> mapClazzDonm = joinMappings.get(idTrans);
            if (mapClazzDonm == null) {
                mapClazzDonm = new LinkedHashMap<>();
            }
            mapClazzDonm.put(CLASS_TRANSLATION_PREFIX + idTrans, domainClass);
            joinMappings.put(idTrans, mapClazzDonm);
            if (domainClass == null) {
                LOGGER.log(Level.INFO, "No existe objeto " + nameClazz + " con parametros " + searchCriteria);
                return null;
            }
            CriteriaBuilder builder = this.getEntityManager().getCriteriaBuilder();
            List resultList = null;
            Optional<List<Object>> optionalObjects = findAllDynamic(searchCriteria);
            if (optionalObjects.isPresent()) {
                resultList = optionalObjects.get();
            }
            Utilities.logs(this, "Datos procesados ", resultList);
            // Para realizar el conteo
            count = countData(searchCriteria, builder, domainClass, idTrans);
            if (headers != null) {
                headers.add("rootSize", (count == null ? "0" : count.toString()));
            }
            Utilities.logs(this, "Registros encontrados", count);
            removeTranssaction(idTrans);
            return resultList;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Transaccion parametros " + searchCriteria);
            LOGGER.log(Level.SEVERE, "", e);
            headers.add("Error", e.getMessage());
            return null;
        }
    }

    private Long countData(BusquedaModel searchCriteria, CriteriaBuilder builder, Class domainClass, Long idTrans) {
        Long count;
        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        Root from = countQuery.from(domainClass);
        removeTranssaction(idTrans);
        Predicate[] preds = processWhere(idTrans, builder, from, searchCriteria.getFilters(), countQuery);
        if (searchCriteria.getDistinct() != null && searchCriteria.getDistinct().equals(true)) {
            countQuery.select(builder.countDistinct(from));
        } else {
            countQuery.select(builder.count(from));
        }
        if (Utilities.isNotEmpty(preds)) {
            countQuery.where(preds);
        }
        count = this.getEntityManager().createQuery(countQuery).getSingleResult();
        return count;
    }

    /**
     * Resaliza la ejecución de una funcion de base de datos.
     *
     * @param busq
     * @param headers
     * @return
     */
    @Override
    public Object findAllFunction(BusquedaModel busq, MultiValueMap<String, String> headers) {
        return null;
    }

    @Override
    @Transactional
    public Respuesta save(GuardarModel model) {
        Respuesta rws = new Respuesta();
        rws.setEstado(false);
        String msg = "";
        boolean array = false;
        try {
            if (model == null) {
                msg += Messages.NO_DATO_ERROR_MENSAJE + "\n";
            }
            if (model.getEntity() == null) {
                msg += Messages.NO_DATO_ERROR_MENSAJE + " del Objeto" + "\n";
            }
            if (model.getData() == null) {
                msg += Messages.NO_DATO_ERROR_MENSAJE + " del cuerpo del Objeto" + "\n";
            }
            Long transsaction = getIdTrans();
            rws.setMensaje(msg);
            EntityType o = getEntityTypeFromCache(model.getEntity(), transsaction);
            if (o == null) {
                msg += "No existe objeto " + model.getEntity() + " con parametros " + model.getEntity() + "\n";
                System.out.println(model);
                return rws;
            }
            Class entityClass = o.getJavaType();
            if (entityClass == null) {
                msg += "No existe objeto " + model.getEntity() + " con parametros " + model.getEntity() + "\n";
                System.out.println(model);
                return rws;
            }
            if (model.getData() == null) {
                msg += "No existe información a guardar.\n";
                System.out.println(model);
                return rws;
            }
            if (model.getData().startsWith("[")) {
                array = true;
                Object x = Array.newInstance(entityClass, 1);
                System.out.println("Nueva instancia " + x.getClass());
                entityClass = x.getClass();
            }
            rws.setMensaje(msg);
            List d = new ArrayList();
            if (array) {
                Object[] ao = (Object[]) Utilities.toObjectFromJson(model.getData(), entityClass);
                System.out.println(Arrays.toString(ao));
                d.addAll(Arrays.asList(ao));
            } else {
                d.add(Utilities.toObjectFromJson(model.getData(), entityClass));
            }
            rws.setEstado(true);
            Object datas = null;
            List rs = new ArrayList();
            for (Object entity : d) {
                boolean ok = this.processValidations(rws, entity, model);
                System.out.println("Guardar " + ok);
                if (ok) {
                    SingularAttribute id = o.getDeclaredId(o.getIdType().getJavaType());
                    Field field = entity.getClass().getDeclaredField(id.getName());
                    field.setAccessible(true);
                    Object idVal = field.get(entity);
                    if (idVal == null) {
                        this.getEntityManager().persist(entity);
                        //this.getEm().getTransaction().commit();
                        try {
                            this.getEntityManager().merge(entity);
                        } catch (Exception e) {
                            System.out.println("Error al refrescar " + e.getMessage());
                        }
                    } else {
                        this.getEntityManager().merge(entity);
                    }
                    datas = entity;
                    rs.add(entity);
                    rws.setEstado(true);
                } else {
                    rws.setEstado(false);
                    break;
                }
            }
            if (Utilities.isNotEmpty(rs)) {
                try {
                    rws.setData(Utilities.toJson(rs));
                } catch (Exception e) {
                    System.out.println("Error al convertir save(GuardarModel model) " + e.getMessage());
                }
            }
            if (datas != null) {
                try {
                    Field f = datas.getClass().getDeclaredField(o.getId(o.getIdType().getJavaType()).getName());
                    f.setAccessible(true);
                    Object idVal = f.get(datas);
                    if (idVal != null) {
                        try {
                            Long idParse = Long.valueOf(idVal.toString());
                            rws.setId(idParse);
                        } catch (NumberFormatException e) {
                            System.out.println("ID no numérico: " + idVal);
                            rws.setInfo(idVal.toString());
                        }
                    }
                    //rws.setId(f.getLong(f.get(datas)));
                } catch (Exception e) {
                    System.out.println("Error al obtener id " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (rws.getEstado()) {
                rws.setEstado(true);
                rws.setMensaje(Messages.DATOS_GUARDADOS);
            } else {
                rws.setEstado(false);
//                rws.setMensaje(Mensajes.DATOS_NO_GUARDADOS);
            }
            System.out.println("Respuesta guardado generico " + rws);
            removeTranssaction(transsaction);
            return rws;
        } catch (Exception e) {
            rws.setEstado(false);
            rws.setMensaje(e.getMessage());
            LOGGER.log(Level.SEVERE, "", e);
            this.getEntityManager().getTransaction().rollback();
        }
        return rws;
    }

    @Override
    @Transactional
    public Object saveUpdate(Object entity) {
        try {
            EntityType o = getEntityTypeFromCache(entity.getClass().getSimpleName(), getIdTrans());
            SingularAttribute id = o.getDeclaredId(o.getIdType().getJavaType());
            Field field = entity.getClass().getDeclaredField(id.getName());
            field.setAccessible(true);
            Object idVal = field.get(entity);
            if (idVal == null) {
                this.getEntityManager().persist(entity);
                this.getEntityManager().refresh(entity);
            } else {
                update(entity);
            }
            return entity;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-->", e);
            return null;
        }
    }

    /**
     * Guarda lista de objetos sin validación.
     *
     * @param list Lista de objetos guardados.
     */
    @Override
    @Transactional
    public Collection saveAll(Collection list) {
        try {
            Collection saved = new ArrayList(list.size());
            int i = 0;

            for (Object entity : list) {
                saved.add(saveUpdate(entity));
                if (++i % JDBC_BATCH_SIZE == 0) {
                    this.getEntityManager().flush();
                    this.getEntityManager().clear();
                }
            }
            this.getEntityManager().flush();
            this.getEntityManager().clear();
            return saved;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "--> Error el persistir entidad ", e);
            this.getEntityManager().getTransaction().rollback();
        }
        return null;
    }

    @Transactional
    public Object update(Object entity) {
        try {
            this.getEntityManager().merge(entity);
            return entity;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-->", e);
            return null;
        }
    }

    private boolean processValidations(Respuesta rws, Object entity, GuardarModel model) {
        String msg = "";
        if (rws.getMensaje() != null) {
            msg = rws.getMensaje();
        }
//        System.out.println("Modelo guardado " + model + " v " + (model.getValidaciones() != null));
        try {
            if (model.getValidaciones() != null) {
                for (Map.Entry<String, GuardarModel.Validacion> e : model.getValidaciones().entrySet()) {
                    try {
                        String[] split = e.getKey().split("\\.");
                        int index = 0;
                        Object v = entity;
                        for (String sp : split) {
                            Field f = v.getClass().getDeclaredField(sp);
                            f.setAccessible(true);
                            Object o = f.get(v);
                            System.out.println("Campo " + sp + " Objeto a buscar " + o + " Validacion " + e.getValue());
                            if (index == 0 && split.length >= 1) { // referencia dentro de la propiedad
                                if (e.getValue() != null && e.getValue().getAcciones() != null) {
                                    for (String vf : e.getValue().getAcciones()) {
                                        if ("ISNOTNULL".equalsIgnoreCase(vf)) {
                                            if (o == null) {
                                                msg += String.format(Messages.FALTA_INGRESAR_CAMPO, "Ingresar o Seleccionar", e.getValue().getDescripcionCampo()) + "\n";
                                            }
                                            if (o instanceof String) {
                                                if (o.toString().trim().length() == 0) {
                                                    msg += String.format(Messages.FALTA_INGRESAR_CAMPO, "Ingresar o Seleccionar", e.getValue().getDescripcionCampo()) + "\n";
                                                    System.out.println(model);
                                                }
                                            }
                                        } else if (">".equalsIgnoreCase(vf)) {
                                            if (o != null) {
                                                BigDecimal bd = new BigDecimal(o.toString());
                                                if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                                                    msg += String.format(Messages.EXISTE_REGISTRO, "Mayor", "0") + "\n";
                                                }
                                            }
                                        } else if ("<".equalsIgnoreCase(vf)) {
                                            if (o != null) {
                                                BigDecimal bd = new BigDecimal(o.toString());
                                                if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                                                    msg += String.format(Messages.EXISTE_REGISTRO, "Menor", "0") + "\n";
                                                }
                                            }
                                        }

                                    }
                                }
                            } else if (index < (split.length - 1)) { // Si es el primero o el ultimo
                                System.out.println("No se hace nada...");
                            }
                            index++;
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "-->", ex);
                    }
                }
            }
            if ("".equalsIgnoreCase(msg)) {
                return true;
            } else {
                rws.setMensaje(msg);
                rws.setEstado(false);
                return false;
            }
        } catch (Exception e) {
            rws.setEstado(false);
            rws.setMensaje(e.getMessage());
            LOGGER.log(Level.SEVERE, "--> ", e);
            return false;
        }
    }

    //    @Override
    public Object findAllFunction(EjecucionFuncion funcion) {
        try {
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "findAllFunction", e);
        }
        return null;
    }

    public Object find(BusquedaModel busq) {
        try {
            String nameClazz = busq.getEntity();
            Long transsaction = getIdTrans();
            boolean isTuple = isMultiColumnQuery(busq);
            EntityType o = getEntityTypeFromCache(busq.getEntity(), transsaction);
            if (o == null) {
                LOGGER.log(Level.INFO, "(find) No existe objeto en cache model " + nameClazz + " con parametros " + busq);
                return null;
            }
            Class domainClass = o.getJavaType();
            if (domainClass == null) {
                LOGGER.log(Level.INFO, "No existe objeto " + nameClazz + " con parametros " + busq);
                return null;
            }
            Map<String, Object> mapClazzDonm = joinMappings.get(transsaction);
            if (mapClazzDonm == null) {
                mapClazzDonm = new LinkedHashMap<>();
            }
            mapClazzDonm.put(CLASS_TRANSLATION_PREFIX + transsaction, domainClass);
            joinMappings.put(transsaction, mapClazzDonm);
            CriteriaBuilder builder = this.getEntityManager().getCriteriaBuilder();
            CriteriaQuery query = builder.createQuery(domainClass);
            if (isTuple) {
                query = builder.createTupleQuery();
            }
            Root from = query.from(domainClass);
            processFunction(transsaction, busq, builder, query, from);
            processWhere(transsaction, builder, from, busq.getFilters(), query);
            processGroupBy(transsaction, busq, query, from);
            processOrderBY(transsaction, builder, from, busq.getOrders(), query);
            removeTranssaction(transsaction);
            List list = getList(busq, isTuple, query);
            if (Utilities.isNotEmpty(list)) {
                return list.get(0);
            }
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
            return null;
        }
    }

    public boolean exists(BusquedaModel busq) {
        try {
            Long transsaction = getIdTrans();
            EntityType o = getEntityTypeFromCache(busq.getEntity(), transsaction);
            if (o == null) {
                LOGGER.log(Level.INFO, "(exists) No existe objeto en cache model " + busq.getEntity() + " con parametros " + busq);
                return false;
            }
            Class domainClass = o.getJavaType();
            if (domainClass == null) {
                LOGGER.log(Level.INFO, "(exists) No existe objeto " + busq.getEntity() + " con parametros " + busq);
                return false;
            }
            CriteriaBuilder builder = this.getEntityManager().getCriteriaBuilder();
            CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);

            Map<String, Object> mapClazzDonm = joinMappings.get(transsaction);
            if (mapClazzDonm == null) {
                mapClazzDonm = new LinkedHashMap<>();
            }
            mapClazzDonm.put(CLASS_TRANSLATION_PREFIX + transsaction, domainClass);
            joinMappings.put(transsaction, mapClazzDonm);

            Root from = countQuery.from(domainClass);
            Predicate[] preds = processWhere(transsaction, builder, from, busq.getFilters(), countQuery);
            if (busq.getDistinct() != null && busq.getDistinct().equals(true)) {
                if (preds == null) {
                    countQuery.select(builder.countDistinct(from));
                } else {
                    countQuery.select(builder.countDistinct(from)).where(preds);
                }
            } else {
                if (preds == null) {
                    countQuery.select(builder.count(from));
                } else {
                    countQuery.select(builder.count(from)).where(preds);
                }
            }
//            Long count = this.getEntityManager().createQuery(countQuery).getSingleResult();
            TypedQuery<Long> countTyped = this.getEntityManager().createQuery(countQuery);
            countTyped.setHint("org.hibernate.readOnly", Boolean.TRUE);
            try {
                org.hibernate.query.Query<?> hq = countTyped.unwrap(org.hibernate.query.Query.class);
                hq.setReadOnly(true);
                hq.setTimeout(Math.max(1, DEFAULT_QUERY_TIMEOUT_MS / 1000));
            } catch (Exception ignore) {
            }
            Long count = countTyped.getSingleResult();

            removeTranssaction(transsaction);
            if (count == null) {
                count = 0L;
            }
            return count > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
            return false;
        }
    }

    @Transactional
    public boolean deleteModelAll(GuardarModel model) {
        try {
            boolean array = false;
            Long transsaction = getIdTrans();
            EntityType o = getEntityTypeFromCache(model.getEntity(), transsaction);
            if (o == null) {
                System.out.println("No existe objeto " + model.getEntity() + " con parametros " + model.getEntity());
                return false;
            }
            Class entityClass = o.getJavaType();
            if (entityClass == null) {
                System.out.println("No existe objeto " + model.getEntity() + " con parametros " + model.getEntity());
                return false;
            }
            if (model.getData() == null) {
                System.out.println("No existe información a guardar.");
                return false;
            }
            if (model.getData().startsWith("[")) {
                array = true;
                Object x = Array.newInstance(entityClass, 1);
                System.out.println("Nueva instancia " + x.getClass());
                entityClass = x.getClass();
            }
            List entitiList = new ArrayList();
            if (array) {
                Object[] ao = (Object[]) Utilities.toObjectFromJson(model.getData(), entityClass);
                System.out.println(Arrays.toString(ao));
                entitiList.addAll(Arrays.asList(ao));
            } else {
                entitiList.add(Utilities.toObjectFromJson(model.getData(), entityClass));
            }
            boolean deleteAll = deleteAll(entitiList);
            System.out.println("Respuesta guardado generico registro eliminados" + deleteAll);
            removeTranssaction(transsaction);
            return deleteAll;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "--> Error el persistir entidad ", e);
            this.getEntityManager().getTransaction().rollback();
        }
        return false;
    }

    @Transactional
    public boolean deleteAll(Collection list) {
        try {
            for (Object entity : list) {
                Long transsaction = getIdTrans();
                EntityType o = getEntityTypeFromCache(entity.getClass().getSimpleName(), transsaction);
                if (o == null) {
                    System.out.println("No existe objeto " + entity.getClass().getSimpleName() + " con parametros " + entity.getClass().getSimpleName());
                    return false;
                }
                CriteriaDelete<?> criteriaDelete = this.getEntityManager().getCriteriaBuilder().createCriteriaDelete(entity.getClass());
                Root from = criteriaDelete.from(o.getJavaType());
                Field f = entity.getClass().getDeclaredField(o.getDeclaredId(o.getIdType().getJavaType()).getName());
                f.setAccessible(true);
                Object idVal = f.get(entity);
                criteriaDelete.where(this.getEntityManager().getCriteriaBuilder().equal(from.get(o.getDeclaredId(o.getIdType().getJavaType()).getName()), idVal));
                int i = this.getEntityManager().createQuery(criteriaDelete).executeUpdate();
                removeTranssaction(transsaction);
                if (++i % JDBC_BATCH_SIZE == 0) {
                    this.getEntityManager().flush();
                    this.getEntityManager().clear();
                }
            }
            this.getEntityManager().flush();
            this.getEntityManager().clear();

            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "--> Error el persistir entidad ", e);
            this.getEntityManager().getTransaction().rollback();
        }
        return false;
    }

    @Override
    public Long count(BusquedaModel searchCriteria) {
        String nameClazz = searchCriteria.getEntity();
        Long count = 0L;
        try {
            Long idTrans = getIdTrans();
            EntityType o = getEntityTypeFromCache(nameClazz, idTrans);
            if (o == null) {
                LOGGER.log(Level.INFO, "No existe objeto " + nameClazz + " con parametros " + searchCriteria);
                return null;
            }

            Class domainClass = o.getJavaType();
            Map<String, Object> mapClazzDonm = joinMappings.get(idTrans);
            if (mapClazzDonm == null) {
                mapClazzDonm = new LinkedHashMap<>();
            }
            mapClazzDonm.put(CLASS_TRANSLATION_PREFIX + idTrans, domainClass);
            joinMappings.put(idTrans, mapClazzDonm);
            if (domainClass == null) {
                LOGGER.log(Level.INFO, "No existe objeto " + nameClazz + " con parametros " + searchCriteria);
                return null;
            }
            CriteriaBuilder builder = this.getEntityManager().getCriteriaBuilder();
            count = countData(searchCriteria, builder, domainClass, idTrans);

            Utilities.logs(this, "Registros encontrados", count);
            removeTranssaction(idTrans);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
}