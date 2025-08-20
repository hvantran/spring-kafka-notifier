package hoatv.kafka.notifier.repository;

import hoatv.kafka.notifier.model.NotifierConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotifierConfigurationRepository extends MongoRepository<NotifierConfiguration, String> {
    
    List<NotifierConfiguration> findByTopic(String topic);
    
    List<NotifierConfiguration> findByEnabledTrue();
    
    List<NotifierConfiguration> findByTopicAndEnabledTrue(String topic);
    
    List<NotifierConfiguration> findByNotifier(String notifier);
    
    Optional<NotifierConfiguration> findByNotifierAndTopic(String notifier, String topic);
    
    @Query("{ 'enabled': true, 'topic': { $in: ?0 } }")
    List<NotifierConfiguration> findEnabledConfigurationsByTopics(List<String> topics);
    
    boolean existsByNotifierAndTopic(String notifier, String topic);
}
