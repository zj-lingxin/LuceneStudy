package com.others

class HowToBreak {
  //Use some construct that includes a conditional that you test.
  def howToBreak1 = {
    var sum = 0
    (0 to 10).toStream.takeWhile(_ => sum < 10).foreach(i => sum += i)
    sum
  }

  //You can also use a guard in your loop:
  def howToBreak2 = {
    var sum = 0
    for (i <- 0 to 10 if (sum < 10))
      sum += i
    sum
  }

  //Put the code into a method and use return.
  def howToBreak3: Int = {
    var sum = 0
    0 to 10 foreach(i => if(sum < 10) sum += i; else return sum)
    sum
  }

  // In Scala 2.8+ this is already pre-packaged in scala.util.control.
  // Breaks using syntax that looks a lot like your familiar old break from C/Java:
  def howToBreak4 = {
    var sum = 0
    import scala.util.control.Breaks._
    breakable {
      0 to 10 foreach (i => if (sum < 10) sum += i else break)
    }
    sum
  }

  //Use tail recursion instead of a for loop, taking advantage of how easy it is to write a new method in Scala:
  def howToBreak5 = {
    var sum = 0
    def add(i: Int, total: Int) {
      sum += i
      if (sum < 10) add(i + 1, total)
    }
    add(0, sum)
    sum
  }
}
