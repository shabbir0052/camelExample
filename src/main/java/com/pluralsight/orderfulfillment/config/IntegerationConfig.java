package com.pluralsight.orderfulfillment.config;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import javax.inject.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pluralsight.orderfulfillment.order.OrderStatus;
import com.pluralsight.orderfulfillment.fulfillmentcenterone.service.FulfillmentCenterOneProcessor;
import com.pluralsight.orderfulfillment.order.OrderItemMessageTranslator;

@org.springframework.context.annotation.Configuration
public class IntegerationConfig extends CamelConfiguration {
	 
	@Inject
	private org.springframework.core.env.Environment environment;

	@Inject
	private javax.sql.DataSource dataSource;
	
	@Inject 
	private OrderItemMessageTranslator orderItemMessageTranslator;
	
	
	@Inject 
	private FulfillmentCenterOneProcessor fulfillmentCenterOneProcessor; 
	
	@Bean
	public org.apache.camel.component.sql.SqlComponent sql(){
		org.apache.camel.component.sql.SqlComponent sqlComponent = new org.apache.camel.component.sql.SqlComponent();
		sqlComponent.setDataSource(dataSource);
		return sqlComponent;
	}
	
	
	@Bean
	    public javax.jms.ConnectionFactory jmsConnectionFactory() {
	        return new org.apache.activemq.ActiveMQConnectionFactory(
	        		environment.getProperty("activemq.broker.url"));
	    }
	

	   
	    @Bean(initMethod = "start", destroyMethod = "stop")
	    public org.apache.activemq.pool.PooledConnectionFactory pooledConnectionFactory() {
	    	org.apache.activemq.pool.PooledConnectionFactory factory = new PooledConnectionFactory();
	        factory.setConnectionFactory(jmsConnectionFactory());
	        factory.setMaxConnections(Integer.parseInt(environment
	        		.getProperty("pooledConnectionFactory.maxConnections")));
	        return factory;
	    }
	
	    
	    @Bean
	    public org.apache.camel.component.jms.JmsConfiguration jmsConfiguration() {
	    	org.apache.camel.component.jms.JmsConfiguration jmsConfiguration = 
	    			new org.apache.camel.component.jms.JmsConfiguration();
	        jmsConfiguration.setConnectionFactory(pooledConnectionFactory());
	        return jmsConfiguration;
	    }
		
	
	    @Bean
	    public org.apache.activemq.camel.component.ActiveMQComponent activeMq() {
	    	org.apache.activemq.camel.component.ActiveMQComponent activeMq = 
	    			new org.apache.activemq.camel.component.ActiveMQComponent();
	        activeMq.setConfiguration(jmsConfiguration());
	        return activeMq	;
	    }	    
	    
	@Bean
	public org.apache.camel.builder.RouteBuilder newWebSiteOrderRoute(){
		 return new org.apache.camel.builder.RouteBuilder() {
				@Override 
				 public void configure () throws  Exception{
					from( 
							"sql:"
							+"select id from orders.\"order\" where status ='"
							+OrderStatus.NEW.getCode()	
							+"'"
							+"?"
							+"consumer.onConsume=update orders.\"order\" set status='"
							+OrderStatus.PROCESSING.getCode()
							+"' where id= :#id").bean(orderItemMessageTranslator, "transformToOrderItemMessage")
							.to("activemq:queue:ORDER_ITEM_PROCESSING");
							
										
				}

		 };
		  		   		
	}
	
	@Bean
    public org.apache.camel.builder.RouteBuilder fulfillmentCenterContentBasedRouter() {
        return new org.apache.camel.builder.RouteBuilder() {
            @Override
            public void configure() throws Exception {
                org.apache.camel.builder.xml.Namespaces namespace = new org.apache.camel.builder.xml.Namespaces("o", "http://www.pluralsight.com/orderfullfillment/Order");

                from("activemq:queue:ORDER_ITEM_PROCESSING")
                        .choice()
                        .when()
                         .xpath("/o:Order/o:OrderType/o:FulfillmentCenter = '" + com.pluralsight.orderfulfillment.generated.FulfillmentCenter.ABC_FULFILLMENT_CENTER.value()
                                + "'", namespace)
                         .to("activemq:queue:ABC_FULFILLMENT_REQUEST")
                        .when()
                         .xpath("/o:Order/o:OrderType/o:FulfillmentCenter = '" + com.pluralsight.orderfulfillment.generated.FulfillmentCenter.FULFILLMENT_CENTER_ONE.value()
                                + "'", namespace)
                        	.to("activemq:queue:FC1_FULFILLMENT_REQUEST")
                        .otherwise()
                        	.to("activemq:queue:ERROR_FULFILLMENT_REQUEST");
            }
        };
    }
	
	
	 /**
	    * Route builder to implement production to a RESTful web service. This route
	    * will first consume a message from the FC1_FULFILLMENT_REQUEST ActiveMQ
	    * queue. The message body will be an order in XML format. The message will
	    * then be passed to the fulfillment center one processor where it will be
	    * transformed from the XML to JSON format. Next, the message header content
	    * type will be set as JSON format and a message will be posted to the
	    * fulfillment center one RESTful web service. If the response is success,
	    * the route will be complete. If not, the route will error out.
	    * 
	    * @return
	    */
	   @Bean
	   public org.apache.camel.builder.RouteBuilder fulfillmentCenterOneRouter() {
	      return new org.apache.camel.builder.RouteBuilder() {
	         @Override
	         public void configure() throws Exception {
	            from("activemq:queue:FC1_FULFILLMENT_REQUEST")
	                  .bean(fulfillmentCenterOneProcessor,
	                        "transformToOrderRequestMesage")
	                  .setHeader(org.apache.camel.Exchange.CONTENT_TYPE,
	                        constant("application/json"))
	                     .setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
	                  .to("http4://localhost:8090/services/orderFulfillment/processOrders");
	         }
	      };
	   }
	
	
	 private static final Logger log = LoggerFactory
		         .getLogger(IntegerationConfig.class);
	

}


