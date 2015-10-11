package com.mylucene.utils

import java.io.File
import org.apache.lucene.document._
import scala.io.Source

/**
 * Created by xin on 2015/10/9.
 */
object Utils {

  //读取文件内容
  def readFileContent(filePath: String) = {
    val content = new StringBuffer()
    for (line <- Source.fromFile(filePath).getLines)
      content.append(line).append("\n")
    content.toString
  }

}
