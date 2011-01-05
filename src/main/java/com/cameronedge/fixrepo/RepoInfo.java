/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixrepo;

import com.cameronedge.fixwiki.FIXVersionInfo;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Stores data extracted from FIX repository.
 *
 * @author John Cameron
 */
public class RepoInfo {

  public static final String PROP_DESCRIPTION = "Description";

  public static final String PROP_COMPONENT_DESCRIPTION = PROP_DESCRIPTION;
  public static final String PROP_COMPONENT_NAME = "Name";

  public static final String PROP_ENUM_DESCRIPTION = PROP_DESCRIPTION;
  public static final String PROP_ENUM_NAME = "SymbolicName";
  public static final String PROP_ENUM_VALUE = "Value";

  public static final String PROP_FIELD_DESCRIPTION = PROP_DESCRIPTION;
  public static final String PROP_FIELD_ENUM_FIELD_NAME = "EnumFieldName";
  public static final String PROP_FIELD_NAME = "Name";
  public static final String PROP_FIELD_USES_ENUMS_FROM_TAG = "EnumDatatype";

  public static final String PROP_MESSAGE_DESCRIPTION = PROP_DESCRIPTION;
  public static final String PROP_MESSAGE_NAME = "Name";

  public static final String PROP_SEGMENT_DESCRIPTION = PROP_DESCRIPTION;

  public static final String PROP_TYPE_NAME = "Name";

  private static final FIXVersionInfo[] fixVersionInfos = new FIXVersionInfo[]{
          new FIXVersionInfo("FIX.1.0", "1.0", 0),
          new FIXVersionInfo("FIX.2.7", "2.7", 103),
          new FIXVersionInfo("FIX.3.0", "3.0", 113),
          new FIXVersionInfo("FIX.4.0", "4.0", 140),
          new FIXVersionInfo("FIX.4.1", "4.1", 211),
          new FIXVersionInfo("FIX.4.2", "4.2", 446),
          new FIXVersionInfo("FIX.4.3", "4.3", 659),
          new FIXVersionInfo("FIX.4.4", "4.4", 956),
          new FIXVersionInfo("FIX.5.0", "5.0", 1139),
          new FIXVersionInfo("FIXT.1.1", "T1.1", 1409),
          new FIXVersionInfo("FIX.5.0SP1", "5.0SP1", 1426),
          new FIXVersionInfo("FIX.5.0SP2", "5.0SP2", 1621),
  };
  public static final int latestFIXVersionIndex = fixVersionInfos.length - 1;

  public static final int fixTVersionIndex = 9;

  public static int maxCommonPrefix = 43;

  private int[] enumNameLens = new int[200];

  //TODO JC Not sure if this is needed any more
  //Key is MsgType. Each MsgType is associated with an array where each element
  //corresponds to a FIX version (array index is FIXVersion index). The value of each element is the FIXVersion
  //index of the FIX Version at which this version of the message first appeared.
  //Value is -1 if the message was not present in this FIX version.
  private Map<String, Integer[]> messageVersionInfos = new HashMap<String, Integer[]>();

  //TODO JC Not sure if this is needed any more
  //Key is ComponentName. Otherwise same as messageVersionInfos
  private Map<String, Integer[]> componentVersionInfos = new HashMap<String, Integer[]>();

  //Message component properties indexed by ComponentName. (List<Properties> only contains one Properties).
  //The MsgID property links to the corresponding message segment in segmentInfos.
  private Map<String, List<Properties>>[] componentInfosByVersion = new Map[latestFIXVersionIndex + 1];
  private Map<String, List<Properties>> componentInfosAllVersions = new HashMap();

  //Data type properties indexed by TypeName. (List<Properties> only contains one Properties).
  private Map<String, List<Properties>>[] typeInfosByVersion = new Map[latestFIXVersionIndex + 1];
  private Map<String, List<Properties>> typeInfosAllVersions = new HashMap();

  //Enumerated field values indexed by tag. Each enumerated value has an associated Properties.
  private Map<String, List<Properties>>[] enumInfosByVersion = new Map[latestFIXVersionIndex + 1];
  private Map<String, List<Properties>> enumInfosAllVersions = new HashMap();

  //Field properties indexed by tag. (List<Properties> only contains one Properties).
  private Map<String, List<Properties>>[] fieldInfosByVersion = new Map[latestFIXVersionIndex + 1];
  private Map<String, List<Properties>> fieldInfosAllVersions = new HashMap();

  //Map of all field names, occurring in any FIX version, to a tag number.
  private Map<String, Integer> fieldNameTagMap = new HashMap<String, Integer>();

  //Message properties indexed by MsgType. (List<Properties> only contains one Properties).
  //The MsgID property links to the corresponding message segment in segmentInfos.
  private Map<String, List<Properties>>[] messageInfosByVersion = new Map[latestFIXVersionIndex + 1];
  private Map<String, List<Properties>> messageInfosAllVersions = new HashMap();

  //For each tag or component, List of all message names or components containing it.
  //Indexed by tag number or component name.
  private Map<String, Set<String>> messagesAndComponentsByName = new HashMap<String, Set<String>>();

