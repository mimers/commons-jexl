/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlScript;

import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import org.apache.commons.jexl3.parser.ASTAdditiveNode;
import org.apache.commons.jexl3.parser.ASTAdditiveOperator;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTSizeMethod;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.Node;
import org.apache.commons.jexl3.parser.ParserVisitor;

import org.apache.commons.logging.Log;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.jexl3.parser.ASTJexlLambda;

/**
 * An interpreter of JEXL syntax.
 *
 * @since 2.0
 */
public class Interpreter extends ParserVisitor {
    /** The JEXL engine. */
    protected final Engine jexl;
    /** The logger. */
    protected final Log logger;
    /** The uberspect. */
    protected final JexlUberspect uberspect;
    /** The arithmetic handler. */
    protected final JexlArithmetic arithmetic;
    /** The map of registered functions. */
    protected final Map<String, Object> functions;
    /** The map of registered functions. */
    protected Map<String, Object> functors;
    /** The context to store/retrieve variables. */
    protected final JexlContext context;
    /** Strict interpreter flag. */
    protected final boolean strictEngine;
    /** Strict interpreter flag. */
    protected final boolean strictArithmetic;
    /** Silent intepreter flag. */
    protected final boolean silent;
    /** Cache executors. */
    protected final boolean cache;
    /** Registers or arguments. */
    protected final Frame frame;
    /** Cancellation support. */
    protected volatile boolean cancelled = false;
    /** Empty parameters for method matching. */
    protected static final Object[] EMPTY_PARAMS = new Object[0];

    /**
     * Creates an interpreter.
     * @param engine the engine creating this interpreter
     * @param aContext the context to evaluate expression
     * @param frame the engine evaluation frame
     */
    protected Interpreter(Engine engine, JexlContext aContext, Frame eFrame) {
        this.jexl = engine;
        this.logger = jexl.logger;
        this.uberspect = jexl.uberspect;
        this.context = aContext != null ? aContext : Engine.EMPTY_CONTEXT;
        if (this.context instanceof JexlEngine.Options) {
            JexlEngine.Options opts = (JexlEngine.Options) context;
            Boolean ostrict = opts.isStrict();
            Boolean osilent = opts.isSilent();
            this.strictEngine = ostrict == null ? jexl.isStrict() : ostrict.booleanValue();
            this.silent = osilent == null ? jexl.isSilent() : osilent.booleanValue();
            this.arithmetic = jexl.arithmetic.options(opts);
        } else {
            this.strictEngine = jexl.isStrict();
            this.silent = jexl.isSilent();
            this.arithmetic = jexl.arithmetic;
        }
        this.functions = jexl.functions;
        this.strictArithmetic = this.arithmetic.isStrict();
        this.cache = jexl.cache != null;
        this.frame = eFrame;
        this.functors = null;
    }

    protected Interpreter(Interpreter copy, Frame eFrame) {
        this.jexl = copy.jexl;
        this.logger = copy.logger;
        this.uberspect = copy.uberspect;
        this.context = copy.context;
        this.arithmetic = copy.arithmetic;
        this.silent = copy.silent;
        this.functions = copy.functions;
        this.strictEngine = copy.strictEngine;
        this.strictArithmetic = copy.strictArithmetic;
        this.cache = copy.cache;
        this.frame = eFrame;
        this.functors = null;
    }

    /**
     * Interpret the given script/expression.
     * <p>
     * If the underlying JEXL engine is silent, errors will be logged through its logger as info.
     * </p>
     * @param node the script or expression to interpret.
     * @return the result of the interpretation.
     * @throws JexlException if any error occurs during interpretation.
     */
    public Object interpret(JexlNode node) {
        try {
            return node.jjtAccept(this, null);
        } catch (JexlException.Return xreturn) {
            Object value = xreturn.getValue();
            return value;
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl;
        } finally {
            functors = null;
        }
    }

    /**
     * Finds the node causing a NPE for diadic operators.
     * @param xrt the RuntimeException
     * @param node the parent node
     * @param left the left argument
     * @param right the right argument
     * @return the left, right or parent node
     */
    protected JexlNode findNullOperand(RuntimeException xrt, JexlNode node, Object left, Object right) {
        if (xrt instanceof ArithmeticException
                && (Object) JexlException.NULL_OPERAND == xrt.getMessage()) {
            if (left == null) {
                return node.jjtGetChild(0);
            }
            if (right == null) {
                return node.jjtGetChild(1);
            }
        }
        return node;
    }

    /**
     * Triggered when variable can not be resolved.
     * @param xjexl the JexlException ("undefined variable " + variable)
     * @return throws JexlException if isStrict, null otherwise
     */
    protected Object unknownVariable(JexlException xjexl) {
        if (strictEngine) {
            throw xjexl;
        }
        if (!silent) {
            logger.warn(xjexl.getMessage());
        }
        return null;
    }

