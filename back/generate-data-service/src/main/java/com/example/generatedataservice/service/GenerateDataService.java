package com.example.generatedataservice.service;

import com.example.generatedataservice.entity.*;
import com.example.generatedataservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class GenerateDataService {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final MaterialRepository materialRepository;
    private final WorkshopRepository workshopRepository;
    private final OperationRepository operationRepository;
    private final OrderRepository orderRepository;
    private final DataAccessRepository dataAccessRepository;
    private final JdbcTemplate jdbcTemplate;

    private final Random random = new Random();

    private static final String[] FIRST_NAMES = {
        "Иван", "Петр", "Сергей", "Алексей", "Дмитрий", "Михаил", "Андрей", "Николай", "Владимир", "Евгений"
    };
    private static final String[] LAST_NAMES = {
        "Иванов", "Петров", "Сергеев", "Алексеев", "Дмитриев", "Михайлов", "Андреев", "Николаев", "Владимиров", "Евгеньев"
    };
    private static final String[] EMPLOYEE_FIRST_NAMES = {
        "Наталья", "Станислав", "Ольга", "Игорь", "Елена", "Сергей", "Анна", "Михаил", "Татьяна", "Александр"
    };
    private static final String[] EMPLOYEE_LAST_NAMES = {
        "Мохирёва", "Шипилов", "Кузнецова", "Попов", "Сидорова", "Иванов", "Петрова", "Смирнов", "Фёдорова", "Николаев"
    };
    private static final String[] COMPANIES = {
        "ООО Вектор", "АО ТехноПром", "ЗАО СтройКомплект", "ООО МетаЛайн", "ПАФ ЭнергоМастер"
    };
    private static final String[] POSITIONS = {
        "Менеджер", "Печатник", "Бухгалтер", "Производственник", "Дизайнер", "Ламинатор"
    };
    private static final String[] MATERIALS = {
        "Баннер лит. 450 гр/м2", "Баннер лит. 300 гр/м2", "Пленка самоклеящаяся", "Жидкий ламинат",
        "Плоттерная резка", "УФ-печать", "Сушка", "Постой баннера", "Подворот", "Установка люверса",
        "Монтаж на конструкцию", "Установка подсветки", "Пошив штор", "Изготовление стенда",
        "Лазерная резка", "Фрезеровка", "Тиснение фольгой", "Вырубка", "Склейка"
    };
    private static final String[] MATERIAL_UNITS = { "м2", "м.п." };
    private static final MaterialType[] MATERIAL_TYPES = { MaterialType.MATERIAL, MaterialType.OPERATION };
    private static final String[] OPERATION_NAMES = {
        "Печать 720 dpi", "Печать 1440 dpi", "Резка плоттерная", "Подворот",
        "Установка люверсов", "Сварка", "Ламинация", "УФ-лакировка", "Тиснение", "Фрезеровка"
    };
    private static final ApplicableType[] APPLICABLE_TYPES = { ApplicableType.BANNER, ApplicableType.PLENKA, ApplicableType.BOTH };
    private static final UnitType[] UNIT_TYPES = { UnitType.SQUARE_METER, UnitType.LINEAR_METER, UnitType.PIECE };
    private static final String[] WORKSHOP_NAMES = {
        "Цех печати", "Цех резки", "Цех сборки", "Цех ламинации", "Цех упаковки"
    };

    @Transactional
    public List<Client> generateClients(int count) {
        List<Client> clients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Client client = new Client();
            boolean isCompany = random.nextBoolean();
            client.setType(isCompany ? ClientType.COMPANY : ClientType.PRIVATE);
            if (isCompany) {
                client.setName(COMPANIES[random.nextInt(COMPANIES.length)] + " " + (i + 1));
                client.setContactPerson(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
            } else {
                client.setName(LAST_NAMES[random.nextInt(LAST_NAMES.length)] + " " + FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
                client.setContactPerson(client.getName());
            }
            client.setPhone("+7(9" + (100 + random.nextInt(900)) + ")" + String.format("%03d-%02d-%02d", random.nextInt(1000), random.nextInt(100), random.nextInt(100)));
            client.setEmail("gen_client_" + System.currentTimeMillis() + "_" + i + "@example.com");
            client.setInn("" + (7700000000L + random.nextInt(999999999)));
            client.setAddress("г. Москва, ул. Тестовая, д. " + (i + 1));
            clients.add(clientRepository.save(client));
        }
        return clients;
    }

    @Transactional
    public List<Employee> generateEmployees(int count) {
        List<Employee> employees = new ArrayList<>();
        List<Long> workshopIds = dataAccessRepository.fetchWorkshopIds();
        for (int i = 0; i < count; i++) {
            Employee employee = new Employee();
            employee.setFullName(EMPLOYEE_LAST_NAMES[random.nextInt(EMPLOYEE_LAST_NAMES.length)] + " " + EMPLOYEE_FIRST_NAMES[random.nextInt(EMPLOYEE_FIRST_NAMES.length)]);
            employee.setUsername("gen_user_" + System.currentTimeMillis() + "_" + i);
            employee.setPosition(POSITIONS[random.nextInt(POSITIONS.length)]);
            employee.setPhone("+7(9" + (100 + random.nextInt(900)) + ")" + String.format("%03d-%02d-%02d", random.nextInt(1000), random.nextInt(100), random.nextInt(100)));
            employee.setEmail("gen_emp_" + System.currentTimeMillis() + "_" + i + "@example.com");
            if (!workshopIds.isEmpty()) {
                employee.setWorkshopId(workshopIds.get(random.nextInt(workshopIds.size())));
            }
            employees.add(employeeRepository.save(employee));
        }
        return employees;
    }

    @Transactional
    public List<Material> generateMaterials(int count) {
        List<Material> materials = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String unit = MATERIAL_UNITS[random.nextInt(MATERIAL_UNITS.length)];
            Material material = new Material();
            material.setName(MATERIALS[random.nextInt(MATERIALS.length)] + " " + (i + 1) + " (" + unit + ")");
            material.setUnit(unit);
            material.setPrice(BigDecimal.valueOf(100 + random.nextInt(9900)));
            material.setWasteCoefficient(BigDecimal.ONE.add(BigDecimal.valueOf(random.nextInt(50) / 100.0)));
            material.setType(MATERIAL_TYPES[random.nextInt(MATERIAL_TYPES.length)]);
            materials.add(materialRepository.save(material));
        }
        return materials;
    }

    @Transactional
    public List<Operation> generateOperations(int count) {
        List<Operation> operations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Operation operation = new Operation();
            operation.setName(OPERATION_NAMES[random.nextInt(OPERATION_NAMES.length)] + " " + (i + 1));
            operation.setPrice(BigDecimal.valueOf(50 + random.nextInt(5000)));
            operation.setUnit(UNIT_TYPES[random.nextInt(UNIT_TYPES.length)]);
            operation.setApplicableTo(APPLICABLE_TYPES[random.nextInt(APPLICABLE_TYPES.length)]);
            operation.setIsDefault(random.nextBoolean());
            if (random.nextBoolean()) {
                operation.setHemWidthMm(10 + random.nextInt(90));
                operation.setHemCount(1 + random.nextInt(4));
            }
            operations.add(operationRepository.save(operation));
        }
        return operations;
    }

    @Transactional
    public List<Workshop> generateWorkshops(int count) {
        List<Workshop> workshops = new ArrayList<>();
        List<Long> operationIds = dataAccessRepository.fetchOperationIds();
        for (int i = 0; i < count; i++) {
            Workshop workshop = new Workshop();
            workshop.setName(WORKSHOP_NAMES[random.nextInt(WORKSHOP_NAMES.length)] + " " + (i + 1));
            workshop.setSortOrder(i);
            List<Long> assignedOps = new ArrayList<>();
            if (!operationIds.isEmpty()) {
                int opsCount = 1 + random.nextInt(Math.min(5, operationIds.size()));
                for (int j = 0; j < opsCount; j++) {
                    Long opId = operationIds.get(random.nextInt(operationIds.size()));
                    if (!assignedOps.contains(opId)) {
                        assignedOps.add(opId);
                    }
                }
            }
            workshop.setOperationIds(assignedOps);
            workshops.add(workshopRepository.save(workshop));
        }
        return workshops;
    }

    @Transactional
    public List<Order> generateOrders(int count) {
        List<Long> clientIds = dataAccessRepository.fetchClientIds();
        List<Long> employeeIds = dataAccessRepository.fetchEmployeeIds();
        List<DataAccessRepository.MaterialInfo> materials = dataAccessRepository.fetchMaterials();

        if (clientIds.isEmpty() || employeeIds.isEmpty()) {
            throw new RuntimeException("Необходимо сначала сгенерировать клиентов и сотрудников");
        }

        List<Order> orders = new ArrayList<>();
        long existingCount = dataAccessRepository.countOrders();

        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setOrderNumber("GEN-" + String.format("%05d", existingCount + i + 1));
            order.setClientId(clientIds.get(random.nextInt(clientIds.size())));
            order.setManagerId(employeeIds.get(random.nextInt(employeeIds.size())));
            order.setOrderDate(LocalDate.now().minusDays(random.nextInt(30)));
            order.setDueDate(LocalDate.now().plusDays(random.nextInt(60) + 7));
            order.setStatus(OrderStatus.values()[random.nextInt(OrderStatus.values().length)]);
            order.setProductionStage(ProductionStage.values()[random.nextInt(ProductionStage.values().length)]);
            order.setDescription("Тестовый заказ #" + (existingCount + i + 1));
            order.setTotalAmount(BigDecimal.valueOf(1000 + random.nextInt(50000)));
            order.setPaidAmount(BigDecimal.valueOf(random.nextInt(1000)));
            order.setCostPrice(BigDecimal.valueOf(500 + random.nextInt(20000)));
            order.setHasDocuments(random.nextBoolean());
            orders.add(orderRepository.save(order));

            if (!materials.isEmpty()) {
                generateOrderMaterials(order.getId(), materials);
            }
        }
        return orders;
    }

    @Transactional
    public int deleteAllClients() {
        return dataAccessRepository.deleteAllClients();
    }

    @Transactional
    public int deleteAllEmployees() {
        return dataAccessRepository.deleteAllEmployees();
    }

    @Transactional
    public int deleteAllMaterials() {
        return dataAccessRepository.deleteAllMaterials();
    }

    @Transactional
    public int deleteAllWorkshops() {
        return dataAccessRepository.deleteAllWorkshops();
    }

    @Transactional
    public int deleteAllOperations() {
        return dataAccessRepository.deleteAllOperations();
    }

    @Transactional
    public int deleteAllOrders() {
        return dataAccessRepository.deleteAllOrders();
    }

    private void generateOrderMaterials(Long orderId, List<DataAccessRepository.MaterialInfo> materials) {
        int itemCount = 1 + random.nextInt(3);
        for (int j = 0; j < itemCount; j++) {
            DataAccessRepository.MaterialInfo mat = materials.get(random.nextInt(materials.size()));
            BigDecimal quantity;
            if ("м2".equals(mat.getUnit())) {
                int dim1 = 500 + random.nextInt(2500);
                int dim2 = 500 + random.nextInt(2500);
                quantity = BigDecimal.valueOf(dim1 / 1000.0).multiply(BigDecimal.valueOf(dim2 / 1000.0))
                    .setScale(3, java.math.RoundingMode.HALF_UP);
            } else if ("м.п.".equals(mat.getUnit())) {
                int dim = 500 + random.nextInt(5000);
                quantity = BigDecimal.valueOf(dim / 1000.0).setScale(3, java.math.RoundingMode.HALF_UP);
            } else {
                quantity = BigDecimal.valueOf(1 + random.nextInt(20));
            }

            jdbcTemplate.update(
                "INSERT INTO svschema.order_materials (order_id, material_id, quantity, width_m, height_m, ready_date, deleted) VALUES (?, ?, ?, ?, ?, ?, false)",
                orderId, mat.getId(), quantity,
                BigDecimal.valueOf(1 + random.nextInt(5)),
                BigDecimal.valueOf(1 + random.nextInt(5)),
                LocalDate.now().plusDays(random.nextInt(30) + 7)
            );
        }
    }
}
