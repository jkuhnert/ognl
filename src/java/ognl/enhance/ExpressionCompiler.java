package ognl.enhance;

import ognl.ASTAnd;
import ognl.ASTChain;
import ognl.ASTConst;
import ognl.ASTCtor;
import ognl.ASTList;
import ognl.ASTMethod;
import ognl.ASTOr;
import ognl.ASTProperty;
import ognl.ASTRootVarRef;
import ognl.ASTSequence;
import ognl.ASTStaticField;
import ognl.ASTStaticMethod;
import ognl.ASTThisVarRef;
import ognl.ASTVarRef;
import ognl.BooleanExpression;
import ognl.ExpressionNode;
import ognl.Node;
import ognl.NodeType;
import ognl.NumericExpression;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlRuntime;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;


/**
 * Responsible for managing/providing functionality related to compiling generated java source
 * expressions via bytecode enhancements for a given ognl expression. 
 * 
 * @author jkuhnert
 */
public class ExpressionCompiler implements OgnlExpressionCompiler
{   
    public static final String PRE_CAST = "_preCast";
    
    protected Map _loaders = new HashMap();
    protected Map _classPools = new HashMap();
    
    protected ClassPool _pool;
    
    public ExpressionCompiler()
    {
    }
    
    public static void addCastString(OgnlContext context, String cast)
    {
        String value = (String)context.get(PRE_CAST);
        
        if (value != null)
            value = cast + value;
        else
            value = cast;
        
        context.put(PRE_CAST, value);
    }
    
    public String castExpression(OgnlContext context, Node expression, String body)
    {
        if (context.getCurrentAccessor() == null
                || context.getPreviousType() == null 
                || context.getCurrentAccessor().isAssignableFrom(context.getPreviousType())
                || body == null || body.trim().length() < 1
                || (context.getCurrentType() != null && context.getCurrentType().isArray())
                || ASTOr.class.isInstance(expression)
                || ASTAnd.class.isInstance(expression)
                || ASTRootVarRef.class.isInstance(expression)
                || context.getCurrentAccessor() == Class.class
                || Number.class.isAssignableFrom(context.getCurrentAccessor())
                || (context.get(ExpressionCompiler.PRE_CAST) != null && ((String)context.get(ExpressionCompiler.PRE_CAST)).startsWith("new"))
                || ASTStaticField.class.isInstance(expression)
                || (OrderedReturn.class.isInstance(expression) && ((OrderedReturn)expression).getLastExpression() != null))
            return body;
        /*
        System.out.println("castExpression() with expression " + expression + " currentType is: " + context.getCurrentType() 
                + " previousType: " + context.getPreviousType()
                + " current Accessor: " + context.getCurrentAccessor()
                + " previous Accessor: " + context.getPreviousAccessor());
        */
        String castClass = null;
        if (context.getCurrentType() != null && context.getCurrentType().isArray()) {
            
            castClass = context.getCurrentType().getCanonicalName();
        } else if (context.getCurrentAccessor().isArray()) {
            
            castClass = context.getCurrentAccessor().getCanonicalName();
        } else
            castClass = OgnlRuntime.getCompiler().getInterfaceClass(context.getCurrentAccessor()).getName();
        
        ExpressionCompiler.addCastString(context, "((" + castClass + ")");
        
        return ")" + body;
    }
    
    public String getClassName(Class clazz)
    {
        if (clazz.getName().equals("java.util.AbstractList$Itr"))
            return Iterator.class.getName();
        
        if (Modifier.isPublic(clazz.getModifiers()) && clazz.isInterface())
            return clazz.getName();
        
        Class[] intf = clazz.getInterfaces();
        
        for (int i=0; i < intf.length; i++) {
            if (intf[i].getName().indexOf("util.List") > 0)
                return intf[i].getName();
            else if (intf[i].getName().indexOf("Iterator") > 0)
                return intf[i].getName();
        }
        
        if (clazz.getSuperclass() != null && clazz.getSuperclass().getInterfaces().length > 0)
            return getClassName(clazz.getSuperclass());
        
        return clazz.getName();
    }
    
