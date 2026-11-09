package com.codexsphere.codearena.tatasteel.customer.service;

import com.codexsphere.codearena.tatasteel.common.exception.TataSteelException;
import com.codexsphere.codearena.tatasteel.common.util.PhoneNumberUtils;
import com.codexsphere.codearena.tatasteel.customer.model.Customer;
import com.codexsphere.codearena.tatasteel.customer.model.CustomerStatus;
import com.codexsphere.codearena.tatasteel.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Optional<Customer> findByPhone(String rawPhoneNumber) {
        String phone = PhoneNumberUtils.normalize(rawPhoneNumber);
        if (!StringUtils.hasText(phone)) {
            return Optional.empty();
        }
        return customerRepository.findByPhoneNumber(phone);
    }

    /**
     * Registers a customer if the phone number is not already known.
     * Duplicate submissions return the existing customer instead of creating
     * a second record.
     */
    @Transactional
    public RegistrationResult register(String rawPhoneNumber, String name, String city) {
        String phone = PhoneNumberUtils.normalize(rawPhoneNumber);
        if (!StringUtils.hasText(phone)) {
            throw new TataSteelException("A valid WhatsApp phone number is required.");
        }
        if (!StringUtils.hasText(name)) {
            throw new TataSteelException("Customer name is required.");
        }

        Optional<Customer> existing = customerRepository.findByPhoneNumber(phone);
        if (existing.isPresent()) {
            log.debug(
                    "Customer id={} already registered for {}; returning existing record.",
                    existing.get().getId(),
                    PhoneNumberUtils.mask(phone)
            );
            return new RegistrationResult(existing.get(), false);
        }

        Customer customer = Customer.builder()
                .phoneNumber(phone)
                .name(name.trim())
                .city(city == null ? null : city.trim())
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Registered Tata Steel customer {}", saved.getId());
        return new RegistrationResult(saved, true);
    }

    public record RegistrationResult(Customer customer, boolean created) {
    }

}
