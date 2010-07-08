/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixwiki;

import com.cameronedge.fixrepo.RepoInfo;
import com.cameronedge.fixrepo.RepoUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Writes names of all FIXwiki generated pages into a number of files.
 * @author John Cameron
 */
public class DumpPageNames {
  private RepoInfo repoInfo;
  private File outDir;

  public DumpPageNames(File outDir, RepoInfo repoInfo) throws FileNotFoundException {
    this.repoInfo = repoInfo;
    this.outDir = outDir;

//    generateConstantPageNames();

//    generateFieldPageNames();

    generateValuePageNames();

//    generateMessagePageNames();

//    generateComponentPageNames();

//    generateTypePageNames();
  }

  private void generateValuePageNames() throws FileNotFoundException {
    Map<String, List<Properties>> fieldInfos = repoInfo.getFieldInfos();
    Map<String, List<Properties>> enumInfos = repoInfo.getEnumInfos();

    String fname = outDir.getAbsolutePath() + File.separator + "ValuePages.txt";
    PrintWriter pw = new PrintWriter(fname);
    for (Map.Entry<String, List<Properties>> fieldInfoEntry : fieldInfos.entrySet()) {
      String fieldTag = fieldInfoEntry.getKey();

      Properties fieldProps = fieldInfoEntry.getValue().get(0);

      String fieldName = fieldProps.getProperty("Name");

      String enumTag;
      String usesEnumsFromTag = fieldProps.getProperty("AssociatedDataTag");
      if (usesEnumsFromTag != null) {
        enumTag = usesEnumsFromTag;
      } else {
        enumTag = fieldTag;
      }

      //Get the enumerated values for this field, if any.
      List<Properties> values = enumInfos.get(enumTag);
      if (values != null) {
        for (Properties props : values) {

          String enumValue = props.getProperty("Value");

          String enumName = props.getProperty("EnumName");

          //Create value subpages from field name and enum name and enum value.
          String userTitle = fieldName + "/" + enumValue + " " + enumName;
          String fplTitle = RepoUtil.computeFPLTitle(userTitle);

          pw.println(userTitle);
          pw.println(fplTitle);

        }
      }
    }
    pw.close();
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

    new DumpPageNames(outDir, repoInfo);
  }

  private static void usage() {
    System.out.println();
    System.out.println("Usage:");
    System.out.println();
    System.out.println("com.cameronedge.fixrepo.DumpPageNames <FIX repository> <Dump directory>");
    System.out.println();
    System.out.println("Writes FIXwiki page names generated from the given FIX repository.");
    System.out.println("<FIX repository>   - a directory containing a FIX repository");
    System.out.println("<Dump directory>   - the directory where the page name files are written.");
    System.out.println();
  }
}