    public Class getInterfaceClass(Class clazz)
    {
        if (clazz.getName().equals("java.util.AbstractList$Itr"))
            return Iterator.class;
        
        if (Modifier.isPublic(clazz.getModifiers())
                && clazz.isInterface() || clazz.isPrimitive()) {
            
            return clazz;
        }
        
        Class[] intf = clazz.getInterfaces();
        
        for (int i=0; i < intf.length; i++) {
            
            if (List.class.isAssignableFrom(intf[i]))
                return List.class;
            else if (Iterator.class.isAssignableFrom(intf[i]))
                return Iterator.class;
            else if (Map.class.isAssignableFrom(intf[i]))
                return Map.class;
            else if (Set.class.isAssignableFrom(intf[i]))
                return Set.class;
            else if (Collection.class.isAssignableFrom(intf[i]))
                return Collection.class;
        }
        
        if (clazz.getSuperclass() != null && clazz.getSuperclass().getInterfaces().length > 0)
            return getInterfaceClass(clazz.getSuperclass());
        
        return clazz;
    }
    
    public static String getRootExpression(Node expression, Object root, boolean boolValue)
    {
        String rootExpr = "";
        
        if ((!ASTList.class.isInstance(expression) && !ASTStaticMethod.class.isInstance(expression) && !ASTStaticField.class.isInstance(expression)
                && !ASTConst.class.isInstance(expression)
                && !ExpressionNode.class.isInstance(expression) 
                && !ASTCtor.class.isInstance(expression)
                && !ASTStaticMethod.class.isInstance(expression)
                && root != null) || (boolValue && root != null)
                || (root != null && ASTRootVarRef.class.isInstance(expression))) {
            
            if (root.getClass().isArray() || ASTRootVarRef.class.isInstance(expression)
                    || ASTThisVarRef.class.isInstance(expression)) {
                rootExpr = "((" + root.getClass().getCanonicalName() + ")$2)";
                
                if (ASTProperty.class.isInstance(expression) && !((ASTProperty)expression).isIndexedAccess())
                    rootExpr += ".";
            } else if ((ASTProperty.class.isInstance(expression)
                    && ((ASTProperty)expression).isIndexedAccess())
                    || ASTChain.class.isInstance(expression)) {
                
                rootExpr = "((" +  OgnlRuntime.getCompiler().getClassName(root.getClass()) + ")$2)";
            } else {
                
                rootExpr = "((" +  OgnlRuntime.getCompiler().getClassName(root.getClass()) + ")$2).";
            }
        }
        
        return rootExpr;
    }
    
