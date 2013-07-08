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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.cameronedge.fixwiki.FixwikiUtil.formatDescription;

/**
 * Generates basic FIXwiki pages from FIX repository.
 *
 * @author John Cameron
 */
public class FixwikiGenerator {
  private static final String BUILD_SCRIPT = "buildFIXwiki.sh";
  private static boolean createUserPages = false;
  private LinkDetector linkDetector;
  private RepoInfo repoInfo;

  private static final String COMPONENT_CONTENT_INFO = "Component Content info";
  private static final String COMPONENT_INFO = "Component info";
  private static final String EXTENSION_PACK_NAME_PREFIX = "EP";
  private static final String FIELD_INFO = "Field info";
  private static final String FIX_NAMES_FILE_NAME = "FIXNames.txt";
  private static final String IMPORT_DIR_VARIABLE = "importdir";
  private static final String INVITATION_TO_POST = "Invitation to post";
  private static final String MESSAGE_CONTENT_INFO = "Message Content info";
  private static final String MESSAGE_INFO = "Message info";
  private static final int MIN_ONLINE_EXTENSION_PACK_NUMBER = 98;
  private static final String REFER_TO_USER_PAGE = "ReferToUserPage";
  private static final String TYPE_INFO = "Type info";
  private static final String UPLOAD_DIR_NAME = "uploads";
  private static final String VALUE_INFO = "Value info";
  private static final String FPL_PAGE_TERMINATION_STRING = "</includeonly><noinclude>{{" + REFER_TO_USER_PAGE + "}}</noinclude>";

  private boolean ignoreErrors;

  public FixwikiGenerator(RepoInfo repoInfo) {

    ignoreErrors = System.getProperty("ignoreErrors") != null;

    this.repoInfo = repoInfo;
    linkDetector = new LinkDetector(repoInfo, 0);
  }

  private void addImportToScript(PrintWriter script, String relName,
                                 String title, boolean overwrite) {
    String cmd = "php importTextFile.php " +
            (title == null ? "" : "--title '" + title + "' ") +
            (overwrite ? "" : "--nooverwrite ") +
            "$" + IMPORT_DIR_VARIABLE + File.separator + "'" + relName + "'";
    script.println(cmd);
  }

  private void addResourceImportToScript(File scriptDir, PrintWriter script, String resourceName, String title) throws IOException {
    addResourceToOutput(scriptDir, resourceName);

    addImportToScript(script, resourceName, title, true);
  }

  private void addResourceToOutput(File outputDir, String resourceName) throws IOException {
    String fname = outputDir.getAbsolutePath() + File.separator + resourceName;
    copyResourceToFile(resourceName, fname);
  }

  private void addUploadToScript(PrintWriter script, String subdirectoryName) {
    String cmd = "php importImages.php " +
            "--overwrite " +
            "$" + IMPORT_DIR_VARIABLE + File.separator + "'" + subdirectoryName + "'";
    script.println(cmd);
  }

  private void copyResourceToFile(String resourceName, String fname) throws IOException {
    File f = new File(fname);
    File parent = f.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
    FileOutputStream fos = new FileOutputStream(f);

    ClassLoader loader = this.getClass().getClassLoader();


    InputStream is = loader.getResourceAsStream(resourceName);
    if (is == null) {
      throw new RuntimeException("Missing resource file " + resourceName);
    }

    byte[] buffer = new byte[10000];
    int read;
    do {
      read = is.read(buffer);
      if (read > 0) {
        fos.write(buffer, 0, read);
      }
    } while (read >= 0);

    fos.close();
    is.close();
  }

  private static String epToName(String epNum) {
    return EXTENSION_PACK_NAME_PREFIX + epNum;
  }

  private static String epToName(Integer epNum) {
    return epToName(epNum.toString());
  }

  /**
   * Hint text (pop up mouse overs) cannot contain single quote characters or < or >.
   * <p/>
   * This replaces them with acceptable alternatives.
   *
   * @param text Text from which text is extracted.
   * @return Modified copy of text.
   */
  static String extractHint(String text) {
    String hint = null;
    if (text != null) {
      //Replace single quotes by space.
      hint = text.replace('\'', ' ');

      //Replace > by gt
      hint = hint.replaceAll(">", "gt");

      //Replace < by lt
      hint = hint.replaceAll("<", "lt");

    }
    return hint;
  }

  private void generate(File scriptDir) throws Exception {

    //Create script file.
    String fname = scriptDir.getAbsolutePath() + File.separator + BUILD_SCRIPT;
    PrintWriter script = new PrintWriter(new FileWriter(fname));
    script.println("#!/bin/sh");
    script.println(IMPORT_DIR_VARIABLE + "=`dirname $0`");

    generateConstantPages(scriptDir, script);

    generateFieldPages(scriptDir, script);

    generateValuePages(scriptDir, script);

    generateMessagePages(scriptDir, script);

    generateComponentPages(scriptDir, script);

    generateTypePages(scriptDir, script);
    
    generateExtensionPackPages(scriptDir, script);

    generateFIXNamesFile(scriptDir);

    doUploads(script);

    script.println("php rebuildall.php");

    script.close();

  }

  private void doUploads(PrintWriter script) {
    addUploadToScript(script, "images");
    addUploadToScript(script, UPLOAD_DIR_NAME);
  }

