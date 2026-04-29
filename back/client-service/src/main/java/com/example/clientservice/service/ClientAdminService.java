package com.example.clientservice.service;

import com.example.common.entity.Client;
import com.example.common.entity.ClientType;
import com.example.clientservice.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientAdminService {

    private final ClientRepository clientRepository;
    private Random random = new Random();

    private static final String[] FIRST_NAMES = {
        "Иван", "Петр", "Сергей", "Алексей", "Дмитрий", "Михаил", "Андрей", "Николай", "Владимир", "Евгений"
    };
    private static final String[] LAST_NAMES = {
        "Иванов", "Петров", "Сергеев", "Алексеев", "Дмитриев", "Михайлов", "Андреев", "Николаев", "Владимиров", "Евгеньев"
    };
    private static final String[] COMPANIES = {
        "ООО Вектор", "АО ТехноПром", "ЗАО СтройКомплект", "ООО МетаЛайн", "ПАФ ЭнергоМастер"
    };
    private static final String[] PHONES = {
        "+7(900)123-45-67", "+7(903)987-65-43", "+7(905)456-78-90", "+7(903)111-22-33", "+7(999)888-77-66"
    };

    public List<Client> generateTestClients(int count) {
        List<Client> clients = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Client client = new Client();
            boolean isCompany = random.nextBoolean();
            client.setType(isCompany ? ClientType.COMPANY : ClientType.PRIVATE);

            if (isCompany) {
                client.setName(COMPANIES[random.nextInt(COMPANIES.length)] + " " + (i + 1));
                client.setContactPerson(
                    FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " +
                    LAST_NAMES[random.nextInt(LAST_NAMES.length)]
                );
            } else {
                client.setName(
                    LAST_NAMES[random.nextInt(LAST_NAMES.length)] + " " +
                    FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]
                );
                client.setContactPerson(client.getName());
            }

            client.setPhone(PHONES[random.nextInt(PHONES.length)]);
            client.setEmail("test" + (i + 1) + "@example.com");
            client.setInn("" + (7700000000L + i));
            client.setAddress("г. Москва, ул. Примерная, д. " + (i + 1));

            clients.add(clientRepository.save(client));
        }

        return clients;
    }
}
