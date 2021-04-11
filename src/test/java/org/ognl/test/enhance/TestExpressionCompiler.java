/**
 *
 */
package org.ognl.test.enhance;

import junit.framework.TestCase;
import ognl.Node;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.enhance.ExpressionCompiler;
import ognl.enhance.OgnlExpressionCompiler;
import org.ognl.test.objects.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import ognl.ExpressionSyntaxException;
import ognl.MethodFailedException;
import ognl.OgnlException;
import ognl.ParseException;


/**
 * Tests functionality of {@link ExpressionCompiler}.
 */
public class TestExpressionCompiler extends TestCase
{
    OgnlExpressionCompiler _compiler;
    OgnlContext _context = (OgnlContext) Ognl.createDefaultContext(null);

    public void setUp()
    {
        _compiler = new ExpressionCompiler();
    }

    public void test_Get_Property_Access()
            throws Throwable
    {
        Node expr = (Node)Ognl.parseExpression("bean2");
        Bean1 root = new Bean1();

        _compiler.compileExpression(_context, expr, root);

        assertNotNull(expr.getAccessor().get(_context, root));
    }

    public void test_Get_Indexed_Property()
            throws Throwable
    {
        Node expr = (Node)Ognl.parseExpression("bean2.bean3.indexedValue[25]");
        Bean1 root = new Bean1();

        assertNull(Ognl.getValue(expr, _context, root));

        _compiler.compileExpression(_context, expr, root);

        assertNull(expr.getAccessor().get(_context, root));
    }

    public void test_Set_Indexed_Property()
            throws Throwable
    {
        Node expr = (Node)Ognl.parseExpression("bean2.bean3.indexedValue[25]");
        Bean1 root = new Bean1();

        assertNull(Ognl.getValue(expr, _context, root));

        _compiler.compileExpression(_context, expr, root);

        expr.getAccessor().set(_context, root, "test string");

        assertEquals("test string", expr.getAccessor().get(_context, root));
    }

    public void test_Expression()
            throws Throwable
    {
        Node expr = (Node)Ognl.parseExpression("bean2.bean3.value <= 24");
        Bean1 root = new Bean1();

        assertEquals(Boolean.FALSE, Ognl.getValue(expr, _context, root));

        _compiler.compileExpression(_context, expr, root);

        assertEquals(Boolean.FALSE, expr.getAccessor().get(_context, root));
    }

    public void test_Get_Context_Property()
            throws Throwable
    {
        _context.put("key", "foo");
        Node expr = (Node)Ognl.parseExpression("bean2.bean3.map[#key]");
        Bean1 root = new Bean1();

        assertEquals("bar", Ognl.getValue(expr, _context, root));

        _compiler.compileExpression(_context, expr, root);

        assertEquals("bar", expr.getAccessor().get(_context, root));

        _context.put("key", "bar");

        assertEquals("baz", Ognl.getValue(expr, _context, root));
        assertEquals("baz", expr.getAccessor().get(_context, root));
    }
    
    public void test_Set_Context_Property()
            throws Throwable
    {
        _context.put("key", "foo");
        Node expr = (Node)Ognl.parseExpression("bean2.bean3.map[#key]");
        Bean1 root = new Bean1();

        _compiler.compileExpression(_context, expr, root);

        assertEquals("bar", expr.getAccessor().get(_context, root));

        _context.put("key", "bar");
        assertEquals("baz", expr.getAccessor().get(_context, root));

        expr.getAccessor().set(_context, root, "bam");
        assertEquals("bam", expr.getAccessor().get(_context, root));
    }

    public void test_Property_Index()
            throws Throwable
    {
        Root root = new Root();
        Node expr = (Node) Ognl.compileExpression(_context, root, "{index + 1}");

        Object ret = expr.getAccessor().get(_context, root);

        assertTrue(Collection.class.isInstance(ret));
    }

    public void test_Root_Expression_Inheritance()
            throws Throwable
    {
        Inherited obj1 = new TestInherited1();
        Inherited obj2 = new TestInherited2();

        Node expr = (Node) Ognl.compileExpression(_context, obj1, "myString");

        assertEquals(expr.getAccessor().get(_context, obj1), "inherited1");
        assertEquals(expr.getAccessor().get(_context, obj2), "inherited2");
    }

