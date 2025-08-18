package org.angbyte.repositories;


import org.angbyte.model.BusquedaModel;
import org.angbyte.model.GuardarModel;
import org.angbyte.model.Respuesta;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.List;

public interface BuhoPersistable {
    /**
     * Implementación para la busqueda dinamica
     *
     * @param filtros Modelo con los datos para la busqueda
     * @param <T>     Cualquier tipo de dato
     * @return Listado con los registro encontrado, si el un multiselect devuelve un listado Map<Strng, Object>, caso contrario el listado de la misma entidad.
     */
    <T> List<T> findAllDinamic(BusquedaModel filtros);

    /**
     * Implementación para la busqueda dinamica
     *
     * @param busq      Modelo con los datos para la busqueda
     * @param headers   Parametro para agregar en el Header de la respuesta el rootSize con el conteo de los datos encontrados en la consulta.
     * @param <T>       Cualquier tipo de dato
     * @return Listado con los registro encontrado, si el un multiselect devuelve un listado Map<Strng, Object>, caso contrario el listado de la misma entidad.
     */
    <T> List<T> findAllDinamic(BusquedaModel busq, MultiValueMap<String, String> headers);

    /**
     * Resaliza la ejecución de una funcion de base de datos.
     *
     * @param busq
     * @param headers
     * @return
     */
    public Object findAllFunction(BusquedaModel busq, MultiValueMap<String, String> headers);

    public Respuesta save(GuardarModel model);

    public Object update(Object entity);
    
    /**
     * Busca si la clave existe en cache del persistencia del hibernate.
     *
     * @param nameClazz Nombre de clase a buscar
     * @return Class
     */
    public Class getEntityClass(String nameClazz);

    public Object saveEntity(Object entity);

    Long max(BusquedaModel data);

    public boolean deleteModelAll(GuardarModel model);

    public boolean deleteAll(Collection list);

    public boolean exists(BusquedaModel busq);

}
