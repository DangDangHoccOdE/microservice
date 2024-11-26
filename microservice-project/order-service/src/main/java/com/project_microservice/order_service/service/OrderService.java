package com.project_microservice.order_service.service;

import com.project_microservice.order_service.dto.InventoryResponse;
import com.project_microservice.order_service.dto.OrderLineItemDto;
import com.project_microservice.order_service.dto.OrderRequest;
import com.project_microservice.order_service.event.OrderPlaceEvent;
import com.project_microservice.order_service.model.Order;
import com.project_microservice.order_service.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.project_microservice.order_service.model.OrderLineItem;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClient;
    private final Tracer tracer;
    private final KafkaTemplate<String,OrderPlaceEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOderNumber(UUID.randomUUID().toString());

        List<OrderLineItem> list = orderRequest.getOrderLineItemDtos().stream()
                .map(this::mapToOrderResponse).toList();

        order.setOrderLineItems(list);
        List<String> skuCodes = order.getOrderLineItems().stream().map(OrderLineItem::getSkuCode).toList();

        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

        try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())){
            // Call inventory service, and place order if product is in
            // stock
            InventoryResponse[] inventoryResponseArray = webClient.build().get()
                    .uri("http://inventory-service/api/inventory",uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean result = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);

            if(result){
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic",new OrderPlaceEvent(order.getOderNumber()));
                return "Order place successfully";
            }else{
                throw new IllegalArgumentException("Product is not in stock, please try again");
            }
        }finally {
            inventoryServiceLookup.end();
        }
    }

    private OrderLineItem mapToOrderResponse(OrderLineItemDto orderLineItemDto) {
        OrderLineItem orderLineItem = new OrderLineItem();
        orderLineItem.setPrice(orderLineItemDto.getPrice());
        orderLineItem.setQuantity(orderLineItemDto.getQuantity());
        orderLineItem.setSkuCode(orderLineItemDto.getSkuCode());
        return orderLineItem;
    }
}
