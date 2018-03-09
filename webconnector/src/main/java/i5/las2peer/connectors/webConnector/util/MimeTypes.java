package i5.las2peer.connectors.webConnector.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.core.MediaType;

public class MimeTypes {

	public static final String DEFAULT = MediaType.APPLICATION_OCTET_STREAM;
	
	/**
	 * MIME-type map parseable to MimetypesFileTypeMap 
	 * As found here: https://github.com/jjYBdx4IL/misc/blob/master/text-utils/src/main/resources/com/github/jjYBdx4IL/utils/text/mimetypes.txt
	 */
	private static String types = 
			"application/javascript js\n" + 
			"application/msword doc docx docm\n" + 
			"application/pdf pdf\n" + 
			"application/postscript ai eps ps\n" + 
			"application/rss+xml rss\n" + 
			"application/rtf rtf\n" + 
			"application/vnd.ms-excel xls xlsx xlsm XLS\n" + 
			"application/vnd.ms-powerpoint ppt pps pot pptx pptm\n" + 
			"application/vnd.oasis.database odb\n" + 
			"application/vnd.oasis.opendocument.text odt\n" + 
			"application/vnd.oasis.presentation odp\n" + 
			"application/vnd.oasis.spreadsheet ods\n" + 
			"application/vnd.oasis.text odt\n" + 
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet xlsx\n" + 
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document docx\n" + 
			"application/x-awk awk\n" + 
			"application/x-blender blend\n" + 
			"application/x-cd-image iso\n" + 
			"application/x-compress zip gz tar rar\n" + 
			"application/x-deb deb\n" + 
			"application/x-font-otf otf OTF\n" + 
			"application/x-font-ttf ttf TTF\n" + 
			"application/x-java-applet class\n" + 
			"application/x-java-archive jar\n" + 
			"application/xml xml\n" + 
			"application/x-ms-dos-executable exe msi\n" + 
			"application/x-perl pl\n" + 
			"application/x-php php\n" + 
			"application/x-rpm rpm\n" + 
			"application/x-sharedlib o\n" + 
			"application/x-shellscript sh\n" + 
			"application/x-tar tar\n" + 
			"application/x-texinfo texinfo texi\n" + 
			"application/x-tex tex\n" + 
			"application/x-trash autosave\n" + 
			"application/x-troff t tr roff\n" + 
			"application/x-vnd.oasis.opendocument.spreadsheet ods\n" + 
			"application/zip zip\n" + 
			"audio/ac3 ac3\n" + 
			"audio/basic au\n" + 
			"audio/midi midi mid\n" + 
			"audio/mpeg mp3 mpeg3\n" + 
			"audio/x-aifc aifc\n" + 
			"audio/x-aiff aif aiff\n" + 
			"audio/x-generic wav wma mp3 ogg\n" + 
			"audio/x-mpeg mpeg mpg\n" + 
			"audio/x-wav wav\n" + 
			"image/gif gif GIF\n" + 
			"image/ief ief\n" + 
			"image/jpeg jpeg jpg jpe JPG\n" + 
			"image/png png PNG\n" + 
			"image/svg+xml svg svgz\n" + 
			"image/tiff tiff tif\n" + 
			"image/x-eps eps\n" + 
			"image/x-generic bmp jpg jpeg png tif tiff xpm wmf emf\n" + 
			"image/x-xwindowdump xwd\n" + 
			"text/css css\n" + 
			"text/csv csv\n" + 
			"text/html html htm HTML HTM\n" + 
			"text/plain txt text TXT TEXT\n" + 
			"text/richtext rtx\n" + 
			"text/rtf rtf\n" + 
			"text/tab-separated-values tsv tab\n" + 
			"text/x-bibtex bib\n" + 
			"text/x-c++hdr h\n" + 
			"text/x-csrc c\n" + 
			"text/x-c++src cpp c++\n" + 
			"text/x-java java\n" + 
			"text/x-log log\n" + 
			"text/xml xml XML osm\n" + 
			"text/x-pascal pas\n" + 
			"text/x-po po pot\n" + 
			"text/x-python py\n" + 
			"text/x-sql sql\n" + 
			"text/x-tcl tcl\n" + 
			"text/x-tex tex\n" + 
			"video/mpeg mpeg mpg mpe mpv vbs mpegv\n" + 
			"video/msvideo avi\n" + 
			"video/quicktime qt mov moov\n" + 
			"video/x-generic wmv mpeg mp4 ogv swf mov dvd osp\n" + 
			"video/x-msvideo avi";
	
    private static MimetypesFileTypeMap MAP = createMap();  

    private static MimetypesFileTypeMap createMap() {
        try {
        	InputStream stream = new ByteArrayInputStream(types.getBytes(StandardCharsets.UTF_8));
            return new MimetypesFileTypeMap(stream);      
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Infer MIME-type from file name
     * @param fileName	May include path.
     * @return	MIME-type. application/octet-stream if unknown.
     */
    public static String get(String fileName) {
        return MAP.getContentType(fileName.toLowerCase());
    }
    
}