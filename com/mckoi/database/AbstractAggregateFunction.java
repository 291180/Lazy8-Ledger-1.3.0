/**
 * com.mckoi.database.AbstractAggregateFunction  06 Aug 2000
 *
 * Mckoi SQL Database ( http://www.mckoi.com/database )
 * Copyright (C) 2000, 2001  Diehl and Associates, Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * Version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License Version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * Version 2 along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Change Log:
 * 
 * 
 */

package com.mckoi.database;

/**
 * Provides convenience methods for handling aggregate functions (functions
 * that are evaluated over a grouping set).  Note that this class handles the
 * most common form of aggregate functions.  These are aggregates with no more
 * or no less than one parameter, and that return NULL if the group set has a
 * length of 0.  If an aggregate function doesn't fit this design, then the
 * developer must roll their own AbstractFunction to handle it.
 * <p>
 * This object handles full expressions being passed as parameters to the
 * aggregate function.  The expression is evaluated for each set in the
 * group.  Therefore the aggregate function, avg(length(description)) will
 * find the average length of the description column.  sum(price * quantity)
 * will find the sum of the price * quantity of each set in the group.
 *
 * @author Tobias Downer
 */

public abstract class AbstractAggregateFunction extends AbstractFunction {

  /**
   * Constructs an aggregate function.
   */
  public AbstractAggregateFunction(String name, Expression[] params) {
    super(name, params);
    setAggregate(true);

    // Aggregates must have only one argument
    if (parameterCount() != 1) {
      throw new Error("'" + name + "' function must have one argument.");
    }

  }

  // ---------- Abstract ----------

  /**
   * Evaluates the aggregate function for the given values and returns the
   * result.  If this aggregate was 'sum' then this method would sum the two
   * values.  If this aggregate was 'avg' then this method would also sum the
   * two values and the 'postEvalAggregate' would divide by the number
   * processed.
   * <p>
   * NOTE: This first time this method is called on a set, 'val1' is 'null'.
   */
  public abstract Object evalAggregate(GroupResolver group,
                                       QueryContext context,
                                       Object val1, Object val2);

  /**
   * Called just before the value is returned to the parent.  This does any
   * final processing on the result before it is returned.  If this aggregate
   * was 'avg' then we'd divide by the size of the group.
   */
  public Object postEvalAggregate(GroupResolver group,
                                  QueryContext context,
                                  Object result) {
    // By default, do nothing....
    return result;
  }



  // ---------- Implemented from AbstractFunction ----------

  public final Object evaluate(GroupResolver group,
                               VariableResolver resolver,
                               QueryContext context) {
    if (group == null) {
      throw new Error("'" + getName() +
                      "' can only be used as an aggregate function.");
    }

    Object result = null;
    // All aggregates functions return 'null' if group size is 0
    int size = group.size();
    if (size == 0) {
      return Expression.NULL_OBJ;
    }

    Object val;
    Variable v = getParameter(0).getVariable();
    // If the aggregate parameter is a simple variable, then use optimal
    // routine,
    if (v != null) {
      for (int i = 0; i < size; ++i) {
        val = group.resolve(v, i);
        if (val != null) {
          result = evalAggregate(group, context, result, val);
        }
      }
    }
    else {
      // Otherwise we must resolve the expression for each entry in group,
      // This allows for expressions such as 'sum(quantity * price)' to
      // work for a group.
      Expression exp = getParameter(0);
      for (int i = 0; i < size; ++i) {
        val = exp.evaluate(null, group.getVariableResolver(i), context);
        if (val != null) {
          result = evalAggregate(group, context, result, val);
        }
      }
    }

    // Post method.
    result = postEvalAggregate(group, context, result);

    return result;
  }

}
