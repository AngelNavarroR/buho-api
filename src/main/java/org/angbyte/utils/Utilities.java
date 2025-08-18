package org.angbyte.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;

public class Utilities {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Utilities.class.getName());

    public static Object toObjectFromJson(Object json, Class clazz) {
        if (json == null) {
            return null;
        }
        try {
            GsonBuilder builder = new GsonBuilder();
            Class finalParentClazz = clazz;
            builder.enableComplexMapKeySerialization().setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .excludeFieldsWithModifiers(Modifier.STATIC)
                    .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                    .setExclusionStrategies(new ExclusionStrategy() {
                        private Class clazz;

                        @Override
                        public boolean shouldSkipField(FieldAttributes field) {
                            if (field.getAnnotation(JsonIgnore.class) != null) {
                                return true;
                            }
                            return field.getDeclaredType().equals(finalParentClazz);
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> clazz) {
                            this.clazz = clazz;
                            return false;
                        }
                    }).setPrettyPrinting();
            Gson gson2 = builder.create();
            return gson2.fromJson(json.toString(), clazz);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Transformar json a objeto.", e);
            return null;
        }
    }

    public static Object toObjecttoHashMap(Object json) {
        if (json == null) {
            return null;
        }
        try {
            GsonBuilder builder = new GsonBuilder();
            Class finalParentClazz = null;
            Type typeOfHashMap = null;
            if (json instanceof Collection || json instanceof List || json instanceof ArrayList) {
                finalParentClazz = ((List) json).get(0).getClass();
                typeOfHashMap = new com.google.common.reflect.TypeToken<List<Map<String, Object>>>() {
                }.getType();
            } else {
                typeOfHashMap = new com.google.common.reflect.TypeToken<Map<String, Object>>() {
                }.getType();
                finalParentClazz = json.getClass();
            }
            System.out.println("-> " + finalParentClazz);
            Class finalParentClazz1 = finalParentClazz;
            builder.enableComplexMapKeySerialization().setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .excludeFieldsWithModifiers(Modifier.STATIC)
                    .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                    .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).setExclusionStrategies(new ExclusionStrategy() {
                        private Class clazz;

                        @Override
                        public boolean shouldSkipField(FieldAttributes field) {
                            return field.getDeclaredClass().equals(finalParentClazz1);
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> clazz) {
                            this.clazz = clazz;
                            return false;
                        }
                    });
            Gson gson2 = builder.create();
            String js = gson2.toJson(json);
            return gson2.fromJson(js, typeOfHashMap);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Transformar json a objeto.", e);
            return null;
        }
    }

    public static Object toObjecttoHashMap(Object json, List<String> ignoreFields, List<Class> ignoreClass) {
        if (json == null) {
            return null;
        }
        try {
            if (ignoreFields == null) {
                ignoreFields = new ArrayList<>();
            }
            if (ignoreClass == null) {
                ignoreClass = new ArrayList<>();
            }
            GsonBuilder builder = new GsonBuilder();
            Class finalParentClazz = null;
            Type typeOfHashMap = null;
            if (json instanceof Collection || json instanceof List || json instanceof ArrayList) {
                List l = (List) json;
                if (isNotEmpty(l)) {
                    finalParentClazz = ((List) json).get(0).getClass();
                } else {
                    finalParentClazz = Object.class;
                }
                typeOfHashMap = new TypeToken<List<Map<String, Object>>>() {
                }.getType();
            } else {
                typeOfHashMap = new TypeToken<Map<String, Object>>() {
                }.getType();
                finalParentClazz = json.getClass();
            }
            Class finalParentClazz1 = finalParentClazz;
            List<String> finalIgnoreFields = ignoreFields;
            List<Class> finalIgnoreClass = ignoreClass;
            builder.enableComplexMapKeySerialization().setDateFormat("yyyy-MM-dd HH:mm:ss").excludeFieldsWithModifiers(Modifier.STATIC).setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER).setExclusionStrategies(new ExclusionStrategy() {
                private Class clazz;

                @Override
                public boolean shouldSkipField(FieldAttributes field) {
                    if (field.getAnnotation(JsonIgnore.class) != null) {
                        return true;
                    }
                    if (finalIgnoreFields.contains(field.getName())) {
                        return true;
                    }
                    return field.getDeclaredClass().equals(finalParentClazz1);
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    this.clazz = clazz;
                    return finalIgnoreClass.contains(clazz);
                }
            }).registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
            Gson gson2 = builder.create();
            String js = gson2.toJson(json);
            Object fromJson = gson2.fromJson(js, typeOfHashMap);
            return fromJson;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Transformar json a objeto. Data: " + json, e);
            return null;
        }
    }

    public static String toJson(Object obj) {
        Map<String, Class> m = new HashMap<>();
        try {
            if (obj == null) {
                return null;
            }
            Class parentClazz = obj.getClass();
            if (parentClazz.equals(Collection.class) || parentClazz.equals(ArrayList.class)) {
                List c = (List) obj;
                if (c.size() == 0) {
                    return null;
                }
                parentClazz = c.get(0).getClass();
            }
            GsonBuilder builder = new GsonBuilder();
            Class finalParentClazz = parentClazz;
            builder.enableComplexMapKeySerialization().setDateFormat("yyyy-MM-dd HH:mm:ss").excludeFieldsWithModifiers(Modifier.STATIC).setExclusionStrategies(new ExclusionStrategy() {
                private Class clazz;

                @Override
                public boolean shouldSkipField(FieldAttributes field) {
                    if (field.getAnnotation(JsonIgnore.class) != null) {
                        return true;
                    }
                    return field.getDeclaredType().equals(finalParentClazz);
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    this.clazz = clazz;
                    return false;
                }
            })/*.setPrettyPrinting()*/;
            builder.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
            Gson gson2 = builder.create();
            return gson2.toJson(obj);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Generar Json.", e);
        }
        return null;
    }

    public static String getUltimaPosicion(String key, String s) {
        try {
            String[] sp = key.split(s);
            return sp[sp.length - 1];
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Obtener ultima posición desde un string.", e);
            return null;
        }
    }

    public static boolean isEmpty(Collection l) {
        if (l == null) {
            return true;
        } else return l.size() == 0;
    }

    @SuppressWarnings("rawtypes")
    public static boolean isNotEmpty(Collection l) {
        return !isEmpty(l);
    }
}