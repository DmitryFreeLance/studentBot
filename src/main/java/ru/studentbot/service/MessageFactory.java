// Этот файл отвечает за сборку сообщений и inline-кнопок.
package ru.studentbot.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import java.util.List;
/*
 * Фабрика сообщений.
 * Создает текстовые сообщения и inline-кнопки в нужном формате.
 */
@Component
public class MessageFactory {
    private final ObjectMapper objectMapper;
    /*
     * Конструктор фабрики.
     * Получает ObjectMapper для сборки JSON.
     */
    public MessageFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    /*
     * Собирает JSON сообщения с текстом и inline-кнопками.
     * Формирует вложение inline_keyboard по кнопкам.
     */
    public ObjectNode textWithButtons(String text, List<List<Button>> buttons) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("text", text);
        if (buttons == null || buttons.isEmpty()) {
            return message;
        }
        ArrayNode attachments = objectMapper.createArrayNode();
        ObjectNode inlineKeyboard = objectMapper.createObjectNode();
        inlineKeyboard.put("type", "inline_keyboard");
        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode rows = objectMapper.createArrayNode();
        for (List<Button> row : buttons) {
            ArrayNode rowNode = objectMapper.createArrayNode();
            for (Button button : row) {
                ObjectNode buttonNode = objectMapper.createObjectNode();
                buttonNode.put("type", "callback");
                buttonNode.put("text", button.text());
                buttonNode.put("payload", button.payload());
                rowNode.add(buttonNode);
            }
            rows.add(rowNode);
        }
        payload.set("buttons", rows);
        inlineKeyboard.set("payload", payload);
        attachments.add(inlineKeyboard);
        message.set("attachments", attachments);
        return message;
    }
    /*
     * Модель кнопки inline-клавиатуры.
     * Хранит текст на кнопке и payload для callback.
     */
    public record Button(String text, String payload) {
    }
}
