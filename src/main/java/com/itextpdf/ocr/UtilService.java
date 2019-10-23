package com.itextpdf.ocr;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.geom.Rectangle;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * Helper class.
 *
 */
final class UtilService {

    /**
     * Constant to convert pixels to points (for tests).
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static final float PX_TO_PT = 3f / 4f;

    /**
     * Constantsfor points per inch (for tests).
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static final float POINTS_PER_INCH = 72.0f;

    /**
     * UtilService logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(UtilService.class);

    /**
     * Simple UtilService constructor.
     */
    private UtilService() {
    }

    /**
     * Run given command in command line.
     *
     * @param command List<String>
     * @param isWindows boolean
     * @return true if command succeeded, false - if there ere some errors
     */
    static boolean runCommand(final List<String> command,
                              final boolean isWindows) {
        LOGGER.info("Running command: " + String.join(" ", command));
        boolean cmdSucceeded = true;
        try {
            Process process;
            if (isWindows) {
                String cmd =  String.join(" ", command);
                process  = Runtime.getRuntime().exec(cmd, null);
            } else {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                        String.join(" ", command));
                pb.redirectErrorStream(true);
                process = pb.start();
            }

            int exitVal = process.waitFor();

            cmdSucceeded = exitVal == 0;
            if (!cmdSucceeded) {
                LOGGER.error("Error occurred during running command: "
                        + String.join(" ", command));
            }

            process.destroy();
        } catch (IOException | InterruptedException e) {
            cmdSucceeded = false;
            LOGGER.error("Error occurred:" + e.getLocalizedMessage());
        }

        return cmdSucceeded;
    }

    /**
     * Parse `hocr` file, retrieve text, and return in the format
     * described below.
     * each list element : Map.Entry<String, List<Integer>> contains
     * word or line as a key and its 4 coordinates(bbox) as a values
     *
     * @param inputFile File
     * @return List<TextInfo>
     * @throws IOException IOException
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static List<TextInfo> parseHocrFile(final File inputFile)
            throws IOException {
        List<TextInfo> textData = new ArrayList<>();

        // Using the jericho library to parse the HTML file
        Source source = new Source(inputFile);

        // In order to place text behind the recognised text snippets
        // we are interested in the bbox property
        Pattern bboxPattern = Pattern.compile("bbox(\\s+\\d+){4}");
        // This pattern separates the coordinates of the bbox property
        Pattern bboxCoordinatePattern = Pattern
                .compile("(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");

        Pattern pagePattern = Pattern.compile("page_(\\d+)");

        String searchedTag = "ocrx_word"; // ocr_line // ocrx_word

        StartTag pageBlock = source.getNextStartTag(0, "class",
                "ocr_page", false);
        while (pageBlock != null) {
            Element pageElement = pageBlock.getElement();
            String valueId = pageElement.getAttributeValue("id");
            Matcher pageMatcher = pagePattern.matcher(valueId);
            if (pageMatcher.find()) {
                String matchedPageString = pageMatcher.group()
                        .split("page_")[1];
                int matchedPage = Integer.parseInt(matchedPageString);
                EndTag pageBlockEnd = pageElement.getEndTag();
                StartTag ocrTag = source.getNextStartTag(pageBlock.getBegin(),
                        "class", searchedTag, false);

                while (ocrTag != null
                        && ocrTag.getBegin() < pageBlockEnd.getEnd()) {
                    Element lineElement = ocrTag.getElement();
                    String valueTitle = lineElement
                            .getAttributeValue("title");
                    Matcher bboxMatcher = bboxPattern.matcher(valueTitle);
                    if (bboxMatcher.find()) {
                        // We found a tag of the ocr_line class containing
                        // a bbox property
                        Matcher bboxCoordinateMatcher = bboxCoordinatePattern
                                .matcher(bboxMatcher.group());
                        bboxCoordinateMatcher.find();

                        List<Integer> coordinates1 = IntStream
                                .range(0, 4)
                                .boxed()
                                .map(i -> Integer.parseInt((
                                        bboxCoordinateMatcher.group(i + 1))))
                                .collect(Collectors.toList());

                        String line = lineElement.getContent()
                                .getTextExtractor().toString();

                        textData.add(new TextInfo(line, matchedPage,
                                coordinates1));
                    }
                    ocrTag = source.getNextStartTag(ocrTag.getEnd(),
                            "class", searchedTag, false);
                }
            }
            pageBlock = source.getNextStartTag(pageBlock.getEnd(),
                    "class", "ocr_page", false);
        }

        return textData;
    }

    /**
     * Calculate the size of the PDF document page
     * should transform pixels to points and according to image resolution.
     *
     * @param imageData ImageData
     * @param scaleMode IPdfRenderer.ScaleMode
     * @param requiredSize Rectangle
     * @return Rectangle
     */
    public static Rectangle calculatePageSize(final ImageData imageData,
                                              final IPdfRenderer.ScaleMode
                                                      scaleMode,
                                              final Rectangle requiredSize) {
        // Adjust image size and dpi
        // The resolution of a PDF file is 72pt per inch
        float dotsPerPointX = 1.0f;
        float dotsPerPointY = 1.0f;
        if (imageData != null && imageData.getDpiX() > 0) {
            dotsPerPointX = imageData.getDpiX() / POINTS_PER_INCH;
            dotsPerPointY = imageData.getDpiY() / POINTS_PER_INCH;
        }

        if (imageData != null) {
            float imgWidthPt = getPoints(imageData.getWidth());
            float imgHeightPt = getPoints(imageData.getHeight());
            LOGGER.info("Original image size in pixels: ("
                    + imageData.getWidth() + ", "
                    + imageData.getHeight() + ")");
            if (scaleMode == IPdfRenderer.ScaleMode.keepOriginalSize) {
                Rectangle size = new Rectangle(imgWidthPt, imgHeightPt);

                LOGGER.info("Final size in points: (" + size.getWidth() + ", "
                        + size.getHeight() + ")");
                return size;
            } else {
                // scale image and add to canvas to background
                if (scaleMode == IPdfRenderer.ScaleMode.scaleHeight) {
                    float newHeight = imgHeightPt
                            * requiredSize.getWidth()  / imgWidthPt;
                    requiredSize.setHeight(newHeight);
                } else if (scaleMode == IPdfRenderer.ScaleMode.scaleWidth) {
                    float newWidth = imgWidthPt
                            * requiredSize.getHeight()  / imgHeightPt;
                    requiredSize.setWidth(newWidth);
                }
                LOGGER.info("Final size in points: (" + requiredSize.getWidth()
                        + ", " + requiredSize.getHeight() + ")");
                return requiredSize;
            }
        } else {
            return requiredSize;
        }
    }

    /**
     * Convert from pixels to points.
     *
     * @param pixels float
     * @return float
     */
    public static float getPoints(final float pixels) {
        return pixels * PX_TO_PT;
    }

    /**
     * Retrieve text for specified page.
     *
     * @param data List<TextInfo>
     * @param page Integer
     * @return List<TextInfo>
     */
    public static List<TextInfo> getTextForPage(final List<TextInfo> data,
                                                final Integer page) {
        return data.stream()
                .filter(item -> item.getPage().equals(page))
                .collect(Collectors.toList());
    }

}
