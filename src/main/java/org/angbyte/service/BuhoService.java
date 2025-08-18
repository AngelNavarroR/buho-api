package org.angbyte.service;


import org.angbyte.config.BuhoProperties;
import org.angbyte.model.BusquedaModel;
import org.angbyte.model.GuardarModel;
import org.angbyte.model.Respuesta;
import org.angbyte.repositories.BuhoPersistable;
import org.angbyte.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class BuhoService {

    private final BuhoPersistable repository;
    private final BuhoProperties buhoProperties;
    private final Logger LOG = Logger.getLogger(BuhoService.class.getName());

    @Autowired
    public BuhoService(BuhoProperties buhoProperties, BuhoPersistable repository) {
        this.buhoProperties = buhoProperties;
        this.repository = repository;
    }

    public Object findAllDinamic(BusquedaModel data) {
        try {
            List<Object> rs = repository.findAllDinamic(data);
            if (Utilities.isNotEmpty(rs)) {
                if (buhoProperties.isDebug()) {
                    LOG.log(Level.INFO, "Resultados de la busqueda: " + rs.size() + " unico resultado: " + data.getUnicoResultado() + " gson: " + data.getGson());
                }
                if (data.getUnicoResultado()) {
                    if (data.getGson()) {
                        return Utilities.toObjecttoHashMap(rs.get(0));
                    } else {
                        return rs.get(0);
                    }
                } else {
                    return getResultGson(data, rs);
                }
            } else {
                if (buhoProperties.isDebug()) {
                    LOG.log(Level.INFO, "Resultados de la busqueda: " + rs.size() + " unico resultado: " + data.getUnicoResultado() + " gson: " + data.getGson());
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
        }
        return null;
    }

    private Object getResultGson(BusquedaModel data, List rs) {
        if (data.getGson()) {
            if (Utilities.isNotEmpty(data.getIgnoreClassGsonw())) {
                if (Utilities.isEmpty(data.getIgnoreClassGson())) {
                    data.setIgnoreClassGson(new ArrayList<>());
                }
                for (String nameCl : data.getIgnoreClassGsonw()) {
                    Class clazz = repository.getEntityClass(nameCl);
                    if (clazz != null) {
                        data.getIgnoreClassGson().add(clazz);
                    }
                }
            }
            return Utilities.toObjecttoHashMap(rs, data.getIgnoreFieldsGson(), data.getIgnoreClassGson());
        } else {
            return rs;
        }
    }

    public Object findAllDinamic(BusquedaModel data, MultiValueMap<String, String> headers) {
        try {
            List<Object> allDinamic = repository.findAllDinamic(data, headers);
            return getResultGson(data, allDinamic);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object saveEntiti(GuardarModel data) {
        try {
            return repository.save(data);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
        }
        return null;
    }


    public Boolean existe(BusquedaModel data) {
        try {
            boolean exists = repository.exists(data);
            return exists;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
        }
        return false;
    }

    /**
     * Si tiene id se envia a actualizar caso contrario solo se actualiza,
     *
     * @param entiti entidad que se va a enviar a actualizar o guardar.
     * @return Obejto persistido
     */
    public Object save(Object entiti) {
        try {
            Object rsd = repository.saveEntity(entiti);
            return rsd;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
        }
        return null;
    }

    public Long max(BusquedaModel data, MultiValueMap<String, String> headers) {
        try {
            Long rsd = repository.max(data);
            return rsd;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
        }
        return 0l;
    }

    /**
     * Puede recibir una lista o un solo objeto
     *
     * @param entity
     * @return
     */
    public Object delete(GuardarModel entity) {
        try {
            boolean delete = repository.deleteModelAll(entity);
            Respuesta respuestaWs = new Respuesta();
            respuestaWs.setEstado(delete);
            return respuestaWs;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
            return null;
        }
    }

}