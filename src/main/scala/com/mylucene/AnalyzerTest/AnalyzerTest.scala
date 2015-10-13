package com.mylucene.AnalyzerTest

import java.io.StringReader

import org.apache.lucene.analysis.cjk.CJKAnalyzer
import org.apache.lucene.analysis.core.{KeywordAnalyzer, SimpleAnalyzer, StopAnalyzer}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.{Analyzer, TokenStream}
import org.junit.Test

/**
 * Created by lingx on 2015/10/10.
 */
class AnalyzerTest {
  val text = "IndexWriter addDocument's a javadoc.txt 方法动画风格和规范化的发个电话官方发个电话g"
  val text2 = "我们是中国人"
  val analyzer = new StandardAnalyzer()
  //单字分词
  val analyzer2 = new SimpleAnalyzer()
  val analyzer3 = new KeywordAnalyzer()
  val analyzer4 = new StopAnalyzer()
  val analyzer5 = new CJKAnalyzer() //二分法分词

  @Test
  def test() = {
    analyze(analyzer5, text2).foreach(println)
  }

  /**
   * 将该分词器对某个字符串分词后保存到数组中，并返回
   * @param analyzer
   * @param text
   * @return
   */
  def analyze(analyzer: Analyzer, text: String) = {
    val result = scala.collection.mutable.ArrayBuffer[String]()
    val tokenStream: TokenStream = analyzer.tokenStream(null, new StringReader(text))
    tokenStream.reset()
    while (tokenStream.incrementToken()) {
      result += (tokenStream.getAttribute(classOf[CharTermAttribute]).toString)
    }
    result
  }
}

