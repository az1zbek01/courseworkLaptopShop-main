package com.example.LaptopKG.service.implementations;

import com.example.LaptopKG.dto.order.RequestOrderDTO;
import com.example.LaptopKG.dto.order.ResponseOrderDTO;
import com.example.LaptopKG.exception.LaptopNotFoundException;
import com.example.LaptopKG.exception.NotFoundException;
import com.example.LaptopKG.model.*;
import com.example.LaptopKG.model.enums.DeliveryType;
import com.example.LaptopKG.model.enums.OrderStatus;
import com.example.LaptopKG.model.enums.PaymentType;
import com.example.LaptopKG.model.enums.Status;
import com.example.LaptopKG.repository.LaptopRepository;
import com.example.LaptopKG.repository.NotificationRepository;
import com.example.LaptopKG.repository.OrderRepository;
import com.example.LaptopKG.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.LaptopKG.dto.order.ResponseOrderDTO.toResponseOrderDTO;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final LaptopRepository laptopRepository;
    private final NotificationRepository notificationRepository;

    public List<ResponseOrderDTO> getAllOrdersOfUser(User user){
        return toResponseOrderDTO(orderRepository.findAllByUser(user)
                .stream().filter(order -> order.getStatus() == Status.ACTIVE)
                .collect(Collectors.toList())
        );
    }

    public List<ResponseOrderDTO> getAllOrders(){
        return toResponseOrderDTO(orderRepository.findAll()
                .stream().filter(order -> order.getStatus() == Status.ACTIVE)
                .collect(Collectors.toList())
        );
    }

    public List<ResponseOrderDTO> getAllDeletedOrders(){
        return toResponseOrderDTO(orderRepository.findAll()
                .stream().filter(order -> order.getStatus() == Status.DELETED)
                .collect(Collectors.toList())
        );
    }

    public ResponseOrderDTO addOrder(RequestOrderDTO orderDTO, User user){
        // Get set of laptops by ids
        Set<Laptop> laptopsSet = new HashSet<>();
        for(long id:orderDTO.getLaptops()) {
            laptopsSet.add(laptopRepository.findById(id)
                    .orElseThrow(() -> new LaptopNotFoundException("Ноутбук с айди " + id + " не найден"))
            );
        }

        Order order = Order.builder()
                .laptops(new ArrayList<>(laptopsSet))
                .deliveryType(DeliveryType.of(orderDTO.getDeliveryType()))
                .paymentType(PaymentType.of(orderDTO.getPaymentType()))
                .orderStatus(OrderStatus.NEW)
                .status(Status.ACTIVE)
                .user(user)
                .build();

        orderRepository.save(order);

        notificationRepository.save(
                Notification.builder()
                        .header("Оформлен новый заказ!")
                        .message("Вы оформили новый заказ на ноутбуки, подробнее можете узнать в истории заказов.")
                        .status(Status.ACTIVE)
                        .user(user)
                        .build()
        );

        return toResponseOrderDTO(order);
    }

    public ResponseEntity<String> changeOrderStatus(Long id, OrderStatus orderStatus, String message){
        Order order = orderRepository.findById(id)
                .filter(o -> o.getStatus() == Status.ACTIVE)
                .orElseThrow(
                        () -> new NotFoundException("Заказ с айди " + id + " не найден")
        );

        order.setOrderStatus(orderStatus);
        orderRepository.save(order);

        if(message != null){
            notificationRepository.save(
              Notification.builder()
                      .header("Обновлён статус заказа!")
                      .message(message)
                      .user(order.getUser())
                      .status(Status.ACTIVE)
                      .build()
            );
        }

        return ResponseEntity.ok("Заказ обновлён");
    }


    public ResponseEntity<String> cancelOrder(Long id, User user){
        Order order = orderRepository.findById(id)
                .filter(o -> o.getStatus() == Status.ACTIVE)
                .filter(o -> o.getUser().equals(user))
                .orElseThrow(
                        () -> new NotFoundException("Заказ с айди " + id + " не найден")
                );

        order.setOrderStatus(OrderStatus.CANCELED);
        orderRepository.save(order);

        return ResponseEntity.ok("Заказ отменён");
    }

    public ResponseEntity<String> deleteOrder(Long id, User user){
        Order order = orderRepository.findById(id)
                .filter(o -> o.getStatus() == Status.ACTIVE)
                .filter(o -> o.getUser().equals(user))
                .orElseThrow(
                        () -> new NotFoundException("Заказ с айди " + id + " не найден")
                );

        order.setStatus(Status.DELETED);
        orderRepository.save(order);

        return ResponseEntity.ok("Заказ удалён");
    }

}
