-- ============================================
-- SEED DATA: Workshops, Employees, Clients, Materials, Orders
-- Compatible with Flyway V1-V24 schema
-- ============================================

-- 1. INSERT WORKSHOPS (5 workshops)
INSERT INTO svtables.workshops (name, sort_order) VALUES
('Печать', 1),
('Ламинация', 2),
('Порезка', 3),
('Сборка', 4),
('Упаковка', 5);

-- 2. INSERT EMPLOYEES (20 employees)
INSERT INTO svtables.employees (full_name, username, position, phone, email, workshop_id) VALUES
('Иванов Иван Иванович', 'ivanov_i', 'Мастер цеха', '+79001234561', 'ivanov@sv.ru', 1),
('Петров Пётр Петрович', 'petrov_p', 'Мастер цеха', '+79001234562', 'petrov@sv.ru', 2),
('Сидоров Сидор Сидорович', 'sidorov_s', 'Мастер цеха', '+79001234563', 'sidorov@sv.ru', 3),
('Козлов Козьма Козлович', 'kozlov_k', 'Мастер цеха', '+79001234564', 'kozlov@sv.ru', 4),
('Новиков Новик Новикович', 'novikov_n', 'Мастер цеха', '+79001234565', 'novikov@sv.ru', 5),
('Морозов Мороз Морозович', 'morozov_m', 'Рабочий', '+79001234566', 'morozov@sv.ru', 1),
('Волков Волк Волкович', 'volkov_v', 'Рабочий', '+79001234567', 'volkov@sv.ru', 1),
('Соколов Сокол Соколович', 'sokolov_s', 'Рабочий', '+79001234568', 'sokolov@sv.ru', 2),
('Лебедев Лебедь Лебедевич', 'lebedev_l', 'Рабочий', '+79001234569', 'lebedev@sv.ru', 2),
('Кузнецов Кузнец Кузнецович', 'kuznetsov_k', 'Рабочий', '+79001234570', 'kuznetsov@sv.ru', 3),
('Попов Поп Попович', 'popov_p', 'Рабочий', '+79001234571', 'popov@sv.ru', 3),
('Васильев Василий Васильевич', 'vasiliev_v', 'Рабочий', '+79001234572', 'vasiliev@sv.ru', 4),
('Смирнов Смирнова Смирновна', 'smirnov_s', 'Рабочий', '+79001234573', 'smirnov@sv.ru', 4),
('Фёдоров Фёдор Фёдорович', 'fedorov_f', 'Рабочий', '+79001234574', 'fedorov@sv.ru', 5),
('Орлов Орел Орлович', 'orlov_o', 'Рабочий', '+79001234575', 'orlov@sv.ru', 5),
('Макаров Макар Макарович', 'makarov_m', 'Менеджер', '+79001234576', 'makarov@sv.ru', NULL),
('Зайцев Заяц Зайцевич', 'zaitsev_z', 'Менеджер', '+79001234577', 'zaitsev@sv.ru', NULL),
('Павлов Павел Павлович', 'pavlov_p', 'Бухгалтер', '+79001234578', 'pavlov@sv.ru', NULL),
('Семёнов Семён Семёнович', 'semenov_s', 'Администратор', '+79001234579', 'semenov@sv.ru', NULL),
('Голубев Голубь Голубевич', 'golubev_g', 'Директор', '+79001234580', 'golubev@sv.ru', NULL);

