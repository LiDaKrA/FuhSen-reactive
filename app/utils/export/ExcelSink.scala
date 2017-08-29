package utils.export

import java.io.{ByteArrayOutputStream, File}
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}


/**
  * Writes entities to an Excel Workbook.
  * Each type is written to a separate sheet.
  */
object ExcelSink {

  def generate() : ByteArrayOutputStream = {
    val wb = new XSSFWorkbook()
    val createHelper = wb.getCreationHelper()

    val sheet = wb.createSheet("Results Sheet")
    val row = sheet.createRow(0.toShort)
    // Create a cell and put a value in it.
    val cell = row.createCell(0)
    cell.setCellValue(1)

    // Or do it on one line.
    row.createCell(1).setCellValue(1.2)
    row.createCell(2).setCellValue(createHelper.createRichTextString("This is a string"))
    row.createCell(3).setCellValue(true)

    // Write the output to a file
    val fileOut = new ByteArrayOutputStream()
    wb.write(fileOut)
    fileOut
  }

}