  private void generateConstantPages(File scriptDir, PrintWriter script) throws IOException {
    addResourceImportToScript(scriptDir, script, "MediaWiki/Disclaimers.wiki", "MediaWiki:Disclaimers");
    addResourceImportToScript(scriptDir, script, "MediaWiki/FixNames-desc.wiki", "MediaWiki:FixNames-desc");
    addResourceImportToScript(scriptDir, script, "MediaWiki/FixNames-url.wiki", "MediaWiki:FixNames-url");
    addResourceImportToScript(scriptDir, script, "MediaWiki/FixRepoError.wiki", "MediaWiki:FixRepoError");
    addResourceImportToScript(scriptDir, script, "MediaWiki/FixRepoError-url.wiki", "MediaWiki:FixRepoError-url");
    addResourceImportToScript(scriptDir, script, "MediaWiki/FixSpec.wiki", "MediaWiki:FixSpec");
    addResourceImportToScript(scriptDir, script, "MediaWiki/FixSpec-url.wiki", "MediaWiki:FixSpec-url");
    addResourceImportToScript(scriptDir, script, "MediaWiki/FixSpecError.wiki", "MediaWiki:FixSpecError");
    addResourceImportToScript(scriptDir, script, "MediaWiki/FixSpecError-url.wiki", "MediaWiki:FixSpecError-url");
    addResourceImportToScript(scriptDir, script, "MediaWiki/Loginreqpagetext.wiki", "MediaWiki:Loginreqpagetext");
    addResourceImportToScript(scriptDir, script, "MediaWiki/Logouttext.wiki", "MediaWiki:Logouttext");
    addResourceImportToScript(scriptDir, script, "MediaWiki/Mainpage.wiki", "MediaWiki:Mainpage");
    addResourceImportToScript(scriptDir, script, "MediaWiki/Privacy.wiki", "MediaWiki:Privacy");
    addResourceImportToScript(scriptDir, script, "MediaWiki/Loginreqpagetext.wiki", "MediaWiki:Loginreqpagetext");
    addResourceImportToScript(scriptDir, script, "MediaWiki/Searchresulttext.wiki", "MediaWiki:Searchresulttext");
    addResourceImportToScript(scriptDir, script, "MediaWiki/Sidebar.wiki", "MediaWiki:Sidebar");
    addResourceImportToScript(scriptDir, script, "FIXwiki/About.wiki", "FIXwiki:About");
    addResourceImportToScript(scriptDir, script, "FIXwiki/Copyrights.wiki", "FIXwiki:Copyrights");
    addResourceImportToScript(scriptDir, script, "FIXwiki/FIXNames.wiki", "FIXwiki:FIX Names");
    addResourceImportToScript(scriptDir, script, "FIXwiki/FIXwiki.wiki", "FIXwiki");
    addResourceImportToScript(scriptDir, script, "FIXwiki/Structure.wiki", "FIXwiki:Structure");
    addResourceImportToScript(scriptDir, script, "FIXwiki/Use.wiki", "FIXwiki:Use");
    addResourceImportToScript(scriptDir, script, "fpl/aboutFPL.wiki", "FPL:About FIX Protocol Limited (FPL)");
    addResourceImportToScript(scriptDir, script, "fpl/CFICodeUsage.wiki", "FPL:CFICode usage");
    addResourceImportToScript(scriptDir, script, "fpl/CommonComponents.wiki", "FPL:Common Components");
    addResourceImportToScript(scriptDir, script, "fpl/CommonMessages.wiki", "FPL:Common Messages");
    addResourceImportToScript(scriptDir, script, "fpl/CurrencyCodes.wiki", "FPL:Currency Codes");
    addResourceImportToScript(scriptDir, script, "fpl/disclaimer.wiki", "FPL:General Disclaimer");
    addResourceImportToScript(scriptDir, script, "fpl/ExchangeCodes.wiki", "FPL:Exchange Codes");
    addResourceImportToScript(scriptDir, script, "fpl/FIXMLSyntax.wiki", "FPL:FIXML Syntax");
    addResourceImportToScript(scriptDir, script, "fpl/FIXUsageNotes.wiki", "FPL:FIX Usage Notes");
    addResourceImportToScript(scriptDir, script, "fpl/history.wiki", "FPL:History of FIX");
    addResourceImportToScript(scriptDir, script, "fpl/OtherStandards.wiki", "FPL:Other Standards");
    addResourceImportToScript(scriptDir, script, "fpl/PartiesReferenceData.wiki", "FPL:Parties Reference Data");
    addResourceImportToScript(scriptDir, script, "fpl/PosttradeMessages.wiki", "FPL:Post-trade messages");
    addResourceImportToScript(scriptDir, script, "fpl/PretradeMessages.wiki", "FPL:Pre-trade messages");
    addResourceImportToScript(scriptDir, script, "fpl/ProductMarketDataModel.wiki", "FPL:Product Reference and Market Structure Data Model");
    addResourceImportToScript(scriptDir, script, "fpl/QuotingModels.wiki", "FPL:Quoting Models");
    addResourceImportToScript(scriptDir, script, "fpl/reproduction.wiki", "FPL:Reproduction of Specification");
    addResourceImportToScript(scriptDir, script, "fpl/SecurityReferenceDataScenarios.wiki", "FPL:Security Definition, Security Status, and Trading Session Message Scenarios");
    addResourceImportToScript(scriptDir, script, "fpl/Specification.wiki", "FPL:FIX Specification");
    addResourceImportToScript(scriptDir, script, "fpl/TagValueSyntax.wiki", "FPL:Tag Value Syntax");
    addResourceImportToScript(scriptDir, script, "fpl/TradeMessages.wiki", "FPL:Trade messages");
    addResourceImportToScript(scriptDir, script, "fpl/TraditionalSessionProtocol.wiki", "FPL:Traditional FIX Session Protocol");
    addResourceImportToScript(scriptDir, script, "fpl/TransportProtocols.wiki", "FPL:Transport Protocols");
    addResourceImportToScript(scriptDir, script, "templates/ComponentContentInfo.wiki", "Template:" + COMPONENT_CONTENT_INFO);
    addResourceImportToScript(scriptDir, script, "templates/ComponentInfo.wiki", "Template:" + COMPONENT_INFO);
    addResourceImportToScript(scriptDir, script, "templates/FieldInfo.wiki", "Template:" + FIELD_INFO);
    addResourceImportToScript(scriptDir, script, "templates/InvitationToPost.wiki", "Template:" + INVITATION_TO_POST);
    addResourceImportToScript(scriptDir, script, "templates/MessageContentInfo.wiki", "Template:" + MESSAGE_CONTENT_INFO);
    addResourceImportToScript(scriptDir, script, "templates/MessageInfo.wiki", "Template:" + MESSAGE_INFO);
    addResourceImportToScript(scriptDir, script, "templates/ReferToUserPage.wiki", "Template:" + REFER_TO_USER_PAGE);
    addResourceImportToScript(scriptDir, script, "templates/RepoError.wiki", "Template:RepoError");
    addResourceImportToScript(scriptDir, script, "templates/SpecError.wiki", "Template:SpecError");
    addResourceImportToScript(scriptDir, script, "templates/ToDo.wiki", "Template:Todo");
    addResourceImportToScript(scriptDir, script, "templates/TypeInfo.wiki", "Template:" + TYPE_INFO);
    addResourceImportToScript(scriptDir, script, "templates/ValueInfo.wiki", "Template:" + VALUE_INFO);
    addResourceImportToScript(scriptDir, script, "categories/FIX.2.7.wiki", "Category:FIX.2.7");
    addResourceImportToScript(scriptDir, script, "categories/FIX.3.0.wiki", "Category:FIX.3.0");
    addResourceImportToScript(scriptDir, script, "categories/FIX.4.0.wiki", "Category:FIX.4.0");
    addResourceImportToScript(scriptDir, script, "categories/FIX.4.1.wiki", "Category:FIX.4.1");
    addResourceImportToScript(scriptDir, script, "categories/FIX.4.2.wiki", "Category:FIX.4.2");
    addResourceImportToScript(scriptDir, script, "categories/FIX.4.3.wiki", "Category:FIX.4.3");
    addResourceImportToScript(scriptDir, script, "categories/FIX.4.4.wiki", "Category:FIX.4.4");
    addResourceImportToScript(scriptDir, script, "categories/FIX.5.0.wiki", "Category:FIX.5.0");
    addResourceImportToScript(scriptDir, script, "categories/FIXT.1.1.wiki", "Category:FIXT.1.1");
    addResourceImportToScript(scriptDir, script, "categories/FIX.5.0SP1.wiki", "Category:FIX.5.0SP1");
    addResourceImportToScript(scriptDir, script, "categories/FIX.5.0SP2.wiki", "Category:FIX.5.0SP2");    
    addResourceImportToScript(scriptDir, script, "categories/message.wiki", "Category:Message");
    addResourceImportToScript(scriptDir, script, "categories/component.wiki", "Category:Component");
    addResourceImportToScript(scriptDir, script, "categories/field.wiki", "Category:Field");
    addResourceImportToScript(scriptDir, script, "categories/RepoError.wiki", "Category:RepoError");
    addResourceImportToScript(scriptDir, script, "categories/SpecError.wiki", "Category:SpecError");
    addResourceImportToScript(scriptDir, script, "categories/type.wiki", "Category:Type");
    addResourceImportToScript(scriptDir, script, "categories/value.wiki", "Category:Value");
    addResourceImportToScript(scriptDir, script, "categories/extensionpack.wiki", "Category:ExtensionPack");
    addResourceImportToScript(scriptDir, script, "help/Editing.wiki", "Help:Editing");
    addResourceImportToScript(scriptDir, script, "help/Searching.wiki", "Help:Searching");

    addResourceToOutput(scriptDir, "images/CustomizableSchemaFiles.png");
    addResourceToOutput(scriptDir, "images/ExtensibilityPattern.png");
    addResourceToOutput(scriptDir, "images/FIXMLRootElement.png");
    addResourceToOutput(scriptDir, "images/LayersFIXMLSchema.png");
    addResourceToOutput(scriptDir, "images/PartiesReferenceDataNormalMessageFlow.png");
    addResourceToOutput(scriptDir, "images/PartyDetailsListReportMessageStructure.png");
    addResourceToOutput(scriptDir, "images/PartyReferenceStructure.png");
    addResourceToOutput(scriptDir, "images/ProductMarketDataModelFig1.png");
    addResourceToOutput(scriptDir, "images/ProductMarketDataModelFig2.png");
    addResourceToOutput(scriptDir, "images/ProductMarketDataModelFig3.png");
    addResourceToOutput(scriptDir, "images/ProductMarketDataModelFig4.png");
    addResourceToOutput(scriptDir, "images/ProductMarketDataModelFig5.png");
    addResourceToOutput(scriptDir, "images/RelatedPartyComponentStructure.png");
    addResourceToOutput(scriptDir, "images/SchemaFileHierarchy.png");
    addResourceToOutput(scriptDir, "images/SchemaFileNaming.png");
    addResourceToOutput(scriptDir, "images/UserDefinedSpreadMessageFlow.png");
    addResourceToOutput(scriptDir, "images/UserDefinedSpreadOneStepProcess.png");
    addResourceToOutput(scriptDir, "images/UserDefinedSpreadTwoStepProcess.png");

    generateFIXVersionPlusTemplates(scriptDir, script);
  }