    /**
     * Triggered when method, function or constructor invocation fails.
     * @param xjexl the JexlException wrapping the original error
     * @return throws JexlException if isStrict, null otherwise
     */
    protected Object invocationFailed(JexlException xjexl) {
        if (strictEngine || xjexl instanceof JexlException.Return) {
            throw xjexl;
        }
        if (!silent) {
            logger.warn(xjexl.getMessage(), xjexl.getCause());
        }
        return null;
    }

    /**
     * Checks whether this interpreter execution was cancelled due to thread interruption.
     * @return true if cancelled, false otherwise
     */
    protected boolean isCancelled() {
        if (cancelled | Thread.interrupted()) {
            cancelled = true;
        }
        return cancelled;
    }

    /**
     * Resolves a namespace, eventually allocating an instance using context as constructor argument.
     * The lifetime of such instances span the current expression or script evaluation.
     *
     * @param prefix the prefix name (may be null for global namespace)
     * @param node the AST node
     * @return the namespace instance
     */
    protected Object resolveNamespace(String prefix, JexlNode node) {
        Object namespace = null;
        // check whether this namespace is a functor
        if (functors != null) {
            namespace = functors.get(prefix);
            if (namespace != null) {
                return namespace;
            }
        }
        // check if namespace if a resolver
        if (context instanceof JexlContext.NamespaceResolver) {
            namespace = ((JexlContext.NamespaceResolver) context).resolveNamespace(prefix);
        }
        if (namespace == null) {
            namespace = functions.get(prefix);
            if (prefix != null && namespace == null) {
                throw new JexlException(node, "no such function namespace " + prefix, null);
            }
        }
        // allow namespace to be instantiated as functor with context if possible, not an error otherwise
        if (namespace instanceof Class<?>) {
            Object[] args = new Object[]{context};
            JexlMethod ctor = uberspect.getConstructor(namespace, args);
            if (ctor != null) {
                try {
                    namespace = ctor.invoke(namespace, args);
                    if (functors == null) {
                        functors = new HashMap<String, Object>();
                    }
                    functors.put(prefix, namespace);
                } catch (Exception xinst) {
                    throw new JexlException(node, "unable to instantiate namespace " + prefix, xinst);
                }
            }
        }
        return namespace;
    }

    @Override
    protected Object visit(ASTAdditiveNode node, Object data) {
        /**
         * The pattern for exception mgmt is to let the child*.jjtAccept
         * out of the try/catch loop so that if one fails, the ex will
         * traverse up to the interpreter.
         * In cases where this is not convenient/possible, JexlException must
         * be caught explicitly and rethrown.
         */
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        for (int c = 2, size = node.jjtGetNumChildren(); c < size; c += 2) {
            Object right = node.jjtGetChild(c).jjtAccept(this, data);
            try {
                JexlNode op = node.jjtGetChild(c - 1);
                if (op instanceof ASTAdditiveOperator) {
                    String which = op.image;
                    if ("+".equals(which)) {
                        left = arithmetic.add(left, right);
                        continue;
                    }
                    if ("-".equals(which)) {
                        left = arithmetic.subtract(left, right);
                        continue;
                    }
                    throw new UnsupportedOperationException("unknown operator " + which);
                }
                throw new IllegalArgumentException("unknown operator " + op);
            } catch (ArithmeticException xrt) {
                JexlNode xnode = findNullOperand(xrt, node, left, right);
                throw new JexlException(xnode, "+/- error", xrt);
            }
        }
        return left;
    }

    @Override
    protected Object visit(ASTAdditiveOperator node, Object data) {
        throw new UnsupportedOperationException("Shoud not be called.");
    }

