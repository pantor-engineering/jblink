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

public final class NsName
{
   public String getNs () { return ns; }
   public String getName () { return name; }

   @Override
   public boolean equals (Object o)
   {
      return this == o;
   }

   @Override
   public int hashCode ()
   {
      return (ns.hashCode () * 37) ^ name.hashCode ();
   }

   public static NsName get (String ns, String name)
   {
      Pool local_ = local.get ();
      NsName nm = local_.get (ns, name);
      if (nm == null)
      {
         nm = global.put (ns, name);
         local_.cache (nm);
      }
      return nm;
   }
   
   public static NsName get (String name)
   {
      return get ("", name);
   }

   public static NsName parse (String lit)
   {
      String [] parts = lit.split (":");
      if (parts.length == 1)
         return get (parts [0]);
      else if (parts.length == 2)
         return get (parts [0], parts [1]);
      else
         return null;
   }

   @Override
   public String toString ()
   {
      if (ns.equals (""))
         return name;
      else
         return ns + ":" + name;
   }

   public boolean isQualified () { return ! ns.equals (""); }
   
   private NsName (String ns, String name)
   {
      this.ns = ns;
      this.name = name;
   }
   
   private static class Ns extends HashMap <String, NsName> { }
   
   private static class Pool
   {
      synchronized NsName put (String ns, String name)
      {
         Ns nsMap = namespaces.get (ns);
         if (nsMap == null)
            namespaces.put (ns, nsMap = new Ns ());
         NsName nm = nsMap.get (name);
         if (nm == null)
            nsMap.put (name, nm = new NsName (ns, name));
         return nm;
      }

      NsName get (String ns, String name)
      {
         Ns nsMap = namespaces.get (ns);
         if (nsMap != null)
            return nsMap.get (name);
         else
            return null;
      }

      void cache (NsName name)
      {
         Ns nsMap = namespaces.get (name.getNs ());
         if (nsMap == null)
            namespaces.put (name.getNs (), nsMap = new Ns ());
         nsMap.put (name.getName (), name);
      }
      
      private final HashMap <String, Ns> namespaces =
         new HashMap <String, Ns> ();
   }
   
   private final String ns;
   private final String name;

   private static Pool global = new Pool ();
   
   private final static ThreadLocal<Pool> local = new ThreadLocal<Pool> () {
      @Override protected Pool initialValue () { return new Pool (); }
   };
}
