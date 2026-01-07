package com.smartparking.email_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailAmqpConfig {
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_VERIFICATION_QUEUE = "email.verification.queue";
    public static final String EMAIL_VERIFICATION_ROUTING = "email.verification";
    public static final String EMAIL_CONTACT_QUEUE = "email.contact.queue";
    public static final String EMAIL_CONTACT_ROUTING = "email.contact";
    public static final String EMAIL_TOPUP_QUEUE = "email.topup.queue";
    public static final String EMAIL_TOPUP_ROUTING = "email.topup";
    public static final String EMAIL_RESERVATION_QUEUE = "email.reservation.queue";
    public static final String EMAIL_RESERVATION_ROUTING = "email.reservation";
    public static final String EMAIL_PARKING_PAYMENT_ROUTING = "email.parking.payment";
    public static final String EMAIL_PARKING_PAYMENT_QUEUE = "email.parking.payment.queue";
    public static final String EMAIL_DLX = "email.dlx";
    public static final String EMAIL_DLQ = "email.verification.dlq";
    public static final String EMAIL_CONTACT_DLQ = "email.contact.dlq";
    public static final String EMAIL_TOPUP_DLQ = "email.topup.dlq";
    public static final String EMAIL_RESERVATION_DLQ = "email.reservation.dlq";
    public static final String EMAIL_PARKING_PAYMENT_DLQ = "email.parking.payment.dlq";

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public TopicExchange emailDlx() {
        return new TopicExchange(EMAIL_DLX);
    }

    @Bean
    public Queue emailVerificationQueue() {
        return QueueBuilder.durable(EMAIL_VERIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailVerificationBinding(Queue emailVerificationQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailVerificationQueue)
                .to(emailExchange)
                .with(EMAIL_VERIFICATION_ROUTING);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, TopicExchange emailDlx) {
        return BindingBuilder.bind(emailDlq)
                .to(emailDlx)
                .with(EMAIL_DLQ);
    }

    @Bean
    public Queue emailContactQueue() {
        return QueueBuilder.durable(EMAIL_CONTACT_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_CONTACT_DLQ)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue emailContactDlq() {
        return QueueBuilder.durable(EMAIL_CONTACT_DLQ).build();
    }

    @Bean
    public Binding emailContactBinding(Queue emailContactQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailContactQueue)
                .to(emailExchange)
                .with(EMAIL_CONTACT_ROUTING);
    }

    @Bean
    public Binding emailContactDlqBinding(Queue emailContactDlq, TopicExchange emailDlx) {
        return BindingBuilder.bind(emailContactDlq)
                .to(emailDlx)
                .with(EMAIL_CONTACT_DLQ);
    }

    @Bean
    public Queue emailTopUpQueue() {
        return QueueBuilder.durable(EMAIL_TOPUP_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_TOPUP_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue emailTopUpDlq() {
        return QueueBuilder.durable(EMAIL_TOPUP_DLQ).build();
    }

    @Bean
    public Binding emailTopUpBinding(Queue emailTopUpQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailTopUpQueue)
                .to(emailExchange)
                .with(EMAIL_TOPUP_ROUTING);
    }

    @Bean
    public Binding emailTopUpDlqBinding(Queue emailTopUpDlq, TopicExchange emailDlx) {
        return BindingBuilder.bind(emailTopUpDlq)
                .to(emailDlx)
                .with(EMAIL_TOPUP_DLQ);
    }

    @Bean
    public Queue emailReservationQueue() {
        return QueueBuilder.durable(EMAIL_RESERVATION_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_RESERVATION_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue emailReservationDlq() {
        return QueueBuilder.durable(EMAIL_RESERVATION_DLQ).build();
    }

    @Bean
    public Binding emailReservationBinding(Queue emailReservationQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailReservationQueue)
                .to(emailExchange)
                .with(EMAIL_RESERVATION_ROUTING);
    }

    @Bean
    public Binding emailReservationDlqBinding(Queue emailReservationDlq, TopicExchange emailDlx) {
        return BindingBuilder.bind(emailReservationDlq)
                .to(emailDlx)
                .with(EMAIL_RESERVATION_DLQ);
    }

    @Bean
    public Queue emailParkingPaymentQueue() {
        return QueueBuilder.durable(EMAIL_PARKING_PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_PARKING_PAYMENT_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue emailParkingPaymentDlq() {
        return QueueBuilder.durable(EMAIL_PARKING_PAYMENT_DLQ).build();
    }

    @Bean
    public Binding emailParkingPaymentBinding(Queue emailParkingPaymentQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailParkingPaymentQueue)
                .to(emailExchange)
                .with(EMAIL_PARKING_PAYMENT_ROUTING);
    }

    @Bean
    public Binding emailParkingPaymentDlqBinding(Queue emailParkingPaymentDlq, TopicExchange emailDlx) {
        return BindingBuilder.bind(emailParkingPaymentDlq)
                .to(emailDlx)
                .with(EMAIL_PARKING_PAYMENT_DLQ);
    }
}

