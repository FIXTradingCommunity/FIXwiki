/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixwiki;

import com.cameronedge.fixrepo.RepoInfo;
import com.cameronedge.fixrepo.RepoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

import static com.cameronedge.fixwiki.FixwikiUtil.writeWikiTable;

/**
 * Need to preprocess by removing leading tab, adding tab to end of each row, converting arrows into < or >
 * @author John Cameron
 */
public class ConvertWordTableToWikiTable {
  public ConvertWordTableToWikiTable(File wordFile, File wikiFile, int nColumns, RepoInfo repoInfo) throws IOException {
    LinkDetector linkDetector = new LinkDetector(repoInfo, 0);
    
    FileInputStream is = new FileInputStream(wordFile);
    InputStreamReader reader = new InputStreamReader(is, Charset.defaultCharset());
    //Read file into String buffer.
    StringBuffer sbuf = new StringBuffer();
    char[] buffer = new char[8192];
    int nRead;
    do {
      nRead = reader.read(buffer);
      if (nRead > 0) {
        sbuf.append(buffer, 0, nRead);
      }
    } while (nRead > 0);
    reader.close();

    List<String[]> table = RepoUtil.parseMSWordTable(sbuf.toString(), nColumns);

    PrintWriter wikiWriter = new PrintWriter(wikiFile);
    writeWikiTable(wikiWriter, table, linkDetector);    
    wikiWriter.close();
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      usage();
      System.exit(0);
    }

    File repoDir = new File(args[0]);
    File wordFile = new File(args[1]);
    if (!wordFile.exists()) {
      System.out.println();
      System.out.println("Input file does not exist " + wordFile);
      System.out.println();
      System.exit(1);
    }
    File wikiFile = new File(args[2]);
    int nColumns = Integer.parseInt(args[3]);

    RepoInfo repoInfo = new RepoInfo(repoDir);

    new ConvertWordTableToWikiTable(wordFile, wikiFile, nColumns, repoInfo);
  }

  private static void usage() {
    System.out.println();
    System.out.println("Usage:");
    System.out.println();
    System.out.println("com.cameronedge.fixwiki.ConvertWordTableToWikiTable <FIX repository> <WordTableFile> <WikiTableFile> <nColumns>");
    System.out.println();
    System.out.println("Converts a text file containing a pasted MS Word Table into a file containing a table in" +
            "wiki format.");
    System.out.println("The text in the table is scanned for FIXwiki links.");
    System.out.println("<FIX repository>   - a directory containing a FIX repository");
    System.out.println("<WordTableFile>    - the text file containing the pasted MS Word table.");
    System.out.println("<WikiTableFile>    - the file coresponding wiki text.");
    System.out.println("<nColumns>         - the number of columns in the table.");
    System.out.println();
  }
}