    public void test_Create_Empty_Collection()
            throws Throwable
    {
        Node expr = (Node) Ognl.compileExpression(_context, null, "{}");

        Object ret = expr.getAccessor().get(_context, null);

        assertNotNull(ret);
        assertTrue(Collection.class.isAssignableFrom(ret.getClass()));
    }

    public String getKey()
    {
        return "key";
    }

    public void test_Indexed_Property()
            throws Throwable
    {
        Map map = new HashMap();
        map.put("key", "value");

        Node expression = Ognl.compileExpression(_context, this, "key");
        assertEquals("key", expression.getAccessor().get(_context, this));
    }

    IndexedMapObject mapObject = new IndexedMapObject("propertyValue");

    public IndexedMapObject getObject()
    {
        return mapObject;
    }

    public String getPropertyKey()
    {
        return "property";
    }

    public void test_Indexed_Map_Property()
            throws Throwable
    {
        assertEquals("propertyValue", Ognl.getValue("object[propertyKey]", this));

        _context.clear();
        Node expression = Ognl.compileExpression(_context, this, "object[#this.propertyKey]");
        assertEquals("propertyValue", expression.getAccessor().get(_context, this));

        _context.clear();
        expression = Ognl.compileExpression(_context, this, "object[propertyKey]");
        assertEquals("propertyValue", expression.getAccessor().get(_context, this));
    }

    public void test_Set_Generic_Property() throws Exception
    {
        _context.clear();
        
        GenericRoot root = new GenericRoot();

        Node node = Ognl.compileExpression(_context, root, "cracker.param");
        assertEquals(null, node.getAccessor().get(_context, root));

        node.getAccessor().set(_context, root, new Integer(0));
        assertEquals(new Integer(0), node.getAccessor().get(_context, root));

        node.getAccessor().set(_context, root, new Integer(12));
        assertEquals(new Integer(12), node.getAccessor().get(_context, root));
    }

    /**
     * Test ApplyExpressionMaxLength() mechanism for OGNL expression parsing.
     * 
     * @throws Exception 
     */
    public void test_ApplyExpressionMaxLength() throws Exception {
        final OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);
        final String shortFakeExpression = new String(new char[10]).replace('\0', 'S');
        final String mediumFakeExpression = new String(new char[100]).replace('\0', 'S');
        final String longFakeExpression = new String(new char[1000]).replace('\0', 'S');
        final String veryLongFakeExpression = new String(new char[10000]).replace('\0', 'S');

