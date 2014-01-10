package athena.data

import scala.annotation.implicitNotFound
import akka.util.ByteString
import athena.util.ByteStringUtils._
import java.nio.ByteOrder
import org.joda.time.DateTime
import java.util.{UUID, Date}
import java.math.BigInteger
import java.net.{InetSocketAddress, InetAddress}
import scala.reflect.ClassTag

/**
 * A trait that defines a class that can convert from a cassandra value to another type A.
 *
 * This is used to unmarshal the Cassandra-specific return values from queries into other data types.
 */
@implicitNotFound(
"No Cassandra deserializer found for type ${A}. Try to implement an implicit Reads or Format for this type."
)
trait Reads[A] {
  self =>

  /**
   * Convert the cassandra value into an A
   */
  def reads(cvalue: CValue): CvResult[A]

  def map[B](f: A => B): Reads[B] =
    Reads[B] { cvalue => self.reads(cvalue).map(f) }

  def flatMap[B](f: A => Reads[B]): Reads[B] = Reads[B] { cvalue =>
    self.reads(cvalue).flatMap(t => f(t).reads(cvalue))
  }

  def filter(f: A => Boolean): Reads[A] =
    Reads[A] { cvalue => self.reads(cvalue).filter(f) }

  def filter(error: String)(f: A => Boolean): Reads[A] =
    Reads[A] { cvalue => self.reads(cvalue).filter(error)(f) }

  def filterNot(f: A => Boolean): Reads[A] =
    Reads[A] { cvalue => self.reads(cvalue).filterNot(f) }

  def filterNot(error: String)(f: A => Boolean): Reads[A] =
    Reads[A] { cvalue => self.reads(cvalue).filterNot(error)(f) }

  def collect[B](error: String)(f: PartialFunction[A, B]) =
    Reads[B] { cvalue => self.reads(cvalue).collect(error)(f) }

  def orElse(v: Reads[A]): Reads[A] =
    Reads[A] { cvalue => self.reads(cvalue).orElse(v.reads(cvalue)) }

  def compose[B <: CValue](rb: Reads[B]): Reads[A] =
    Reads[A] { value =>
      rb.reads(value) match {
        case CvSuccess(b) => this.reads(b)
        case CvError(e) => CvError(e)
      }
    }

}

object Reads extends DefaultReads {
  def apply[A](f: CValue => CvResult[A]): Reads[A] = new Reads[A] {
    def reads(value: CValue) = f(value)
  }
}

trait DefaultReads {

  implicit object CValueReads extends Reads[CValue] {
    def reads(cvalue: CValue): CvResult[CValue] = CvSuccess(cvalue)
  }

  implicit def optionReads[T](implicit rt: Reads[T]): Reads[Option[T]] = new Reads[Option[T]] {
      def reads(cvalue: CValue): CvResult[Option[T]] = cvalue match {
        case CNull => CvSuccess(None)
        case x => rt.reads(x).map(Some(_))
      }
  }

  import scala.reflect.runtime.universe._
  def nonNull[T](r: PartialFunction[CValue, CvResult[T]])(implicit t: TypeTag[T]): Reads[T] = {
    new Reads[T] {
      def reads(cvalue: CValue): CvResult[T] = cvalue match {
        case CNull => CvError(s"Cannot convert null value to type ${t.tpe.toString}.")
        case x => if(r.isDefinedAt(x)) r(x) else CvError("Cannot convert value to to type ${t.tpe.toString}.")
      }
    }
  }

  implicit val StringReads: Reads[String] = nonNull {
    case CVarChar(value) => CvSuccess(value)
    case CASCIIString(value) => CvSuccess(value)
    case CBigInt(value) => CvSuccess(value.toString)
    case CBoolean(value) => CvSuccess(value.toString)
    case CCounter(value) => CvSuccess(value.toString)
    case CDecimal(value) => CvSuccess(value.toString)
    case CDouble(value) => CvSuccess(value.toString)
    case CFloat(value) => CvSuccess(value.toString)
    case CInt(value) => CvSuccess(value.toString)
    case CTimestamp(value) => CvSuccess(value.toString)
    case CUUID(value) => CvSuccess(value.toString)
    case CVarInt(value) => CvSuccess(value.toString)
    case CTimeUUID(value) => CvSuccess(value.toString)
    case CInetAddress(address) => CvSuccess(address.getHostAddress)
  }

  implicit val BooleanReads: Reads[Boolean] = nonNull {
    case CBoolean(bool) => CvSuccess(bool)
  }

  implicit val IntReads: Reads[Int] = nonNull {
    case CInt(value) => CvSuccess(value)
  }

  implicit val LongReads: Reads[Long] = nonNull {
    case CInt(value) => CvSuccess(value)
    case CBigInt(value) => CvSuccess(value)
    case CCounter(value) => CvSuccess(value)
  }

  implicit val BigIntReads: Reads[java.math.BigInteger] = nonNull {
    case CVarInt(value) => CvSuccess(value)
  }

