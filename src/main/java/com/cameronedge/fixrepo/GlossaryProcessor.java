/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixrepo;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads FIX Specification Glossary and extracts definitions from it.
 *
 * @author John Cameron
 */
public class GlossaryProcessor {

  public List<GlossaryEntry> processGlossaryText(Reader glossaryReader) throws IOException {
    List<GlossaryEntry> entries = new ArrayList<GlossaryEntry>();

    while (true) {
      GlossaryEntry entry = new GlossaryEntry();

      entry.valueName = copyUntil(glossaryReader, '\t');
      if (entry.valueName == null) {
        break;
      }

      if (entry.valueName.length() > 58) {
        System.out.println("WARNING: Long value " + entry.valueName );  
      }
      
      entry.valueName = RepoUtil.computeEnumName(entry.valueName);

//      System.out.println("Value: " + entry.valueName);
      
      String s = copyUntil(glossaryReader, '\t');  
      entry.description = RepoUtil.cleanText(s);

//      System.out.println("Desc: " + entry.description);

      entry.fieldName = copyUntil(glossaryReader, '\n');
      if (entry.fieldName.length() > 35) {
        System.out.println("WARNING: Long value " + entry.fieldName );  
      }

//      System.out.println("Field: " + entry.fieldName);

      entries.add(entry);
    }

    return entries;
  }

  private static String copyUntil(Reader glossaryReader, char c) throws IOException {
    StringBuffer sb = new StringBuffer();
    String retVal;

    int ch;
    do {
      ch = glossaryReader.read();
      if (ch >= 0 && ch != c) {
        sb.append((char)ch);
      }
    } while (ch != -1 && ch != c);

    if (ch == -1 && sb.length() == 0) {
      //No data read and end of stream detected.
      retVal = null;
    } else {
      retVal = sb.toString();
    }

    return retVal;
  }
}
