package org.apache.spark.graph.impl

import org.apache.spark.graph._
import org.scalatest.FunSuite

class VertexPartitionSuite extends FunSuite {

  test("isDefined, filter") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1))).filter { (vid, attr) => vid == 0 }
    assert(vp.isDefined(0))
    assert(!vp.isDefined(1))
    assert(!vp.isDefined(2))
    assert(!vp.isDefined(-1))
  }

  test("isActive, numActives, replaceActives") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1)))
      .filter { (vid, attr) => vid == 0 }
      .replaceActives(Iterator(0, 2, 0))
    assert(vp.isActive(0))
    assert(!vp.isActive(1))
    assert(vp.isActive(2))
    assert(!vp.isActive(-1))
    assert(vp.numActives == Some(2))
  }

  test("map") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1))).map { (vid, attr) => 2 }
    assert(vp(0) === 2)
  }

  test("diff") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1), (2L, 1)))
    val vp2 = vp.filter { (vid, attr) => vid <= 1 }
    val vp3a = vp.map { (vid, attr) => 2 }
    val vp3b = VertexPartition(vp3a.iterator)
    // diff with same index
    val diff1 = vp2.diff(vp3a)
    assert(diff1(0) === 2)
    assert(diff1(1) === 2)
    assert(diff1(2) === 2)
    assert(!diff1.isDefined(2))
    // diff with different indexes
    val diff2 = vp2.diff(vp3b)
    assert(diff2(0) === 2)
    assert(diff2(1) === 2)
    assert(diff2(2) === 2)
    assert(!diff2.isDefined(2))
  }

  test("leftJoin") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1), (2L, 1)))
    val vp2a = vp.filter { (vid, attr) => vid <= 1 }.map { (vid, attr) => 2 }
    val vp2b = VertexPartition(vp2a.iterator)
    // leftJoin with same index
    val join1 = vp.leftJoin(vp2a) { (vid, a, bOpt) => bOpt.getOrElse(a) }
    assert(join1.iterator.toSet === Set((0L, 2), (1L, 2), (2L, 1)))
    // leftJoin with different indexes
    val join2 = vp.leftJoin(vp2b) { (vid, a, bOpt) => bOpt.getOrElse(a) }
    assert(join2.iterator.toSet === Set((0L, 2), (1L, 2), (2L, 1)))
    // leftJoin an iterator
    val join3 = vp.leftJoin(vp2a.iterator) { (vid, a, bOpt) => bOpt.getOrElse(a) }
    assert(join3.iterator.toSet === Set((0L, 2), (1L, 2), (2L, 1)))
  }

  test("innerJoin") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1), (2L, 1)))
    val vp2a = vp.filter { (vid, attr) => vid <= 1 }.map { (vid, attr) => 2 }
    val vp2b = VertexPartition(vp2a.iterator)
    // innerJoin with same index
    val join1 = vp.innerJoin(vp2a) { (vid, a, b) => b }
    assert(join1.iterator.toSet === Set((0L, 2), (1L, 2)))
    // innerJoin with different indexes
    val join2 = vp.innerJoin(vp2b) { (vid, a, b) => b }
    assert(join2.iterator.toSet === Set((0L, 2), (1L, 2)))
    // innerJoin an iterator
    val join3 = vp.innerJoin(vp2a.iterator) { (vid, a, b) => b }
    assert(join3.iterator.toSet === Set((0L, 2), (1L, 2)))
  }

  test("createUsingIndex") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1), (2L, 1)))
    val elems = List((0L, 2), (2L, 2), (3L, 2))
    val vp2 = vp.createUsingIndex(elems.iterator)
    assert(vp2.iterator.toSet === Set((0L, 2), (2L, 2)))
    assert(vp.index === vp2.index)
  }

  test("innerJoinKeepLeft") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1), (2L, 1)))
    val elems = List((0L, 2), (2L, 2), (3L, 2))
    val vp2 = vp.innerJoinKeepLeft(elems.iterator)
    assert(vp2.iterator.toSet === Set((0L, 2), (2L, 2)))
    assert(vp2(1) === 1)
  }

  test("aggregateUsingIndex") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1), (2L, 1)))
    val messages = List((0L, "a"), (2L, "b"), (0L, "c"), (3L, "d"))
    val vp2 = vp.aggregateUsingIndex[String](messages.iterator, _ + _)
    assert(vp2.iterator.toSet === Set((0L, "ac"), (2L, "b")))
  }

  test("reindex") {
    val vp = VertexPartition(Iterator((0L, 1), (1L, 1), (2L, 1)))
    val vp2 = vp.filter { (vid, attr) => vid <= 1 }
    val vp3 = vp2.reindex()
    assert(vp2.iterator.toSet === vp3.iterator.toSet)
    assert(vp2(2) === 1)
    assert(vp3.index.getPos(2) === -1)
  }

}
