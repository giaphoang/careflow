package com.example.hms.careflow.config;

import com.example.hms.careflow.dto.CareFlowDtos.*;
import com.example.hms.careflow.repository.InventoryRepository;
import com.example.hms.careflow.service.*;
import com.example.hms.security.dto.CreateUserRequest;
import com.example.hms.security.model.Role;
import com.example.hms.security.repository.AppUserRepository;
import com.example.hms.security.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Set;

@Component
@ConditionalOnProperty(name="careflow.demo-enabled", havingValue="true")
public class CareFlowDemoData implements CommandLineRunner {
    private final CareFlowService service;
    private final InventoryRepository inventory;
    private final AppUserRepository users;
    private final AuthService auth;
    public CareFlowDemoData(CareFlowService service, InventoryRepository inventory, AppUserRepository users, AuthService auth) {
        this.service = service; this.inventory = inventory; this.users = users; this.auth = auth;
    }
    @Override public void run(String... args) {
        seed("requester", Role.REQUESTER); seed("clinical", Role.CLINICAL_STAFF); seed("inventory", Role.INVENTORY_MANAGER);
        if (inventory.count() > 0) return;
        Actor manager = new Actor("inventory", Set.of("INVENTORY_MANAGER"));
        Actor requester = new Actor("requester", Set.of("REQUESTER"));
        Actor clinician = new Actor("clinical", Set.of("CLINICAL_STAFF"));
        String[][] lots = {
            {"Cardiac pacing lead kit", "Cardiovascular", "North · Store A", "84", "20"},
            {"Insulin pump infusion set", "Diabetes care", "East · Store B", "126", "30"},
            {"Surgical access kit", "Surgical", "North · Store A", "18", "20"},
            {"Neurostimulation connector", "Neuroscience", "West · Store C", "42", "10"},
            {"Patient monitoring sensor", "Monitoring", "East · Store B", "156", "40"},
            {"Vascular delivery catheter", "Cardiovascular", "North · Store A", "9", "12"},
            {"Sterile procedure pack", "Surgical", "West · Store C", "68", "15"},
            {"Pump reservoir cartridge", "Diabetes care", "East · Store B", "35", "10"}
        };
        for (int i = 0; i < lots.length; i++) {
            String[] row = lots[i];
            var item = service.createItem(manager, new CreateItem(row[0], "SYN-UDI-" + (10001+i), "SIM-26-" + (701+i),
                row[2], row[1], LocalDate.now().plusDays(90L + i * 25), Integer.parseInt(row[3]), Integer.parseInt(row[4])));
            var request = service.submit(requester, new CreateRequest(item.id(), 2 + i, i % 2 == 0 ? "Cardiac services" : "Surgical services",
                "Scheduled supply replenishment · synthetic simulation"));
            if (i < 5) service.review(clinician, request.id(), new Review(true, "Resource need confirmed for simulation"));
            if (i < 3) service.reserve(manager, request.id());
            if (i < 2) service.fulfill(manager, request.id(), "demo-fulfillment-" + request.id());
            if (i == 7) service.adjustItem(manager, item.id(), new AdjustItem(0, true, "Simulated quality hold pending lot review"));
        }
    }
    private void seed(String name, Role role) {
        if (!users.existsByUsername(name)) auth.createUser(new CreateUserRequest(name, "CareFlow@123", role));
    }
}
