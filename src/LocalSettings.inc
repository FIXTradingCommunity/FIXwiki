<?php

#$wgReadOnly = "Backing up FIXwiki";

$wgEnableUploads = true;

#Enable short urls.
#The following only works if you have installed into "w" and if your Apache .htaccess file has the following lines in it:
#RewriteEngine On
#RewriteRule ^fixwiki/(.*)$ /w/index.php?title=$1 [PT,L,QSA]
#RewriteRule ^fixwiki/*$ /w/index.php [L,QSA]
$wgArticlePath = "/fixwiki/$1";
$wgUsePathInfo = true;


$wgUseAjax = true;
$wgEnableMWSuggest = true;
$wgLogo = "$wgScriptPath/images/cameronedge_logo_blue135trans.png";
$wgFavicon = "$wgScriptPath/images/favicon.ico";
$wgRightsIcon = "$wgScriptPath/images/cameronedge_logo_blueSmall.png";
$wgRightsUrl = "http://cameronedge.com/";
$wgRightsText = "FIXwiki copyrights";
$wgRightsPage = "FIXwiki:Copyrights";
$wgEmailConfirmToEdit = true;

$wgFileExtensions[] = 'txt'; //Used for FIXnames.txt file

$wgNamespacesWithSubpages[NS_MAIN] = true;
$wgNamespacesWithSubpages[NS_TALK] = true;
$wgNamespacesWithSubpages[NS_CATEGORY] = true;
$wgNamespacesWithSubpages[NS_CATEGORY_TALK] = true;

#Wiki requires login to read or edit
$wgGroupPermissions['*']['edit'] = false;
$wgGroupPermissions['*']['read'] = false;
$wgWhitelistRead = array(
  'FIXwiki',
  'FIXwiki:About',
  'FIXwiki:Copyrights',
  'FIXwiki:Structure',
  'FIXwiki:Use',
  'Special:RecentChanges',
  'MediaWiki:Common.css',
  'MediaWiki:Monobook.css',
  'MediaWiki:Modern.css',
  '-',
);

#Add FPL namespace
define("NS_FPL", 100);
define("NS_FPL_TALK", 101);
$wgExtraNamespaces = array(
  NS_FPL => 'FPL',
  NS_FPL_TALK => 'FPL_talk'
);

$wgNamespacesWithSubpages[NS_FPL] = true;
$wgNamespacesWithSubpages[NS_FPL_TALK] = true;

#Namespace protection
$wgNamespaceProtection[NS_PROJECT] = array('editinterface');
$wgNamespaceProtection[NS_FPL] = array('editinterface');
$wgNamespaceProtection[NS_TEMPLATE] = array('editinterface');
$wgNamespaceProtection[NS_HELP] = array('editinterface');
$wgNamespaceProtection[NS_CATEGORY] = array('editinterface');

$wgExternalLinkTarget = '_blank';

require_once( "$IP/extensions/ParserFunctions/ParserFunctions.php" );
require_once("$IP/extensions/SubPageList3/SubPageList3.php"); // for SubPageList3 extension

require_once( "$IP/extensions/recaptcha/ReCaptcha.php" );
#This is global key - generate a CameronEdge specific one.
$recaptcha_public_key = '6LdtuQcAAAAAAFB8LFXNfmMSkOtOowSNQrP1TbDx';
$recaptcha_private_key = '6LdtuQcAAAAAAFlf4sHg8L2QCq-ppRJoaCupFif_';

#Maintenance shell
# Sysops will have access to the MaintenanceShell
$wgGroupPermissions['sysop']['maintenanceshell'] = true;
 
#Place this on the VERY LAST line of your Localsettings.php file:
require_once("$IP/extensions/MaintenanceShell.0.2.2/MaintenanceShell.php");
