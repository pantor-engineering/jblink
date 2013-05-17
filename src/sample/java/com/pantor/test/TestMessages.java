// Copyright (c), Pantor Engineering AB, 2013-
// All rights reserved

package com.pantor.test;

public class TestMessages
{
   public static class Pong
   {
      public Pong () { }
      public Pong (int msg) { value = msg; } 
      public void setValue (int msg) { value = msg; }
      public int getValue () { return value; }

      private int value;
   }

   public static class Ping
   {
      public Ping () { }
      public Ping (int msg) { value = msg; } 
      public void setValue (int msg) { value = msg; }
      public int getValue () { return value; }

      private int value;
   }
}