-- 3. INSERT CLIENTS (50 clients)
INSERT INTO svtables.clients (name, type, contact_person, phone, email, inn, address, priceplus) VALUES
('ООО Рога и Копыта', 'ООО', 'Директор Рог', '+79101111111', 'roga@mail.ru', '7701234567', 'Москва, ул. Ленина 1', 10.00),
('ИП Копытов', 'ИП', 'Копытов И.И.', '+79102222222', 'kop@mail.ru', '7701234568', 'Москва, ул. Пушкина 2', 5.00),
('ООО Вектор', 'ООО', 'Менеджер Вектор', '+79103333333', 'vektor@mail.ru', '7701234569', 'СПб, Невский 3', 15.00),
('АО Прогресс', 'АО', 'Директор Прогресс', '+79104444444', 'progress@mail.ru', '7701234570', 'Екатеринбург, Мира 4', 12.00),
('ООО Спектр', 'ООО', 'Бухгалтер Спектр', '+79105555555', 'spektr@mail.ru', '7701234571', 'Новосибирск, Ленина 5', 8.00),
('ИП Сидоров', 'ИП', 'Сидоров С.С.', '+79106666666', 'sid@mail.ru', '7701234572', 'Казань, Баумана 6', 7.00),
('ООО Альфа', 'ООО', 'Менеджер Альфа', '+79107777777', 'alfa@mail.ru', '7701234573', 'Нижний Новгород, Горького 7', 11.00),
('ООО Бета', 'ООО', 'Директор Бета', '+79108888888', 'beta@mail.ru', '7701234574', 'Самара, Советская 8', 9.00),
('АО Гамма', 'АО', 'Менеджер Гамма', '+79109999999', 'gamma@mail.ru', '7701234575', 'Ростов, Пушкинская 9', 13.00),
('ООО Дельта', 'ООО', 'Бухгалтер Дельта', '+79101010101', 'delta@mail.ru', '7701234576', 'Воронеж, Кирова 10', 6.00),
('ИП Ершов', 'ИП', 'Ершов Е.Е.', '+79101112131', 'ershov@mail.ru', '7701234577', 'Краснодар, Красная 11', 10.00),
('ООО Жуков', 'ООО', 'Директор Жуков', '+79101415161', 'zhukov@mail.ru', '7701234578', 'Уфа, Ленина 12', 14.00),
('ООО Звезда', 'ООО', 'Менеджер Звезда', '+79101718191', 'zvezda@mail.ru', '7701234579', 'Пермь, Комсомольский 13', 8.00),
('АО Империал', 'АО', 'Директор Империал', '+79102021222', 'imperial@mail.ru', '7701234580', 'Тюмень, Республики 14', 16.00),
('ООО Корона', 'ООО', 'Бухгалтер Корона', '+79102324252', 'korona@mail.ru', '7701234581', 'Ижевск, Пушкина 15', 7.00),
('ИП Лисицын', 'ИП', 'Лисицын Л.Л.', '+79102627282', 'lis@mail.ru', '7701234582', 'Тула, Ленина 16', 5.00),
('ООО Метеор', 'ООО', 'Менеджер Метеор', '+79102930313', 'meteor@mail.ru', '7701234583', 'Ярославль, Свободы 17', 12.00),
('ООО Нептун', 'ООО', 'Директор Нептун', '+79103233343', 'neptun@mail.ru', '7701234584', 'Владимир, Горького 18', 9.00),
('АО Олимп', 'АО', 'Менеджер Олимп', '+79103536373', 'olimp@mail.ru', '7701234585', 'Калуга, Кирова 19', 11.00),
('ООО Планета', 'ООО', 'Бухгалтер Планета', '+79103839404', 'planeta@mail.ru', '7701234586', 'Тверь, Мира 20', 10.00),
('ИП Романов', 'ИП', 'Романов Р.Р.', '+79104142434', 'roman@mail.ru', '7701234587', 'Брянск, Ленина 21', 6.00),
('ООО Сатурн', 'ООО', 'Директор Сатурн', '+79104445464', 'saturn@mail.ru', '7701234588', 'Курск, Пушкина 22', 13.00),
('ООО Титан', 'ООО', 'Менеджер Титан', '+79104748494', 'titan@mail.ru', '7701234589', 'Белгород, Советская 23', 8.00),
('АО Уран', 'АО', 'Директор Уран', '+79105051525', 'uran@mail.ru', '7701234590', 'Смоленск, Горького 24', 15.00),
('ООО Феникс', 'ООО', 'Бухгалтер Феникс', '+79105354555', 'fenix@mail.ru', '7701234591', 'Псков, Ленина 25', 7.00),
('ИП Харитонов', 'ИП', 'Харитонов Х.Х.', '+79105657586', 'hariton@mail.ru', '7701234592', 'Великий Новгород, Мира 26', 10.00),
('ООО Центавр', 'ООО', 'Менеджер Центавр', '+79105960616', 'centaur@mail.ru', '7701234593', 'Мурманск, Ленина 27', 12.00),
('ООО Челлендж', 'ООО', 'Директор Челлендж', '+79106263647', 'challenge@mail.ru', '7701234594', 'Архангельск, Пушкина 28', 9.00),
('АО Шторм', 'АО', 'Менеджер Шторм', '+79106566678', 'storm@mail.ru', '7701234595', 'Сыктывкар, Комсомольский 29', 14.00),
('ООО Эклипс', 'ООО', 'Бухгалтер Эклипс', '+79106869709', 'eclipse@mail.ru', '7701234596', 'Петрозаводск, Горького 30', 11.00),
('ИП Белов', 'ИП', 'Белов Б.Б.', '+79107172737', 'belov@mail.ru', '7701234597', 'Вологда, Ленина 31', 8.00),
('ООО Гром', 'ООО', 'Директор Гром', '+79107475767', 'grom@mail.ru', '7701234598', 'Кострома, Пушкина 32', 11.00),
('ООО Дракон', 'ООО', 'Менеджер Дракон', '+79107778797', 'dragon@mail.ru', '7701234599', 'Пенза, Мира 33', 9.00),
('АО Эльф', 'АО', 'Директор Эльф', '+79108081828', 'elf@mail.ru', '7701234600', 'Саратов, Горького 34', 13.00),
('ООО Феникс Плюс', 'ООО', 'Бухгалтер Феникс', '+79108384858', 'fenixplus@mail.ru', '7701234601', 'Тамбов, Ленина 35', 7.00),
('ИП Григорьев', 'ИП', 'Григорьев Г.Г.', '+79108687888', 'grigor@mail.ru', '7701234602', 'Орёл, Пушкина 36', 10.00),
('ООО Горизонт', 'ООО', 'Менеджер Горизонт', '+79108989909', 'horizon@mail.ru', '7701234603', 'Рязань, Мира 37', 12.00),
('ООО Империя', 'ООО', 'Директор Империя', '+79109293939', 'empire@mail.ru', '7701234604', 'Липецк, Горького 38', 8.00),
('АО Корона Голд', 'АО', 'Менеджер Корона', '+79109596969', 'corona@mail.ru', '7701234605', 'Тула, Ленина 39', 15.00),
('ООО Легион', 'ООО', 'Бухгалтер Легион', '+79109899000', 'legion@mail.ru', '7701234606', 'Курск, Пушкина 40', 6.00),
('ИП Максимов', 'ИП', 'Максимов М.М.', '+79110102030', 'maximov@mail.ru', '7701234607', 'Брянск, Мира 41', 9.00),
('ООО Новация', 'ООО', 'Менеджер Новация', '+79110405060', 'novation@mail.ru', '7701234608', 'Смоленск, Горького 42', 11.00),
('ООО Орион', 'ООО', 'Директор Орион', '+79110708090', 'orion@mail.ru', '7701234609', 'Калуга, Ленина 43', 14.00),
('АО Прайм', 'АО', 'Менеджер Прайм', '+79111011120', 'prime@mail.ru', '7701234610', 'Тверь, Пушкина 44', 10.00),
('ООО Кварц', 'ООО', 'Бухгалтер Кварц', '+79111314150', 'quartz@mail.ru', '7701234611', 'Ярославль, Мира 45', 7.00),
('ИП Рябов', 'ИП', 'Рябов Р.Р.', '+79111617180', 'ryabov@mail.ru', '7701234612', 'Владимир, Горького 46', 12.00),
('ООО Сигма', 'ООО', 'Менеджер Сигма', '+79111920210', 'sigma@mail.ru', '7701234613', 'Иваново, Ленина 47', 8.00),
('ООО Тандем', 'ООО', 'Директор Тандем', '+79112223240', 'tandem@mail.ru', '7701234614', 'Кострома, Пушкина 48', 13.00),
('АО Ультра', 'АО', 'Менеджер Ультра', '+79112526270', 'ultra@mail.ru', '7701234615', 'Вологда, Мира 49', 9.00),
('ООО Флагман', 'ООО', 'Бухгалтер Флагман', '+79112829300', 'flagman@mail.ru', '7701234616', 'Псков, Горького 50', 11.00);