    @Override
    protected Object visit(ASTAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (!leftValue) {
                return Boolean.FALSE;
            }
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (!rightValue) {
                return Boolean.FALSE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTArrayAccess node, Object data) {
        // first objectNode is the identifier
        Object object = node.jjtGetChild(0).jjtAccept(this, data);
        // can have multiple nodes - either an expression, integer literal or reference
        int numChildren = node.jjtGetNumChildren();
        for (int i = 1; i < numChildren; i++) {
            JexlNode nindex = node.jjtGetChild(i);
            if (nindex instanceof JexlNode.Literal<?>) {
                object = nindex.jjtAccept(this, object);
            } else {
                Object index = nindex.jjtAccept(this, null);
                object = getAttribute(object, index, nindex);
            }
        }

        return object;
    }

    @Override
    protected Object visit(ASTArrayLiteral node, Object data) {
        Object literal = node.getLiteral();
        if (literal == null) {
            int childCount = node.jjtGetNumChildren();
            Object[] array = new Object[childCount];
            for (int i = 0; i < childCount; i++) {
                Object entry = node.jjtGetChild(i).jjtAccept(this, data);
                array[i] = entry;
            }
            literal = arithmetic.narrowArrayType(array);
            node.setLiteral(literal);
        }
        return literal;
    }

    @Override
    protected Object visit(ASTAssignment node, Object data) {
        // left contains the reference to assign to
        int register = -1;
        JexlNode left = node.jjtGetChild(0);
        if (left instanceof ASTIdentifier) {
            ASTIdentifier var = (ASTIdentifier) left;
            register = var.getRegister();
            if (register < 0) {
                throw new JexlException(left, "unknown variable " + left.image);
            }
        } else if (!(left instanceof ASTReference)) {
            throw new JexlException(left, "illegal assignment form 0");
        }
        // right is the value expression to assign
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        // determine initial object & property:
        JexlNode objectNode = null;
        Object object = register >= 0 ? frame.registers[register] : null;
        JexlNode propertyNode = null;
        Object property = null;
        boolean isVariable = true;
        int v = 0;
        StringBuilder variableName = null;
        // 1: follow children till penultimate, resolve dot/array
        int last = left.jjtGetNumChildren() - 1;
        // check we are not assigning a register itself
        boolean isRegister = last < 0 && register >= 0;
        // start at 1 if register
        for (int c = register >= 0 ? 1 : 0; c < last; ++c) {
            objectNode = left.jjtGetChild(c);
            // evaluate the property within the object
            object = objectNode.jjtAccept(this, object);
            if (object != null) {
                continue;
            }
            isVariable &= objectNode instanceof ASTIdentifier
                    || (objectNode instanceof ASTNumberLiteral && ((ASTNumberLiteral) objectNode).isInteger());
            // if we get null back as a result, check for an ant variable
            if (isVariable) {
                if (v == 0) {
                    variableName = new StringBuilder(left.jjtGetChild(0).image);
                    v = 1;
                }
                for (; v <= c; ++v) {
                    variableName.append('.');
                    variableName.append(left.jjtGetChild(v).image);
                }
                object = context.get(variableName.toString());
                // disallow mixing ant & bean with same root; avoid ambiguity
                if (object != null) {
                    isVariable = false;
                }
            } else {
                throw new JexlException(objectNode, "illegal assignment form");
            }
        }
        // 2: last objectNode will perform assignement in all cases
        propertyNode = isRegister ? null : left.jjtGetChild(last);
        boolean antVar = false;
        if (propertyNode instanceof ASTIdentifier) {
            ASTIdentifier identifier = (ASTIdentifier) propertyNode;
            register = identifier.getRegister();
            if (register >= 0) {
                isRegister = true;
            } else {
                property = identifier.image;
                antVar = true;
            }
        } else if (propertyNode instanceof ASTNumberLiteral && ((ASTNumberLiteral) propertyNode).isInteger()) {
            property = ((ASTNumberLiteral) propertyNode).getLiteral();
            antVar = true;
        } else if (propertyNode instanceof ASTArrayAccess) {
            // first objectNode is the identifier
            objectNode = propertyNode;
            ASTArrayAccess narray = (ASTArrayAccess) objectNode;
            Object nobject = narray.jjtGetChild(0).jjtAccept(this, object);
            if (nobject == null) {
                throw new JexlException(objectNode, "array element is null");
            } else {
                object = nobject;
            }
            // can have multiple nodes - either an expression, integer literal or
            // reference
            last = narray.jjtGetNumChildren() - 1;
            for (int i = 1; i < last; i++) {
                objectNode = narray.jjtGetChild(i);
                if (objectNode instanceof JexlNode.Literal<?>) {
                    object = objectNode.jjtAccept(this, object);
                } else {
                    Object index = objectNode.jjtAccept(this, null);
                    object = getAttribute(object, index, objectNode);
                }
            }
            property = narray.jjtGetChild(last).jjtAccept(this, null);
        } else if (!isRegister) {
            throw new JexlException(objectNode, "illegal assignment form");
        }
        // deal with ant variable; set context
        if (isRegister) {
            frame.registers[register] = right;
            return right;
        } else if (antVar) {
            if (isVariable && object == null) {
                if (variableName != null) {
                    if (last > 0) {
                        variableName.append('.');
                    }
                    variableName.append(property);
                    property = variableName.toString();
                }
                try {
                    context.set(String.valueOf(property), right);
                } catch (UnsupportedOperationException xsupport) {
                    throw new JexlException(node, "context is readonly", xsupport);
                }
                return right;
            }
        }
        if (property == null) {
            // no property, we fail
            throw new JexlException(propertyNode, "property is null");
        }
        if (object == null) {
            // no object, we fail
            throw new JexlException(objectNode, "bean is null");
        }
        // one before last, assign
        setAttribute(object, property, right, propertyNode);
        return right;
    }

    @Override
    protected Object visit(ASTBitwiseAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.bitwiseAnd(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "& error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseComplNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            return arithmetic.bitwiseComplement(left);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "~ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.bitwiseOr(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "| error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseXorNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.bitwiseXor(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "^ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBlock node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(ASTDivNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.divide(left, right);
        } catch (ArithmeticException xrt) {
            if (!strictArithmetic) {
                return new Double(0.0);
            }
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "divide error", xrt);
        }
    }

    @Override
    protected Object visit(ASTEmptyFunction node, Object data) {
        Object o = node.jjtGetChild(0).jjtAccept(this, data);
        if (o == null) {
            return Boolean.TRUE;
        }
        if (o instanceof String && "".equals(o)) {
            return Boolean.TRUE;
        }
        if (o.getClass().isArray() && ((Object[]) o).length == 0) {
            return Boolean.TRUE;
        }
        if (o instanceof Collection<?>) {
            return ((Collection<?>) o).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
        }
        // Map isn't a collection
        if (o instanceof Map<?, ?>) {
            return ((Map<?, ?>) o).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
        }
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTEQNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "== error", xrt);
        }
    }

