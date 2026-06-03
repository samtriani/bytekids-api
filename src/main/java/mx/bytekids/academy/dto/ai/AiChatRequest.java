package mx.bytekids.academy.dto.ai;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiChatRequest {
    private String message;
    private List<Map<String, String>> history;
}