-- 4. INSERT MATERIALS (50 materials)
INSERT INTO svtables.materials (name, article, type, unit, price, waste_coefficient, supplier, current_stock, min_stock, default_width_m, default_height_m) VALUES
('Баннерная ткань 440 г/м²', 'BNR-440', 'Баннер', 'м²', 450.00, 1.05, 'Поставщик А', 500.00, 50.00, 3.20, 1.00),
('Баннерная ткань 510 г/м²', 'BNR-510', 'Баннер', 'м²', 520.00, 1.05, 'Поставщик А', 300.00, 30.00, 3.20, 1.00),
('Виниловая плёнка белая', 'VNL-WHT', 'Плёнка', 'м²', 380.00, 1.10, 'Поставщик Б', 800.00, 80.00, 1.52, 1.00),
('Виниловая плёнка прозрачная', 'VNL-TRN', 'Плёнка', 'м²', 420.00, 1.10, 'Поставщик Б', 400.00, 40.00, 1.52, 1.00),
('Самоклеящаяся плёнка', 'SML-001', 'Плёнка', 'м²', 350.00, 1.15, 'Поставщик В', 600.00, 60.00, 1.26, 1.00),
('Бумага для печати 170 г/м²', 'PPR-170', 'Бумага', 'лист', 5.50, 1.02, 'Поставщик Г', 10000.00, 1000.00, 0.42, 0.297),
('Бумага для печати 250 г/м²', 'PPR-250', 'Бумага', 'лист', 8.00, 1.02, 'Поставщик Г', 8000.00, 800.00, 0.42, 0.297),
('Бумага для печати 300 г/м²', 'PPR-300', 'Бумага', 'лист', 10.00, 1.02, 'Поставщик Г', 5000.00, 500.00, 0.42, 0.297),
('Картон 300 г/м²', 'CRT-300', 'Картон', 'лист', 15.00, 1.03, 'Поставщик Д', 3000.00, 300.00, 0.70, 1.00),
('Картон 400 г/м²', 'CRT-400', 'Картон', 'лист', 20.00, 1.03, 'Поставщик Д', 2000.00, 200.00, 0.70, 1.00),
('ПВХ 3 мм', 'PVC-03', 'Пластик', 'лист', 800.00, 1.05, 'Поставщик Е', 100.00, 10.00, 1.22, 2.44),
('ПВХ 5 мм', 'PVC-05', 'Пластик', 'лист', 1200.00, 1.05, 'Поставщик Е', 80.00, 8.00, 1.22, 2.44),
('ПВХ 10 мм', 'PVC-10', 'Пластик', 'лист', 2000.00, 1.05, 'Поставщик Е', 50.00, 5.00, 1.22, 2.44),
('Оргстекло 3 мм', 'ORG-03', 'Стекло', 'лист', 1500.00, 1.10, 'Поставщик Ж', 60.00, 6.00, 1.00, 1.50),
('Оргстекло 5 мм', 'ORG-05', 'Стекло', 'лист', 2500.00, 1.10, 'Поставщик Ж', 40.00, 4.00, 1.00, 1.50),
('Алюминиевый композит 4 мм', 'ALC-04', 'Композит', 'лист', 3500.00, 1.05, 'Поставщик З', 30.00, 3.00, 1.22, 2.44),
('Поликарбонат 6 мм', 'PLC-06', 'Пластик', 'лист', 1800.00, 1.08, 'Поставщик И', 45.00, 5.00, 1.22, 2.44),
('Поликарбонат 8 мм', 'PLC-08', 'Пластик', 'лист', 2400.00, 1.08, 'Поставщик И', 35.00, 4.00, 1.22, 2.44),
('Полистирол 2 мм', 'PLS-02', 'Пластик', 'лист', 400.00, 1.10, 'Поставщик К', 200.00, 20.00, 1.00, 2.00),
('Полистирол 3 мм', 'PLS-03', 'Пластик', 'лист', 550.00, 1.10, 'Поставщик К', 150.00, 15.00, 1.00, 2.00),
('ПЭТ 0.5 мм', 'PET-05', 'Пластик', 'лист', 250.00, 1.12, 'Поставщик Л', 300.00, 30.00, 1.00, 1.50),
('ПЭТ 1 мм', 'PET-10', 'Пластик', 'лист', 400.00, 1.12, 'Поставщик Л', 250.00, 25.00, 1.00, 1.50),
('Фотобумага 240 г/м²', 'FPR-240', 'Бумага', 'лист', 12.00, 1.03, 'Поставщик М', 4000.00, 400.00, 0.61, 0.914),
('Фотобумага 260 г/м²', 'FPR-260', 'Бумага', 'лист', 15.00, 1.03, 'Поставщик М', 3000.00, 300.00, 0.61, 0.914),
('Холст для печати', 'HLT-001', 'Ткань', 'м²', 800.00, 1.08, 'Поставщик Н', 100.00, 10.00, 1.60, 1.00),
('Блюбэк', 'BLB-001', 'Подложка', 'м²', 280.00, 1.05, 'Поставщик О', 400.00, 40.00, 1.52, 1.00),
('Литой винил', 'VNL-CST', 'Плёнка', 'м²', 550.00, 1.10, 'Поставщик П', 350.00, 35.00, 1.52, 1.00),
('Перфорированная плёнка', 'VNL-PRF', 'Плёнка', 'м²', 480.00, 1.12, 'Поставщик Р', 200.00, 20.00, 1.26, 1.00),
('Магнитный винил', 'VNL-MGN', 'Плёнка', 'м²', 650.00, 1.08, 'Поставщик С', 150.00, 15.00, 0.61, 1.00),
('Термоплёнка', 'VNL-TRM', 'Плёнка', 'м²', 320.00, 1.10, 'Поставщик Т', 500.00, 50.00, 0.51, 1.00),
('Ламинация глянцевая 75 мкм', 'LAM-GL75', 'Ламинация', 'м²', 120.00, 1.05, 'Поставщик У', 1000.00, 100.00, 1.52, 1.00),
('Ламинация матовая 75 мкм', 'LAM-MT75', 'Ламинация', 'м²', 130.00, 1.05, 'Поставщик У', 800.00, 80.00, 1.52, 1.00),
('Ламинация глянцевая 200 мкм', 'LAM-GL200', 'Ламинация', 'м²', 200.00, 1.05, 'Поставщик У', 500.00, 50.00, 1.52, 1.00),
('Ламинация матовая 200 мкм', 'LAM-MT200', 'Ламинация', 'м²', 220.00, 1.05, 'Поставщик У', 400.00, 40.00, 1.52, 1.00),
('Клей для ПВХ', 'GLU-PVC', 'Расходник', 'кг', 450.00, 1.00, 'Поставщик Ф', 50.00, 5.00, NULL, NULL),
('Растворитель', 'SLV-001', 'Расходник', 'л', 350.00, 1.00, 'Поставщик Х', 30.00, 3.00, NULL, NULL),
('Краска УФ белая', 'INK-UVW', 'Расходник', 'л', 2500.00, 1.00, 'Поставщик Ц', 20.00, 2.00, NULL, NULL),
('Краска УФ голубая', 'INK-UVC', 'Расходник', 'л', 2800.00, 1.00, 'Поставщик Ц', 15.00, 2.00, NULL, NULL),
('Краска УФ пурпурная', 'INK-UVM', 'Расходник', 'л', 2800.00, 1.00, 'Поставщик Ц', 15.00, 2.00, NULL, NULL),
('Краска УФ жёлтая', 'INK-UVY', 'Расходник', 'л', 2800.00, 1.00, 'Поставщик Ц', 15.00, 2.00, NULL, NULL),
('Краска УФ чёрная', 'INK-UVK', 'Расходник', 'л', 2600.00, 1.00, 'Поставщик Ц', 18.00, 2.00, NULL, NULL),
('Краска УФ лайм', 'INK-UVL', 'Расходник', 'л', 3000.00, 1.00, 'Поставщик Ц', 10.00, 1.00, NULL, NULL),
('Пленка для лазерного принтера', 'PPR-LSR', 'Бумага', 'лист', 15.00, 1.05, 'Поставщик Ч', 2000.00, 200.00, 0.21, 0.297),
('Трансферная плёнка', 'VNL-TRF', 'Плёнка', 'м²', 750.00, 1.15, 'Поставщик Ш', 120.00, 12.00, 0.51, 1.00),
('Светоотражающая плёнка', 'VNL-RFL', 'Плёнка', 'м²', 900.00, 1.10, 'Поставщик Щ', 80.00, 8.00, 1.26, 1.00),
('Броневая плёнка', 'VNL-BRN', 'Плёнка', 'м²', 1200.00, 1.10, 'Поставщик Ъ', 50.00, 5.00, 1.26, 1.00),
('Гофрокартон 3 мм', 'CRG-03', 'Картон', 'м²', 80.00, 1.05, 'Поставщик Ы', 500.00, 50.00, 1.20, 2.40),
('Гофрокартон 5 мм', 'CRG-05', 'Картон', 'м²', 120.00, 1.05, 'Поставщик Ы', 400.00, 40.00, 1.20, 2.40),
('Пенокартон 5 мм', 'CRP-05', 'Картон', 'лист', 180.00, 1.08, 'Поставщик Ь', 200.00, 20.00, 0.70, 1.00),
('Пенокартон 10 мм', 'CRP-10', 'Картон', 'лист', 280.00, 1.08, 'Поставщик Ь', 150.00, 15.00, 0.70, 1.00),
('Композитная панель 3 мм', 'CMP-03', 'Композит', 'лист', 2800.00, 1.05, 'Поставщик Э', 25.00, 3.00, 1.22, 2.44);

