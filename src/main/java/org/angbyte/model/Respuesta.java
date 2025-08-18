package org.angbyte.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class Respuesta implements Serializable {

    private Long id;
    private Long idAux;
    private Boolean estado;
    private String data;
    private String mensaje;
    private String info;
    private String xError;
    private String ip;
    private List result;
    private Object request;

    private BigDecimal valor;

    /**
     * Instantiates a new Respuesta ws.
     */
    public Respuesta() {

    }

    /**
     * Gets estado.
     *
     * @return the estado
     */
    public Boolean getEstado() {
        return estado;
    }

    /**
     * Sets estado.
     *
     * @param estado the estado
     */
    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    /**
     * Gets data.
     *
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * Sets data.
     *
     * @param data the data
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Gets mensaje.
     *
     * @return the mensaje
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Sets mensaje.
     *
     * @param mensaje the mensaje
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getxError() {
        return xError;
    }

    public void setxError(String xError) {
        this.xError = xError;
    }

    public List getResult() {
        return result;
    }

    public void setResult(List result) {
        this.result = result;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }


    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public Long getIdAux() {
        return idAux;
    }

    public void setIdAux(Long idAux) {
        this.idAux = idAux;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "Respuesta{" +
                "id=" + id +
                ", idAux=" + idAux +
                ", estado=" + estado +
                ", data='" + data + '\'' +
                ", mensaje='" + mensaje + '\'' +
                ", info='" + info + '\'' +
                ", xError='" + xError + '\'' +
                ", ip='" + ip + '\'' +
                ", result=" + result +
                ", request=" + request +
                ", valor=" + valor +
                '}';
    }
}