  //Message segments (lists of fields/components) indexed by MsgID. Each field or component has an associated
  //Properties. The TagText field is either a numeric tag number - indexing via the tag index to a field entry in
  //fieldInfos, or a component name - indexing via the ComponentName index to a component entry in componentInfos.
  private Map<String, List<Properties>>[] segmentInfosByVersion = new Map[latestFIXVersionIndex + 1];
  private Map<String, List<Properties>> segmentInfosAllVersions = new HashMap();

  private SAXParser parser;

  private boolean ignoreErrors;

  public RepoInfo(File repoDir) throws Exception {

    ignoreErrors = System.getProperty("ignoreErrors") != null;

    ClassLoader loader = this.getClass().getClassLoader();

    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    parser = parserFactory.newSAXParser();

    //Now process the data for each FIX versions.
    for (int i = 0; i < fixVersionInfos.length; i++) {
      String version = fixVersionInfos[i].version;
      System.out.println("Processing FIX version " + version);

      String fixDirPath = repoDir.getAbsolutePath() + File.separator + version + File.separator + "Base";
      File fixDir = new File(fixDirPath);

      if (fixDir.exists()) {

        componentInfosByVersion[i] = parse(fixDir, "Components.xml", "Component", "Name", false);
        System.out.println("    Processed " + componentInfosByVersion[i].size() + " categories");

        enumInfosByVersion[i] = parse(fixDir, "Enums.xml", "Enum", "Tag", true);
        System.out.println("    Processed " + enumInfosByVersion[i].size() + " enums");

        fieldInfosByVersion[i] = parse(fixDir, "Fields.xml", "Field", "Tag", false);
        System.out.println("    Processed " + fieldInfosByVersion[i].size() + " fields");

        messageInfosByVersion[i] = parse(fixDir, "Messages.xml", "Message", "MsgType", false);
        System.out.println("    Processed " + messageInfosByVersion[i].size() + " message types");

        segmentInfosByVersion[i] = parse(fixDir, "MsgContents.xml", "MsgContent", "ComponentID", true);
        System.out.println("    Processed " + segmentInfosByVersion[i].size() + " message segments");

        typeInfosByVersion[i] = parse(fixDir, "Datatypes.xml", "Datatype", "Name", false);
        System.out.println("    Processed " + (typeInfosByVersion[i] == null ? "0" : typeInfosByVersion[i].size()) + " data types");
      } else {
        System.out.println("    No repo directory found for version " + version);
      }
    }

    String s;
    Map<String, List<Properties>> extraData;
    InputStream is;

    //Just creates merged infos for all versions
    postProcessComponents();
    
    //Just creates merged infos for all versions
    postProcessMessages();
    
    //Just creates merged infos for all versions
    postProcessTypes();

    //Special post process for latest version. That is what users will use.
    postProcessEnums();

    //Must follow postProcessEnums.
    //Adds Enum attribute to fields with enumerated values. 
    // Computed from enumInfos. This assumes that the enums are up to date.
    postProcessFields();

    //Postprocess message/component segments. This should come after 
    //postprocessing of fields, messages and components.
    postProcessSegments();

    //Now enrich the merged data with extra resource files.

    s = "MessageDesc.xml";
    is = loader.getResourceAsStream(s);
    if (is == null) {
      System.out.println("WARNING: Missing resource file " + s);
    }
    extraData = parse(is, "MessageDesc", "MsgType", false);
    processExtraMessageDescriptions(extraData);

    s = "ComponentDesc.xml";
    is = loader.getResourceAsStream(s);
    if (is == null) {
      System.out.println("WARNING: Missing resource file " + s);
    }
    extraData = parse(is, "ComponentDesc", PROP_COMPONENT_NAME, false);
    processExtraComponentDescriptions(extraData);

    s = "EnumDesc.xml";
    is = loader.getResourceAsStream(s);
    if (is == null) {
      System.out.println("WARNING: Missing resource file " + s);
    }
    extraData = parse(is, "Enums", "Tag", true);
    processExtraEnumDescriptions(extraData);

    s = "Glossary.txt";
    is = loader.getResourceAsStream(s);
    if (is == null) {
      System.out.println("WARNING: Missing resource file " + s);
    }
    processGlossary(is);

  }

  private void addContainsItem(int fixVersionIndex, String tagText, String containerName) {
    //TODO JC If you ever wanted to you could easily have a copy by FIX version here.  
    Set<String> mcs = messagesAndComponentsByName.get(tagText);
    if (mcs == null) {
      mcs = new HashSet<String>();
      messagesAndComponentsByName.put(tagText, mcs);
    }

    mcs.add(containerName);
  }

