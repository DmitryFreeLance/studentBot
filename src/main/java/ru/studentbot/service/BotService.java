// Этот файл отвечает за основную логику бота и обработку обновлений.
package ru.studentbot.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.studentbot.model.Category;
import ru.studentbot.model.Event;
import ru.studentbot.model.UserState;
import ru.studentbot.repo.EventRepository;
import ru.studentbot.repo.RegistrationRepository;
import ru.studentbot.repo.StateRepository;
import ru.studentbot.service.MessageFactory.Button;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
 * Основная логика бота.
 * Обрабатывает апдейты, управляет состояниями и админ-панелью.
 */
@Service
public class BotService {
    private static final Logger log = LoggerFactory.getLogger(BotService.class);
    private static final String PASSWORD = "Teacher2026";
    private static final String STATE_IDLE = "IDLE";
    private static final String STATE_WAITING_SIGNUP = "WAITING_SIGNUP";
    private static final String STATE_ADMIN_WAITING_ADD = "ADMIN_WAITING_ADD";
    private static final String STATE_ADMIN_WAITING_EDIT = "ADMIN_WAITING_EDIT";
    private final ObjectMapper objectMapper;
    private final MaxApiClient apiClient;
    private final MessageFactory messageFactory;
    private final StateRepository stateRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    /*
     * Конструктор сервиса.
     * Получает зависимости для парсинга, отправки сообщений и БД.
     */
    public BotService(ObjectMapper objectMapper,
                      MaxApiClient apiClient,
                      MessageFactory messageFactory,
                      StateRepository stateRepository,
                      EventRepository eventRepository,
                      RegistrationRepository registrationRepository) {
        this.objectMapper = objectMapper;
        this.apiClient = apiClient;
        this.messageFactory = messageFactory;
        this.stateRepository = stateRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }
    /*
     * Принимает сырое тело апдейта.
     * Парсит JSON и передает дальше в общий обработчик.
     */
    public void handleUpdate(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            handleUpdateNode(root);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse update", e);
        }
    }
    /*
     * Определяет тип апдейта.
     * Вызывает обработчик сообщений или callback-кнопок.
     */
    public void handleUpdateNode(JsonNode root) {
        String updateType = root.path("update_type").asText("");
        switch (updateType) {
            case "message_created" -> handleMessageCreated(root);
            case "message_callback" -> handleCallback(root);
            default -> log.debug("Skip update type: {}", updateType);
        }
    }
    /*
     * Обрабатывает входящее сообщение пользователя.
     * Проверяет /start, пароль админа, ожидание ввода и общий fallback.
     */
    private void handleMessageCreated(JsonNode root) {
        JsonNode message = root.path("message");
        String text = message.path("body").path("text").asText("").trim();
        if (text.isEmpty()) {
            return;
        }
        long userId = message.path("sender").path("user_id").asLong(0);
        if (userId == 0) {
            return;
        }
        UserState state = stateRepository.findByUserId(userId);
        boolean isAdmin = state != null && state.isAdmin();
        if ("/start".equalsIgnoreCase(text)) {
            stateRepository.clear(userId, isAdmin);
            sendToRecipient(message, startMessage());
            return;
        }
        if (PASSWORD.equals(text)) {
            stateRepository.save(userId, STATE_IDLE, null, true);
            sendToRecipient(message, adminMenuMessage());
            return;
        }
        if (state != null && STATE_WAITING_SIGNUP.equals(state.getState())) {
            handleSignupText(message, userId, state, text, isAdmin);
            return;
        }
        if (state != null && isAdmin) {
            if (STATE_ADMIN_WAITING_ADD.equals(state.getState())) {
                handleAdminAdd(message, userId, state, text);
                return;
            }
            if (STATE_ADMIN_WAITING_EDIT.equals(state.getState())) {
                handleAdminEdit(message, userId, state, text);
                return;
            }
        }
        sendToRecipient(message, fallbackMessage(isAdmin));
    }
    /*
     * Обрабатывает нажатия inline-кнопок.
     * Читает payload, обновляет состояние и отвечает через answerCallback.
     */
    private void handleCallback(JsonNode root) {
        JsonNode callback = root.path("callback");
        String payload = callback.path("payload").asText("");
        String callbackId = callback.path("callback_id").asText("");
        long userId = callback.path("user").path("user_id").asLong(0);
        if (callbackId.isEmpty() || userId == 0) {
            return;
        }
        UserState state = stateRepository.findByUserId(userId);
        boolean isAdmin = state != null && state.isAdmin();
        ObjectNode response = switch (payload) {
            case "start_yes" -> {
                stateRepository.clear(userId, isAdmin);
                yield categoriesMessage();
            }
            case "back:categories" -> {
                stateRepository.clear(userId, isAdmin);
                yield categoriesMessage();
            }
            case "admin:exit" -> {
                stateRepository.clear(userId, false);
                yield startMessage();
            }
            case "admin:menu" -> {
                stateRepository.save(userId, STATE_IDLE, null, true);
                yield adminMenuMessage();
            }
            case "admin:add_menu" -> {
                stateRepository.save(userId, STATE_IDLE, null, true);
                yield adminAddMenuMessage();
            }
            default -> handleCallbackPayload(payload, userId, isAdmin);
        };
        if (response != null) {
            apiClient.answerCallback(callbackId, response);
        }
    }
    /*
     * Разбирает payload от кнопки.
     * Роутит на выбор категории, просмотр, запись и админ-операции.
     */
    private ObjectNode handleCallbackPayload(String payload, long userId, boolean isAdmin) {
        if (payload.startsWith("cat:")) {
            Category category = Category.fromPayload(payload.substring("cat:".length()));
            stateRepository.clear(userId, isAdmin);
            return category != null ? userCategoryMessage(category) : categoriesMessage();
        }
        if (payload.startsWith("event:")) {
            long eventId = parseId(payload, "event:");
            return eventId > 0 ? eventDetailsMessage(eventId) : categoriesMessage();
        }
        if (payload.startsWith("signup:")) {
            long eventId = parseId(payload, "signup:");
            if (eventId > 0) {
                stateRepository.save(userId, STATE_WAITING_SIGNUP, "eventId=" + eventId, isAdmin);
                return signupPromptMessage(eventId);
            }
            return categoriesMessage();
        }
        if (payload.startsWith("back:event:")) {
            long eventId = parseId(payload, "back:event:");
            stateRepository.clear(userId, isAdmin);
            return eventId > 0 ? eventDetailsMessage(eventId) : categoriesMessage();
        }
        if (payload.startsWith("admin:cat:")) {
            Category category = Category.fromPayload(payload.substring("admin:cat:".length()));
            if (category == null) {
                return adminMenuMessage();
            }
            return adminCategoryMessage(category);
        }
        if (payload.startsWith("admin:add:")) {
            Category category = Category.fromPayload(payload.substring("admin:add:".length()));
            if (category == null) {
                return adminMenuMessage();
            }
            stateRepository.save(userId, STATE_ADMIN_WAITING_ADD, "category=" + category.name(), true);
            return adminAddPrompt(category);
        }
        if (payload.startsWith("admin:event:")) {
            long eventId = parseId(payload, "admin:event:");
            return eventId > 0 ? adminEventMessage(eventId) : adminMenuMessage();
        }
        if (payload.startsWith("admin:edit:")) {
            long eventId = parseId(payload, "admin:edit:");
            if (eventId > 0) {
                stateRepository.save(userId, STATE_ADMIN_WAITING_EDIT, "eventId=" + eventId, true);
                return adminEditPrompt(eventId);
            }
            return adminMenuMessage();
        }
        if (payload.startsWith("admin:delete:")) {
            long eventId = parseId(payload, "admin:delete:");
            if (eventId > 0) {
                Event event = eventRepository.findById(eventId);
                if (event != null) {
                    eventRepository.deactivate(eventId);
                    return adminCategoryMessage(event.getCategory(), "Мероприятие удалено.");
                }
            }
            return adminMenuMessage();
        }
        return null;
    }
    /*
     * Сохраняет заявку пользователя на мероприятие.
     * Записывает текст в базу и возвращает подтверждение.
     */
    private void handleSignupText(JsonNode message, long userId, UserState state, String text, boolean isAdmin) {
        Map<String, String> data = parseData(state.getData());
        long eventId = parseLong(data.get("eventId"));
        if (eventId <= 0) {
            stateRepository.clear(userId, isAdmin);
            sendToRecipient(message, categoriesMessage());
            return;
        }
        registrationRepository.insert(eventId, userId, text);
        stateRepository.clear(userId, isAdmin);
        Event event = eventRepository.findById(eventId);
        String title = event != null ? event.getTitle() : "мероприятие";
        String responseText = "Спасибо! Я записал вашу заявку на «" + title +
                "».\n\nЕсли захотите изменить данные или отказаться от участия, просто напишите мне.";
        ObjectNode response = messageFactory.textWithButtons(
                responseText,
                List.of(List.of(new Button("В меню", "start_yes")))
        );
        sendToRecipient(message, response);
    }
    /*
     * Создает новое мероприятие из текста администратора.
     * Валидирует формат и добавляет запись в базу.
     */
    private void handleAdminAdd(JsonNode message, long userId, UserState state, String text) {
        Map<String, String> data = parseData(state.getData());
        Category category = Category.fromPayload(data.get("category"));
        if (category == null) {
            stateRepository.save(userId, STATE_IDLE, null, true);
            sendToRecipient(message, adminMenuMessage());
            return;
        }
        EventInput input = EventInput.parse(text);
        if (!input.isValid()) {
            sendToRecipient(message, adminInputError(category));
            return;
        }
        eventRepository.insert(category, input.title(), input.details());
        stateRepository.save(userId, STATE_IDLE, null, true);
        sendToRecipient(message, adminCategoryMessage(category, "Мероприятие добавлено."));
    }
    /*
     * Обновляет существующее мероприятие.
     * Проверяет формат и сохраняет изменения.
     */
    private void handleAdminEdit(JsonNode message, long userId, UserState state, String text) {
        Map<String, String> data = parseData(state.getData());
        long eventId = parseLong(data.get("eventId"));
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            stateRepository.save(userId, STATE_IDLE, null, true);
            sendToRecipient(message, adminMenuMessage());
            return;
        }
        EventInput input = EventInput.parse(text);
        if (!input.isValid()) {
            sendToRecipient(message, adminInputError(event.getCategory()));
            return;
        }
        eventRepository.update(eventId, input.title(), input.details());
        stateRepository.save(userId, STATE_IDLE, null, true);
        sendToRecipient(message, adminEventMessage(eventId, "Мероприятие обновлено."));
    }
    /*
     * Отправляет ответ в нужное место.
     * Если это чат — шлет в чат, иначе в личный диалог.
     */
    private void sendToRecipient(JsonNode message, ObjectNode response) {
        long chatId = message.path("recipient").path("chat_id").asLong(0);
        String chatType = message.path("recipient").path("chat_type").asText("");
        long userId = message.path("sender").path("user_id").asLong(0);
        if (chatId > 0 && !"dialog".equalsIgnoreCase(chatType)) {
            apiClient.sendMessageToChat(chatId, response);
        } else {
            apiClient.sendMessageToUser(userId, response);
        }
    }
    /*
     * Формирует стартовое приветствие.
     * Добавляет кнопку «Да!» для продолжения.
     */
    private ObjectNode startMessage() {
        String text = "Привет! Рассказать тебе, какие мероприятия проходят у нас в школе?";
        return messageFactory.textWithButtons(text, List.of(List.of(new Button("Да!", "start_yes"))));
    }
    /*
     * Формирует меню категорий мероприятий.
     * Добавляет inline-кнопки для выбора направления.
     */
    private ObjectNode categoriesMessage() {
        String text = "Какие мероприятия тебя интересуют?";
        List<List<Button>> buttons = new ArrayList<>();
        buttons.add(List.of(new Button("Культурные", "cat:CULTURAL"), new Button("Развлекательные", "cat:ENTERTAINMENT")));
        buttons.add(List.of(new Button("Спортивные", "cat:SPORTS")));
        return messageFactory.textWithButtons(text, buttons);
    }
    /*
     * Показывает мероприятия выбранной категории.
     * Если одно — показывает карточку, если несколько — список кнопок.
     */
    private ObjectNode userCategoryMessage(Category category) {
        List<Event> events = eventRepository.findActiveByCategory(category);
        if (events.isEmpty()) {
            String text = category.getEmoji() + " В этой категории пока нет активных мероприятий.\n" +
                    "Можно выбрать другое направление.";
            return messageFactory.textWithButtons(
                    text,
                    List.of(List.of(new Button("Назад", "back:categories")))
            );
        }
        if (events.size() == 1) {
            return eventDetailsMessage(events.get(0));
        }
        String text = category.getEmoji() + " Здесь несколько мероприятий. Выберите то, которое интересно:";
        List<List<Button>> buttons = new ArrayList<>();
        for (Event event : events) {
            buttons.add(List.of(new Button(event.getTitle(), "event:" + event.getId())));
        }
        buttons.add(List.of(new Button("Назад", "back:categories")));
        return messageFactory.textWithButtons(text, buttons);
    }
    /*
     * Загружает мероприятие по id.
     * Возвращает карточку или меню категорий, если id неверный.
     */
    private ObjectNode eventDetailsMessage(long eventId) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            return categoriesMessage();
        }
        return eventDetailsMessage(event);
    }
    /*
     * Формирует карточку мероприятия.
     * Добавляет кнопку записи и кнопку возврата.
     */
    private ObjectNode eventDetailsMessage(Event event) {
        String text = event.getDetails() + "\n\n" +
                "Если хотите участвовать, нажмите «Записаться» и отправьте ФИО и класс.";
        List<List<Button>> buttons = new ArrayList<>();
        buttons.add(List.of(new Button("Записаться", "signup:" + event.getId())));
        buttons.add(List.of(new Button("Назад", "back:categories")));
        return messageFactory.textWithButtons(text, buttons);
    }
    /*
     * Просит пользователя отправить ФИО и класс.
     * Дает пример и кнопку «Назад».
     */
    private ObjectNode signupPromptMessage(long eventId) {
        Event event = eventRepository.findById(eventId);
        String title = event != null ? event.getTitle() : "мероприятие";
        String text = "Отлично! Напишите одним сообщением ваши ФИО и класс.\n" +
                "Пример: Иванов Иван Иванович, 7Б.\n\n" +
                "Мы запишем вас на «" + title + "».";
        return messageFactory.textWithButtons(
                text,
                List.of(List.of(new Button("Назад", "back:event:" + eventId)))
        );
    }
    /*
     * Отправляет подсказку при непонятном сообщении.
     * Предлагает вернуться в меню и админ-панель для админа.
     */
    private ObjectNode fallbackMessage(boolean isAdmin) {
        String text = "Я могу показать список школьных мероприятий и записать вас на участие.";
        List<List<Button>> buttons = new ArrayList<>();
        buttons.add(List.of(new Button("В меню", "start_yes")));
        if (isAdmin) {
            buttons.add(List.of(new Button("Админ-панель", "admin:menu")));
        }
        return messageFactory.textWithButtons(text, buttons);
    }
    /*
     * Формирует главное меню админа.
     * Показывает список активных мероприятий и кнопку добавления.
     */
    private ObjectNode adminMenuMessage() {
        String text = "Добро пожаловать в админ-панель.\n\nЧто вы хотите поменять?";
        List<Event> events = eventRepository.findAllActive();
        List<List<Button>> buttons = new ArrayList<>();
        for (Event event : events) {
            String label = event.getTitle() + " (" + event.getCategory().getDisplayName() + ")";
            buttons.add(List.of(new Button(label, "admin:event:" + event.getId())));
        }
        buttons.add(List.of(new Button("Добавить", "admin:add_menu")));
        buttons.add(List.of(new Button("Выйти", "admin:exit")));
        return messageFactory.textWithButtons(text, buttons);
    }
    /*
     * Предлагает выбрать категорию для добавления.
     * Нужен промежуточный шаг перед вводом данных.
     */
    private ObjectNode adminAddMenuMessage() {
        String text = "Куда добавить мероприятие?";
        List<List<Button>> buttons = new ArrayList<>();
        buttons.add(List.of(new Button("Культурные", "admin:add:CULTURAL"), new Button("Развлекательные", "admin:add:ENTERTAINMENT")));
        buttons.add(List.of(new Button("Спортивные", "admin:add:SPORTS")));
        buttons.add(List.of(new Button("Назад", "admin:menu")));
        return messageFactory.textWithButtons(text, buttons);
    }
    /*
     * Меню категории для админа.
     * Позволяет выбрать мероприятие или добавить новое.
     */
    private ObjectNode adminCategoryMessage(Category category) {
        return adminCategoryMessage(category, null);
    }
    /*
     * То же меню категории, но с уведомлением.
     * Используется после успешных действий или ошибок.
     */
    private ObjectNode adminCategoryMessage(Category category, String notice) {
        List<Event> events = eventRepository.findActiveByCategory(category);
        StringBuilder text = new StringBuilder();
        text.append("Категория: ").append(category.getDisplayName()).append(".\n");
        if (notice != null && !notice.isBlank()) {
            text.append(notice).append("\n");
        }
        text.append("\nВыберите мероприятие для изменения или добавьте новое.");
        List<List<Button>> buttons = new ArrayList<>();
        for (Event event : events) {
            buttons.add(List.of(new Button(event.getTitle(), "admin:event:" + event.getId())));
        }
        buttons.add(List.of(new Button("Добавить", "admin:add:" + category.name())));
        buttons.add(List.of(new Button("Назад", "admin:menu")));
        return messageFactory.textWithButtons(text.toString(), buttons);
    }
    /*
     * Карточка мероприятия для админа.
     * Дает кнопки «Изменить» и «Удалить».
     */
    private ObjectNode adminEventMessage(long eventId) {
        return adminEventMessage(eventId, null);
    }
    /*
     * Карточка мероприятия с уведомлением.
     * Используется после обновления или удаления.
     */
    private ObjectNode adminEventMessage(long eventId, String notice) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            return adminMenuMessage();
        }
        StringBuilder text = new StringBuilder();
        text.append(event.getDetails());
        if (notice != null && !notice.isBlank()) {
            text.append("\n\n").append(notice);
        }
        text.append("\n\nЧто вы хотите сделать?");
        List<List<Button>> buttons = new ArrayList<>();
        buttons.add(List.of(new Button("Изменить", "admin:edit:" + eventId), new Button("Удалить", "admin:delete:" + eventId)));
        buttons.add(List.of(new Button("Назад", "admin:cat:" + event.getCategory().name())));
        return messageFactory.textWithButtons(text.toString(), buttons);
    }
    /*
     * Инструкция по добавлению мероприятия.
     * Требует формат: Название, Дата, Время, Место, Описание.
     */
    private ObjectNode adminAddPrompt(Category category) {
        String text = "Создание нового мероприятия в категории «" + category.getDisplayName() + "».\n\n" +
                "Пришлите данные в формате:\n" +
                "Название: ...\n" +
                "Дата: ...\n" +
                "Время: ...\n" +
                "Место: ...\n" +
                "Описание: ...";
        return messageFactory.textWithButtons(text, List.of(List.of(new Button("Назад", "admin:cat:" + category.name()))));
    }
    /*
     * Инструкция по редактированию мероприятия.
     * Просит отправить обновленные данные в нужном формате.
     */
    private ObjectNode adminEditPrompt(long eventId) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            return adminMenuMessage();
        }
        String text = "Обновление мероприятия «" + event.getTitle() + "».\n\n" +
                "Пришлите обновлённые данные в формате:\n" +
                "Название: ...\n" +
                "Дата: ...\n" +
                "Время: ...\n" +
                "Место: ...\n" +
                "Описание: ...";
        return messageFactory.textWithButtons(text, List.of(List.of(new Button("Назад", "admin:event:" + eventId))));
    }
    /*
     * Сообщает об ошибке формата ввода.
     * Повторно показывает правильный шаблон данных.
     */
    private ObjectNode adminInputError(Category category) {
        String text = "Кажется, не хватает данных.\n\n" +
                "Пожалуйста, отправьте сообщение в формате:\n" +
                "Название: ...\n" +
                "Дата: ...\n" +
                "Время: ...\n" +
                "Место: ...\n" +
                "Описание: ...";
        return messageFactory.textWithButtons(text, List.of(List.of(new Button("Назад", "admin:cat:" + category.name()))));
    }
    /*
     * Извлекает числовой id из payload.
     * Используется для кнопок вида "prefix:id".
     */
    private long parseId(String payload, String prefix) {
        return parseLong(payload.substring(prefix.length()));
    }
    /*
     * Безопасно парсит число long.
     * Возвращает -1, если парсинг не удался.
     */
    private long parseLong(String value) {
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    /*
     * Разбирает строку формата key=value, key2=value2.
     * Возвращает карту параметров состояния.
     */
    private Map<String, String> parseData(String data) {
        Map<String, String> result = new HashMap<>();
        if (data == null || data.isBlank()) {
            return result;
        }
        String[] pairs = data.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            }
        }
        return result;
    }
    /*
     * Вспомогательная структура для разбора текста администратора.
     * Хранит заголовок, детали и флаг валидности.
     */
    private record EventInput(String title, String details, boolean valid) {
        /*
         * Проверяет, что данные для мероприятия валидны.
         * Используется перед сохранением в базу.
         */
        boolean isValid() {
            return valid;
        }
        /*
         * Разбирает текст администратора по ключам.
         * Формирует заголовок и полное описание мероприятия.
         */
        static EventInput parse(String text) {
            Map<String, String> map = new HashMap<>();
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                int idx = line.indexOf(':');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim().toLowerCase(java.util.Locale.ROOT);
                String value = line.substring(idx + 1).trim();
                if (!value.isEmpty()) {
                    map.put(key, value);
                }
            }
            String title = map.get("название");
            String date = map.get("дата");
            String time = map.get("время");
            String place = map.get("место");
            String description = map.get("описание");
            if (title == null || date == null || time == null || place == null || description == null) {
                return new EventInput(null, null, false);
            }
            String details = "✨ " + title + "\n\n" +
                    "🗓 Дата: " + date + "\n" +
                    "🕒 Время: " + time + "\n" +
                    "📍 Место: " + place + "\n\n" +
                    description;
            return new EventInput(title, details, true);
        }
    }
}
