package com.mylucene.utils

import scala.io.Source

object Utils {
  //读取文件内容
  def readFileContent(filePath: String) = {
    val content = new StringBuffer()
    for (line <- Source.fromFile(filePath).getLines)
      content.append(line).append("\n")
    content.toString
  }
}
