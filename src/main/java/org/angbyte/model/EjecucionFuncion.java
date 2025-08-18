package org.angbyte.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class EjecucionFuncion implements Serializable {


    private String nombre;
    private Boolean esTabla = false;
    private Map<Integer, Object> parametros;
    private Map<String, String> orders;
    private Map<String, Object> filters;
    private Map<String, Object> functions;
    private List<String> groupsBy;

    private Integer first;
    private Integer pageSize;
    private Boolean distinct = true;

    public EjecucionFuncion() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getEsTabla() {
        return esTabla;
    }

    public void setEsTabla(Boolean esTabla) {
        this.esTabla = esTabla;
    }

    public Map<Integer, Object> getParametros() {
        return parametros;
    }

    public void setParametros(Map<Integer, Object> parametros) {
        this.parametros = parametros;
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

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public String toString() {
        return "BusquedaDinamica{" +
                "entity='" + nombre + '\'' +
                ", orders=" + orders +
                ", filters=" + filters +
                ", functions=" + functions +
                ", groupsBy=" + groupsBy +
                ", first=" + first +
                ", pageSize=" + pageSize +
                ", distinct=" + distinct +
                '}';
    }

}
