// Copyright (c) 2013, Pantor Engineering AB
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
//  * Redistributions in binary form must reproduce the above
//    copyright notice, this list of conditions and the following
//    disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
//  * Neither the name of Pantor Engineering AB nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
//
// IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
// OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
// USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
// DAMAGE.

package com.pantor.blink;

public interface Logger
{
   void fatal (String msg, Object... args);
   void fatal (Throwable e, String msg, Object... args);

   void error (String msg, Object... args);
   void error (Throwable e, String msg, Object... args);

   void warn (String msg, Object... args);
   void warn (Throwable e, String msg, Object... args);

   void info (String msg, Object... args);
   void info (Throwable e, String msg, Object... args);

   void debug (String msg, Object... args);
   void debug (Throwable e, String msg, Object... args);

   void trace (String msg, Object... args);
   void trace (Throwable e, String msg, Object... args);

   public enum Level
   {
      Off (0), Fatal (1), Error (2), Warn (3), Info (4), Debug (5), Trace (6),
         All (Integer.MAX_VALUE);

      public int cmp (Level other)
      {
         return val - other.val;
      }

      public int intValue ()
      {
         return val;
      }
      
      private Level (int val) { this.val = val; }
      private final int val;
   }

   boolean isActiveAtLevel (Level level);

   public interface Factory
   {
      Logger getLogger (String name);
      Logger getLogger (Class cl);
   }

   public class Manager
   {
      public static synchronized void setFactory (Factory fact)
      {
         Manager.fact = fact;
      }
      
      public static synchronized Logger getLogger (String name)
      {
         return fact.getLogger (name);
      }
      
      public static synchronized Logger getLogger (Class cl)
      {
         return fact.getLogger (cl);
      }

      private static Factory fact;
      
      static
      {
         Logger.Manager.setFactory (new DefaultLogger.Factory ());
      }
   }
}