        try {
            // ---------------------------------------------------------------------
            // Test initial default state.  Any length expression should work
            try {
                Ognl.parseExpression(shortFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(mediumFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(longFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(veryLongFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Confirm illegal length values are rejected
            try {
                Ognl.applyExpressionMaxLength(Integer.MIN_VALUE);
                fail("applyExpressionMaxLength illegal value " + Integer.MIN_VALUE + " was permitted ?");
            } catch (IllegalArgumentException iaex) {
                // Expected result
            } catch (Exception ex) {
                fail ("applyExpressionMaxLength illegal value " + Integer.MIN_VALUE + " failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Confirm maximum length value are accepted
            try {
                Ognl.applyExpressionMaxLength(Integer.MAX_VALUE);
            } catch (Exception ex) {
                fail("applyExpressionMaxLength value " + Integer.MAX_VALUE + " failed unexpectedly - Error: " + ex);
            }

            // Test state with maximum length limit.  Any length expression should work
            try {
                Ognl.parseExpression(shortFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(mediumFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(longFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(veryLongFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Confirm all lengths up to and equal to the largest testing string are accepted
            try {
                Ognl.applyExpressionMaxLength(veryLongFakeExpression.length());
            } catch (Exception ex) {
                fail("applyExpressionMaxLength value " + veryLongFakeExpression.length() + " failed unexpectedly - Error: " + ex);
            }

            // Test state with veryLongFakeExpression.length() limit.  All tested length expressions should work
            try {
                Ognl.parseExpression(shortFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(mediumFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(longFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(veryLongFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Confirm all lengths less than the largest testing string length are accepted
            try {
                Ognl.applyExpressionMaxLength(veryLongFakeExpression.length() - 1);
            } catch (Exception ex) {
                fail("applyExpressionMaxLength value " + (veryLongFakeExpression.length() - 1) + " failed unexpectedly - Error: " + ex);
            }

            // Test state with veryLongFakeExpression.length() -1 limit.  Only veryLongFakeExpression should fail
            try {
                Ognl.parseExpression(shortFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(mediumFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(longFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(veryLongFakeExpression);
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Confirm all lengths greater than the shortest testing string length are rejected
            try {
                Ognl.applyExpressionMaxLength(shortFakeExpression.length());
            } catch (Exception ex) {
                fail("applyExpressionMaxLength value " + shortFakeExpression.length() + " failed unexpectedly - Error: " + ex);
            }

            // Test state with shortFakeExpression.length() limit.  Only shortFakeExpression should succeed
            try {
                Ognl.parseExpression(shortFakeExpression);
            } catch (Exception ex) {
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(mediumFakeExpression);
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(longFakeExpression);
                fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of longFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(veryLongFakeExpression);
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Confirm even the shortest testing string length is rejected
            try {
                Ognl.applyExpressionMaxLength(shortFakeExpression.length() - 1);
            } catch (Exception ex) {
                fail("applyExpressionMaxLength value " + (shortFakeExpression.length() - 1) + " failed unexpectedly - Error: " + ex);
            }

            // Test state with shortFakeExpression.length() limit.  Only shortFakeExpression should succeed
            try {
                Ognl.parseExpression(shortFakeExpression);
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(mediumFakeExpression);
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(longFakeExpression);
                fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of longFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(veryLongFakeExpression);
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Confirm only the empty string is not rejected
            try {
                Ognl.applyExpressionMaxLength(0);
            } catch (Exception ex) {
                fail("applyExpressionMaxLength value 0 failed unexpectedly - Error: " + ex);
            }

            // Test state with 0 length limit.  Only the empty string should succeed
            try {
                Ognl.parseExpression("");
            } catch (ExpressionSyntaxException esx) {
                // Expected for an empty expression (acceptable state).
            } catch (Exception ex) {
                fail ("Parse of empty string failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(shortFakeExpression);
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of shortFakeExpression (" + shortFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(mediumFakeExpression);
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of mediumFakeExpression (" + mediumFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(longFakeExpression);
                fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of longFakeExpression (" + longFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of longFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }

            try {
                Ognl.parseExpression(veryLongFakeExpression);
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") succeded unexpectedly after limit set below its length ?");
            } catch (OgnlException oex) {
                if (oex.getCause() instanceof SecurityException) {
                    // Expected result
                } else {
                    fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly after limit set below its length - Error: " + oex);
                }
            } catch (Exception ex) {
                fail ("Parse of veryLongFakeExpression (" + veryLongFakeExpression.length() + ") failed unexpectedly - Error: " + ex);
            }
        } finally {
            try {
                Ognl.applyExpressionMaxLength(null);  // Reset to default state before leaving test.
            } catch (Exception ex) {} // Do not care for cleanup
        }
    }

    /**
     * Test freezing and thawing of maximum expression length mechanism for OGNL expression parsing.
     * 
     * @throws Exception 
     */
    public void test_FreezeThawExpressionMaxLength() throws Exception {
        final OgnlContext context = (OgnlContext) Ognl.createDefaultContext(null);

        try {
            // ---------------------------------------------------------------------
            // Test initial default state.  Can change maximum length to valid values without any issues
            try {
                Ognl.applyExpressionMaxLength(0);
                Ognl.applyExpressionMaxLength(Integer.MAX_VALUE);
                Ognl.applyExpressionMaxLength(0);
                Ognl.applyExpressionMaxLength(10000);
                Ognl.applyExpressionMaxLength(1000);
                Ognl.applyExpressionMaxLength(100);
                Ognl.applyExpressionMaxLength(10);
            } catch (Exception ex) {
                fail ("applyExpressionMaxLength in default (initial) state with legal values failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Test thawing permitted even if never frozen, does not prevent setting afterward
            try {
                Ognl.thawExpressionMaxLength();
                Ognl.applyExpressionMaxLength(Integer.MAX_VALUE);
                Ognl.thawExpressionMaxLength();
            } catch (IllegalStateException ise) {
                fail ("applyExpressionMaxLength was blocked when thawed ?");
                // Expected result
            } catch (Exception ex) {
                fail ("applyExpressionMaxLength (thaw attempt) failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Test freezing maximum length
            try {
                Ognl.freezeExpressionMaxLength();
                Ognl.applyExpressionMaxLength(Integer.MAX_VALUE);
                fail ("applyExpressionMaxLength was not blocked when frozen ?");
            } catch (IllegalStateException ise) {
                // Expected result
            } catch (Exception ex) {
                fail ("applyExpressionMaxLength (freeze attempt) failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Test repetative freezing
            try {
                Ognl.freezeExpressionMaxLength();
                Ognl.freezeExpressionMaxLength();
            } catch (Exception ex) {
                fail ("freezeExpressionMaxLength failed during repetative freeze operations - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Confirm still frozen, then thaw and demonstrate set permitted
            try {
                Ognl.applyExpressionMaxLength(Integer.MAX_VALUE);
                fail ("applyExpressionMaxLength was not blocked when frozen ?");
            } catch (IllegalStateException ise) {
                // Expected result
            } catch (Exception ex) {
                fail ("applyExpressionMaxLength (when frozen) failed unexpectedly - Error: " + ex);
            }
            try {
                Ognl.thawExpressionMaxLength();
                Ognl.applyExpressionMaxLength(Integer.MAX_VALUE);
            } catch (IllegalStateException ise) {
                fail ("applyExpressionMaxLength was blocked when thawed ?");
                // Expected result
            } catch (Exception ex) {
                fail ("applyExpressionMaxLength (thaw attempt) failed unexpectedly - Error: " + ex);
            }

            // ---------------------------------------------------------------------
            // Test repetative thawing
            try {
                Ognl.thawExpressionMaxLength();
                Ognl.thawExpressionMaxLength();
            } catch (Exception ex) {
                fail ("thawExpressionMaxLength failed during repetative thaw operations - Error: " + ex);
            }
        } finally {
            try {
                Ognl.thawExpressionMaxLength();  // Reset to default state before leaving test.
            } catch (Exception ex) {} // Do not care for cleanup
            try {
                Ognl.applyExpressionMaxLength(null);  // Reset to default state before leaving test.
            } catch (Exception ex) {} // Do not care for cleanup
        }
    }

    /**
     * Test if OGNL can parse an expression involving a package with "or" in it, and
     * successfully call a static method of a class in that package.
     * 
     * @throws Throwable 
     */
    public void test_package_with_or_in_name() throws Throwable
    {
        try {
            Node expr = (Node) Ognl.parseExpression("@test.or.BasicTest@generateId()");
            Object obj = expr.getValue(_context, null);
            assertNotNull("Expression result is null ?", obj);
        } catch (Exception ex) {
            fail("OGNL was unable to parse package with 'or' in its name, but newer versions should succeed");
        }
    }


    /**
     * Test if OGNL can parse an expression involving a package with "or" in it, and
     * successfully compare something using instanceof.
     * 
     * @throws Throwable 
     */
    public void test_package_with_or_in_name_instanceof() throws Throwable
    {
        try {
            Node expr = (Node) Ognl.parseExpression("null instanceof test.or.BasicTest");
            Object obj = expr.getValue(_context, null);
            assertNotNull("Expression result is null ?", obj);
        } catch (Exception ex) {
            fail("OGNL was unable to parse package with 'or' in its name, but newer versions should succeed");
        }
    }

    /**
     * Test if OGNL can parse an expression involving a package with "and" in it.
     * 
     * @throws Throwable 
     */
    public void test_nonexistent_package_with_and_in_name() throws Throwable
    {
        try {
            Node expr = (Node) Ognl.parseExpression("@test.and.BasicTest@generateId()");
            Object obj = expr.getValue(_context, null);
            fail("Was able to get a value for a package and class that does not exist ?");
        } catch (MethodFailedException mfe) {
            // Test would cause an ExpressionSyntaxException (cause ParseException) with older OGNL versions.
            // With newer OGNL versions it will cause a MethodFailedException (cause ClassNotFoundException).
            // The MethodFailedException is the expected/desired outcome now, as it demonstrates that package names that are also
            // OGNL tokens can be parsed successfully to find classes.
            assertTrue("Cause was not a ClassNotFoundException ?", mfe.getCause() instanceof ClassNotFoundException);
        }
    }

    /**
     * Test if OGNL can parse an expression involving a package with "and" in it.
     * 
     * @throws Throwable 
     */
    public void test_nonexistent_package_with_and_in_name_instanceof() throws Throwable
    {
        try {
            Node expr = (Node) Ognl.parseExpression("null instanceof test.and.BasicTest");
            Object obj = expr.getValue(_context, null);
            fail("Was able to get a value for a package and class that does not exist ?");
        } catch (OgnlException oex) {
            // Test would cause an ExpressionSyntaxException (cause ParseException) with older OGNL versions.
            // With newer OGNL versions it will cause a OgnlException (cause ClassNotFoundException).
            // The OgnlException is the expected/desired outcome now, as it demonstrates that package names that are also
            // OGNL tokens can be parsed successfully to find classes.
            assertTrue("Cause was not a ClassNotFoundException ?", oex.getCause() instanceof ClassNotFoundException);
        }
    }

    /**
     * Test if OGNL can parse an expression involving a package with "and" in it.
     * 
     * @throws Throwable 
     */
    public void test_nonexistent_package_with_and_in_name_twice() throws Throwable
    {
        try {
            Node expr = (Node) Ognl.parseExpression("@test.and.and.BasicTest@generateId()");
            Object obj = expr.getValue(_context, null);
            fail("Was able to get a value for a package and class that does not exist ?");
        } catch (MethodFailedException mfe) {
            // Test would cause an ExpressionSyntaxException (cause ParseException) with older OGNL versions.
            // With newer OGNL versions it will cause a MethodFailedException(cause ClassNotFoundException).
            // The MethodFailedException is the expected/desired outcome now, as it demonstrates that package names that are also
            // OGNL tokens can be parsed successfully to find classes.
            assertTrue("Cause was not a ClassNotFoundException ?", mfe.getCause() instanceof ClassNotFoundException);
        }
    }

    /**
     * Test if OGNL can parse an expression involving a package with "and" in it.
     * 
     * @throws Throwable 
     */
    public void test_nonexistent_package_with_and_in_name_multiple_times_separated() throws Throwable
    {
        try {
            Node expr = (Node) Ognl.parseExpression("@test.and.some.and.other.BasicTest@generateId()");
            Object obj = expr.getValue(_context, null);
            fail("Was able to get a value for a package and class that does not exist ?");
        } catch (MethodFailedException mfe) {
            // Test would cause an ExpressionSyntaxException (cause ParseException) with older OGNL versions.
            // With newer OGNL versions it will cause a MethodFailedException(cause ClassNotFoundException).
            // The MethodFailedException is the expected/desired outcome now, as it demonstrates that package names that are also
            // OGNL tokens can be parsed successfully to find classes.
            assertTrue("Cause was not a ClassNotFoundException ?", mfe.getCause() instanceof ClassNotFoundException);
        }
    }

    /**
     * Test if OGNL can parse an expression involving a package with all of the possible valid identifier token names in it.
     * 
     * @throws Throwable 
     */
    public void test_nonexistent_package_with_all_valid_token_names() throws Throwable
    {
        try {
            Node expr = (Node) Ognl.parseExpression("@test.or.and.bor.xor.band.eq.neq.lt.gt.lte.gte.in.not.shl.shr.ushr.true.false.null.BasicTest@generateId()");
            Object obj = expr.getValue(_context, null);
            fail("Was able to get a value for a package and class that does not exist ?");
        } catch (MethodFailedException mfe) {
            // Test would cause an ExpressionSyntaxException (cause ParseException) with older OGNL versions.
            // With newer OGNL versions it will cause a MethodFailedException(cause ClassNotFoundException).
            // The MethodFailedException is the expected/desired outcome now, as it demonstrates that package names that are also
            // OGNL tokens can be parsed successfully to find classes.
            assertTrue("Cause was not a ClassNotFoundException ?", mfe.getCause() instanceof ClassNotFoundException);
        }
    }

    /**
     * Test if OGNL can parse an expression involving a package with "or" starting it.
     * 
     * @throws Throwable 
     */
    public void test_nonexistent_package_starting_with_or_in_name() throws Throwable
    {
        try {
            Node expr = (Node) Ognl.parseExpression("@or.BasicTest@generateId()");
            fail("Was able to parse an expression whose package started with or ?");
        } catch (ExpressionSyntaxException esex) {
            // This test should cause an ExpressionSyntaxException (cause ParseException) with all OGNL versions.
            assertTrue("Cause was not a ParseException ?", esex.getCause() instanceof ParseException);
        }
    }
}
