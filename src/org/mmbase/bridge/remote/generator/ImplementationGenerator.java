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
 * @version $Id: ImplementationGenerator.java,v 1.2 2006-09-29 15:01:56 pierre Exp $
 */
public class ImplementationGenerator extends AbstractClassGenerator {

    String originalName;
    String interfaceName;
    String implementationName;

    public ImplementationGenerator(Class c) {
        super(c);
    }

    public void generateHeader() {
        generateLicense();
        //create the default imports for the interface
        buffer.append("package org.mmbase.bridge.remote.implementation;\n");
        buffer.append("\n");
        buffer.append("import java.util.*;\n");
        buffer.append("import org.mmbase.bridge.*;\n");
        buffer.append("import org.mmbase.datatypes.*;\n");
        buffer.append("import org.mmbase.storage.search.*;\n");
        buffer.append("import org.mmbase.util.functions.*;\n");
        buffer.append("import org.mmbase.bridge.remote.*;\n");
        buffer.append("import org.mmbase.security.*;\n\n");
        buffer.append("import org.mmbase.bridge.remote.util.*;\n\n");
        buffer.append("/**\n");
        buffer.append(" * " + implementationName + " in a generated implementation of " + originalName + "<br />\n");
        buffer.append(" * This implementation is used by a local class when the MMCI is called remotely\n");
        buffer.append(" * @since MMBase-1.9\n");
        buffer.append(" * @author generated by org.mmbase.bridge.remote.generator.RmiGenerator\n");
        buffer.append(" */\n");
        buffer.append(" //DO NOT EDIT THIS FILE. IT IS GENERATED by org.mmbase.bridge.remote.generator.RMMCI\n");
    }

    boolean abstractListMethod(Method m) {
        String name = m.getName();
        return name.equals("toArray") || name.equals("iterator") || name.equals("listIterator");
    }

    protected void appendMethod(Method m) {
        if (!abstractListMethod(m)) {
            appendMethodHeader(m, false, false);
            buffer.append(" {\n");
            buffer.append("    try {\n");
            String name = m.getName();
            String remoteName = name;
            if (isBasicMethod(m)) remoteName = "wrapped_" + name;
            int paramCounter = 0;
            for (Type t:m.getGenericParameterTypes()) {
                //
                Type ct = getComponentType(t);
                if (ct instanceof TypeVariable) {
                    if (((TypeVariable)ct).getBounds().length > 0) {
                        buffer.append("/* " + ct + ((TypeVariable)ct).getBounds()[0] + " */ ");
                    }
                }
                if (needsRemote(ct)) {
                    if (((Class)ct).isArray()) { // not remote!
                        indent6();
                        appendTypeInfo(ct,true,true);
                        buffer.append("[] remoteArg" + paramCounter + " = new ");
                        appendTypeInfo(ct,true,true);
                        buffer.append("[arg" + paramCounter + ".length];\n");
                        indent6();
                        buffer.append("for(int i = 0; i <arg" + paramCounter + ".length; i++ ) {\n");
                        indent8();
                        buffer.append("localArg" + paramCounter + "[i] = (");
                        appendTypeInfo(ct);
                        buffer.append(")( arg" + paramCounter + "[i] == null ? null : ");
                        buffer.append("((MappedObject) arg" + paramCounter + "[i]).getWrappedObject());\n");
                        indent6();
                        buffer.append("}\n");
                    } else {
                        indent6();
                        appendTypeInfo(ct,true,true);
                        buffer.append(" remoteArg" + paramCounter + " = (");
                        appendTypeInfo(ct,true,true);
                        buffer.append(")(arg" + paramCounter + " == null ? null : ");
                        buffer.append("((MappedObject) arg" + paramCounter + ").getWrappedObject());\n");
                    }
                }
                paramCounter++;
            }

            indent6();
            Type returnType = m.getGenericReturnType();
            if (!returnType.equals(Void.TYPE)) {
                appendTypeInfo(returnType,true,false);
                buffer.append(" retval = (");
                appendTypeInfo(returnType,true,false);
                buffer.append(")");
            }

            boolean needToWrap = needtoWrap(returnType);
            if (needToWrap) {
                buffer.append("ObjectWrapper.rmiObjectToRemoteImplementation(remoteObject." + remoteName + "(");
            } else {
                buffer.append("remoteObject." + remoteName + "(");
            }

            paramCounter = 0;
            Type[] parameters = m.getGenericParameterTypes();
            for (Type t : parameters) {
                Type componentType = getComponentType(t);
                if (needsRemote(componentType)) {
                    buffer.append("remoteArg" + paramCounter);
                } else if (isBasicType(componentType) && !((Class)componentType).isArray()) {
                    buffer.append("(" + ((Class)componentType).getName() + ")ObjectWrapper.remoteImplementationToRMIObject(arg" + paramCounter + ")");
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
                indent6();
                buffer.append("return retval;\n");
            }

            buffer.append("    } catch (RuntimeException e) {\n");
            buffer.append("      throw e ;\n");
            buffer.append("    } catch(Exception e) {\n");
            buffer.append("      throw new BridgeException(e.getMessage(), e);\n");
            buffer.append("    }\n");
            buffer.append("  }\n\n");
        }
    }

    public void generate() {
        originalName = getShortName(currentClass);
        interfaceName = "Remote" + originalName;
        implementationName = interfaceName + "_Impl";

        generateHeader();

        buffer.append("public class " + implementationName);
        appendTypeParameters(currentClass.getTypeParameters(), false, false);
        if (List.class.isAssignableFrom(currentClass)) {
            buffer.append(" extends AbstractList");
            appendTypeParameters(currentClass.getTypeParameters(), true);
        }
        buffer.append(" implements " + originalName);
        appendTypeParameters(currentClass.getTypeParameters(), true);

        Type[] interfaces = currentClass.getGenericInterfaces();
        resolveTypeParameters(interfaces);

        buffer.append(", MappedObject  {\n");

        buffer.append("  //remote object\n");
        buffer.append("  " + interfaceName);
//        appendTypeParameters(currentClass.getTypeParameters(), true);
        buffer.append(" remoteObject;\n\n");

        //constructor
        buffer.append("  public " + implementationName + "(" + interfaceName + " remoteObject) {\n");
        buffer.append("    super();\n");
        buffer.append("    this.remoteObject = remoteObject;\n");
        buffer.append("  }\n\n");

        // methods
        // methods
        appendMethods();

        buffer.append("  public String getMapperCode() {\n");
        buffer.append("    String code =null;\n");
        buffer.append("    try { code = remoteObject.getMapperCode(); } catch (Exception e) {}\n");
        buffer.append("    return code;\n");
        buffer.append("  }\n\n");
        buffer.append("  public Object getWrappedObject() {\n");
        buffer.append("    return remoteObject;\n");
        buffer.append("  }\n\n");

        buffer.append("}\n");
    }

    public void generate(String implementationDir) {
        generate();
        String filename = implementationDir + implementationName + ".java";
        writeSourceFile(filename);
    }

}
