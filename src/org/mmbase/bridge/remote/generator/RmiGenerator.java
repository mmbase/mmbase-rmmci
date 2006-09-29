/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */

package org.mmbase.bridge.remote.generator;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;


/**
 * @javadoc
 *
 * @since MMBase-1.9
 * @author Pierre van Rooden
 * @version $Id: RmiGenerator.java,v 1.1 2006-09-29 08:59:07 pierre Exp $
 */
public class RmiGenerator extends AbstractClassGenerator {

    String originalName;
    String interfaceName;
    String rmiName;

    public RmiGenerator(Class c) {
        super(c);
    }

    public void generateHeader() {
        generateLicense();
        buffer.append("package org.mmbase.bridge.remote.rmi;\n");
        buffer.append("\n");
        buffer.append("import org.mmbase.bridge.*;\n");
        buffer.append("import org.mmbase.datatypes.*;\n");
        buffer.append("import org.mmbase.security.*;\n");
        buffer.append("import org.mmbase.storage.search.*;\n");
        buffer.append("import org.mmbase.util.functions.*;\n");
        buffer.append("import org.mmbase.util.logging.*;\n");
        buffer.append("import java.util.*;\n");
        buffer.append("import java.rmi.*;\n");
        buffer.append("import java.rmi.server.*;\n");
        buffer.append("import org.mmbase.bridge.remote.*;\n\n");
        buffer.append("import org.mmbase.bridge.remote.util.*;\n\n");

        buffer.append("/**\n");
        buffer.append(" * " + rmiName + " in a generated implementation of " + interfaceName + "<br />\n");
        buffer.append(" * This implementation is used by rmci to create a stub and skeleton for communication between remote and server.\n");
        buffer.append(" * @since MMBase-1.9\n");
        buffer.append(" * @author generated by org.mmbase.bridge.remote.generator.RmiGenerator\n");
        buffer.append(" */\n");
        buffer.append(" //DO NOT EDIT THIS FILE. IT IS GENERATED by org.mmbase.bridge.remote.generator.RMMCI\n");
    }

    protected void appendMethod(Method m) {
        appendMethodHeader(m, true, true);
        buffer.append(" throws RemoteException {\n");
        int paramCounter = 0;
        for (Type t:m.getGenericParameterTypes()) {
            //
            Type ct = getComponentType(t);
            if (needsRemote(ct)) {
                if (((Class)ct).isArray()) { // not remote!
                    indent4();
                    appendTypeInfo(ct,true,false);
                    buffer.append("[] localArg" + paramCounter + " = new ");
                    appendTypeInfo(ct,true,false);
                    buffer.append("[arg" + paramCounter + ".length];\n");
                    indent4();
                    buffer.append("for(int i = 0; i <arg" + paramCounter + ".length; i++ ) {\n");
                    indent6();
                    buffer.append("localArg" + paramCounter + "[i] = (");
                    appendTypeInfo(ct);
                    buffer.append(")StubToLocalMapper.get(arg" + paramCounter + "[i] == null ? \"\" + null : arg" + paramCounter + "[i].getMapperCode());");
                    indent4();
                    buffer.append("}\n");
                } else {
                    indent4();
                    appendTypeInfo(ct,true,false);
                    buffer.append(" localArg" + paramCounter + " = (");
                    appendTypeInfo(ct,true,false);
                    buffer.append(")StubToLocalMapper.get(arg" + paramCounter);
                    buffer.append(" == null ? \"\" + null : arg" + paramCounter + ".getMapperCode());\n");
                }
            }
            paramCounter++;
        }

        indent4();
        Type returnType = m.getGenericReturnType();
        if (!returnType.equals(Void.TYPE)) {
            appendTypeInfo(returnType);
            buffer.append(" retval = (");
            appendTypeInfo(returnType);
            buffer.append(")");
        }

        boolean needToWrap = needtoWrap(returnType);
        if (needToWrap) {
            buffer.append("ObjectWrapper.localToRMIObject(originalObject." + m.getName() + "(");
        } else {
            buffer.append("originalObject." + m.getName() + "(");
        }

        paramCounter = 0;
        Type[] parameters = m.getGenericParameterTypes();
        for (Type t : parameters) {
            Type componentType = getComponentType(t);
            if (needsRemote(componentType)) {
                buffer.append("localArg" + paramCounter);
            } else if (isBasicType(componentType) && !((Class)componentType).isArray()) {
                buffer.append("(" + ((Class)componentType).getName() + ")ObjectWrapper.rmiObjectToLocal(arg" + paramCounter + ")");
            } else {
                buffer.append("arg" + paramCounter);
            }
            paramCounter++;
            if (paramCounter < parameters.length) {
                buffer.append(", ");
            }
        }
        if (needToWrap) {
            buffer.append(")");
        }
        buffer.append(");\n");

        if (!returnType.equals(Void.TYPE)) {
            indent4();
            buffer.append("return retval;\n");
        }

        buffer.append("  }\n\n");
    }

    public void generate() {
        originalName = getShortName(currentClass);
        interfaceName = "Remote" + originalName;
        rmiName = interfaceName + "_Rmi";

        generateHeader();

        buffer.append("public class " + rmiName);
        appendTypeParameters(currentClass.getTypeParameters());

        buffer.append(" extends UnicastRemoteObject");
        buffer.append(" implements " + interfaceName);
        appendTypeParameters(currentClass.getTypeParameters(), true);
        buffer.append(", ");

        Type[] interfaces = currentClass.getGenericInterfaces();
        resolveTypeParameters(interfaces);
        for (int i = 0; i < interfaces.length; i++) {
            Type ct = getComponentType(interfaces[i]);
            if (needsRemote(ct)) {
                appendTypeInfo(interfaces[i], true, true);
                buffer.append(", ");
            }
        }
        buffer.append("Unreferenced  {\n");

        buffer.append("   private static Logger log = Logging.getLoggerInstance(" + rmiName + ".class);\n");
        buffer.append("   //original object\n");
        buffer.append("   " + originalName + " originalObject;\n\n");
        buffer.append("   //mapper code\n");
        buffer.append("   String mapperCode = null;\n\n");


        //constructor
        buffer.append("   public " + rmiName + "(" + originalName + " originalObject) throws RemoteException{\n");
        buffer.append("      super();\n");
        buffer.append("      log.debug(\"new " + rmiName + "\");\n");
        buffer.append("      this.originalObject = originalObject;\n");
        buffer.append("      mapperCode = StubToLocalMapper.add(this.originalObject);\n");
        buffer.append("   }\n");

        // methods
        appendMethods();

        buffer.append("\n");
        buffer.append("   public String getMapperCode() throws RemoteException{\n");
        buffer.append("      return mapperCode;\n");
        buffer.append("   }\n");
        buffer.append("\n");
        buffer.append("   //clean up StubToLocalMapper when the class is unreferenced\n");
        buffer.append("   public void unreferenced() {\n");
        buffer.append("      if (StubToLocalMapper.remove(mapperCode)){\n");
        buffer.append("         mapperCode = null;\n");
        buffer.append("      }\n");
        buffer.append("   }\n");

        buffer.append("}\n");
    }

    public void generate(String rmiDir) {
        generate();
        String filename = rmiDir + rmiName + ".java";
        writeSourceFile(filename);
    }

}