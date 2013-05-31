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

import java.io.Reader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public final class SchemaReader
{
   public SchemaReader (Observer obs)
   {
      this.obs = obs;
      curTok = new Token ();
      pendTok = new Token ();
   }

   public void read (Reader rd) throws IOException, BlinkException.Schema
   {
      read (rd, "-");
   }

   public static void read (Reader rd, String srcName, Schema s)
      throws IOException, BlinkException
   {
      SchemaReader srd = new SchemaReader (new SchemaBuilder (s));
      srd.read (rd, srcName);
   }

   public static void read (String fileName, Schema s)
      throws IOException, BlinkException
   {
      read (new FileReader (fileName), fileName, s);
   }

   public static void readFromString (String data, String srcName, Schema s)
      throws IOException, BlinkException
   {
      read (new StringReader (data), srcName, s);
   }
   
   public static void readFromString (String data, Schema s)
      throws IOException, BlinkException
   {
      readFromString (data, "-", s);
   }
   
   public static enum Tk
   {
      Undefined, End, Err, Comma, Dot, Eq, Slash, RArrow, LArrow, Int,
      UInt, Str, Ct, LBrk, RBrk, Colon, QMark, Asterisk, Bar, Hex,
      QName, Name, At, KwI8, KwU8, KwI16, KwU16, KwI32, KwU32, KwI64,
      KwU64, KwF64, KwDecimal, KwDate, KwTimeOfDayMilli,
      KwTimeOfDayNano, KwNanotime, KwMillitime, KwBool, KwString,
      KwObject, KwNamespace, KwType, KwSchema
   }

   public interface Observer
   {
      void onStartSchema () throws BlinkException.Schema;
      void onEndSchema () throws BlinkException.Schema;
      void onNsDecl (String ns, Location loc) throws BlinkException.Schema;

      void onStartDefine (String name, String id, AnnotSet annots,
                          Location loc) throws BlinkException.Schema;;
      void onEndDefine () throws BlinkException.Schema;
      void onStartGroupDef (String name, String id, String superName,
                            AnnotSet annots, Location loc)
         throws BlinkException.Schema;
      void onEndGroupDef () throws BlinkException.Schema;
      void onStartField (Location loc) throws BlinkException.Schema;
      void onEndField (String name, String id, Schema.Presence pres,
                       AnnotSet annots) throws BlinkException.Schema;

      void onPrimType (Schema.TypeCode t, Schema.Rank r, AnnotSet annots,
                       Location loc);
      void onStringType (Schema.Rank r, String ct, AnnotSet annots,
                         Location loc) throws BlinkException.Schema;
      void onTypeRef (String name, Schema.Layout layout, Schema.Rank r,
                      AnnotSet annots, Location loc)
         throws BlinkException.Schema;

      void onStartEnum (Location loc) throws BlinkException.Schema;
      void onEndEnum () throws BlinkException.Schema;
      void onEnumSym (String name, String val, AnnotSet annots, Location loc)
         throws BlinkException.Schema;

      void onSchemaAnnot (AnnotSet annots, Location loc)
         throws BlinkException.Schema;
      void onIncrAnnot (String name, String substep, Schema.PathType t,
                        String id, AnnotSet annots, Location loc)
         throws BlinkException.Schema;
   }
   
   public void read (Reader rd, String srcName)
      throws IOException, BlinkException.Schema
   {
      reset (rd, srcName);
   
      fillBuf ();

      next (); // Init current token
      next (); // Init lookahead token

      obs.onStartSchema ();
   
      if (next (Tk.KwNamespace))
         nsDecl ();
      
      while (! match (Tk.End))
         def ();
      
      obs.onEndSchema ();
   }

   private void reset (Reader rd, String srcName)
   {
      this.rd = rd;
      this.srcName = srcName;
      lastLoc = new Location ();

      hasMore = true;
      bufTake = 0;
      bufEnd = 0;

      line = 1;
      col = 0;
      tokPos = 0;
      lastCommaPos = 0;
      lastCommaLine = 0;
      lastFieldNameLine = 0;
      lastFieldTypeLine = 0;
      lastEmptyGrpLine = 0;
      lastNameLine = 0;
      clearAnnots ();
      curTok.type = Tk.Undefined;
      pendTok.type = Tk.Undefined;
      pendName = null;
      pendId = null;
   }
   
   // Lexer
   ////////////////////////////////////////////////////////////

   private static final HashMap<String, Tk> kwMap = new HashMap <String, Tk> ();

   static
   {
      kwMap.put ("i8", Tk.KwI8);
      kwMap.put ("u8", Tk.KwU8);
      kwMap.put ("i16", Tk.KwI16);
      kwMap.put ("u16", Tk.KwU16);
      kwMap.put ("i32", Tk.KwI32);
      kwMap.put ("u32", Tk.KwU32);
      kwMap.put ("i64", Tk.KwI64);
      kwMap.put ("u64", Tk.KwU64);
      kwMap.put ("f64", Tk.KwF64);
      kwMap.put ("decimal", Tk.KwDecimal);
      kwMap.put ("date", Tk.KwDate);
      kwMap.put ("timeOfDayMilli", Tk.KwTimeOfDayMilli);
      kwMap.put ("timeOfDayNano", Tk.KwTimeOfDayNano);
      kwMap.put ("nanotime", Tk.KwNanotime);
      kwMap.put ("millitime", Tk.KwMillitime);
      kwMap.put ("bool", Tk.KwBool);
      kwMap.put ("string", Tk.KwString);
      kwMap.put ("object", Tk.KwObject);
      kwMap.put ("namespace", Tk.KwNamespace);
      kwMap.put ("type", Tk.KwType);
      kwMap.put ("schema", Tk.KwSchema);
   }
   
   private static final class Token
   {
      Token ()
      {
      }

      private String getText ()
      {
         return text.toString ();
      }
      
      private void clearText ()
      {
         text.setLength (0);
      }
      
      private void appendText (char c)
      {
         text.append (c);
      }

      private void setText (char c)
      {
         text.setLength (0);
         text.append (c);
      }

      private void setText (String s)
      {
         text.setLength (0);
         text.append (s);
      }
      
      Tk type;
      StringBuilder text = new StringBuilder ();
      int line;
      int col;
      int start;
      int startOfLine;
   }
   
   private boolean match (Tk expected)
   {
      return curTok.type == expected;
   }

   private boolean next (Tk expected) throws IOException
   {
      if (match (expected))
      {
         next ();
         return true;
      }
      else
         return false;
   }

   private String nextOf (Tk... toks) throws IOException
   {
      for (Tk t : toks)
         if (match (t))
         {
            String val = curTok.getText ();
            next ();
            return val;
         }

      return null;
   }

   private String nextNameOrKeyword () throws IOException
   {
      if (match (Tk.Name) || match (Tk.QName) ||
          curTok.type.ordinal () >= Tk.KwI8.ordinal ())
      {
         String val = curTok.getText ();
         next ();
         return val;
      }
      else
         return null;
   }

   private boolean matchPend (Tk t)
   {
      return pendTok.type == t;
   }

   private void consume () throws IOException
   {
      if (buf [bufTake] == '\n')
      {
         ++ line;
         lineStart = bufTake + 1;
         col = 0;
      }

      ++ bufTake;
      ++ col;
         
      if (bufTake == bufEnd)
         fillBuf ();
   }

   private void fillBuf () throws IOException
   {
      bufTake = 0;
      lineStart = 0;
      pendTok.start = 0;
      pendTok.startOfLine = 0;
      curTok.start = 0;
      curTok.startOfLine = 0;
      bufEnd = rd.read (buf);
      hasMore = hasMore && bufEnd != -1;
   }

   private char peek ()
   {
      if (hasMore)
         return buf [bufTake];
      else
         return '\0';
   }

   private char get () throws IOException
   {
      if (hasMore)
      {
         char c = peek ();
         consume ();
         return c;
      }
      else
         return '\0';
   }

   private boolean lookahead (char expected) throws IOException
   {
      if (peek () == expected)
      {
         consume ();
         return true;
      }
      else
         return false;
   }

   private boolean isDigit (char c)
   {
      return c >= '0' && c <= '9';
   }

   private boolean isHexDigit (char c)
   {
      return isDigit (c) || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
   }

   private boolean isNameStartChar (char c)
   {
      return c == '_' ||  (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
   }
   
   private void innerNext () throws IOException, LexError
   {
      lastLoc = new Location (srcName, curTok.line, curTok.col);

      Token tmp = curTok;
      curTok = pendTok;
      pendTok = tmp;

      if (curTok.type == Tk.End)
      {
         pendTok.type = Tk.End;
         return;
      }

      ++ tokPos;
      
      skipWsAndComments ();

      char c = get ();

      pendTok.start = bufTake;
      pendTok.startOfLine = lineStart;

      switch (c)
      {
       case ',':
          setToken (Tk.Comma);
          lastCommaPos = tokPos;
          lastCommaLine = line;
          break;

       case '.': setToken (Tk.Dot); break;
       case '=': setToken (Tk.Eq); break;
       case '/': setToken (Tk.Slash); break;
       case ':': setToken (Tk.Colon); break;
       case '?': setToken (Tk.QMark); break;
       case '*': setToken (Tk.Asterisk); break;
       case '|': setToken (Tk.Bar); break;
       case '[': setToken (Tk.LBrk); break;
       case ']': setToken (Tk.RBrk); break;
       case '@': setToken (Tk.At); break;

       case '-':
          if (lookahead ('>'))
             setToken (Tk.RArrow);
          else
          {
             if (! isDigit (peek ()))
                throw lexError ("Expected digit or '>' after '-'");
             setToken (Tk.Int);
             readUInt (c);
          }
          break;

       case '"': case '\'':
          setToken (Tk.Str);
          readStr (c);
          break;

       case '<':
          if (lookahead ('-'))
             setToken (Tk.LArrow);
          else
             throw lexError ("Expected dash after '<'");
          break;

       case '(':
       {
          setToken (Tk.Ct);
          readStr (')');
       }
       break;
            
       case '\\':
       {
          pendTok.clearText ();
          setToken (Tk.Name);
          if (hasMore)
             requireNameStart (peek (), "name after backslash");
          else
             throw lexError (
                "Missing name after backslash at end of file");
          readNcName ();
       }
       break;

      case '\0':
         if (hasMore)
            throw lexError ("Character not allowed here", c);
         else
         {
            pendTok.start = 0;
            setToken (Tk.End);
         }
         break;
    
      case '0':
         if (lookahead ('x'))
         {
            readHex ();
            break;
         }
         // Fallthrough
            
      default:
         if (isDigit (c))
         {
            setToken (Tk.UInt);
            readUInt (c);
         }
         else if (isNameStartChar (c))
         {
            pendTok.setText (c);
            readNameOrKeyword ();
         }
         else
            throw lexError ("Character not allowed here", c);
      }
   }

   private void setToken (Tk t)
   {
      pendTok.type = t;
      pendTok.line = line;
      pendTok.col = col;
   }

   private void skipWsAndComments () throws IOException
   {
      for (;;)
         switch (peek ())
         {
         case 0x9: case 0xd: case 0x20: case 0xa:
            consume ();
            break;
         case '#':
            consume ();
            skipComment ();
            break;
         default:
            return;
         }
   }
   
   private void readUInt (char first) throws IOException, LexError
   {
      pendTok.setText (first);
      char c;
      while (isDigit (c = peek ()))
      {
         consume ();
         pendTok.appendText (c);
      }

      if (hasMore && isNameStartChar (c))
         throw lexError ("A number must end in digits");
   }

   private void readHex () throws IOException, LexError
   {
      setToken (Tk.Hex);
      pendTok.setText ("0x");
      char c;
      while (isHexDigit (c = peek ()))
      {
         consume ();
         pendTok.appendText (c);
      }

      if (hasMore && isNameStartChar (c))
         throw lexError ("A hex number must end in hex digits");
   }

   private void requireNameStart (char c, String what)
      throws IOException, LexError
   {
      if (! isNameStartChar (c))
      {
         if (Character.isWhitespace (c))
            throw lexError ("Missing " + what);
         else
            throw lexError ("Character not allowed at start of " + what, c);
      }
   }

   private void readNcName () throws IOException
   {
      char c;
      while (isNameStartChar (c = peek ()) || isDigit (c))
      {
         consume ();
         pendTok.appendText (c);
      }
   }

   private void readStr (char end) throws IOException, LexError
   {
      pendTok.clearText ();
      while (hasMore)
      {
         char c = get ();
         if (c != end)
         {
            if (c != '\n')
               pendTok.appendText (c);
            else
               throw lexError ("Multiline literals are not allowed");
         }
         else
            return;
      }

      throw lexError ("Literal not terminated at end of file, expected", end);
   }

   private void readNameOrKeyword () throws IOException, LexError
   {
      readNcName ();
      if (peek () == ':')
      {
         consume ();
         setToken (Tk.QName);
         pendTok.appendText (':');
         if (hasMore)
         {
            requireNameStart (peek (), "name part in qualified name");
            readNcName ();
         }
         else
            throw lexError ("Missing name part after colon at end of file");
      }
      else
      {
         Tk kw = kwMap.get (pendTok.getText ());
         if (kw != null)
            setToken (kw);
         else
            setToken (Tk.Name);
      }
   }
   
   private void skipComment () throws IOException
   {
      while (hasMore)
         if (get () == '\n')
            return;
   }
   
   private void next () throws IOException
   {
      try
      {
         innerNext ();
      }
      catch (LexError e)
      {
         setToken (Tk.Err);
         pendTok.setText (e.msg);
      }
   }

   // Grammar
   ////////////////////////////////////////////////////////////

   private void def () throws IOException, BlinkException.Schema
   {
      annots ();
   
      lastNameLine = curTok.line;
      if (match (Tk.QName))
      {
         pendName = require (Tk.QName);
         incrAnnot ();
      }
      else if (match (Tk.KwSchema))
         incrAnnot ();
      else
      {
         nameWithId ("group or type definition name, or an incremental " + 
                     "annotation");
         if (match (Tk.LArrow) || match (Tk.Dot))
            incrAnnot ();
         else if (next (Tk.Eq))
            define ();
         else
            groupDef ();
      }
   }

   private boolean matchEnum () throws IOException
   {
      // An enumeration starts with   "|", name "|", or name "/"
      
      return (match (Tk.Bar) ||
              (match (Tk.Name) &&
               (matchPend (Tk.Slash) || matchPend (Tk.Bar))));
   }
   
   private void define () throws IOException, BlinkException.Schema
   {
      obs.onStartDefine (pendName, consumeId (), annotations, lastLoc);

      clearAnnots ();
      annots ();

      if (matchEnum ())
         enumeration ();
      else
         type ();

      obs.onEndDefine ();
   }

   private void groupDef () throws IOException, BlinkException.Schema
   {
      if (lastEmptyGrpLine == lastNameLine && lastEmptyGrpLine != 0)
         warning ("Multiple empty groups on the same line. Maybe " +
                  "there is a missing '->' earlier", lastEmptyGrpLine);
   
      int grpStartLine = lastNameLine;

      String superName = null;
   
      if (next (Tk.Colon))
      {
         grpStartLine = curTok.line;
         superName = superRef ();
      }

      obs.onStartGroupDef (pendName, consumeId (), superName, annotations,
                           lastLoc);
   
      clearAnnots ();

      if (next (Tk.RArrow))
         for (;;)
         {
            field ();
            if (! next (Tk.Comma))
               break;
         }
      else
         lastEmptyGrpLine = grpStartLine;

      obs.onEndGroupDef ();
   }

   private String superRef () throws IOException, BlinkException.Schema
   {
      String name = nextOf (Tk.Name, Tk.QName);
      if (name != null)
         return name;
      else
         throw expected ("supertype name");
   }

   private void field () throws IOException, BlinkException.Schema
   {
      annots ();
      lastFieldTypeLine = curTok.line;
      obs.onStartField (new Location (srcName, curTok.line, curTok.col));
      type ();
      annots ();
      lastFieldNameLine = curTok.line;
      nameWithId ("field name");
      Schema.Presence pres = Schema.Presence.Required;
      if (next (Tk.QMark))
         pres = Schema.Presence.Optional;
      obs.onEndField (pendName, consumeId (), pres, annotations);
      clearAnnots ();
   }

   private void nameWithId (String what)
      throws IOException, BlinkException.Schema
   {
      pendId = null;
      pendName = require (Tk.Name, what);
      if (next (Tk.Slash))
      {
         pendId = nextOf (Tk.UInt, Tk.Hex);
         if (pendId == null)
            throw expected ("unsigned integer or hex number");
      }
   }
   
   private void enumeration () throws IOException, BlinkException.Schema
   {
      obs.onStartEnum (lastLoc);
   
      if (next (Tk.Bar))
         sym ();
      else
         for (;;)
         {
            sym ();
            if (! next (Tk.Bar))
               break;
         }

      obs.onEndEnum ();
   }

   private void sym () throws IOException, BlinkException.Schema
   {
      annots ();
      String name = require (Tk.Name, "enum symbol name");
      String val = null;
      if (next (Tk.Slash))
      {
         val = nextOf (Tk.UInt, Tk.Int, Tk.Hex);
         if (val == null)
            throw expected ("integer or hex number");
      }
      obs.onEnumSym (name, val, annotations, lastLoc);
      clearAnnots ();
   }

   private Schema.Rank rank () throws IOException, BlinkException.Schema
   {
      if (next (Tk.LBrk))
      {
         require (Tk.RBrk);
         return Schema.Rank.Sequence;
      }
      else
         return Schema.Rank.Single;
   }

   private void type () throws IOException, BlinkException.Schema
   {
      switch (curTok.type)
      {
       case Name: case QName:
          ref ();
          break;

       case KwString:
          string ();
          break;

       case KwI8: case KwU8: case KwI16: case KwU16: case KwI32: case KwU32:
       case KwI64: case KwU64: case KwF64: case KwDecimal: case KwDate:
       case KwTimeOfDayMilli: case KwTimeOfDayNano: case KwNanotime:
       case KwMillitime: case KwBool: case KwObject:
          primType ();
          break;
    
       default:
          throw expected ("type");
      }
   }

   private void primType () throws IOException, BlinkException.Schema
   {
      Tk t = curTok.type;
      next ();
      Schema.Rank r = rank ();
      obs.onPrimType (mapTypeCode (t), r, annotations, lastLoc);
      clearAnnots ();
   }

   private void ref () throws IOException, BlinkException.Schema
   {
      String name = curTok.getText ();
      next ();
      Schema.Layout layout = Schema.Layout.Static;
      if (next (Tk.Asterisk))
         layout = Schema.Layout.Dynamic;

      Schema.Rank r = rank ();
   
      obs.onTypeRef (name, layout, r, annotations, lastLoc);
      clearAnnots ();
   }

   private void string () throws IOException, BlinkException.Schema
   {
      next ();
      String ct = nextOf (Tk.Ct);
      Schema.Rank r = rank ();
      obs.onStringType (r, ct, annotations, lastLoc);
      clearAnnots ();
   }
   
   private void nsDecl () throws IOException, BlinkException.Schema
   {
      String ns = require (Tk.Name, "namespace name");
      obs.onNsDecl (ns, lastLoc);
   }

   private void annots () throws IOException, BlinkException.Schema
   {
      while (next (Tk.At))
         annot ();
   }

   private void annot () throws IOException, BlinkException.Schema
   {
      String name = nextNameOrKeyword ();
      if (name != null)
      {
         require (Tk.Eq);
         StringBuilder val = new StringBuilder ();
         val.append (require (Tk.Str));
         for (;;)
         {
            String seg = nextOf (Tk.Str);
            if (seg == null)
               break;
            val.append (seg);
         }
         annotations.set (NsName.parse (name), val.toString ());
      }
      else
         throw expected ("annotation name");
   }

   private void incrAnnot () throws IOException, BlinkException.Schema
   {
      Location loc = lastLoc;
   
      if (! annotations.empty ())
         throw error ("An incremental annotation clause cannot be preceded" +
                      " by annotations");
      
      if (pendId != null)
         throw error ("An incremental annotation clause cannot set an" +
                 " ID using the slash notation. Use '<- id' instead");

      if (next (Tk.KwSchema))
      {
         incrAnnotList ();
         consumeId ();
         obs.onSchemaAnnot (annotations, loc);
      }
      else
      {
         String substep = null;
         Schema.PathType pathType = Schema.PathType.NameStep;

         if (next (Tk.Dot))
         {
            if (next (Tk.KwType))
               pathType = Schema.PathType.TypeStep;
            else
            {
               substep = require (Tk.Name, "field or symbol name");
               if (next (Tk.Dot))
               {
                  require (Tk.KwType);
                  pathType = Schema.PathType.TypeStep;
               }
            }
         }

         incrAnnotList ();
         obs.onIncrAnnot (pendName, substep, pathType, consumeId (),
                          annotations, loc);
      }

      clearAnnots ();
   }

   private void incrAnnotList () throws IOException, BlinkException.Schema
   {
      if (! match (Tk.LArrow))
         throw expected (getTokenDescr (Tk.LArrow));

      while (next (Tk.LArrow))
      {
         if (next (Tk.At))
            annot ();
         else
         {
            pendId = nextOf (Tk.Int, Tk.UInt, Tk.Hex);
            if (pendId == null)
               throw expected ("incremental annotation, integer or hex number");
         }
      }
   }

   private void clearAnnots ()
   {
      annotations.clear ();
   }

   private String consumeId ()
   {
      String id = pendId;
      pendId = null;
      return id;
   }

   private String require (Tk t, String what)
      throws IOException, BlinkException.Schema
   {
      if (match (t))
      {
         String val = curTok.getText ();
         next ();
         return val;
      }
      else
         throw expected (what != null ? what : getTokenDescr (t));
   }

   private String require (Tk t) throws IOException, BlinkException.Schema
   {
      return require (t, null);
   }
   
   private String getTokenDescr (Tk t)
   {
      switch (t)
      {
       default:
       case Undefined: return "UNDEFINED";
       case End: return "END";
       case Err: return "ERROR";
       case Comma: return "','";
       case Dot: return "'.'";
       case Eq: return "'='";
       case Slash: return "'/'";
       case RArrow: return "'->'";
       case LArrow: return "'<-'";
       case Int: return "integer";
       case UInt: return "unsigned integer";
       case Str: return "string literal";
       case Ct: return "content type";
       case LBrk: return "'['";
       case RBrk: return "']'";
       case Colon: return "':'";
       case QMark: return "'?'";
       case Asterisk: return "'*'";
       case Bar: return "'|'";
       case Hex: return "hex number";
       case QName: return "qualified name";
       case Name: return "name";
       case At: return "'@'";
       case KwI8: return "keyword 'i8'";
       case KwU8: return "keyword 'u8'";
       case KwI16: return "keyword 'i16'";
       case KwU16: return "keyword 'u16'";
       case KwI32: return "keyword 'i32'";
       case KwU32: return "keyword 'u32'";
       case KwI64: return "keyword 'i64'";
       case KwU64: return "keyword 'u64'";
       case KwF64: return "keyword 'f64'";
       case KwDecimal: return "keyword 'decimal'";
       case KwDate: return "'keyword date'";
       case KwTimeOfDayMilli: return "keyword 'timeOfDayMilli'";
       case KwTimeOfDayNano: return "keyword 'timeOfDayNano'";
       case KwNanotime: return "keyword 'nanotime'";
       case KwMillitime: return "keyword 'millitime'";
       case KwBool: return "keyword 'bool'";
       case KwString: return "keyword 'string'";
       case KwObject: return "keyword 'object'";
       case KwNamespace: return "keyword 'namespace'";
       case KwType: return "keyword 'type'";
       case KwSchema: return "keyword 'schema'";
      }
   }

   private Schema.TypeCode mapTypeCode (Tk t)
   {
      switch (t)
      {
       case KwI8: return Schema.TypeCode.I8;
       case KwU8: return Schema.TypeCode.U8;
       case KwI16: return Schema.TypeCode.I16;
       case KwU16: return Schema.TypeCode.U16;
       case KwI32: return Schema.TypeCode.I32;
       case KwU32: return Schema.TypeCode.U32;
       case KwI64: return Schema.TypeCode.I64;
       case KwU64: return Schema.TypeCode.U64;
       case KwF64: return Schema.TypeCode.F64;
       case KwDecimal: return Schema.TypeCode.Decimal;
       case KwDate: return Schema.TypeCode.Date;
       case KwTimeOfDayMilli: return Schema.TypeCode.TimeOfDayMilli;
       case KwTimeOfDayNano: return Schema.TypeCode.TimeOfDayNano;
       case KwNanotime: return Schema.TypeCode.Nanotime;
       case KwMillitime: return Schema.TypeCode.Millitime;
       case KwBool: return Schema.TypeCode.Bool;
       case KwString: return Schema.TypeCode.String;
       case KwObject: return Schema.TypeCode.Object;
       default:
          throw new RuntimeException ("cannot happen");
      }
   }

   // Error handling
   ////////////////////////////////////////////////////////////

   private LexError lexError (String msg)
   {
      return new LexError (msg);
   }

   private static final class LexError extends Exception
   {
      LexError (String msg)
      {
         super (msg);
         this.msg = msg;
      }
      String msg;
   }
   
   private LexError lexError (String msg, char c)
   {
      return new LexError (
         String.format ("%s: %s", msg, Util.displayStr (String.valueOf (c))));
   }
   
   private BlinkException.Schema error (String msg, String details)
   {
      StringBuilder sb = new StringBuilder ();
      sb.append (msg);
      if (details != null)
         sb.append (String.format ("%n  ")).append (details);
      if (curTok.startOfLine < curTok.start)
      {
         int off = curTok.start - curTok.startOfLine;
         int from = curTok.startOfLine;
         int following = Math.min (70, bufEnd - curTok.start);
         int to = curTok.start;
         for (; to < curTok.start + following; ++ to)
            if (buf [to] == '\n')
               break;

         int size = to - from;
         if (size > 70)
         {
            int trim = size - 70;
            from += trim;
            off -= trim;
            size = 70;
         }

         String content = Util.displayStr (new String (buf, from, size), 80);
 
         sb.append (String.format ("%n  Content: %s%n           %-" +
                                   String.valueOf (off) + "s^",
                                   content, ""));
      }

      return new BlinkException.Schema (sb.toString (),
                                        new Location (srcName, line, col));
   }

   private BlinkException.Schema error (String msg)
   {
      return error (msg, (String)null);
   }

   private BlinkException.Schema error (String msg, Tk t)
   {
      return error (String.format ("%s: %s", msg , getTokenDescr (t)));
   }

   private BlinkException.Schema expected (String what)
   {
      if (match (Tk.Err))
         return error (curTok.getText () + ", when expecting " + what);
      else
      {
         String details = null;
         if (lastCommaLine > 0 &&
             line != lastCommaLine && (tokPos - lastCommaPos - 1) <= 2)
            details = String.format ("Hint: There could be a superfluous " +
                                     "comma at line: %d", lastCommaLine);
         else if (lastFieldNameLine != lastFieldTypeLine &&
                  pendTok.line == lastFieldNameLine)
            details = String.format ("Hint: There could be a missing field " +
                                     "name following the type on line %d",
                                     lastFieldTypeLine);
         else if (lastEmptyGrpLine == (pendTok.line - 1) &&
                  lastEmptyGrpLine != 0)
            details = String.format ("Hint: There could be a missing '->' " +
                                     "at line %d", lastEmptyGrpLine);
         else
         {
            if (lastEmptyGrpLine == pendTok.line)
               details = "Hint: It could be a missing '->', " +
                         "'=', ':', or '/' earlier";
         }

         return error ("Expected " + what + " but got " +
                       getTokenDescr (curTok.type), details);

      }
   }

   private void warning (String msg, int line)
   {
      log.warning (String.format ("%s:%d: warning: %s%n", srcName, line, msg));
   }
   
   // State
   ////////////////////////////////////////////////////////////
   
   private final Observer obs;
   private final char [] buf = new char [4096];

   private AnnotSet annotations = new AnnotSet ();

   private int bufTake;
   private int bufEnd;
   private boolean hasMore;

   private Reader rd;
   private String srcName;
   
   private Token curTok;
   private Token pendTok;
   private int tokPos;
   private String pendName;
   private String pendId;

   private int line;
   private int col;
   private int lineStart;
   private int lastCommaPos;
   private int lastCommaLine;
   private int lastFieldNameLine;
   private int lastFieldTypeLine;
   private int lastEmptyGrpLine;
   private int lastNameLine;
   private Location lastLoc;

   private final static Logger log =
      Logger.getLogger (SchemaReader.class.getName ());
}