  private void generateExtensionPackPages(File scriptDir, PrintWriter script) throws IOException {
    
    Set<Integer> epNums = repoInfo.getExtensionPacks();

    for (Integer epNum : epNums) {

      String epName = epToName(epNum);
      String epTitle = "Category:" + epName;
      String relName = epName + ".ep";
      String fname = scriptDir.getAbsolutePath() + File.separator + relName;
      PrintWriter fw = new PrintWriter(new FileWriter(fname));
      
      //Not all EP info is online
      if (epNum < MIN_ONLINE_EXTENSION_PACK_NUMBER) {
        fw.println("Details of extension packs prior to " + 
                epToName(MIN_ONLINE_EXTENSION_PACK_NUMBER) + 
                " are not available on the FPL website.");
      } else {
        fw.println("See http://www.fixprotocol.org/specifications/" + epName);
      }

      fw.println("[[Category:ExtensionPack]]");
      fw.close();

      //Write to script file
      addImportToScript(script, relName, epTitle, true);

    }
  }

  private void generateFIXNamesFile(File outputDir) throws FileNotFoundException {
    String fname = outputDir.getAbsolutePath() + File.separator + UPLOAD_DIR_NAME +
            File.separator + FIX_NAMES_FILE_NAME;
    File f = new File(fname);
    File parent = f.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
    PrintWriter pw = new PrintWriter(new FileOutputStream(f));

    Set<String> names = new HashSet<String>();
    
    //Extract all repo names.

    //Type and component names can come from key set.
    Map<String, List<Properties>> typeInfos = repoInfo.getTypeInfos();
    names.addAll(typeInfos.keySet());

    //Add in message names. Use messageInfos for that.
    Map<String, List<Properties>> messageInfos = repoInfo.getMessageInfos();
    //We need to iterate since the key to this map is msgType.
    for (List<Properties> values : messageInfos.values()) {
      Properties props = values.get(0); //Message only has only one Properties.
      String messageName = props.getProperty(RepoInfo.PROP_MESSAGE_NAME);
      names.add(messageName);
    }

    //Type and component names can come from key set.
    Map<String, List<Properties>> componentInfos = repoInfo.getComponentInfos();
    names.addAll(componentInfos.keySet());

    //Add in enum names. Use enumInfos for that.
    Map<String, List<Properties>> enumInfos = repoInfo.getEnumInfos();
    //We need to iterate since the key to this map is tag.
    for (List<Properties> values : enumInfos.values()) {
      //Now iterate for each enumerated value
      for (Properties props : values) {
        String enumName = props.getProperty(RepoInfo.PROP_ENUM_NAME);
        names.add(enumName);        
      }
    }
    
    //Field names from the key set of fieldNameTagMap.
    Map<String, Integer> fieldNameTagMap = repoInfo.getFieldNameTagMap();
    names.addAll(fieldNameTagMap.keySet());

    //Only print names of length 3 or longer
    int nSpellers = 0;
    for (String name : names) {
      if (name.length() >= 3) {
        pw.println(name);
        nSpellers++;
      }
    }
    System.out.println("INFO: " + nSpellers + " names in FIXNames.txt");

    pw.close();
  }

