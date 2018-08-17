package com.pluralsight.orderfulfillment.fulfillmentcenterone.service;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.pluralsight.orderfulfillment.generated.OrderItemType;
import com.pluralsight.orderfulfillment.generated.OrderType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentCenterOneProcessor {
    private static final Logger LOG = LoggerFactory
    		.getLogger(FulfillmentCenterOneProcessor.class);
    
    public String transformToOrderRequestMesage(String orderXML) {
    	String output=null;
    	try {
    		if (orderXML==null) {
    			throw new Exception
    			("Order xml was not bound to the method via"
        				+ "integeration framework");
    		}
    		output = processCreateOrderMessageRequest(orderXML);
    		
    	} catch(Exception e) {
    		LOG.error("Fullfillmentcenterone message transalation failed  ", e);
    	}
    	 
    	return output;
    }
      
    protected String processCreateOrderMessageRequest (String orderXML)
        throws Exception {
            JAXBContext context = 
            		JAXBContext
            		   .newInstance(com.pluralsight.orderfulfillment.generated.Order.class);
            Unmarshaller unmarshaller=context.createUnmarshaller();
            com.pluralsight.orderfulfillment.generated.Order order=
            		(com.pluralsight.orderfulfillment.generated.Order)unmarshaller
            		              .unmarshal(new StringReader(orderXML));
            
           return new Gson().toJson(builderOrderRequestType(order));
    }
        
    protected OrderRequest builderOrderRequestType
               (com.pluralsight.orderfulfillment.generated.Order orderXmlType) {
    	
    	OrderType orderTypeFromXML= orderXmlType.getOrderType();
    	List <OrderItemType> OrderItemTypesFromXML=  orderTypeFromXML.getOrderItems();
    	List<com.pluralsight.orderfulfillment.fulfillmentcenterone.service.OrderItem> orderItems= 
    			new ArrayList<com.pluralsight.orderfulfillment.fulfillmentcenterone.service.OrderItem>();
    	for (OrderItemType orderItemTypeFromXML:OrderItemTypesFromXML) {
    		orderItems
    			.add(new com.pluralsight.orderfulfillment.fulfillmentcenterone.service.OrderItem(
    					orderItemTypeFromXML.getItemNumber(), orderItemTypeFromXML.getPrice(),
    					orderItemTypeFromXML.getQuantity())    					
    				 );    		    		
    	}
    	

    	com.pluralsight.orderfulfillment.fulfillmentcenterone.service.Order order=
    				new com.pluralsight.orderfulfillment.fulfillmentcenterone.service.Order();
    	order.setFirstName(orderTypeFromXML.getFirstName());
    	order.setLastName(orderTypeFromXML.getLastName());
    	order.setEmail(orderTypeFromXML.getEmail());
    	order.setOrderNumber(orderTypeFromXML.getOrderNumber());
    	order.setTimeOrderPlaced(orderTypeFromXML.getTimeOrderPlaced().toGregorianCalendar()
    							.getTime());
    	order.setOrderItems(orderItems);
     	List <com.pluralsight.orderfulfillment.fulfillmentcenterone.service.Order> orders=
    				new ArrayList<com.pluralsight.orderfulfillment.fulfillmentcenterone.service.Order>();
     	orders.add(order);
     	
     	OrderRequest orderRequest= new OrderRequest();
     	orderRequest.setOrders(orders);
     	
    	return orderRequest ;    	    	
    	
    	
    }
    
   
}
