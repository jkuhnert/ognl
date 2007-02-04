// --------------------------------------------------------------------------
// Copyright (c) 1998-2004, Drew Davidson and Luke Blanshard
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
// Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// Neither the name of the Drew Davidson nor the names of its contributors
// may be used to endorse or promote products derived from this software
// without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
// OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
// AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
// THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
// DAMAGE.
// --------------------------------------------------------------------------
package ognl;

import ognl.enhance.ExpressionCompiler;
import ognl.enhance.UnsupportedCompilationException;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
public class ASTProperty extends SimpleNode implements NodeType
{
    private boolean _indexedAccess = false;
    
    private Class _getterClass;
    private Class _setterClass;
    
    public ASTProperty(int id)
    {
        super(id);
    }

    public void setIndexedAccess(boolean value)
    {
        _indexedAccess = value;
    }

    /**
     * Returns true if this property is itself an index reference.
     */
    public boolean isIndexedAccess()
    {
        return _indexedAccess;
    }
    
    /**
     * Returns true if this property is described by an IndexedPropertyDescriptor and that if
     * followed by an index specifier it will call the index get/set methods rather than go through
     * property accessors.
     */
    public int getIndexedPropertyType(OgnlContext context, Object source)
        throws OgnlException
    {
        if (!isIndexedAccess()) {
            Object property = getProperty(context, source);
            
            if (property instanceof String) { return OgnlRuntime.getIndexedPropertyType(context,
                    (source == null) ? null : OgnlRuntime.getCompiler().getInterfaceClass(source.getClass()), (String) property); }
        }
        
        return OgnlRuntime.INDEXED_PROPERTY_NONE;
    }
    
    public Object getProperty(OgnlContext context, Object source)
        throws OgnlException
    {
        return _children[0].getValue(context, source);
    }
    
    protected Object getValueBody(OgnlContext context, Object source)
        throws OgnlException
    {
        if (_parent != null && ASTProperty.class.isInstance(_parent))
            source = ((OgnlContext)context).getRoot();
        
        Object result = null;
        Object property = null;
        result = property = getProperty(context, source);
        
        result = OgnlRuntime.getProperty(context, source, property);
        
        if (result == null) {
            result = OgnlRuntime.getNullHandler(OgnlRuntime.getTargetClass(source)).nullPropertyValue(context, source,
                    property);
        }
        
        return result;
    }
    
    protected void setValueBody(OgnlContext context, Object target, Object value)
        throws OgnlException
    {
        OgnlRuntime.setProperty(context, target, getProperty(context, target), value);
    }
    
    public boolean isNodeSimpleProperty(OgnlContext context)
        throws OgnlException
    {
        return (_children != null) && (_children.length == 1) && ((SimpleNode) _children[0]).isConstant(context);
    }
    
    public Class getGetterClass()
    {
        return _getterClass;
    }
    
    public Class getSetterClass()
    {
        return _setterClass;
    }
    
    public String toString()
    {
        String result;
        
        if (isIndexedAccess()) {
            result = "[" + _children[0] + "]";
        } else {
            result = ((ASTConst) _children[0]).getValue().toString();
        }
        return result;
    }
    
