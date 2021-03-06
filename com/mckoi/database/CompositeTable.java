/**
 * com.mckoi.database.CompositeTable  28 Oct 2001
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

import com.mckoi.util.IntegerVector;

/**
 * A composite of two or more datasets used to implement UNION, INTERSECTION,
 * and DIFFERENCE.
 *
 * @author Tobias Downer
 */

public class CompositeTable extends Table implements RootTable {

  // ---------- Statics ----------

  /**
   * The composite function for finding the union of the tables.
   */
  public static int UNION = 1;

  /**
   * The composite function for finding the interestion of the tables.
   */
  public static int INTERSECT = 2;

  /**
   * The composite function for finding the difference of the tables.
   */
  public static int EXCEPT = 3;


  // ---------- Members ----------

  /**
   * The 'master table' used to resolve information about this table such as
   * fields and field types.
   */
  private Table master_table;

  /**
   * The tables being made a composite of.
   */
  private Table[] composite_tables;

  /**
   * The list of indexes of rows to include in each table.
   */
  private IntegerVector[] table_indexes;

  /**
   * The schemes to describe the entity relation in the given column.
   */
  private SelectableScheme[] column_scheme;

  /**
   * The number of root locks on this table.
   */
  private int roots_locked;

  /**
   * Constructs the composite table given the 'master_table' (the field
   * structure this composite dataset is based on), and a list of tables to
   * be the composite of this table.  The 'master_table' must be one of the
   * elements of the 'composite_list' array.
   * <p>
   * NOTE: This does not set up table indexes for a composite function.
   */
  public CompositeTable(Table master_table, Table[] composite_list) {
    super();
    this.master_table = master_table;
    this.composite_tables = composite_list;
    this.column_scheme = new SelectableScheme[master_table.getColumnCount()];
  }

  /**
   * Consturcts the composite table assuming the first item in the list is the
   * master table.
   */
  public CompositeTable(Table[] composite_list) {
    this(composite_list[0], composite_list);
  }


  /**
   * Removes duplicate rows from the table.  If 'pre_sorted' is true then each
   * composite index is already in sorted order.
   */
  private void removeDuplicates(boolean pre_sorted) {
    throw new Error("PENDING");
  }

  /**
   * Sets up the indexes in this composite table by performing for composite
   * function on the tables.  If the 'all' parameter is true then duplicate
   * rows are removed.
   */
  public void setupIndexesForCompositeFunction(int function, boolean all) {
    int size = composite_tables.length;
    table_indexes = new IntegerVector[size];

    if (function == UNION) {
      // Include all row sets in all tables
      for (int i = 0; i < size; ++i) {
        table_indexes[i] = composite_tables[i].selectAll();
      }
      if (!all) {
        removeDuplicates(false);
      }
    }
    else {
      throw new Error("Unrecognised composite function");
    }

  }

  // ---------- Implemented from Table ----------

  public Database getDatabase() {
    return master_table.getDatabase();
  }

  public int getColumnCount() {
    return master_table.getColumnCount();
  }

  public int getRowCount() {
    int row_count = 0;
    for (int i = 0; i < table_indexes.length; ++i) {
      row_count += table_indexes[i].size();
    }
    return row_count;
  }

  public TableField[] getFields() {
    return master_table.getFields();
  }

  public TableField getFieldAt(int column) {
    return master_table.getFieldAt(column);
  }

  /**
   * @deprecated
   */
  public int findFieldName(String name) {
    return master_table.findFieldName(name);
  }

  public int findFieldName(Variable v) {
    return master_table.findFieldName(v);
  }

  /**
   * @deprecated
   */
  public String getResolvedColumnName(int column) {
    return master_table.getResolvedColumnName(column);
  }

  public Variable getResolvedVariable(int column) {
    return master_table.getResolvedVariable(column);
  }

  SelectableScheme getSelectableSchemeFor(int column,
                                          int original_column, Table table) {

    SelectableScheme scheme = column_scheme[column];
    if (scheme == null) {
      scheme = new BlindSearch(this, column);
      column_scheme[column] = scheme;
    }

    // If we are getting a scheme for this table, simple return the information
    // from the column_trees Vector.
    if (table == this) {
      return scheme;
    }
    // Otherwise, get the scheme to calculate a subset of the given scheme.
    else {
      return scheme.getSubsetScheme(table, original_column);
    }
  }

  void setToRowTableDomain(int column, IntegerVector row_set,
                           TableDataSource ancestor) {
    if (ancestor != this) {
      throw new RuntimeException("Method routed to incorrect table ancestor.");
    }
  }

  RawTableInformation resolveToRawTable(RawTableInformation info) {
    System.err.println("Efficiency Warning in DataTable.resolveToRawTable.");
    IntegerVector row_set = new IntegerVector();
    RowEnumeration e = rowEnumeration();
    while (e.hasMoreRows()) {
      row_set.addInt(e.nextRowIndex());
    }
    info.add(this, row_set);
    return info;
  }

  public DataCell getCellContents(int column, int row) {
    for (int i = 0; i < table_indexes.length; ++i) {
      IntegerVector ivec = table_indexes[i];
      int sz = ivec.size();
      if (row < sz) {
        return composite_tables[i].getCellContents(column, ivec.intAt(row));
      }
      else {
        row -= sz;
      }
    }
    throw new Error("Row '" + row + "' out of bounds.");
  }

  public int compareCellTo(DataCell ob, int column, int row) {
    for (int i = 0; i < table_indexes.length; ++i) {
      IntegerVector ivec = table_indexes[i];
      int sz = ivec.size();
      if (row < sz) {
        return composite_tables[i].compareCellTo(ob, column, ivec.intAt(row));
      }
      else {
        row -= sz;
      }
    }
    throw new Error("Row '" + row + "' out of bounds.");
  }

  public RowEnumeration rowEnumeration() {
    return new SimpleRowEnumeration(getRowCount());
  }

  void addDataTableListener(DataTableListener listener) {
    for (int i = 0; i < composite_tables.length; ++i) {
      composite_tables[i].addDataTableListener(listener);
    }
  }

  void removeDataTableListener(DataTableListener listener) {
    for (int i = 0; i < composite_tables.length; ++i) {
      composite_tables[i].removeDataTableListener(listener);
    }
  }

  public void lockRoot(int lock_key) {
    // For each table, recurse.
    roots_locked++;
    for (int i = 0; i < composite_tables.length; ++i) {
      composite_tables[i].lockRoot(lock_key);
    }
  }

  public void unlockRoot(int lock_key) {
    // For each table, recurse.
    roots_locked--;
    for (int i = 0; i < composite_tables.length; ++i) {
      composite_tables[i].unlockRoot(lock_key);
    }
  }

  public boolean hasRootsLocked() {
    return roots_locked != 0;
  }

  // ---------- Implemented from RootTable ----------

  public boolean typeEquals(RootTable table) {
    return (this == table);
//    return true;
  }

}
