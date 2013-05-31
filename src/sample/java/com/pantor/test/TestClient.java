// Copyright (c), Pantor Engineering AB, 2013-
// All rights reserved

package com.pantor.test;

import com.pantor.blink.DefaultObjectModel;
import com.pantor.blink.Client;
import com.pantor.blink.Schema;

public class TestClient
{
   public TestClient (Client c) { this.c = c; }

   public void onPong (TestMessages.Pong pong)
   {
      try
      {
         int val = pong.getValue ();
         System.err.printf ("Got pong (%d), sending ping (%d)%n", val, val + 1);
         Thread.sleep (1000);
         c.send (new TestMessages.Ping (val + 1));
      }
      catch (Throwable e)
      {
         while (e.getCause () != null)
            e = e.getCause ();
         System.err.println (e);
      }
   }
   
   public static void main (String... args) throws Exception
   {
      DefaultObjectModel om = new DefaultObjectModel (args [0]);
      om.setWrapper (TestMessages.class);
      Client c = new Client (args [1], om);
      TestClient obs = new TestClient (c);
      c.addObserver (obs);
      c.start ();
      System.err.printf ("Sending ping (1)%n");
      c.send (new TestMessages.Ping (1));
   }

   private final Client c;
}
