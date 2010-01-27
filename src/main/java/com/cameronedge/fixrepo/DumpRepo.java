/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixrepo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Creates a RepoInfo then dumps it in repo style files.
 *
 * @author John Cameron
 */
public class DumpRepo {

  private static final String BLANKS = "                                                         ";
  private static final int INDENT_INCREMENT = 3;
  private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
          "<dataroot>";
  private static final String XML_FOOTER = "</dataroot>";

  private int indent;

  public DumpRepo(File outDir, RepoInfo repoInfo) throws IOException {

    //Components.xml
    dumpXMLFile(outDir, repoInfo.getComponentInfos(), "Components", "Components");

    //DataTypes.xml
    dumpXMLFile(outDir, repoInfo.getTypeInfos(), "DataTypes", "Datatype");

    //Enums.xml
    dumpXMLFile(outDir, repoInfo.getEnumInfos(), "Enums", "Enums");

    //Fields.xml
    dumpXMLFile(outDir, repoInfo.getFieldInfos(), "Fields", "Fields");

    //MsgContents.xml
    dumpXMLFile(outDir, repoInfo.getSegmentInfos(), "MsgContents", "MsgContents");

    //MsgType.xml
    dumpXMLFile(outDir, repoInfo.getMessageInfos(), "MsgType", "MsgType");

  }

  private void dumpXMLFile(File outDir, Map<String, List<Properties>> fieldInfos, String fileRoot, String baseElement)
          throws IOException {
    String fname = outDir.getAbsolutePath() + File.separator + fileRoot + ".xml";
    File f = new File(fname);
    File parent = f.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }

    PrintWriter os = new PrintWriter(f);

    os.println(XML_HEADER);

    indent = INDENT_INCREMENT;

    for (List<Properties> values : fieldInfos.values()) {
      for (Properties props : values) {
        openElement(os, baseElement, true);
        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
          String name = (String) names.nextElement();
          String value = props.getProperty(name);
          openElement(os, name, false);
          writeValue(os, value);
          closeElement(os, name, false);
        }
        closeElement(os, baseElement, true);
      }
    }

    os.println(XML_FOOTER);

    os.close();
  }

  private void writeValue(PrintWriter os, String value) throws IOException {

    int len = value.length();
    for (int i = 0; i < len; i++) {
      char c = value.charAt(i);
      switch (c) {
        case '>':
          os.print("&gt;");
          break;
        case '<':
          os.print("&lt;");
          break;
        case '&':
          os.print("&amp;");
          break;

        case '¥':
          os.print("*");
          break;

        default:
          if (c < 255) {
            os.print(c);
          } else {
            os.print(" ");
          }
      }
    }

  }

  private void openElement(PrintWriter os, String element, boolean forceNewline) throws IOException {
    boolean newLine = forceNewline || "Desc".equals(element) || "Description".equals(element);
    os.print(BLANKS.substring(0, indent));
    os.print("<" + element + ">");
    if (newLine) {
      os.println();
    }

    indent += INDENT_INCREMENT;
  }

  private void closeElement(PrintWriter os, String element, boolean forceNewline) throws IOException {
    indent -= INDENT_INCREMENT;

    boolean newLine = forceNewline || "Desc".equals(element) || "Description".equals(element);
    if (newLine) {
      os.println();
      os.print(BLANKS.substring(0, indent));
    }
    os.println("</" + element + ">");
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      usage();
      System.exit(0);
    }

    File repoDir = new File(args[0]);
    File outDir = new File(args[1]);
    System.out.println(outDir.getAbsolutePath());

    RepoInfo repoInfo = new RepoInfo(repoDir);

    new DumpRepo(outDir, repoInfo);
  }

  private static void usage() {
    System.out.println();
    System.out.println("Usage:");
    System.out.println();
    System.out.println("com.cameronedge.fixrepo.DumpRepo <FIX repository> <Dump directory>");
    System.out.println();
    System.out.println("Writes enhanced repo files generated from the given FIX repository.");
    System.out.println("<FIX repository>   - a directory containing a FIX repository");
    System.out.println("<Dump directory>   - the directory where the enhanced repo files are written.");
    System.out.println();
  }

}