  private boolean checkEnumName(String enumName, Set<String> names, String tagStr, String enumValue, String description) {
    boolean warning = false;

    //Keeps tally of enumName lengths and  checks for similar names.    
    int nameLen = enumName.length();
    enumNameLens[nameLen]++;
    String enumNameExpectedUnique;
    if (nameLen > maxCommonPrefix) {
      enumNameExpectedUnique = enumName.substring(0, maxCommonPrefix);
    } else {
      enumNameExpectedUnique = enumName;
    }
    for (String name : names) {
      String nameExpectedUnique;
      if (name.length() >= maxCommonPrefix) {
        nameExpectedUnique = name.substring(0, maxCommonPrefix);
      } else {
        nameExpectedUnique = name;
      }

      if (enumNameExpectedUnique.equalsIgnoreCase(nameExpectedUnique)) {
        System.out.println("WARNING: Tag " + tagStr + " Similar value names: " + enumName + " and " + name);
        dumpEnumWarning(tagStr, enumValue, enumName, description);
        warning = true;
        break;
      }
    }
    return warning;
  }

  private void dumpEnumWarning(String tagStr, String enumValue, String enumName, String description) {
    System.out.println("  <Enums>");
    System.out.println("    <Tag>" + tagStr + "</Tag>");
    System.out.println("    <" + PROP_ENUM_VALUE + ">" + enumValue + "</" + PROP_ENUM_VALUE + ">");
    System.out.println("    <" + PROP_ENUM_NAME + ">" + enumName + "</" + PROP_ENUM_NAME + ">");
    System.out.println("    <" + PROP_ENUM_DESCRIPTION + ">");
    System.out.println(description);
    System.out.println("    </" + PROP_ENUM_DESCRIPTION + ">");
    System.out.println("  </Enums>");
  }

  private static String extractGlossaryFieldName(String fieldName) {
    int start = fieldName.indexOf('[');
    int end = fieldName.indexOf(']');

    if (start < 0 || end < 0) {
      return null;
    }

    return fieldName.substring(start + 1, end);
  }

  public Map<String, List<Properties>> getComponentInfos() {
    return componentInfosByVersion[latestFIXVersionIndex];
  }

  public Properties getComponentPropsFromName(String name, int fixVersion) {
    Map<String, List<Properties>> componentInfos = componentInfosByVersion[fixVersion];
    List<Properties> componentPropsList = componentInfos.get(name);
    if (componentPropsList == null) {
      throw new RuntimeException("Unknown component - " + name);
    }
    return componentPropsList.get(0);
  }

  public Map<String, Integer[]> getComponentVersionInfos() {
    return componentVersionInfos;
  }

  public Map<String, List<Properties>> getEnumInfos() {
    return enumInfosByVersion[latestFIXVersionIndex];
  }

  private Properties getEnumInfoFromTagValueName(Integer tag, String valueName) {
    Map<String, List<Properties>> enumInfos = getEnumInfos();

    List<Properties> enumInfoList = enumInfos.get(tag.toString());

    if (enumInfoList != null) {
      //Search through property list for matching value name.
      for (Properties props : enumInfoList) {
        if (valueName.equalsIgnoreCase(props.getProperty(PROP_ENUM_NAME)) || valueName.equalsIgnoreCase(props.getProperty(PROP_ENUM_VALUE))) {
          return props;
        }
      }
    }

    return null;
  }

  /**
   * Returns map of field properties indexed by tag.
   * <p/>
   *
   * @return Map indexed by tag number (as String). Indexed entries are Lists of Properties, but each
   *         List should only contain one Properties. That is the field properties corresponding to the field tag.
   */
  public Map<String, List<Properties>> getFieldInfos() {
    return fieldInfosAllVersions;
  }

  /**
   * Returns map of message properties indexed by msgType.
   * <p/>
   *
   * @return Map indexed by msgType. Indexed entries are Lists of Properties, but each List should only contain one
   *         Properties. That is the message properties corresponding to the msgType.
   */
  public Map<String, List<Properties>> getMessageInfos() {
    return messageInfosAllVersions;
  }

  public Map<String, Integer[]> getMessageVersionInfos() {
    return messageVersionInfos;
  }

  public Map<String, List<Properties>> getSegmentInfos() {
    return segmentInfosAllVersions;
  }

  public Map<String, List<Properties>> getTypeInfos() {
    return typeInfosAllVersions;
  }

  /**
   * Returns list of message or component names which contain the given item - where the item
   * is either a tag number (representing a field) or a component name representing a component.
   *
   * @param tagOrComponentName Tag or component name
   * @return List of all messages or components which contain the given field or component.
   */
  public Set<String> getContainingMessagesAndComponents(String tagOrComponentName) {
    return messagesAndComponentsByName.get(tagOrComponentName);
  }

  private List<Properties> getContentsOfComponent(String componentName, int fixVersionIndex) {

    if (fixVersionIndex < 0) {
      return null;
    }

    //Hack around FIXT version - we don't want to count it here. Assume it has same contents
    //as previous version.
    if (fixVersionIndex == fixTVersionIndex) {
      fixVersionIndex = fixTVersionIndex - 1;
    }

    Map<String, List<Properties>> segmentInfos = segmentInfosByVersion[fixVersionIndex];

    //Find corresponding msgID.
    Map<String, List<Properties>> componentInfos = componentInfosByVersion[fixVersionIndex];
    if (componentInfos == null) {
      return null;
    }

    List<Properties> componentInfo = componentInfos.get(componentName);
    if (componentInfo == null) {
      //Component does not appear in this version.
      return null;
    }

    String msgID = componentInfo.get(0).getProperty("ComponentID");

    //Look up segment contents from msgID.
    return segmentInfos == null ? null : segmentInfos.get(msgID);
  }

