/**
 * 
 */
package org.ognl.test.objects;

import java.util.Map;

import ognl.ObjectPropertyAccessor;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;
import ognl.enhance.ExpressionCompiler;
import ognl.enhance.UnsupportedCompilationException;


/**
 * Implementation of provider that works with {@link BeanProvider} instances.
 */
public class BeanProviderAccessor extends ObjectPropertyAccessor implements PropertyAccessor
{
    public Object getProperty(Map context, Object target, Object name) 
    throws OgnlException
    {
        BeanProvider provider = (BeanProvider)target;
        String beanName = (String)name;
        
        return provider.getBean(beanName);
    }

    /**
     *  Returns true if the name matches a bean provided by the provider.
     *  Otherwise invokes the super implementation.
     * 
     **/
    
    public boolean hasGetProperty(Map context, Object target, Object oname) 
    throws OgnlException
    {
        BeanProvider provider = (BeanProvider)target;
        String beanName = ((String)oname).replaceAll("\"", "");
        
        return provider.getBean(beanName) != null;
    }
    
    public Class getPropertyClass(OgnlContext context, Object target, Object name)
    {
        BeanProvider provider = (BeanProvider)target;
        String beanName = ((String)name).replaceAll("\"", "");
        
        if (provider.getBean(beanName) != null)
            return provider.getBean(beanName).getClass();
        else
            return super.getPropertyClass(context, target, name);
    }
    
    public String getSourceAccessor(OgnlContext context, Object target, Object name)
    {
        BeanProvider provider = (BeanProvider)target;
        String beanName = ((String)name).replaceAll("\"", "");
        
        if (provider.getBean(beanName) != null) {
            ExpressionCompiler.addCastString(context, "((" 
                    + OgnlRuntime.getCompiler().getInterfaceClass(provider.getBean(beanName).getClass()).getName() + ")");
            
            return ".getBean(" + name + "))";
        }
        
        return super.getSourceAccessor(context, target, name);
    }
    
    public String getSourceSetter(OgnlContext context, Object target, Object name)
    {
        throw new UnsupportedCompilationException("Can't set beans on BeanProvider.");
    }
}
