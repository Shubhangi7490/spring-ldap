/*
 * Copyright 2005-2016 the original author or authors.
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

package org.springframework.ldap.repository.support;

import com.querydsl.core.types.Constant;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.ParamExpression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.TemplateExpression;
import com.querydsl.core.types.Visitor;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.GreaterThanOrEqualsFilter;
import org.springframework.ldap.filter.LessThanOrEqualsFilter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.NotFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.ldap.filter.PresentFilter;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;

/**
 * Helper class for generating LDAP filters from QueryDSL Expressions.
 *
 * @author Mattias Hellborg Arthursson
 * @author Eddu Melendez
 * @since 2.0
 */
class LdapSerializer implements Visitor<Object, Void> {

    private final ObjectDirectoryMapper odm;
    private final Class<?> clazz;

    public LdapSerializer(ObjectDirectoryMapper odm, Class<?> clazz) {
        this.odm = odm;
        this.clazz = clazz;
    }

    public Filter handle(Expression<?> expression) {
        return (Filter) expression.accept(this, null);
    }

    @Override
    public Object visit(Constant<?> expr, Void context) {
        return expr.getConstant().toString();
    }

    @Override
    public Object visit(FactoryExpression<?> expr, Void context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Operation<?> expr, Void context) {
        Operator operator = expr.getOperator();
        if (operator == Ops.EQ) {
            return new EqualsFilter(attribute(expr), value(expr));
        } else if (operator == Ops.AND) {
            return new AndFilter()
                    .and(handle(expr.getArg(0)))
                    .and(handle(expr.getArg(1)));
        } else if (operator == Ops.OR) {
            return new OrFilter()
                    .or(handle(expr.getArg(0)))
                    .or(handle(expr.getArg(1)));
        } else if (operator == Ops.NOT) {
            return new NotFilter(handle(expr.getArg(0)));
        } else if (operator == Ops.LIKE) {
            return new LikeFilter(attribute(expr), value(expr));
        } else if (operator == Ops.STARTS_WITH || operator == Ops.STARTS_WITH_IC) {
            return new LikeFilter(attribute(expr), value(expr) + "*");
        } else if (operator == Ops.ENDS_WITH || operator == Ops.ENDS_WITH_IC) {
            return new LikeFilter(attribute(expr), "*" + value(expr));
        } else if (operator == Ops.STRING_CONTAINS || operator == Ops.STRING_CONTAINS_IC) {
            return new LikeFilter(attribute(expr), "*" + value(expr) + "*");
        } else if (operator == Ops.IS_NOT_NULL) {
            return new PresentFilter(attribute(expr));
        } else if (operator == Ops.IS_NULL) {
            return new NotFilter(new PresentFilter(attribute(expr)));
        } else if (operator == Ops.GOE) {
            return new GreaterThanOrEqualsFilter(attribute(expr), value(expr));
        } else if (operator == Ops.LOE) {
            return new LessThanOrEqualsFilter(attribute(expr), value(expr));
        }


        throw new UnsupportedOperationException("Unsupported operator " + operator.toString());
    }

    private String value(Operation<?> expr) {
        return (String) expr.getArg(1).accept(this, null);
    }

    private String attribute(Operation<?> expr) {
        return odm.attributeFor(clazz, (String) expr.getArg(0).accept(this, null));
    }

    @Override
    public Object visit(ParamExpression<?> expr, Void context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Path<?> expr, Void context) {
        return expr.getMetadata().getName();
    }

    @Override
    public Object visit(SubQueryExpression<?> expr, Void context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(TemplateExpression<?> expr, Void context) {
        throw new UnsupportedOperationException();
    }
}