-- 5. INSERT MATERIAL OPERATIONS
INSERT INTO svtables.material_operations (material_id, name, price_per_unit, unit, sort_order, active) VALUES
(1, 'Широкоформатная печать', 450.00, 'м²', 1, true),
(2, 'Широкоформатная печать 510', 520.00, 'м²', 1, true),
(3, 'Печать на виниле', 380.00, 'м²', 1, true),
(4, 'Печать на прозрачном виниле', 420.00, 'м²', 1, true),
(5, 'Печать на самоклейке', 350.00, 'м²', 1, true),
(31, 'Ламинация глянцевая', 120.00, 'м²', 1, true),
(32, 'Ламинация матовая', 130.00, 'м²', 1, true),
(NULL, 'Резка по контуру', 50.00, 'шт', 1, true),
(NULL, 'Фигурная резка', 80.00, 'шт', 2, true),
(NULL, 'Перфорация', 15.00, 'шт', 1, true),
(NULL, 'Установка люверсов', 25.00, 'шт', 1, true),
(NULL, 'Склейка', 30.00, 'шт', 1, true),
(NULL, 'Сборка конструкции', 150.00, 'шт', 1, true),
(NULL, 'Упаковка', 50.00, 'шт', 1, true),
(11, 'Фрезеровка ПВХ', 500.00, 'м²', 1, true),
(15, 'Резка оргстекла', 300.00, 'м²', 1, true),
(16, 'Резка алюминиевого композита', 400.00, 'м²', 1, true),
(25, 'Печать на холсте', 800.00, 'м²', 1, true),
(23, 'Интерьерная печать', 15.00, 'лист', 1, true),
(44, 'Термоперенос', 200.00, 'м²', 1, true);

