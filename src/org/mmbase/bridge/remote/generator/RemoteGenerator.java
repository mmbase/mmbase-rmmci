/*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */

package org.mmbase.bridge.remote.generator;
import java.util.*;
import java.io.*;

/**
 * @author Kees Jongenburger <keesj@dds.nl>
 **/
public class RemoteGenerator {

    private MMCI mmci = null;
    private String targetDir = null;

    /*
     * main method to generate the Remote MMCI files
     * this method wil create 2 directories
     * <UL>
     *    <LI>org/mmbase/bridge/remote/rmi</LI>
     *    <LI>org/mmbase/bridge/remote/implementation</LI>
     * </UL>
     * @param targetDir the root directory of the mmbase source where the generated sources
     * should be (e.q) /home/mmbase/src/
     * @param mmciFile the location of the MMCI.xml file generated by org.mmbase.bridge.generator.MMCI
     */
    public RemoteGenerator(String targetDir, String mmciFile) throws Exception {
        //check if the org/mmbase/bridge/remote dir exists
        File file = new File(targetDir + "/org/mmbase/bridge/remote");
        if (!file.exists() || !file.isDirectory()) {
            throw new Exception("directory {" + file.getName() + "} does not contain a sub directory org/mmbase/bridge/remote. this is required for RemoteGenerator to work");
        }
        file = new File(targetDir + "/org/mmbase/bridge/remote/rmi");
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(targetDir + "/org/mmbase/bridge/remote/implementation");
        if (!file.exists()) {
            file.mkdirs();
        }
        mmci = MMCI.getDefaultMMCI(mmciFile);
        this.targetDir = targetDir;
        Enumeration enum = mmci.getClasses().elements();
        while (enum.hasMoreElements()) {

            XMLClass xmlClass = (XMLClass)enum.nextElement();
            String name = xmlClass.getName();

            if (name.indexOf("org.mmbase") != -1) {
                generateInterface(xmlClass);
                generateRmi(xmlClass);
                generateImplementation(xmlClass);
            }
        }
        generateObjectWrappers(mmci);
    }

    /**
     * This method generates an (RMI)remote interface based on
     * an XMLClass
     */
    public void generateInterface(XMLClass xmlClass) {
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName;
        StringBuffer sb = new StringBuffer();

        //create the default imports for the interface
        sb.append("package org.mmbase.bridge.remote;\n");
        sb.append("\n");

        sb.append("import java.util.*;\n");
        sb.append("import java.rmi.*;\n");
        sb.append("\n");

        sb.append("/**\n");
        sb.append(" * " + className + " is a generated interface based on " + xmlClass.getName() + "<BR>\n");
        sb.append(" * This interface has almoost the same methods names as the " + xmlClass.getName() + " interface.\n");
        sb.append(" * The interface is created in such way that it can implement java.rmi.Remote.\n");
        sb.append(" * Where needed other return values or parameters are used.\n");
        sb.append(" * @Author Kees Jongenburger <keesj@dds.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE, IT IS GENERATED by org.mmbase.bridge.remote.remoteGenerator\n");

        String impl = " ServerMappedObject";
        //impl += ",java.io.Serializable";

        if (xmlClass.getImplements().indexOf("org.mmbase") != -1) {
            String m = xmlClass.getImplements();
            StringTokenizer st = new StringTokenizer(m, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.indexOf("org.mmbase") != -1) {
                    m = token;
                }
            }
            try {
                //XMLClass xmlc = mmci.getClass(xmlClass.getImplements());
                XMLClass xmlc = mmci.getClass(m);
                impl = "Remote" + xmlc.getShortName();
            } catch (NotInMMCIException e) {
                System.err.println("ERROR" + e.getMessage());
            }
        }

        sb.append("public interface " + className + " extends  " + impl + "{\n");
        System.err.println("generate interface " + className);

        //for every method in the XMLClass create an alternate method fot the
        //remote interface

