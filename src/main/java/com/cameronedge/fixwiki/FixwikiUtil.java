/*
 * Copyright (c) 2010. Cameron Edge Pty Ltd. All rights reserved.
 */

package com.cameronedge.fixwiki;

import com.cameronedge.fixrepo.RepoUtil;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FIXwiki related utilities.
 *
 * @author John Cameron
 */
public class FixwikiUtil {
  private static Set<Character> wikiFormattingChars = new HashSet<Character>();

  static {
    wikiFormattingChars.add(' ');
    wikiFormattingChars.add('*');
    wikiFormattingChars.add('#');
    wikiFormattingChars.add(';');
    wikiFormattingChars.add(':');
    wikiFormattingChars.add('=');
    wikiFormattingChars.add('{');
    wikiFormattingChars.add('|');
    wikiFormattingChars.add('!');
  }

  public static String formatDescription(String value, LinkDetector linkDetector) {
    //Turn all whole words which match field or messages names into links by surrounding them with [[]]
    String valueWithLinks = linkDetector == null ? value : linkDetector.convert(value);

    String formatted = valueWithLinks;

    if (valueWithLinks != null && valueWithLinks.length() > 0) {
      LineNumberReader reader = new LineNumberReader(new StringReader(valueWithLinks));

      StringBuffer result = new StringBuffer();
      String s;
      try {
        while ((s = reader.readLine()) != null) {
          s = s.trim();

          if (s.length() > 0) {

            s = RepoUtil.cleanText(s);

            char firstChar = s.charAt(0);
            if (firstChar <= 127) {
              //Normal ASCII character.
              //Escape it if it is some Wiki formatting character.
              if (wikiFormattingChars.contains(firstChar)) {
                result.append("<nowiki>");
                result.append(firstChar);
                result.append("</nowiki>");
                if (s.length() > 1) {
                  result.append(s.substring(1));
                }
              } else {
                result.append(s);
              }
            } else {
              //Special character triggers some special processing.
              if (firstChar == RepoUtil.BULLET) {
                //Treat this as a list - translate to standard Wiki list character '*'.
                result.append('*');
                if (s.length() > 1) {
                  result.append(s.substring(1));
                }
              } else {
                result.append(s);
              }
            }
          }

          //New lines convert to double new lines, forcing a Wiki line break
          result.append("\n\n");
        }
      } catch (IOException ex) {
        //Cannot happen processing a String
      }

      formatted = result.toString();

      //Having bar characters anywhere in the string is asking for trouble! Bar is used as delimiter is passing
      //parameteres to Wiki templates.
      formatted = formatted.replace('|', ' ');
    }
    return formatted;
  }

  public static void writeWikiTable(Writer writer, List<String[]> table, LinkDetector linkDetector) {
    PrintWriter wikiWriter = new PrintWriter(writer);
    wikiWriter.println("\n{|border=\"1\"");
    for (String[] columns : table) {

      wikiWriter.println("\n|-");
      for (String column : columns) {
        wikiWriter.print("\n|");

        //Convert text to contain links.
        wikiWriter.print(formatDescription(column, linkDetector));
      }
    }
    wikiWriter.println("\n|}");
  }
}
