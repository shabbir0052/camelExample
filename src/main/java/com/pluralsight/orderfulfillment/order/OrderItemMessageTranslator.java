package com.pluralsight.orderfulfillment.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

/**
 * Created by shabbir on 5/5/2016.
 */
@Service
public class OrderItemMessageTranslator {
	
    private static final Logger LOG = LoggerFactory
    		.getLogger(OrderItemMessageTranslator.class);

    @Inject
    private OrderService orderService;

    public String transformToOrderItemMessage(Map<String, Object> orderIds) {
        String output = null;
        try {
            if (orderIds == null) {
                throw new Exception("Order id Was not bound to the method via integration Framework");
            }
            if (!orderIds.containsKey("id")) {
                throw new Exception("Could not find a valid key of 'id' for the order ID.");
            }
            if (orderIds.get("id") == null || !(orderIds.get("id") instanceof Long)) {
                throw new Exception("The order ID was not correctly provided or formatted.");
            }

            output = orderService.processCreateOrderMessage((Long) orderIds.get("id"));
        } catch (Exception e) {
            LOG.error("order processing failded" + e.getMessage(), e);
        }
        return output;
    }


}
