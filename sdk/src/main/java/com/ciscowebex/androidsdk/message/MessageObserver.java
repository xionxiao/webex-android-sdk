package com.ciscowebex.androidsdk.message;

import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.WebexEvent;

/**
 * Callback to receive the events from a {@link MessageClient}.
 *
 * @since 1.4.0
 */
public interface MessageObserver {

    /**
     * Mark interface for all message events.
     */
    interface MessageEvent extends WebexEvent {
    }

    /**
     * The event when a new message has arrived.
     *
     * @deprecated
     * @since 1.4.0
     */
    @Deprecated
    class MessageArrived extends WebexEvent.Base implements MessageEvent {
        private Message message;

        protected MessageArrived(Message message, Activity activity) {
            super(activity);
            this.message = message;
        }

        /**
         * Returns the message arrived.
         *
         * @return The message arrived.
         */
        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

    }

    /**
     * The event when a new message has arrived.
     *
     * @since 2.1.0
     */
    class MessageReceived extends WebexEvent.Base implements MessageEvent {

        private Message message;

        protected MessageReceived(Message message, Activity activity) {
            super(activity);
            this.message = message;
        }

        /**
         * Returns the message arrived.
         *
         * @return The message arrived.
         */
        public Message getMessage() {
            return message;
        }

    }

    /**
     * The event when a message has been deleted.
     *
     * @since 1.4.0
     */
    class MessageDeleted extends WebexEvent.Base implements MessageEvent {

        private String messageId;

        protected MessageDeleted(String messageId, Activity activity) {
            super(activity);
            this.messageId = messageId;
        }

        /**
         * Returns the id of the deleted message
         *
         * @return The id of the deleted message
         */
        public String getMessageId() {
            return messageId;
        }

    }

    /**
     * Invoked when there is a new {@link MessageEvent}.
     *
     * @param event Message event
     */
    void onEvent(MessageEvent event);
}