    public String toGetSourceString(OgnlContext context, Object target)
    {
        if (context.getCurrentObject() == null)
            throw new UnsupportedCompilationException("Current target is null.");
        
        String result = "";
        Method m = null;
        
        try {
           //System.out.println("astproperty is indexed? : " + isIndexedAccess() + " child: " + _children[0].getClass().getName()
            //        + " target: " + target.getClass().getName() + " current object: " + context.getCurrentObject().getClass().getName());
            
            if (isIndexedAccess()) {
                
                Object value = _children[0].getValue(context, context.getRoot());
                
                if (value == null || DynamicSubscript.class.isAssignableFrom(value.getClass()))
                    throw new UnsupportedCompilationException("Value passed as indexed property was null or not supported.");
                
                // Get root cast string if the child is a type that needs it (like a nested ASTProperty)
                
                String srcString = _children[0].toGetSourceString(context, context.getCurrentObject());
                
                srcString = ExpressionCompiler.getRootExpression(_children[0], context.getRoot(), false) + srcString;
                
                //System.out.println("indexed getting with child srcString: " + srcString + " value class: " + value.getClass());
                
                if (context.get("_indexedMethod") != null) {
                    
                    m = (Method)context.remove("_indexedMethod");
                    _getterClass = m.getReturnType();
                    
                    context.setCurrentType(_getterClass);
                    
                    return "." + m.getName() + "(" + srcString + ")";
                } else {
                    
                    PropertyAccessor p = OgnlRuntime.getPropertyAccessor(target.getClass());
                    
                    //System.out.println("child value : " + _children[0].getValue(context, context.getCurrentObject()) + " using propaccessor " + p.getClass().getName()
                     //       + " and srcString " + srcString);
                    
                    Object indexVal = p.getProperty(context, target, _children[0].getValue(context, context.getCurrentObject()));
                    /*
                    System.out.println("astprop srcString: " + srcString 
                            + " from child class " + _children[0].getClass().getName()
                            + " and indexVal " + indexVal
                            + " propertyAccessor : " + p.getClass().getName() + " context obj " + context.getCurrentObject()
                            + " context obj is array? : " + context.getCurrentObject().getClass().isArray());
                    */
                    result = p.getSourceAccessor(context, target, srcString);
                    _getterClass = p.getPropertyClass(context, target, srcString);
                    
                    if (_getterClass == null && context.getCurrentObject().getClass().isArray() 
                            && ArrayPropertyAccessor.class.isAssignableFrom(p.getClass())) {
                        
                        _getterClass = context.getCurrentObject().getClass().getComponentType();
                    } else if (_getterClass == null && indexVal != null) {
                        
                        _getterClass = indexVal.getClass().isArray() ? indexVal.getClass().getComponentType() : indexVal.getClass();
                    }
                    
                    //System.out.println("result of index src is " + result + " and getterClass " + _getterClass.getName());
                    
                    context.setCurrentType(_getterClass);
                    context.setCurrentObject(indexVal);
                    
                    return result;
                }

            }
            
            Object tmp = context.getCurrentObject();
            
            String name = ((ASTConst) _children[0]).getValue().toString();
            
            if (!Iterator.class.isAssignableFrom(context.getCurrentObject().getClass()) 
                    || (Iterator.class.isAssignableFrom(context.getCurrentObject().getClass()) && name.indexOf("next") < 0)) {
                
                try {
                    target = getValue(context, context.getCurrentObject());
                } catch (NoSuchPropertyException e) {
                    try { 
                        
                        target = getValue(context, context.getRoot());
                        context.setCurrentObject(tmp);
                        
                    } catch (NoSuchPropertyException ex) { }
                }
            }
            
            context.setCurrentObject(tmp);
            
            PropertyDescriptor pd = OgnlRuntime.getPropertyDescriptor(context.getCurrentObject().getClass(), name);
            
            if (this.getIndexedPropertyType(context, context.getCurrentObject()) > 0 && pd != null){
                
                // if an indexed method accessor need to use special property descriptors to find methods
                
                if (pd instanceof IndexedPropertyDescriptor) {
                    m = ((IndexedPropertyDescriptor) pd).getIndexedReadMethod();
                } else {
                    if (pd instanceof ObjectIndexedPropertyDescriptor) {
                        m = ((ObjectIndexedPropertyDescriptor) pd).getIndexedReadMethod();
                    } else {
                        throw new OgnlException("property '" + name + "' is not an indexed property");
                    }
                }
                
               //System.out.println("================== Indexed property type found for " + name);
                if (_parent == null) {
                    // the above pd will be the wrong result sometimes, such as methods like getValue(int) vs String[] getValue() 
                    
                    m = OgnlRuntime.getReadMethod(context.getCurrentObject().getClass(), name);
                    result = m.getName() + "()";
                    _getterClass = m.getReturnType();
                } else {
                    context.put("_indexedMethod", m);
                }
            } else {
                
                context.setCurrentObject(tmp);
                
                // set context object back again as getValue results in context change
                
                //System.out.println("astproperty trying to get " + name + " on object target: " + context.getCurrentObject().getClass().getName());
                
                PropertyAccessor pa = OgnlRuntime.getPropertyAccessor(context.getCurrentObject().getClass());
                
                if (context.getCurrentObject().getClass().isArray()) {
                    
                    if (pd == null) {
                        pd = OgnlRuntime.getProperty(context.getCurrentObject().getClass(), name);
                        
                        if (pd != null && pd.getReadMethod() != null) {
                            m = pd.getReadMethod();
                            result = pd.getName();
                        } else {
                            _getterClass = int.class;
                            context.setCurrentAccessor(context.getCurrentObject().getClass());
                            context.setCurrentType(int.class);
                            result = "." + name;
                        }
                    }
                } else {
                    
                    if (pa != null) {
                        
                        result = pa.getSourceAccessor(context, context.getCurrentObject(), 
                                _children[0].toGetSourceString(context, context.getRoot()));
                        
                        if (_parent != null && ASTSequence.class.isInstance(_parent) 
                                && Map.class.isAssignableFrom(context.getCurrentObject().getClass()))
                            _getterClass = Object.class;
                        else
                            _getterClass = pa.getPropertyClass(context, context.getCurrentObject(), name);
                        
                    } else if (pd != null) {
                        
                        m = pd.getReadMethod();
                        result = m.getName() + "()";
                    } else {
                        
                        m = OgnlRuntime.getReadMethod(context.getCurrentObject().getClass(), name);
                        if (m == null) {
                            
                            m = OgnlRuntime.getReadMethod(context.getCurrentObject().getClass(), name);
                            result = name + "()";
                        } else {
                            result = m.getName() + "()";
                            _getterClass = m.getReturnType();
                        }
                    }
                }
                
            }

        } catch (Throwable t) {
            if (UnsupportedCompilationException.class.isInstance(t))
                throw (UnsupportedCompilationException)t;
            else
                throw new RuntimeException(t);
        }
        
        // set known property types for NodeType interface when possible
        
        if (m != null) {
            _getterClass = m.getReturnType();
            
            context.setCurrentType(m.getReturnType());
            context.setCurrentAccessor(OgnlRuntime.getSuperOrInterfaceClass(m, m.getDeclaringClass()));
        }
        
        context.setCurrentObject(target);
        
        return result;
    }
    
