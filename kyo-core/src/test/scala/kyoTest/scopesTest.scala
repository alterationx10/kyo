package kyoTest

import java.io.Closeable

import kyo.core._
import kyo.scopes._
import kyo.options._

class scopesTest extends KyoTest {

  trait Context {
    case class Resource(id: Int, var closes: Int = 0) extends Closeable {
      def close() = closes += 1
    }

    val r1 = Resource(1)
    val r2 = Resource(2)
  }

  "acquire + close" in new Context {
    Scopes.close(Scopes.acquire(r1))
    assert(r1.closes == 1)
    assert(r2.closes == 0)
  }

  "acquire + tranform + close" in new Context {
    Scopes.close(Scopes.acquire(r1)(_ => assert(r1.closes == 0)))
    assert(r1.closes == 1)
    assert(r2.closes == 0)
  }

  "acquire + effectful tranform + close" in new Context {
    val r =
      Scopes.close(Scopes.acquire(r1) { _ =>
        assert(r1.closes == 0)
        Option(1) > Options
      })
    assert(r1.closes == 0)
    assert(r2.closes == 0)
    r < Options
    assert(r1.closes == 1)
    assert(r2.closes == 0)
  }

  "two acquires + close" in new Context {
    Scopes.close(Scopes.acquire(r1)(_ => Scopes.acquire(r2)))
    assert(r1.closes == 1)
    assert(r2.closes == 1)
  }

  "two acquires + for-comp + close" in new Context {
    val r: Int =
      Scopes.close {
        for {
          r1 <- Scopes.acquire(r1)
          i1 <- r1.id * 3
          r2 <- Scopes.acquire(r2)
          i2 <- r2.id * 5
        } yield i1 + i2
      }
    assert(r == 13)
    assert(r1.closes == 1)
    assert(r2.closes == 1)
  }

  "two acquires + effectful for-comp + close" in new Context {
    val r: Int > Options =
      Scopes.close {
        for {
          r1 <- Scopes.acquire(r1)
          i1 <- Option(r1.id * 3) > Options
          r2 <- Scopes.acquire(r2)
          i2 <- Option(r2.id * 3) > Options
        } yield i1 + i2
      }
    assert(r1.closes == 0)
    assert(r2.closes == 0)
    r < Options
    assert(r1.closes == 1)
    assert(r2.closes == 1)
  }

}
