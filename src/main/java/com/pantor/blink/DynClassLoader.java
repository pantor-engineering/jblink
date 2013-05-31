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

import java.lang.reflect.Method;

public final class DynClassLoader extends ClassLoader
{
   public DynClassLoader ()
   {
      try
      {
         defc = ClassLoader.class.getDeclaredMethod (
            "defineClass", String.class, byte[].class, int.class, int.class);
         defc.setAccessible (true);
      }
      catch (Exception e)
      {
         throw new RuntimeException (e); // FIXME
      }
   }

   public Class<?> load (DynClass dc)
   {
      byte [] b = dc.render ();
      if (false)
         dump (dc, b);
      
      return defineClass (dc.getName (), b, 0, b.length);
   }

   public Class<?> loadPrivileged (DynClass dc, Class <?> scope)
   {
      byte [] b = dc.render ();
      if (false)
         dump (dc, b);

      try
      {
         // Try to load class with the loader of the specified scope
         ClassLoader priv = scope.getClassLoader ();
         return (Class<?>)defc.invoke (priv, dc.getName (), b, 0, b.length);
      }
      catch (Exception ignored)
      {
         // Fallback to non-privileged loading
         return defineClass (dc.getName (), b, 0, b.length);
      }   
   }

   private void dump (DynClass dc, byte [] b)
   {
      try
      {
         java.io.FileOutputStream f =
            new java.io.FileOutputStream (dc.getName () + ".class");
         f.write (b);
         f.close ();
      }
      catch (Exception e)
      {
      }
   }

   private final Method defc;
}
