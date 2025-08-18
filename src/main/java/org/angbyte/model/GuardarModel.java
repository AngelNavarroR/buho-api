package org.angbyte.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GuardarModel {


    private String entity;
    private String data;
    private Map<String, Object> dataMap;
    private Map<String, Validacion> validaciones;


    public GuardarModel() {
    }

    public GuardarModel(String entity) {
        this.entity = entity;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    public void setDataMap(Map<String, Object> dataMap) {
        this.dataMap = dataMap;
    }

    public Map<String, Validacion> getValidaciones() {
        return validaciones;
    }

    public void setValidaciones(Map<String, Validacion> validaciones) {
        this.validaciones = validaciones;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuardarModel)) return false;
        GuardarModel that = (GuardarModel) o;
        return entity.equals(that.entity) && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, data);
    }

    @Override
    public String toString() {
        return "GuardarModel{" +
                "entity='" + entity + '\'' +
                ", data='" + data + '\'' +
                ", validaciones=" + validaciones +
                '}';
    }

    public static class Validacion {

        private String descripcionCampo;
        private List<String> acciones;

        public String getDescripcionCampo() {
            return descripcionCampo;
        }

        public void setDescripcionCampo(String descripcionCampo) {
            this.descripcionCampo = descripcionCampo;
        }

        public List<String> getAcciones() {
            return acciones;
        }

        public void setAcciones(List<String> acciones) {
            this.acciones = acciones;
        }
    }
}
