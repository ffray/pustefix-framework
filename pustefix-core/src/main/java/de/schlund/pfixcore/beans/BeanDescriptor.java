/*
 * This file is part of Pustefix.
 *
 * Pustefix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Pustefix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Pustefix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.schlund.pfixcore.beans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import de.schlund.pfixcore.beans.metadata.Bean;
import de.schlund.pfixcore.beans.metadata.Beans;
import de.schlund.pfixcore.beans.metadata.Property;

/**
 * Bean property descriptor for bean classes. Introspects a class to find all
 * bean properties by its getter and setter methods.
 * 
 * @author mleidig@schlund.de
 */
public class BeanDescriptor {

    protected final static Logger LOG = Logger.getLogger(BeanDescriptor.class);

    Class<?> clazz;

    HashMap<String, Type> types = new HashMap<String, Type>();
    HashMap<String, Method> getters = new HashMap<String, Method>();
    HashMap<String, Method> setters = new HashMap<String, Method>();
    HashMap<String, Field> directFields = new HashMap<String, Field>();

    public <T> BeanDescriptor(Class<T> clazz) {
        this.clazz = clazz;
        introspectNew(clazz, null);
    }

    public <T> BeanDescriptor(Class<T> clazz, Beans metadata) {
        this.clazz = clazz;
        introspectNew(clazz, metadata);
    }