    boolean lastChild(OgnlContext context)
    {
        return _parent == null || context.get("_lastChild") !=  null;
    }
    
    public String toSetSourceString(OgnlContext context, Object target)
    {
        String result = "";
        Method m = null;
        
        if (context.getCurrentObject() == null)
            throw new UnsupportedCompilationException("Current target is null.");
        
        //System.out.println("astproperty(setter) is indexed? : " + isIndexedAccess() + " child: " + _children[0].getClass().getName()
         //       + " target: " + target.getClass().getName());
        
        try {
            
            if (isIndexedAccess()) {
                
                //System.out.println("ast set source property is indexed");
                
                Object value = _children[0].getValue(context, context.getRoot());
                
                if (value == null)
                    throw new UnsupportedCompilationException("Value passed as indexed property is null, can't enhance statement to bytecode.");
                
                String srcString = _children[0].toGetSourceString(context, context.getCurrentObject());
                
                srcString = ExpressionCompiler.getRootExpression(_children[0], context.getRoot(), false) + srcString;
                
               // System.out.println("astproperty setter using indexed value " + value + " and srcString: " + srcString);
                
                if (context.get("_indexedMethod") != null) {
                    
                    m = (Method)context.remove("_indexedMethod");
                    
                    _setterClass = m.getParameterTypes()[0];
                    
                    context.setCurrentType(_setterClass);
                    context.setCurrentAccessor(OgnlRuntime.getSuperOrInterfaceClass(m, m.getDeclaringClass()));
                    
                    return m.getName() + "(" + srcString + ")";
                } else {
                    PropertyAccessor p = OgnlRuntime.getPropertyAccessor(target.getClass());
                    
                    result = p.getSourceSetter(context, target, srcString);
                    
                    context.setCurrentObject(value);
                    context.setCurrentType(_getterClass);
                    return result;
                }
            }
            
            String name = ((ASTConst) _children[0]).getValue().toString();
            
            Object tmp = context.getCurrentObject();
            
            //System.out.println(" astprop(setter) : trying to get " + name + " on object target " + context.getCurrentObject().getClass().getName());
            
            if (!Iterator.class.isAssignableFrom(context.getCurrentObject().getClass()) 
                    || (Iterator.class.isAssignableFrom(context.getCurrentObject().getClass()) &&  name.indexOf("next") < 0)) {
                
                try {
                    target = getValue(context, context.getCurrentObject());
                } catch (NoSuchPropertyException e) {
                    try {
                        
                        target = getValue(context, context.getRoot());
                        context.setCurrentObject(tmp);
                        
                    } catch (NoSuchPropertyException ex) { }
                }
            }
            
            context.setCurrentObject(tmp);
            
            PropertyDescriptor pd = OgnlRuntime.getPropertyDescriptor(OgnlRuntime.getCompiler().getInterfaceClass(context.getCurrentObject().getClass()), name);
            
            if (pd != null && this.getIndexedPropertyType(context, context.getCurrentObject()) > 0){
                
                // if an indexed method accessor need to use special property descriptors to find methods
                
                if (pd instanceof IndexedPropertyDescriptor) {
                    IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor)pd;
                    
                    m = lastChild(context) ? ipd.getIndexedWriteMethod() : ipd.getIndexedReadMethod();
                } else {
                    if (pd instanceof ObjectIndexedPropertyDescriptor) {
                        ObjectIndexedPropertyDescriptor opd = (ObjectIndexedPropertyDescriptor)pd;
                        
                        m = lastChild(context) ? opd.getIndexedWriteMethod() : opd.getIndexedReadMethod();
                    } else {
                        throw new OgnlException("property '" + name + "' is not an indexed property");
                    }
                }
                
                if (_parent == null) {
                    // the above pd will be the wrong result sometimes, such as methods like getValue(int) vs String[] getValue() 
                    
                    m = OgnlRuntime.getWriteMethod(context.getCurrentObject().getClass(), name);
                    Class parm = m.getParameterTypes()[0];
                    String cast = parm.isArray() ? parm.getCanonicalName() : parm.getName();
                    
                    result = m.getName() + "((" + cast + ")$3)";
                    _setterClass = parm;
                } else {
                    context.put("_indexedMethod", m);
                }
                
            } else {
                
                context.setCurrentObject(tmp);
                
                //System.out.println("astproperty trying to set " + name + " on object target: " + context.getCurrentObject().getClass().getName());
                
                PropertyAccessor pa = OgnlRuntime.getPropertyAccessor(context.getCurrentObject().getClass());
                
                if (target != null)
                    _setterClass = target.getClass();
                
                if (_parent != null && pd != null && pa == null) {
                    
                    m = pd.getReadMethod();
                    result = m.getName() + "()";
                } else {
                    
                    if (context.getCurrentObject().getClass().isArray()) {
                        result = "";
                    } else if (pa != null) {
                        
                        if (!lastChild(context)) {
                            
                            result = pa.getSourceAccessor(context, context.getCurrentObject(), 
                                    _children[0].toGetSourceString(context, context.getRoot()));
                            
                        } else {
                            
                            result = pa.getSourceSetter(context, context.getCurrentObject(), 
                                    _children[0].toGetSourceString(context, context.getRoot()));
                        }
                        _getterClass = pa.getPropertyClass(context, context.getCurrentObject(), name);
                    } else if (pd != null) {
                        
                        m = pd.getWriteMethod();
                        if (m != null) {
                            
                            result = m.getName() + "(";
                            Class ptype = m.getParameterTypes()[0];
                            
                            if (ptype.isArray()) {
                                
                                result += "(" + ptype.getCanonicalName() 
                                + ")ognl.OgnlOps.convertValue($3," 
                                + target.getClass().getCanonicalName() + ".class))";
                            } else {
                                
                                if (ptype.isPrimitive()) {
                                    
                                    Class wrapClass = OgnlRuntime.getPrimitiveWrapperClass(ptype);
                                    
                                    result += "((" + wrapClass.getName() 
                                    + ")ognl.OgnlOps.convertValue($3," 
                                    + wrapClass.getName() + ".class, true))."
                                    + OgnlRuntime.getNumericValueGetter(wrapClass)
                                    + ")";
                                } else
                                    result += "ognl.OgnlOps.convertValue($3," + ptype.getName() + ".class))";
                                
                            }
                        }
                        
                    }
                }
            }
            
        } catch (Throwable t) {
            if (UnsupportedCompilationException.class.isInstance(t))
                throw (UnsupportedCompilationException)t;
            else
                throw new RuntimeException(t);
        }
        
        context.setCurrentObject(target);
        
        if (m != null) {
            
            context.setCurrentType(m.getReturnType());
            context.setCurrentAccessor(OgnlRuntime.getSuperOrInterfaceClass(m, m.getDeclaringClass()));
        }
        
        return result;
    }
}
