/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixwiki;

import com.cameronedge.fixrepo.RepoInfo;
import com.cameronedge.fixrepo.Util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author John Cameron
 */
public class ConvertToWikiText {
  public ConvertToWikiText(File textFile, File wikiFile, RepoInfo repoInfo) throws IOException {
    LinkDetector linkDetector = new LinkDetector(repoInfo, 0);

    InputStream is = new FileInputStream(textFile);
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

    //Format the read text into wikiText.
    String wikiText = Util.formatDescription(sbuf.toString(), linkDetector);

    //Write wikiText to output.
    Writer wikiWriter = new FileWriter(wikiFile);
    wikiWriter.write(wikiText);
    wikiWriter.close();
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      usage();
      System.exit(0);
    }

    File repoDir = new File(args[0]);
    File textFile = new File(args[1]);
    if (!textFile.exists()) {
      System.out.println();
      System.out.println("Input file does not exist " + textFile);
      System.out.println();
      System.exit(1);
    }

    File wikiFile = new File(args[2]);

    RepoInfo repoInfo = new RepoInfo(repoDir);

    new ConvertToWikiText(textFile, wikiFile, repoInfo);
  }

  private static void usage() {
    System.out.println();
    System.out.println("com.cameronedge.fixwiki.ConvertToWikiText <FIX repository> <TextFile> <WikiTextFile>");
    System.out.println();
    System.out.println("Converts a text file into a wiki format text file - including links.");
    System.out.println("The text in the table is scanned for FIXwiki links.");
    System.out.println("<FIX repository>   - a directory containing a FIX repository");
    System.out.println("<TextFile>         - the text file.");
    System.out.println("<WikiTextFile>     - the file corresponding wiki text.");
    System.out.println();
  }
}