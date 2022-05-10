package com.fiats.arrangement.internal.event;

import com.fiats.arrangement.constant.NotificationCodeEnum;
import com.fiats.tmgcoreutils.payload.ArrangementNotificationDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationWrapperEvent extends ApplicationEvent {

    private ArrangementNotificationDTO notiModel;

    private NotificationCodeEnum template;

    public NotificationWrapperEvent(Object source) {
        super(source);
    }

    public NotificationWrapperEvent(Object source,
                                    ArrangementNotificationDTO notiModel,
                                    NotificationCodeEnum template) {
        super(source);
        this.notiModel = notiModel;
        this.template = template;
    }
}
