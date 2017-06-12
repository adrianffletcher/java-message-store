package com.example.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Digest object represents the result of a hash operation on a message can be automatically generated into json.
 */
@XmlRootElement
public class Digest {

    private String digest;

    public Digest() {
    }

    public Digest(String digest) {
        this.digest = digest;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }
}
