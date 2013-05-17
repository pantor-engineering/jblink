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

import java.math.BigInteger;

public class SchemaBuilder implements SchemaReader.Observer
{
   public SchemaBuilder (Schema schema)
   {
      this.schema = schema;
   }
   
   @Override public void onStartSchema () { }
   @Override public void onEndSchema () { }

   @Override public void onNsDecl (String ns, Location loc)
   {
      defaultNs = ns;
   }

   @Override public void onStartGroupDef (String name, String id,
					  String superName,
					  AnnotSet annots,
					  Location loc)
       throws BlinkException.Schema
   {
      curGrp = schema.addGroup (
	 NsName.get (defaultNs, name), parseTypeId (id, loc),
	 superName != null ? NsName.parse (superName) : null,
	 annots, loc);
   }

   private final Long parseTypeId (String id, Location loc)
      throws BlinkException.Schema
   {
      Long id_ = null;
      if (id != null && ! id.equals (""))
	 try
	 {
	    id_ = Util.parseU64IdAnnot (id);
	 }
	 catch (NumberFormatException e)
	 {
	    throw new BlinkException.Schema ("Bad type id syntax: " + id, loc);
	 }
      return id_;
   }

   @Override public void onEndGroupDef () { }
   
   @Override public void onStartDefine (String name, String id,
					AnnotSet annots,
					Location loc)
       throws BlinkException.Schema
   {
      curDef = schema.addDefine (NsName.get (defaultNs, name), id, annots, loc);
   }
   
   @Override public void onEndDefine ()
   {
      curDef.setType (pendType);
   }
   
   @Override public void onStartField (Location loc)
   {
      pendLoc = loc;
   }
   
   @Override public void onEndField (String name, String id,
				     Schema.Presence pres,
				     AnnotSet annots)
       throws BlinkException.Schema
   {
      curGrp.addField (name, id, pendType, pres, annots, pendLoc);
   }

   @Override public void onPrimType (Schema.TypeCode t, Schema.Rank r,
				     AnnotSet annots, Location loc)
   {
      pendType = new Schema.Type (t, r, annots, loc);
   }
      
   @Override public void onStringType (Schema.Rank r, String ct,
				       AnnotSet annots,
				       Location loc)
   {
      pendType = new Schema.StrType (ct, r, annots, loc);
   }
   
   @Override public void onTypeRef (String name, Schema.Layout layout,
				    Schema.Rank r, AnnotSet annots,
				    Location loc)
   {
      pendType = new Schema.Ref (NsName.parse (name), defaultNs, r, layout,
				 annots, loc);
   }
   
   @Override public void onStartEnum (Location loc)
   {
      curEnum = new Schema.Enum (loc);
      pendType = curEnum;
      nextEnumVal = 0;
   }

   @Override public void onEndEnum () { }
   
   @Override public void onEnumSym (String name, String val,
				    AnnotSet annots, Location loc)
       throws BlinkException.Schema
   {
      int intVal;
      if (val == null || val.equals (""))
	 intVal = nextEnumVal;
      else
      {
	 try
	 {
	    intVal = Util.parseI32IdAnnot (val);
	 }
	 catch (NumberFormatException e)
	 {
	    throw new BlinkException.Schema ("Bad enum value syntax: " + val,
					     loc);
	 }
      }

      nextEnumVal = intVal + 1;
      curEnum.addSymbol (name, intVal, annots, loc);
   }

   @Override public void onSchemaAnnot (AnnotSet annots, Location loc)
   {
      schema.addAnnotations (annots, defaultNs);
   }
   
   @Override public void onIncrAnnot (String name, String substep,
				      Schema.PathType t, String id,
				      AnnotSet annots,
				      Location loc)
   {
      schema.addIncrAnnot (NsName.parse (name), defaultNs, substep, t, id,
			   annots, loc);
   }
   
   private final Schema schema;
   private String defaultNs = "";
   private Schema.Group curGrp;
   private Schema.Define curDef;
   private Schema.Type pendType;
   private Schema.Enum curEnum;
   private Location pendLoc;
   private int nextEnumVal;

}
