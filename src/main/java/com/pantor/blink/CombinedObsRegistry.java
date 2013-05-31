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
import java.util.Arrays;
import java.util.ArrayList;

/**
   The {@code CombinedObsRegistry} implements an observer registry
   that is a combination of a list of subregistries. When looking up
   an observer, the list of registries is searched to find the most
   specific match to the type used as key. A matching observer <i>O1</i> is
   more specific than another matching obsever <i>02</i>, if the type
   associated with <i>01</i> is a subclass to the type associated with
   <i>02</i>.
   <p>
   If there are multiple matching observers that are equally
   specific, the observer from the registry added first to the list of
   registries is selected.
   <p>
   If no matching observer is found in the list of registries, a fallback
   observer is returned if it has been set.
*/

public final class CombinedObsRegistry implements ObserverRegistry
{
   /**
      Creates a combined observer registry

      @param om an object model
      @param oregs zero or more subregistries
    */
   
   public CombinedObsRegistry (ObjectModel om, ObserverRegistry... oregs)
   {
      this.om = om;
      this.oregs.addAll (Arrays.asList (oregs));
   }

   /**
      Adds one or more registries

      @param oregs one or more subregistries
    */
   
   public void addRegistry (ObserverRegistry... oregs)
   {
      this.oregs.addAll (Arrays.asList (oregs));
      byClass.clear ();
      byName.clear ();
      byClassDirect.clear ();
      byNameDirect.clear ();
   }

   /**
      Sets a fallback observer
    */

   public void setFallbackObserver (Observer fallback)
   {
      this.fallback = fallback;
      byClass.clear ();
      byName.clear ();
      byClassDirect.clear ();
      byNameDirect.clear ();
   }
   
   @Override
   public Observer findObserver (Class<?> type) throws BlinkException
   {
      Observer o = byClass.get (type);
      if (o == null)
      {
         ObjectModel.GroupBinding bnd = om.getGroupBinding (type);
         if (bnd != null)
            o = findObserver (bnd.getGroup ());
         byClass.put (type, o);
      }
      return o;
   }

   @Override
   public Observer findObserver (Schema.Group g) throws BlinkException

   {
      NsName n = g.getName ();
      Observer o = byName.get (n);
      if (o == null)
      {
         o = lookupPoly (g);
         byName.put (n, o);
      }
      return o;
   }

   @Override
   public Observer findDirectObserver (Class<?> type) throws BlinkException
   {
      Observer o = byClassDirect.get (type);
      if (o == null)
      {
         ObjectModel.GroupBinding bnd = om.getGroupBinding (type);
         if (bnd != null)
            o = findDirectObserver (bnd.getGroup ());
         byClassDirect.put (type, o);
      }
      return o;
   }

   @Override
   public Observer findDirectObserver (Schema.Group g) throws BlinkException

   {
      NsName n = g.getName ();
      Observer o = byNameDirect.get (n);
      if (o == null)
      {
         o = lookupDirect (g);
         byNameDirect.put (n, o);
      }
      return o;
   }

   @Override
   public Observer getFallbackObserver () throws BlinkException
   {
      for (ObserverRegistry oreg : oregs)
      {
         Observer o = oreg.getFallbackObserver ();
         if (o != null)
            return o;
      }

      return fallback;
   }

   private Observer lookupPoly (Schema.Group g) throws BlinkException
   {
      // Do a breath first search among registries for matches of g or
      // any of its ancestors
      
      Observer o = null;

     POLY:
      for (Schema.Group candidate = g; candidate != null;
           candidate = candidate.getSuperGroup ())
      {
         for (ObserverRegistry oreg : oregs)
         {
            o = oreg.findDirectObserver (candidate);
            if (o != null)
               break POLY;
         }
      }

      if (o == null)
         // Look for the first fallback in the registries
         for (ObserverRegistry oreg : oregs)
         {
            o = oreg.getFallbackObserver ();
            if (o != null)
               break;
         }

      // as a final resort, use any fallback defined directly on this
      // registry
      
      if (o == null)
         o = fallback;
      
      return o;
   }

   private Observer lookupDirect (Schema.Group g) throws BlinkException
   {
      for (ObserverRegistry oreg : oregs)
      {
         Observer o = oreg.findDirectObserver (g);
         if (o != null)
            return o;
      }

      return null;
   }

   private final ObjectModel om;
   private final HashMap<Class<?>, Observer> byClass =
      new HashMap<Class<?>, Observer> ();
   private final HashMap<NsName, Observer> byName =
      new HashMap<NsName, Observer> ();
   private final HashMap<Class<?>, Observer> byClassDirect =
      new HashMap<Class<?>, Observer> ();
   private final HashMap<NsName, Observer> byNameDirect =
      new HashMap<NsName, Observer> ();
   private final ArrayList<ObserverRegistry> oregs =
      new ArrayList<ObserverRegistry> ();
   private Observer fallback;
}
