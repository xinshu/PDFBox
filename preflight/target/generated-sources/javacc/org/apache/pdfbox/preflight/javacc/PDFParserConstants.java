/* Generated By:JavaCC: Do not edit this line. PDFParserConstants.java */
package org.apache.pdfbox.preflight.javacc;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface PDFParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int SPACE = 1;
  /** RegularExpression Id. */
  int OTHER_WHITE_SPACE = 2;
  /** RegularExpression Id. */
  int EOL = 3;
  /** RegularExpression Id. */
  int PERCENT = 4;
  /** RegularExpression Id. */
  int PDFA_HEADER = 5;
  /** RegularExpression Id. */
  int BINARY_TAG = 6;
  /** RegularExpression Id. */
  int HTML_OPEN = 7;
  /** RegularExpression Id. */
  int HTML_CLOSE = 8;
  /** RegularExpression Id. */
  int END_OBJECT = 9;
  /** RegularExpression Id. */
  int STREAM = 10;
  /** RegularExpression Id. */
  int OBJ_BOOLEAN = 11;
  /** RegularExpression Id. */
  int OBJ_NUMERIC = 12;
  /** RegularExpression Id. */
  int OBJ_STRING_HEX = 13;
  /** RegularExpression Id. */
  int OBJ_STRING_LIT = 14;
  /** RegularExpression Id. */
  int OBJ_ARRAY_START = 15;
  /** RegularExpression Id. */
  int OBJ_ARRAY_END = 16;
  /** RegularExpression Id. */
  int OBJ_NAME = 17;
  /** RegularExpression Id. */
  int OBJ_NULL = 18;
  /** RegularExpression Id. */
  int OBJ_REF = 19;
  /** RegularExpression Id. */
  int START_OBJECT = 20;
  /** RegularExpression Id. */
  int DIGITS = 21;
  /** RegularExpression Id. */
  int LOWERLETTER = 22;
  /** RegularExpression Id. */
  int UPPERLETTER = 23;
  /** RegularExpression Id. */
  int UNICODE = 25;
  /** RegularExpression Id. */
  int UNBALANCED_LEFT_PARENTHESES = 26;
  /** RegularExpression Id. */
  int UNBALANCED_RIGHT_PARENTHESES = 27;
  /** RegularExpression Id. */
  int END_LITERAL = 28;
  /** RegularExpression Id. */
  int INNER_START_LIT = 29;
  /** RegularExpression Id. */
  int END_STREAM = 31;
  /** RegularExpression Id. */
  int XREF_TAG = 32;
  /** RegularExpression Id. */
  int FULL_LINE = 33;
  /** RegularExpression Id. */
  int SUBSECTION_START = 34;
  /** RegularExpression Id. */
  int SUBSECTION_ENTRIES = 35;
  /** RegularExpression Id. */
  int FIRST_OBJECT_NUMBER = 36;
  /** RegularExpression Id. */
  int TRAILER_TAG = 37;
  /** RegularExpression Id. */
  int START_DICTONNARY = 38;
  /** RegularExpression Id. */
  int END_DICTONNARY = 39;
  /** RegularExpression Id. */
  int STARTXREF_TAG = 40;
  /** RegularExpression Id. */
  int OBJ_NUMBER = 41;
  /** RegularExpression Id. */
  int EOF_TRAILER_TAG = 42;

  /** Lexical state. */
  int DEFAULT = 0;
  /** Lexical state. */
  int WithinTrailer = 1;
  /** Lexical state. */
  int CrossRefTable = 2;
  /** Lexical state. */
  int WithinLIT = 3;
  /** Lexical state. */
  int WithinStream = 4;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "<OTHER_WHITE_SPACE>",
    "<EOL>",
    "\"%\"",
    "<PDFA_HEADER>",
    "<BINARY_TAG>",
    "<HTML_OPEN>",
    "<HTML_CLOSE>",
    "<END_OBJECT>",
    "<STREAM>",
    "<OBJ_BOOLEAN>",
    "<OBJ_NUMERIC>",
    "<OBJ_STRING_HEX>",
    "<OBJ_STRING_LIT>",
    "\"[\"",
    "\"]\"",
    "<OBJ_NAME>",
    "\"null\"",
    "<OBJ_REF>",
    "<START_OBJECT>",
    "<DIGITS>",
    "<LOWERLETTER>",
    "<UPPERLETTER>",
    "<token of kind 24>",
    "<UNICODE>",
    "\"\\\\(\"",
    "\"\\\\)\"",
    "\")\"",
    "<INNER_START_LIT>",
    "<token of kind 30>",
    "\"endstream\"",
    "\"xref\"",
    "<FULL_LINE>",
    "<SUBSECTION_START>",
    "<SUBSECTION_ENTRIES>",
    "<FIRST_OBJECT_NUMBER>",
    "\"trailer\"",
    "\"<<\"",
    "\">>\"",
    "\"startxref\"",
    "<OBJ_NUMBER>",
    "\"%%EOF\"",
  };

}
