package Lab1;

import java.util.ArrayList;
import java.util.List;

public class Customer {
    private String customerId;
    private String name;
    private String email;
    private String phone;
    private List<Account> accounts;

    public Customer() {
        this.accounts = new ArrayList<>();
    }

    public Customer(String customerId, String name, String email, String phone) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.accounts = new ArrayList<>();
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<Account> getAccounts() { return accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }

    @Override
    public String toString() {
        return "Customer{id='" + customerId + "', name='" + name + "', email='" + email
                + "', phone='" + phone + "', accounts=" + accounts.size() + "}";
    }
}