  private void generateFIXVersionPlusTemplates(File scriptDir, PrintWriter script) throws IOException {
    for (int i = 0; i <= RepoInfo.latestFIXVersionIndex; i++) {
      String fixVersion = repoInfo.getFIXVersionString(i);

      //Create file from fixversion name.
      String plusName = fixVersion + "+";
      String relName = plusName + ".tem";
      String fname = scriptDir.getAbsolutePath() + File.separator + relName;
      PrintWriter fw = new PrintWriter(new FileWriter(fname));

      for (int j = i; j <= RepoInfo.latestFIXVersionIndex; j++) {
        fw.print("[[Category:" + repoInfo.getFIXVersionString(j) + "]]");
      }
      fw.println();
      fw.close();

      //Update script
      addImportToScript(script, relName, "Template:" + plusName, true);
    }
  }

  private void generateFieldPages(File scriptDir, PrintWriter script) throws Exception {
    Map<String, List<Properties>> fieldInfos = repoInfo.getFieldInfos();

    //Iterate through fieldNames generating field pages.
    //Note that fieldNames contains ALL field names - for all versions - not just the latest version.
    //Doing it this way allows us to track field name changes - redirecting old name to new name.
    Map<String, Integer> fieldNameTagMap = repoInfo.getFieldNameTagMap();
    for (Map.Entry<String, Integer> fieldNameTagMapEntry : fieldNameTagMap.entrySet()) {

      String fieldName = fieldNameTagMapEntry.getKey();
      int fieldTag = fieldNameTagMapEntry.getValue();

      //Look up fieldInfo from tag.
      List<Properties> fieldInfo = fieldInfos.get(Integer.toString(fieldTag));
      if (fieldInfo == null) {
        String errmess = "Inconsistency in repository - missing information for field " + fieldTag;
        if (ignoreErrors) {
          System.out.println("ERROR: " + errmess);
        } else {
          throw new Exception(errmess);
        }
      } else {
        Properties props = fieldInfo.get(0);

        String currentFieldName = props.getProperty(RepoInfo.PROP_FIELD_NAME);
        if (!currentFieldName.equalsIgnoreCase(fieldName)) {
          //Create redirect page for old field name.

          //Ignore old field names containing '(' - this is always some note like (no longer used) or (replaced)
          if (fieldName.indexOf('(') < 0) {

//          System.out.println("Redirecting old field name " + fieldName + "->" + currentFieldName);

            //Old field name redirection page
            //Create file from old field name.
            String relName = fieldName + ".fld";
            String fname = scriptDir.getAbsolutePath() + File.separator + relName;
            PrintWriter fw = new PrintWriter(new FileWriter(fname));
            fw.println("#REDIRECT [[" + currentFieldName + "]]");
            fw.close();

            //Write to script file
            addImportToScript(script, relName, fieldName, true);
          }

        } else {
          //Create new field page for current field name.

          //Field page
          String userTitle = fieldName;
          String fplTitle = RepoUtil.computeFPLTitle(userTitle);

          //Create file from field name.
          String relName = titleToName(fplTitle) + ".fld";
          String fname = scriptDir.getAbsolutePath() + File.separator + relName;
          PrintWriter fw = new PrintWriter(new FileWriter(fname));

          //Write field info transclusion passing each property as a parameter.
          fw.println("<includeonly>");
          fw.println("{{Field info");
          for (Map.Entry<Object, Object> entry : props.entrySet()) {
            writeWikiTemplateParameter(fw, (String) entry.getKey(), (String) entry.getValue());
          }

          fw.println("}}");

          writeEPCategory(props, fw);
          
          writeFIXVersionCategories(props, fw);

          writeMessageAndComponentCategories(Integer.toString(fieldTag), fw);
          fw.println(FPL_PAGE_TERMINATION_STRING);
          fw.close();

          //Write to script file
          addImportToScript(script, relName, fplTitle, true);

          writeUserVersion(scriptDir, script, userTitle, fplTitle, ".fld");


          //Field tag redirection page
          //Create file from field tag.
          relName = fieldTag + ".fld";
          fname = scriptDir.getAbsolutePath() + File.separator + relName;
          fw = new PrintWriter(new FileWriter(fname));
          fw.println("#REDIRECT [[" + fieldName + "]]");
          fw.close();

          //Write to script file
          addImportToScript(script, relName, Integer.toString(fieldTag), true);
        }
      }

      //I don't think that AbbrNames are worth the effort. Could add special disambiguation page afterwards
      //if required.
      //If AbbrName != FieldName, add another redirect page.
//      if (abbrName != null && !fieldName.equalsIgnoreCase(abbrName)) {
//          System.out.println(fieldName + " or " + abbrName);
//          //Create file from field abbrName.
//          //Check that we are not overwriting an existing field.
//          fname = scriptDir.getAbsolutePath() + File.separator + abbrName + ".fld";
//          File abbrFile = new File(fname);
//          if (abbrFile.exists()) {
//            System.out.println("WARNING: AbbrName clashes with existing name - " + abbrName );
//          }
//          fw = new PrintWriter(new FileWriter(abbrFile));
//          fw.println("#REDIRECT [[" + fieldName + "]]");
//          fw.close();
//
//          //Write to script file
//          addImportToScript(script, fname);
//      }
    }
  }

