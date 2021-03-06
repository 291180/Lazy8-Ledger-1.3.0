/**
 * com.mckoi.database.ParameterSubstitution  09 Sep 2001
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
 * An object that represents a constant value that is to be lately binded to
 * a constant value in an Expression.  This is used when we have ? style
 * prepared statement values.  This object is used as a marker in the
 * elements of a expression.
 *
 * @author Tobias Downer
 */

public class ParameterSubstitution implements java.io.Serializable {

  /**
   * The numerical number of this parameter substitution.  The first
   * substitution is '0', the second is '1', etc.
   */
  private int parameter_id;

  /**
   * Creates the substitution.
   */
  public ParameterSubstitution(int parameter_id) {
    this.parameter_id = parameter_id;
  }

  /**
   * Returns the number of this parameter id.
   */
  public int getID() {
    return parameter_id;
  }

  /**
   * Equality test.
   */
  public boolean equals(Object ob) {
    ParameterSubstitution sub = (ParameterSubstitution) ob;
    return this.parameter_id == sub.parameter_id;
  }

}
