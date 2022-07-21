package Zeze.Services.RocketMQ;

public interface Constant {

	String NO_MESSAGE_SELECTOR = "*";

	boolean DEFAULT_NO_LOCAL = true;

	boolean DEFAULT_DURABLE = false;

	//------------------------JMS message header constant---------------------------------
	String JMS_DESTINATION = "jmsDestination";
	String JMS_DELIVERY_MODE = "jmsDeliveryMode";
	String JMS_EXPIRATION = "jmsExpiration";
	String JMS_DELIVERY_TIME = "jmsDeliveryTime";
	String JMS_PRIORITY = "jmsPriority";
	String JMS_MESSAGE_ID = "jmsMessageID";
	String JMS_TIMESTAMP = "jmsTimestamp";
	String JMS_CORRELATION_ID = "jmsCorrelationID";
	String JMS_REPLY_TO = "jmsReplyTo";
	String JMS_TYPE = "jmsType";
	String JMS_REDELIVERED = "jmsRedelivered";

	//-------------------------JMS defined properties constant----------------------------
	/**
	 * The identity of the user sending the Send message
	 */
	String JMS_XUSER_ID = "jmsXUserID";
	/**
	 * The identity of the application Send sending the message
	 */
	String JMS_XAPP_ID = "jmsXAppID";
	/**
	 * The number of message delivery Receive attempts
	 */
	String JMS_XDELIVERY_COUNT = "jmsXDeliveryCount";
	/**
	 * The identity of the message group this message is part of
	 */
	String JMS_XGROUP_ID = "jmsXGroupID";
	/**
	 * The sequence number of this message within the group; the first message is 1, the second 2,...
	 */
	String JMS_XGROUP_SEQ = "jmsXGroupSeq";
	/**
	 * The transaction identifier of the Send transaction within which this message was produced
	 */
	String JMS_XPRODUCER_TXID = "jmsXProducerTXID";
	/**
	 * The transaction identifier of the Receive transaction within which this message was consumed
	 */
	String JMS_XCONSUMER_TXID = "jmsXConsumerTXID";

	/**
	 * The time JMS delivered the Receive message to the consumer
	 */
	String JMS_XRCV_TIMESTAMP = "jmsXRcvTimestamp";
	/**
	 * Assume there exists a message warehouse that contains a separate copy of each message sent to each consumer and
	 * that these copies exist from the time the original message was sent. Each copy’s state is one of: 1(waiting),
	 * 2(ready), 3(expired) or 4(retained) Since state is of no interest to producers and consumers it is not provided
	 * to either. It is only of relevance to messages looked up in a warehouse and JMS provides no API for this.
	 */
	String JMS_XSTATE = "jmsXState";

	//---------------------------JMS Headers' value constant---------------------------
	/**
	 * Default time to live
	 */
	long DEFAULT_TIME_TO_LIVE = 3 * 24 * 60 * 60 * 1000;

	/**
	 * Default Jms Type
	 */
	String DEFAULT_JMS_TYPE = "rocketmq";
}