  implicit val DoubleReads: Reads[Double] = nonNull {
    case CInt(value) => CvSuccess(value.toDouble)
    case CDouble(value) => CvSuccess(value)
    case CFloat(value) => CvSuccess(value.toDouble)
  }

  implicit val BigDecimalReads: Reads[java.math.BigDecimal] = nonNull {
    case CDecimal(value) => CvSuccess(value)
  }

  implicit val JodaDateReads: Reads[DateTime] = nonNull {
    case CTimestamp(date) => CvSuccess(date)
  }

  implicit val DateReads: Reads[Date] = nonNull {
    case CTimestamp(date) => CvSuccess(date.toDate)
  }

  implicit val UUIDReads: Reads[UUID] = nonNull {
    case CUUID(value) => CvSuccess(value)
    case CTimeUUID(value) => CvSuccess(value)
  }

  implicit val InetAddressReads: Reads[InetAddress] = nonNull {
    case CInetAddress(address) => CvSuccess(address)
  }

  implicit def mapReads[A, B](implicit keyReads: Reads[A], valueReads: Reads[B], a: TypeTag[A], b: TypeTag[B]): Reads[Map[A, B]] = nonNull {
    case CMap(values) =>
      val converted: Iterable[CvResult[(A, B)]] = values.map {
        case (key, value) => keyReads.reads(key).zip(valueReads.reads(value))
      }
      CvResult.sequence(converted).map(_.toMap)
  }

  implicit def seqReads[A](implicit valueReads: Reads[A], a: TypeTag[A]): Reads[Seq[A]] = nonNull {
    case CList(values) =>
      CvResult.sequence(values.map(valueReads.reads(_)))
  }

  implicit def setReads[A](implicit valueReads: Reads[A], a: TypeTag[A]): Reads[Set[A]] = nonNull {
    case CList(values) => CvResult.sequence(values.map(valueReads.reads(_)).toSet)
    case CSet(values) => CvResult.sequence(values.map(valueReads.reads(_)))
  }

  implicit object ByteStringReads extends Reads[ByteString] {

    private implicit val byteOrder = ByteOrder.BIG_ENDIAN
    /**
     * Convert the cassandra value into an A
     */
    def reads(cvalue: CValue): CvResult[ByteString] = cvalue match {
      case CASCIIString(value) => CvSuccess(ByteString(value, "ASCII"))
      case CBigInt(value) => CvSuccess(newBuilder(8).putLong(value).result())
      case CBlob(bytes) => CvSuccess(bytes)
      case CBoolean(value) => CvSuccess(if(value) CBoolean.TRUE_BUFFER else CBoolean.FALSE_BUFFER)
      case CCounter(value) => CvSuccess(newBuilder(8).putLong(value).result())
      case CDecimal(value) =>
        val scale = value.scale()
        val intBytes = value.unscaledValue().toByteArray
        CvSuccess(newBuilder(4 + intBytes.length).putInt(scale).putBytes(intBytes).result())
      case CDouble(value) => CvSuccess(newBuilder(8).putDouble(value).result())
      case CFloat(value) => CvSuccess(newBuilder(4).putFloat(value).result())
      case CInt(value) => CvSuccess(newBuilder(4).putInt(value).result())
      case CTimestamp(value) => CvSuccess(newBuilder(8).putLong(value.getMillis).result())
      case CUUID(value) =>
        val uuid = newBuilder(16)
          .putLong(value.getMostSignificantBits)
          .putLong(value.getLeastSignificantBits)
          .result()
        CvSuccess(uuid)
      case CVarChar(value) => CvSuccess(ByteString(value, "UTF-8"))
      case CVarInt(value) => CvSuccess(ByteString(value.toByteArray))
      case CTimeUUID(value) =>
        val uuid = newBuilder(16)
          .putLong(value.getMostSignificantBits)
          .putLong(value.getLeastSignificantBits)
          .result()
        CvSuccess(uuid)
      case CInetAddress(value) =>  CvSuccess(ByteString(value.getAddress))
      case CMap(values) =>
        val length = values.size
        val strings = values.map {
          case (key, value) =>
            CValue.fromValue[ByteString](key).combine(CValue.fromValue[ByteString](value)) { (convertedKey, convertedValue) =>
                convertedKey ++ convertedValue
            }
        }
        CvResult.sequence(strings).map { byteStrings =>
          ParamCodecUtils.packParamByteStrings(byteStrings, length)
        }
      case CList(values) =>
        CvResult.sequence(values.map(CValue.fromValue[ByteString])).map { byteStrings =>
          ParamCodecUtils.packParamByteStrings(byteStrings, byteStrings.size)
        }
      case CSet(values) =>
        CvResult.sequence(values.map(CValue.fromValue[ByteString])).map { byteStrings =>
          ParamCodecUtils.packParamByteStrings(byteStrings, byteStrings.size)
        }
      case CCustomValue(_, _) => CvError("Custom values are not supported.")
      case CNull => CvSuccess(ByteString.empty)
    }
  }

  
}