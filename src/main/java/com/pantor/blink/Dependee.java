// Copyright (c) 2015, Pantor Engineering AB
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

import java.lang.ref.WeakReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;

public interface Dependee
{
   public static class Impl implements Dependee
   {
      @Override
      public Impl getDependeeImpl ()
      {
         return this;
      }

      public final void notifyAllDependents ()
         throws BlinkException
      {
         for (WeakReference<Dependent> ref : deps)
         {
            Dependent dep = ref.get ();
            if (dep != null)
               dep.onDependeeChanged ();
         }
      }

      public final void addDependent (Dependent dep)
      {
         clean ();
         deps.add (new WeakReference<Dependent> (dep, refQueue));
      }
   
      public final void removeDependent (Dependent dep)
      {
         clean ();
         for (WeakReference<Dependent> ref : deps)
            if (ref.get () == dep)
            {
               deps.remove (ref);
               break;
            }
      }
   
      private final void clean ()
      {
         for (;;)
         {
            Reference<? extends Dependent> ref = refQueue.poll ();
            if (ref == null)
               return;
            deps.remove (ref);
         }
      }

      private final ReferenceQueue<Dependent> refQueue =
         new ReferenceQueue<Dependent> ();
      private final HashSet<WeakReference<Dependent>> deps =
         new HashSet<WeakReference<Dependent>>  ();
   }

   public Impl getDependeeImpl ();
}