  private List<Properties> getContentsOfMessage(String msgType, int fixVersionIndex) {

    if (fixVersionIndex < 0) {
      return null;
    }

    //Hack around FIXT version - we don't want to count it here. Assume it has same message contents
    //as previous version.
    if (fixVersionIndex == fixTVersionIndex) {
      fixVersionIndex = fixTVersionIndex - 1;
    }

    Map<String, List<Properties>> segmentInfos = segmentInfosByVersion[fixVersionIndex];

    //Find corresponding msgID.
    Map<String, List<Properties>> messageInfos = messageInfosByVersion[fixVersionIndex];
    if (messageInfos == null) {
      return null;
    }

    List<Properties> messageInfo = messageInfos.get(msgType);
    if (messageInfo == null) {
      //Message type does not appear in this version.
      return null;
    }

    String msgID = messageInfo.get(0).getProperty("ComponentID");

    //Look up segment contents from msgID.
    return segmentInfos == null ? null : segmentInfos.get(msgID);
  }

  public List<Properties> getSegmentInfoForComponent(String componentName, int fixVersion) {
    List<Properties> segmentInfo = null;

    Map<String, List<Properties>> componentInfos = componentInfosByVersion[fixVersion];

    if (componentInfos != null) {
      //First get msgID from componentName
      List<Properties> messageInfo = componentInfos.get(componentName);
      if (messageInfo != null) {
        Properties props = messageInfo.get(0);

        String msgID = props.getProperty("ComponentID");

        //Now look up segment info using MsgID.
        Map<String, List<Properties>> segmentInfos = segmentInfosByVersion[fixVersion];
        segmentInfo = segmentInfos.get(msgID);
      }
    }

    return segmentInfo;
  }

  public List<Properties> getSegmentInfoForMessage(String msgType, int fixVersion) {
    List<Properties> segmentInfo = null;

    Map<String, List<Properties>> messageInfos = messageInfosByVersion[fixVersion];

    if (messageInfos != null) {
      //First get msgID from msgType
      List<Properties> messageInfo = messageInfos.get(msgType);
      if (messageInfo != null) {
        Properties props = messageInfo.get(0);

        String msgID = props.getProperty("ComponentID");

        //Now look up segment info using MsgID.
        Map<String, List<Properties>> segmentInfos = segmentInfosByVersion[fixVersion];
        segmentInfo = segmentInfos.get(msgID);
      }
    }

    return segmentInfo;
  }

  /**
   * Look up fieldInfos to get field name of this tag
   *
   * @param tag Tag of field required
   * @return Name of field with given tag
   */
  public String getFieldNameFromTag(int tag) {
    Properties fieldProps = getFieldPropsFromTag(tag);
    return fieldProps.getProperty(PROP_FIELD_NAME);
  }

  public Map<String, Integer> getFieldNameTagMap() {
    return fieldNameTagMap;
  }

  public Properties getFieldPropsFromTag(int tag, int fixVersion) {
    Map<String, List<Properties>> fieldInfos = fieldInfosByVersion[fixVersion];
    List<Properties> fieldPropsList = fieldInfos.get(String.valueOf(tag));
    if (fieldPropsList == null) {
      throw new RuntimeException("Unknown tag value - " + tag);
    }
    return fieldPropsList.get(0);
  }

  public Properties getFieldPropsFromTag(int tag) {
    return getFieldPropsFromTag(tag, latestFIXVersionIndex);
  }

  public int getFIXVersionIndex(String fixVersionString) throws Exception {
    if (fixVersionString == null) {
      return 0;
    }
    for (int i = 0; i < fixVersionInfos.length; i++) {
      String version = fixVersionInfos[i].version;
      if (version.equalsIgnoreCase(fixVersionString)) {
        return i;
      }
    }
    throw new Exception("Unrecognized FIX version - " + fixVersionString);
  }

  public String getFIXVersionString(int fixVersionIndex) {
    return fixVersionInfos[fixVersionIndex].version;
  }

  public String getFIXVersionSuffix(int fixVersionIndex) {
    return fixVersionInfos[fixVersionIndex].suffix;
  }

  public static String getFIXVersionTagIntroduced(int tag) throws Exception {
    for (FIXVersionInfo fixVersionInfo : fixVersionInfos) {
      int maxTag = fixVersionInfo.maxTag;
      if (tag <= maxTag) {
        return fixVersionInfo.version;
      }
    }
    throw new Exception("Unrecognized FIX tag - " + tag);
  }

