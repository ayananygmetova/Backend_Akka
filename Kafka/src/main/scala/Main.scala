import java.util.concurrent.atomic.AtomicLong
import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import ch.qos.logback.classic.{Level,Logger}
import org.slf4j.LoggerFactory
import scala.util.matching.Regex
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main {
  val log = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
  val logger = log.asInstanceOf[Logger].setLevel(Level.OFF)
  implicit val system = ActorSystem("Quickstart")
  implicit val ec = system.dispatcher
  val bootstrapServers = "localhost:9092"
  val config = system.settings.config.getConfig("akka.kafka.producer")


  def findFirstMatchIn(regex: Regex, line:String):String= {
    regex.findFirstMatchIn(line) match {
      case Some(result) => result.toString
      case None => null
    }
  }

  def unitPublisher(num:Int=0, service: String): Unit = {
    val service0: Regex = "\\w*0".r
    val topic = service

    val producerSettings =
      ProducerSettings(config, new StringSerializer, new StringSerializer)
        .withBootstrapServers(bootstrapServers)
    if (findFirstMatchIn(service0, service) == service) {
      val random = scala.util.Random
      val done = Source(Seq.fill(10)(random.nextInt(10)))
          .map(_.toString)
          .map(value => {
            val r = new ProducerRecord[String, String](topic, value)
            Thread.sleep(3000)
            r
          })
          .runWith(Producer.plainSink(producerSettings))
      done.onComplete {
        case Success(_)=>
        case Failure(exception) => println(exception)
      }
    }
    else {
      val done = Source(List(num))
        .map(_.toString)
        .map { elem =>
          new ProducerRecord[String, String](topic, elem)
        }
        .runWith(Producer.plainSink(producerSettings))
      done.onComplete {
        case Success(_)=>
        case Failure(exception) => println(exception)
      }
    }
  }

  def service1(num:Int):Int={
    val res = num*2
    unitPublisher(res, "service1")
    res
  }
  def service2(num:Int):Int={
    val res = num*3
    unitPublisher(res, "service2")
    res
  }

  def unitConsumer(topic:String): Unit={
    val service0: Regex = "\\w*0".r
    class OffsetStore {
      private val offset = new AtomicLong
      def businessLogicAndStoreOffset(record: ConsumerRecord[String, String]): Future[Done] = {
        if (findFirstMatchIn(service0, topic) == topic) {
          println(s"Number generator published: ${record.value}")
          service1(record.value.toInt)
          service2(record.value.toInt)
        }
        else{
          println(s"$topic got: ${record.value}")
          //          log.info(s"${topic} got: ${record.value}")
        }
        offset.set(record.offset)
        Future.successful(Done)
      }
      def loadOffset(): Future[Long] = {
        Future.successful(offset.get)
      }
    }

    val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withClientId("externalOffsetStorage")

    val db = new OffsetStore
    db.loadOffset().map { fromOffset =>
      Consumer
        .plainSource(
          consumerSettings,
          Subscriptions.assignmentWithOffset(
            new TopicPartition(topic, 0) -> fromOffset
          )
        )
        .mapAsync(1)(db.businessLogicAndStoreOffset)
        .toMat(Sink.seq)(DrainingControl.apply)
        .run()
    }
  }

  def main(args: Array[String]): Unit = {
    unitPublisher(service = "service0")
    unitConsumer("service0")
    unitConsumer("service1")
    unitConsumer("service2")
  }

}
