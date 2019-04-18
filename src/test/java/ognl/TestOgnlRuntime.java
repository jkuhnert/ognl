package ognl;

import junit.framework.TestCase;
import ognl.security.MethodBodyExecutionSandbox;
import org.ognl.test.objects.*;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests various methods / functionality of {@link ognl.OgnlRuntime}.
 */
public class TestOgnlRuntime extends TestCase {

    
    public void test_Get_Super_Or_Interface_Class() throws Exception
    {
        ListSource list = new ListSourceImpl();

        Method m = OgnlRuntime.getReadMethod(list.getClass(), "total");
        assertNotNull(m);

        assertEquals(ListSource.class, OgnlRuntime.getCompiler().getSuperOrInterfaceClass(m, list.getClass()));
    }

    public void test_Get_Private_Class() throws Exception
    {
        List list = Arrays.asList(new String[]{"hello", "world"});

        Method m = OgnlRuntime.getReadMethod(list.getClass(), "iterator");
        assertNotNull(m);

        assertEquals(Iterable.class, OgnlRuntime.getCompiler().getSuperOrInterfaceClass(m, list.getClass()));
    }

    public void test_Complicated_Inheritance() throws Exception
    {
        IForm form = new FormImpl();

        Method m = OgnlRuntime.getWriteMethod(form.getClass(), "clientId");
        assertNotNull(m);

        assertEquals(IComponent.class, OgnlRuntime.getCompiler().getSuperOrInterfaceClass(m, form.getClass()));
    }

    public void test_Get_Read_Method()
            throws Exception
    {
        Method m = OgnlRuntime.getReadMethod(Bean2.class, "pageBreakAfter");
        assertNotNull(m);

        assertEquals("isPageBreakAfter", m.getName());
    }

    public void test_Get_Read_Field()
            throws Exception
    {
        Method m = OgnlRuntime.getReadMethod(Bean2.class, "code");
        assertNull(m);

        Field field = OgnlRuntime.getField(Bean2.class, "code");
        assertNotNull(field);
        assertEquals("code", field.getName());
    }

    class TestGetters {
        public boolean isEditorDisabled()
        {
            return false;
        }

        public boolean isDisabled()
        {
            return true;
        }

        public boolean isNotAvailable() {
            return false;
        }

        public boolean isAvailable() {
            return true;
        }
    }

    public void test_Get_Read_Method_Multiple()
            throws Exception
    {
        Method m = OgnlRuntime.getReadMethod(TestGetters.class, "disabled");
        assertNotNull(m);

        assertEquals("isDisabled", m.getName());
    }

    public void test_Get_Read_Method_Multiple_Boolean_Getters()
            throws Exception
    {
        Method m = OgnlRuntime.getReadMethod(TestGetters.class, "available");
        assertNotNull(m);

        assertEquals("isAvailable", m.getName());

        m = OgnlRuntime.getReadMethod(TestGetters.class, "notAvailable");
        assertNotNull(m);

        assertEquals("isNotAvailable", m.getName());
    }

    public void test_Find_Method_Mixed_Boolean_Getters()
    throws Exception
    {
        Method m = OgnlRuntime.getReadMethod(GetterMethods.class, "allowDisplay");
        assertNotNull(m);

        assertEquals("getAllowDisplay", m.getName());
    }

    public void test_Get_Appropriate_Method()
            throws Exception
    {
        ListSource list = new ListSourceImpl();
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);

        Object ret = OgnlRuntime.callMethod(context, list, "addValue", new String[] {null});
        
