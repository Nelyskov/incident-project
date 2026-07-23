package com.example.incedent_processor;

import com.example.common.events.Incident;
import com.example.common.events.IncidentPriority;
import com.example.incedent_processor.services.Service;
import net.datafaker.Faker;

public class DataGenerationHelper {
    static Faker faker = new Faker();

    public static Incident createIncidentWithHighPriority() {
        Incident incident = new Incident();
        incident.setId(faker.number().randomNumber());
        incident.setPriority(IncidentPriority.HIGH);
        Service service = faker.options().option(
                Service.PAYMENT_SERVICE,
                Service.AUTH_SERVICE,
                Service.ORDER_SERVICE,
                Service.INVENTORY_SERVICE
        );
        incident.setService(service.name());
        incident.setInfo("Test incident info");
        incident.setTimestamp(System.currentTimeMillis());
        incident.setStatus(com.example.common.events.IncidentStatus.CREATED);
        return incident;
    }
}
