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
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.annotation.Annotation;

public final class DefaultObsRegistry implements ObserverRegistry 
{
   public DefaultObsRegistry (ObjectModel om)
   {
      this.om = om;
      if (om != null)
	 this.dload = new DynClassLoader ();
      else
	 this.dload = null;
   }

   public DefaultObsRegistry ()
   {
      this (null);
   }

   @Override
   public Observer findObserver (Class<?> type) throws BlinkException
   {
      if (om != null)
      {
	 ObjectModel.GroupBinding bnd = findGroupBinding (type);
	 if (bnd != null)
	    return findObserver (bnd.getGroup ());
      }

      return null;
   }

   @Override
   public Observer findObserver (Schema.Group g) throws BlinkException

   {
      Observer obs = obsByName.get (g.getName ());
      if (obs != null)
	 return obs;
      else
	 return findInAncestry (g.getSuperGroup ());
   }

   @Override
   public Observer findDirectObserver (Class<?> type) throws BlinkException
   {
      if (om != null)
      {
	 ObjectModel.GroupBinding bnd = findGroupBinding (type);
	 if (bnd != null)
	    return findDirectObserver (bnd.getGroup ());
      }

      return null;
   }

   @Override
   public Observer findDirectObserver (Schema.Group g) throws BlinkException

   {
      return obsByName.get (g.getName ());
   }

   @Override
   public Observer getFallbackObserver ()
   {
      return fallback;
   }
   
   public void addObserver (NsName name, Observer obs)
   {
      obsByName.put (name, obs);
   }
   
   public void addObserver (Object obs) throws BlinkException
   {
      addObserver (obs, "on");
   }

   public void addObserver (Object obs, String prefix) throws BlinkException
   {
      if (om != null)
      {
	 for (Method m : obs.getClass ().getMethods ())
	    if (m.getName ().startsWith (prefix))
	       addObserver (m, obs);
      }
      else
	 throw new RuntimeException ("DefaultObsRegistry: Cannot add " +
				     "dynamic observer if no data model is " +
				     "specified: " + obs.getClass ());
   }

   public void addObserver (Object obs, Class<? extends Annotation> annot)
      throws BlinkException
   {
      if (om != null)
      {
	 for (Method m : obs.getClass ().getMethods ())
	    if (m.isAnnotationPresent (annot))
	       if (! addObserver (m, obs))
		  throw new BlinkException.Binding (
		     "Observer method signature does not match any known " +
		     "blink type: " + m);
      }
      else
	 throw new RuntimeException ("DefaultObsRegistry: Cannot add " +
				     "dynamic observer if no data model is " +
				     "specified: " + obs.getClass ());
   }

   private boolean addObserver (Method m, Object obs) throws BlinkException
   {
      if (m.getReturnType () == void.class &&
	  ! Modifier.isStatic (m.getModifiers ()))
      {
	 m.setAccessible (true);
	 
	 Class<?> [] prms = m.getParameterTypes ();

	 if (prms.length == 1)
	 {
	    ObjectModel.GroupBinding bnd = findGroupBinding (prms [0]);
	    if (bnd != null)
	    {
	       obsByName.put (bnd.getGroup ().getName (),
			      createDynObs (m, obs, false));
	       return true;
	    }
	 }
	 else
	 {
	    if (prms.length == 2 && prms [1] == Schema.Group.class)
	    {
	       ObjectModel.GroupBinding bnd = findGroupBinding (prms [0]);
	       if (bnd != null)
	       {
		  obsByName.put (bnd.getGroup ().getName (),
				 createDynObs (m, obs, true));
		  return true;
	       }
	    }
	 }

	 // Test for fallback void onAny (Object o) or
	 // void onAny (Object o, Schema.Group g)
	    
	 if (prms.length > 0)
	 {
	    if (prms [0] == Object.class)
	    {
	       if (prms.length == 1)
	       {
		  fallback = createDynObs (m, obs, false);
		  return true;
	       }
	       else
	       {
		  if (prms.length == 2 && prms [1] == Schema.Group.class)
		  {
		     fallback = createDynObs (m, obs, true);
		     return true;
		  }
	       }
	    }
	 }
      }

      return false;
   }

   private ObjectModel.GroupBinding findGroupBinding (Class<?> c)
   {
      try
      {
	 return om.getGroupBinding (c);
      }
      catch (BlinkException e)
      {
	 return null;
      }
   }

   // public final class <T>+<method>_obs implements Observer
   // {
   //    <T>+<method>_obs (T obs)
   //    {
   //       this.obs = obs;
   //    }
   //
   //    public void onObj (Object o, Schema.Group g)
   //    {
   //       obs.<method> (o); // or obs.<method> (o, g)
   //    }
   //
   //    private final T obs;
   // }
   
   private Observer createDynObs (Method m, Object obs, boolean withGroup)
      throws BlinkException.Binding
   {
      Class c = obs.getClass ();
      String obsName = c.getName () + "+" + m.getName () + "_obs";
      String obsDescr = "L" + DynClass.toInternal (c) + ";";
      String onObjSig = "(Ljava/lang/Object;Lcom/pantor/blink/Schema$Group;)V";
      DynClass dc = new DynClass (obsName);
      dc.setFlags (DynClass.ClassFlag.Final);
      dc.addInterface ("com.pantor.blink.Observer");

      dc.addField ("obs", obsDescr, DynClass.FieldFlag.Private,
		   DynClass.FieldFlag.Final);
		   
      // Constructor

      dc.startPublicMethod ("<init>", "(" + obsDescr + ")V")
	 .aload0 ()
	 .invokeSpecial ("java.lang.Object", "<init>", "()V") // super ()
	 .aload0 ()
	 .aload1 ()
	 .putField (obsName, "obs", obsDescr)
	 .return_ ().setMaxStack (2).endMethod ();

      // void onObj (Object, Schema.Group g)

      dc.startPublicMethod ("onObj", onObjSig)
	 .aload0 () // this
	 .getField (obsName, "obs", obsDescr)
	 .aload1 () // obj
	 .checkCast (m.getParameterTypes () [0]);
      if (withGroup)
	 dc.aload2 (); // group

      dc.invoke (m)
	 .return_ ().setMaxStack (2 + (withGroup ? 1 : 0)).endMethod ();

      return createInstance (dc, obs);
   }

   private Observer createInstance (DynClass dc, Object obs)
      throws BlinkException.Binding
   {
      try
      {
	 Class<?> pojoObsClass = obs.getClass ();
	 Class<?> obsClass = dload.loadPrivileged (dc, pojoObsClass);
	 Constructor<?> ctor = obsClass.getConstructor (pojoObsClass);
	 return (Observer)ctor.newInstance (obs);
      }
      catch (NoSuchMethodException e)
      {
	 throw new BlinkException.Binding (e);
      }
      catch (InstantiationException e)
      {
	 throw new BlinkException.Binding (e);
      }
      catch (IllegalAccessException e)
      {
	 throw new BlinkException.Binding (e);
      }
      catch (InvocationTargetException e)
      {
	 throw new BlinkException.Binding (e);
      }
   }
   
   private Observer findInAncestry (Schema.Group g)
   {
      if (g != null)
      {
	 Observer obs = obsByName.get (g.getName ());
	 if (obs != null)
	 {
	    obsByName.put (g.getName (), obs);
	    return obs;
	 }
	 else
	    return findInAncestry (g.getSuperGroup ());
      }
      else
	 return fallback;
   }

   private final HashMap<NsName, Observer> obsByName =
      new HashMap <NsName, Observer> ();
   private final ObjectModel om;
   private final DynClassLoader dload;
   private Observer fallback;
}
