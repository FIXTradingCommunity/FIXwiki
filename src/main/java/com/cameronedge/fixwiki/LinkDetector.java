/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixwiki;

import com.cameronedge.fixrepo.RepoInfo;
import com.cameronedge.fixrepo.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Searches String for words matching a list of special "link" names.
 * Such words are turned into Wiki style links by surrounding them with [[ and ]].
 * <p/>
 * There are two levels of link names. It will only search the second lot of links if there is no match in the
 * first lot of links.
 * In FIXwiki the first lot of links contains message and field names. The second lot of linke contains
 * value names. So if the same token in teh text matches a field name as well as a value name, the field name will
 * be used in preference.
 *
 * @author John Cameron
 */
public class LinkDetector {
  private StringBuffer converted;
  private int index;
  private Map<String, String> linkNames1, linkNames2;
  private String source;
  private int sourceLen;

  /**
   * Initialize detector with thr two soources of links. The first one take priority in case of a token appear in both.
   * <p/>
   * The link names are in the form of token and link. Often the two are the same.
   * If they are the same, the link is translated to:
   * <pre>
   *   [[token]]
   * </pre>
   * If they are different the link is translated to:
   * <pre>
   *   [[link|token]]
   * </pre>
   *
   * @param linkNames1 First priority links
   * @param linkNames2 Second priority links
   */
  public LinkDetector(Map<String, String> linkNames1, Map<String, String> linkNames2) {
    this.linkNames1 = linkNames1;
    this.linkNames2 = linkNames2;
  }

  public LinkDetector(RepoInfo repoInfo, int matchTagValues) {
    linkNames1 = new HashMap<String, String>();
    linkNames2 = new HashMap<String, String>();

    if (matchTagValues > 0) {
      //Add in values names as second priority. Use enumInfos for that.
      Map<String, List<Properties>> enumInfos = repoInfo.getEnumInfos();
      List<Properties> values = enumInfos.get(Integer.toString(matchTagValues));
      String fieldName = repoInfo.getFieldNameFromTag(matchTagValues);
      for (Properties props : values) {
        String enumValue = props.getProperty("Enum");
        String enumName = props.getProperty("EnumName");
        linkNames2.put(enumName, Util.computeValueTitle(fieldName, enumValue, enumName));
      }
    } else {
      //Extract all linkable repo names into a name set which can be used by our LinkDetector.
      //Start by adding in all field names - can get those from the key set of fieldNameTagMap.
      Map<String, Integer> fieldNameTagMap = repoInfo.getFieldNameTagMap();
      for (String s : fieldNameTagMap.keySet()) {
        //Account is a bit too common.
        if (!"Account".equals(s)) {
          linkNames1.put(s, s);
        }
      }

      //Now add component names. Can get those from the key set of component infos.
      Map<String, List<Properties>> componentInfos = repoInfo.getComponentInfos();
      for (String s : componentInfos.keySet()) {
        String previous = linkNames1.put(s, s);
        if (previous != null) {
          System.out.println("WARNING: Duplicate name " + s);
        }
      }

      //Add in message names. Use messageInfos for that.
      Map<String, List<Properties>> messageInfos = repoInfo.getMessageInfos();
      //We need to iterate since the key to this map is msgType.
      for (List<Properties> values : messageInfos.values()) {
        Properties props = values.get(0); //Message only has only one Properties.
        String messageName = props.getProperty("MessageName");
        String previous = linkNames1.put(messageName, messageName);
        if (previous != null) {
          System.out.println("WARNING: Duplicate name " + messageName);
        }
      }
    }
  }

  public String convert(String source) {
    if (source == null) {
      return null;
    }

    this.source = source;
    sourceLen = source.length();
    converted = new StringBuffer();
    index = 0;

    String token;
    do {
      emitToTokenStart();

      token = extractToken();

      if (token != null) {
        String link = null;
        boolean gotLink = linkNames1.keySet().contains(token);
        if (gotLink) {
          link = linkNames1.get(token);
        } else if (linkNames2 != null) {
          //Try the second set of links.
          gotLink = linkNames2.keySet().contains(token);
          if (gotLink) {
            link = linkNames2.get(token);
          }
        }

        if (gotLink) {
          emit("[[");
          if (!token.equals(link)) {
            emit(link + "|");
          }
        }
        emit(token);
        if (gotLink) {
          emit("]]");
        }
      }
    } while (token != null);

    return converted.toString();
  }

  private void emit(char ch) {
    converted.append(ch);
  }

  private void emit(String s) {
    converted.append(s);
  }

  /**
   * Token start is first Letter encountered that is not already a in a link - ie contained in [[...]].
   */
  private void emitToTokenStart() {
    boolean startToken = false;
    boolean skippingLink = false;
    while (!startToken && index < sourceLen) {
      char ch = source.charAt(index);
      if (skippingLink) {
        emit(ch);
        index++;
        if (ch == ']') {
          if (index < sourceLen && source.charAt(index) == ']') {
            skippingLink = false;
            //Emit the second ]].
            emit(ch);
            index++;
          }
        }
      } else {
        if (ch == '[') {
          emit(ch);
          index++;
          if (index < sourceLen && source.charAt(index) == '[') {
            skippingLink = true;
            //Emit the second [ 
            emit(ch);
            index++;
          }
        } else {
          startToken = Character.isLetter(ch);
          if (!startToken) {
            emit(ch);
            index++;
          }
        }
      }
    }
  }

  private String extractToken() {
    if (index >= sourceLen) {
      return null;
    }

    StringBuffer token = new StringBuffer();
    boolean endToken = false;

    while (!endToken && index < sourceLen) {
      char ch = source.charAt(index);
      endToken = !Character.isLetterOrDigit(ch);
      if (!endToken) {
        token.append(ch);
        index++;
      }
    }

    return token.toString();
  }
}
