/*
 * Copyright 2003,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis.management;

import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.i18n.Messages;
import org.apache.commons.logging.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * class to act as a dynamic loading registrar to commons-modeler, so
 * as to autoregister stuff
 *
 * @link http://www.webweavertech.com/costin/archives/000168.html#000168
 */
public class Registrar {
    /**
     * our log
     */
            protected static Log log = LogFactory.getLog(Registrar.class.getName());

    /**
     * register using reflection. The perf hit is moot as jmx is
     * all reflection anyway
     *
     * @param objectToRegister
     * @param name
     * @param context
     */
    public static boolean register(Object objectToRegister,
                                   String name, String context) {
        if (isBound()) {
            if (log.isDebugEnabled()) {
                log.debug("Registering " + objectToRegister + " as "
                        + name);
            }
            return modelerBinding.register(objectToRegister, name, context);
        } else {
            return false;
        }
    }

    /**
     * Check for being bound to a modeler -this will force
     * a binding if none existed.
     *
     * @return
     */

    public static boolean isBound() {
        createModelerBinding();
        return modelerBinding.canBind();
    }

    /**
     * create the modeler binding if it is needed
     * At the end of this call, modelerBinding != null
     */
    private static void createModelerBinding() {
        if (modelerBinding == null) {
            modelerBinding = new ModelerBinding();
        }
    }

    /**
     * the inner class that does the binding
     */
            private static ModelerBinding modelerBinding = null;

    /**
     * This class provides a dynamic binding to the
     * commons-modeler registry at run time
     */
    static class ModelerBinding {
        /**
         * the constructor binds
         */
        public ModelerBinding() {
            bindToModeler();
        }

        /**
         * can the binding bind?
         *
         * @return true iff the classes are bound
         */
        public boolean canBind() {
            return registry != null;
        }

        /**
         * log
         */
                protected static Log log = LogFactory.getLog(ModelerBinding.class.getName());
        /**
         * registry object
         */
                Object registry;
        /**
         * method to call
         */
                Method registerComponent;

        /**
         * register using reflection. The perf hit is moot as jmx is
         * all reflection anyway
         *
         * @param objectToRegister
         * @param name
         * @param context
         */
        public boolean register(Object objectToRegister, String name, String context) {
            if (registry != null) {
                Object args[] = new Object[]{objectToRegister, name, context};
                try {
                    registerComponent.invoke(registry, args);
                    if (log.isDebugEnabled()) {
                        log.debug("Registered " + name + " in " + context);
                    }
                } catch (IllegalAccessException e) {
                    log.error(e);
                    return false;
                } catch (IllegalArgumentException e) {
                    log.error(e);
                    return false;
                } catch (InvocationTargetException e) {
                    log.error(e);
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * bind to the modeler; return success/failure flag
         *
         * @return true if the binding worked
         */
        private boolean bindToModeler() {
            Exception ex = null;
            Class clazz;
            try {
                clazz = Class.forName("org.apache.commons.modeler.Registry");
            } catch (ClassNotFoundException e) {
                // just ignore it silently if we don't have commons-modeler.jar around
                registry = null;
                return false;
            }
            try {
                Class[] getRegistryArgs = new Class[]{Object.class, Object.class,};
                Method getRegistry = clazz.getMethod("getRegistry", getRegistryArgs);
                Object[] getRegistryOptions = new Object[]{null, null};
                registry = getRegistry.invoke(null, getRegistryOptions);
                Class[] registerArgs = new Class[]{Object.class,
                    String.class,
                    String.class};
                registerComponent = clazz.getMethod("registerComponent", registerArgs);
            } catch (IllegalAccessException e) {
                ex = e;
            } catch (IllegalArgumentException e) {
                ex = e;
            } catch (InvocationTargetException e) {
                ex = e;
            } catch (NoSuchMethodException e) {
                ex = e;
            }
            // handle any of these exceptions
            if (ex != null) {
                //log the error
                log.warn(Messages.getMessage("Registrar.cantregister"), ex);
                //mark the registration as a failure
                registry = null;
                //and fail
                return false;
            } else {
                //success
                return true;
            }
        }
    }
}
