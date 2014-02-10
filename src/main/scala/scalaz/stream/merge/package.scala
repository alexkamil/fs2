package scalaz.stream

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.Process._


package object merge {


  /**
   * Merges non-deterministically processes that are output of the `source` process.
   *
   * Merging stops when all processes generated by source have stopped, and all source process stopped as well.
   * Merging will also stop when resulting process terminated. In that case the cleanup of all `source`
   * processes is run, followed by cleanup of resulting process.
   *
   * When one of the source processes fails the mergeN process will fail with that reason.
   *
   * Merging is non-deterministic, but is fair in sense that every process is consulted, once it has `A` ready.
   * That means processes that are `faster` provide it's `A` more often than slower processes.
   *
   * Internally mergeN keeps small buffer that reads ahead up to `n` values of `A` where `n` equals to number
   * of active source streams. That does not mean that every `source` process is consulted in this read-ahead
   * cache, it just tries to be as much fair as possible when processes provide their `A` on almost the same speed.
   *
   */
  def mergeN[A](source: Process[Task, Process[Task, A]])
    (implicit S: Strategy = Strategy.DefaultStrategy): Process[Task, A] =
    mergeN(0)(source)(S)

  /**
   * MergeN variant, that allows to specify maximum of open `source` processes.
   * If, the maxOpen is <= 0 it acts like standard mergeN, where the number of processes open is not limited.
   * However, when the maxOpen > 0, then at any time only `maxOpen` processes will be running at any time
   *
   * This allows for limiting the eventual concurrent processing of opened streams not only by supplied strategy,
   * but also by providing a `maxOpen` value.
   *
   *
   * @param maxOpen   Max number of open (running) processes at a time
   * @param source    source of processes to merge
   */
  def mergeN[A](maxOpen: Int)(source: Process[Task, Process[Task, A]])
    (implicit S: Strategy = Strategy.DefaultStrategy): Process[Task, A] =
    await(Task.delay(Junction(JunctionStrategies.mergeN[A](maxOpen), source)(S)))({
      case junction => junction.downstreamO onComplete eval_(junction.downstreamClose(End))
    })


}