-- 6. ASSIGN WORKSHOP OPERATIONS
INSERT INTO svtables.workshop_operations (workshop_id, operation_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 18), (1, 19),
(2, 6), (2, 7),
(3, 8), (3, 9), (3, 15), (3, 16), (3, 17),
(4, 10), (4, 11), (4, 12), (4, 13),
(5, 14);

-- 7. GENERATE 1000 ORDERS WITH ITEMS, MATERIALS, OPERATIONS, FILES
-- OrderStatus enum: DRAFT, APPROVAL, IN_PROGRESS, READY
-- ProductionStage enum: NOT_STARTED, DESIGN, PRINTING, FINISHING, QUALITY_CONTROL, PACKAGING, SHIPPING
DO $$
DECLARE
    v_order_id BIGINT;
    v_order_number VARCHAR(50);
    v_client_id BIGINT;
    v_manager_id BIGINT;
    v_status VARCHAR(30);
    v_production_stage VARCHAR(50);
    v_order_date DATE;
    v_due_date DATE;
    v_total_amount NUMERIC(12,2);
    v_paid_amount NUMERIC(12,2);
    v_cost_price NUMERIC(15,2);
    v_margin_percent NUMERIC(8,2);
    v_priceplus NUMERIC(10,2);
    v_item_id BIGINT;
    v_material_id BIGINT;
    v_num_items INTEGER;
    v_num_materials INTEGER;
    v_num_operations INTEGER;
    v_num_files INTEGER;
    v_num_stages INTEGER;
    v_workshop_id BIGINT;
    v_stage_status VARCHAR(50);
    v_comment_author_id BIGINT;
    v_payment_amount NUMERIC(12,2);
    v_payment_date DATE;
    v_statuses TEXT[] := ARRAY['DRAFT', 'APPROVAL', 'IN_PROGRESS', 'READY'];
    v_stages TEXT[] := ARRAY['NOT_STARTED', 'DESIGN', 'PRINTING', 'FINISHING', 'QUALITY_CONTROL', 'PACKAGING', 'SHIPPING'];
    v_stage_statuses TEXT[] := ARRAY['NOT_STARTED', 'IN_PROGRESS', 'DONE'];
    v_item_names TEXT[] := ARRAY['Баннер 3x1.5', 'Баннер 2x1', 'Плакат А1', 'Плакат А2', 'Наклейка', 'Вывеска', 'Штендер', 'Табличка', 'Плотер', 'Холст', 'Фотообои', 'Roll-up', 'Пресс-волл', 'Мобильная стойка', 'Информационный стенд'];
    v_descriptions TEXT[] := ARRAY['Срочный заказ', 'Стандартный заказ', 'Для выставки', 'Для магазина', 'Для офиса', 'Для мероприятия', 'Тестовый заказ', 'Повторный заказ'];
    v_file_names TEXT[] := ARRAY['design.pdf', 'layout.ai', 'image.png', 'photo.jpg', 'logo.svg', 'scheme.dwg', 'mockup.psd', 'preview.jpg', 'tech_spec.docx', 'requirements.pdf'];
    v_mime_types TEXT[] := ARRAY['application/pdf', 'application/postscript', 'image/png', 'image/jpeg', 'image/svg+xml', 'application/acad', 'image/vnd.adobe.photoshop', 'image/jpeg', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/pdf'];
    v_comment_texts TEXT[] := ARRAY['Заказ принят', 'Начато производство', 'Ожидает согласования', 'Отправлено клиенту', 'Получено подтверждение', 'Производство завершено', 'Отгружено', 'Закрыто'];
    v_manager_ids BIGINT[] := ARRAY[16, 17, 18];
    v_workshop_ids BIGINT[] := ARRAY[1, 2, 3, 4, 5];
    v_employee_ids BIGINT[] := ARRAY[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20];
    i INTEGER;
    j INTEGER;
    k INTEGER;
    m INTEGER;
    n INTEGER;
    v_emp_idx INTEGER;
    v_stage_name VARCHAR(50);
    v_stage_workshop_map INTEGER[] := ARRAY[1, 1, 2, 4, 3, 5]; -- DESIGN->1, PRINTING->1, FINISHING->2, QUALITY_CONTROL->4, PACKAGING->3, SHIPPING->5
BEGIN
    FOR i IN 1..1000 LOOP
        v_order_number := 'ORD-' || LPAD(i::TEXT, 6, '0');
        v_client_id := 1 + (i % 50);
        v_manager_id := v_manager_ids[1 + (i % 3)];
        v_status := v_statuses[1 + (i % 4)];
        v_production_stage := v_stages[1 + (i % 7)];
        v_order_date := CURRENT_DATE - (30 + (i % 335));
        v_due_date := v_order_date + (3 + (i % 14));
        v_priceplus := 5 + (i % 15);
        v_total_amount := 5000 + (i * 100) + (random() * 5000)::NUMERIC(12,2);
        v_paid_amount := CASE 
            WHEN v_status = 'READY' THEN v_total_amount
            WHEN v_status = 'IN_PROGRESS' THEN (v_total_amount * (0.3 + random() * 0.5))::NUMERIC(12,2)
            ELSE (v_total_amount * random() * 0.3)::NUMERIC(12,2)
        END;
        v_cost_price := (v_total_amount * (0.4 + random() * 0.3))::NUMERIC(15,2);
        v_margin_percent := CASE WHEN v_cost_price > 0 THEN ((v_total_amount - v_cost_price) / v_cost_price * 100)::NUMERIC(8,2) ELSE 0 END;

        INSERT INTO svtables.orders (
            order_number, client_id, description, total_amount, paid_amount, debt_amount,
            cost_price, margin_percent, status, production_stage, order_date, due_date,
            manager_id, priceplus, total_with_priceplus, launched_at, ready_at, accepted_at, closed_at
        ) VALUES (
            v_order_number, v_client_id, v_descriptions[1 + (i % 8)], 
            v_total_amount, v_paid_amount, v_total_amount - v_paid_amount,
            v_cost_price, v_margin_percent, v_status, v_production_stage, v_order_date, v_due_date,
            v_manager_id, v_priceplus, v_total_amount + v_priceplus,
            CASE WHEN v_status != 'DRAFT' THEN v_order_date + INTERVAL '1 day' ELSE NULL END,
            CASE WHEN v_status = 'READY' THEN v_due_date - INTERVAL '1 day' ELSE NULL END,
            CASE WHEN v_status IN ('IN_PROGRESS', 'READY') THEN v_order_date + INTERVAL '2 days' ELSE NULL END,
            CASE WHEN v_status = 'READY' THEN v_due_date + INTERVAL '1 day' ELSE NULL END
        ) RETURNING id INTO v_order_id;

        v_num_items := 1 + (i % 5);
        FOR j IN 1..v_num_items LOOP
            INSERT INTO svtables.order_items (
                order_id, name, width, height, price, quantity, cost, ready_date, params
            ) VALUES (
                v_order_id, 
                v_item_names[1 + ((i + j) % 15)],
                (0.5 + random() * 3)::NUMERIC(10,3),
                (0.5 + random() * 2)::NUMERIC(10,3),
                (500 + random() * 5000)::NUMERIC(12,2),
                1 + (j % 10),
                (200 + random() * 2000)::NUMERIC(12,2),
                v_due_date - (j % 3),
                jsonb_build_object(
                    'color', CASE (i + j) % 4 WHEN 0 THEN 'CMYK' WHEN 1 THEN 'RGB' WHEN 2 THEN 'Pantone' ELSE 'Black' END,
                    'resolution', CASE (i + j) % 3 WHEN 0 THEN '720dpi' WHEN 1 THEN '1440dpi' ELSE '360dpi' END,
                    'finish', CASE (i + j) % 3 WHEN 0 THEN 'glossy' WHEN 1 THEN 'matte' ELSE 'satin' END
                )
            ) RETURNING id INTO v_item_id;

            v_num_materials := 1 + (j % 3);
            FOR k IN 1..v_num_materials LOOP
                v_material_id := 1 + ((i + j + k) % 50);
                INSERT INTO svtables.order_materials (
                    order_id, order_item_id, material_id, quantity, waste_coefficient,
                    cost, cost_priceplus, width_m, height_m, eyelet_cost, ready_date
                ) VALUES (
                    v_order_id,
                    v_item_id,
                    v_material_id,
                    (1 + random() * 20)::NUMERIC(12,2),
                    1 + (random() * 0.15)::NUMERIC(5,3),
                    (100 + random() * 2000)::NUMERIC(12,2),
                    (100 + random() * 2200)::NUMERIC(12,2),
                    (0.5 + random() * 3)::NUMERIC(10,3),
                    (0.5 + random() * 2)::NUMERIC(10,3),
                    CASE WHEN k = 1 THEN (10 + random() * 50)::NUMERIC(12,2) ELSE 0 END,
                    v_due_date - (k % 2)
                );
            END LOOP;

            v_num_operations := 1 + (j % 4);
            FOR m IN 1..v_num_operations LOOP
                INSERT INTO svtables.order_item_operations (
                    order_item_id, operation_id, operation_name, price_per_unit,
                    calculated_quantity, subtotal, width_m, height_m
                ) VALUES (
                    v_item_id,
                    1 + ((i + j + m) % 20),
                    (SELECT name FROM svtables.material_operations WHERE id = 1 + ((i + j + m) % 20)),
                    (50 + random() * 500)::NUMERIC(12,2),
                    (1 + random() * 10)::NUMERIC(12,4),
                    (100 + random() * 3000)::NUMERIC(12,2),
                    (0.5 + random() * 3)::NUMERIC(10,4),
                    (0.5 + random() * 2)::NUMERIC(10,4)
                );
            END LOOP;

            v_num_files := (j % 3);
            FOR n IN 1..v_num_files LOOP
                INSERT INTO svtables.files (
                    file_name, original_name, file_path, file_url, mime_type,
                    file_size, order_id, order_item_id, uploaded_by
                ) VALUES (
                    'file_' || v_item_id || '_' || n || '_' || (1000000 + i)::BIGINT,
                    v_file_names[1 + ((i + j + n) % 10)],
                    '/uploads/orders/' || v_order_id || '/items/' || v_item_id || '/',
                    '/api/files/download/' || v_item_id || '/' || n,
                    v_mime_types[1 + ((i + j + n) % 10)],
                    (100000 + (random() * 5000000)::BIGINT)::BIGINT,
                    v_order_id,
                    v_item_id,
                    'manager_' || v_manager_id::TEXT
                );
            END LOOP;
        END LOOP;

        v_num_files := (i % 4);
        FOR n IN 1..v_num_files LOOP
            INSERT INTO svtables.files (
                file_name, original_name, file_path, file_url, mime_type,
                file_size, order_id, uploaded_by
            ) VALUES (
                'file_order_' || v_order_id || '_' || n || '_' || (2000000 + i)::BIGINT,
                v_file_names[1 + ((i + n) % 10)],
                '/uploads/orders/' || v_order_id || '/',
                '/api/files/download/order/' || v_order_id || '/' || n,
                v_mime_types[1 + ((i + n) % 10)],
                (100000 + (random() * 5000000)::BIGINT)::BIGINT,
                v_order_id,
                'manager_' || v_manager_id::TEXT
            );
        END LOOP;

        v_num_stages := 2 + (i % 5);
        FOR k IN 1..v_num_stages LOOP
            v_stage_name := v_stages[k];
            v_workshop_id := v_stage_workshop_map[k];
            
            v_stage_status := CASE v_status
                WHEN 'READY' THEN 'DONE'
                WHEN 'IN_PROGRESS' THEN
                    CASE 
                        WHEN k <= 2 THEN 'DONE'
                        WHEN k = 3 THEN 'IN_PROGRESS'
                        ELSE 'NOT_STARTED'
                    END
                WHEN 'APPROVAL' THEN
                    CASE 
                        WHEN k = 1 THEN 'DONE'
                        WHEN k = 2 THEN 'IN_PROGRESS'
                        ELSE 'NOT_STARTED'
                    END
                WHEN 'DRAFT' THEN
                    CASE 
                        WHEN k = 1 THEN 'IN_PROGRESS'
                        ELSE 'NOT_STARTED'
                    END
                ELSE 'NOT_STARTED'
            END;
            
            INSERT INTO svtables.order_stages (
                order_id, workshop_id, wait_previous, due_date, note, status, source_files
            ) VALUES (
                v_order_id,
                v_workshop_id,
                k > 1,
                CURRENT_DATE + (k * 2),
                v_stage_name || ': ' || (SELECT name FROM svtables.workshops WHERE id = v_workshop_id),
                v_stage_status,
                CASE WHEN k = 1 THEN '/uploads/orders/' || v_order_id || '/source/' ELSE NULL END
            );
        END LOOP;

        IF i % 3 = 0 THEN
            v_emp_idx := 1 + ((i - 1) % 20);
            v_comment_author_id := v_employee_ids[v_emp_idx];
            INSERT INTO svtables.order_comments (order_id, author_id, message, created_at)
            VALUES (v_order_id, v_comment_author_id, v_comment_texts[1 + (i % 8)], v_order_date + INTERVAL '1 day');
        END IF;
        IF i % 5 = 0 THEN
            v_emp_idx := 1 + (i % 20);
            v_comment_author_id := v_employee_ids[v_emp_idx];
            INSERT INTO svtables.order_comments (order_id, author_id, message, created_at)
            VALUES (v_order_id, v_comment_author_id, v_comment_texts[1 + ((i + 3) % 8)], v_order_date + INTERVAL '2 days');
        END IF;

        IF v_paid_amount > 0 THEN
            v_payment_date := v_order_date + 1;
            INSERT INTO svtables.payments (order_id, amount, payment_date, payment_type, details, is_partial)
            VALUES (v_order_id, 
                CASE WHEN v_paid_amount > 10000 THEN (v_paid_amount * 0.5)::NUMERIC(12,2) ELSE v_paid_amount END,
                v_payment_date,
                CASE i % 3 WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'TRANSFER' END,
                'Оплата заказа ' || v_order_number,
                v_paid_amount > 10000
            );
            
            IF v_paid_amount > 10000 THEN
                INSERT INTO svtables.payments (order_id, amount, payment_date, payment_type, details, is_partial)
                VALUES (v_order_id, 
                    (v_paid_amount * 0.5)::NUMERIC(12,2),
                    v_payment_date + 7,
                    CASE (i + 1) % 3 WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'TRANSFER' END,
                    'Доплата заказа ' || v_order_number,
                    true
                );
            END IF;
        END IF;
    END LOOP;
END $$;

-- Show statistics
SELECT 'Orders' as entity, COUNT(*)::TEXT as count FROM svtables.orders
UNION ALL
SELECT 'Order Items', COUNT(*)::TEXT FROM svtables.order_items
UNION ALL
SELECT 'Order Materials', COUNT(*)::TEXT FROM svtables.order_materials
UNION ALL
SELECT 'Order Item Operations', COUNT(*)::TEXT FROM svtables.order_item_operations
UNION ALL
SELECT 'Files', COUNT(*)::TEXT FROM svtables.files
UNION ALL
SELECT 'Order Stages', COUNT(*)::TEXT FROM svtables.order_stages
UNION ALL
SELECT 'Payments', COUNT(*)::TEXT FROM svtables.payments
UNION ALL
SELECT 'Comments', COUNT(*)::TEXT FROM svtables.order_comments
UNION ALL
SELECT 'Clients', COUNT(*)::TEXT FROM svtables.clients
UNION ALL
SELECT 'Employees', COUNT(*)::TEXT FROM svtables.employees
UNION ALL
SELECT 'Materials', COUNT(*)::TEXT FROM svtables.materials
UNION ALL
SELECT 'Workshops', COUNT(*)::TEXT FROM svtables.workshops
UNION ALL
SELECT 'Material Operations', COUNT(*)::TEXT FROM svtables.material_operations;
