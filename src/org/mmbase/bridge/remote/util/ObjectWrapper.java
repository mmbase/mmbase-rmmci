package org.mmbase.bridge.remote.util;

import java.rmi.*;
import org.mmbase.bridge.*;
import org.mmbase.bridge.remote.*;
import org.mmbase.bridge.remote.rmi.*;
import org.mmbase.bridge.remote.implementation.*;

import org.mmbase.util.logging.*;
/**
 * Util class that performs translations of object to remote objects and 
 * remote bridge node implementations
 **/
public abstract class ObjectWrapper {
    private static Logger log = Logging.getLoggerInstance(ObjectWrapper.class);

    /*
     * method called to create a RMI object based on a "local" object  
     */
    public static Object localToRMIObject(Object o) throws RemoteException {
        //if the object is null -> return null
        if (o == null) {
            return null;
        }

        String className = o.getClass().getName();

        if (className.indexOf("mmbase") == -1) {
            //if it is not a mmbase class we don't care and hope the object is serialisable
            return o;
        }

        //it's an mmbase object
        Object retval = null;
        if (o instanceof Node) {
            retval = new RemoteNode_Rmi((Node)o);
        } else if (o instanceof Query) {
            retval = new RemoteQuery_Rmi((Query)o);
        } else {

            log.warn("please add a  wrapper for objects of type " + className);
        }
        return retval;
    }

    /*
     * 
     */
    public static Object rmiObjectToRemoteImplementation(Object o) throws RemoteException {
        if (o == null) {
            return null;
        }
        Object retval = null;
        String className = o.getClass().getName();

        if (className.indexOf("mmbase") != -1) {
            if (o instanceof RemoteNode) {
                retval = new RemoteNode_Impl((RemoteNode)o);
            } else if (o instanceof RemoteQuery) {
                retval = new RemoteQuery_Impl((RemoteQuery)o);
            } else {
                log.warn("please add a  wrapper for objects of type " + className);
            }

        } else {
            retval = o;
        }
        //System.err.println("wrapped " + className + " to " + retval.getClass().getName());
        return retval;
    }

    public static Object remoteImplementationToRMIObject(Object o) {
        if (o == null) {
            return null;
        }
        String className = o.getClass().getName();
        Object retval = null;
        if (o instanceof MappedObject) {
            retval = ((MappedObject)o).getWrappedObject();
        } else {
            retval = o;
        }
        //System.err.println("wrapped "+ className + " to " + retval.getClass().getName());
        return retval;
    }

    public static Object rmiObjectToLocal(Object o) throws RemoteException {
        if (o == null) {
            return null;
        }
        if (o instanceof ServerMappedObject) {
            return StubToLocalMapper.get(((ServerMappedObject)o).getMapperCode());
        } else {
            return o;
        }
    }
}