  private void generateMessagePages(File scriptDir, PrintWriter script) throws Exception {
    Map<String, List<Properties>> messageInfos = repoInfo.getMessageInfos();
    Map<String, Integer[]> messageVersionInfos = repoInfo.getMessageVersionInfos();

    String fname;//Now iterate through messageInfos generating message pages.
    for (List<Properties> values : messageInfos.values()) {
      Properties props = values.get(0); //Message only has only one Properties.

      String messageName = getCleanProperty(props, RepoInfo.PROP_MESSAGE_NAME);

      String userTitle = messageName;
      String fplTitle = RepoUtil.computeFPLTitle(messageName);

      //Message page
      //Create file from message name.
      String relName = titleToName(fplTitle) + ".msg";
      fname = scriptDir.getAbsolutePath() + File.separator + relName;
      PrintWriter fw = new PrintWriter(new FileWriter(fname));

      //Write message info transclusion passing each property as a parameter.
      fw.println("<includeonly>");
      fw.println("{{Message info");
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        writeWikiTemplateParameter(fw, (String) entry.getKey(), (String) entry.getValue());
      }

      //Note that it is a Message rather than a Component
      fw.println("| SegmentType=Message");

      fw.println("}}");

      writeEPCategory(props, fw);

      fw.println(FPL_PAGE_TERMINATION_STRING);
      fw.close();

      //Write to script file
      addImportToScript(script, relName, fplTitle, true);

      writeUserVersion(scriptDir, script, userTitle, fplTitle, ".msg");


      //Message category page
      //Create file from message name.
      relName = messageName + ".cat";
      fname = scriptDir.getAbsolutePath() + File.separator + relName;
      fw = new PrintWriter(new FileWriter(fname));
      //Just redirect to message page.
      fw.println("#REDIRECT [[" + userTitle + "]]");
      fw.close();
      //Write to script file
      addImportToScript(script, relName, "Category:" + messageName, true);


      //Write contents subpages based on FIX versions.
      //Look up associated message version info.
      String msgType = props.getProperty("MsgType");
      Integer[] versionInfo = messageVersionInfos.get(msgType);

      int fromVersion = -1;
      for (int i = 0; i < versionInfo.length; i++) {
        Integer fixVersion = versionInfo[i];

        if (fromVersion == -1) {
          //Looking for start of version range.
          if (fixVersion != null) {
            fromVersion = fixVersion;
          }
        } else {
          //Looking for end of version range
          if (fixVersion != null && fixVersion == fromVersion) {
            //Still on same version. Just keep looking for end of range.
          } else {
            //Version has changed, so reached end of range.
            int toVersion = i - 1;

            //Hack to avoid counting FIXT as version in this context.
            if (toVersion == RepoInfo.fixTVersionIndex) {
              toVersion--;
            }

            //Now create a message contents subpage constructed from the Message and its fromVersion and toVersion.
            userTitle = messageName + "/";
            if (fromVersion == toVersion) {
              userTitle += repoInfo.getFIXVersionString(fromVersion);
            } else {
              userTitle += repoInfo.getFIXVersionString(fromVersion) + "-" + repoInfo.getFIXVersionSuffix(toVersion);
            }
            fplTitle = RepoUtil.computeFPLTitle(userTitle);
            relName = titleToName(fplTitle) + ".msg";
            fname = scriptDir.getAbsolutePath() + File.separator + relName;

            writeSegmentFile(fname, messageName, msgType, fromVersion, toVersion, toVersion);

            //Write to script file
            addImportToScript(script, relName, fplTitle, true);

            writeUserVersion(scriptDir, script, userTitle, fplTitle, ".msg");

            //Give up if no longer exists from this version
            if (fixVersion == null) {
              fromVersion = -1;
              break;
            }

            //Set fromVersion to current version and continue scanning.
            fromVersion = i;
          }
        }

      }

      //Create + subpage from any lingering fromVersion
      if (fromVersion >= 0) {
        userTitle = messageName + "/" + repoInfo.getFIXVersionString(fromVersion) + "+";
        fplTitle = RepoUtil.computeFPLTitle(userTitle);
        relName = titleToName(fplTitle) + ".msg";
        fname = scriptDir.getAbsolutePath() + File.separator + relName;

        writeSegmentFile(fname, messageName, msgType, fromVersion, -1, RepoInfo.latestFIXVersionIndex);

        //Write to script file
        addImportToScript(script, relName, fplTitle, true);

        writeUserVersion(scriptDir, script, userTitle, fplTitle, ".msg");
      }
    }

  }

  private void generateComponentPages(File scriptDir, PrintWriter script) throws Exception {
    Map<String, List<Properties>> componentInfos = repoInfo.getComponentInfos();
    Map<String, Integer[]> componentVersionInfos = repoInfo.getComponentVersionInfos();

    String fname;//Now iterate through componentInfos generating component pages.
    for (List<Properties> values : componentInfos.values()) {
      Properties props = values.get(0); //Component only has only one Properties.

      String componentName = getCleanProperty(props, RepoInfo.PROP_COMPONENT_NAME);

      //Component page
      String userTitle = componentName;
      String fplTitle = RepoUtil.computeFPLTitle(userTitle);

      //Create file from component name.
      String relName = titleToName(fplTitle) + ".cmp";
      fname = scriptDir.getAbsolutePath() + File.separator + relName;
      PrintWriter fw = new PrintWriter(new FileWriter(fname));

      //Write component info transclusion passing each property as a parameter.
      fw.println("<includeonly>");
      fw.println("{{" + COMPONENT_INFO);
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        writeWikiTemplateParameter(fw, (String) entry.getKey(), (String) entry.getValue());
      }

      //Note that it is a Component rather than a Message
      fw.println("| SegmentType=Component");

      fw.println("}}");

      writeEPCategory(props, fw);

      writeMessageAndComponentCategories(componentName, fw);
      fw.println(FPL_PAGE_TERMINATION_STRING);
      fw.close();

      //Write to script file
      addImportToScript(script, relName, fplTitle, true);

      writeUserVersion(scriptDir, script, userTitle, fplTitle, ".cmp");


      //Component category page
      //Create file from component name.
      relName = componentName + ".cat";
      fname = scriptDir.getAbsolutePath() + File.separator + relName;
      fw = new PrintWriter(new FileWriter(fname));
      //Just redirect to component page.
      fw.println("#REDIRECT [[" + userTitle + "]]");
      fw.close();
      //Write to script file
      addImportToScript(script, relName, "Category:" + componentName, true);

      //Write contents subpages based on FIX versions.
      //Look up associated message version info.
      Integer[] versionInfo = componentVersionInfos.get(componentName);

      int fromVersion = -1;
      for (int i = 0; i < versionInfo.length; i++) {
        Integer fixVersion = versionInfo[i];

        if (fromVersion == -1) {
          //Looking for start of version range.
          if (fixVersion != null) {
            fromVersion = fixVersion;
          }
        } else {
          //Looking for end of version range
          if (fixVersion != null && fixVersion == fromVersion) {
            //Still on same version. Just keep looking for end of range.
          } else {
            //Version has changed, so reached end of range.
            int toVersion = i - 1;

            //Hack to avoid counting FIXT as version in this context.
            if (toVersion == RepoInfo.fixTVersionIndex) {
              toVersion--;
            }

            //Now create a component contents subpage constructed from the Component and its fromVersion and toVersion.
            userTitle = componentName + "/";
            if (fromVersion == toVersion) {
              userTitle += repoInfo.getFIXVersionString(fromVersion);
            } else {
              userTitle += repoInfo.getFIXVersionString(fromVersion) + "-" + repoInfo.getFIXVersionSuffix(toVersion);
            }
            fplTitle = RepoUtil.computeFPLTitle(userTitle);
            relName = titleToName(fplTitle) + ".cmp";
            fname = scriptDir.getAbsolutePath() + File.separator + relName;

            writeSegmentFile(fname, componentName, null, fromVersion, toVersion, toVersion);

            //Write to script file
            addImportToScript(script, relName, fplTitle, true);

            writeUserVersion(scriptDir, script, userTitle, fplTitle, ".cmp");

            //Give up if no longer exists from this version
            if (fixVersion == null) {
              fromVersion = -1;
              break;
            }
            
            //Set fromVersion to current version and continue scanning.
            fromVersion = i;
          }
        }

      }

      //Create + subpage from any lingering fromVersion
      if (fromVersion >= 0) {
        userTitle = componentName + "/" + repoInfo.getFIXVersionString(fromVersion) + "+";
        fplTitle = RepoUtil.computeFPLTitle(userTitle);
        relName = titleToName(fplTitle) + ".cmp";
        fname = scriptDir.getAbsolutePath() + File.separator + relName;

        writeSegmentFile(fname, componentName, null, fromVersion, -1, RepoInfo.latestFIXVersionIndex);

        //Write to script file
        addImportToScript(script, relName, fplTitle, true);

        writeUserVersion(scriptDir, script, userTitle, fplTitle, ".cmp");
      }
    }
  }

  private void generateValuePages(File scriptDir, PrintWriter script) throws Exception {
    Map<String, List<Properties>> fieldInfos = repoInfo.getFieldInfos();
    Map<String, List<Properties>> enumInfos = repoInfo.getEnumInfos();

    String fname;//Now iterate through fieldInfos generating value pages for those fields with enumerated values.
    for (Map.Entry<String, List<Properties>> fieldInfoEntry : fieldInfos.entrySet()) {
      String fieldTag = fieldInfoEntry.getKey();

      Properties fieldProps = fieldInfoEntry.getValue().get(0);

      String fieldName = fieldProps.getProperty(RepoInfo.PROP_FIELD_NAME);

      String enumTag;
      String usesEnumsFromTag = fieldProps.getProperty(RepoInfo.PROP_FIELD_USES_ENUMS_FROM_TAG);
      if (usesEnumsFromTag != null) {
        enumTag = usesEnumsFromTag;
      } else {
        enumTag = fieldTag;
      }

      //Get the enumerated values for this field, if any.
      List<Properties> values = enumInfos.get(enumTag);
      if (values != null) {
        for (Properties props : values) {

          String enumValue = props.getProperty(RepoInfo.PROP_ENUM_VALUE);

          String enumName = props.getProperty(RepoInfo.PROP_ENUM_NAME);

          //Create value subpages from field name and enum name and enum value.
          String userTitle = RepoUtil.computeValueTitle(fieldName, enumValue, enumName);
          String fplTitle = RepoUtil.computeFPLTitle(userTitle);

          //Write FPL version of the page.
          //Create file from title.
          String relName = titleToName(fplTitle) + ".val";
          fname = scriptDir.getAbsolutePath() + File.separator + relName;
          PrintWriter fw = new PrintWriter(new FileWriter(fname));

          //Write field info transclusion passing each property as a parameter.
          fw.println("<includeonly>");
          fw.println("{{" + VALUE_INFO);
          for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            //Override tag value which tag of actual field we are processing (we may be using another tag's enums).
            if ("Tag".equals(key)) {
              value = fieldTag;
            }
            writeWikiTemplateParameter(fw, key, value);
          }

          //Add extra FieldName property - redundant since we have tag, but probably convenient.
          fw.println("| " + RepoInfo.PROP_FIELD_NAME + "=" + fieldName);

          fw.println("}}");

          writeEPCategory(props, fw);

          writeFIXVersionCategories(props, fw);
          fw.println(FPL_PAGE_TERMINATION_STRING);
          fw.close();

          //Write to script file
          addImportToScript(script, relName, fplTitle, true);

          writeUserVersion(scriptDir, script, userTitle, fplTitle, ".val");

        }
      }
    }
  }

  private void generateTypePages(File scriptDir, PrintWriter script) throws Exception {
    Map<String, List<Properties>> typeInfos = repoInfo.getTypeInfos();

    String fname;//Now iterate through typeInfos generating type pages.
    for (List<Properties> values : typeInfos.values()) {
      Properties props = values.get(0); //Type only has only one Properties.

      String typeName = getCleanProperty(props, RepoInfo.PROP_TYPE_NAME);

      String userTitle = typeName + "DataType";
      String fplTitle = RepoUtil.computeFPLTitle(userTitle);

      //Type page
      //Create file from type name with DataType suffix so that it does not clash with Field names (eg Price).
      String relName = titleToName(fplTitle) + ".typ";
      fname = scriptDir.getAbsolutePath() + File.separator + relName;
      PrintWriter fw = new PrintWriter(new FileWriter(fname));

      //Write type info transclusion passing each property as a parameter.
      fw.println("<includeonly>");
      fw.println("{{" + TYPE_INFO);
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        writeWikiTemplateParameter(fw, (String) entry.getKey(), (String) entry.getValue());
      }

      fw.println("}}");

      writeEPCategory(props, fw);

      fw.println(FPL_PAGE_TERMINATION_STRING);
      fw.close();

      //Write to script file
      addImportToScript(script, relName, fplTitle, true);

      writeUserVersion(scriptDir, script, userTitle, fplTitle, ".typ");
    }
  }

  private static String getCleanProperty(Properties props, String key) {
    String s = props.getProperty(key);
    if (s != null) {
      s = s.trim();
      s = s.replace('/', ' ');
    }
    return s;
  }

  private void inviteInput(PrintWriter fw) {
    fw.println();
    fw.println("=Notes=");
    fw.println("<!-- The first person to post something here should delete this comment and the following line -->");
    fw.println("{{" + INVITATION_TO_POST + "}}");

  }

  private static String titleToName(String title) {
    title = title.replace(':', '_');
    return title.replace('/', '-');
  }

  private void writeSegmentFile(String fname, String name, String msgType,
                                int fromVersion, int toVersion, int fixVersion) throws Exception {
    List<Properties> segmentInfo;
    if (msgType != null) {
      segmentInfo = repoInfo.getSegmentInfoForMessage(msgType, fixVersion);
    } else {
      segmentInfo = repoInfo.getSegmentInfoForComponent(name, fixVersion);
    }
    if (segmentInfo != null) {
      PrintWriter fw;
      fw = new PrintWriter(new FileWriter(fname));

      fw.println("<includeonly>");
      fw.println("{{" + (msgType != null ? MESSAGE_CONTENT_INFO : COMPONENT_CONTENT_INFO));
      fw.println("| Name=" + name);
      if (msgType != null) {
        fw.println("| MsgType=" + msgType);
      }
      fw.println("| " + RepoInfo.PROP_ADDED_VERSION + "=" + repoInfo.getFIXVersionString(fromVersion));
      if (toVersion >= 0) {
        fw.println("| deprecated=" + repoInfo.getFIXVersionString(toVersion));
      }
      fw.println("}}");

      writeSegmentContents(fw, segmentInfo, fixVersion);

      writeFIXVersionCategories(fromVersion, toVersion, fw);

      fw.println(FPL_PAGE_TERMINATION_STRING);
      fw.close();
    }
  }

  private void writeEPCategory(Properties props, PrintWriter fw) throws Exception {
    String addedEP = props.getProperty(RepoInfo.PROP_ADDED_EXTENSION_PACK);
    if (addedEP != null) {
      fw.println("[[Category:" + epToName(addedEP) + "]]");
    }
  }
  
  private void writeFIXVersionCategories(Properties props, PrintWriter fw) throws Exception {
    //Compute valid FIX versions from added property (gives start) and any deprecated property (gives end).
    //Undeprecated always have form {{FIX.x.y+}}.
    //Deprecated just have explicit list of categories eg [[Category:FIX.4.1]][[Category:FIX.4.2]]
    //Check for deprecated property
    int toVersion;
    String deprecated = getCleanProperty(props, "deprecated");
    if (deprecated == null || deprecated.length() == 0) {
      toVersion = -1;
    } else {
      toVersion = repoInfo.getFIXVersionIndex(deprecated) - 1;
    }

    int fromVersion;
    String startFIXVersion = props.getProperty(RepoInfo.PROP_ADDED_VERSION);
    fromVersion = repoInfo.getFIXVersionIndex(startFIXVersion);

    writeFIXVersionCategories(fromVersion, toVersion, fw);
  }

  private void writeFIXVersionCategories(int fromVersion, int toVersion, PrintWriter fw) throws Exception {
    //No toVersion (-ve) means open ended. Use {{FIX.x.y+}}.
    if (toVersion < 0) {
      String fixVersion = repoInfo.getFIXVersionString(fromVersion);
      fw.println("{{" + fixVersion + "+}}");
    } else {
      //Explicit list of categories eg [[Category:FIX.4.1]][[Category:FIX.4.2]]
      for (int i = fromVersion; i <= toVersion; i++) {
        String fixVersion = repoInfo.getFIXVersionString(i);
        fw.print("[[Category:" + fixVersion + "]]");
      }
      fw.println();
    }
  }

  private void writeMessageAndComponentCategories(String name, PrintWriter fw) {
    Set<String> messageAndComponents = repoInfo.getContainingMessagesAndComponents(name);
    if (messageAndComponents == null) {
      System.out.println("WARNING: Unused in any message: " + name);
    } else {
      List<String> sortedList = new ArrayList<String>(messageAndComponents);
      Collections.sort(sortedList);
      for (String messageAndComponent : sortedList) {
        fw.print("[[Category:" + messageAndComponent + "]]");
      }
      fw.println();
    }
  }

  private void writeSegmentContents(PrintWriter fw, List<Properties> segmentInfo, int fixVersion) {
    for (Properties props : segmentInfo) {
      fw.println();
      fw.println("|-");

      String tagText = props.getProperty("TagText").trim();

      int tag;
      try {
        tag = Integer.parseInt(tagText);
      } catch (NumberFormatException ex) {
        tag = 0;
      }


      if (tag > 0) {
        fw.print("|| [[" + tagText + "]] ");

        Properties fieldProps = repoInfo.getFieldPropsFromTag(tag, fixVersion);
        String fieldName = fieldProps.getProperty(RepoInfo.PROP_FIELD_NAME);
        String hint = extractHint(fieldProps.getProperty(RepoInfo.PROP_FIELD_DESCRIPTION));

        if (hint == null || hint.length() == 0) {
          fw.print("|| [[" + fieldName + "]] ");
        } else {
          fw.print("|| [[" + fieldName + " | <span title='" + hint + "'>" + fieldName + "</span>]] ");
        }
      } else {
        fw.print("|| ");  //leave tag field blank for components

        Properties componentProps = repoInfo.getComponentPropsFromName(tagText, fixVersion);
        String hint = extractHint(componentProps.getProperty(RepoInfo.PROP_FIELD_DESCRIPTION));

        if (hint == null || hint.length() == 0) {
          fw.print("|| [[" + tagText + "]] ");
        } else {
          fw.print("|| [[" + tagText + " | <span title='" + hint + "'>" + tagText + "</span>]] ");
        }

      }

      String reqd = props.getProperty("Reqd");
      fw.print("|| " + (reqd.equals("1") ? "x" : "") + " ");

      String description = props.getProperty(RepoInfo.PROP_SEGMENT_DESCRIPTION);
      fw.print("|| " + (description == null ? "" : formatDescription(description, linkDetector)) + " ");

    }

    fw.println();
    fw.println("|}");
  }

  private void writeUserVersion(File scriptDir, PrintWriter script, String userTitle, String fplTitle, String suffix) throws IOException {
    if (createUserPages) {
      String relName = titleToName(userTitle) + suffix;
      String fname = scriptDir.getAbsolutePath() + File.separator + relName;
      PrintWriter fw = new PrintWriter(new FileWriter(fname));
      //Write include of fpl version of page
      fw.println("{{" + fplTitle + "}}");
      inviteInput(fw);
      fw.close();

      //Write to script file
      addImportToScript(script, relName, userTitle, false);
    }
  }

  private void writeWikiTemplateParameter(PrintWriter fw, String key, String value) {
    //Get rid of any bar characters that will confuse the wiki.
    value = value.replace('|', ' ');
    
    //Create links in descriptions and elaborations.
    if (RepoInfo.PROP_DESCRIPTION.equals(key) || 
            RepoInfo.PROP_ELABORATION.equals(key)) {
      value = formatDescription(value, linkDetector);
    }
    fw.println("| " + key + "=" + value);
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      usage();
      System.exit(0);
    }

    File repoDir = new File(args[0]);
    File scriptDir = new File(args[1]);

    //Create output directory if necessary.
    scriptDir.mkdirs();

    createUserPages = false;
    File epDir = null;

    if (args.length > 2) {
      String s = args[2];
      if ("createUserPages".equals(s)) {
        createUserPages = true;
      } else {
        epDir = new File(s);
      }
    }
    
    if (args.length > 3) {
      String s = args[3];
      if ("createUserPages".equals(s)) {
        createUserPages = true;
      } else {
        epDir = new File(s);
      }
    }

    RepoInfo repoInfo = new RepoInfo(repoDir, epDir);

    FixwikiGenerator generator = new FixwikiGenerator(repoInfo);
    generator.generate(scriptDir);

  }

  private static void usage() {
    System.out.println();
    System.out.println("Usage:");
    System.out.println();
    System.out.println("com.cameronedge.fixwiki.FixwikiGenerator <FIX repository> <Script directory> [<EP repository>] [createUserPages]");
    System.out.println();
    System.out.println("Generates text files and a script for populating a FIX Wiki from the given FIX repository.");
    System.out.println("<FIX repository>   - a directory containing a FIX repository. The directory should contain subdirectories named after each FIX version.");
    System.out.println("<Script directory> - the directory where the Wiki page creation script and the associated");
    System.out.println("                     text files used to populate the FIX Wiki are stored.");
    System.out.println("<EP repository>    - a directory containing the latest repository files for the last FIX version. It should contain a set of the standard repository XML files: Messaages.xml, Fields.xml etc");
    System.out.println("createUserPages    - User pages are generated only if this is specified.");
    System.out.println("                     Note that existing user pages are NEVER overwritten, even if this is specified.");
    System.out.println();
    System.out.println("There is also a system property ignoreErrors which will try to continue processing even if fatal errors are detected.");
    System.out.println("Activate on the command line in the usual way - ie -DignoreErrors");
    System.out.println();
    System.out.println("Example:");
    System.out.println("com.cameronedge.fixwiki.FixwikiGenerator ~/FIX/Repository ~/Temp/fixwiki ~/FIX/FIXRepository_FIX.5.0SP2_EP156/Basic createUserPages");
    System.out.println();
  }


}
