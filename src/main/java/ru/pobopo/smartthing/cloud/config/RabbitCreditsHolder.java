package ru.pobopo.smartthing.cloud.config;

public class RabbitCreditsHolder {
    private final String login;
    private final String password;

    public RabbitCreditsHolder(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
