package com.itextpdf.pdfocr;

public class PdfOcrLogMessageConstant {
    public static final String CANNOT_READ_INPUT_IMAGE =
            "Cannot read input image {0}";
    public static final String PROVIDED_FONT_PROVIDER_IS_INVALID =
            "Provided FontProvider is invalid. Please check that it contains "
                    + "valid fonts and default font family name.";
    public static final String CANNOT_READ_DEFAULT_FONT =
            "Cannot default read font: {0}";
    public static final String CANNOT_ADD_DATA_TO_PDF_DOCUMENT =
            "Cannot add data to PDF document: {1}";
    public static final String START_OCR_FOR_IMAGES =
            "Starting ocr for {0} image(s)";
    public static final String NUMBER_OF_PAGES_IN_IMAGE =
            "Image {0} contains {1} page(s)";
    public static final String COULD_NOT_FIND_CORRESPONDING_GLYPH_TO_UNICODE_CHARACTER =
            "Could not find a glyph corresponding to Unicode character {0} "
                    + "in any of the fonts";
    public static final String PDF_LANGUAGE_PROPERTY_IS_NOT_SET =
            "PDF language property is not set";

    private PdfOcrLogMessageConstant() {
    }
}