    private <T> void introspectNew(Class<T> clazz, Beans metadata) {
        Field[] fields = clazz.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (!Modifier.isStatic(fields[i].getModifiers()) && !Modifier.isFinal(fields[i].getModifiers())) {
                Method getter = null;
                try {
                    getter = clazz.getMethod(createGetterName(fields[i].getName()), new Class[0]);
                    if (getter != null && (Modifier.isStatic(getter.getModifiers()) || getter.getReturnType() == void.class)) {
                        getter = null;
                    }
                } catch (NoSuchMethodException x) {
                }
                if (getter == null && fields[i].getDeclaringClass().equals(Boolean.TYPE)) {
                    try {
                        getter = clazz.getMethod(createBooleanGetterName(fields[i].getName()), new Class[0]);
                        if (getter != null && (Modifier.isStatic(getter.getModifiers()) || getter.getReturnType() == void.class)) {
                            getter = null;
                        }
                    } catch (NoSuchMethodException x) {
                    }
                }
                if (getter == null) {
                    String origPropName = fields[i].getName();
                    String propName = origPropName;
                    boolean isExcluded = false;
                    Bean beanMeta = null;
                    if (metadata != null) beanMeta = metadata.getBean(fields[i].getDeclaringClass().getName());
                    if (beanMeta != null) {
                        Property propMeta = beanMeta.getProperty(origPropName);
                        if (beanMeta.isExcludedByDefault()) {
                            if (propMeta == null || propMeta.isExcluded()) isExcluded = true;
                        } else {
                            if (propMeta != null && propMeta.isExcluded()) isExcluded = true;
                        }
                        if (propMeta != null && propMeta.getAlias() != null) propName = propMeta.getAlias();
                    } else {
                        ExcludeByDefault exDef = fields[i].getDeclaringClass().getAnnotation(ExcludeByDefault.class);
                        if (exDef == null) {
                            Exclude ex = fields[i].getAnnotation(Exclude.class);
                            if (ex != null) isExcluded = true;
                        } else {
                            Include inc = fields[i].getAnnotation(Include.class);
                            if (inc == null) isExcluded = true;
                        }
                        Alias alias = fields[i].getAnnotation(Alias.class);
                        if (alias != null) propName = alias.value();
                    }
                    if (!isExcluded) {
                        if (types.get(propName) != null) throw new IntrospectionException("Duplicate bean property name: " + propName);
                        types.put(propName, fields[i].getGenericType());
                        directFields.put(propName, fields[i]);
                    }
                } else if (getter.getReturnType() != fields[i].getType()) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Ignore public field '" + fields[i].getName() + "' cause getter with different " + "return type found: "
                                + getter.getReturnType().getName() + " -> " + fields[i].getType().getName());
                }
            }
        }
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getDeclaringClass() != Object.class && methods[i].getDeclaringClass() != Enum.class) {
                if (!Modifier.isStatic(methods[i].getModifiers()) && !methods[i].isSynthetic()) {
                    String origPropName = extractPropertyName(methods[i]);
                    if (origPropName != null) {
                        String propName = origPropName;
                        boolean isExcluded = false;
                        Bean beanMeta = null;
                        if (metadata != null) beanMeta = metadata.getBean(methods[i].getDeclaringClass().getName());
                        if (beanMeta != null) {
                            Property propMeta = beanMeta.getProperty(origPropName);
                            if (beanMeta.isExcludedByDefault()) {
                                if (propMeta == null || propMeta.isExcluded()) isExcluded = true;
                            } else {
                                if (propMeta != null && propMeta.isExcluded()) isExcluded = true;
                            }
                            if (propMeta != null && propMeta.getAlias() != null) propName = propMeta.getAlias();
                        } else {
                            Field field = null;
                            try {
                                field = clazz.getField(origPropName);
                                if (field != null && (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())))
                                    field = null;
                            } catch (NoSuchFieldException x) {
                            }
                            ExcludeByDefault exDef = methods[i].getDeclaringClass().getAnnotation(ExcludeByDefault.class);
                            if (exDef == null) {
                                Exclude ex = methods[i].getAnnotation(Exclude.class);
                                if (ex == null && field != null) ex = field.getAnnotation(Exclude.class);
                                if (ex != null) isExcluded = true;
                            } else {
                                Include inc = methods[i].getAnnotation(Include.class);
                                if (inc == null && field != null) inc = field.getAnnotation(Include.class);
                                if (inc == null) isExcluded = true;
                            }
                            Alias alias = methods[i].getAnnotation(Alias.class);
                            if (alias == null && field != null) alias = field.getAnnotation(Alias.class);
                            if (alias != null) propName = alias.value();
                        }
                        if (!isExcluded) {
                            if (getters.get(propName) != null) throw new IntrospectionException("Duplicate bean property name: " + propName);
                            getters.put(propName, methods[i]);
                            types.put(propName, methods[i].getGenericReturnType());
                            Method setter = null;
                            try {
                                setter = clazz.getMethod(createSetterName(origPropName), new Class[] { methods[i].getReturnType() });
                                if (setter.getReturnType() != void.class) setter = null;
                            } catch (NoSuchMethodException x) {
                            }
                            if (setter != null) {
                                setters.put(propName, setter);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check, whether the method is a getter method according
     * to the Java Bean specifications and extract the name
     * of the property.
     * 
     * If the method is not getter method, null will
     * be returned.
     * 
     * @param method
     * @return
     */
    private String extractPropertyName(Method method) {
        if (method.getParameterTypes().length != 0) {
            return null;
        }
        String methodName = method.getName();
        if (methodName.length() > 3
            && Character.isUpperCase(methodName.charAt(3))
            && methodName.startsWith("get")) {
            String name = methodName.substring(3);
            if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
                return name;
            }
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        if (methodName.length() > 2
            && Character.isUpperCase(methodName.charAt(2))
            && methodName.startsWith("is")) {
            String name = methodName.substring(2);
            if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
                return name;
            }
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return null;
        
    }

    private String createSetterName(String propName) {
        return "set" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
    }

    private String createGetterName(String propName) {
        return "get" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
    }

    private String createBooleanGetterName(String propName) {
        return "is" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
    }

    public Set<String> getReadableProperties() {
        return types.keySet();
    }

    public Set<String> getWritableProperties() {
        return setters.keySet();
    }

    public Method getSetMethod(String propName) {
        return setters.get(propName);
    }

    public Method getGetMethod(String propName) {
        return getters.get(propName);
    }

    public Field getDirectAccessField(String propName) {
        return directFields.get(propName);
    }

    public Type getPropertyType(String propName) {
        return types.get(propName);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Class:\n");
        sb.append("\t" + clazz.getName() + "\n");
        sb.append("Properties:\n");
        Iterator<String> it = getReadableProperties().iterator();
        while (it.hasNext()) {
            String propName = it.next();
            sb.append("\t" + propName + "\n");
        }
        return sb.toString();
    }
}