    @Override
    protected Object visit(ASTFalseNode node, Object data) {
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTForeachStatement node, Object data) {
        Object result = null;
        /* first objectNode is the loop variable */
        ASTReference loopReference = (ASTReference) node.jjtGetChild(0);
        ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);
        int register = loopVariable.getRegister();
        /* second objectNode is the variable to iterate */
        Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);
        // make sure there is a value to iterate on and a statement to execute
        if (iterableValue != null && node.jjtGetNumChildren() >= 3) {
            /* third objectNode is the statement to execute */
            JexlNode statement = node.jjtGetChild(2);
            // get an iterator for the collection/array etc via the
            // introspector.
            Iterator<?> itemsIterator = uberspect.getIterator(iterableValue);
            if (itemsIterator != null) {
                while (itemsIterator.hasNext()) {
                    if (isCancelled()) {
                        throw new JexlException.Cancel(node);
                    }
                    // set loopVariable to value of iterator
                    Object value = itemsIterator.next();
                    if (register < 0) {
                        context.set(loopVariable.image, value);
                    } else {
                        frame.registers[register] = value;
                    }
                    // execute statement
                    result = statement.jjtAccept(this, data);
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTGENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.greaterThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, ">= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTGTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.greaterThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "> error", xrt);
        }
    }

    @Override
    protected Object visit(ASTERNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            // use arithmetic / pattern matching ?
            if (right instanceof java.util.regex.Pattern || right instanceof String) {
                return arithmetic.matches(left, right) ? Boolean.TRUE : Boolean.FALSE;
            }
            // left in right ? <=> right.contains(left) ?
            // try contains on collection
            if (right instanceof Set<?>) {
                return ((Set<?>) right).contains(left) ? Boolean.TRUE : Boolean.FALSE;
            }
            // try contains on map key
            if (right instanceof Map<?, ?>) {
                return ((Map<?, ?>) right).containsKey(left) ? Boolean.TRUE : Boolean.FALSE;
            }
            // try contains on collection
            if (right instanceof Collection<?>) {
                return ((Collection<?>) right).contains(left) ? Boolean.TRUE : Boolean.FALSE;
            }
            // try a contains method (duck type set)
            try {
                Object[] argv = {left};
                JexlMethod vm = uberspect.getMethod(right, "contains", argv);
                if (vm != null) {
                    return arithmetic.toBoolean(vm.invoke(right, argv)) ? Boolean.TRUE : Boolean.FALSE;
                } else if (arithmetic.narrowArguments(argv)) {
                    vm = uberspect.getMethod(right, "contains", argv);
                    if (vm != null) {
                        return arithmetic.toBoolean(vm.invoke(right, argv)) ? Boolean.TRUE : Boolean.FALSE;
                    }
                }
            } catch (InvocationTargetException e) {
                throw new JexlException(node, "=~ invocation error", e.getCause());
            } catch (Exception e) {
                throw new JexlException(node, "=~ error", e);
            }
            // try iterative comparison
            Iterator<?> it = uberspect.getIterator(right);
            if (it != null) {
                while (it.hasNext()) {
                    Object next = it.next();
                    if (next == left || (next != null && next.equals(left))) {
                        return Boolean.TRUE;
                    }
                }
                return Boolean.FALSE;
            }
            // defaults to equal
            return arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "=~ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTIdentifier node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        String name = node.image;
        if (data == null) {
            int register = node.getRegister();
            if (register >= 0) {
                return frame.registers[register];
            }
            Object value = context.get(name);
            if (value == null
                    && !(node.jjtGetParent() instanceof ASTReference)
                    && !context.has(name)
                    && !isTernaryProtected(node)) {
                JexlException xjexl = new JexlException.Variable(node, name);
                return unknownVariable(xjexl);
            }
            return value;
        } else {
            return getAttribute(data, name, node);
        }
    }

    @Override
    protected Object visit(ASTVar node, Object data) {
        return visit((ASTIdentifier) node, data);
    }

    @Override
    protected Object visit(ASTIfStatement node, Object data) {
        int n = 0;
        try {
            Object result = null;
            /* first objectNode is the condition */
            Object expression = node.jjtGetChild(0).jjtAccept(this, data);
            if (arithmetic.toBoolean(expression)) {
                // first objectNode is true statement
                n = 1;
                result = node.jjtGetChild(1).jjtAccept(this, data);
            } else {
                // if there is a false, execute it. false statement is the second
                // objectNode
                if (node.jjtGetNumChildren() == 3) {
                    n = 2;
                    result = node.jjtGetChild(2).jjtAccept(this, data);
                }
            }
            return result;
        } catch (JexlException error) {
            throw error;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(n), "if error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNumberLiteral node, Object data) {
        if (data != null && node.isInteger()) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTJexlScript node, Object data) {
        if (node instanceof ASTJexlLambda) {
            return new Closure((ASTJexlLambda) node, frame);
        } else {
            final int numChildren = node.jjtGetNumChildren();
            Object result = null;
            for (int i = 0; i < numChildren; i++) {
                JexlNode child = node.jjtGetChild(i);
                result = child.jjtAccept(this, data);
            }
            return result;
        }
    }
    
    private static final class Closure {
        Frame frame;
        ASTJexlLambda lambda;
        Closure(ASTJexlLambda l, Frame f) {
            lambda = l;
            frame = lambda.createFrame(f);
        }
    }
    
    private Object call(Closure closure, Object[] argv) {
        Frame cframe = closure.frame;
        if (cframe != null) {
            cframe.assign(argv);
        }
        ASTJexlLambda lambda = closure.lambda;
        JexlNode block = lambda.jjtGetChild(lambda.jjtGetNumChildren() - 1);
        Interpreter interpreter = new Interpreter(this, cframe);
        return interpreter.interpret(block);
    }

    @Override
    protected Object visit(ASTLENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.lessThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "<= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTLTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.lessThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "< error", xrt);
        }
    }

    @Override
    protected Object visit(ASTMapEntry node, Object data) {
        Object key = node.jjtGetChild(0).jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);
        return new Object[]{key, value};
    }

    @Override
    protected Object visit(ASTMapLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        Map<Object, Object> map = new HashMap<Object, Object>();

        for (int i = 0; i < childCount; i++) {
            Object[] entry = (Object[]) (node.jjtGetChild(i)).jjtAccept(this, data);
            map.put(entry[0], entry[1]);
        }

        return map;
    }

    /**
     * Calls a method (or function).
     * <p>
     * Method resolution is a follows:
     * 1 - attempt to find a method in the bean passed as parameter;
     * 2 - if this fails, narrow the arguments and try again
     * 3 - if this still fails, seeks a JexlScript or JexlMethod as a property of that bean.
     * </p>
     * @param node the method node
     * @param bean the bean this method should be invoked upon
     * @param methodNode the node carrying the method name
     * @param argb the first argument index, child of the method node
     * @return the result of the method invocation
     */
    private Object call(JexlNode node, Object bean, ASTIdentifier methodNode, int argb) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        String methodName = methodNode.image;
        // evaluate the arguments
        int argc = node.jjtGetNumChildren() - argb;
        Object[] argv = new Object[argc];
        for (int i = 0; i < argc; i++) {
            argv[i] = node.jjtGetChild(i + argb).jjtAccept(this, null);
        }

        JexlException xjexl = null;
        try {
            // attempt to reuse last executor cached in volatile JexlNode.value
            if (cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlMethod) {
                    JexlMethod me = (JexlMethod) cached;
                    Object eval = me.tryInvoke(methodName, bean, argv);
                    if (!me.tryFailed(eval)) {
                        return eval;
                    }
                }
            }
            boolean cacheable = cache;
            boolean narrow = true;
            JexlMethod vm = null;
            // pseudo loop
            while (true) {
                vm = uberspect.getMethod(bean, methodName, argv);
                if (vm != null) {
                    break;
                }
                Object functor = null;
                // could not find a method, try as a var
                if (bean == context) {
                    int register = methodNode.getRegister();
                    if (register >= 0) {
                        functor = frame.registers[register];
                    } else {
                        functor = context.get(methodName);
                    }
                } else {
                    JexlPropertyGet gfunctor = uberspect.getPropertyGet(bean, methodName);
                    if (gfunctor != null) {
                        functor = gfunctor.tryInvoke(bean, methodName);
                    }
                }
                // lambda, script or jexl method will do
                if (functor instanceof Closure) {
                    return call((Closure) functor, argv);
                }
                if (functor instanceof JexlScript) {
                    return ((JexlScript) functor).execute(context, argv);
                }
                if (functor instanceof JexlMethod) {
                    return ((JexlMethod) functor).invoke(bean, argv);
                }
                // if we did not find an exact match and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again
                if (narrow && arithmetic.narrowArguments(argv)) {
                    narrow = false;
                } else {
                    break;
                }
            }
            // we have either evaluated and returned or might have found a method
            if (vm != null) {
                // vm cannot be null if xjexl is null
                Object eval = vm.invoke(bean, argv);
                // cache executor in volatile JexlNode.value
                if (cacheable && vm.isCacheable()) {
                    node.jjtSetValue(vm);
                }
                return eval;
            } else {
                xjexl = new JexlException.Method(node, methodName, null);
            }
        } catch (Exception xany) {
            xjexl = new JexlException(node, methodName, xany);
        }
        return invocationFailed(xjexl);
    }

    @Override
    protected Object visit(ASTMethodNode node, Object data) {
        // the object to invoke the method on should be in the data argument
        if (data == null) {
            // if the method node is the first child of the (ASTReference) parent,
            // it is considered as calling a 'top level' function
            if (node.jjtGetParent().jjtGetChild(0) == node) {
                data = resolveNamespace(null, node);
                if (data == null) {
                    data = context;
                }
            } else {
                throw new JexlException(node, "attempting to call method on null");
            }
        }
        // objectNode 0 is the identifier (method name), the others are parameters.
        ASTIdentifier methodNode = (ASTIdentifier) node.jjtGetChild(0);
        return call(node, data, methodNode, 1);
    }

    @Override
    protected Object visit(ASTFunctionNode node, Object data) {
        // objectNode 0 is the prefix
        String prefix = node.jjtGetChild(0).image;
        Object namespace = resolveNamespace(prefix, node);
        // objectNode 1 is the identifier , the others are parameters.
        ASTIdentifier functionNode = (ASTIdentifier) node.jjtGetChild(1);
        return call(node, namespace, functionNode, 2);
    }

    @Override
    protected Object visit(ASTConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // first child is class or class name
        Object cobject = node.jjtGetChild(0).jjtAccept(this, data);
        // get the ctor args
        int argc = node.jjtGetNumChildren() - 1;
        Object[] argv = new Object[argc];
        for (int i = 0; i < argc; i++) {
            argv[i] = node.jjtGetChild(i + 1).jjtAccept(this, null);
        }

        JexlException xjexl = null;
        try {
            // attempt to reuse last constructor cached in volatile JexlNode.value
            if (cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlMethod) {
                    JexlMethod mctor = (JexlMethod) cached;
                    Object eval = mctor.tryInvoke(null, cobject, argv);
                    if (!mctor.tryFailed(eval)) {
                        return eval;
                    }
                }
            }
            JexlMethod ctor = uberspect.getConstructor(cobject, argv);
            // DG: If we can't find an exact match, narrow the parameters and try again
            if (ctor == null) {
                if (arithmetic.narrowArguments(argv)) {
                    ctor = uberspect.getConstructor(cobject, argv);
                }
                if (ctor == null) {
                    String dbgStr = cobject != null ? cobject.toString() : null;
                    xjexl = new JexlException.Method(node, dbgStr, null);
                }
            }
            if (xjexl == null) {
                Object instance = ctor.invoke(cobject, argv);
                // cache executor in volatile JexlNode.value
                if (cache && ctor.isCacheable()) {
                    node.jjtSetValue(ctor);
                }
                return instance;
            }
        } catch (Exception xany) {
            String dbgStr = cobject != null ? cobject.toString() : null;
            xjexl = new JexlException(node, dbgStr, xany);
        }
        return invocationFailed(xjexl);
    }

    @Override
    protected Object visit(ASTModNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.mod(left, right);
        } catch (ArithmeticException xrt) {
            if (!strictArithmetic) {
                return new Double(0.0);
            }
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "% error", xrt);
        }
    }

    @Override
    protected Object visit(ASTMulNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.multiply(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "* error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "!= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNRNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            if (right instanceof java.util.regex.Pattern || right instanceof String) {
                // use arithmetic / pattern matching
                return arithmetic.matches(left, right) ? Boolean.FALSE : Boolean.TRUE;
            }
            // try contains on collection
            if (right instanceof Set<?>) {
                return ((Set<?>) right).contains(left) ? Boolean.FALSE : Boolean.TRUE;
            }
            // try contains on map key
            if (right instanceof Map<?, ?>) {
                return ((Map<?, ?>) right).containsKey(left) ? Boolean.FALSE : Boolean.TRUE;
            }
            // try contains on collection
            if (right instanceof Collection<?>) {
                return ((Collection<?>) right).contains(left) ? Boolean.FALSE : Boolean.TRUE;
            }
            // try a contains method (duck type set)
            try {
                Object[] argv = {left};
                JexlMethod vm = uberspect.getMethod(right, "contains", argv);
                if (vm != null) {
                    return arithmetic.toBoolean(vm.invoke(right, argv)) ? Boolean.FALSE : Boolean.TRUE;
                } else if (arithmetic.narrowArguments(argv)) {
                    vm = uberspect.getMethod(right, "contains", argv);
                    if (vm != null) {
                        return arithmetic.toBoolean(vm.invoke(right, argv)) ? Boolean.FALSE : Boolean.TRUE;
                    }
                }
            } catch (InvocationTargetException e) {
                throw new JexlException(node, "!~ invocation error", e.getCause());
            } catch (Exception e) {
                throw new JexlException(node, "!~ error", e);
            }
            // try iterative comparison
            Iterator<?> it = uberspect.getIterator(right);//, node.jjtGetChild(1));
            if (it != null) {
                while (it.hasNext()) {
                    Object next = it.next();
                    if (next == left || (next != null && next.equals(left))) {
                        return Boolean.FALSE;
                    }
                }
                return Boolean.TRUE;
            }
            // defaults to not equal
            return arithmetic.equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "!~ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNotNode node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        return arithmetic.toBoolean(val) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTNullLiteral node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (leftValue) {
                return Boolean.TRUE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (rightValue) {
                return Boolean.TRUE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTReference node, Object data) {
        // could be array access, identifier or map literal
        // followed by zero or more ("." and array access, method, size,
        // identifier or integer literal)
        int numChildren = node.jjtGetNumChildren();
        // pass first piece of data in and loop through children
        Object result = null;
        StringBuilder variableName = null;
        String propertyName = null;
        boolean isVariable = true;
        int v = 0;
        for (int c = 0; c < numChildren; c++) {
            if (isCancelled()) {
                throw new JexlException.Cancel(node);
            }
            JexlNode theNode = node.jjtGetChild(c);
            // integer literals may be part of an antish var name only if no bean was found so far
            if (result == null && theNode instanceof ASTNumberLiteral && ((ASTNumberLiteral) theNode).isInteger()) {
                isVariable &= v > 0;
            } else {
                isVariable &= (theNode instanceof ASTIdentifier);
                result = theNode.jjtAccept(this, result);
            }
            // if we get null back a result, check for an ant variable
            if (result == null && isVariable) {
                if (v == 0) {
                    variableName = new StringBuilder(node.jjtGetChild(0).image);
                    v = 1;
                }
                for (; v <= c; ++v) {
                    variableName.append('.');
                    variableName.append(node.jjtGetChild(v).image);
                }
                result = context.get(variableName.toString());
            } else {
                propertyName = theNode.image;
            }
        }
        if (result == null) {
            if (isVariable && !isTernaryProtected(node)
                    // variable unknow in context and not (from) a register
                    && !(context.has(variableName.toString())
                    || (numChildren == 1
                        && node.jjtGetChild(0) instanceof ASTIdentifier
                        && ((ASTIdentifier) node.jjtGetChild(0)).getRegister() >= 0))) {
                JexlException xjexl = propertyName != null
                                      ? new JexlException.Property(node, variableName.toString())
                                      : new JexlException.Variable(node, variableName.toString());
                return unknownVariable(xjexl);
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTReferenceExpression node, Object data) {
        ASTArrayAccess upper = node;
        return visit(upper, data);
    }

    @Override
    protected Object visit(ASTReturnStatement node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        throw new JexlException.Return(node, null, val);
    }

    /**
     * Check if a null evaluated expression is protected by a ternary expression.
     * The rationale is that the ternary / elvis expressions are meant for the user to explictly take
     * control over the error generation; ie, ternaries can return null even if the engine in isStrict mode
     * would normally throw an exception.
     * @param node the expression node
     * @return true if nullable variable, false otherwise
     */
    private boolean isTernaryProtected(JexlNode node) {
        for (JexlNode walk = node.jjtGetParent(); walk != null; walk = walk.jjtGetParent()) {
            if (walk instanceof ASTTernaryNode) {
                return true;
            } else if (!(walk instanceof ASTReference || walk instanceof ASTArrayAccess)) {
                break;
            }
        }
        return false;
    }

    @Override
    protected Object visit(ASTSizeFunction node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        if (val == null) {
            throw new JexlException(node, "size() : argument is null", null);
        }
        return Integer.valueOf(sizeOf(node, val));
    }

    @Override
    protected Object visit(ASTSizeMethod node, Object data) {
        return Integer.valueOf(sizeOf(node, data));
    }

    @Override
    protected Object visit(ASTStringLiteral node, Object data) {
        if (data != null) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.image;
    }

    @Override
    protected Object visit(ASTTernaryNode node, Object data) {
        Object condition = node.jjtGetChild(0).jjtAccept(this, data);
        if (node.jjtGetNumChildren() == 3) {
            if (condition != null && arithmetic.toBoolean(condition)) {
                return node.jjtGetChild(1).jjtAccept(this, data);
            } else {
                return node.jjtGetChild(2).jjtAccept(this, data);
            }
        }
        if (condition != null && arithmetic.toBoolean(condition)) {
            return condition;
        } else {
            return node.jjtGetChild(1).jjtAccept(this, data);
        }
    }

    @Override
    protected Object visit(ASTTrueNode node, Object data) {
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTUnaryMinusNode node, Object data) {
        JexlNode valNode = node.jjtGetChild(0);
        Object val = valNode.jjtAccept(this, data);
        try {
            Object number = arithmetic.negate(val);
            // attempt to recoerce to literal class
            if (valNode instanceof ASTNumberLiteral && number instanceof Number) {
                number = arithmetic.narrowNumber((Number) number, ((ASTNumberLiteral) valNode).getLiteralClass());
            }
            return number;
        } catch (ArithmeticException xrt) {
            throw new JexlException(valNode, "arithmetic error", xrt);
        }
    }

    @Override
    protected Object visit(ASTWhileStatement node, Object data) {
        Object result = null;
        /* first objectNode is the expression */
        Node expressionNode = node.jjtGetChild(0);
        while (arithmetic.toBoolean(expressionNode.jjtAccept(this, data))) {
            if (isCancelled()) {
                throw new JexlException.Cancel(node);
            }
            // execute statement
            if (node.jjtGetNumChildren() > 1) {
                result = node.jjtGetChild(1).jjtAccept(this, data);
            }
        }
        return result;
    }

    /**
     * Calculate the <code>size</code> of various types: Collection, Array,
     * Map, String, and anything that has a int size() method.
     * @param node the node that gave the value to size
     * @param val the object to get the size of.
     * @return the size of val
     */
    private int sizeOf(JexlNode node, Object val) {
        if (val instanceof Collection<?>) {
            return ((Collection<?>) val).size();
        } else if (val.getClass().isArray()) {
            return Array.getLength(val);
        } else if (val instanceof Map<?, ?>) {
            return ((Map<?, ?>) val).size();
        } else if (val instanceof String) {
            return ((String) val).length();
        } else {
            // check if there is a size method on the object that returns an
            // integer and if so, just use it
            Object[] params = new Object[0];
            JexlMethod vm = uberspect.getMethod(val, "size", EMPTY_PARAMS);
            if (vm != null && vm.getReturnType() == Integer.TYPE) {
                Integer result;
                try {
                    result = (Integer) vm.invoke(val, params);
                } catch (Exception e) {
                    throw new JexlException(node, "size() : error executing", e);
                }
                return result.intValue();
            }
            throw new JexlException(node, "size() : unsupported type : " + val.getClass(), null);
        }
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @return the attribute value
     */
    public Object getAttribute(Object object, Object attribute) {
        return getAttribute(object, attribute, null);
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @param node the node that evaluated as the object
     * @return the attribute value
     */
    protected Object getAttribute(Object object, Object attribute, JexlNode node) {
        if (object == null) {
            throw new JexlException(node, "object is null");
        }
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // attempt to reuse last executor cached in volatile JexlNode.value
        if (node != null && cache) {
            Object cached = node.jjtGetValue();
            if (cached instanceof JexlPropertyGet) {
                JexlPropertyGet vg = (JexlPropertyGet) cached;
                Object value = vg.tryInvoke(object, attribute);
                if (!vg.tryFailed(value)) {
                    return value;
                }
            }
        }
        JexlPropertyGet vg = uberspect.getPropertyGet(object, attribute);
        if (vg != null) {
            try {
                Object value = vg.invoke(object);
                // cache executor in volatile JexlNode.value
                if (node != null && cache && vg.isCacheable()) {
                    node.jjtSetValue(vg);
                }
                return value;
            } catch (Exception xany) {
                String attrStr = attribute != null ? attribute.toString() : null;
                JexlException xjexl = new JexlException.Property(node, attrStr, xany);
                if (strictArithmetic) {
                    throw xjexl;
                }
                if (!silent) {
                    logger.warn(xjexl.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @param value the value to assign to the object's attribute
     */
    public void setAttribute(Object object, Object attribute, Object value) {
        setAttribute(object, attribute, value, null);
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @param value the value to assign to the object's attribute
     * @param node the node that evaluated as the object
     */
    protected void setAttribute(Object object, Object attribute, Object value, JexlNode node) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // attempt to reuse last executor cached in volatile JexlNode.value
        if (node != null && cache) {
            Object cached = node.jjtGetValue();
            if (cached instanceof JexlPropertySet) {
                JexlPropertySet setter = (JexlPropertySet) cached;
                Object eval = setter.tryInvoke(object, attribute, value);
                if (!setter.tryFailed(eval)) {
                    return;
                }
            }
        }
        JexlException xjexl = null;
        JexlPropertySet vs = uberspect.getPropertySet(object, attribute, value);
        // if we can't find an exact match, narrow the value argument and try again
        if (vs == null) {
            // replace all numbers with the smallest type that will fit
            Object[] narrow = {value};
            if (arithmetic.narrowArguments(narrow)) {
                vs = uberspect.getPropertySet(object, attribute, narrow[0]);
            }
        }
        if (vs != null) {
            try {
                // cache executor in volatile JexlNode.value
                vs.invoke(object, value);
                if (node != null && cache && vs.isCacheable()) {
                    node.jjtSetValue(vs);
                }
                return;
            } catch (RuntimeException xrt) {
                if (node == null) {
                    throw xrt;
                }
                xjexl = new JexlException(node, "set object property error", xrt);
            } catch (Exception xany) {
                if (node == null) {
                    throw new RuntimeException(xany);
                }
                xjexl = new JexlException(node, "set object property error", xany);
            }
        }
        if (xjexl == null) {
            if (node == null) {
                String error = "unable to set object property"
                        + ", class: " + object.getClass().getName()
                        + ", property: " + attribute
                        + ", argument: " + value.getClass().getSimpleName();
                throw new UnsupportedOperationException(error);
            }
            xjexl = new JexlException.Property(node, attribute.toString());
        }
        if (strictArithmetic) {
            throw xjexl;
        }
        if (!silent) {
            logger.warn(xjexl.getMessage());
        }
    }
}