    /* (non-Javadoc)
     * @see ognl.enhance.OgnlExpressionCompiler#compileExpression(ognl.OgnlContext, ognl.Node, java.lang.Object)
     */
    public void compileExpression(OgnlContext context, Node expression, Object root)
    throws Exception
    {
        System.out.println("Compiling expr class " + expression.getClass().getName() + " and root " + root);
        
        if (expression.getAccessor() != null)
            return;
        
        String getBody = null;
        String setBody = null;
        
        EnhancedClassLoader loader = getClassLoader(context);
        ClassPool pool = getClassPool(context, loader);
        
        CtClass newClass = pool.makeClass(expression.getClass().getName()+expression.hashCode()+"Accessor");
        newClass.addInterface(getCtClass(ExpressionAccessor.class));
        
        CtClass ognlClass = getCtClass(OgnlContext.class);
        CtClass objClass = getCtClass(Object.class);
        
        CtMethod valueGetter = new CtMethod(objClass, "get", new CtClass[] { ognlClass, objClass }, newClass);
        CtMethod valueSetter = new CtMethod(CtClass.voidType, "set", new CtClass[] { ognlClass, objClass, objClass }, newClass);
        
        CtField nodeMember = null; // will only be set if uncompilable exception is thrown
        
        // must evaluate expression value at least once if object isn't null
        
        if (root != null)
            Ognl.getValue(expression, context, root);
        
        CtClass nodeClass = getCtClass(Node.class);
        CtMethod setExpression = null;
        
        try {
            
            getBody = generateGetter(context, newClass, valueGetter, expression, root);
            
        } catch (UnsupportedCompilationException uc) {
            
            nodeMember = new CtField(nodeClass, "_node", newClass);
            
            newClass.addField(nodeMember);
            
            getBody = generateOgnlGetter(newClass, valueGetter, nodeMember);
            
            if (setExpression == null) {
                
                setExpression = CtNewMethod.setter("setExpression", nodeMember);
                newClass.addMethod(setExpression);
            }
        }
        
        try {
            
            setBody = generateSetter(context, newClass, valueSetter, expression, root);
            
        } catch (UnsupportedCompilationException uc) {
            
            if (nodeMember == null) {
                
                nodeMember = new CtField(nodeClass, "_node", newClass);
                newClass.addField(nodeMember);
            }
            
            setBody = generateOgnlSetter(newClass, valueSetter, nodeMember);
            
            if (setExpression == null) {
                
                setExpression = CtNewMethod.setter("setExpression", nodeMember);
                newClass.addMethod(setExpression);
            }
        }
        
        try {
            newClass.addConstructor(CtNewConstructor.defaultConstructor(newClass));
            
            Class clazz = pool.toClass(newClass);
            
            newClass.detach();
            
            expression.setAccessor((ExpressionAccessor)clazz.newInstance());
            
            // need to set expression on node if the field was just defined.
            
            if (nodeMember != null) {
                
                expression.getAccessor().setExpression(expression);
            }
            
        } catch (Throwable t) {
            throw new RuntimeException("Error compiling expression on object " + root
                    + " with expression node " + expression + " getter body: " + getBody 
                    + " setter body: " + setBody, t);
        }
    }
    
    protected String generateGetter(OgnlContext context, CtClass newClass, CtMethod valueGetter, Node expression, Object root)
    throws Exception
    {
        String pre="";
        String post="";
        String body = null;
        
        context.setRoot(root);
        context.setCurrentObject(root);
        
        // the ExpressionAccessor API has to reference the generic Object class for get/set operations, so this sets up that known
        // type beforehand
        
        context.remove(PRE_CAST);
        
        // Recursively generate the java source code representation of the top level expression
        
        String getterCode = expression.toGetSourceString(context, root);
        
        if (getterCode == null || getterCode.trim().length() <= 0 && !ASTVarRef.class.isAssignableFrom(expression.getClass()))
            getterCode = "null";
        
        if (NodeType.class.isInstance(expression)) {
            NodeType nType = (NodeType)expression;
            Class clazz = nType.getGetterClass();
            
            if (clazz != null && clazz.isPrimitive()) {
                
                if (clazz == Boolean.TYPE) {
                    
                    pre = "Boolean.valueOf((";
                    post = "))";
                } else {
                    
                    pre = "new " + OgnlRuntime.getPrimitiveWrapperClass(clazz).getName() + "(";
                    post = ")";
                }
            } else if (clazz != null 
                    && (Number.class.isAssignableFrom(clazz) 
                            && (NumericExpression.class.isInstance(expression) 
                                    || BooleanExpression.class.isInstance(expression)
                                    || ASTSequence.class.isInstance(expression)))) {
                
                if (BigInteger.class.isAssignableFrom(clazz)) {
                    
                    pre = "java.math.BigInteger.valueOf((long)";
                } else {

                    pre = "new " + clazz.getName() + "(";
                }
                post = ")";
            } else if (clazz != null && Boolean.class.isAssignableFrom(clazz)) {
                
                pre = "new " + clazz.getName() + "(";
                post = ")";
            } else if (clazz != null && Character.class.isAssignableFrom(clazz)) {
                
                pre = "new " + nType.getGetterClass().getName() + "(";
                post = ")";
            }
        }
        
        String castExpression = (String)context.get(PRE_CAST);
        
        String rootExpr = (getterCode != null && !getterCode.equals("null")) ? getRootExpression(expression, root, false) : "";
        
        String noRoot = (String)context.remove("_noRoot");
        if (noRoot != null)
            rootExpr = "";
        
        if (OrderedReturn.class.isInstance(expression) && ((OrderedReturn)expression).getLastExpression() != null) {
            
            body = "{ "
                + (ASTMethod.class.isInstance(expression) || ASTChain.class.isInstance(expression) ? rootExpr : "")
                + (castExpression != null ? castExpression : "")
                + ((OrderedReturn)expression).getCoreExpression()
                + " return " + pre + ((OrderedReturn)expression).getLastExpression()
                + post
                + ";}";
            
        } else {
            
            body = "{ return " + pre 
            + (castExpression != null ? castExpression : "")
            + rootExpr
            + getterCode
            + post
            + ";}";
        }
        
        body = body.replaceAll("\\.\\.", ".");
        
        System.out.println("Getter Body: ===================================\n"+body);
        valueGetter.setBody(body);

        newClass.addMethod(valueGetter);

        return body;
    }
    
