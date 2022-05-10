package com.fiats.arrangement.constant;

public enum ArrangementAction {
    BROKER, //Broker gioi thieu lenh
    CUSTOMER_CONFIRM, //Khach hang xac nhan lenh
    PLACE_ORDER, //Khach hang tu dat lenh
    SIGN_CONTRACT, //Khach hang ky hop dong
    CONFIRM, //He thong confirm AFC
    PAYMENT, //He thong thuc hien thanh toan
    DELIVERY, //He thong thuc hien chuyen nhuong
    HOLD,
    UNHOLD,
    CANCEL,
    EXPIRED,
    UNCONFIRM,
    CANCEL_AFC, // Xoá local queue khi đã gửi AFC (KH huỷ lệnh)
}