        Enumeration methodsEnum = xmlClass.getMethods().elements();
        while (methodsEnum.hasMoreElements()) {
            XMLMethod xmlMethod = (XMLMethod)methodsEnum.nextElement();
            String methodName = xmlMethod.getName();
            boolean wrapped = false;
            if (methodName.equals("equals") || methodName.equals("hashCode") || methodName.equals("toString") || methodName.equals("clone")) {
                methodName = "wrapped_" + methodName;
                wrapped = true;
            }
            XMLClass returnType = xmlMethod.getReturnType();
            String retTypeName = xmlMethod.getReturnType().getShortName();

            //if the return type is in the MMBase bridge we need to
            //create a wrapper
            if (xmlMethod.getReturnType().getOriginalName().indexOf("org.mmbase") != -1) {
                retTypeName = "Remote" + retTypeName;
            }

            if (returnType.isArray) {
                sb.append("   public " + xmlMethod.getReturnType().getName() + "[] " + methodName + "(");
            } else {
                sb.append("   public " + retTypeName + " " + methodName + "(");
            }

            Iterator iter = xmlMethod.getParameterList().iterator();
            int counter = 0;
            while (iter.hasNext()) {
                counter++;
                XMLClass parameter = (XMLClass)iter.next();
                if (parameter != null) {
                    if (parameter.isArray) {
                        sb.append(parameter.getOriginalName() + "[] param" + counter);
                    } else {
                        if (parameter.getOriginalName().indexOf("org.mmbase") != -1) {
                            sb.append("Remote" + parameter.getShortName() + " param" + counter);
                        } else {
                            sb.append(parameter.getOriginalName() + " param" + counter);
                        }
                    }
                } else {
                    System.err.println("Class " + xmlMethod.getName() + " Parameter == null");
                }
                if (iter.hasNext()) {
                    sb.append(" ,");
                }
            }
            sb.append(") throws RemoteException;\n");
        }
        sb.append("}\n");
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/" + className + ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println("writeFile" + e.getMessage());
        }
    }
    /**
     * This method generates an (RMI)remote implementation based on
     * an XMLClass
     */
    public void generateRmi(XMLClass xmlClass) {
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName + "_Rmi";
        StringBuffer sb = new StringBuffer();
        sb.append("package org.mmbase.bridge.remote.rmi;\n");
        sb.append("\n");
        sb.append("import org.mmbase.bridge.*;\n");
        sb.append("import org.mmbase.storage.search.*;\n");
        sb.append("import org.mmbase.util.logging.*;\n");
        sb.append("import java.util.*;\n");
        sb.append("import java.rmi.*;\n");
        sb.append("import java.rmi.server.*;\n");
        sb.append("import org.mmbase.bridge.remote.*;\n\n");
        sb.append("import org.mmbase.bridge.remote.util.*;\n\n");

        sb.append("/**\n");
        sb.append(" * " + className + " in a generated implementation of Remote" + xmlClass.getShortName() + "<BR>\n");
        sb.append(" * This implementation is used by rmic to create a stub and skelton for communication between remote and server.\n");
        sb.append(" * @Author Kees Jongenburger <keesj@dds.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE, IT IS GENERATED by remote.remote.remoteGenerator\n");
        String impl = "";

        if (xmlClass.getImplements().indexOf("org.mmbase") != -1) {
            String m = xmlClass.getImplements();
            StringTokenizer st = new StringTokenizer(m, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.indexOf("org.mmbase") != -1) {
                    m = token;
                }
            }
            try {
                //XMLClass xmlc = mmci.getClass(xmlClass.getImplements());
                XMLClass xmlc = mmci.getClass(m);
                impl = ",Remote" + xmlc.getShortName();
            } catch (NotInMMCIException e) {
                System.err.println("ERROR" + e.getMessage());
            }
        }

        sb.append("public class " + className + " extends  UnicastRemoteObject implements Unreferenced,Remote" + shortName + impl + "  {\n");
        System.err.println("generate implementation " + className);

        sb.append("   //original object\n");
        sb.append("   " + xmlClass.getShortName() + " originalObject;\n\n");
        sb.append("   //mapper code\n");
        sb.append("   String mapperCode = null;\n\n");

        sb.append("   private static Logger log = Logging.getLoggerInstance(" + className + ".class);\n");

        //constructor
        sb.append("   public " + className + "(" + xmlClass.getShortName() + " originalObject) throws RemoteException{\n");
        sb.append("      super();\n");
        sb.append("      log.debug(\"new " + className + "\");\n");
        sb.append("      this.originalObject = originalObject;\n");
        sb.append("      mapperCode = StubToLocalMapper.add(this.originalObject);\n");
        sb.append("   }\n");

        Enumeration methodsEnum = xmlClass.getMethods().elements();
        while (methodsEnum.hasMoreElements()) {
            XMLMethod xmlMethod = (XMLMethod)methodsEnum.nextElement();
            String methodName = xmlMethod.getName();
            boolean wrapped = false;
            if (methodName.equals("equals") || methodName.equals("hashCode") || methodName.equals("toString") || methodName.equals("clone")) {
                methodName = "wrapped_" + methodName;
                wrapped = true;
            }
            XMLClass returnType = xmlMethod.getReturnType();
            String retTypeName = xmlMethod.getReturnType().getName();

            //if the return type is in the MMBase bridge we need to
            //create a wrapper
            if (xmlMethod.getReturnType().getOriginalName().indexOf("org.mmbase") != -1) {
                retTypeName = "Remote" + xmlMethod.getReturnType().getShortName();
            }

            if (returnType.isArray) {
                sb.append("   public " + retTypeName + "[] " + methodName + "(");
            } else {
                sb.append("   public " + retTypeName + " " + methodName + "(");
            }

            Iterator iter = xmlMethod.getParameterList().iterator();
            int counter = 0;
            while (iter.hasNext()) {
                counter++;
                XMLClass parameter = (XMLClass)iter.next();
                if (parameter.isArray) {
                    sb.append(parameter.getOriginalName() + "[] param" + counter);
                } else {
                    if (parameter.getOriginalName().indexOf("org.mmbase") != -1) {
                        sb.append("Remote" + parameter.getShortName() + " param" + counter);
                    } else {
                        sb.append(parameter.getOriginalName() + " param" + counter);
                    }
                }
                if (iter.hasNext()) {
                    sb.append(" ,");
                }
            }
            sb.append(") throws RemoteException{\n");

            if (xmlMethod.getReturnType().getName().indexOf("void") == -1) {
                if (xmlMethod.getReturnType().getName().indexOf("org.mmbase") != -1) {
                    if (!xmlMethod.getReturnType().isArray) {
                        sb.append("         Remote" + xmlMethod.getReturnType().getShortName() + " retval =(Remote" + xmlMethod.getReturnType().getShortName() + ")");
                    } else {
                        sb.append("         Remote" + xmlMethod.getReturnType().getShortName() + "[] retval =(Remote" + xmlMethod.getReturnType().getShortName() + "[])");
                    }
                } else {
                    if (!xmlMethod.getReturnType().isArray) {
                        sb.append("         " + xmlMethod.getReturnType().getName() + " retval =(" + xmlMethod.getReturnType().getName() + ")");
                    } else {
                        sb.append("         " + xmlMethod.getReturnType().getName() + "[] retval =(" + xmlMethod.getReturnType().getName() + "[])");
                    }

                }
            }

            String typeName = xmlMethod.getReturnType().getOriginalName();
            if (typeName.indexOf("org.mmbase") != -1 || typeName.equals("java.lang.Object") || typeName.equals("java.util.List")) {
                sb.append("ObjectWrapper.localToRMIObject(originalObject." + xmlMethod.getName() + "(");
            } else {
                sb.append("originalObject." + xmlMethod.getName() + "(");
            }

            int paramCounter = 0;
            Iterator paramIter = xmlMethod.getParameterList().iterator();
            while (paramIter.hasNext()) {
                XMLClass parameter = (XMLClass)paramIter.next();

                paramCounter++;
                if (parameter.getOriginalName().indexOf("org.mmbase") != -1) {
                    sb.append("(" + parameter.getShortName() + ")StubToLocalMapper.get(param" + paramCounter + " == null ? \"\" + null : param" + paramCounter + ".getMapperCode())");
                } else if ((parameter.getOriginalName().equals("java.lang.Object") || parameter.getOriginalName().equals("java.util.List")) && !parameter.isArray) {
                    sb.append("(" + parameter.getName() + ")ObjectWrapper.rmiObjectToLocal(param" + paramCounter + ")");
                } else {
                    sb.append(" param" + paramCounter);
                }
                if (paramIter.hasNext()) {
                    sb.append(" ,");
                }
            }
            if (typeName.indexOf("org.mmbase") != -1 || typeName.equals("java.lang.Object") || typeName.equals("java.util.List")) {
                sb.append(")");
            }
            if (!xmlMethod.getReturnType().getOriginalName().equals(xmlMethod.getReturnType().getName())) {
                sb.append(")");
            }
            sb.append(");\n");

            if (xmlMethod.getReturnType().getName().indexOf("void") == -1) {
                sb.append("return retval;\n");
            }
            sb.append("   }\n");
            sb.append("\n");
        }

        sb.append("\n");
        sb.append("   public String getMapperCode() throws RemoteException{\n");
        sb.append("      return mapperCode;\n");
        sb.append("   }\n");
        sb.append("\n");
        sb.append("   //clean up StubToLocalMapper when the class is unreferenced\n");
        sb.append("   public void unreferenced() {\n");
        sb.append("      if (StubToLocalMapper.remove(mapperCode)){\n");
        sb.append("         mapperCode = null;\n");
        sb.append("      }\n");
        sb.append("   }\n");
        sb.append("}\n");
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/rmi/" + className + ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * This method generates an (Remote)bridge implementation
     */
    public void generateImplementation(XMLClass xmlClass) {
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName + "_Impl";
        StringBuffer sb = new StringBuffer();
        sb.append("package org.mmbase.bridge.remote.implementation;\n");
        sb.append("\n");
        sb.append("import java.util.*;\n");
        sb.append("import org.mmbase.bridge.*;\n");
        sb.append("import org.mmbase.storage.search.*;\n");
        sb.append("import org.mmbase.bridge.remote.*;\n\n");
        sb.append("import org.mmbase.bridge.remote.util.*;\n\n");
        sb.append("/**\n");
        sb.append(" * " + className + " in a generated implementation of " + xmlClass.getShortName() + "<BR>\n");
        sb.append(" * This implementation is used by a local class when the MMCI is called remotely\n");
        sb.append(" * @Author Kees Jongenburger <keesj@dds.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE. IT IS GENERATED by remote.remote.remoteGenerator\n");
        String impl = xmlClass.getShortName() + ",MappedObject";
        //impl += ",java.io.Serializable";
        if (!xmlClass.getImplements().equals("")) {
            impl += ",";
        }
        boolean extendsAbstractList = false;
        String extendsString = "";
        if (xmlClass.getImplements().indexOf("List") != -1 && xmlClass.getImplements().indexOf("Iterator") == -1) {
            extendsString = " extends AbstractList ";
            extendsAbstractList = true;
        }
        sb.append("public class " + className + extendsString + " implements " + impl + xmlClass.getImplements() + "  {\n");
        System.err.println("generate implementation " + className);

        sb.append("   //original object\n");
        sb.append("   " + "Remote" + xmlClass.getShortName() + " originalObject;\n\n");

        //constructor
        sb.append("   public " + className + "(Remote" + xmlClass.getShortName() + " originalObject) {\n");
        sb.append("      super();\n");
        sb.append("      this.originalObject = originalObject;\n");
        sb.append("   }\n");

        Enumeration methodsEnum = xmlClass.getMethods().elements();
        while (methodsEnum.hasMoreElements()) {
            XMLMethod xmlMethod = (XMLMethod)methodsEnum.nextElement();
            String methodName = xmlMethod.getName();

            boolean wrapped = false;
            if (!methodName.equals("toArray") && !methodName.equals("iterator") && !methodName.equals("listIterator")) {

                if (methodName.equals("equals") || methodName.equals("hashCode") || methodName.equals("toString") || methodName.equals("clone")) {
                    wrapped = true;
                }
                XMLClass returnType = xmlMethod.getReturnType();
                String retTypeName = xmlMethod.getReturnType().getName();

                //if the return type is in the MMBase bridge we need to
                //create a wrapper
                if (xmlMethod.getReturnType().getOriginalName().indexOf("org.mmbase") != -1) {
                    retTypeName = xmlMethod.getReturnType().getShortName();
                }

                if (returnType.isArray) {
                    sb.append("   public " + retTypeName + "[] " + xmlMethod.getName() + "(");
                } else {
                    sb.append("   public " + retTypeName + " " + xmlMethod.getName() + "(");
                }

                Iterator iter = xmlMethod.getParameterList().iterator();
                int counter = 0;
                while (iter.hasNext()) {
                    counter++;
                    XMLClass parameter = (XMLClass)iter.next();
                    if (parameter.isArray) {
                        sb.append(parameter.getOriginalName() + "[] param" + counter);
                    } else {
                        sb.append(parameter.getOriginalName() + " param" + counter);
                    }
                    if (iter.hasNext()) {
                        sb.append(" ,");

                    }
                }
                sb.append(") {\n");
                sb.append("      try {\n");

                //**
                if (xmlMethod.getReturnType().getName().indexOf("void") == -1) {
                    if (!xmlMethod.getReturnType().isArray) {
                        sb.append("         " + xmlMethod.getReturnType().getName() + " retval =(" + xmlMethod.getReturnType().getName() + ")");
                    } else {
                        sb.append("         " + xmlMethod.getReturnType().getName() + "[] retval =(" + xmlMethod.getReturnType().getName() + "[])");
                    }
                }

                String typeName = xmlMethod.getReturnType().getOriginalName();
                if (typeName.indexOf("org.mmbase") != -1 || typeName.equals("java.lang.Object") || typeName.equals("java.util.List")) {
                    sb.append("ObjectWrapper.rmiObjectToRemoteImplementation(originalObject." + (wrapped ? "wrapped_" : "") + xmlMethod.getName() + "(");
                } else {
                    sb.append("originalObject." + (wrapped ? "wrapped_" : "") + xmlMethod.getName() + "(");
                }
                //sb.append("originalObject." + (wrapped ? "wrapped_" : "") + xmlMethod.getName() + "(");

                int paramCounter = 0;
                Iterator paramIter = xmlMethod.getParameterList().iterator();
                while (paramIter.hasNext()) {
                    XMLClass parameter = (XMLClass)paramIter.next();
                    paramCounter++;
                    if (parameter.getOriginalName().indexOf("org.mmbase") != -1) {

                        sb.append("(Remote" + parameter.getShortName() + ")( param" + paramCounter + " == null ? null : ((MappedObject) param" + paramCounter + ").getWrappedObject())");
                    } else {
                        if (parameter.getOriginalName().equals("java.lang.Object") || parameter.getOriginalName().equals("java.util.List")) {
                            String sss = className.substring(6, className.length() - 9);
                            if (sss.equals("String")) {
                                sb.append("param" + paramCounter);
                            } else {
                                sb.append("(" + parameter.getName() + ")ObjectWrapper.remoteImplementationToRMIObject(param" + paramCounter + ")");
                            }
                        } else {
                            sb.append("param" + paramCounter);
                        }
                    }
                    if (paramIter.hasNext()) {
                        sb.append(" ,");
                    }
                }
                if (typeName.indexOf("org.mmbase") != -1 || typeName.equals("java.lang.Object") || typeName.equals("java.util.List")) {
                    sb.append(")");
                }
                //sb.append(")");
                sb.append(");\n");
                if (typeName.indexOf("void") == -1) {

                    sb.append("    return retval;\n");
                }

                sb.append("      } catch (Exception e){ if (e instanceof BridgeException){ throw (BridgeException)e ;} else {throw new BridgeException(e.getMessage(),e);}}\n");

                sb.append("   }\n");
                sb.append("\n");
            }
        }
        sb.append(" public String getMapperCode(){ String code =null; try {code = originalObject.getMapperCode();} catch (Exception e){} return code ;}\n");
        sb.append(" public Object getWrappedObject(){  return originalObject ;}\n");
        sb.append("}\n");
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/implementation/" + className + ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    void generateObjectWrappers(MMCI mmci) {
        StringBuffer helper = new StringBuffer();
        helper.append("package org.mmbase.bridge.remote;");
        helper.append("import java.util.*;\n");
        helper.append("import java.rmi.*;\n");
        helper.append("import java.util.Vector;\n");

        helper.append("import org.mmbase.bridge.*;\n");
        helper.append("import org.mmbase.bridge.remote.*;\n");
        helper.append("import org.mmbase.bridge.remote.rmi.*;\n");
        helper.append("import org.mmbase.bridge.remote.implementation.*;\n");

        helper.append("import org.mmbase.storage.search.*;\n");
        helper.append("import org.mmbase.storage.search.Step;\n");
        helper.append("import org.mmbase.util.logging.*;\n");

        helper.append("public abstract class ObjectWrapperHelper {\n");

        StringBuffer sb = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();
        Vector v = mmci.getClasses();

        Collections.sort(v, new Comparator() {
            public int compare(Object one, Object two) {
                int retval = 0;
                XMLClass oneClass = (XMLClass)one;
                XMLClass twoClass = (XMLClass)two;

                Vector oneImpl = getSupperClasses(oneClass);
                Vector twoImplt = getSupperClasses(twoClass);
                
                Vector oneSub = getSubClasses(oneClass);
                Vector twoSub = getSubClasses(twoClass);

                //classes that don't implement anything always first
                if (oneImpl.size() == 0 || twoImplt.size() == 0) {
                	
                    return oneImpl.size() - twoImplt.size();
                }
                
                boolean oneExtendsTwo = false;
				boolean twoExtendsOne = false;
                for (int x = 0; x < oneSub.size(); x++) {
                    XMLClass j = (XMLClass)oneSub.get(x);
                    if (j.getName().equals(twoClass.getName())) {
						twoExtendsOne = true;
                    }
                }
                
				
				
				for (int x = 0; x < twoSub.size(); x++) {
					XMLClass j = (XMLClass)twoSub.get(x);
					if (j.getName().equals(oneClass.getName())) {
						
						oneExtendsTwo = true;
					}
				}

				if (oneExtendsTwo){
					//System.err.println(oneClass.getName() + " extends " + twoClass.getName());
					return 1;
				}
				
				if (twoExtendsOne){
					//System.err.println(oneClass.getName() + " is extended by " + twoClass.getName());
					return -1;
				}
				
				//System.err.println(oneClass.getName() + " equals " + twoClass.getName());
                /*
                if (ontImpl.indexOf(twoClass.getName()) != -1) {
                    retval = 1;
                } else if (twoImplt.indexOf(oneClass.getName()) != -1) {
                    retval = -1;
                }
                
                if (retval != 0) {
                    System.err.println(oneClass.getName() + " < " + twoClass.getName());
                
                }
                return retval;
                //System.err.println(oneClass.getName() + " ?? " + twoClass.getName());
                //return oneClass.getName().compareTo(twoClass.getName());
                 
                 */
                return 0;
            }

        });
		Collections.reverse(v);
        Enumeration enum = v.elements();

        sb.append("public static Object localToRMIObject(Object o) throws RemoteException {\n");
        sb.append("		Object retval = null;\n");
        sb2.append("public static Object rmiObjectToRemoteImplementation(Object o) throws RemoteException {\n");
        sb2.append("		Object retval = null;\n");

        boolean isFirst = true;
        while (enum.hasMoreElements()) {

            XMLClass xmlClass = (XMLClass)enum.nextElement();
            String name = xmlClass.getName();

            if (name.indexOf("org.mmbase") != -1) {
                if (!isFirst) {
                    sb.append("}else");
                    sb2.append("}else");
                }
                sb.append(" if (o instanceof " + xmlClass.getShortName() + ") {\n");
                sb.append("retval = new Remote" + xmlClass.getShortName() + "_Rmi((" + xmlClass.getShortName() + ")o);\n");

                sb2.append(" if (o instanceof Remote" + xmlClass.getShortName() + ") {\n");
                sb2.append("retval = new Remote" + xmlClass.getShortName() + "_Impl((Remote" + xmlClass.getShortName() + ")o);\n");
                isFirst = false;
            }
        }
        sb.append("		}\n;return retval ;\n}\n");
        sb2.append("	}\n;	return retval ;\n}\n");
        helper.append(sb);
        helper.append(sb2);
        helper.append("}\n");
        //System.out.println(helper.toString());
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/ObjectWrapperHelper.java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(helper.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static Vector getSubClasses(XMLClass xmlClass) {
        Vector retval = new Vector();
        MMCI mmci = null;
        try {
            mmci = MMCI.getDefaultMMCI();
        } catch (Exception e) {
            System.err.println("can not get MMCI");
        }
        Vector v = mmci.getClasses();
        Iterator iter = v.iterator();
        while (iter.hasNext()) {
            XMLClass f = (XMLClass)iter.next();
            Vector list = getSupperClasses(f);
            for (int x = 0; x < list.size(); x++) {
                XMLClass listItem = (XMLClass)list.get(x);
                if (listItem.getName().equals(xmlClass.getName())) {
                    retval.add(f);
                    retval.addAll(getSubClasses(f));
                    //System.err.println(xmlClass.getName() + " has subclass " + f.getName());
                }
            }
        }
        return retval;
    }

    private static Vector getSupperClasses(XMLClass xmlClass) {
        //System.err.println(xmlClass.getName());
        MMCI mmci = null;
        Vector retval = new Vector();
        try {
            mmci = MMCI.getDefaultMMCI();
        } catch (Exception e) {
            System.err.println("can not get MMCI");
        }

        if (xmlClass.getImplements() != null && xmlClass.getImplements().trim().length() > 0) {
            StringTokenizer st = new StringTokenizer(xmlClass.getImplements(), ",");
            while (st.hasMoreTokens()) {
                String newClass = st.nextToken();
                //System.err.println(newClass);
                if (newClass.indexOf("mmbase") != -1) {
                    try {
                        XMLClass f = MMCI.getDefaultMMCI().getClass(newClass);
                        retval.add(f);
                        retval.addAll(getSupperClasses(f));
                    } catch (NotInMMCIException e) {
                        System.err.println(e.getMessage());
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }
        return retval;
    }

    /*
     * main method
     * parameters required are targetdirectory and MMCI.xml file location
     */
    public static void main(String[] argv) throws Exception {
        if (argv.length != 2) {
            System.err.println("Usage: java org.mmbase.bridge.remote.generator.RemoteGenerator <targetdir> <mmci-xml-file>");
            System.exit(1);
        }
        new RemoteGenerator(argv[0], argv[1]);
    }
}
