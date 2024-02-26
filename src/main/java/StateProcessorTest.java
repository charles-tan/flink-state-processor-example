import java.nio.file.Path;
import java.util.Arrays;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitSerializer;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.state.api.OperatorIdentifier;
import org.apache.flink.state.api.SavepointReader;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.CloseableIterator;

public class StateProcessorTest {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        String savepointPath = Path.of("/tmp/checkpoints/609bc335486ca6cfcc8692e4c1ff8782/chk-8").toString();
        SavepointReader savepoint = SavepointReader.read(env, savepointPath, new HashMapStateBackend());
        DataStream<byte[]> listState = savepoint.readListState(
            OperatorIdentifier.forUid("kafkasourceuid"),
            "SourceReaderState",
            PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO);
        CloseableIterator<byte[]> states = listState.executeAndCollect();
        while (states.hasNext()) {
            byte[] s = states.next();
            KafkaPartitionSplitSerializer serializer = new KafkaPartitionSplitSerializer();
            KafkaPartitionSplit split = serializer.deserialize(serializer.getVersion(), Arrays.copyOfRange(s, 8, s.length));
            System.out.println(
                String.format("topic=%s, partition=%s, startingOffset=%s, stoppingOffset=%s, topicPartition=%s",
                    split.getTopic(), split.getPartition(),
                    split.getStartingOffset(), split.getStoppingOffset(), split.getTopicPartition()));
        }

        System.out.println("DONE");
    }
}
