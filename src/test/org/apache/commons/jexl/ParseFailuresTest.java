/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl;

import junit.framework.TestCase;

import org.apache.commons.jexl.parser.ParseException;

/**
 * Tests for malformed expressions.
 * ({@link ExpressionFactory} should throw {@link ParseException}s).
 *
 * @since 1.1
 */
public class ParseFailuresTest extends TestCase {

    /**
     * Create the test.
     *
     * @param testName name of the test
     */
    public ParseFailuresTest(String testName) {
        super(testName);
    }

    public void testMalformedExpression1() throws Exception {
        // this will throw a ParseException
        String badExpression = "eq";
        try {
            Expression e = ExpressionFactory.createExpression(badExpression);
            fail("Parsing \"" + badExpression
                + "\" should result in a ParseException");
        } catch (ParseException pe) {
            // expected
        }
    }

    public void testMalformedExpression2() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a ParseException
        String badExpression = "?";
        try {
            Expression e = ExpressionFactory.createExpression(badExpression);
            fail("Parsing \"" + badExpression
                + "\" should result in a ParseException");
        } catch (ParseException pe) {
            // expected
        }
    }

}