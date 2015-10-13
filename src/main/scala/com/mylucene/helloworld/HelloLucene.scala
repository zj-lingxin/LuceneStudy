package com.mylucene.helloworld

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import com.mylucene.utils.Utils
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{DirectoryReader, IndexReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.{MultiFieldQueryParser, QueryParser}
import org.apache.lucene.search.highlight._
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc, TopDocs}
import org.apache.lucene.store.{Directory, FSDirectory, IOContext, RAMDirectory}

object HelloLucene {
  val docsPath = "E:\\study\\workspace\\LuceneStudy\\luceneDataSource\\"
  val indexPath = "E:\\study\\workspace\\LuceneStudy\\luceneIndex\\"

  //创建分词器
  val analyzer: Analyzer = new StandardAnalyzer()

  //建立索引
  def createIndex() = {
    //创建Directory，相当于索引库的位置
    val directory: Directory = FSDirectory.open(Paths.get(indexPath))
    //创建IndexWriter，IndexWriter用于操作(增、删、改)索引库的。
    val iwc: IndexWriterConfig = new IndexWriterConfig(analyzer)
    // Create a new index in the directory, removing any previously indexed documents:
    iwc.setOpenMode(OpenMode.CREATE)
    val indexWriter: IndexWriter = new IndexWriter(directory, iwc)
    //创建Document对象，为Document添加Field
    val docDir: Path = Paths.get(docsPath)
    indexDocs(indexWriter, docDir)

    //关闭IndexWriter
    indexWriter.close()
  }

  private def indexDocs(writer: IndexWriter, path: Path) = {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          indexDoc(writer, file)
          FileVisitResult.CONTINUE
        }
      })
    } else {
      indexDoc(writer, path)
    }
  }

  private def indexDoc(writer: IndexWriter, path: Path) = {
    val doc = new Document
    val file = path.toFile
    //TextField是进行分词后索引的。需要查询的就是要索引的，不需要查询就不用索引
    doc.add(new TextField("name", file.getName, Field.Store.YES))
    doc.add(new TextField("content", Utils.readFileContent(file.getAbsolutePath), Field.Store.YES))
    doc.add(new LongField("size", file.length(), Field.Store.YES))
    //StringField是不分词直接索引的(把整个值当成一个关键字)
    doc.add(new StringField("path", file.getAbsolutePath, Field.Store.YES))

    //通过IndexWriter添加Document到索引库中
    writer.addDocument(doc)
  }

  /**
   * 同时使用FSDirectory(文件系统目录)和RAMDirectory(内存目录)
   */
  def createIndex2() = {
    //1、启动时读取
    val fsDir = FSDirectory.open(Paths.get(indexPath))
    val ramDir = new RAMDirectory(fsDir, IOContext.DEFAULT)
    val ramIwc: IndexWriterConfig = new IndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE)

    //添加Documents
    val ramIndexWriter = new IndexWriter(ramDir, ramIwc)
    val docDir: Path = Paths.get(docsPath)
    indexDocs(ramIndexWriter, docDir)
    ramIndexWriter.close()

    //2、退出时保存
    val fsIwc: IndexWriterConfig = new IndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE)
    val fsIndexWriter = new IndexWriter(fsDir, fsIwc)
    fsIndexWriter.addIndexes(ramDir)
    fsIndexWriter.close()
  }

  //详细的分页看官网的Demo
  def doPageingSearch(searcher: IndexSearcher, query: Query, hitsPerPage: Int, raw: Boolean = false) = {
    // Collect enough docs to show 2 pages
    val results: TopDocs = searcher.search(query, 2 * hitsPerPage)
    val hits: Array[ScoreDoc] = results.scoreDocs
    val numTotalHits: Int = results.totalHits
    println(s"总共有【${numTotalHits}】条匹配结果")
    val start = 0
    val end = Math.min(numTotalHits, start + hitsPerPage)
    for (i <- start until end) {
      if (raw) {
        println(s"docID:${hits(i).doc};score:${hits(i).score}")
      } else {
        val doc: Document = searcher.doc(hits(i).doc)
        val path = doc.get("path")
        if (path != null) {
          printDocumentInfo(doc)
        } else {
          println(s"${start + 1}. no path for this document")
        }
      }
    }
  }

  private def printDocumentInfo(doc: Document): Unit = {
    println("name = " + doc.get("name"))
    println("content = " + doc.get("content"))
    println("size = " + doc.get("size"))
    println("path = " + doc.get("path"))
    println("abstract = " + doc.get("abstract"))
    println("-----------------------------------------")
  }

  //进行搜索
  def search() = {
    //1、把要搜索的文本解析为Query
    val quseryString = "java"
    val fields = Array("name", "content")
    val queryParser: QueryParser = new MultiFieldQueryParser(fields, analyzer)
    val query: Query = queryParser.parse(quseryString)
    val indexReader: IndexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)))
    val searcher: IndexSearcher = new IndexSearcher(indexReader)
    simpleSerarch(searcher, query)
    //doPageingSearch(searcher, query, 2)
  }

  def simpleSerarch(searcher: IndexSearcher, query: Query) = {
    //2、进行查询
    val results: TopDocs = searcher.search(query, 1000)
    val numTotalHits = results.totalHits
    println(s"总共有【$numTotalHits】条匹配结果")

    //3、打印结果
    for (scoreDoc <- results.scoreDocs) {
      val docID = scoreDoc.doc //文档内部编号
      val doc: Document = searcher.doc(docID) //根据文档编号取出相应的文档

      //使用高亮器，对关键字进行高亮，并且生成摘要，打印出来
      val fragmentSize = 50 //摘要长度
      var hc = getHighlightedField(query, analyzer, "content", doc.get("content"), fragmentSize)
      if (hc == null)
        hc = doc.getFields("content").toString.substring(0, fragmentSize)
      doc.add(new TextField("abstract", hc, Field.Store.NO))

      //打印文档
      printDocumentInfo(doc)
    }
  }

  //高亮器可以截取一段文本(生成摘要),并且让关键字高亮显示(通过指定前缀与后缀实现，即html的标签)
  //摘要是指关键字出现最频繁处的一段文字
  private def getHighlightedField(query: Query, analyzer: Analyzer, fieldName: String, fieldValue: String, fragmentSize: Int = Int.MaxValue): String = {
    val formatter: Formatter = new SimpleHTMLFormatter("<font color='red'>", "</font>")
    val queryScorer: QueryScorer = new QueryScorer(query)
    val highlighter: Highlighter = new Highlighter(formatter, queryScorer)
    highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, fragmentSize)) //设置"摘要"的字符个数是50个
    highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE)
    //返回高亮后的结果，如果当前属性值中没有出现该关键字，则返回null
    highlighter.getBestFragment(analyzer, fieldName, fieldValue) //例如 fieldName = content, fieldValue = doc.get("content")
  }

  def main(args: Array[String]) {
    //createIndex
    search
  }
}

