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

import java.lang.reflect.Method;

/**
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
public class ASTStaticMethod extends SimpleNode implements NodeType
{

    private String _className;
    private String _methodName;

    private Class _getterClass;
    
    public ASTStaticMethod(int id)
    {
        super(id);
    }

    public ASTStaticMethod(OgnlParser p, int id)
    {
        super(p, id);
    }

    /** Called from parser action. */
    void init(String className, String methodName)
    {
        _className = className;
        _methodName = methodName;
    }
    
    protected Object getValueBody(OgnlContext context, Object source)
        throws OgnlException
    {
        Object[] args = OgnlRuntime.getObjectArrayPool().create(jjtGetNumChildren());
        Object root = context.getRoot();

        try {
            for(int i = 0, icount = args.length; i < icount; ++i)
                args[i] = _children[i].getValue(context, root);

            return OgnlRuntime.callStaticMethod(context, _className, _methodName, args);
        } finally {
            OgnlRuntime.getObjectArrayPool().recycle(args);
        }
    }
    
    public Class getGetterClass()
    {
        return _getterClass;
    }
    
    public Class getSetterClass()
    {
        return _getterClass;
    }
    
    public String toString()
    {
        String result = "@" + _className + "@" + _methodName;

        result = result + "(";
        if ((_children != null) && (_children.length > 0)) {
            for(int i = 0; i < _children.length; i++) {
                if (i > 0) {
                    result = result + ", ";
                }
                result = result + _children[i];
            }
        }
        result = result + ")";
        return result;
    }
    
    public String toGetSourceString(OgnlContext context, Object target)
    {
        String result = _className + "." + _methodName + "(";
        
        if ((_children != null) && (_children.length > 0)) {
            
            for(int i = 0; i < _children.length; i++) {
                
                if (i > 0) {
                    result = result + ", ";
                }
                result = result + _children[i].toGetSourceString(context, target);
                
            }
        }
        result += ")";
        
        try {
            Class clazz = OgnlRuntime.classForName(context, _className);
            
            Method m = OgnlRuntime.getMethod(context, clazz, _methodName, _children, true);
            
            if (m != null) {
                _getterClass = m.getReturnType();
                
                context.setCurrentType(m.getReturnType());
                context.setCurrentAccessor(OgnlRuntime.getSuperOrInterfaceClass(m, m.getDeclaringClass()));
            }
            
        } catch (Exception e) { throw new RuntimeException(e); }
        
        return result;
    }
    
    public String toSetSourceString(OgnlContext context, Object target)
    {
        return toGetSourceString(context, target);
    }
}
