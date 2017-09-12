package utils.export

import java.io.{ByteArrayOutputStream, File}

import controllers.de.fuhsen.engine.GenerateExcelJsonRequest
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}


/**
  * Writes entities to an Excel Workbook.
  * Each type is written to a separate sheet.
  */
object ExcelSink {

  def generate(request: GenerateExcelJsonRequest) : ByteArrayOutputStream = {
    val wb = new XSSFWorkbook()
    val createHelper = wb.getCreationHelper
    for(sheetJson <- request.sheets) {
      val sheet = wb.createSheet(sheetJson.name)
      for((rowJson, rowIdx) <- sheetJson.rows.zipWithIndex) {
        val row = sheet.createRow(rowIdx.toShort)
        for((cellValue, cellIdx) <- rowJson.zipWithIndex) {
          val cell = row.createCell(cellIdx)
          cell.setCellValue(createHelper.createRichTextString(cellValue))
        }
//        row.createCell(1).setCellValue(1.2)
//        row.createCell(2).setCellValue(createHelper.createRichTextString("This is a string"))
//        row.createCell(3).setCellValue(true)
      }
    }
    // Write the output to a file
    val fileOut = new ByteArrayOutputStream()
    wb.write(fileOut)
    fileOut
  }

}
