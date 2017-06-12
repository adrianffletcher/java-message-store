package com.example.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Message object represents a String message can be automatically generated into json.
 */
@XmlRootElement
public class Message {

    private String message;

    public Message() {
    }

    public Message(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
