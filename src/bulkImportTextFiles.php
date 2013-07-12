/**
* Create or edit pages using the contents of text files.
*
* Based on the standard importTextFile.php. However, this version can
* import more than one text file at a time. 
* It reads the text files to be imported from an input file.
* For large numbers of import files, this makes the import process 3 or 4 times
* faster.
* 
* @author John Cameron 
*/
<?php
require_once(__DIR__ . '/commandLine.inc');
echo("Import Text File\n\n");

if (count($args) < 1) {
    showHelp();
} else {
    $importsFilename = $args[0];
    if (is_file($importsFilename)) {
        $opsfilesize = filesize($importsFilename);
        $dirname = dirname($importsFilename);
        $file = @fopen($importsFilename, "r");
        if ($file) {
            $user = User::newFromName( 'Maintenance script' );
            $comment = 'Importing text file';
            $flags = 0;
            while (($buffer = fgets($file, 4096)) !== false) {
                $upto = ftell($file);
                $percent = round($upto * 100 / $opsfilesize);
                echo $percent . "% ";
                $tokens = explode("|", $buffer);
                if (count($tokens) > 1) {
                    $title = trim($tokens[0]);
                    $filename = $dirname . "/" . trim($tokens[1]);
                    if (count($tokens) > 2) {
                        $option = trim($tokens[2]);
                        if ($option != "nooverwrite") {
                            abort("Unknown option: " . $option);
                        }
                    } else {
                        unset($option);
                    }

                    if (is_file($filename)) {
                        $title = Title::newFromURL($title);
                        if (is_object($title)) {

                            if ($title->exists() && isset($option)) {
                                echo( "Leave " . $title . "...exists.\n" );
                            } else {
                                $text = file_get_contents($filename);

                                echo( "Edit " . $title . "..." );
                                $page = WikiPage::factory($title);
                                $content = ContentHandler::makeContent($text, $title);
                                $page->doEditContent($content, $comment, $flags, false, $user);
                                echo( "done.\n" );
                            }
                        } else {
                            abort("invalid title: " . $title . "\n");
                        }
                    } else {
                        abort(" No such file: " . $filename . "\n");
                    }
                }
            }
            if (!feof($file)) {
                abort("Error: unexpected fgets() fail\n");
            }
            fclose($file);
        }
    } else {
        abort("No such file: " . $importsFilename . "\n");
    }
}

function abort($message) {
    echo ("\n" . $message . "\n" );
    echo ("\n !!!!! THE BUILD FAILED !!!!!\n" );
    echo ("\n Correct the errors and try again \n" );
    exit(2);
}

function showHelp()
{
    print <<<EOF
USAGE: php bulkImportTextFiles.php <imports file>

<imports file> : Path to a file containing the files to be imported.
                 
Each line of the imports file describes a file to be imported.

The format of each line is as follows:

<title>|<file name>[|nooverwrite]

where:

<title>     - Title for the wiki page.
<file name> - Name of file containing page content to import. Note that files
              are assumed to be located in the same directory as the 
              <imports file>.
nooverwrite - Optional. If present, indicates that any existing page should not
              be overwritten. An existing page will be left unchanged.

Note that the | character is used as the delimiter.

EOF;
}
