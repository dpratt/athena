package athena.util

import scala.concurrent.duration._

//inherits from anyval to allow representation at runtime to just be a Long
class Timestamp private (val timestampNanos: Long) extends AnyVal {
  def +(period: Duration): Timestamp =
    if (isNever) this
    else if (!period.isFinite()) Timestamp.never
    else new Timestamp(timestampNanos + period.toNanos)

  def -(other: Timestamp): Duration =
    if (isNever) Duration.Inf
    else if (other.isNever) Duration.MinusInf
    else (timestampNanos - other.timestampNanos).nanos

  def isPast: Boolean = (System.nanoTime() - Timestamp.zero) >= timestampNanos
  def isPast(now: Timestamp): Boolean = now.timestampNanos >= timestampNanos
  def isFuture: Boolean = !isPast

  def isFinite: Boolean = timestampNanos < Long.MaxValue
  def isNever: Boolean = timestampNanos == Long.MaxValue
}

object Timestamp {
  private val zero = System.nanoTime()
  def now: Timestamp = new Timestamp(System.nanoTime()-zero)
  def never: Timestamp = new Timestamp(Long.MaxValue)

  implicit val timestampOrdering: Ordering[Timestamp] = new Ordering[Timestamp] {
    def compare(x: Timestamp, y: Timestamp): Int =
      if (x.timestampNanos < y.timestampNanos) -1 else if (x.timestampNanos == y.timestampNanos) 0 else 1
  }
}

