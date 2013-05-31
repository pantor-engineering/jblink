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

import java.util.HashMap;

/**
   The {@code Dispatcher} class provides functionality for dispatching
   objects to observers obtaind from a list of observer registries.
   <p>
   The dispatcher looks up an observer in the list of registries based
   on the class of the object to dispatch. The process of matching the
   class to an observer is handled by a {@link CombinedObsRegistry}
   managed by the dispatcher. At most one observer will receive an
   object that is dispatched.
 */

public final class Dispatcher
{
   /**
      Creates a dispacher that can dispatch types that are members of
      the specified object model. The constructor takes zero or more
      registries as arguments.

      @param om an object model
      @param oregs zero or more observer registries
   */
   
   public Dispatcher (ObjectModel om, ObserverRegistry... oregs)
   {
      this.om = om;
      this.oreg = new CombinedObsRegistry (om, oregs);
   }

   /**
      Adds registries of observers to be dispatched to

      @param oregs one or more registries to add
    */
   
   public void addRegistry (ObserverRegistry... oregs)
   {
      oreg.addRegistry (oregs);
      omap.clear ();
   }

   /**
      Sets an observer that will be used if no matching observer
      is found in the observer registries
    */
   
   public void setFallbackObserver (Observer o)
   {
      oreg.setFallbackObserver (o);
      omap.clear ();
   }

   /**
      Dispatches an object to the first observer that matches the
      class of the object. The class must be a member of the object
      model specified in the constructor of this dispatcher. If no
      matching observer is found and no fallback observer is
      available, the object will not be dispached.

      @param o an object to dispatch
      @return {@code true} if the object was dispatched, otherwise
      {@code false}

      @throws BlinkException if the class of the object is not
      a member of the object model
   */
   
   public boolean dispatch (Object o) throws BlinkException
   {
      Class<?> c = o.getClass ();
      Context cx = omap.get (c);
      if (cx == null)
      {
         Observer obs = oreg.findObserver (c);
         Schema.Group grp = om.getGroupBinding (c).getGroup ();
         if (obs != null)
            cx = new Context (obs, grp);
      }
      
      if (cx != null)
      {
         cx.dispatch (o);
         return true;
      }
      else
         return false;
   }

   private final static class Context
   {
      Context (Observer obs, Schema.Group grp)
      {
         this.obs = obs;
         this.grp = grp;
      }

      void dispatch (Object obj) { obs.onObj (obj, grp); }
      
      Observer obs;
      Schema.Group grp;
   }

   private final ObjectModel om;
   private final CombinedObsRegistry oreg;
   private final HashMap<Class<?>, Context> omap =
      new HashMap<Class<?>, Context> ();
}
