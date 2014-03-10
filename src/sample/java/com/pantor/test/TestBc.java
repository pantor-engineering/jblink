// Copyright (c), Pantor Engineering AB, 2013-
// All rights reserved

package com.pantor.test;

import com.pantor.blink.DefaultObjectModel;
import com.pantor.blink.Broadcaster;
import java.util.Timer;
import java.util.TimerTask;

public class TestBc extends TimerTask
{
   public TestBc (Broadcaster bc) { this.bc = bc; }

   public void run ()
   {
      System.err.printf ("Sending ping (1)%n");
      try
      {
         bc.send (new TestMessages.Ping (1));
      }
      catch (Exception e)
      {
         e.printStackTrace ();
      }
   }
   
   public static void main (String... args) throws Exception
   {
      DefaultObjectModel om = new DefaultObjectModel (args [0]);
      om.setWrapper (TestMessages.class);
      Broadcaster bc = new Broadcaster (args [1], om);
      TestBc tbc = new TestBc (bc);
      int t = 1000;
      System.err.println ("Start broadcasting");
      timer.schedule (tbc, t, t);
   }

   private final Broadcaster bc;
   private static Timer timer = new Timer ("TestBc", false /* no daemon */);
}