        assert ret != null;
    }

    public void test_Call_Static_Method_Invalid_Class()
    {

        try {

            OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
            OgnlRuntime.callStaticMethod(context, "made.up.Name", "foo", null);

            fail("ClassNotFoundException should have been thrown by previous reference to <made.up.Name> class.");
        } catch (Exception et) {

            assertTrue(MethodFailedException.class.isInstance(et));
            assertTrue(et.getMessage().indexOf("made.up.Name") > -1);
        }
    }

    public void test_Setter_Returns()
            throws Exception
    {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        SetterReturns root = new SetterReturns();

        Method m = OgnlRuntime.getWriteMethod(root.getClass(), "value");
        assertTrue(m != null);

        Ognl.setValue("value", context, root, "12__");
        assertEquals(Ognl.getValue("value", context, root), "12__");
    }

    public void test_Call_Method_VarArgs()
            throws Exception
    {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        GenericService service = new GenericServiceImpl();

        GameGenericObject argument = new GameGenericObject();

        Object[] args = OgnlRuntime.getObjectArrayPool().create(2);
        args[0] = argument;

        assertEquals("Halo 3", OgnlRuntime.callMethod(context, service, "getFullMessageFor", args));
    }

    public void test_Call_Method_In_JDK_Sandbox()
            throws Exception {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        GenericService service = new GenericServiceImpl();

        Object[] args = OgnlRuntime.getObjectArrayPool().create(1);
        args[0] = 0;

        MethodBodyExecutionSandbox.enable();

        try {
            OgnlRuntime.callMethod(context, service, "exec", args);
            fail("JDK sandbox should block execution");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof InvocationTargetException);
            assertTrue(((InvocationTargetException)ex.getCause()).getTargetException().getMessage().contains("execute"));
        } finally {
            MethodBodyExecutionSandbox.disable();
        }
    }

    public void test_Call_Method_In_JDK_Sandbox_Thread_Safety()
            throws Exception {
        final OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        final GenericService service = new GenericServiceImpl();

        MethodBodyExecutionSandbox.enable();

        try {
            final int NUM_THREADS = 100;
            final int MAX_WAIT_MS = 300;
            ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
            final CountDownLatch allThreadsWaitOnThis = new CountDownLatch(1);
            final AtomicInteger numThreadsFailedTest = new AtomicInteger(0);
            for (int i = 0; i < NUM_THREADS; ++i) {
                exec.submit(new Runnable() {
                    public void run() {
                        try {
                            allThreadsWaitOnThis.await();
                        } catch (InterruptedException ignored) {
                        }

                        try {
                            Thread.sleep((long) (Math.random() * MAX_WAIT_MS));
                        } catch (InterruptedException ignored) {
                        }

                        Object[] args = OgnlRuntime.getObjectArrayPool().create(1);
                        args[0] = Math.random() * MAX_WAIT_MS;

                        try {
                            OgnlRuntime.callMethod(context, service, "exec", args);
                            numThreadsFailedTest.incrementAndGet();
                        } catch (Exception ex) {
                            if (!((ex.getCause() instanceof InvocationTargetException &&
                                    ((InvocationTargetException)ex.getCause()).getTargetException().getMessage().contains("execute"))
                                    ||
                                    (ex.getCause() instanceof SecurityException &&
                                            ex.getCause().getMessage().contains("createClassLoader")))) {
                                numThreadsFailedTest.incrementAndGet();
                            }
                        }
                    }
                });
            }

            // release all the threads
            allThreadsWaitOnThis.countDown();

            // wait for them all to finish
            Thread.sleep(MAX_WAIT_MS * 2);
            exec.shutdown();
            exec.awaitTermination(MAX_WAIT_MS * 2, TimeUnit.MILLISECONDS);
            assertTrue(exec.isTerminated());
            assertEquals(0, numThreadsFailedTest.get());
        } finally {
            MethodBodyExecutionSandbox.disable();
        }
    }

    public void test_Disable_JDK_Sandbox()
            throws Exception {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        GenericService service = new GenericServiceImpl();

        Object[] args = OgnlRuntime.getObjectArrayPool().create(0);

        MethodBodyExecutionSandbox.enable();

        try {
            OgnlRuntime.callMethod(context, service, "disableSandboxViaReflectionByField", args);
            fail("JDK sandbox should block execution");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof SecurityException);
            assertTrue(ex.getCause().getMessage().contains("accessDeclaredMembers") ||
                    ex.getCause().getMessage().contains("suppressAccessChecks"));
        } finally {
            MethodBodyExecutionSandbox.disable();
        }

        MethodBodyExecutionSandbox.enable();

        try {
            OgnlRuntime.callMethod(context, service, "disableSandboxViaReflectionByMethod", args);
            fail("JDK sandbox should block execution");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof InvocationTargetException &&
                            ((InvocationTargetException)ex.getCause()).getTargetException().getMessage().contains("setSecurityManager"));
        } finally {
            MethodBodyExecutionSandbox.disable();
        }
    }

    public void test_Exit_JDK_Sandbox()
            throws Exception {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        GenericService service = new GenericServiceImpl();

        Object[] args = OgnlRuntime.getObjectArrayPool().create(0);

        MethodBodyExecutionSandbox.enable();

        try {
            OgnlRuntime.callMethod(context, service, "exit", args);
            fail("JDK sandbox should block execution");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof InvocationTargetException);
            assertTrue(((InvocationTargetException)ex.getCause()).getTargetException().getMessage().contains("exit"));
        } finally {
            MethodBodyExecutionSandbox.disable();
        }
    }

    public void test_Class_Cache_Inspector()
            throws Exception
    {
        OgnlRuntime.clearCache();
        assertEquals(0, OgnlRuntime._propertyDescriptorCache.getSize());

        Root root = new Root();
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        Node expr = Ognl.compileExpression(context, root, "property.bean3.value != null");

        assertTrue((Boolean)expr.getAccessor().get(context, root));

        int size = OgnlRuntime._propertyDescriptorCache.getSize();
        assertTrue(size > 0);

        OgnlRuntime.clearCache();
        assertEquals(0, OgnlRuntime._propertyDescriptorCache.getSize());

        // now register class cache prevention

        OgnlRuntime.setClassCacheInspector(new TestCacheInspector());

        expr = Ognl.compileExpression(context, root, "property.bean3.value != null");
        assertTrue((Boolean)expr.getAccessor().get(context, root));

        assertEquals((size - 1), OgnlRuntime._propertyDescriptorCache.getSize());
    }

    class TestCacheInspector implements ClassCacheInspector {

        public boolean shouldCache(Class type)
        {
            if (type == null || type == Root.class)
                return false;

            return true;
        }
    }

    public void test_Set_Generic_Parameter_Types()
        throws Exception
    {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);

        Method m = OgnlRuntime.getSetMethod(context, GenericCracker.class, "param");
        assertNotNull(m);

        Class[] types = m.getParameterTypes();
        assertEquals(1, types.length);
        assertEquals(Integer.class, types[0]);
    }

    public void test_Get_Generic_Parameter_Types()
        throws Exception
    {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);

        Method m = OgnlRuntime.getGetMethod(context, GenericCracker.class, "param");
        assertNotNull(m);

        assertEquals(Integer.class, m.getReturnType());
    }

    public void test_Find_Parameter_Types()
            throws Exception
    {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);

        Method m = OgnlRuntime.getSetMethod(context, GameGeneric.class, "ids");
        assertNotNull(m);

        Class[] types = OgnlRuntime.findParameterTypes(GameGeneric.class, m);
        assertEquals(1, types.length);
        assertEquals(new Long[0].getClass(), types[0]);
    }

    public void test_Find_Parameter_Types_Superclass()
            throws Exception
    {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);

        Method m = OgnlRuntime.getSetMethod(context, BaseGeneric.class, "ids");
        assertNotNull(m);

        Class[] types = OgnlRuntime.findParameterTypes(BaseGeneric.class, m);
        assertEquals(1, types.length);
        assertEquals(new Serializable[0].getClass(), types[0]);
    }

    public void test_Get_Declared_Methods_With_Synthetic_Methods()
        throws Exception
    {
        List result = OgnlRuntime.getDeclaredMethods(SubclassSyntheticObject.class, "list", false);

        // synthetic method would be "public volatile java.util.List org.ognl.test.objects.SubclassSyntheticObject.getList()",
        // causing method return size to be 3
        
        assertEquals(2, result.size());
    }

    public void test_Get_Property_Descriptors_With_Synthetic_Methods()
        throws Exception
    {
        PropertyDescriptor pd = OgnlRuntime.getPropertyDescriptor(SubclassSyntheticObject.class, "list");

        assert pd != null;
        assert OgnlRuntime.isMethodCallable(pd.getReadMethod());
    }

    private static class GenericParent<T>
     {
         public void save(T entity)
         {

         }
     }

     private static class StringChild extends GenericParent<String>
     {

     }

     private static class LongChild extends GenericParent<Long>
     {

     }

     /**
      * Tests OGNL parameter discovery.
      */
     public void testOGNLParameterDiscovery() throws NoSuchMethodException
     {
         Method saveMethod = GenericParent.class.getMethod("save", Object.class);
         System.out.println(saveMethod);

         Class[] longClass = OgnlRuntime.findParameterTypes(LongChild.class, saveMethod);
         assertNotSame(longClass[0], String.class);
         assertSame(longClass[0], Long.class);

         Class[] stringClass = OgnlRuntime.findParameterTypes(StringChild.class, saveMethod);
         assertNotSame("The cached parameter types from previous calls are used", stringClass[0], Long.class);
         assertSame(stringClass[0], String.class);
     }

    public void testBangOperator() throws Exception {
        Object value = Ognl.getValue("!'false'", new Object());
        assertEquals(Boolean.TRUE, value);
    }

    public void testGetStaticField() throws Exception {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        Object obj = OgnlRuntime.getStaticField(context, "org.ognl.test.objects.Root", "SIZE_STRING");
        assertEquals(Root.SIZE_STRING, obj);
    }

    public void testGetStaticFieldEnum() throws Exception {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        Object obj = OgnlRuntime.getStaticField(context, "org.ognl.test.objects.OtherEnum", "ONE");
        assertEquals(OtherEnum.ONE, obj);
    }

    public void testGetStaticFieldEnumStatic() throws Exception {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        Object obj = OgnlRuntime.getStaticField(context, "org.ognl.test.objects.OtherEnum", "STATIC_STRING");
        assertEquals(OtherEnum.STATIC_STRING, obj);
    }
}
