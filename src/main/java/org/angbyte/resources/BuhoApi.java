package org.angbyte.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.angbyte.model.BusquedaModel;
import org.angbyte.model.GuardarModel;
import org.angbyte.service.BuhoService;
import org.angbyte.utils.Utilities;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Level;
import java.util.logging.Logger;

@RestController()
@RequestMapping("${buho.path:/filters}")
public class BuhoApi {
    private static final Logger LOG = Logger.getLogger(BuhoApi.class.getName());
    private final BuhoService service;

    /**
     * @param service Servicio donde se encuentra toda la logica de negocia.
     */
    public BuhoApi(BuhoService service) {
        this.service = service;
    }


    /**
     * Find by response entity.
     *
     * @param data the data
     * @return the response entity
     */
    @PostMapping(value = "/findBy", produces = "application/json")
    public ResponseEntity<?> findBy(@RequestBody BusquedaModel data, HttpServletRequest request, HttpServletResponse response) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Object result = null;
        try {
            System.out.println("Ejecutando solictud de '/findBy' " + request.getRemoteHost());
            result = this.service.findAllDinamic(data);
            LOG.log(Level.INFO, "/busquedas/findBy: " + data + " result = {0}", (result == null) ? "null" : "Con datos");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "/busquedas/findBy: " + data, e);
            headers.add("error", e.getMessage());
            return new ResponseEntity<>(headers, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(result, headers, HttpStatus.OK);
    }

    @PostMapping(value = "/findBy/page", produces = "application/json")
    public ResponseEntity<?> findByPage(@RequestBody BusquedaModel data, HttpServletRequest request, HttpServletResponse response) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Object result = null;
        try {
            System.out.println("Ejecutando solictud de '/findBy/page' " + request.getRemoteHost());
            result = this.service.findAllDinamic(data, headers);
            LOG.log(Level.INFO, "/busquedas/findBy: " + data + " result = " + ((result == null) ? "null" : "Con datos"));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "/busquedas/findBy/page" + data, e);
            headers.add("error", e.getMessage());
            return new ResponseEntity<>(headers, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(result, headers, HttpStatus.OK);
    }


    @PostMapping(value = "/findBy/count", produces = "application/json")
    public ResponseEntity<?> findByCount(@RequestBody BusquedaModel data) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Object result = null;
        try {
            result = this.service.findAllDinamic(data, headers);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "/busquedas/findBy/count: " + data, e);
            headers.add("error", e.getMessage());
            return new ResponseEntity<>(headers, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(result, headers, HttpStatus.OK);
    }

    @PostMapping(value = "/findBy/max", produces = "application/json")
    public ResponseEntity<?> findByMax(@RequestBody BusquedaModel data) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Long result = null;
        try {
            result = this.service.max(data, headers);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "/busquedas/findBy/max: " + data, e);
            headers.add("error", e.getMessage());
            return new ResponseEntity<>(headers, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(result, headers, HttpStatus.OK);
    }

    @PostMapping(value = "/findBy/exists", produces = "application/json")
    public ResponseEntity<?> findByExiste(@RequestBody BusquedaModel data) {
        Boolean result = false;
        try {
            result = this.service.existe(data);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(value = "/save/entiti", produces = "application/json")
    public ResponseEntity<?> saveEntiti(@RequestBody GuardarModel data) {
        Object result = null;
        try {
            result = this.service.saveEntiti(data);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
//            headers.add("error", e.getMessage());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(value = "/save/entiti/map", produces = "application/json")
    public ResponseEntity<?> saveEntitiMap(@RequestBody GuardarModel data) {
        Object result = null;
        try {
            if (data.getDataMap() != null) {
                data.setData(Utilities.toJson(data.getDataMap()));
            }
            result = this.service.saveEntiti(data);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
//            headers.add("error", e.getMessage());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(value = "/eliminar/entiti", produces = "application/json")
    public ResponseEntity<?> eliminar(@RequestBody GuardarModel data) {
        Object result = null;
        try {
            result = this.service.delete(data);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}