package ru.mai;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class PublisherServer extends PublisherGrpc.PublisherImplBase {
    private static final Logger log = LoggerFactory.getLogger(PublisherServer.class);

    private final Map<String, List<StreamObserver<SubsribeService.Message>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void subscribe(SubsribeService.Subscription request, StreamObserver<SubsribeService.Message> responseObserver) {
        String topic = request.getTopic(); // Get topic if used
        if (!subscribers.containsKey(topic)) {
            log.debug("Adding new topic: {} and inserting new observer: {}", topic, responseObserver);
            List<StreamObserver<SubsribeService.Message>> topicSubscribers = new CopyOnWriteArrayList<>();
            topicSubscribers.add(responseObserver);
            subscribers.put(topic, topicSubscribers);
        } else {
            var topicSubscribers = subscribers.get(topic);
            if (!topicSubscribers.contains(responseObserver)) {
                log.debug("Inserting new observer: {}", responseObserver);
                topicSubscribers.add(responseObserver);
                subscribers.put(topic, topicSubscribers);
            }

        }
//
//        subscribers.computeIfAbsent(topic, topicToAdd ->
//        {
//            log.debug("Adding new topic: {} and inserting new observer: {}", topicToAdd, responseObserver);
//            List<StreamObserver<SubsribeService.Message>> topicSubscribers = new CopyOnWriteArrayList<>();
//            topicSubscribers.add(responseObserver);
//            return topicSubscribers;
//        });
//
//        subscribers.computeIfPresent(topic,
//                (keyTopic, value) -> {
//                    log.debug("Inserting new observer: {}", responseObserver);
//                    value.add(responseObserver);
//                    return value;
//                });

//        responseObserver.onCompleted(); // Signal completion of connection establishment

        // Implement logic to handle client disconnection (e.g., implement on disconnect callback)
    }

    @Override
    public void publishMessage(SubsribeService.Message request, StreamObserver<Empty> responseObserver) {
        publish(request);
        responseObserver.onCompleted();
    }

    public void publish(SubsribeService.Message message) {
        log.debug("Start publishing message: {}", message.getContent());
        for (List<StreamObserver<SubsribeService.Message>> topicSubscribers : subscribers.values()) {
            for (StreamObserver<SubsribeService.Message> subscriber : topicSubscribers) {
                try {
                    log.debug("Sending message to subscriber {}", subscriber);
                    subscriber.onNext(message);
                } catch (RuntimeException e) {
                    // Handle errors sending to specific subscriber (e.g., remove disconnected)
                }
            }
        }
    }
}

