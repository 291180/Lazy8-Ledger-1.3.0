/*
 *  Copyright (C) 2002 Lazy Eight Data HB, Thomas Dilts This program is free
 *  software; you can redistribute it and/or modify it under the terms of the
 *  GNU General Public License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any later version. This
 *  program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details. You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA For more
 *  information, surf to www.lazy8.nu or email lazy8@telia.com
 */
package org.lazy8.nu.ledger.reports;

import java.util.*;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;

// Imports for picking up mouse events from the JTable.

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 *  Description of the Class
 *
 *@author     Lazy Eight Data HB, Thomas Dilts
 *@created    den 5 mars 2002
 */
public class TableSorter extends TableMap {
  int indexes[];
  Vector sortingColumns = new Vector();
  boolean ascending = true;
  int compares;

  /**
   *  Constructor for the TableSorter object
   */
  public TableSorter() {
    indexes = new int[0];
    // For consistency.
  }

  /**
   *  Constructor for the TableSorter object
   *
   *@param  model  Description of the Parameter
   */
  public TableSorter(TableModel model) {
    setModel(model);
  }

  /**
   *  Sets the model attribute of the TableSorter object
   *
   *@param  model  The new model value
   */
  public void setModel(TableModel model) {
    super.setModel(model);
    reallocateIndexes();
  }

  /**
   *  Description of the Method
   *
   *@param  row1    Description of the Parameter
   *@param  row2    Description of the Parameter
   *@param  column  Description of the Parameter
   *@return         Description of the Return Value
   */
  public int compareRowsByColumn(int row1, int row2, int column) {
    Class type = model.getColumnClass(column);
    TableModel data = model;

    // Check for nulls

    Object o1 = data.getValueAt(row1, column);
    Object o2 = data.getValueAt(row2, column);

    // If both values are null return 0
    if (o1 == null && o2 == null)
      return 0;
    else if (o1 == null)
      // Define null less than everything.
      return -1;
    else if (o2 == null)
      return 1;

    /*
     *  We copy all returned values from the getValue call in case
     *  an optimised model is reusing one object to return many values.
     *  The Number subclasses in the JDK are immutable and so will not be used in
     *  this way but other subclasses of Number might want to do this to save
     *  space and avoid unnecessary heap allocation.
     */
    if (type.getSuperclass() == java.lang.Number.class) {
      Number n1 = (Number) data.getValueAt(row1, column);
      double d1 = n1.doubleValue();
      Number n2 = (Number) data.getValueAt(row2, column);
      double d2 = n2.doubleValue();

      if (d1 < d2)
        return -1;
      else if (d1 > d2)
        return 1;
      else
        return 0;
    }
    else if (type == java.util.Date.class) {
      Date d1 = (Date) data.getValueAt(row1, column);
      long n1 = d1.getTime();
      Date d2 = (Date) data.getValueAt(row2, column);
      long n2 = d2.getTime();

      if (n1 < n2)
        return -1;
      else if (n1 > n2)
        return 1;
      else
        return 0;
    }
    else if (type == String.class) {
      String s1 = (String) data.getValueAt(row1, column);
      String s2 = (String) data.getValueAt(row2, column);
      int result = s1.compareTo(s2);

      //want blank strings to come last

      if (s1.length() == 0 || s2.length() == 0) {
        //(note rarely used exclusive or)
        if (s1.length() == 0 ^ s2.length() == 0) {
          if (s1.length() == 0)
            return 1;
          else
            return -1;
        }
        else
        //both strings are blank here
        //order them by the natural row numbers
            if (row1 > row2)
          return 1;
        else
          return -1;
      }

      if (result < 0)
        return -1;
      else if (result > 0)
        return 1;
      else
        return 0;
    }
    else if (type == Boolean.class) {
      Boolean bool1 = (Boolean) data.getValueAt(row1, column);
      boolean b1 = bool1.booleanValue();
      Boolean bool2 = (Boolean) data.getValueAt(row2, column);
      boolean b2 = bool2.booleanValue();

      if (b1 == b2)
        return 0;
      else if (b1)
        // Define false < true
        return 1;
      else
        return -1;
    }
    else {
      Object v1 = data.getValueAt(row1, column);
      String s1 = v1.toString();
      Object v2 = data.getValueAt(row2, column);
      String s2 = v2.toString();
      int result = s1.compareTo(s2);

      if (result < 0)
        return -1;
      else if (result > 0)
        return 1;
      else
        return 0;
    }
  }

  /**
   *  Description of the Method
   *
   *@param  row1  Description of the Parameter
   *@param  row2  Description of the Parameter
   *@return       Description of the Return Value
   */
  public int compare(int row1, int row2) {
    compares++;
    for (int level = 0; level < sortingColumns.size(); level++) {
      Integer column = (Integer) sortingColumns.elementAt(level);
      int result = compareRowsByColumn(row1, row2, column.intValue());
      if (result != 0)
        return ascending ? result : -result;
    }
    return 0;
  }

  /**
   *  Description of the Method
   */
  public void reallocateIndexes() {
    int rowCount = model.getRowCount();

    // Set up a new array of indexes with the right number of elements
    // for the new data model.
    indexes = new int[rowCount];

    // Initialise with the identity mapping.
    for (int row = 0; row < rowCount; row++)
      indexes[row] = row;
  }

  /**
   *  Description of the Method
   *
   *@param  e  Description of the Parameter
   */
  public void tableChanged(TableModelEvent e) {
    reallocateIndexes();

    super.tableChanged(e);
  }

