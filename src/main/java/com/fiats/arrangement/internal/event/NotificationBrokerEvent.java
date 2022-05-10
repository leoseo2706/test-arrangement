package com.fiats.arrangement.internal.event;

import com.fiats.arrangement.constant.NotificationCodeEnum;
import com.fiats.tmgcoreutils.payload.CustomerBrokerWrapper;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class NotificationBrokerEvent extends ApplicationEvent {

    private CustomerBrokerWrapper cusBroker;

    private OrderPlacementDTO orderDTO;

    private NotificationCodeEnum template;

    private List<String> attachmentPaths;

    public NotificationBrokerEvent(Object source) {
        super(source);
    }

    public NotificationBrokerEvent(Object source,
                                   CustomerBrokerWrapper cusBroker,
                                   OrderPlacementDTO orderDTO,
                                   NotificationCodeEnum template,
                                   List<String> attachmentPaths) {
        super(source);
        this.cusBroker = cusBroker;
        this.orderDTO = orderDTO;
        this.template = template;
        this.attachmentPaths = attachmentPaths;
    }

    public NotificationBrokerEvent(Object source,
                                   CustomerBrokerWrapper cusBroker,
                                   OrderPlacementDTO orderDTO,
                                   NotificationCodeEnum template) {
        super(source);
        this.cusBroker = cusBroker;
        this.orderDTO = orderDTO;
        this.template = template;
    }
}
