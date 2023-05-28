import java.util.Arrays;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitSerializer;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.state.api.OperatorIdentifier;
import org.apache.flink.state.api.SavepointReader;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.TestUtils;
import org.apache.flink.streaming.api.operators.util.SimpleVersionedListState;
import org.apache.flink.util.CloseableIterator;

public class StateProcessorTest {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        String savepointPath = StateProcessorTest.class.getResource("/savepoint-27c85e-10ecaba6c582").getPath();
        SavepointReader savepoint = SavepointReader.read(env, savepointPath, new HashMapStateBackend());
        System.out.println(savepoint);
        DataStream<byte[]> listState = savepoint.readListState(
            OperatorIdentifier.forUid("kafkasourceuid"),
            "SourceReaderState",
            PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO);
        CloseableIterator<byte[]> a = listState.executeAndCollect();
        TestUtils.MockListState<byte[]> rawBytes = new TestUtils.MockListState<>();
        a.forEachRemaining(rawBytes::add);
        SimpleVersionedListState<KafkaPartitionSplit> partitionOffsets = new SimpleVersionedListState<>(rawBytes, new KafkaPartitionSplitSerializer());
        for (KafkaPartitionSplit split : partitionOffsets.get()) {
            System.out.println(String.format("topic=%s, partition=%s, startingOffset=%s, stoppingOffset=%s, topicPartition=%s",
                    split.getTopic(), split.getPartition(),
                    split.getStartingOffset(), split.getStoppingOffset(), split.getTopicPartition()));
        }

        System.out.println("DONE");
    }
}