  /**
   *  Description of the Method
   */
  public void checkModel() {
    /*
     *  if (indexes.length != model.getRowCount())
     *  {
     *  }
     */
  }

  /**
   *  Description of the Method
   *
   *@param  sender  Description of the Parameter
   */
  public void sort(Object sender) {
    checkModel();

    compares = 0;
    // n2sort();
    // qsort(0, indexes.length-1);
    shuttlesort((int[]) indexes.clone(), indexes, 0, indexes.length);
    //        SystemLog.ProblemPrintln("Compares: "+compares);
  }

  /**
   *  Description of the Method
   */
  public void n2sort() {
    for (int i = 0; i < getRowCount(); i++)
      for (int j = i + 1; j < getRowCount(); j++)
        if (compare(indexes[i], indexes[j]) == -1)
          swap(i, j);

  }


  /**
   *  This is a home-grown implementation which we have not had time to research
   *  - it may perform poorly in some circumstances. It requires twice the space
   *  of an in-place algorithm and makes NlogN assigments shuttling the values
   *  between the two arrays. The number of compares appears to vary between N-1
   *  and NlogN depending on the initial order but the main reason for using it
   *  here is that, unlike qsort, it is stable.
   *
   *@param  from  Description of the Parameter
   *@param  to    Description of the Parameter
   *@param  low   Description of the Parameter
   *@param  high  Description of the Parameter
   */
  public void shuttlesort(int from[], int to[], int low, int high) {
    if (high - low < 2)
      return;
    int middle = (low + high) / 2;
    shuttlesort(to, from, low, middle);
    shuttlesort(to, from, middle, high);

    int p = low;
    int q = middle;

    /*
     *  This is an optional short-cut; at each recursive call,
     *  check to see if the elements in this subset are already
     *  ordered.  If so, no further comparisons are needed; the
     *  sub-array can just be copied.  The array must be copied rather
     *  than assigned otherwise sister calls in the recursion might
     *  get out of sinc.  When the number of elements is three they
     *  are partitioned so that the first set, [low, mid), has one
     *  element and and the second, [mid, high), has two. We skip the
     *  optimisation when the number of elements is three or less as
     *  the first compare in the normal merge will produce the same
     *  sequence of steps. This optimisation seems to be worthwhile
     *  for partially ordered lists but some analysis is needed to
     *  find out how the performance drops to Nlog(N) as the initial
     *  order diminishes - it may drop very quickly.
     */
    if (high - low >= 4 && compare(from[middle - 1], from[middle]) <= 0) {
      for (int i = low; i < high; i++)
        to[i] = from[i];

      return;
    }

    // A normal merge.

    for (int i = low; i < high; i++)
      if (q >= high || (p < middle && compare(from[p], from[q]) <= 0))
        to[i] = from[p++];

      else
        to[i] = from[q++];

  }

  /**
   *  Description of the Method
   *
   *@param  i  Description of the Parameter
   *@param  j  Description of the Parameter
   */
  public void swap(int i, int j) {
    int tmp = indexes[i];
    indexes[i] = indexes[j];
    indexes[j] = tmp;
  }

  // The mapping only affects the contents of the data rows.
  // Pass all requests to these rows through the mapping array: "indexes".

  /**
   *  Gets the valueAt attribute of the TableSorter object
   *
   *@param  aRow     Description of the Parameter
   *@param  aColumn  Description of the Parameter
   *@return          The valueAt value
   */
  public Object getValueAt(int aRow, int aColumn) {
    checkModel();
    return model.getValueAt(indexes[aRow], aColumn);
  }

  /**
   *  Sets the valueAt attribute of the TableSorter object
   *
   *@param  aValue   The new valueAt value
   *@param  aRow     The new valueAt value
   *@param  aColumn  The new valueAt value
   */
  public void setValueAt(Object aValue, int aRow, int aColumn) {
    checkModel();
    model.setValueAt(aValue, indexes[aRow], aColumn);
  }

  /**
   *  Description of the Method
   *
   *@param  column  Description of the Parameter
   */
  public void sortByColumn(int column) {
    sortByColumn(column, true);
  }

  /**
   *  Description of the Method
   *
   *@param  column     Description of the Parameter
   *@param  ascending  Description of the Parameter
   */
  public void sortByColumn(int column, boolean ascending) {
    this.ascending = ascending;
    sortingColumns.removeAllElements();
    sortingColumns.addElement(new Integer(column));
    sort(this);
    super.tableChanged(new TableModelEvent(this));
  }

  /**
   *  There is no-where else to put this. Add a mouse listener to the Table to
   *  trigger a table sort when a column heading is clicked in the JTable.
   *
   *@param  table  The feature to be added to the MouseListenerToHeaderInTable
   *      attribute
   */
  public void addMouseListenerToHeaderInTable(JTable table) {
    final TableSorter sorter = this;
    final JTable tableView = table;
    tableView.setColumnSelectionAllowed(false);
    MouseAdapter listMouseListener =
      new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          TableColumnModel columnModel = tableView.getColumnModel();
          int viewColumn = columnModel.getColumnIndexAtX(e.getX());
          int column = tableView.convertColumnIndexToModel(viewColumn);
          if (e.getClickCount() == 1 && column != -1) {
            //                    SystemLog.ProblemPrintln("Sorting ...");
            int shiftPressed = e.getModifiers() & InputEvent.SHIFT_MASK;
            boolean ascending = (shiftPressed == 0);
            sorter.sortByColumn(column, ascending);
          }
        }
      };
    JTableHeader th = tableView.getTableHeader();
    th.addMouseListener(listMouseListener);
  }

}
