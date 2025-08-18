package org.angbyte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.angbyte.utils.Utilities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

public class BusquedaModel implements Serializable {

    private String entity;
    private Map<String, String> orders;
    private Map<String, Object> filters;
    private Map<String, Object> functions;
    private List<String> groupsBy;

    private Integer first;
    private Integer pageSize;
    private Boolean distinct = true;
    private Boolean unicoResultado = false;
    private Boolean unproxy = false;
    private Boolean gson = false;
    private Boolean resolverDto = false;
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

    @Override
    public String toString() {
        return "BusquedaDinamica{" + "entity='" + entity + '\'' + ", orders=" + orders + ", filters=" + filters + ", functions=" + functions + ", groupsBy=" + groupsBy + ", first=" + first + ", pageSize=" + pageSize + ", distinct=" + distinct + '}';
    }

    public static class WhereCondition {
        private String comparador = "EQ";
        private Boolean trim = true;
        private Boolean upper = true;
        private List<Object> values;

        public WhereCondition(List<Object> values) {
            this.values = values;
        }

        public WhereCondition(String comparador, List<Object> values) {
            this.comparador = comparador;
            this.values = values;
        }

        public String getComparador() {
            return comparador;
        }

        public void setComparador(String comparador) {
            this.comparador = comparador;
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

        @JsonIgnore
        public List<Object> getValuesCast(Class fieldType) {
            List list = null;
            if (values != null) {
                list = new ArrayList<>();
                for (Object value : values) {
                    if (value == null) {
                        continue;
                    }
                    if ("null".equalsIgnoreCase(value.toString())) {
                        continue;
                    }
                    if (fieldType.equals(String.class)) {
                        if (this.getTrim()) {
                            list.add(value.toString().trim().toUpperCase());
                        } else {
                            list.add(value.toString().toUpperCase());
                        }
                    } else if (fieldType.equals(BigDecimal.class)) {
                        list.add(new BigDecimal(value.toString()));
                    } else if (fieldType.equals(BigInteger.class)) {
                        list.add(new BigInteger(value.toString()));
                    } else if (fieldType.equals(Long.class)) {
                        list.add(Long.valueOf(value.toString()));
                    } else if (fieldType.equals(Integer.class)) {
                        list.add(Integer.valueOf(value.toString()));
                    } else if (fieldType.equals(Short.class)) {
                        list.add(Short.valueOf(value.toString()));
                    } else if (fieldType.equals(Date.class)) {
                        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date fechaDate = null;
                        System.out.println("Fecha transformada -> " + value.toString());
                        try {
                            fechaDate = formato.parse(value.toString());
                            list.add(fechaDate);
                        } catch (Exception e) {
                            System.out.println(e);
                            list.add(value);
                        }
                    } else {
                        list.add(value);
                    }
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
                if (values != null) {
                    String like = values.get(0).toString().toUpperCase();
                    return "%".concat(like.toUpperCase().concat("%"));
                }
            } catch (Exception e) {
                System.out.println("Error al obtener valor para like");
            }
            return null;
        }

        @JsonIgnore
        public String getValueStart() {
            try {
                if (values != null) {
                    String like = values.get(0).toString();
                    return like.concat("%");
                }
            } catch (Exception e) {
                System.out.println("Error al obtener valor para like");
            }
            return null;
        }

        @JsonIgnore
        public String getValueEnd() {
            try {
                if (values != null) {
                    String like = values.get(0).toString();
                    return "%".concat(like);
                }
            } catch (Exception e) {
                System.out.println("Error al obtener valor para like");
            }
            return null;
        }
    }

    public static final class Builder {

        private BusquedaModel b;

        public Builder(String entity) {
            b = new BusquedaModel(entity);
        }

        public Builder first(Integer first) {
            b.setFirst(first);
            return this;
        }

        public Builder pageSize(Integer pageSize) {
            b.setPageSize(pageSize);
            return this;
        }

        public Builder pageSize(Boolean distinct) {
            b.setDistinct(distinct);
            return this;
        }

        public Builder where(String field, Object value) {
            if (b.getFilters() == null) {
                b.setFilters(new LinkedHashMap<>());
            }
            b.getFilters().put(field, value);
            return this;
        }

        public Builder whereCondition(String field, WhereCondition value) {
            if (b.getFilters() == null) {
                b.setFilters(new LinkedHashMap<>());
            }
            b.getFilters().put(field, value);
            return this;
        }

        public Builder order(String field, String order) {
            if (b.getOrders() == null) {
                b.setOrders(new LinkedHashMap<>());
            }
            b.getOrders().put(field, order);
            return this;
        }

        public Builder functions(String field, String order) {
            if (b.getFunctions() == null) {
                b.setFunctions(new LinkedHashMap<>());
            }
            b.getFunctions().put(field, order);
            return this;
        }

        public Builder distinct(Boolean distinct) {
            b.setDistinct(distinct);
            return this;
        }

        public Builder unicoResultado(Boolean unicoResultado) {
            b.setUnicoResultado(unicoResultado);
            return this;
        }

        public BusquedaModel build() {
            return b;
        }
    }

}