/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", "Jexl" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.jexl;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.jexl.parser.Parser;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.resolver.FlatResolver;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 *  Simple testcases
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: JexlTest.java,v 1.16 2002/08/19 22:41:10 jstrachan Exp $
 */
public class JexlTest extends TestCase
{
    protected static final String METHOD_STRING = "Method string";
    protected static final String GET_METHOD_STRING = "GetMethod string";

    protected static final String[] GET_METHOD_ARRAY =
        new String[] { "One", "Two", "Three" };

    protected static final String[][] GET_METHOD_ARRAY2 =
        new String[][] { {"One", "Two", "Three"},{"Four", "Five", "Six"} };

    public static Test suite()
    {
        return new TestSuite(JexlTest.class);
    }

    public JexlTest(String testName)
    {
        super(testName);
    }

    /**
      *  test a simple property expression
      */
    public void testProperty()
         throws Exception
    {
        /*
         *  tests a simple property expression
         */

        Expression e = ExpressionFactory.createExpression("foo.bar");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("o incorrect", o.equals(GET_METHOD_STRING));
    }

    /**
      *  test a simple method expression
      */
    public void testMethod()
         throws Exception
    {
        /*
         *  tests a simple method expression
         */

        Expression e = ExpressionFactory.createExpression("foo.bar()");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );

        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("o incorrect", o.equals(METHOD_STRING));
    }

    /**
      *  test a simple method expression
      */
    public void testArrayAccess()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("list[1]");
        JexlContext jc = JexlHelper.createContext();

        /*
         *  test List access
         */

        List l = new ArrayList();
        l.add(new Integer(1));
        l.add(new Integer(2));
        l.add(new Integer(3));

        jc.getVars().put("list", l);

        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Integer", o instanceof Integer);
        assertTrue("o incorrect", o.equals(new Integer(2)));

        e = ExpressionFactory.createExpression("list[1+1]");

        o = e.evaluate(jc);

        assertTrue("o not instanceof Integer", o instanceof Integer);
        assertTrue("o incorrect", o.equals(new Integer(3)));

        e = ExpressionFactory.createExpression("list[loc+1]");

        jc.getVars().put("loc", new Integer(1));
        o = e.evaluate(jc);

        assertTrue("o not instanceof Integer", o instanceof Integer);
        assertTrue("o incorrect", o.equals(new Integer(3)));

        /*
         * test array access
         */

        String[] args = {"hello", "there"};

        jc.getVars().put("array", args);

        e = ExpressionFactory.createExpression("array[0]");
        o = e.evaluate(jc);

        assertTrue("array[0]", o.equals("hello"));

        jc.getVars().put("zero", new Integer(0));

        /*
         * to think that this was an intentional syntax...
         */
        e = ExpressionFactory.createExpression("array.0");
        o = e.evaluate(jc);

        assertTrue("array[0]", o.equals("hello"));

        /*
         * test map access
         */

        Map m = new HashMap();
        m.put("foo", "bar");

        jc.getVars().put("map", m);
        jc.getVars().put("key", "foo");

        e = ExpressionFactory.createExpression("map[\"foo\"]");
        o = e.evaluate(jc);
        assertTrue("map[foo]", o.equals("bar"));

        e = ExpressionFactory.createExpression("map[key]");
        o = e.evaluate(jc);
        assertTrue("map[key]", o.equals("bar"));

        /*
         *  test bean access
         */

        jc.getVars().put("foo", new Foo());

        e = ExpressionFactory.createExpression("foo[\"bar\"]");
        o = e.evaluate(jc);

        assertTrue("foo['bar']", o.equals(GET_METHOD_STRING));

        e = ExpressionFactory.createExpression("foo[\"bar\"] == foo.bar");
        o = e.evaluate(jc);

        assertTrue("foo['bar'] == foo.bar", o.equals(Boolean.TRUE));

    }

    public void testMulti()
         throws Exception
    {
        /*
         *  tests a simple property expression
         */

        Expression e = ExpressionFactory.createExpression("foo.innerFoo.bar()");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("o incorrect", o.equals(METHOD_STRING));
    }

    public void testBoolean()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo.convertBoolean(a==b)");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        jc.getVars().put("a", Boolean.TRUE);
        jc.getVars().put("b", Boolean.FALSE);

        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("1 : o incorrect", o.equals("Boolean : false"));

        e = ExpressionFactory.createExpression("foo.convertBoolean(a==true)");
        o = e.evaluate(jc);
        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("2 : o incorrect", o.equals("Boolean : true"));

        e = ExpressionFactory.createExpression("foo.convertBoolean(a==false)");
        o = e.evaluate(jc);
        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("3 : o incorrect", o.equals("Boolean : false"));

        e = ExpressionFactory.createExpression("foo.convertBoolean(true==false)");
        o = e.evaluate(jc);
        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("4 : o incorrect", o.equals("Boolean : false"));

        e = ExpressionFactory.createExpression("true eq false");
        o = e.evaluate(jc);
        assertTrue("true eq false", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("true ne false");
        o = e.evaluate(jc);
        assertTrue("true ne false", o.equals(Boolean.TRUE));
    }

    public void testStringLit()
         throws Exception
    {
        /*
         *  tests a simple property expression
         */

        Expression e = ExpressionFactory.createExpression("foo.get(\"woogie\")");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("o incorrect", o.equals("Repeat : woogie"));
    }

    public void testExpression()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("a == b");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        jc.getVars().put("a", Boolean.TRUE);
        jc.getVars().put("b", Boolean.FALSE);
        jc.getVars().put("num", new Integer(5));

        Object o = e.evaluate(jc);

        assertTrue("1 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("a==true");
        o = e.evaluate(jc);
        assertTrue("2 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("a==false");
        o = e.evaluate(jc);
        assertTrue("3 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("true==false");
        o = e.evaluate(jc);
        assertTrue("4 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("num < 3");
        o = e.evaluate(jc);
        assertTrue("5 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("num <= 5");
        o = e.evaluate(jc);
        assertTrue("6 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("num >= 5");
        o = e.evaluate(jc);
        assertTrue("7 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("num > 4");
        o = e.evaluate(jc);
        assertTrue("8 : o incorrect", o.equals(Boolean.TRUE));

//
//   $$$ GMJ - trying to be spec conformant re addition means no string concat.
//         so get rid of it for the moment.  Will certainly revisit
//
//        e = ExpressionFactory.createExpression("\"foo\" + \"bar\" == \"foobar\"");
//        o = e.evaluate(jc);
//        assertTrue("9 : o incorrect", o.equals(Boolean.TRUE));

    }

    public void testEmpty()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("string", "");
        jc.getVars().put("array", new Object[0]);
        jc.getVars().put("map", new HashMap());
        jc.getVars().put("list", new ArrayList());
        jc.getVars().put("longstring", "thingthing");

        /*
         *  I can't believe anyone thinks this is a syntax.. :)
         */

        Expression e = ExpressionFactory.createExpression("empty nullthing");
        Object o = e.evaluate(jc);
        assertTrue("1 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty string");
        o = e.evaluate(jc);
        assertTrue("2 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty array");
        o = e.evaluate(jc);
        assertTrue("3 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty map");
        o = e.evaluate(jc);
        assertTrue("4 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty list");
        o = e.evaluate(jc);
        assertTrue("5 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty longstring");
        o = e.evaluate(jc);
        assertTrue("6 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("not empty longstring");
        o = e.evaluate(jc);
        assertTrue("7 : o incorrect", o.equals(Boolean.TRUE));

    }

    public void testSize()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("string", "five!");
        jc.getVars().put("array", new Object[5]);

        Map map = new HashMap();

        map.put("1", new Integer(1));
        map.put("2", new Integer(2));
        map.put("3", new Integer(3));
        map.put("4", new Integer(4));
        map.put("5", new Integer(5));

        jc.getVars().put("map", map);

        List list = new ArrayList();

        list.add("");
        list.add("");
        list.add("");
        list.add("");
        list.add("");

        jc.getVars().put("list", list);

        Expression e = ExpressionFactory.createExpression("size(string)");
        Object o = e.evaluate(jc);
        assertTrue("1 : o incorrect", o.equals(new Integer(5)));

        e = ExpressionFactory.createExpression("size(array)");
        o = e.evaluate(jc);
        assertTrue("2 : o incorrect", o.equals(new Integer(5)));

        e = ExpressionFactory.createExpression("size(map)");
        o = e.evaluate(jc);
        assertTrue("3 : o incorrect", o.equals(new Integer(5)));

        e = ExpressionFactory.createExpression("size(list)");
        o = e.evaluate(jc);
        assertTrue("4 : o incorrect", o.equals(new Integer(5)));
    }



    /**
      *  test some simple mathematical calculations
      */
    public void testCalculations()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo + 2");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Integer(2) );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Long", o instanceof Long);
        assertEquals("o incorrect", new Long(4), o);

        e = ExpressionFactory.createExpression("3 + 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(6), o );

        e = ExpressionFactory.createExpression("3 + 3 + foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(8), o );

        e = ExpressionFactory.createExpression("3 * 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(9), o );

        e = ExpressionFactory.createExpression("3 * 3 + foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(11), o );

        e = ExpressionFactory.createExpression("3 * 3 - foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(7), o );

        /*
         * test some floaty stuff
         */
        e = ExpressionFactory.createExpression("3 * \"3.0\"");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(9), o );

        e = ExpressionFactory.createExpression("3 * 3.0");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(9), o );
    }

    /**
      *  test some simple conditions
      */
    public void testConditions()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo == 2");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Integer(2) );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o);

        e = ExpressionFactory.createExpression("2 == 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("3 == foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("3 != foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("foo != 2");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );
    }

    /**
      *  test some simple conditions
      */
    public void testNotConditions()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("!x");
        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();
        jc.getVars().put("x", Boolean.TRUE );
        jc.getVars().put("foo", foo );
        jc.getVars().put("bar", "true" );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.FALSE, o);

        e = ExpressionFactory.createExpression("x");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("!bar");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("!foo.isSimple()");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );
        
        e = ExpressionFactory.createExpression("foo.isSimple()");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
        
        e = ExpressionFactory.createExpression("!foo.simple");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );
        
        e = ExpressionFactory.createExpression("foo.simple");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
        
        e = ExpressionFactory.createExpression("foo.getCheeseList().size() == 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
        
        e = ExpressionFactory.createExpression("foo.cheeseList.size() == 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
    }


    /**
      *  test some simple conditions
      */
    public void testNotConditionsWithDots()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("x.a == true");
        e.addPostResolver(new FlatResolver());
        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();
        jc.getVars().put("x.a", Boolean.TRUE );
        jc.getVars().put("x.b", Boolean.FALSE );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o );
        
        e = ExpressionFactory.createExpression("!x.a");
        e.addPreResolver(new FlatResolver());
        o = e.evaluate(jc);
        
        assertEquals("o incorrect", Boolean.FALSE, o);

        e = ExpressionFactory.createExpression("!x.b");
        e.addPreResolver(new FlatResolver());
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
    }

    /**
      *  test some simple conditions
      */
    public void testComparisons()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo.indexOf('quick') > 0");
        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();
        jc.getVars().put("foo", "the quick and lazy fox" );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o);

        e = ExpressionFactory.createExpression("foo.indexOf('bar') >= 0");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("foo.indexOf('bar') < 0");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
    }



    /**
      *  test some null conditions
      */
    public void testNull()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("bar", new Integer(2) );
        
        Expression e = ExpressionFactory.createExpression("empty foo");
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o);

        /*
         * also support the functional syntax extension
         */
        e = ExpressionFactory.createExpression("empty(foo)");
        o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o);

        e = ExpressionFactory.createExpression("bar == null");
        o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("bar != null");
        o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("foo != null");
        o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.FALSE, o);

        e = ExpressionFactory.createExpression("foo == null");
        o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o);
    }

    /**
      *  test some blank strings
      */
    public void testBlankStrings()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("bar", "" );
        
        Expression e = ExpressionFactory.createExpression("foo == ''");
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.FALSE, o);

        e = ExpressionFactory.createExpression("bar == ''");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("barnotexist == ''");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("empty bar");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
        
        e = ExpressionFactory.createExpression("bar.length() == 0");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
    }

    /**
      *  test some simple double array lookups
      */
    public void testDoubleArrays()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo[0][1]");
        JexlContext jc = JexlHelper.createContext();

        Object[][] foo = new Object[2][2];
        foo[0][0] = "one";
        foo[0][1] = "two";
        
        jc.getVars().put("foo", foo );
        Object o = e.evaluate(jc);

        assertEquals("o incorrect", "two", o);
    }

    /**
      *  test variables with underscore names
      */
    public void testVariableNames()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo_bar");
        JexlContext jc = JexlHelper.createContext();

        
        jc.getVars().put("foo_bar", "123" );
        Object o = e.evaluate(jc);

        assertEquals("o incorrect", "123", o);
    }

    /**
      *  test the use of dot notation to lookup map entries
      */
    public void testMapDot()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo.bar");
        JexlContext jc = JexlHelper.createContext();

        Map foo = new HashMap();
        foo.put( "bar", "123" );
                
        jc.getVars().put("foo", foo );
        Object o = e.evaluate(jc);

        assertEquals("o incorrect", "123", o);
    }

    /**
     *  Tests string literals
     */
    public void testStringLiterals()
        throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo == \"bar\"");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", "bar" );

        Object o = e.evaluate(jc);

        assertTrue("o incorrect", Boolean.TRUE.equals(o));

        e = ExpressionFactory.createExpression("foo == 'bar'");
        o = e.evaluate(jc);

        assertTrue("o incorrect", Boolean.TRUE.equals(o));
    }

    /**
      *  test the use of an int based property
      */
    public void testIntProperty()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo.count");
        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();
                
        jc.getVars().put("foo", foo );
        Object o = e.evaluate(jc);

        assertEquals("o incorrect", new Integer(5), o);
    }

    public void testArrayProperty()
        throws Exception
    {
        Expression bracketForm =
            ExpressionFactory.createExpression("foo.array[1]");

        Expression dotForm =
            ExpressionFactory.createExpression("foo.array.1");

        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();

        jc.getVars().put("foo", foo );

        Object o1 = bracketForm.evaluate(jc);
        assertEquals("bracket form failed", GET_METHOD_ARRAY[1], o1);

        Object o2 = dotForm.evaluate(jc);
        assertEquals("dot form failed", GET_METHOD_ARRAY[1], o2);


        bracketForm =
            ExpressionFactory.createExpression("foo.array2[1][1]");

//        dotForm =
//            ExpressionFactory.createExpression("foo.array2.1.1");

        jc = JexlHelper.createContext();

        jc.getVars().put("foo", foo );

        o1 = bracketForm.evaluate(jc);
        assertEquals("bracket form failed", GET_METHOD_ARRAY2[1][1], o1);

//        o2 = dotForm.evaluate(jc);
//        assertEquals("dot form failed", GET_METHOD_ARRAY2[1][1], o2);
    }

    public void testResolver()
        throws Exception
    {
        /*
         * first, a simple override
         */

        Expression expr =
            ExpressionFactory.createExpression("foo.bar");

        expr.addPreResolver(new FlatResolver());

        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();

        jc.getVars().put("foo.bar", "flat value");
        jc.getVars().put("foo", foo );

        Object o = expr.evaluate(jc);

        assertEquals("flat override", o,"flat value");

        /*
         * now, let the resolver not find it and have it drop to jexl
         */

        expr =
            ExpressionFactory.createExpression("foo.bar.length()");

        expr.addPreResolver(new FlatResolver());

        o = expr.evaluate(jc);

        assertEquals("flat override 1", o,new Integer(GET_METHOD_STRING.length()));

        /*
         * now, let the resolver not find it and NOT drop to jexl
         */

        expr =
            ExpressionFactory.createExpression("foo.bar.length()");

        expr.addPreResolver(new FlatResolver(false));

        o = expr.evaluate(jc);

        assertEquals("flat override 2", o, null);

    }

    public class Foo
    {
        public String bar()
        {
            return METHOD_STRING;
        }

        public String getBar()
        {
            return GET_METHOD_STRING;
        }

        public Foo getInnerFoo()
        {
            return new Foo();
        }

        public String get(String arg)
        {
            return "Repeat : " + arg;
        }

        public String convertBoolean(boolean b)
        {
            return "Boolean : " + b;
        }

        public int getCount() {
            return 5;
        }

        public List getCheeseList() 
        {
            ArrayList answer = new ArrayList();
            answer.add("cheddar");
            answer.add("edam");
            answer.add("brie");
            return answer;
        }
            
        public String[] getArray()
        {
            return GET_METHOD_ARRAY;
        }

        public String[][] getArray2()
        {
            return GET_METHOD_ARRAY2;
        }
        
        public boolean isSimple() 
        {
            return true;
        }
    }

}
