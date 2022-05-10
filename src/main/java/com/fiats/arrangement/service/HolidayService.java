package com.fiats.arrangement.service;

import java.util.Date;

public interface HolidayService {
    String getListWorkingDays(Date fromDate, Date toDate);
}