    protected String generateSetter(OgnlContext context, CtClass newClass, CtMethod valueSetter, Node expression, Object root)
    throws Exception
    {
        if (ExpressionNode.class.isInstance(expression) 
                || ASTConst.class.isInstance(expression))
            throw new UnsupportedCompilationException("Can't compile expression/constant setters.");
        
        context.setRoot(root);
        context.setCurrentObject(root);
        context.remove(PRE_CAST);
        
        String body = null;
        
        String setterCode = expression.toSetSourceString(context, root);
        String castExpression = (String)context.get(PRE_CAST);
        
        if (setterCode == null || setterCode.trim().length() < 1)
            throw new UnsupportedCompilationException("Can't compile null setter body.");
        
        if (root == null)
            throw new UnsupportedCompilationException("Can't compile setters with a null root object.");
        
        String pre = getRootExpression(expression, root, false);
        
        String noRoot = (String)context.remove("_noRoot");
        if (noRoot != null)
            pre = "";
        
        body = "{" 
            + (castExpression != null ? castExpression : "")
            + pre 
            + setterCode + ";}";
        
        body = body.replaceAll("\\.\\.", ".");
        
        System.out.println("Setter Body: ===================================\n"+body);
        valueSetter.setBody(body);
        
        newClass.addMethod(valueSetter);

        return body;
    }
    
    protected String generateOgnlGetter(CtClass newClass, CtMethod valueGetter, CtField node)
    throws Exception
    {
        String body = "return " + node.getName() + ".getValue($1, $2);";
        
        valueGetter.setBody(body);
        newClass.addMethod(valueGetter);
        
        return body;
    }
    
    protected String generateOgnlSetter(CtClass newClass, CtMethod valueSetter, CtField node)
    throws Exception
    {
        String body = node.getName() + ".setValue($1, $2, $3);";
        
        valueSetter.setBody(body);
        newClass.addMethod(valueSetter);
        
        return body;
    }
    
    protected EnhancedClassLoader getClassLoader(OgnlContext context)
    {
        EnhancedClassLoader ret = (EnhancedClassLoader) _loaders.get(context.getClassResolver());
        
        if (ret != null)
            return ret;
        
        ClassLoader classLoader = new ContextClassLoader(OgnlContext.class.getClassLoader(), context);
        
        ret = new EnhancedClassLoader(classLoader);
        _loaders.put(context.getClassResolver(), ret);
        
        return ret;
    }
    
    protected CtClass getCtClass(Class searchClass)
    throws NotFoundException
    {
        return _pool.get(searchClass.getName());
    }
    
    protected ClassPool getClassPool(OgnlContext context, EnhancedClassLoader loader)
    {
        if (_pool != null)
            return _pool;
        
        _pool = ClassPool.getDefault();
        _pool.insertClassPath(new LoaderClassPath(loader.getParent()));
        
        //ret = new HiveMindClassPool();//ClassPool.getDefault();
        //ret.insertClassPath(new LoaderClassPath(loader.getParent()));
        
        return _pool;
        
        /*
        ClassPool ret = (ClassPool) _classPools.get(context.getClassResolver());
        
        if (ret != null) {
            
            return ret;   
        }
        
        ret = ClassPool.getDefault();
        ret.insertClassPath(new LoaderClassPath(loader.getParent()));
        
        _classPools.put(context.getClassResolver(), ret);
        
        return ret;*/
    }
}
