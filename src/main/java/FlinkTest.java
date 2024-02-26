import java.util.Properties;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkTest {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<String> source = KafkaSource.<String>builder()
            .setBootstrapServers("localhost:9092")
            .setTopics("source")
            .setGroupId("my-group")
            .setStartingOffsets(OffsetsInitializer.latest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();

        DataStream<String> sourceStream = env.fromSource(
                source, WatermarkStrategy.forMonotonousTimestamps(), "Kafka Source")
            .uid("kafkasourceuid");

        KafkaRecordSerializationSchema<String> serializer = KafkaRecordSerializationSchema.builder()
            .setValueSerializationSchema(new SimpleStringSchema())
            .setTopic("sink")
            .build();
        Properties kprops = new Properties();
        kprops.setProperty("transaction.timeout.ms", "300000"); // e.g., 5 mins
        KafkaSink<String> sink = KafkaSink.<String>builder()
            .setBootstrapServers("localhost:9092")
            .setRecordSerializer(serializer)
            .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
            .setKafkaProducerConfig(kprops)
            .setTransactionalIdPrefix("txn-prefix")
            .build();

        sourceStream.sinkTo(sink);
        env.enableCheckpointing(10000L);
        env.getCheckpointConfig().setCheckpointTimeout(60000);
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500L);
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(1);
        env.getCheckpointConfig().setCheckpointStorage("file:///tmp/checkpoints");
        env.execute("tester");
    }
}
