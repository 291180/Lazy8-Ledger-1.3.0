/**
 * com.mckoi.database.DatabaseDispatcher  19 Aug 2000
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

import java.util.*;
import com.mckoi.debug.*;

/**
 * This is the database system dispatcher thread.  This is a thread that
 * runs in the background servicing delayed events.  This thread serves a
 * number of purposes.  It can be used to perform optimizations/clean ups in
 * the background (similar to hotspot).  It could be used to pause until
 * sufficient information has been collected or there is a lull in
 * work before it does a query in the background.  For example, if a VIEW
 * is invalidated because the underlying data changes, then we can wait
 * until the data has finished updating, then perform the view query to
 * update it correctly.
 *
 * @author Tobias Downer
 */

class DatabaseDispatcher extends Thread {

  private ArrayList event_queue = new ArrayList();

  private TransactionSystem system;

  /**
   * NOTE: Constructing this object will start the thread.
   */
  DatabaseDispatcher(TransactionSystem system) {
    this.system = system;
    setDaemon(true);
    start();
  }

  /**
   * Creates an event object that is passed into 'addEventToDispatch' method
   * to run the given Runnable method after the time has passed.
   * <p>
   * The event created here can be safely posted on the event queue as many
   * times as you like.  It's useful to create an event as a persistant object
   * to service some event.  Just post it on the dispatcher when you want
   * it run!
   */
  Object createEvent(Runnable runnable) {
    return new DatabaseEvent(runnable);
  }

  /**
   * Adds a new event to be dispatched on the queue after 'time_to_wait'
   * milliseconds has passed.
   */
  synchronized void postEvent(int time_to_wait, Object event) {
    DatabaseEvent evt = (DatabaseEvent) event;
    // Remove this event from the queue,
    event_queue.remove(event);
    // Set the correct time for the event.
    evt.time_to_run_event = System.currentTimeMillis() + time_to_wait;
    // Add to the queue in correct order
    int index = Collections.binarySearch(event_queue, event);
    if (index < 0) {
      index = -(index + 1);
    }
    event_queue.add(index, event);

    notifyAll();
  }


  public void run() {
    while (true) {
      try {

        DatabaseEvent evt = null;
        synchronized (this) {
          while (evt == null) {
            if (event_queue.size() > 0) {
              // Get the top entry, do we execute it yet?
              evt = (DatabaseEvent) event_queue.get(0);
              long diff = evt.time_to_run_event - System.currentTimeMillis();
              // If we got to wait for the event then do so now...
              if (diff >= 0) {
                evt = null;
                wait((int) diff);
              }
            }
            else {
              // Event queue empty so wait for someone to put an event on it.
              wait();
            }
          }
          // Remove the top event from the list,
          event_queue.remove(0);
        }

        // 'evt' is our event to run,
        evt.runnable.run();

      }
      catch (Throwable e) {
        system.Debug().write(Lvl.ERROR, this, "SystemDispatchThread error");
        system.Debug().writeException(e);
      }
    }
  }

  // ---------- Inner classes ----------

  class DatabaseEvent implements Comparable {
    private long     time_to_run_event;
    private Runnable runnable;

    DatabaseEvent(Runnable runnable) {
      this.runnable = runnable;
    }

    public int compareTo(Object ob) {
      DatabaseEvent evt2 = (DatabaseEvent) ob;
      long dif = time_to_run_event - evt2.time_to_run_event;
      if (dif > 0) {
        return 1;
      }
      else if (dif < 0) {
        return -1;
      }
      return 0;
    }
  }


}
