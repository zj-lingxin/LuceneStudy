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
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc, TopDocs}
import org.apache.lucene.store.{Directory, FSDirectory, IOContext, RAMDirectory}

object HelloLucene {
  val docsPath = "D:\\study\\IdeaProject\\LuceneStudy\\luceneDataSource\\"
  val indexPath = "D:\\study\\IdeaProject\\LuceneStudy\\luceneIndex\\"

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
          try {
            indexDoc(writer, file)
          }
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

  def doPageingSearch(searcher: IndexSearcher, query: Query, hitsPerPage: Int, raw: Boolean = false) = {
    // Collect enough docs to show 2 pages
    val results: TopDocs = searcher.search(query, 2 * hitsPerPage)
    val hits: Array[ScoreDoc] = results.scoreDocs
    val numTotalHits: Int = results.totalHits
    println(s"总共有【${numTotalHits}】条匹配结果")
    val start = 0
    var end = Math.min(numTotalHits, hitsPerPage)

    end = Math.min(numTotalHits, start + hitsPerPage)
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

  private def printDocumentInfo(doc: Document) = {
    /*
      获取name属性的值的两种方法：
      1、doc.getField("name").stringValue()
      2、doc.get("name")
     */
    println("name = " + doc.get("name"))
    println("content = " + doc.get("content"))
    println("size = " + doc.get("size"))
    println("path = " + doc.get("path"))
    println("-----------------------------------------")
  }

  def main(args: Array[String]) {
    search()
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
    println(s"总共有【${results.totalHits}】条匹配结果")
    //3、打印结果
    //方式一
    /*
    for (scoreDoc <- results.scoreDocs) {
      val docID = scoreDoc.doc //文档内部编号
      val doc: Document = searcher.doc(docID) //根据文档编号取出相应的文档
      printDocumentInfo(doc)
    }*/
    //方式二
    results.scoreDocs.foreach(scoreDoc => printDocumentInfo(searcher.doc(scoreDoc.doc)))
  }
}

