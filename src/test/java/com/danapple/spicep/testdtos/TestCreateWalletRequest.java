package com.danapple.spicep.testdtos;

public class TestCreateWalletRequest {
    private String emailAddress;

    public TestCreateWalletRequest(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public TestCreateWalletRequest() {
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
