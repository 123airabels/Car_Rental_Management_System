package com.carrental.car_rental_app.support;

/** The "SMS / email service" box in the architecture diagram. Swap the implementation to send real messages. */
public interface NotificationService {
    void notifyCustomer(int customerId, String subject, String message);
}
