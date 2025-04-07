import org.apache.kafka.clients.consumer.ConsumerRecord;

public class ConsumerFactory {
    public static ConsumerRecord createConsumerRecord() {
        return new ConsumerRecord<String, String>("", 0, 0L, "key", null);
    }
}