  private boolean hasTagValue(Map<String, List<Properties>> enumInfos, String tagStr, String enumValue) {
    if (enumInfos == null) {
      return false;
    }

    //Retrieve the values associated with the given tag.
    List<Properties> values = enumInfos.get(tagStr);

    if (values == null) {
      return false;
    }

    //Check if enumValue appears in any of the tag's values.
    for (Properties props : values) {
      if (enumValue.equals(props.getProperty(PROP_ENUM_VALUE))) {
        return true;
      }
    }

    return false;
  }

  private boolean isFunnyFieldName(String fieldName) {
    //First letter of name should be upper case.
    char c = fieldName.charAt(0);
    if (!Character.isUpperCase(c)) {
      return true;
    }

    //Should only contain A-Za-z0-9
    return !fieldName.matches("[A-Za-z0-9]*");

  }

  /**
   * Clears mergedInfos and repopulates it with merge of all data in infosByVersion.
   */
  private void mergeInfos(Map<String, List<Properties>> mergedInfos, 
                          Map<String, List<Properties>>[] infosByVersion) {

    mergedInfos.clear();

    //Scan through all versions
    for (int i = 0; i <= latestFIXVersionIndex; i++) {
      Map<String, List<Properties>> infos = infosByVersion[i];
      if (infos != null) {
        //Populate merged infos
        for (Map.Entry<String, List<Properties>> entry : infos.entrySet()) {
          mergedInfos.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private Map<String, List<Properties>> parse(InputStream is, String element, String indexElement, boolean multiValued) throws IOException, SAXException {
    if (is != null) {
      FixRepoHandler handler = new FixRepoHandler(element, indexElement, multiValued);
      parser.parse(is, handler);

      return handler.getInfos();
    } else {
      return null;
    }
  }

  private Map<String, List<Properties>> parse(File repoDir, String filename, String element, String indexElement, boolean multiValued) throws IOException, SAXException {
    File source = new File(repoDir.getAbsolutePath() + File.separator + filename);
    if (source.exists()) {
      FileInputStream fis = new FileInputStream(source);
      return parse(fis, element, indexElement, multiValued);
    } else {
      return null;
    }
  }

  /**
   * Constructs merged componentInfosAllVersions
   */
  private void postProcessComponents() {
    mergeInfos(componentInfosAllVersions, componentInfosByVersion);
  }

  /**
   * Checks for good data and fabricates names for enum values in new EnumName property.
   */
  private void postProcessEnums() {
    System.out.println("INFO: --- Processing Enums");
    
    mergeInfos(enumInfosAllVersions, enumInfosByVersion);    

    Map<String, List<Properties>> latestInfos = getEnumInfos();

    int warnCount = 0;

    for (Map.Entry<String, List<Properties>> enumInfoEntry : latestInfos.entrySet()) {
      String tagStr = enumInfoEntry.getKey();
      Integer.parseInt(tagStr); //Throws exception if not a number.

      //Used to check for duplicate enum names
      Set<String> names = new HashSet<String>();

      List<Properties> values = enumInfoEntry.getValue();
      for (Properties valueProps : values) {
        String enumName = valueProps.getProperty(PROP_ENUM_NAME);
        String enumValue = valueProps.getProperty(PROP_ENUM_VALUE);
        String description = valueProps.getProperty(PROP_ENUM_DESCRIPTION);
        if (enumName == null) {
          //Compute new EnumName property from the description attribute.
          enumName = RepoUtil.computeEnumName(description);

          valueProps.setProperty(PROP_ENUM_NAME, enumName);
        }

        if (checkEnumName(enumName, names, tagStr, enumValue, description)) {
          warnCount++;
        }
        names.add(enumName);

        try {
          Integer.parseInt(enumName);
          System.out.println("WARNING: Tag " + tagStr + " Numeric enumName: " + enumName);
        } catch (Exception ex) {
          //Normally will end up here because name will not be entirely numeric.
        }

        if (enumName.length() <= 1) {
          System.out.println("WARNING: Tag " + tagStr + " Short enumName: " + enumName + " Value " + enumValue);
          dumpEnumWarning(tagStr, enumValue, enumName, description);
          warnCount++;
        }

        if (enumName.length() > maxCommonPrefix) {
          System.out.println("WARNING: Tag " + tagStr + " Long enumName: " + enumName);
          dumpEnumWarning(tagStr, enumValue, enumName, description);
          warnCount++;
        }

        //Fix possible bad deprecated FIX versions that appear in current repo.
        String version = valueProps.getProperty("deprecated");
        boolean renamed = false;
        if ("FIX 5.0".equals(version)) {
          valueProps.setProperty("deprecated", "FIX.5.0");
          renamed = true;
        } else if ("FIX 4.2".equals(version)) {
          valueProps.setProperty("deprecated", "FIX.4.2");
          renamed = true;
        } else if ("FIX 4.3".equals(version)) {
          valueProps.setProperty("deprecated", "FIX.4.3");
          renamed = true;
        } else if ("FIX 4.4".equals(version)) {
          valueProps.setProperty("deprecated", "FIX.4.4");
          renamed = true;
        }
        if (renamed) {
          System.out.println("WARNING: Renaming bad deprecated version (" + version + ") in enum " + enumValue + " of tag " + tagStr);
        }
      }
    }

    System.out.println("WARNING: Number of enumName check warnings: " + warnCount);
    System.out.println("INFO: --- End Processing Enums");
  }

  /**
   * Adds fieldInfo PROP_FIELD_ENUM_FIELD_NAME attribute.
   * <p/>
   * Populates fieldNameTagMap.
   * <p/>
   * Tidies up field names (eg removing / characters and trimming spaces)
   *
   * @throws Exception Any fatal problems
   */
  private void postProcessFields() throws Exception {

    //First scan through field names for all versions, populating {@link #fieldNameTagMap} and
    //tidying up field names.
    for (int i = 0; i <= latestFIXVersionIndex; i++) {
      Map<String, List<Properties>> fieldInfos = fieldInfosByVersion[i];
      if (fieldInfos != null) {
        for (List<Properties> values : fieldInfos.values()) {
          Properties props = values.get(0);

          int fieldTag = Integer.parseInt(props.getProperty("Tag"));
          String fieldName = props.getProperty(PROP_FIELD_NAME);

          if (fieldName != null) {
            String clean = fieldName.replace('/', ' ');
            clean = clean.trim();
            if (!fieldName.equals(clean)) {
              System.out.println("WARNING: Bad chars in field name " + fieldName + ". Tag " + fieldTag +
                      ". Version " + getFIXVersionString(i));
              fieldName = clean;
            }
          }


          if (isFunnyFieldName(fieldName)) {
            System.out.println("WARNING: Funny field name " + fieldName + ". Tag " + fieldTag +
                    ". Version " + getFIXVersionString(i));
          }

          fieldNameTagMap.put(fieldName, fieldTag);

        }
      }
    }

    mergeInfos(fieldInfosAllVersions, fieldInfosByVersion);    


    //Now scan through latest field infos.
    Map<String, List<Properties>> fieldInfos = getFieldInfos();
    Map<String, List<Properties>> enumInfos = getEnumInfos();

    for (List<Properties> values : fieldInfos.values()) {

      if (values.size() != 1) {
        throw new RuntimeException("Field does not have single Properties");
      }

      Properties props = values.get(0);

      int fieldTag = Integer.parseInt(props.getProperty("Tag"));

      String fieldName = props.getProperty(PROP_FIELD_NAME);

      //Add an Enum tag if this field has enumerated values.
      String enumFieldName = null;
      String usesStr = props.getProperty(PROP_FIELD_USES_ENUMS_FROM_TAG);
      if (usesStr != null) {
        int enumFieldTag = Integer.parseInt(usesStr);
        enumFieldName = getFieldNameFromTag(enumFieldTag);
      } else {
        //Check whether this field has its own enumerated values. If so, it will have an entry in enumInfos.
        if (enumInfos.keySet().contains(String.valueOf(fieldTag))) {
          enumFieldName = fieldName;
        }
      }

      //If field has enumerated values add an Enum attribute.
      if (enumFieldName != null) {
        props.setProperty(PROP_FIELD_ENUM_FIELD_NAME, enumFieldName);
      }
    }
  }

  private void postProcessMessages() {
    mergeInfos(messageInfosAllVersions, messageInfosByVersion);
  }

  /**
   * Sorts segmentInfo lists.
   * <p/>
   * Constructs messagesAndComponentsByName
   * <p/>
   * Constructs messageVersionInfos and componentVersionInfos.
   */
  private void postProcessSegments() {

    //First sort segment lists by Position tag. They are not in order in repo.
    Comparator<Properties> propertiesComparator = new Comparator<Properties>() {
      PositionComparator positionComparator = new PositionComparator();

      public int compare(Properties o1, Properties o2) {
        String position1 = o1.getProperty("Position");
        String position2 = o2.getProperty("Position");
        return positionComparator.compare(position1, position2);
      }
    };

    for (Map<String, List<Properties>> segmentInfos : segmentInfosByVersion) {
      if (segmentInfos != null) {
        for (List<Properties> segmentInfo : segmentInfos.values()) {
          Collections.sort(segmentInfo, propertiesComparator);
        }
      }
    }

    
    mergeInfos(segmentInfosAllVersions, segmentInfosByVersion);    

    //Construct messagesAndComponentsByName.

    //Iterate through all latest messages.
    Map<String, List<Properties>> messageInfos = getMessageInfos();

    for (List<Properties> messagePropsList : messageInfos.values()) {
      Properties messageProps = messagePropsList.get(0);
      String msgType = messageProps.getProperty("MsgType");
      String messageName = messageProps.getProperty(PROP_MESSAGE_NAME);

      for (int i = 0; i < fixVersionInfos.length; i++) {
        List<Properties> segmentInfo = getSegmentInfoForMessage(msgType, i);
        if (segmentInfo != null) {
          for (Properties props : segmentInfo) {
            String tagText = props.getProperty("TagText");
            addContainsItem(i, tagText, messageName);
          }
        }
      }
    }


    //Do the same for components
    //Iterate through all latest components.
    Map<String, List<Properties>> componentInfos = getComponentInfos();

    for (String componentName : componentInfos.keySet()) {
      for (int i = 0; i < fixVersionInfos.length; i++) {
        List<Properties> segmentInfo = getSegmentInfoForComponent(componentName, i);
        if (segmentInfo != null) {
          for (Properties props : segmentInfo) {
            String tagText = props.getProperty("TagText");
            addContainsItem(i, tagText, componentName);
          }
        }
      }
    }


    //Construct messageVersionInfos.

    //Iterate through all latest message types.
    messageInfos = getMessageInfos();

    for (String msgType : messageInfos.keySet()) {

      Integer[] versions = messageVersionInfos.get(msgType);
      if (versions == null) {
        versions = new Integer[latestFIXVersionIndex + 1];
        messageVersionInfos.put(msgType, versions);
      }

      //This is the FIXVersion when the current contents was first introduced.
      int currentContentsVersion = -1;
      for (int i = 0; i < fixVersionInfos.length; i++) {

//        if (i > 0) {
//          System.out.println("Comparing " + msgType + " message contents between version "
//                  + fixVersionInfos[i].version + " and " + fixVersionInfos[i - 1].version);
//        }

        List<Properties> segment = getContentsOfMessage(msgType, i);
        List<Properties> previousSegment = getContentsOfMessage(msgType, i - 1);

        Integer myVersion;
        if (segment == null) {
          //Message does not appear in this version
          //Update this FIXversion's entry with -1. Set currentMessageVersion to next FIXVersion
          myVersion = null;
        } else {
          if (segmentsEqual(segment, previousSegment)) {
            //This version's contents is same as last version's contents.
            //Just copy across version of current contents.
            myVersion = currentContentsVersion;
          } else {
            //This version's contents is different to last version's.
            //Version is this version. Set current contents version to this version as well.
            myVersion = i;
            currentContentsVersion = i;
          }
        }
        //Update entry for this FIX version to myVersion
        versions[i] = myVersion;
      }
    }


    //Do the same for components
    //Iterate through all latest component types.
    componentInfos = getComponentInfos();

    for (String componentName : componentInfos.keySet()) {

      Integer[] versions = componentVersionInfos.get(componentName);
      if (versions == null) {
        versions = new Integer[latestFIXVersionIndex + 1];
        componentVersionInfos.put(componentName, versions);
      }

      //This is the FIXVersion when the current contents was first introduced.
      int currentContentsVersion = -1;
      for (int i = 0; i < fixVersionInfos.length; i++) {

//        if (i > 0) {
//          System.out.println("Comparing " + componentName + " contents between version "
//                  + fixVersionInfos[i].version + " and " + fixVersionInfos[i - 1].version);
//        }

        List<Properties> segment = getContentsOfComponent(componentName, i);
        List<Properties> previousSegment = getContentsOfComponent(componentName, i - 1);

        Integer myVersion;
        if (segment == null) {
          //Component does not appear in this version
          //Update this FIXversion's entry with null.
          myVersion = null;
        } else {
          if (segmentsEqual(segment, previousSegment)) {
            //This version's contents is same as last version's contents.
            //Just copy across version of current contents.
            myVersion = currentContentsVersion;
          } else {
            //This version's contents is different to last version's.
            //Version is this version. Set current contents version to this version as well.
            myVersion = i;
            currentContentsVersion = i;
          }
        }
        //Update entry for this FIX version to myVersion
        versions[i] = myVersion;
      }
    }
  }

  private void postProcessTypes() {
    mergeInfos(typeInfosAllVersions, typeInfosByVersion);
  }

  private void processExtraComponentDescriptions(Map<String, List<Properties>> extraData) {
    Map<String, List<Properties>> componentInfos = getComponentInfos();

    for (List<Properties> propertiesList : extraData.values()) {
      Properties props = propertiesList.get(0);

      String componentName = props.getProperty(PROP_COMPONENT_NAME);
      String desc = props.getProperty(PROP_COMPONENT_DESCRIPTION);

      //Now update existing description.
      List<Properties> currentPropList = componentInfos.get(componentName);
      if (currentPropList == null) {
        throw new RuntimeException("Unknown component name in extra component descriptions: " + componentName);
      } else {
        Properties currentProps = currentPropList.get(0);
        currentProps.setProperty(PROP_COMPONENT_DESCRIPTION, desc);
      }
    }
  }

  private void processExtraEnumDescriptions(Map<String, List<Properties>> extraData) {
    Map<String, List<Properties>> enumInfos = getEnumInfos();

    for (List<Properties> propertiesList : extraData.values()) {
      //Each list entry contains a tag, and enum value
      for (Properties props : propertiesList) {

        String tag = props.getProperty("Tag");
        String enumValue = props.getProperty(PROP_ENUM_VALUE);
        String desc = props.getProperty(PROP_ENUM_DESCRIPTION);

        //Now update existing message description in enumInfos.
        List<Properties> currentPropList = enumInfos.get(tag);
        if (currentPropList == null) {
          throw new RuntimeException("Unknown tag in extra enum descriptions: " + tag);
        } else {
          //Scan through current properties looking for one with matching enum value.
          boolean found = false;
          for (Properties currentProps : currentPropList) {
            if (enumValue.equals(currentProps.getProperty(PROP_ENUM_VALUE))) {
              found = true;
              currentProps.setProperty(PROP_ENUM_DESCRIPTION, desc); //Note non standard name in repo for description
              break;
            }
          }
          if (!found) {
            throw new RuntimeException("Unknown enum of tag " + tag + " in extra enum descriptions: " + enumValue);
          }
        }
      }
    }
  }

  private void processExtraMessageDescriptions(Map<String, List<Properties>> extraData) {
    Map<String, List<Properties>> messageInfos = getMessageInfos();

    for (List<Properties> propertiesList : extraData.values()) {
      Properties props = propertiesList.get(0);

      String msgType = props.getProperty("MsgType");
      String desc = props.getProperty(PROP_MESSAGE_DESCRIPTION);

      //Now update existing message description in messageInfos.
      List<Properties> currentPropList = messageInfos.get(msgType);
      if (currentPropList == null) {
        String errmess = "Unknown message type in extra message descriptions: " + msgType;
        if (ignoreErrors) {
          System.out.println("ERROR: " + errmess);
        } else {
          throw new RuntimeException(errmess);
        }
      } else {
        Properties currentProps = currentPropList.get(0);
        currentProps.setProperty(PROP_MESSAGE_DESCRIPTION, desc);
      }
    }
  }

  public void processGlossary(InputStream glossary) throws IOException {
    if (glossary != null) {
      System.out.println("INFO: --- Processing Glossary");
      InputStreamReader glossaryReader = new InputStreamReader(glossary);
      GlossaryProcessor gp = new GlossaryProcessor();
      List<GlossaryEntry> entries = gp.processGlossaryText(glossaryReader);

      for (GlossaryEntry entry : entries) {
//        System.out.println();
//        System.out.println(entry);
//        System.out.println();

        //Extract field - will be surrounded by square brackets.
        String fieldName = extractGlossaryFieldName(entry.fieldName);

        if (fieldName == null) {
          if (entry.fieldName.trim().length() > 0) {
            System.out.println("INFO: Glossary entry not associated with field. " + entry.fieldName);
          }
        } else {

          //Look up its tag.
          Integer tag = fieldNameTagMap.get(fieldName);
          if (tag == null) {
            System.out.println("WARNING: Unknown field in glossary " + fieldName);
          } else {
            //Does value name go with field?
            //Skip marked values
            if (entry.valueName.length() == 0 || entry.valueName.charAt(0) == '?') {
              System.out.println("INFO: Ignoring: " + entry);
            } else {
              Properties enumInfo = getEnumInfoFromTagValueName(tag, entry.valueName);
              if (enumInfo == null) {
                System.out.println("WARNING: Unknown glossary value '" + entry.valueName + "' for field " + fieldName);
              } else {
                if (enumInfo.getProperty(PROP_ENUM_DESCRIPTION).length() > entry.description.length()) {
                  System.out.println("WARNING: Replacing longer description with shorter " + entry.valueName + "for field " + fieldName);
                  System.out.println("Replacing description of " + fieldName + ":" + entry.valueName);
                  System.out.println(enumInfo.getProperty(PROP_ENUM_DESCRIPTION));
                  System.out.println("   V");
                  System.out.println(entry.description);
                }
                enumInfo.setProperty(PROP_ENUM_DESCRIPTION, entry.description);
              }
            }
          }
        }
      }
      System.out.println("INFO: --- End Processing Glossary");
    }
  }

  private boolean segmentsEqual(List<Properties> segment1, List<Properties> segment2) {
    if (segment1 == null || segment2 == null || segment1.size() != segment2.size()) {
      return false;
    }

    //Same number of content entries.
    //Normally would expect contents to be the same, but check that each detailed entry is identical.
    Iterator<Properties> iter2 = segment2.iterator();
    for (Properties props1 : segment1) {
      Properties props2 = iter2.next();
      if (props1.size() != props2.size()) {
        //Different number of properties.
//        System.out.println("WARNING: Same number of segment entries but number of properties different " + props1);
//        System.out.println();
//        System.out.println("   " + props2);
        return false;
      }

      //Same number of properties. Are they different?
      //Check that each key value in props1 appears in props2.
      for (Object key : props1.keySet()) {
        //Ignore Description and MsgID keys - doesn't matter if they change.
        if (!key.equals(PROP_COMPONENT_DESCRIPTION) && !key.equals("ComponentID")) {
          String val1 = props1.getProperty((String) key);
          String val2 = props2.getProperty((String) key);
          if (!val1.equals(val2)) {
//            System.out.println("WARNING: Segment properties different for key " + key + ". Values " + val1 + " / " + val2);
            return false;
          }
        }
      }

    }

    //Identical segments
    return true;
  }
}
