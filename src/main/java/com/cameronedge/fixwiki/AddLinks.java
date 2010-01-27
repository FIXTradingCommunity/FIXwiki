/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixwiki;

import com.cameronedge.fixrepo.RepoInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * @author John Cameron
 */
public class AddLinks {
  public AddLinks(File inputFile, File outputFile, RepoInfo repoInfo, int matchTagValues) throws IOException {
    LinkDetector linkDetector = new LinkDetector(repoInfo, matchTagValues);

    InputStream is = new FileInputStream(inputFile);
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

    String outputText = linkDetector.convert(sbuf.toString());

    //Write converted text to output.
    Writer writer = new FileWriter(outputFile);
    writer.write(outputText);
    writer.close();
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      usage();
      System.exit(0);
    }

    File repoDir = new File(args[0]);
    File inFile = new File(args[1]);
    if (!inFile.exists()) {
      System.out.println();
      System.out.println("Input file does not exist " + inFile);
      System.out.println();
      System.exit(1);
    }

    File outFile = new File(args[2]);

    int matchTagValues = args.length < 4 ? 0 : Integer.parseInt(args[3]);

    RepoInfo repoInfo = new RepoInfo(repoDir);

    new AddLinks(inFile, outFile, repoInfo, matchTagValues);
  }

  private static void usage() {
    System.out.println();
    System.out.println("Usage:");
    System.out.println();
    System.out.println("com.cameronedge.fixwiki.AddLinks <FIX repository> <InTextFile> <OutTextFile> [matchTagValues]");
    System.out.println();
    System.out.println("Converts input text file into an output text file including links from Repo.");
    System.out.println("<FIX repository>   - a directory containing a FIX repository");
    System.out.println("<InTextFile>       - the input text file.");
    System.out.println("<OutTextFile>      - the output text file.");
    System.out.println("matchTagValues     - If present, is the tag number whose value names should convert to links");
    System.out.println();
  }
}